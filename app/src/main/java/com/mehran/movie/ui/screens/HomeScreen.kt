package com.mehran.movie.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mehran.movie.data.CatalogRepository
import com.mehran.movie.data.HomeResponse
import com.mehran.movie.data.MediaItem
import com.mehran.movie.ui.UiState
import com.mehran.movie.ui.components.BottomScrim
import com.mehran.movie.ui.components.ErrorBox
import com.mehran.movie.ui.components.FocusButton
import com.mehran.movie.ui.components.LoadingBox
import com.mehran.movie.ui.components.MediaRow
import com.mehran.movie.ui.components.metaLine
import com.mehran.movie.ui.theme.Bg0
import com.mehran.movie.ui.theme.Text1
import com.mehran.movie.ui.theme.Text2

@Composable
fun HomeScreen(
    catalog: CatalogRepository,
    isLoggedIn: Boolean,
    onOpenDetails: (MediaItem) -> Unit,
    onPlay: (MediaItem) -> Unit,
    onBrowse: (type: String, title: String) -> Unit,
    onSearch: () -> Unit,
    onLogin: () -> Unit
) {
    var state by remember { mutableStateOf<UiState<com.mehran.movie.data.HomeResponse>>(UiState.Loading) }
    var reloadKey by remember { mutableStateOf(0) }

    LaunchedEffect(reloadKey) {
        state = UiState.Loading
        state = try {
            UiState.Ready(catalog.home())
        } catch (e: Exception) {
            UiState.Error(e.message ?: "خطا در دریافت داده")
        }
    }

    when (val s = state) {
        is UiState.Loading -> LoadingBox()
        is UiState.Error -> ErrorBox(s.message, onRetry = { reloadKey++ })
        is UiState.Ready -> {
            val home = (s as UiState.Ready<HomeResponse>).data
            val rows: List<Pair<String, List<MediaItem>>> = buildList {
                home.sections.forEach { sec -> if (sec.items.isNotEmpty()) add(sec.persianName to sec.items) }
                home.latestUpdates?.groups?.forEach { g -> if (g.items.isNotEmpty()) add(g.persianName to g.items) }
            }
            val hero = home.sections.firstOrNull()?.items?.firstOrNull()
                ?: home.latestUpdates?.groups?.firstOrNull()?.items?.firstOrNull()

            LazyColumn(modifier = Modifier.fillMaxSize().background(Bg0)) {
                item {
                    HeroBanner(
                        item = hero,
                        isLoggedIn = isLoggedIn,
                        onPlay = { hero?.let(onPlay) },
                        onDetails = { hero?.let(onOpenDetails) },
                        onSearch = onSearch,
                        onLogin = onLogin
                    )
                }
                items(rows) { row ->
                    MediaRow(title = row.first, items = row.second, onClick = onOpenDetails)
                }
                item { Spacer(Modifier.height(40.dp)) }
            }
        }
    }
}

@Composable
private fun HeroBanner(
    item: MediaItem?,
    isLoggedIn: Boolean,
    onPlay: () -> Unit,
    onDetails: () -> Unit,
    onSearch: () -> Unit,
    onLogin: () -> Unit
) {
    Box(Modifier.fillMaxWidth().height(440.dp)) {
        if (item?.posterUrl != null) {
            AsyncImage(
                model = item.posterUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color(0xCC000000),
                        0.5f to Color(0x66000000),
                        1f to Bg0
                    )
                )
        )
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .padding(48.dp)
                .widthIn(max = 720.dp)
        ) {
            Text(
                text = "مهران مووی",
                color = Color(0xFFF0454A),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = item?.displayTitle ?: "به مهران مووی خوش آمدید",
                color = Text1,
                fontSize = 40.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (item != null) {
                Spacer(Modifier.height(8.dp))
                Text(metaLine(item), color = Text2, fontSize = 15.sp)
                item.plot?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = Text2, fontSize = 15.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                }
            }
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                if (item != null) {
                    FocusButton("پخش", onClick = onPlay, primary = true)
                    FocusButton("جزئیات", onClick = onDetails)
                }
                FocusButton("جستجو", onClick = onSearch)
                if (!isLoggedIn) FocusButton("ورود", onClick = onLogin)
            }
        }
    }
}
