package com.mehran.movie

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.mehran.movie.ui.MehranMovieRoot
import com.mehran.movie.ui.theme.MehranMovieTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    /** The DI container; also used to renew the session on resume. */
    private val container by lazy { (application as MehranMovieApp).container }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        setContent {
            MehranMovieTheme {
                MehranMovieRoot(container)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Access tokens last ~5 minutes: renew on every return to the app so the
        // viewer stays signed in after the box has been idle.
        lifecycleScope.launch { container.ensureSession() }
    }
}
