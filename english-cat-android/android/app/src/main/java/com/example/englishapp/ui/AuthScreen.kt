package com.example.englishapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AuthScreen(
    busy: Boolean,
    error: String?,
    backendUrl: String,
    onChangeUrl: (String) -> Unit,
    onClearError: () -> Unit = {},
    onOfflineMode: () -> Unit,
    onSubmit: (isRegister: Boolean, name: String, email: String, password: String) -> Unit,
) {
    var isRegister by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var showUrlField by rememberSaveable { mutableStateOf(false) }
    var localError by rememberSaveable { mutableStateOf<String?>(null) }

    val shownError = localError ?: error

    fun validateAndSubmit() {
        val cleanName = name.trim()
        val cleanEmail = email.trim()
        localError = when {
            isRegister && cleanName.isEmpty() -> "Vui lòng nhập tên của bạn."
            cleanEmail.isEmpty() || !cleanEmail.contains("@") -> "Vui lòng nhập email hợp lệ."
            password.length < 6 -> "Mật khẩu cần tối thiểu 6 ký tự."
            else -> null
        }
        if (localError == null) {
            onSubmit(isRegister, cleanName, cleanEmail, password)
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("🐱", fontSize = 80.sp)
        Text("English Cat", color = Blue, fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Text(
            "Học tiếng Anh mỗi ngày, từng bài một",
            color = Color.Gray,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(28.dp))

        if (isRegister) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it; localError = null },
                label = { Text("Tên của bạn") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
        }
        OutlinedTextField(
            value = email,
            onValueChange = { email = it; localError = null },
            label = { Text("Email") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it; localError = null },
            label = { Text("Mật khẩu (tối thiểu 6 ký tự)") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
        )

        if (showUrlField) {
            Spacer(Modifier.height(12.dp))
            var urlDraft by rememberSaveable { mutableStateOf(backendUrl) }
            OutlinedTextField(
                value = urlDraft,
                onValueChange = { urlDraft = it },
                label = { Text("URL Backend") },
                singleLine = true,
                placeholder = { Text("https://...") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(6.dp))
            TextButton(
                onClick = {
                    if (urlDraft.isNotBlank()) onChangeUrl(urlDraft.trim())
                    showUrlField = false
                },
            ) { Text("Lưu URL", color = Blue) }
        }

        if (shownError != null) {
            Spacer(Modifier.height(12.dp))
            Text(shownError, color = Red, fontSize = 15.sp, textAlign = TextAlign.Center)
        }

        Spacer(Modifier.height(20.dp))
        PrimaryButton(
            text = if (busy) "Đang xử lý..." else if (isRegister) "Tạo tài khoản" else "Đăng nhập",
            enabled = !busy,
            onClick = ::validateAndSubmit,
        )
        Spacer(Modifier.height(8.dp))
        SecondaryButton(
            text = "Học offline (không cần tài khoản)",
            enabled = !busy,
            onClick = onOfflineMode,
        )

        TextButton(
            enabled = !busy,
            onClick = {
                isRegister = !isRegister
                localError = null
                onClearError()
            },
        ) {
            Text(
                if (isRegister) "Đã có tài khoản? Đăng nhập" else "Chưa có tài khoản? Đăng ký",
                color = Blue,
            )
        }
        TextButton(onClick = { showUrlField = !showUrlField }) {
            Text("⚙ Đổi địa chỉ backend", color = Color.Gray, fontSize = 13.sp)
        }
    }
}
