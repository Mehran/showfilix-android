package com.mehran.movie.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mehran.movie.data.CatalogRepository
import com.mehran.movie.data.MediaItem
import com.mehran.movie.ui.components.PosterCard
import com.mehran.movie.ui.theme.Bg0
import com.mehran.movie.ui.theme.Brand
import com.mehran.movie.ui.theme.Surface2
import com.mehran.movie.ui.theme.Text1
import com.mehran.movie.ui.theme.Text2
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

    // Explicit searches (IME "Search", remote Enter, the on-screen key) bump
    // [submitTick] so the effect below re-runs even for an unchanged query and
    // skips the typing debounce.
    var submitTick by remember { mutableIntStateOf(0) }
    var submittedQuery by remember { mutableStateOf<String?>(null) }

    val keyboard = LocalSoftwareKeyboardController.current
    // The native keyboard's own height. When it is open the TV on-screen
    // keypad is redundant and would squeeze the results away, so hide it.
    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0

    /** Runs the search now: IME action, hardware Enter or the on-screen key. */
    fun submitSearch() {
        val q = query.trim()
        keyboard?.hide()
        if (q.length < 2) {
            results = emptyList()
            message = "حداقل ۲ حرف وارد کنید"
            return
        }
        message = null
        submittedQuery = q
        submitTick++
    }

    LaunchedEffect(query, submitTick) {
        val q = query.trim()
        if (q.length < 2) {
            results = emptyList()
            message = if (query.isNotEmpty()) "حداقل ۲ حرف وارد کنید" else null
            return@LaunchedEffect
        }
        // Typing is debounced; an explicit submit searches immediately.
        if (submittedQuery != q) delay(450)
        runCatching { catalog.search(q).items }
            .onSuccess {
                results = it
                message = if (it.isEmpty()) "نتیجه‌ای یافت نشد" else null
            }
            .onFailure { message = it.message }
    }

    // imePadding keeps the field/results above the soft keyboard on devices
    // that show one (NetBox with a USB/Bluetooth keyboard, touch TVs).
    Column(Modifier.fillMaxSize().background(Bg0).imePadding()) {
        Text(
            text = "جستجو",
            color = Text1,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 48.dp, top = 26.dp)
        )
        Spacer(Modifier.height(8.dp))

        // A real text field, so Android can offer its IME. ImeAction.Search
        // labels the IME's action key "Search" (magnifier) instead of the
        // default Enter/Go/"Login" style action, and onSearch below is what
        // actually executes the query.
        Row(
            Modifier
                .padding(horizontal = 48.dp)
                .fillMaxWidth()
                .background(Color(0xFF161922), RoundedCornerShape(10.dp))
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                textStyle = TextStyle(color = Text1, fontSize = 22.sp),
                cursorBrush = SolidColor(Brand),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(
                    // The IME action key ("Search") and Enter on a soft keyboard.
                    onSearch = { submitSearch() },
                    // Some TV IMEs report Go/Done for the same key.
                    onGo = { submitSearch() },
                    onDone = { submitSearch() }
                ),
                modifier = Modifier
                    .weight(1f)
                    .onPreviewKeyEvent { event ->
                        // A physical remote/keyboard Enter (or numpad Enter)
                        // must search too, not just the soft keyboard action.
                        val isEnter = event.key == Key.Enter || event.key == Key.NumPadEnter
                        if (isEnter && event.type == KeyEventType.KeyUp) {
                            submitSearch()
                            true
                        } else {
                            false
                        }
                    },
                decorationBox = { innerTextField ->
                    Box {
                        if (query.isEmpty()) {
                            Text("نام فیلم یا سریال…", color = Text2, fontSize = 22.sp)
                        }
                        innerTextField()
                    }
                }
            )
        }
        Spacer(Modifier.height(14.dp))

        if (!imeVisible) {
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
                    KeyButton("جستجو") { submitSearch() }
                }
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
