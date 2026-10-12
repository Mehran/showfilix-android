package com.showfilix.tv.auth

import com.showfilix.tv.data.ApiClient
import com.showfilix.tv.data.ApiException
import kotlinx.coroutines.Dispatchers
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

    /** Refreshes the access token if the stored refresh token is still valid. */
    suspend fun refreshIfPossible(): Boolean {
        val rt = session.refreshToken ?: return false
        return try {
            val body = buildJsonObject {
                put("refreshToken", rt)
                put("deviceId", session.deviceId())
            }.toString()
            val root = api.json.parseToJsonElement(api.postRaw("/auth/refresh", body, retryOnAuth = false))
            val access = findString(root, "accessToken")
            if (access != null) {
                session.saveSession(access, findString(root, "refreshToken") ?: rt, session.userName)
                true
            } else false
        } catch (_: Exception) {
            false
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
