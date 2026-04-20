package com.example.lf.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    navController: NavController,
    authViewModel: AuthViewModel
) {
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    val isLoading = authState.isLoading

    var passwordError by remember { mutableStateOf<String?>(null) }

    // Обработка успешной регистрации
    LaunchedEffect(Unit) {
        authViewModel.onLoginSuccess = {
            navController.navigate("main_menu") {
                popUpTo("register") { inclusive = true }
            }
        }
    }

    // Валидация
    fun validate(): Boolean {
        if (username.length < 3) {
            passwordError = "Имя пользователя минимум 3 символа"
            return false
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            passwordError = "Неверный формат email"
            return false
        }
        if (password.length < 6) {
            passwordError = "Пароль минимум 6 символов"
            return false
        }
        if (password != confirmPassword) {
            passwordError = "Пароли не совпадают"
            return false
        }
        passwordError = null
        return true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(SecondaryDark, BackgroundDark)
                )
            )
    ) {
        Box(
            modifier = Modifier
                .size(250.dp)
                .background(Secondary.copy(alpha = 0.15f), RoundedCornerShape(50.dp))
                .blur(50.dp)
                .align(Alignment.TopStart)
        )
        Box(
            modifier = Modifier
                .size(300.dp)
                .background(Primary.copy(alpha = 0.1f), RoundedCornerShape(50.dp))
                .blur(50.dp)
                .align(Alignment.BottomEnd)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Создать аккаунт",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Text(
                text = "Присоединяйтесь к нам",
                fontSize = 14.sp,
                color = TextSecondary,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Имя пользователя *", color = TextSecondary) },
                        placeholder = { Text("john_doe", color = TextHint) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Secondary,
                            unfocusedBorderColor = TextHint,
                            focusedLabelColor = Secondary,
                            cursorColor = Secondary
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email *", color = TextSecondary) },
                        placeholder = { Text("example@mail.com", color = TextHint) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Secondary,
                            unfocusedBorderColor = TextHint,
                            focusedLabelColor = Secondary,
                            cursorColor = Secondary
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Пароль *", color = TextSecondary) },
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
                            focusedBorderColor = Secondary,
                            unfocusedBorderColor = TextHint,
                            focusedLabelColor = Secondary,
                            cursorColor = Secondary
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Подтвердите пароль *", color = TextSecondary) },
                        placeholder = { Text("••••••••", color = TextHint) },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Secondary,
                            unfocusedBorderColor = TextHint,
                            focusedLabelColor = Secondary,
                            cursorColor = Secondary
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = firstName,
                            onValueChange = { firstName = it },
                            label = { Text("Имя", color = TextSecondary) },
                            placeholder = { Text("Иван", color = TextHint) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Secondary,
                                unfocusedBorderColor = TextHint,
                                focusedLabelColor = Secondary,
                                cursorColor = Secondary
                            )
                        )

                        OutlinedTextField(
                            value = lastName,
                            onValueChange = { lastName = it },
                            label = { Text("Фамилия", color = TextSecondary) },
                            placeholder = { Text("Иванов", color = TextHint) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Secondary,
                                unfocusedBorderColor = TextHint,
                                focusedLabelColor = Secondary,
                                cursorColor = Secondary
                            )
                        )
                    }

                    if (passwordError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = passwordError ?: "",
                            color = Error,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    }

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
                text = "Зарегистрироваться",
                onClick = {
                    if (validate()) {
                        authViewModel.register(username, email, password, firstName, lastName)
                    }
                },
                isLoading = isLoading,
                gradient = GradientSecondary,
                enabled = !isLoading
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text("Уже есть аккаунт? ", color = TextSecondary)
                TextButton(onClick = { navController.popBackStack() }) {
                    Text("Войти", color = Secondary, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}