package com.showfilix.tv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.showfilix.tv.auth.AuthRepository
import com.showfilix.tv.ui.components.FocusButton
import com.showfilix.tv.ui.theme.Bg0
import com.showfilix.tv.ui.theme.Brand
import com.showfilix.tv.ui.theme.Surface2
import com.showfilix.tv.ui.theme.Text1
import com.showfilix.tv.ui.theme.Text2
import kotlinx.coroutines.launch

private enum class Field { EMAIL, PASSWORD }

private val KEY_ROWS = listOf(
    "qwertyuiop".toList(),
    "asdfghjkl@".toList(),
    "zxcvbnm.-_".toList(),
    "1234567890".toList()
)

@Composable
fun LoginScreen(
    auth: AuthRepository,
    onLoggedIn: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var active by remember { mutableStateOf(Field.EMAIL) }
    var shift by remember { mutableStateOf(false) }
    var capsLock by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current

    val upper = shift || capsLock

    fun currentValue(): String = if (active == Field.EMAIL) email else password

    fun setCurrentValue(v: String) {
        if (active == Field.EMAIL) email = v else password = v
    }

    fun type(ch: String) {
        val c = if (upper && ch.length == 1 && ch[0].isLetter()) ch.uppercase() else ch
        setCurrentValue(currentValue() + c)
        // one-shot shift; caps lock stays on
        if (shift && !capsLock) shift = false
    }

    fun backspace() {
        setCurrentValue(currentValue().dropLast(1))
    }

    /** Shift behaviour: off -> one-shot -> caps lock -> off. */
    fun onShift() {
        when {
            capsLock -> { capsLock = false; shift = false }
            shift -> capsLock = true
            else -> shift = true
        }
        notice = when {
            capsLock -> "حروف بزرگ قفل شد"
            shift -> "حروف بزرگ (یک حرف)"
            else -> null
        }
    }

    fun paste() {
        val clip = clipboard.getText()?.text
        if (clip.isNullOrEmpty()) {
            notice = "چیزی برای چسباندن در حافظه نیست"
            return
        }
        // strip line breaks/spaces so an e-mail stays a single token
        val cleaned = clip.replace(Regex("\\s+"), "").ifEmpty { clip.trim() }
        setCurrentValue(currentValue() + cleaned)
        notice = "چسبانده شد"
    }

    fun copy() {
        val value = currentValue()
        if (value.isEmpty()) {
            notice = "کادر خالی است"
        } else {
            clipboard.setText(AnnotatedString(value))
            notice = "کپی شد"
        }
    }

    fun submit() {
        if (loading) return
        if (!email.contains("@")) {
            status = "ایمیل معتبر وارد کنید"
            active = Field.EMAIL
            return
        }
        if (password.length < 8) {
            status = "رمز عبور باید حداقل ۸ کاراکتر باشد"
            active = Field.PASSWORD
            return
        }
        scope.launch {
            loading = true
            status = null
            notice = null
            try {
                auth.login(email, password)
                onLoggedIn()
            } catch (e: Exception) {
                status = e.message ?: "ورود ناموفق بود"
            } finally {
                loading = false
            }
        }
    }

    Column(
        Modifier.fillMaxSize().background(Bg0).padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("شوفیلیکس", color = Brand, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(4.dp))
        Text("با ایمیل و رمز عبور وارد شوید", color = Text2, fontSize = 16.sp)
        Spacer(Modifier.height(16.dp))

        FieldRow(
            label = "ایمیل",
            value = email,
            placeholder = "you@example.com",
            isActive = active == Field.EMAIL,
            mask = false,
            onFocus = { active = Field.EMAIL }
        )
        Spacer(Modifier.height(10.dp))
        FieldRow(
            label = "رمز عبور",
            value = password,
            placeholder = "••••••••",
            isActive = active == Field.PASSWORD,
            mask = true,
            onFocus = { active = Field.PASSWORD }
        )

        status?.let {
            Spacer(Modifier.height(10.dp))
            Text(it, color = Color(0xFFFFB4AB), fontSize = 15.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        notice?.let {
            Spacer(Modifier.height(10.dp))
            Text(it, color = Text2, fontSize = 14.sp)
        }

        Spacer(Modifier.height(16.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            KEY_ROWS.forEach { rowKeys ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    rowKeys.forEach { ch ->
                        val label = if (upper && ch.isLetter()) ch.uppercase() else ch.toString()
                        KeyButton(label) { type(ch.toString()) }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
            // function row 1: shift + editing
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                KeyButton(
                    label = "⇧",
                    wide = true,
                    selected = upper,
                    onClick = { onShift() }
                )
                KeyButton("فاصله", wide = true) { type(" ") }
                KeyButton("⌫", wide = true) { backspace() }
                KeyButton("پاک کردن", wide = true) { setCurrentValue("") }
            }
            Spacer(Modifier.height(8.dp))
            // function row 2: clipboard + submit
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                KeyButton("چسباندن", wide = true) { paste() }
                KeyButton("کپی", wide = true) { copy() }
                FocusButton(
                    text = if (loading) "در حال ورود…" else "ورود",
                    primary = true,
                    onClick = { submit() }
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        Text(
            "⇧ حروف بزرگ · چسباندن/کپی از حافظه دستگاه · جابه‌جایی بین کادرها مقصد تایپ را تعیین می‌کند.",
            color = Text2, fontSize = 12.sp
        )
    }
}

@Composable
private fun FieldRow(
    label: String,
    value: String,
    placeholder: String,
    isActive: Boolean,
    mask: Boolean,
    onFocus: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val shown = if (mask) "•".repeat(value.length) else value
    Row(
        modifier = Modifier
            .width(760.dp)
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) onFocus()
            }
            .clickable { onFocus() }
            .background(Surface2, RoundedCornerShape(10.dp))
            .border(
                2.dp,
                when {
                    focused -> Color.White
                    isActive -> Brand
                    else -> Color.Transparent
                },
                RoundedCornerShape(10.dp)
            )
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Text2, fontSize = 16.sp, modifier = Modifier.width(110.dp))
        Text(
            text = shown.ifEmpty { placeholder },
            color = if (shown.isEmpty()) Text2 else Text1,
            fontSize = 19.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun KeyButton(
    label: String,
    wide: Boolean = false,
    selected: Boolean = false,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val bg = when {
        focused -> Color.White
        selected -> Brand
        else -> Surface2
    }
    Box(
        modifier = Modifier
            .width(if (wide) 112.dp else 58.dp)
            .height(48.dp)
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onClick)
            .background(bg, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (focused) Color(0xFF0B0C10) else Text1,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
