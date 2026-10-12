package com.showfilix.tv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.showfilix.tv.data.CatalogRepository
import com.showfilix.tv.data.MediaItem
import com.showfilix.tv.ui.components.ErrorBox
import com.showfilix.tv.ui.components.FocusButton
import com.showfilix.tv.ui.components.LoadingBox
import com.showfilix.tv.ui.components.PosterCard
import com.showfilix.tv.ui.theme.Bg0
import com.showfilix.tv.ui.theme.Text1
import com.showfilix.tv.ui.theme.Text2

private val SORTS = listOf(
    "3" to "جدیدترین",
    "1" to "محبوب‌ترین",
    "2" to "برترین"
)

@Composable
fun BrowseScreen(
    catalog: CatalogRepository,
    type: String,
    title: String,
    onOpenDetails: (MediaItem) -> Unit
) {
    val items = remember { mutableStateListOf<MediaItem>() }
    var page by remember { mutableStateOf(1) }
    var sortBy by remember { mutableStateOf("3") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var totalPages by remember { mutableStateOf(1) }
    val gridState = rememberLazyGridState()

    suspend fun load(reset: Boolean) {
        if (loading) return
        loading = true
        error = null
        try {
            val p = if (reset) 1 else page
            val res = catalog.browse(type = type, page = p, sortBy = sortBy)
            if (reset) items.clear()
            items.addAll(res.items)
            totalPages = res.meta?.totalPages ?: 1
            page = p + 1
        } catch (e: Exception) {
            error = e.message ?: "خطا"
        } finally {
            loading = false
        }
    }

    LaunchedEffect(type, sortBy) { load(reset = true) }

    // Load the next page when focus approaches the end of the grid.
    LaunchedEffect(gridState, type) {
        snapshotFlow {
            val last = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            last to items.size
        }.collect { (last, size) ->
            if (size > 0 && last >= size - 8 && page <= totalPages && !loading) {
                load(reset = false)
            }
        }
    }

    androidx.compose.foundation.layout.Column(
        Modifier.fillMaxSize().background(Bg0)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 48.dp, end = 48.dp, top = 28.dp, bottom = 12.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Text(title, color = Text1, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.padding(start = 24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SORTS.forEach { (value, label) ->
                    FocusButton(
                        text = label,
                        primary = sortBy == value,
                        onClick = { if (sortBy != value) sortBy = value }
                    )
                }
            }
        }

        when {
            items.isEmpty() && loading -> LoadingBox()
            items.isEmpty() && error != null -> ErrorBox(error!!, onRetry = { })
            else -> LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Fixed(6),
                contentPadding = PaddingValues(horizontal = 44.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                verticalArrangement = Arrangement.spacedBy(26.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(items.size) { index ->
                    val item = items[index]
                    PosterCard(
                        item = item,
                        width = 160,
                        height = 240,
                        showTitle = false,
                        onClick = { onOpenDetails(item) }
                    )
                }
                if (loading) {
                    item {
                        Text("در حال بارگذاری…", color = Text2, fontSize = 14.sp, modifier = Modifier.padding(16.dp))
                    }
                }
            }
        }
    }
}
