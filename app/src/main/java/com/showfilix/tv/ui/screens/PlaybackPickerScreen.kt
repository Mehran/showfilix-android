package com.showfilix.tv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.showfilix.tv.data.CatalogRepository
import com.showfilix.tv.data.LinksResponse
import com.showfilix.tv.data.QualityOption
import com.showfilix.tv.data.toPlaybackPlan
import com.showfilix.tv.ui.UiState
import com.showfilix.tv.ui.components.ErrorBox
import com.showfilix.tv.ui.components.FocusButton
import com.showfilix.tv.ui.components.LoadingBox
import com.showfilix.tv.ui.theme.Bg0
import com.showfilix.tv.ui.theme.Brand
import com.showfilix.tv.ui.theme.Surface2
import com.showfilix.tv.ui.theme.Text1
import com.showfilix.tv.ui.theme.Text2
import kotlinx.coroutines.launch

@Composable
fun PlaybackPickerScreen(
    catalog: CatalogRepository,
    type: String,
    externalId: String,
    siteKey: String?,
    title: String,
    isLoggedIn: Boolean,
    onPlay: (url: String, title: String, subtitle: String?) -> Unit,
    onLogin: () -> Unit
) {
    var state by remember { mutableStateOf<UiState<LinksResponse>>(UiState.Loading) }
    var reloadKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(externalId, reloadKey) {
        state = UiState.Loading
        state = try {
            UiState.Ready(catalog.links(type, externalId, siteKey))
        } catch (e: Exception) {
            UiState.Error(e.message ?: "خطا در دریافت لینک‌های پخش")
        }
    }

    Box(Modifier.fillMaxSize().background(Bg0)) {
        when (val s = state) {
            is UiState.Loading -> LoadingBox()
            is UiState.Error -> ErrorBox(s.message, onRetry = { reloadKey++ })
            is UiState.Ready -> PlanBody(
                links = s.data,
                title = title,
                isLoggedIn = isLoggedIn,
                streamProxy = catalog::streamProxy,
                onPlay = onPlay,
                onLogin = onLogin,
                onReload = { reloadKey++ }
            )
        }
    }
}

@Composable
private fun PlanBody(
    links: LinksResponse,
    title: String,
    isLoggedIn: Boolean,
    streamProxy: (String) -> String,
    onPlay: (url: String, title: String, subtitle: String?) -> Unit,
    onLogin: () -> Unit,
    onReload: () -> Unit
) {
    val plan = remember(links) { links.toPlaybackPlan() }
    val hasAccess = links.access?.hasAccess == true

    var version by remember(links) { mutableIntStateOf(0) }
    var season by remember(links) { mutableIntStateOf(0) }
    var episode by remember(links) { mutableIntStateOf(0) }

    LaunchedEffect(version) { season = 0; episode = 0 }
    LaunchedEffect(season) { episode = 0 }

    Column(Modifier.fillMaxSize().padding(top = 28.dp)) {
        Text(
            "پخش",
            color = Brand, fontSize = 14.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 48.dp)
        )
        Text(
            title,
            color = Text1, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 48.dp, top = 2.dp, bottom = 14.dp)
        )

        if (plan.isEmpty) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 48.dp)) {
                Text(
                    text = when {
                        !hasAccess && !isLoggedIn -> "برای پخش باید وارد حساب کاربری شوید."
                        !hasAccess -> "برای پخش این عنوان به اشتراک فعال نیاز دارید."
                        else -> "لینک پخشی برای این عنوان در دسترس نیست."
                    },
                    color = Text2, fontSize = 17.sp
                )
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (!isLoggedIn) FocusButton("ورود", primary = true, onClick = onLogin)
                    FocusButton("تلاش دوباره", onClick = onReload)
                }
            }
            return@Column
        }

        val versions = plan.versions

        if (versions.size > 1) {
            SelectorRow(
                label = "نسخه",
                items = versions.map { it.title },
                selectedIndex = version,
                onFocusIndex = { version = it },
                onClickIndex = { version = it }
            )
        }

        if (plan.isSeries) {
            val seasons = versions.getOrNull(version)?.seasons.orEmpty()
            if (seasons.size > 1) {
                SelectorRow(
                    label = "فصل",
                    items = seasons.map { it.title ?: "فصل ${it.number}" },
                    selectedIndex = season,
                    onFocusIndex = { season = it },
                    onClickIndex = { season = it }
                )
            }
            val episodes = seasons.getOrNull(season)?.episodes.orEmpty()
            SelectorRow(
                label = "قسمت",
                items = episodes.map { ep ->
                    val base = "قسمت ${ep.episode}"
                    if (ep.title.isNullOrBlank() || ep.title == base) base else "$base · ${ep.title}"
                },
                selectedIndex = episode,
                onFocusIndex = { episode = it },
                onClickIndex = { episode = it }
            )
        }

        val qualities = if (plan.isSeries) {
            versions.getOrNull(version)?.seasons?.getOrNull(season)
                ?.episodes?.getOrNull(episode)?.qualities.orEmpty()
        } else {
            plan.movieQualities
        }

        SelectorRow(
            label = "کیفیت",
            items = qualities,
            selectedIndex = -1,
            labelOf = { it.displayLabel },
            hint = "با OK پخش می‌شود",
            onFocusIndex = { },
            onClickIndex = { i ->
                qualities.getOrNull(i)?.let { q ->
                    val sub = buildSubtitle(plan.isSeries, versions, version, season, episode, q)
                    onPlay(streamProxy(q.url), title, sub)
                }
            }
        )

        Spacer(Modifier.height(10.dp))
        Text(
            "نسخه، فصل، قسمت و کیفیت را انتخاب کنید.",
            color = Text2, fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 48.dp)
        )
    }
}

