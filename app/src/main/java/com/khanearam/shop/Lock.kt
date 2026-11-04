package com.khanearam.shop

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

fun biometricAvailable(act: android.content.Context): Boolean =
    BiometricManager.from(act).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS

fun promptBiometric(act: FragmentActivity, onOk: () -> Unit) {
    if (!biometricAvailable(act)) return
    val prompt = BiometricPrompt(
        act, ContextCompat.getMainExecutor(act),
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onOk()
            }
        }
    )
    val info = BiometricPrompt.PromptInfo.Builder()
        .setTitle("ورود به خانه آرام")
        .setNegativeButtonText("استفاده از رمز")
        .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
        .build()
    prompt.authenticate(info)
}

@Composable
fun LockScreen() {
    val ctx = LocalContext.current
    var pin by remember { mutableStateOf("") }
    var err by remember { mutableStateOf("") }
    var showForgot by remember { mutableStateOf(false) }

    fun tryBio() {
        val act = ctx as? FragmentActivity ?: return
        promptBiometric(act) { Store.locked = false }
    }

    LaunchedEffect(Unit) {
        if (Store.bioEnabled) tryBio()
    }

    Box(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).pointerInput(Unit) {},
        contentAlignment = Alignment.Center
    ) {
        Column(Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("خانه آرام", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text("رمز ۴ رقمی خود را وارد کنید")
            OutlinedTextField(
                value = pin,
                onValueChange = { v ->
                    if (v.length <= 4 && v.all { it.isDigit() }) {
                        pin = v
                        if (v.length == 4) {
                            if (Store.checkPin(v)) {
                                Store.locked = false
                            } else {
                                err = "رمز اشتباه است"
                                pin = ""
                            }
                        }
                    }
                },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.fillMaxWidth()
            )
            if (err.isNotEmpty()) Text(err, color = Color.Red)
            if (Store.bioEnabled) {
                SoftButton(onClick = { tryBio() }) { Text("ورود با اثر انگشت") }
            }
            TextButton(onClick = { showForgot = true }) { Text("رمز را فراموش کرده‌ام") }
        }
    }
    if (showForgot) {
        AlertDialog(
            onDismissRequest = { showForgot = false },
            title = { Text("فراموشی رمز") },
            text = { Text("برای امنیت، با فراموشی رمز از حساب شما خارج می‌شویم و قفل برداشته می‌شود. ادامه می‌دهید؟") },
            confirmButton = {
                TextButton(onClick = {
                    showForgot = false
                    Store.logout()
                    Store.clearLock()
                }) { Text("ادامه") }
            },
            dismissButton = { TextButton(onClick = { showForgot = false }) { Text("انصراف") } }
        )
    }
}
