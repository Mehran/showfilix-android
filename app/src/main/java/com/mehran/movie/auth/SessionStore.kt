package com.mehran.movie.auth

import android.content.Context
import android.content.SharedPreferences
import java.util.UUID

/**
 * Stores the anonymous visitor id plus the access/refresh tokens returned by the
 * auth endpoints. Plain SharedPreferences is enough for a TV client.
 */
class SessionStore(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("mehran_movie_session", Context.MODE_PRIVATE)

    /** Stable per-install visitor id, sent as `x-visitor-id`. */
    fun visitorId(): String {
        prefs.getString(KEY_VISITOR, null)?.let { return it }
        val id = UUID.randomUUID().toString()
        prefs.edit().putString(KEY_VISITOR, id).apply()
        return id
    }

    /** Stable per-install device id, sent to the auth endpoints. */
    fun deviceId(): String {
        prefs.getString(KEY_DEVICE, null)?.let { return it }
        val id = UUID.randomUUID().toString()
        prefs.edit().putString(KEY_DEVICE, id).apply()
        return id
    }

    var accessToken: String?
        get() = prefs.getString(KEY_ACCESS, null)
        set(v) = prefs.edit().putString(KEY_ACCESS, v).apply()

    var refreshToken: String?
        get() = prefs.getString(KEY_REFRESH, null)
        set(v) = prefs.edit().putString(KEY_REFRESH, v).apply()

    var userName: String?
        get() = prefs.getString(KEY_NAME, null)
        set(v) = prefs.edit().putString(KEY_NAME, v).apply()

    fun accessToken(): String? = accessToken
    fun refreshToken(): String? = refreshToken

    val isLoggedIn: Boolean get() = !accessToken.isNullOrBlank()

    fun saveSession(access: String?, refresh: String?, name: String?) {
        prefs.edit()
            .putString(KEY_ACCESS, access)
            .putString(KEY_REFRESH, refresh)
            .putString(KEY_NAME, name)
            .apply()
    }

    fun clear() {
        prefs.edit()
            .remove(KEY_ACCESS)
            .remove(KEY_REFRESH)
            .remove(KEY_NAME)
            .apply()
    }

    private companion object {
        const val KEY_VISITOR = "visitor_id"
        const val KEY_DEVICE = "device_id"
        const val KEY_ACCESS = "access_token"
        const val KEY_REFRESH = "refresh_token"
        const val KEY_NAME = "user_name"
    }
}
