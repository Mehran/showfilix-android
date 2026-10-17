package com.mehran.movie.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.ViewGroup
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay

/** How long the title stays on screen after playback starts. */
private const val OVERLAY_VISIBLE_MS = 4000L

@Composable
fun PlayerScreen(url: String, title: String, subtitle: String?) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var overlayVisible by remember { mutableStateOf(true) }
    // True while playback is wanted: covers buffering too, but not pause/end.
    var keepScreenOn by remember { mutableStateOf(true) }

    val player = remember {
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(context))
            .build()
            .apply {
                // Do NOT force a MIME type: showfilix streams are progressive
                // MKV/MP4 files served via /api/media/stream. Forcing HLS made
                // playback fail; let ExoPlayer sniff the container instead.
                setMediaItem(MediaItem.fromUri(url))
                prepare()
                playWhenReady = true
            }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                keepScreenOn = player.wantsScreenOn()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                keepScreenOn = player.wantsScreenOn()
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    // Keep the NetBox awake while a video is on screen. FLAG_KEEP_SCREEN_ON
    // suppresses the system screensaver/idle timeout for as long as the flag is
    // set; it is cleared on pause, on end of playback and whenever this screen
    // leaves composition, so the box can sleep normally afterwards.
    val activity = context.findActivity()
    DisposableEffect(activity, keepScreenOn) {
        val window = activity?.window
        if (keepScreenOn) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // While playing, show the title briefly and then fade it out.
    // While paused, keep it on screen so the viewer knows what is loaded.
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            overlayVisible = true
            delay(OVERLAY_VISIBLE_MS)
            overlayVisible = false
        } else {
            overlayVisible = true
        }
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

        AnimatedVisibility(
            visible = overlayVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopStart)
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xCC000000), Color(0x66000000), Color.Transparent)
                        )
                    )
            ) {
                Column(Modifier.align(Alignment.TopStart).padding(32.dp)) {
                    Text(
                        title,
                        color = Color.White,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold
                    )
                    subtitle?.takeIf { it.isNotBlank() }?.let {
                        Text(it, color = Color(0xFFB9B5AD), fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

/**
 * Whether playback should currently hold the screen on. `isPlaying` alone is
 * not enough: it is false while the stream buffers after a resume, which would
 * let the screensaver start mid-playback.
 */
private fun Player.wantsScreenOn(): Boolean =
    playWhenReady &&
        playbackState != Player.STATE_IDLE &&
        playbackState != Player.STATE_ENDED

/** Compose hands out a [Context]; the keep-screen-on flag lives on the Activity window. */
private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
