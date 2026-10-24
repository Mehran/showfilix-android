package com.mehran.movie

import android.app.Application
import com.mehran.movie.auth.AuthRepository
import com.mehran.movie.auth.SessionStore
import com.mehran.movie.data.ApiClient
import com.mehran.movie.data.CatalogRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Tiny manual DI container. */
class AppContainer(app: Application) {
    val session = SessionStore(app)
    val api = ApiClient(session)
    val catalog = CatalogRepository(api)
    val auth = AuthRepository(api, session)

    /**
     * Set when a protected request came back 401/403 and the token could not be
     * refreshed. The UI observes it and shows the login screen, so an expired
     * session is never mistaken for "subscription required".
     */
    private val _sessionExpired = MutableStateFlow(false)
    val sessionExpired: StateFlow<Boolean> = _sessionExpired.asStateFlow()

    init {
        // Access tokens expire after ~5 minutes; refresh once on a 401/403.
        api.onUnauthorized = { auth.refreshIfPossible() }
        api.onSessionExpired = { _sessionExpired.value = true }
    }

    /** Called by the UI once it has navigated to Login. */
    fun acknowledgeSessionExpired() {
        _sessionExpired.value = false
    }

    /**
     * Renews the stored session on app start and on resume. Without this the
     * ~5 minute access token is always stale after an idle period, and the first
     * protected request has to rediscover that with a 401 — which looked like a
     * lost login. If the refresh token turns out to be dead, the UI is told so it
     * can offer the login screen instead of a paywall.
     */
    suspend fun ensureSession() {
        if (!session.isLoggedIn) return
        auth.ensureFreshSession()
        if (!session.isLoggedIn) _sessionExpired.value = true
    }
}

class MehranMovieApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
