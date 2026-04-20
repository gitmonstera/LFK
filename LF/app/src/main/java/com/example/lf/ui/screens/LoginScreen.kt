package com.example.lf.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.lf.R
import com.example.lf.ui.components.GradientButton
import com.example.lf.ui.components.GlassCard
import com.example.lf.ui.theme.*
import com.example.lf.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    navController: NavController,
    authViewModel: AuthViewModel
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    val isLoading = authState.isLoading

    // Обработка успешного входа
    LaunchedEffect(Unit) {
        authViewModel.onLoginSuccess = {
            navController.navigate("main_menu") {
                popUpTo("login") { inclusive = true }
            }
        }
    }

    // Показ ошибки
    LaunchedEffect(authState.error) {
        authState.error?.let { error ->
            // Можно показать Snackbar или диалог
            println("Ошибка: $error")
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(PrimaryDark, BackgroundDark)
                )
            )
    ) {
        Box(
            modifier = Modifier
                .size(300.dp)
                .background(Primary.copy(alpha = 0.1f), RoundedCornerShape(50.dp))
                .blur(50.dp)
                .align(Alignment.TopEnd)
        )
        Box(
            modifier = Modifier
                .size(200.dp)
                .background(Secondary.copy(alpha = 0.1f), RoundedCornerShape(50.dp))
                .blur(50.dp)
                .align(Alignment.BottomStart)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = "Logo",
                modifier = Modifier.size(80.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Добро пожаловать",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Text(
                text = "Войдите в свой аккаунт",
                fontSize = 14.sp,
                color = TextSecondary,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(48.dp))

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email", color = TextSecondary) },
                        placeholder = { Text("example@mail.com", color = TextHint) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Primary,
                            unfocusedBorderColor = TextHint,
                            focusedLabelColor = Primary,
                            cursorColor = Primary
                        ),
                        isError = authState.error != null && email.isNotBlank()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Пароль", color = TextSecondary) },
                        placeholder = { Text("••••••••", color = TextHint) },
                        visualTransformation = if (showPassword) PasswordVisualTransformation() else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    painter = painterResource(
                                        id = if (showPassword) R.drawable.ic_visibility_off
                                        else R.drawable.ic_visibility
                                    ),
                                    contentDescription = null,
                                    tint = TextSecondary
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Primary,
                            unfocusedBorderColor = TextHint,
                            focusedLabelColor = Primary,
                            cursorColor = Primary
                        ),
                        isError = authState.error != null
                    )

                    if (authState.error != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = authState.error ?: "",
                            color = Error,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            GradientButton(
                text = "Войти",
                onClick = {
                    if (email.isNotBlank() && password.isNotBlank()) {
                        authViewModel.login(email, password)
                    }
                },
                isLoading = isLoading,
                enabled = email.isNotBlank() && password.isNotBlank() && !isLoading
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text("Нет аккаунта? ", color = TextSecondary)
                TextButton(onClick = { navController.navigate("register") }) {
                    Text("Зарегистрироваться", color = Primary, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}