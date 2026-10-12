package com.showfilix.tv.data

import com.showfilix.tv.auth.SessionStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
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
 * The API lives under `https://showfilix.com/api`.
 */
class ApiClient(private val session: SessionStore) {

    companion object {
        const val SITE = "https://showfilix.com"
        const val BASE = "$SITE/api"
    }

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
            .header("User-Agent", "ShowfilixTV/1.0 (Android TV)")
        session.accessToken()?.let { b.header("Authorization", "Bearer $it") }
        return b
    }

    /** GET a JSON payload as text, throwing [ApiException] on non-2xx. */
    suspend fun getRaw(path: String, params: Map<String, String?> = emptyMap()): String =
        withContext(Dispatchers.IO) {
            val resp = http.newCall(newRequest(path, params).get().build()).execute()
            resp.use {
                val body = it.body?.string().orEmpty()
                if (!it.isSuccessful) throw ApiException(it.code, extractMessage(body, it.code))
                body
            }
        }

    /** POST a JSON body. */
    suspend fun postRaw(
        path: String,
        jsonBody: String,
        accessTokenOverride: String? = null
    ): String = withContext(Dispatchers.IO) {
        val b = newRequest(path, emptyMap()).post(jsonBody.toRequestBody(jsonMedia))
        if (accessTokenOverride != null) b.header("Authorization", "Bearer $accessTokenOverride")
        val resp = http.newCall(b.build()).execute()
        resp.use {
            val body = it.body?.string().orEmpty()
            if (!it.isSuccessful) throw ApiException(it.code, extractMessage(body, it.code))
            body
        }
    }

    suspend inline fun <reified T> get(path: String, params: Map<String, String?> = emptyMap()): T {
        val raw = getRaw(path, params)
        return json.decodeFromString(raw)
    }

    suspend inline fun <reified T> post(path: String, body: String): T {
        val raw = postRaw(path, body)
        return json.decodeFromString(raw)
    }

    private fun extractMessage(body: String, code: Int): String = try {
        val obj = json.parseToJsonElement(body) as? kotlinx.serialization.json.JsonObject
        when (val msg = obj?.get("message")) {
            null -> "HTTP $code"
            // validation errors arrive as an array of strings
            is kotlinx.serialization.json.JsonArray ->
                msg.joinToString("، ") { it.toString().trim('"') }
            else -> msg.toString().trim('"')
        }
    } catch (_: Exception) {
        if (body.isBlank()) "HTTP $code" else body.take(200)
    }
}
