package com.showfilix.tv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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

private enum class Step { PHONE, CODE }

@Composable
fun LoginScreen(
    auth: AuthRepository,
    onLoggedIn: () -> Unit
) {
    var step by remember { mutableStateOf(Step.PHONE) }
    var phone by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var status by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val current = if (step == Step.PHONE) phone else code

    fun onDigit(d: String) {
        if (step == Step.PHONE) {
            if (phone.length < 11) phone += d
        } else {
            if (code.length < 6) code += d
        }
    }

    fun submit() {
        if (loading) return
        scope.launch {
            loading = true
            status = null
            try {
                if (step == Step.PHONE) {
                    if (phone.length < 10) {
                        status = "شماره نامعتبر است"
                    } else {
                        auth.sendOtp(phone)
                        step = Step.CODE
                        status = "کد پیامک شد."
                    }
                } else {
                    if (code.length < 4) {
                        status = "کد نامعتبر است"
                    } else {
                        auth.loginWithOtp(phone, code)
                        onLoggedIn()
                    }
                }
            } catch (e: Exception) {
                status = e.message ?: "خطا"
            } finally {
                loading = false
            }
        }
    }

    Column(
        Modifier.fillMaxSize().background(Bg0).padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(20.dp))
        Text("شوفیلیکس", color = Brand, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(10.dp))
        Text(
            text = if (step == Step.PHONE) "شماره موبایل خود را وارد کنید" else "کد پیامک‌شده را وارد کنید",
            color = Text1, fontSize = 24.sp, fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(18.dp))

        Box(
            Modifier
                .background(Color(0xFF161922), RoundedCornerShape(10.dp))
                .padding(horizontal = 24.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (current.isEmpty()) "—" else current,
                color = Text1, fontSize = 30.sp, letterSpacing = 6.sp
            )
        }

        status?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = if (it.contains("شد")) Text2 else Color(0xFFFFB4AB), fontSize = 16.sp)
        }

        Spacer(Modifier.height(20.dp))
        val digitRows = listOf("123", "456", "789")
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            digitRows.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { ch -> PadKey(ch.toString()) { onDigit(ch.toString()) } }
                }
                Spacer(Modifier.height(12.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                PadKey("⌫") {
                    if (step == Step.PHONE) phone = phone.dropLast(1) else code = code.dropLast(1)
                }
                PadKey("0") { onDigit("0") }
                FocusButton(
                    text = if (step == Step.PHONE) "ارسال کد" else "ورود",
                    primary = true,
                    onClick = { submit() }
                )
            }
        }

        Spacer(Modifier.height(14.dp))
        if (loading) Text("در حال پردازش…", color = Text2, fontSize = 15.sp)
        if (step == Step.CODE) {
            Spacer(Modifier.height(12.dp))
            FocusButton("ویرایش شماره", onClick = { step = Step.PHONE; code = ""; status = null })
        }
    }
}

@Composable
private fun PadKey(label: String, onKey: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .size(width = 84.dp, height = 64.dp)
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onKey)
            .background(if (focused) Color.White else Surface2, RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (focused) Color(0xFF0B0C10) else Text1,
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
