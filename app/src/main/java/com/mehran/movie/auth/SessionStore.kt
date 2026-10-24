package com.mehran.movie.auth

import android.content.Context
import android.content.SharedPreferences
import java.util.UUID

/**
 * Stores the anonymous visitor id plus the access/refresh tokens returned by the
 * auth endpoints.
 *
 * Writes go through [SharedPreferences.Editor.commit] instead of `apply()`: the
 * tokens are the only thing keeping the viewer signed in, and a TV box may kill
 * the process right after a login or a refresh. `apply()` flushes on a
 * background thread, so a kill in that window used to lose the freshly rotated
 * refresh token and sign the viewer out on the next launch.
 */
class SessionStore(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("mehran_movie_session", Context.MODE_PRIVATE)

    /** Stable per-install visitor id, sent as `x-visitor-id`. */
    fun visitorId(): String {
        prefs.getString(KEY_VISITOR, null)?.let { return it }
        val id = UUID.randomUUID().toString()
        prefs.edit().putString(KEY_VISITOR, id).commit()
        return id
    }

    /** Stable per-install device id, sent to the auth endpoints. */
    fun deviceId(): String {
        prefs.getString(KEY_DEVICE, null)?.let { return it }
        val id = UUID.randomUUID().toString()
        prefs.edit().putString(KEY_DEVICE, id).commit()
        return id
    }

    var accessToken: String?
        get() = prefs.getString(KEY_ACCESS, null)
        set(v) {
            prefs.edit().putString(KEY_ACCESS, v).commit()
        }

    var refreshToken: String?
        get() = prefs.getString(KEY_REFRESH, null)
        set(v) {
            prefs.edit().putString(KEY_REFRESH, v).commit()
        }

    var userName: String?
        get() = prefs.getString(KEY_NAME, null)
        set(v) {
            prefs.edit().putString(KEY_NAME, v).commit()
        }

    /** Absolute epoch millis when the stored access token expires; 0 = unknown. */
    var accessExpiresAt: Long
        get() = prefs.getLong(KEY_EXPIRES_AT, 0L)
        set(v) {
            prefs.edit().putLong(KEY_EXPIRES_AT, v).commit()
        }

    fun accessToken(): String? = accessToken
    fun refreshToken(): String? = refreshToken

    /**
     * Signed in when either token is present: a session whose short-lived access
     * token has expired is still valid thanks to the refresh token.
     */
    val isLoggedIn: Boolean
        get() = !accessToken.isNullOrBlank() || !refreshToken.isNullOrBlank()

    /** True when the access token is expired or expires within [skewMs]. */
    fun isAccessTokenExpiring(skewMs: Long = 60_000L): Boolean {
        val at = accessExpiresAt
        return at > 0L && System.currentTimeMillis() >= at - skewMs
    }

    fun saveSession(access: String?, refresh: String?, name: String?, expiresAt: Long = 0L) {
        prefs.edit()
            .putString(KEY_ACCESS, access)
            .putString(KEY_REFRESH, refresh)
            .putString(KEY_NAME, name)
            .putLong(KEY_EXPIRES_AT, expiresAt)
            .commit()
    }

    fun clear() {
        prefs.edit()
            .remove(KEY_ACCESS)
            .remove(KEY_REFRESH)
            .remove(KEY_NAME)
            .remove(KEY_EXPIRES_AT)
            .commit()
    }

    private companion object {
        const val KEY_VISITOR = "visitor_id"
        const val KEY_DEVICE = "device_id"
        const val KEY_ACCESS = "access_token"
        const val KEY_REFRESH = "refresh_token"
        const val KEY_NAME = "user_name"
        const val KEY_EXPIRES_AT = "access_expires_at"
    }
}
