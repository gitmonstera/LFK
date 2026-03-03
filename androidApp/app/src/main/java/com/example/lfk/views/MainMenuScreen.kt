package com.example.lfk.views

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight  // Это правильный импорт
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.lfk.viewmodel.AuthViewModel

/**
 * Главное меню приложения
 */
@Composable
fun MainMenuScreen(
    navController: NavController,
    authViewModel: AuthViewModel
) {
    val isAuthenticated by authViewModel.isAuthenticated.observeAsState(false)
    val userInfo by authViewModel.userInfo.observeAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Заголовок
        Text(
            text = "ГЛАВНОЕ МЕНЮ",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Divider(modifier = Modifier.padding(bottom = 16.dp))

        // Статус авторизации
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isAuthenticated)
                    Color(0xFF4CAF50).copy(alpha = 0.1f)
                else
                    Color(0xFFF44336).copy(alpha = 0.1f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isAuthenticated)
                        Icons.Default.CheckCircle
                    else
                        Icons.Default.Close,
                    contentDescription = null,
                    tint = if (isAuthenticated) Color(0xFF4CAF50) else Color(0xFFF44336)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isAuthenticated)
                        "✅ Авторизован: ${userInfo?.username ?: ""}"
                    else
                        "❌ Не авторизован",
                    fontWeight = FontWeight.Bold,
                    color = if (isAuthenticated) Color(0xFF4CAF50) else Color(0xFFF44336)
                )
            }
        }

        // Кнопки меню
        MenuButton(
            text = "1 - Войти в систему",
            onClick = { navController.navigate("login") },
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        MenuButton(
            text = "2 - Зарегистрироваться",
            onClick = { navController.navigate("register") },
            color = MaterialTheme.colorScheme.secondary
        )

        if (isAuthenticated) {
            Spacer(modifier = Modifier.height(8.dp))

            MenuButton(
                text = "3 - Выбрать упражнение",
                onClick = { navController.navigate("exercise_selection") },
                color = Color(0xFF4CAF50)
            )

            Spacer(modifier = Modifier.height(8.dp))

            MenuButton(
                text = "4 - Мой профиль",
                onClick = {
                    authViewModel.getProfile()
                    navController.navigate("profile")
                },
                color = Color(0xFF2196F3)
            )
        }
        MenuButton(
            text = "5 - Статистика",
            onClick = { navController.navigate("stats") },
            color = Color(0xFFFF9800) // Оранжевый
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Кнопка выхода (только для авторизованных)
        if (isAuthenticated) {
            OutlinedButton(
                onClick = {
                    authViewModel.logout()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.Red
                )
            ) {
                Text("Выйти")
            }
        }
    }
}

/**
 * Кнопка меню
 */
@Composable
fun MenuButton(
    text: String,
    onClick: () -> Unit,
    color: Color
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = color
        )
    ) {
        Text(
            text = text,
            fontSize = 16.sp
        )
    }
}