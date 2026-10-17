package com.mehran.movie.data

import com.mehran.movie.auth.SessionStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Error carrying the HTTP status and the API's message, if any. */
class ApiException(val status: Int, message: String) : IOException(message)

/**
 * Thin client over the showfilix.com JSON API (see showfilix-api-findings.md).
 *
 * The API lives under `https://showfilix.com/api`. Access tokens are short lived
 * (the JWT is valid for 5 minutes), so a 401/403 triggers a single
 * refresh-and-retry through [onUnauthorized].
 *
 * Auth is header based: the Bearer token is read from [SessionStore] for every
 * request. There is deliberately no CookieJar and no global header mutation, so
 * creating or releasing a video player cannot wipe the session — the tokens
 * live in SharedPreferences and nothing in the player touches them.
 */
class ApiClient(private val session: SessionStore) {

    companion object {
        const val SITE = "https://showfilix.com"
        const val BASE = "$SITE/api"
    }

    /** Installed by the DI container; refreshes the session. Returns true on success. */
    var onUnauthorized: (suspend () -> Boolean)? = null

    /**
     * Installed by the DI container. Fired when a request that carried a token
     * was rejected and the session could not be repaired, i.e. the viewer must
     * sign in again. Called from an IO thread.
     */
    var onSessionExpired: (() -> Unit)? = null

    val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
        coerceInputValues = true
    }

    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .callTimeout(30, TimeUnit.SECONDS)
        .build()

    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    private fun url(path: String, params: Map<String, String?> = emptyMap()): String {
        val base = (BASE + path).toHttpUrlOrNull() ?: error("bad url: $path")
        val b = base.newBuilder()
        params.forEach { (k, v) -> if (v != null) b.addQueryParameter(k, v) }
        return b.build().toString()
    }

    private fun newRequest(path: String, params: Map<String, String?>): Request.Builder {
        val b = Request.Builder()
            .url(url(path, params))
            .header("Accept", "application/json")
            .header("x-visitor-id", session.visitorId())
            .header("User-Agent", "MehranMovieTV/1.0 (Android TV)")
            .header("Origin", SITE)
            .header("Referer", "$SITE/")
        session.accessToken()?.let { b.header("Authorization", "Bearer $it") }
        return b
    }

    private fun run(req: Request): Pair<Int, String> {
        http.newCall(req).execute().use { resp ->
            return resp.code to resp.body?.string().orEmpty()
        }
    }

    private suspend fun send(
        path: String,
        params: Map<String, String?>,
        body: String?,
        retryOnAuth: Boolean
    ): String = withContext(Dispatchers.IO) {
        fun build() = newRequest(path, params)
            .apply { if (body != null) post(body.toRequestBody(jsonMedia)) else get() }
            .build()

        // The token this attempt was sent with. Used to decide whether an
        // auth failure is worth refreshing and whether it is a real expiry.
        val sentToken = session.accessToken

        val (code, payload) = run(build())
        if (code in 200..299) return@withContext payload

        // 401 = expired/invalid token. The API also uses 403 when a protected
        // route is hit without a valid session, so both are recoverable.
        val authFailure = code == 401 || code == 403

        if (authFailure && retryOnAuth && !sentToken.isNullOrBlank()) {
            if (onUnauthorized?.invoke() == true) {
                // build() re-reads the session, so this retry carries the new token.
                val (code2, payload2) = run(build())
                if (code2 in 200..299) return@withContext payload2
                if (code2 == 401 || code2 == 403) notifySessionExpired()
                throw ApiException(code2, extractMessage(payload2, code2))
            }
            // The refresh did not succeed. AuthRepository clears the stored
            // tokens when the server rejected the refresh token, so an empty
            // store means the session is gone; a network hiccup keeps them and
            // is surfaced as a normal, retryable error instead.
            if (session.refreshToken.isNullOrBlank()) notifySessionExpired()
        }
        throw ApiException(code, extractMessage(payload, code))
    }

    /** Tells the UI to fall back to the login screen instead of a paywall. */
    private fun notifySessionExpired() {
        onSessionExpired?.invoke()
    }

    /** GET a JSON payload as text, throwing [ApiException] on non-2xx. */
    suspend fun getRaw(
        path: String,
        params: Map<String, String?> = emptyMap(),
        retryOnAuth: Boolean = true
    ): String = send(path, params, body = null, retryOnAuth = retryOnAuth)

    /** POST a JSON body. */
    suspend fun postRaw(
        path: String,
        jsonBody: String,
        retryOnAuth: Boolean = true
    ): String = send(path, emptyMap(), body = jsonBody, retryOnAuth = retryOnAuth)

    suspend inline fun <reified T> get(path: String, params: Map<String, String?> = emptyMap()): T {
        val raw = getRaw(path, params)
        return json.decodeFromString(raw)
    }

    suspend inline fun <reified T> post(path: String, body: String): T {
        val raw = postRaw(path, body)
        return json.decodeFromString(raw)
    }

    private fun extractMessage(body: String, code: Int): String = try {
        val obj = json.parseToJsonElement(body) as? JsonObject
        when (val msg = obj?.get("message")) {
            null -> "HTTP $code"
            // validation errors arrive as an array of strings
            is JsonArray -> msg.joinToString("، ") { it.toString().trim('"') }
            else -> msg.toString().trim('"')
        }
    } catch (_: Exception) {
        if (body.isBlank()) "HTTP $code" else body.take(200)
    }
}
