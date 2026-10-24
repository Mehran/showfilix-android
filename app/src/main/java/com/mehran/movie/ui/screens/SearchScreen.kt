package com.mehran.movie.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
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
import com.mehran.movie.ui.theme.Text1
import com.mehran.movie.ui.theme.Text2
import kotlinx.coroutines.delay

@Composable
fun SearchScreen(
    catalog: CatalogRepository,
    onOpenDetails: (MediaItem) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var message by remember { mutableStateOf<String?>(null) }

    // Explicit searches (IME "Search", remote Enter) bump [submitTick] so the
    // effect below re-runs even for an unchanged query and skips the typing
    // debounce.
    var submitTick by remember { mutableIntStateOf(0) }
    var submittedQuery by remember { mutableStateOf<String?>(null) }

    val keyboard = LocalSoftwareKeyboardController.current

    /** Runs the search now: IME action or hardware Enter. */
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

    // No on-screen keypad: typing goes through the device's own Android
    // keyboard, same as the login screen. imePadding keeps the field and the
    // results visible while that keyboard is open.
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
        // default Enter/Go style action, and onSearch below is what actually
        // executes the query.
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
                    // D-pad / remote focus should raise the native keyboard,
                    // exactly like the login fields do.
                    .onFocusChanged { if (it.isFocused) keyboard?.show() }
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
