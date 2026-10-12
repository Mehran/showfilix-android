package com.showfilix.tv.ui.screens

import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView

@Composable
fun PlayerScreen(url: String, title: String, subtitle: String?) {
    val context = LocalContext.current
    val player = remember {
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(context))
            .build()
            .apply {
                setMediaItem(
                    MediaItem.Builder()
                        .setUri(url)
                        .setMimeType(MimeTypes.APPLICATION_M3U8)
                        .build()
                )
                prepare()
                playWhenReady = true
            }
    }

    DisposableEffect(player) {
        onDispose { player.release() }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    useController = true
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        Column(Modifier.align(Alignment.TopStart).padding(32.dp)) {
            Text(title, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            subtitle?.takeIf { it.isNotBlank() }?.let {
                Text(it, color = Color(0xFFB9B5AD), fontSize = 16.sp)
            }
        }
    }
}
