package com.mehran.movie.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale as drawScale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mehran.movie.ui.theme.Brand
import com.mehran.movie.ui.theme.Text2
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.PI

/** How long the intro owns the screen before handing over to the app. */
private const val INTRO_MILLIS = 2700L

/**
 * Launch intro: a cute clapperboard mascot that pops in, claps once and blinks,
 * then the MEHRAN MOVIE lockup fades up with a light sweep across it. Runs for
 * [INTRO_MILLIS] and hands over to the app through [onFinished].
 */
@Composable
fun MehranMovieIntro(onFinished: () -> Unit) {
    val badgeScale = remember { Animatable(0.7f) }
    val badgeAlpha = remember { Animatable(0f) }
    val clapAngle = remember { Animatable(-30f) }
    val eyeOpen = remember { Animatable(1f) }
    val lockupAlpha = remember { Animatable(0f) }
    val lockupShift = remember { Animatable(26f) }
    val shine = remember { Animatable(-0.45f) }
    val rootAlpha = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        launch { badgeAlpha.animateTo(1f, tween(durationMillis = 340)) }
        launch {
            badgeScale.animateTo(
                1f,
                spring(dampingRatio = 0.42f, stiffness = Spring.StiffnessLow)
            )
        }
        launch {
            delay(250)
            clapAngle.animateTo(
                0f,
                spring(dampingRatio = 0.32f, stiffness = Spring.StiffnessMediumLow)
            )
        }
        launch {
            delay(950)
            eyeOpen.animateTo(0.06f, tween(durationMillis = 90))
            eyeOpen.animateTo(1f, tween(durationMillis = 130))
        }
        launch {
            delay(780)
            lockupAlpha.animateTo(1f, tween(durationMillis = 430, easing = FastOutSlowInEasing))
        }
        launch {
            delay(780)
            lockupShift.animateTo(0f, tween(durationMillis = 430, easing = FastOutSlowInEasing))
        }
        launch {
            delay(1180)
            shine.animateTo(1.45f, tween(durationMillis = 900, easing = LinearEasing))
        }
        launch {
            delay(2240)
            rootAlpha.animateTo(0f, tween(durationMillis = 440, easing = FastOutSlowInEasing))
        }
        delay(INTRO_MILLIS)
        onFinished()
    }

    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = rootAlpha.value }
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF08080C), Color(0xFF171017), Color(0xFF08080C))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        IntroGlow()

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .graphicsLayer {
                        scaleX = badgeScale.value
                        scaleY = badgeScale.value
                        alpha = badgeAlpha.value
                    }
                    .size(196.dp)
                    .shadow(elevation = 30.dp, shape = RoundedCornerShape(48.dp), clip = false)
                    .clip(RoundedCornerShape(48.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFFF76A6E), Color(0xFFE03A42), Color(0xFFB6161F))
                        )
                    )
                    .border(2.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(48.dp))
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    drawMascot(clapDeg = clapAngle.value, eyeOpen = eyeOpen.value)
                }
            }

            Spacer(Modifier.height(30.dp))

            Box(
                Modifier
                    .graphicsLayer {
                        alpha = lockupAlpha.value
                        translationY = lockupShift.value
                    }
                    .padding(horizontal = 28.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "MEHRAN",
                        style = TextStyle(
                            brush = Brush.verticalGradient(
                                listOf(Color(0xFFFFFFFF), Color(0xFFDCD2C4))
                            ),
                            fontSize = 54.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 12.sp
                        )
                    )
                    Text(
                        text = "MOVIE",
                        style = TextStyle(
                            brush = Brush.verticalGradient(
                                listOf(Color(0xFFF76A6E), Color(0xFFD3242E))
                            ),
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 22.sp
                        )
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            Modifier
                                .size(width = 54.dp, height = 1.dp)
                                .background(Color(0x66F5F2EC))
                        )
                        Box(
                            Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Brand)
                        )
                        Box(
                            Modifier
                                .size(width = 54.dp, height = 1.dp)
                                .background(Color(0x66F5F2EC))
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(text = "مهران مووی", color = Text2, fontSize = 17.sp)
                }

                // Light sweep across the wordmark once it has appeared.
                Box(
                    Modifier
                        .matchParentSize()
                        .drawWithContent {
                            drawContent()
                            val p = shine.value
                            if (p > -0.4f) {
                                val band = size.width * 0.34f
                                val x = size.width * p
                                drawRect(
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color(0x66FFE9D6),
                                            Color.Transparent
                                        ),
                                        start = Offset(x - band, 0f),
                                        end = Offset(x + band, size.height * 0.6f)
                                    ),
                                    blendMode = BlendMode.Plus
                                )
                            }
                        }
                )
            }
        }

        LoadingDots(Modifier.align(Alignment.BottomCenter).padding(bottom = 54.dp))
    }
}

