package com.mehran.movie.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mehran.movie.auth.AuthRepository
import com.mehran.movie.ui.components.FocusButton
import com.mehran.movie.ui.theme.Bg0
import com.mehran.movie.ui.theme.Brand
import com.mehran.movie.ui.theme.Surface2
import com.mehran.movie.ui.theme.Text1
import com.mehran.movie.ui.theme.Text2
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    auth: AuthRepository,
    onLoggedIn: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var status by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current

    val emailFocus = remember { FocusRequester() }
    val passwordFocus = remember { FocusRequester() }
    val scroll = rememberScrollState()

    fun submit() {
        if (loading) return
        keyboard?.hide()
        if (!email.contains("@")) {
            status = "ایمیل معتبر وارد کنید"
            emailFocus.requestFocus()
            return
        }
        if (password.length < 8) {
            status = "رمز عبور باید حداقل ۸ کاراکتر باشد"
            passwordFocus.requestFocus()
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

    // Scrollable + imePadding: when the native keyboard opens, the content moves
    // up (and scrolls if needed) so the login button stays on screen.
    Column(
        Modifier
            .fillMaxSize()
            .background(Bg0)
            .imePadding()
            .verticalScroll(scroll)
            .padding(horizontal = 40.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("مهران مووی", color = Brand, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(4.dp))
        Text("با ایمیل و رمز عبور وارد شوید", color = Text2, fontSize = 16.sp)
        Spacer(Modifier.height(20.dp))

        LoginField(
            label = "ایمیل",
            value = email,
            placeholder = "you@example.com",
            mask = false,
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next,
            focusRequester = emailFocus,
            onValueChange = { email = it },
            onImeAction = { passwordFocus.requestFocus() }
        )
        Spacer(Modifier.height(12.dp))
        LoginField(
            label = "رمز عبور",
            value = password,
            placeholder = "••••••••",
            mask = true,
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done,
            focusRequester = passwordFocus,
            onValueChange = { password = it },
            onImeAction = { submit() }
        )

        status?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = Color(0xFFFFB4AB), fontSize = 15.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }

        Spacer(Modifier.height(24.dp))
        FocusButton(
            text = if (loading) "در حال ورود…" else "ورود",
            primary = true,
            onClick = { submit() }
        )

        Spacer(Modifier.height(16.dp))
        Text(
            "برای تایپ، کادر ایمیل یا رمز عبور را انتخاب کنید تا صفحه‌کلید دستگاه باز شود؛ " +
                "صفحه به‌طور خودکار بالا می‌آید تا دکمهٔ ورود دیده شود.",
            color = Text2, fontSize = 12.sp
        )
    }
}

@Composable
private fun LoginField(
    label: String,
    value: String,
    placeholder: String,
    mask: Boolean,
    keyboardType: KeyboardType,
    imeAction: ImeAction,
    focusRequester: FocusRequester,
    onValueChange: (String) -> Unit,
    onImeAction: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val keyboard = LocalSoftwareKeyboardController.current

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = TextStyle(color = Text1, fontSize = 19.sp),
        visualTransformation = if (mask) PasswordVisualTransformation() else VisualTransformation.None,
        cursorBrush = SolidColor(Brand),
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = imeAction
        ),
        keyboardActions = KeyboardActions(
            onNext = { onImeAction() },
            onDone = { onImeAction() }
        ),
        modifier = Modifier
            .widthIn(max = 760.dp)
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .onFocusChanged {
                focused = it.isFocused
                // D-pad / remote focus should also raise the native keyboard.
                if (it.isFocused) keyboard?.show()
            }
    ) { innerTextField ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Surface2, RoundedCornerShape(10.dp))
                .border(
                    2.dp,
                    if (focused) Brand else Color.Transparent,
                    RoundedCornerShape(10.dp)
                )
                .clickable { focusRequester.requestFocus() }
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, color = Text2, fontSize = 16.sp, modifier = Modifier.width(110.dp))
            Box(Modifier.weight(1f)) {
                if (value.isEmpty()) {
                    Text(placeholder, color = Text2, fontSize = 19.sp)
                }
                innerTextField()
            }
        }
    }
}
