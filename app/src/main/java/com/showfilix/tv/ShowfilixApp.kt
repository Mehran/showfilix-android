package com.showfilix.tv

import android.app.Application
import com.showfilix.tv.auth.AuthRepository
import com.showfilix.tv.auth.SessionStore
import com.showfilix.tv.data.ApiClient
import com.showfilix.tv.data.CatalogRepository

/** Tiny manual DI container. */
class AppContainer(app: Application) {
    val session = SessionStore(app)
    val api = ApiClient(session)
    val catalog = CatalogRepository(api)
    val auth = AuthRepository(api, session)
}

class ShowfilixApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
