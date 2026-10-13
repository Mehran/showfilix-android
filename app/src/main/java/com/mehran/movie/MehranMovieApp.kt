package com.mehran.movie

import android.app.Application
import com.mehran.movie.auth.AuthRepository
import com.mehran.movie.auth.SessionStore
import com.mehran.movie.data.ApiClient
import com.mehran.movie.data.CatalogRepository

/** Tiny manual DI container. */
class AppContainer(app: Application) {
    val session = SessionStore(app)
    val api = ApiClient(session)
    val catalog = CatalogRepository(api)
    val auth = AuthRepository(api, session)

    init {
        // Access tokens expire after ~5 minutes; refresh once on a 401.
        api.onUnauthorized = { auth.refreshIfPossible() }
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
