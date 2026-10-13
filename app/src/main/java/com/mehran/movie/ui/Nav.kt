package com.mehran.movie.ui

import androidx.compose.runtime.mutableStateListOf
import com.mehran.movie.data.MediaItem

sealed interface Screen {
    data object Home : Screen
    data class Browse(val type: String, val title: String) : Screen
    data object Search : Screen
    data class Details(val type: String, val externalId: String, val title: String) : Screen
    /** Season / episode / quality picker shown before playback. */
    data class Picker(val media: MediaItem) : Screen
    data class Player(val url: String, val title: String, val subtitle: String? = null) : Screen
    data object Login : Screen
}

/** Minimal back stack. Kept as a plain holder so screens can read it reactively. */
class Navigator(initial: Screen = Screen.Home) {
    private val stack = mutableStateListOf(initial)

    val current: Screen get() = stack.last()
    val canGoBack: Boolean get() = stack.size > 1

    fun push(screen: Screen) {
        stack.add(screen)
    }

    fun pop(): Boolean {
        if (stack.size <= 1) return false
        stack.removeAt(stack.size - 1)
        return true
    }

    fun replaceTop(screen: Screen) {
        stack[stack.size - 1] = screen
    }

    fun resetTo(screen: Screen) {
        stack.clear()
        stack.add(screen)
    }
}
