package com.example.lfk.views

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.lfk.models.User
import com.example.lfk.viewmodel.AuthViewModel
import com.example.lfk.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    authViewModel: AuthViewModel
) {
    val userInfo by authViewModel.userInfo.observeAsState()
    val isLoading by authViewModel.isLoading.observeAsState(false)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Мой профиль",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.primaryGradientStart,
                            MaterialTheme.primaryGradientEnd
                        )
                    )
                )
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = 0.8f))
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (userInfo == null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.large,
                            elevation = CardDefaults.cardElevation(8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.PersonOff,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.textSecondary
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Не удалось загрузить профиль",
                                    fontSize = 16.sp,
                                    color = MaterialTheme.textSecondary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Попробуйте позже",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.textSecondary
                                )
                            }
                        }
                    } else {
                        userInfo?.let { user ->
                            ProfileHeader(user = user)

                            Spacer(modifier = Modifier.height(24.dp))

                            ProfileInfoCard(user = user)

                            Spacer(modifier = Modifier.height(24.dp))

                            // Кнопка назад
                            OutlinedButton(
                                onClick = { navController.popBackStack() },
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.medium,
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(
                                    Icons.Default.ArrowBack,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Назад в главное меню",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileHeader(user: User) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Аватар
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                if (!user.first_name.isNullOrEmpty() || !user.last_name.isNullOrEmpty()) {
                    Text(
                        text = (user.first_name?.firstOrNull()?.toString() ?: "") +
                                (user.last_name?.firstOrNull()?.toString() ?: ""),
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(50.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Имя пользователя
            Text(
                text = user.username,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.textPrimary
            )

            // Полное имя (если есть)
            if (!user.first_name.isNullOrEmpty() || !user.last_name.isNullOrEmpty()) {
                Text(
                    text = listOfNotNull(user.first_name, user.last_name).joinToString(" "),
                    fontSize = 16.sp,
                    color = MaterialTheme.textSecondary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
fun ProfileInfoCard(user: User) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "Информация",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.textPrimary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Divider(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // ID
            ProfileInfoItem(
                icon = Icons.Default.Fingerprint,
                label = "ID",
                value = user.id
            )

            // Email
            ProfileInfoItem(
                icon = Icons.Default.Email,
                label = "Email",
                value = user.email
            )

            // Имя
            if (!user.first_name.isNullOrEmpty()) {
                ProfileInfoItem(
                    icon = Icons.Default.Person,
                    label = "Имя",
                    value = user.first_name ?: ""
                )
            }

            // Фамилия
            if (!user.last_name.isNullOrEmpty()) {
                ProfileInfoItem(
                    icon = Icons.Default.PersonOutline,
                    label = "Фамилия",
                    value = user.last_name ?: ""
                )
            }

            // Дата регистрации
            ProfileInfoItem(
                icon = Icons.Default.CalendarToday,
                label = "Дата регистрации",
                value = formatProfileDate(user.created_at)
            )
        }
    }
}

@Composable
fun ProfileInfoItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                color = MaterialTheme.textSecondary
            )
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.textPrimary
            )
        }
    }
}

// Переименовал функцию, чтобы избежать конфликта с formatDate из StatsScreen
fun formatProfileDate(dateString: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
        inputFormat.timeZone = TimeZone.getTimeZone("UTC")
        val date = inputFormat.parse(dateString) ?: return dateString.take(10)

        val outputFormat = SimpleDateFormat("dd MMMM yyyy", Locale("ru"))
        outputFormat.format(date)
    } catch (e: Exception) {
        dateString.take(10)
    }
}