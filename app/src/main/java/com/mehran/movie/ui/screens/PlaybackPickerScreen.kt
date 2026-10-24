package com.mehran.movie.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mehran.movie.data.ApiException
import com.mehran.movie.data.CatalogRepository
import com.mehran.movie.data.LinksResponse
import com.mehran.movie.data.MediaItem
import com.mehran.movie.data.Person
import com.mehran.movie.data.QualityOption
import com.mehran.movie.data.VersionOption
import com.mehran.movie.data.toPlaybackPlan
import com.mehran.movie.ui.UiState
import com.mehran.movie.ui.components.ErrorBox
import com.mehran.movie.ui.components.FocusButton
import com.mehran.movie.ui.components.LoadingBox
import com.mehran.movie.ui.components.metaLine
import com.mehran.movie.ui.theme.Bg0
import com.mehran.movie.ui.theme.Brand
import com.mehran.movie.ui.theme.Surface1
import com.mehran.movie.ui.theme.Surface2
import com.mehran.movie.ui.theme.Text1
import com.mehran.movie.ui.theme.Text2
import kotlinx.coroutines.launch

@Composable
fun PlaybackPickerScreen(
    catalog: CatalogRepository,
    media: MediaItem,
    isLoggedIn: Boolean,
    onPlay: (url: String, title: String, subtitle: String?) -> Unit,
    onLogin: () -> Unit
) {
    var state by remember { mutableStateOf<UiState<LinksResponse>>(UiState.Loading) }
    var reloadKey by remember { mutableIntStateOf(0) }

    // isLoggedIn is a key so returning here after signing in re-fetches the
    // links instead of leaving the viewer on the sign-in prompt.
    LaunchedEffect(media.externalId, reloadKey, isLoggedIn) {
        state = UiState.Loading
        state = try {
            UiState.Ready(catalog.links(media.type ?: "movie", media.externalId ?: "", media.siteKey))
        } catch (e: ApiException) {
            // A rejected session is not a paywall. Say so, and keep the login
            // action available instead of showing "subscription required".
            if (e.status == 401 || e.status == 403) {
                UiState.Error(
                    if (isLoggedIn) "نشست شما منقضی شده است؛ لطفاً دوباره وارد شوید."
                    else "برای پخش باید وارد حساب کاربری شوید."
                )
            } else {
                UiState.Error(e.message ?: "خطا در دریافت لینک‌های پخش")
            }
        } catch (e: Exception) {
            UiState.Error(e.message ?: "خطا در دریافت لینک‌های پخش")
        }
    }

    Box(Modifier.fillMaxSize().background(Bg0)) {
        // Ambient backdrop built from the title's artwork.
        media.posterUrl?.let { poster ->
            AsyncImage(
                model = poster,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = 0.32f }
            )
        }
        Box(
            Modifier.fillMaxSize().background(
                Brush.horizontalGradient(
                    listOf(Color(0xFA0B0B0F), Color(0xE60B0B0F), Color(0xC40B0B0F))
                )
            )
        )

        when (val s = state) {
            is UiState.Loading -> LoadingBox()
            is UiState.Error -> ErrorBox(
                message = s.message,
                onRetry = { reloadKey++ },
                onLogin = onLogin
            )
            is UiState.Ready -> Row(Modifier.fillMaxSize()) {
                InfoPanel(media)
                Box(Modifier.weight(1f).fillMaxHeight()) {
                    Selectors(
                        links = s.data,
                        media = media,
                        isLoggedIn = isLoggedIn,
                        streamProxy = catalog::streamProxy,
                        onPlay = onPlay,
                        onLogin = onLogin,
                        onReload = { reloadKey++ }
                    )
                }
            }
        }
    }
}

/* --------------------------------- left ---------------------------------- */

