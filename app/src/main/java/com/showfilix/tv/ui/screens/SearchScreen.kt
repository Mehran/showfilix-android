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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.showfilix.tv.data.CatalogRepository
import com.showfilix.tv.data.MediaItem
import com.showfilix.tv.ui.components.PosterCard
import com.showfilix.tv.ui.theme.Bg0
import com.showfilix.tv.ui.theme.Surface2
import com.showfilix.tv.ui.theme.Text1
import com.showfilix.tv.ui.theme.Text2
import kotlinx.coroutines.delay

private val KEY_ROWS = listOf(
    "abcdefghij".toList(),
    "klmnopqrst".toList(),
    "uvwxyz0123".toList(),
    "456789".toList()
)

@Composable
fun SearchScreen(
    catalog: CatalogRepository,
    onOpenDetails: (MediaItem) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var message by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(query) {
        if (query.trim().length < 2) {
            results = emptyList()
            message = if (query.isNotEmpty()) "حداقل ۲ حرف وارد کنید" else null
            return@LaunchedEffect
        }
        delay(450)
        runCatching { catalog.search(query.trim()).items }
            .onSuccess {
                results = it
                message = if (it.isEmpty()) "نتیجه‌ای یافت نشد" else null
            }
            .onFailure { message = it.message }
    }

    Column(Modifier.fillMaxSize().background(Bg0)) {
        Text(
            text = "جستجو",
            color = Text1,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 48.dp, top = 26.dp)
        )
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier
                .padding(horizontal = 48.dp)
                .fillMaxWidth()
                .background(Color(0xFF161922), RoundedCornerShape(10.dp))
                .padding(horizontal = 18.dp, vertical = 14.dp)
        ) {
            Text(
                text = query.ifEmpty { "نام فیلم یا سریال…" },
                color = if (query.isEmpty()) Text2 else Text1,
                fontSize = 22.sp
            )
        }
        Spacer(Modifier.height(14.dp))

        Column(Modifier.padding(horizontal = 48.dp)) {
            KEY_ROWS.forEach { rowKeys ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    rowKeys.forEach { ch -> KeyButton(ch.toString()) { query += ch } }
                }
                Spacer(Modifier.height(8.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KeyButton("فاصله") { query += " " }
                KeyButton("⌫") { if (query.isNotEmpty()) query = query.dropLast(1) }
                KeyButton("پاک کردن") { query = "" }
            }
        }

        Spacer(Modifier.height(14.dp))
        message?.let {
            Text(it, color = Text2, fontSize = 15.sp, modifier = Modifier.padding(horizontal = 48.dp))
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(6),
            contentPadding = PaddingValues(horizontal = 44.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(results.size) { index ->
                val item = results[index]
                PosterCard(item = item, width = 150, height = 225, onClick = { onOpenDetails(item) })
            }
        }
    }
}

@Composable
private fun KeyButton(label: String, onKey: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .width(if (label.length > 1) 120.dp else 62.dp)
            .height(52.dp)
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onKey)
            .background(if (focused) Color.White else Surface2, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (focused) Color(0xFF0B0C10) else Text1,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
