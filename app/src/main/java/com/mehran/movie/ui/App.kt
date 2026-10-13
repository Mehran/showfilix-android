package com.mehran.movie.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.mehran.movie.AppContainer
import com.mehran.movie.data.MediaItem
import com.mehran.movie.ui.screens.BrowseScreen
import com.mehran.movie.ui.screens.DetailsScreen
import com.mehran.movie.ui.screens.HomeScreen
import com.mehran.movie.ui.screens.LoginScreen
import com.mehran.movie.ui.screens.PlaybackPickerScreen
import com.mehran.movie.ui.screens.PlayerScreen
import com.mehran.movie.ui.screens.SearchScreen
import com.mehran.movie.ui.theme.Bg0

@Composable
fun MehranMovieRoot(container: AppContainer) {
    var introDone by remember { mutableStateOf(false) }

    if (!introDone) {
        MehranMovieIntro(onFinished = { introDone = true })
    } else {
        AppContent(container)
    }
}

@Composable
private fun AppContent(container: AppContainer) {
    val nav = remember { Navigator() }
    var isLoggedIn by remember { mutableStateOf(container.session.isLoggedIn) }

    val openDetails: (MediaItem) -> Unit = { item ->
        nav.push(Screen.Details(item.type ?: "movie", item.externalId ?: "", item.displayTitle))
    }

    BackHandler(enabled = nav.canGoBack) { nav.pop() }

    Box(Modifier.fillMaxSize().background(Bg0)) {
        when (val screen = nav.current) {
            is Screen.Home -> HomeScreen(
                catalog = container.catalog,
                isLoggedIn = isLoggedIn,
                onOpenDetails = openDetails,
                onPlay = { item ->
                    nav.push(Screen.Details(item.type ?: "movie", item.externalId ?: "", item.displayTitle))
                },
                onBrowse = { type, title -> nav.push(Screen.Browse(type, title)) },
                onSearch = { nav.push(Screen.Search) },
                onLogin = { nav.push(Screen.Login) }
            )

            is Screen.Browse -> BrowseScreen(
                catalog = container.catalog,
                type = screen.type,
                title = screen.title,
                onOpenDetails = openDetails
            )

            is Screen.Search -> SearchScreen(
                catalog = container.catalog,
                onOpenDetails = openDetails
            )

            is Screen.Details -> DetailsScreen(
                catalog = container.catalog,
                type = screen.type,
                externalId = screen.externalId,
                isLoggedIn = isLoggedIn,
                onPickPlayback = { media -> nav.push(Screen.Picker(media)) },
                onOpenDetails = openDetails,
                onLogin = { nav.push(Screen.Login) }
            )

            is Screen.Picker -> PlaybackPickerScreen(
                catalog = container.catalog,
                media = screen.media,
                isLoggedIn = isLoggedIn,
                onPlay = { url, title, subtitle -> nav.push(Screen.Player(url, title, subtitle)) },
                onLogin = { nav.push(Screen.Login) }
            )

            is Screen.Login -> LoginScreen(
                auth = container.auth,
                onLoggedIn = {
                    isLoggedIn = true
                    nav.pop()
                }
            )

            is Screen.Player -> PlayerScreen(
                url = screen.url,
                title = screen.title,
                subtitle = screen.subtitle
            )
        }
    }
}
