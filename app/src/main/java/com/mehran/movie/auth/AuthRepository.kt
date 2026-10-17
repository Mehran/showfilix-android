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
        session.saveSession(access, refresh, name)
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
                    session.saveSession(access, findString(root, "refreshToken") ?: rt, session.userName)
                    true
                } else {
                    false
                }
            } catch (e: ApiException) {
                // 401/403: the refresh token was rejected. Anything else is
                // treated as transient so a flaky network does not log out.
                if (e.status == 401 || e.status == 403) session.clear()
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

    /** Depth-first search for the first string value under [key]. */
    private fun findString(el: JsonElement, key: String): String? {
        when (el) {
            is JsonObject -> {
                (el[key] as? JsonPrimitive)?.takeIf { it.isString }?.let { return it.content }
                el.values.forEach { v -> findString(v, key)?.let { return it } }
            }
            is JsonArray -> el.forEach { v -> findString(v, key)?.let { return it } }
            else -> {}
        }
        return null
    }
}
