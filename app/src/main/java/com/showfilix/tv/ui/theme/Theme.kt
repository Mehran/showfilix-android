package com.showfilix.tv.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Brand = Color(0xFFF0454A)
val Bg0 = Color(0xFF0B0B0F)
val Bg1 = Color(0xFF111319)
val Surface1 = Color(0xFF161922)
val Surface2 = Color(0xFF20242F)
val Text1 = Color(0xFFF5F2EC)
val Text2 = Color(0xFFB9B5AD)

private val Scheme = darkColorScheme(
    primary = Brand,
    onPrimary = Color.White,
    background = Bg0,
    onBackground = Text1,
    surface = Surface1,
    onSurface = Text1,
    surfaceVariant = Surface2,
    onSurfaceVariant = Text2,
    secondary = Brand,
    onSecondary = Color.White
)

@Composable
fun ShowfilixTheme(content: @Composable () -> Unit) {
    @Suppress("UNUSED_EXPRESSION") isSystemInDarkTheme()
    MaterialTheme(colorScheme = Scheme, content = content)
}
