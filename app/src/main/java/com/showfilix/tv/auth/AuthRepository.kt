package com.showfilix.tv.auth

import com.showfilix.tv.data.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Authentication via the site's auth endpoints.
 *
 * Only the phone-OTP flow is implemented (no Google OAuth on a TV). The exact
 * JSON envelope of a successful login is not documented, so the session is
 * extracted by recursively searching the response for the token keys.
 */
class AuthRepository(
    private val api: ApiClient,
    private val session: SessionStore
) {

    suspend fun sendOtp(phone: String) {
        val body = buildJsonObject { put("phone", phone) }.toString()
        api.postRaw("/auth/otp/send", body)
    }

    /** Exchange the SMS code for a session. Returns the display name if present. */
    suspend fun loginWithOtp(phone: String, code: String): String? {
        val body = buildJsonObject {
            put("phone", phone)
            put("code", code)
            put("deviceId", session.deviceId())
        }.toString()
        val raw = api.postRaw("/auth/otp/login", body)
        val root = api.json.parseToJsonElement(raw)
        val access = findString(root, "accessToken") ?: findString(root, "access_token")
        val refresh = findString(root, "refreshToken") ?: findString(root, "refresh_token")
        val name = findString(root, "name") ?: findString(root, "phone")
            ?: findString(root, "email")
        session.saveSession(access ?: findString(root, "token"), refresh, name)
        return name
    }

    suspend fun me(): JsonElement = withContext(Dispatchers.IO) {
        api.json.parseToJsonElement(api.getRaw("/auth/me"))
    }

    suspend fun refreshIfPossible(): Boolean {
        val rt = session.refreshToken ?: return false
        return try {
            val body = buildJsonObject {
                put("refreshToken", rt)
                put("deviceId", session.deviceId())
            }.toString()
            val raw = api.postRaw("/auth/refresh", body)
            val root = api.json.parseToJsonElement(raw)
            val access = findString(root, "accessToken") ?: findString(root, "access_token")
            val refresh = findString(root, "refreshToken") ?: rt
            if (access != null) {
                session.saveSession(access, refresh, session.userName)
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
            is kotlinx.serialization.json.JsonArray ->
                el.forEach { v -> findString(v, key)?.let { return it } }
            else -> {}
        }
        return null
    }
}