/** Soft pulsing red halo behind the logo. */
@Composable
private fun IntroGlow() {
    val pulse by rememberInfiniteTransition(label = "glow").animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowPulse"
    )
    Canvas(Modifier.size(560.dp)) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Brand.copy(alpha = 0.24f * pulse), Color.Transparent),
                center = center,
                radius = size.minDimension / 2f
            ),
            radius = size.minDimension / 2f,
            center = center
        )
    }
}

/** Three dots beating in sequence while the intro plays. */
@Composable
private fun LoadingDots(modifier: Modifier = Modifier) {
    val phase by rememberInfiniteTransition(label = "dots").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "dotsPhase"
    )
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        repeat(3) { i ->
            val wave = 0.5f + 0.5f * cos(2f * PI.toFloat() * (phase - i / 3f))
            Box(
                Modifier
                    .padding(horizontal = 5.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF5F2EC).copy(alpha = 0.20f + 0.65f * wave))
            )
        }
    }
}

/**
 * The mascot: a smiling clapperboard. [clapDeg] hinges the striped bar (0 = closed)
 * and [eyeOpen] squashes the eyes for the blink.
 */
private fun DrawScope.drawMascot(clapDeg: Float, eyeOpen: Float) {
    val w = size.width
    val h = size.height
    val cream = Color(0xFFF8F4ED)
    val ink = Color(0xFF14151C)
    val blush = Color(0x59F0454A)

    // Clapper bar, hinged at its top-left so it swings down like a real slate.
    val barLeft = w * 0.17f
    val barTop = h * 0.225f
    val barW = w * 0.66f
    val barH = h * 0.145f
    val barRadius = barH * 0.34f

    rotate(degrees = clapDeg, pivot = Offset(barLeft + barRadius, barTop + barH)) {
        drawRoundRect(
            color = cream,
            topLeft = Offset(barLeft, barTop),
            size = Size(barW, barH),
            cornerRadius = CornerRadius(barRadius, barRadius)
        )
        clipRect(
            left = barLeft + barRadius,
            top = barTop + barH * 0.10f,
            right = barLeft + barW - barRadius,
            bottom = barTop + barH * 0.90f
        ) {
            var x = barLeft + barH * 0.30f
            while (x < barLeft + barW) {
                drawLine(
                    color = ink,
                    start = Offset(x, barTop + barH),
                    end = Offset(x + barH, barTop),
                    strokeWidth = barH * 0.28f
                )
                x += barH * 0.92f
            }
        }
    }

    // Board.
    val boardTop = h * 0.375f
    val boardH = h * 0.44f
    drawRoundRect(
        color = cream,
        topLeft = Offset(w * 0.17f, boardTop),
        size = Size(w * 0.66f, boardH),
        cornerRadius = CornerRadius(h * 0.085f, h * 0.085f)
    )

    // Blush.
    listOf(0.30f, 0.70f).forEach { fx ->
        drawCircle(color = blush, radius = h * 0.036f, center = Offset(w * fx, h * 0.700f))
    }

    // Eyes, with a highlight and a quick blink.
    val eyeR = h * 0.058f
    val eyeY = h * 0.535f
    listOf(0.375f, 0.625f).forEach { fx ->
        val c = Offset(w * fx, eyeY)
        drawScale(1f, eyeOpen.coerceAtLeast(0.05f), pivot = c) {
            drawCircle(color = ink, radius = eyeR, center = c)
            drawCircle(
                color = Color(0xFFFFFFFF),
                radius = eyeR * 0.32f,
                center = Offset(c.x - eyeR * 0.30f, c.y - eyeR * 0.34f)
            )
        }
    }

    // Smile.
    val smileR = h * 0.10f
    drawArc(
        color = ink,
        startAngle = 25f,
        sweepAngle = 130f,
        useCenter = false,
        topLeft = Offset(w * 0.5f - smileR, h * 0.590f - smileR),
        size = Size(smileR * 2f, smileR * 2f),
        style = Stroke(width = h * 0.030f, cap = StrokeCap.Round)
    )
}