@Composable
private fun InfoPanel(media: MediaItem) {
    Column(
        Modifier
            .width(430.dp)
            .fillMaxHeight()
            .padding(start = 48.dp, top = 36.dp, end = 28.dp)
    ) {
        media.posterUrl?.let { poster ->
            AsyncImage(
                model = poster,
                contentDescription = media.displayTitle,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(164.dp)
                    .height(246.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Surface1)
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            media.displayTitle,
            color = Text1,
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        media.titleEn?.takeIf { it.isNotBlank() }?.let {
            Spacer(Modifier.height(4.dp))
            Text(it, color = Text2, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.height(8.dp))
        Text(metaLine(media), color = Text2, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)

        // Prefer cast that actually has a photo: the server returns directors
        // with a null avatarUrl ahead of the actors, which looked blank.
        val cast = media.people.filter { it.name.isNotBlank() }
        val people = (cast.filter { it.avatarUrl != null }.ifEmpty { cast }).take(6)
        if (people.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text("بازیگران", color = Text2, fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
            PeopleRow(people)
        }

        media.plot?.takeIf { it.isNotBlank() }?.let {
            Spacer(Modifier.height(16.dp))
            Text(it, color = Text2, fontSize = 14.sp, maxLines = 5, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun PeopleRow(people: List<Person>) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        people.forEach { person ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(58.dp)
            ) {
                if (person.avatarUrl != null) {
                    AsyncImage(
                        model = person.avatarUrl,
                        contentDescription = person.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Surface2)
                    )
                } else {
                    Box(
                        Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Surface2),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(person.name.take(1), color = Text2, fontSize = 18.sp)
                    }
                }
                Spacer(Modifier.height(5.dp))
                Text(
                    person.name,
                    color = Text2,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/* -------------------------------- right ---------------------------------- */

@Composable
private fun Selectors(
    links: LinksResponse,
    media: MediaItem,
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

    Column(Modifier.fillMaxSize().padding(top = 36.dp, end = 48.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("پخش", color = Brand, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(
                if (media.isSeries) "  ·  قسمت و کیفیت را انتخاب کنید" else "  ·  کیفیت را انتخاب کنید",
                color = Text2, fontSize = 13.sp
            )
        }
        Spacer(Modifier.height(14.dp))

        // No access — signed out, dead session or no plan. Always offer
        // sign-in: when the server has dropped the session the screen looks
        // exactly like a paywall, and only a fresh login can tell them apart.
        if (!hasAccess) {
            Text(
                text = if (isLoggedIn) "برای پخش این عنوان به اشتراک فعال نیاز دارید."
                else "برای پخش باید وارد حساب کاربری شوید.",
                color = Text2, fontSize = 17.sp
            )
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FocusButton(
                    text = if (isLoggedIn) "ورود مجدد" else "ورود",
                    primary = true,
                    onClick = onLogin
                )
                FocusButton("تلاش دوباره", onClick = onReload)
            }
            Spacer(Modifier.height(18.dp))
        }

        if (plan.isEmpty) {
            if (hasAccess) {
                Text("لینک پخشی برای این عنوان در دسترس نیست.", color = Text2, fontSize = 17.sp)
            }
            return@Column
        }

        val versions = plan.versions

        if (versions.size > 1) {
            PillRow(
                label = "نسخه",
                items = versions,
                selectedIndex = version,
                labelOf = { it.title },
                onFocusIndex = { version = it },
                onClickIndex = { version = it }
            )
        }

        if (plan.isSeries) {
            val seasons = versions.getOrNull(version)?.seasons.orEmpty()
            if (seasons.size > 1) {
                PillRow(
                    label = "فصل",
                    items = seasons,
                    selectedIndex = season,
                    labelOf = { it.title ?: "فصل ${it.number}" },
                    onFocusIndex = { season = it },
                    onClickIndex = { season = it }
                )
            }
            val episodes = seasons.getOrNull(season)?.episodes.orEmpty()
            CardRow(
                label = "قسمت",
                items = episodes,
                selectedIndex = episode,
                titleOf = { "قسمت ${it.episode}" },
                subtitleOf = { it.title?.takeIf { t -> t.isNotBlank() && t != "قسمت ${it.episode}" } },
                badgeOf = { "کیفیت: ${it.qualities.size}" },
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

        CardRow(
            label = "کیفیت",
            items = qualities,
            selectedIndex = -1,
            titleOf = { it.label },
            subtitleOf = { it.size },
            badgeOf = { null },
            emptyHint = "کیفیتی برای این انتخاب موجود نیست",
            onFocusIndex = { },
            onClickIndex = { index ->
                qualities.getOrNull(index)?.let { quality ->
                    val subtitle = buildSubtitle(plan.versions, version, season, episode, quality, plan.isSeries)
                    onPlay(streamProxy(quality.url), media.displayTitle, subtitle)
                }
            }
        )
    }
}

private fun buildSubtitle(
    versions: List<VersionOption>,
    version: Int,
    season: Int,
    episode: Int,
    quality: QualityOption,
    isSeries: Boolean
): String {
    val parts = mutableListOf<String>()
    if (isSeries) {
        versions.getOrNull(version)?.title?.let { parts.add(it) }
        versions.getOrNull(version)?.seasons?.getOrNull(season)?.episodes?.getOrNull(episode)?.let {
            parts.add("فصل ${it.season} · قسمت ${it.episode}")
        }
    }
    parts.add(quality.label)
    quality.size?.let { parts.add(it) }
    return parts.joinToString(" · ")
}

/* ------------------------------- row widgets ------------------------------ */

@Composable
private fun <T> PillRow(
    label: String,
    items: List<T>,
    selectedIndex: Int,
    labelOf: (T) -> String,
    onFocusIndex: (Int) -> Unit,
    onClickIndex: (Int) -> Unit
) {
    if (items.isEmpty()) return
    SectionHeader(label)
    AutoScrollLazyRow(items, onFocusIndex) { index, item, onFocused ->
        OptionPill(
            text = labelOf(item),
            selected = index == selectedIndex,
            onFocused = onFocused,
            onClick = {
                onFocusIndex(index)
                onClickIndex(index)
            }
        )
    }
}

@Composable
private fun <T> CardRow(
    label: String,
    items: List<T>,
    selectedIndex: Int,
    titleOf: (T) -> String,
    subtitleOf: (T) -> String?,
    badgeOf: (T) -> String?,
    emptyHint: String? = null,
    onFocusIndex: (Int) -> Unit,
    onClickIndex: (Int) -> Unit
) {
    SectionHeader(label)
    if (items.isEmpty()) {
        emptyHint?.let {
            Text(it, color = Text2, fontSize = 13.sp, modifier = Modifier.padding(bottom = 14.dp))
        }
        return
    }
    AutoScrollLazyRow(items, onFocusIndex) { index, item, onFocused ->
        OptionCard(
            title = titleOf(item),
            subtitle = subtitleOf(item),
            badge = badgeOf(item),
            selected = index == selectedIndex,
            onFocused = onFocused,
            onClick = {
                onFocusIndex(index)
                onClickIndex(index)
            }
        )
    }
}

@Composable
private fun SectionHeader(label: String) {
    Text(
        label,
        color = Text2,
        fontSize = 13.sp,
        modifier = Modifier.padding(bottom = 7.dp)
    )
}

/**
 * A horizontal row shared by the pills and cards. Each option gets an
 * [onFocused] callback; using it scrolls the focused option into view.
 */
@Composable
private fun <T> AutoScrollLazyRow(
    items: List<T>,
    onFocusIndex: (Int) -> Unit,
    content: @Composable (index: Int, item: T, onFocused: () -> Unit) -> Unit
) {
    val state = rememberLazyListState()
    val scope = rememberCoroutineScope()
    LazyRow(
        state = state,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(end = 8.dp),
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
    ) {
        itemsIndexed(items) { index, item ->
            val onFocused: () -> Unit = {
                onFocusIndex(index)
                scope.launch { state.animateScrollToItem((index - 1).coerceAtLeast(0)) }
            }
            content(index, item, onFocused)
        }
    }
}

@Composable
private fun OptionPill(
    text: String,
    selected: Boolean,
    onFocused: () -> Unit,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .widthIn(max = 320.dp)
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) onFocused()
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
            text,
            color = if (focused) Color(0xFF0B0C10) else Text1,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun OptionCard(
    title: String,
    subtitle: String?,
    badge: String?,
    selected: Boolean,
    onFocused: () -> Unit,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .width(196.dp)
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) onFocused()
            }
            .clickable(onClick = onClick)
            .background(
                color = if (focused) Color.White else Surface2,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            title,
            color = if (focused) Color(0xFF0B0C10) else Text1,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        subtitle?.takeIf { it.isNotBlank() }?.let {
            Spacer(Modifier.height(3.dp))
            Text(
                it,
                color = if (focused) Color(0xFF44464D) else Text2,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        badge?.let {
            Spacer(Modifier.height(5.dp))
            Text(
                it,
                color = if (focused) Color(0xFF44464D) else Brand,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
