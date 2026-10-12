package com.showfilix.tv.ui.screens

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
import com.showfilix.tv.data.CatalogRepository
import com.showfilix.tv.data.MediaItem
import com.showfilix.tv.ui.UiState
import com.showfilix.tv.ui.components.ErrorBox
import com.showfilix.tv.ui.components.FocusButton
import com.showfilix.tv.ui.components.LoadingBox
import com.showfilix.tv.ui.components.MediaRow
import com.showfilix.tv.ui.components.metaLine
import com.showfilix.tv.ui.theme.Bg0
import com.showfilix.tv.ui.theme.Text1
import com.showfilix.tv.ui.theme.Text2

@Composable
fun DetailsScreen(
    catalog: CatalogRepository,
    type: String,
    externalId: String,
    isLoggedIn: Boolean,
    onPickPlayback: (MediaItem) -> Unit,
    onOpenDetails: (MediaItem) -> Unit,
    onLogin: () -> Unit
) {
    var state by remember { mutableStateOf<UiState<MediaItem>>(UiState.Loading) }
    var similar by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var reloadKey by remember { mutableStateOf(0) }

    LaunchedEffect(externalId, reloadKey) {
        state = UiState.Loading
        state = try {
            UiState.Ready(catalog.detail(type, externalId))
        } catch (e: Exception) {
            UiState.Error(e.message ?: "خطا در دریافت اطلاعات")
        }
        similar = runCatching { catalog.similar(type, externalId).items }.getOrDefault(emptyList())
    }

    when (val s = state) {
        is UiState.Loading -> LoadingBox()
        is UiState.Error -> ErrorBox(s.message, onRetry = { reloadKey++ })
        is UiState.Ready -> {
            val m = s.data
            Box(Modifier.fillMaxSize().background(Bg0)) {
                if (m.posterUrl != null) {
                    AsyncImage(
                        model = m.posterUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                }
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.horizontalGradient(
                            listOf(Color(0xF20B0B0F), Color(0xB30B0B0F), Color(0x660B0B0F))
                        )
                    )
                )
                LazyColumn(Modifier.fillMaxSize()) {
                    item {
                        Column(Modifier.padding(48.dp).widthIn(max = 820.dp)) {
                            Text(
                                text = if (m.isSeries) "سریال" else "فیلم",
                                color = Color(0xFFF0454A),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                m.displayTitle,
                                color = Text1,
                                fontSize = 42.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            m.titleEn?.takeIf { it.isNotBlank() }?.let {
                                Spacer(Modifier.height(4.dp))
                                Text(it, color = Text2, fontSize = 16.sp)
                            }
                            Spacer(Modifier.height(10.dp))
                            Text(metaLine(m), color = Text2, fontSize = 15.sp)
                            m.plot?.let {
                                Spacer(Modifier.height(12.dp))
                                Text(it, color = Text2, fontSize = 16.sp, maxLines = 5, overflow = TextOverflow.Ellipsis)
                            }
                            if (m.people.isNotEmpty()) {
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    "بازیگران: " + m.people.take(6).joinToString("، ") { it.name },
                                    color = Text2,
                                    fontSize = 14.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(Modifier.height(20.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                // Opens the season/episode/quality picker; it resolves
                                // the stream and reports access problems itself.
                                FocusButton("پخش", primary = true, onClick = { onPickPlayback(m) })
                                if (!isLoggedIn) {
                                    FocusButton("ورود", onClick = onLogin)
                                }
                            }
                        }
                    }
                    if (similar.isNotEmpty()) {
                        item {
                            MediaRow(
                                title = "مشابه این عنوان",
                                items = similar,
                                onClick = onOpenDetails
                            )
                        }
                    }
                    item { Spacer(Modifier.height(40.dp)) }
                }
            }
        }
    }
}
