package com.mehran.movie.auth

import com.mehran.movie.data.ApiClient
import com.mehran.movie.data.ApiException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Authentication via the site's auth endpoints.
 *
 * Sign-in is e-mail + password (`POST /api/auth/login`), matching the website's
 * own login form. A successful response carries
 * `{ accessToken, refreshToken, expiresIn }`; those are stored for later API
 * calls and refreshed via `POST /api/auth/refresh`.
 */
class AuthRepository(
    private val api: ApiClient,
    private val session: SessionStore
) {

    /**
     * Serializes refreshes. Access tokens live ~5 minutes, so several requests
     * can hit a 401 at once (e.g. links + detail after returning from playback).
     * Without this lock they would all POST the same refresh token; servers that
     * rotate refresh tokens accept only the first one, so the losers would look
     * like a dead session.
     */
    private val refreshMutex = Mutex()

    /** Signs in and stores the session. Throws [ApiException] with the server message. */
    suspend fun login(email: String, password: String) {
        val body = buildJsonObject {
            put("email", email.trim())
            put("password", password)
            put("deviceId", session.deviceId())
        }.toString()

        val root = api.json.parseToJsonElement(api.postRaw("/auth/login", body, retryOnAuth = false))
        val access = findString(root, "accessToken")
            ?: throw ApiException(0, "پاسخ سرور نامعتبر بود")
        val refresh = findString(root, "refreshToken")
        val name = findString(root, "displayName")
            ?: findString(root, "name")
            ?: findString(root, "email")
            ?: email.trim()
        session.saveSession(access, refresh, name, expiryMillis(root))
    }

    /**
     * Brings the stored session up to date on app start and on resume.
     *
     * Access tokens live ~5 minutes, so after any idle period the first
     * protected call would otherwise have to discover the expiry with a 401.
     * Refreshing here (and persisting the rotated tokens) is what keeps the
     * viewer signed in across restarts instead of falling back to the login
     * screen.
     */
    suspend fun ensureFreshSession() {
        if (session.accessToken.isNullOrBlank()) return
        if (session.refreshToken.isNullOrBlank()) return
        // Expiry is 0 for sessions saved by older builds; refresh once to learn it.
        if (session.accessExpiresAt > 0L && !session.isAccessTokenExpiring()) return
        refreshIfPossible()
    }

    suspend fun me(): JsonElement = withContext(Dispatchers.IO) {
        api.json.parseToJsonElement(api.getRaw("/auth/me"))
    }

    /**
     * Refreshes the access token if the stored refresh token is still valid.
     *
     * Returns true when a usable access token is in place. On a 401/403 from the
     * refresh endpoint the refresh token itself is invalid, so the stored
     * session is cleared; [ApiClient] then reports the session as expired and
     * the UI sends the viewer to the login screen.
     */
    suspend fun refreshIfPossible(): Boolean {
        val tokenBefore = session.accessToken
        val rt = session.refreshToken ?: return false

        return refreshMutex.withLock {
            // Another request may already have refreshed while we waited.
            if (session.accessToken != tokenBefore) return@withLock true

            try {
                val body = buildJsonObject {
                    put("refreshToken", rt)
                    put("deviceId", session.deviceId())
                }.toString()
                val root = api.json.parseToJsonElement(
                    api.postRaw("/auth/refresh", body, retryOnAuth = false)
                )
                val access = findString(root, "accessToken")
                if (access != null) {
                    session.saveSession(
                        access,
                        findString(root, "refreshToken") ?: rt,
                        session.userName,
                        expiryMillis(root)
                    )
                    true
                } else {
                    false
                }
            } catch (e: ApiException) {
                // Only a real rejection of the refresh token ends the session.
                // A 403 also comes from Cloudflare/WAF blocks and rate limits;
                // clearing on those used to sign the viewer out for no reason.
                if (isTokenRejection(e)) session.clear()
                false
            } catch (_: Exception) {
                false
            }
        }
    }

    suspend fun logout() {
        val rt = session.refreshToken
        if (rt != null) {
            runCatching {
                val body = buildJsonObject { put("refreshToken", rt) }.toString()
                api.postRaw("/auth/logout", body)
            }
        }
        session.clear()
    }

    private fun isTokenRejection(e: ApiException): Boolean = isTokenRejection(e.status, e.body)

    /** Absolute expiry of the access token carried by an auth response, 0 if absent. */
    private fun expiryMillis(el: JsonElement): Long =
        expiresAtFrom(findPrimitive(el, "expiresIn")?.content)

    /** Depth-first search for the first string value under [key]. */
    private fun findString(el: JsonElement, key: String): String? = findPrimitive(el, key)
        ?.takeIf { it.isString }
        ?.content

    /** Depth-first search for the first primitive (string or number) under [key]. */
    private fun findPrimitive(el: JsonElement, key: String): JsonPrimitive? {
        when (el) {
            is JsonObject -> {
                (el[key] as? JsonPrimitive)?.let { return it }
                el.values.forEach { v -> findPrimitive(v, key)?.let { return it } }
            }
            is JsonArray -> el.forEach { v -> findPrimitive(v, key)?.let { return it } }
            else -> {}
        }
        return null
    }
}

/**
 * True when the auth endpoint itself rejected the refresh token, as opposed to a
 * transport-level 403 (Cloudflare "1010", rate limit, …) that says nothing about
 * the stored session. Clearing the session on those used to sign the viewer out
 * for no reason.
 */
internal fun isTokenRejection(status: Int, body: String): Boolean {
    if (status == 401) return true
    if (status != 403) return false
    val payload = body.lowercase()
    if (payload.isBlank() || payload.trimStart().startsWith("<")) return false
    return payload.contains("refresh") || payload.contains("token") ||
        payload.contains("session") || payload.contains("auth")
}

/**
 * Turns an `expiresIn` value — seconds as a number or numeric string, or a
 * duration such as `"5m"` / `"1h"` / `"30s"` — into an absolute expiry time.
 * Returns 0 when the server did not say, which disables the proactive refresh.
 */
internal fun expiresAtFrom(raw: String?, now: Long = System.currentTimeMillis()): Long {
    val value = raw?.trim()?.lowercase().orEmpty()
    if (value.isEmpty()) return 0L
    val seconds = when {
        value.all { it.isDigit() } -> value.toLong()
        else -> {
            val n = value.dropLast(1).toLongOrNull() ?: return 0L
            when (value.last()) {
                's' -> n
                'm' -> n * 60
                'h' -> n * 3_600
                'd' -> n * 86_400
                else -> return 0L
            }
        }
    }
    if (seconds <= 0L) return 0L
    return now + seconds * 1_000L
}
