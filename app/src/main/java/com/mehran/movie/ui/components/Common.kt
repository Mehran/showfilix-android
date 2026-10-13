package com.mehran.movie.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mehran.movie.data.MediaItem
import com.mehran.movie.ui.theme.Brand
import com.mehran.movie.ui.theme.Surface1
import com.mehran.movie.ui.theme.Surface2
import com.mehran.movie.ui.theme.Text1
import com.mehran.movie.ui.theme.Text2
import kotlinx.coroutines.launch

/** A focusable poster. Scales up and gains a white ring when focused. */
@Composable
fun PosterCard(
    item: MediaItem,
    modifier: Modifier = Modifier,
    width: Int = 168,
    height: Int = 252,
    showTitle: Boolean = false,
    onClick: () -> Unit,
    onFocused: () -> Unit = {}
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (focused) 1.08f else 1f,
        animationSpec = tween(180),
        label = "posterScale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.width(width.dp)
    ) {
        Box(
            modifier = Modifier
                .width(width.dp)
                .height(height.dp)
                .onFocusChanged {
                    focused = it.isFocused
                    if (it.isFocused) onFocused()
                }
                .clickable(onClick = onClick)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Surface1)
                    .border(
                        width = if (focused) 3.dp else 0.dp,
                        color = if (focused) Color.White else Color.Transparent,
                        shape = RoundedCornerShape(10.dp)
                    )
            ) {
                AsyncImage(
                    model = item.posterUrl,
                    contentDescription = item.displayTitle,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(if (focused) 2.dp else 0.dp),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
                if (item.isSeries) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xCC000000))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("سریال", color = Text1, fontSize = 10.sp)
                    }
                }
            }
        }
        if (showTitle) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = item.displayTitle,
                color = if (focused) Text1 else Text2,
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/** A horizontal row of posters that keeps the focused card in view. */
@Composable
fun MediaRow(
    title: String,
    items: List<MediaItem>,
    onClick: (MediaItem) -> Unit
) {
    if (items.isEmpty()) return
    val state = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var focusedIndex by remember { mutableIntStateOf(0) }

    Column(Modifier.fillMaxWidth().padding(bottom = 18.dp)) {
        Text(
            text = title,
            color = Text1,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 48.dp, bottom = 10.dp)
        )
        LazyRow(
            state = state,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 48.dp)
        ) {
            itemsIndexed(items) { index, item ->
                PosterCard(
                    item = item,
                    onClick = { onClick(item) },
                    onFocused = {
                        focusedIndex = index
                        scope.launch {
                            val target = (index - 1).coerceAtLeast(0)
                            state.animateScrollToItem(target)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun LoadingBox(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Brand)
    }
}

@Composable
fun ErrorBox(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(message, color = Text2, fontSize = 18.sp)
            Spacer(Modifier.height(16.dp))
            FocusButton(text = "تلاش دوباره", onClick = onRetry)
        }
    }
}

/** A button that fills white when focused, for consistent TV focus feedback. */
@Composable
fun FocusButton(
    text: String,
    onClick: () -> Unit,
    primary: Boolean = false,
    modifier: Modifier = Modifier
) {
    var focused by remember { mutableStateOf(false) }
    val bg = when {
        focused -> Color.White
        primary -> Brand
        else -> Surface2
    }
    val fg = when {
        focused -> Color(0xFF0B0C10)
        else -> Text1
    }
    Box(
        modifier = modifier
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onClick)
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .padding(horizontal = 22.dp, vertical = 12.dp)
    ) {
        Text(text, color = fg, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** Simple vertical fade scrim used behind overlays. */
@Composable
fun BottomScrim(modifier: Modifier = Modifier, height: Int = 220) {
    Box(
        modifier
            .fillMaxWidth()
            .height(height.dp)
            .background(
                Brush.verticalGradient(
                    listOf(Color.Transparent, Color(0xE6000000))
                )
            )
    )
}

/** Utility: two-line metadata summary for a title. */
fun metaLine(item: MediaItem): String {
    val parts = mutableListOf<String>()
    item.normalizedImdb?.let { parts.add("IMDb %.1f".format(it)) }
    item.year?.let { parts.add(it.toString()) }
    item.durationMinutes?.let { parts.add("$it دقیقه") }
    item.quality?.let { parts.add(it) }
    if (item.genres.isNotEmpty()) parts.add(item.genres.take(2).joinToString("، "))
    return parts.joinToString(" · ")
}
