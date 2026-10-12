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
    var status by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun type(ch: String) {
        if (active == Field.EMAIL) email += ch else password += ch
    }

    fun backspace() {
        if (active == Field.EMAIL) email = email.dropLast(1) else password = password.dropLast(1)
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
        Modifier.fillMaxSize().background(Bg0).padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(8.dp))
        Text("شوفیلیکس", color = Brand, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(6.dp))
        Text("با ایمیل و رمز عبور وارد شوید", color = Text2, fontSize = 17.sp)
        Spacer(Modifier.height(20.dp))

        FieldRow(
            label = "ایمیل",
            value = email,
            placeholder = "you@example.com",
            isActive = active == Field.EMAIL,
            mask = false,
            onFocus = { active = Field.EMAIL }
        )
        Spacer(Modifier.height(12.dp))
        FieldRow(
            label = "رمز عبور",
            value = password,
            placeholder = "••••••••",
            isActive = active == Field.PASSWORD,
            mask = true,
            onFocus = { active = Field.PASSWORD }
        )

        status?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = Color(0xFFFFB4AB), fontSize = 16.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }

        Spacer(Modifier.height(20.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            KEY_ROWS.forEach { rowKeys ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    rowKeys.forEach { ch -> KeyButton(ch.toString()) { type(ch.toString()) } }
                }
                Spacer(Modifier.height(8.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                KeyButton("فاصله", wide = true) { type(" ") }
                KeyButton("⌫", wide = true) { backspace() }
                KeyButton("پاک کردن", wide = true) {
                    if (active == Field.EMAIL) email = "" else password = ""
                }
                FocusButton(
                    text = if (loading) "در حال ورود…" else "ورود",
                    primary = true,
                    onClick = { submit() }
                )
            }
        }

        Spacer(Modifier.height(14.dp))
        Text(
            "با جابه‌جایی بین دو کادر، مقصد تایپ را انتخاب کنید.",
            color = Text2, fontSize = 13.sp
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
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Text2, fontSize = 16.sp, modifier = Modifier.width(120.dp))
        Text(
            text = shown.ifEmpty { placeholder },
            color = if (shown.isEmpty()) Text2 else Text1,
            fontSize = 20.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun KeyButton(label: String, wide: Boolean = false, onKey: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .width(if (wide) 118.dp else 60.dp)
            .height(50.dp)
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onKey)
            .background(if (focused) Color.White else Surface2, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (focused) Color(0xFF0B0C10) else Text1,
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