private fun buildSubtitle(
    isSeries: Boolean,
    versions: List<com.showfilix.tv.data.VersionOption>,
    version: Int,
    season: Int,
    episode: Int,
    quality: QualityOption
): String {
    val parts = mutableListOf<String>()
    if (isSeries) {
        versions.getOrNull(version)?.title?.let { parts.add(it) }
        val ep = versions.getOrNull(version)?.seasons?.getOrNull(season)?.episodes?.getOrNull(episode)
        if (ep != null) parts.add("فصل ${ep.season} · قسمت ${ep.episode}")
        parts.add(quality.label)
    } else {
        parts.add(quality.label)
    }
    return parts.joinToString(" · ")
}

/** A horizontal, focusable row of options that keeps the focused item in view. */
@Composable
private fun <T> SelectorRow(
    label: String,
    items: List<T>,
    selectedIndex: Int,
    labelOf: (T) -> String = { it.toString() },
    hint: String? = null,
    onFocusIndex: (Int) -> Unit,
    onClickIndex: (Int) -> Unit
) {
    if (items.isEmpty()) return
    val state = rememberLazyListState()
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxWidth().padding(bottom = 14.dp)) {
        Row(
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            modifier = Modifier.padding(start = 48.dp, bottom = 6.dp)
        ) {
            Text(label, color = Text2, fontSize = 14.sp)
            hint?.let {
                Text(" · $it", color = Text2, fontSize = 12.sp)
            }
        }
        LazyRow(
            state = state,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(horizontal = 48.dp)
        ) {
            itemsIndexed(items) { index, item ->
                OptionPill(
                    text = labelOf(item),
                    selected = index == selectedIndex,
                    onClick = {
                        onFocusIndex(index)
                        onClickIndex(index)
                    },
                    onFocus = {
                        onFocusIndex(index)
                        scope.launch {
                            state.animateScrollToItem((index - 1).coerceAtLeast(0))
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun OptionPill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    onFocus: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .widthIn(max = 360.dp)
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) onFocus()
            }
            .clickable(onClick = onClick)
            .background(
                color = when {
                    focused -> Color.White
                    selected -> Brand
                    else -> Surface2
                },
                shape = RoundedCornerShape(999.dp)
            )
            .padding(horizontal = 20.dp, vertical = 11.dp)
    ) {
        Text(
            text = text,
            color = if (focused) Color(0xFF0B0C10) else Text1,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
