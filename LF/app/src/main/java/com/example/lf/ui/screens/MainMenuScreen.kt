package com.example.lf.ui.screens

import androidx.compose.animation.core.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.lf.R
import com.example.lf.ui.components.GlassCard
import com.example.lf.ui.components.MenuItem
import com.example.lf.ui.components.StatCard
import com.example.lf.ui.theme.*
import com.example.lf.viewmodel.AuthViewModel
import com.example.lf.viewmodel.StatsViewModel
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.getValue

@Composable
fun MainMenuScreen(
    navController: NavController,
    authViewModel: AuthViewModel,
    statsViewModel: StatsViewModel
) {
    val userInfo by authViewModel.userInfo.collectAsStateWithLifecycle()
    val token by authViewModel.authToken.collectAsStateWithLifecycle()
    val overallStats by statsViewModel.overallStats.observeAsState()
    val exerciseStats by statsViewModel.exerciseStats.observeAsState(emptyList())

    val currentToken = token

    LaunchedEffect(currentToken) {
        currentToken?.let {
            statsViewModel.loadOverallStats(it)
            statsViewModel.loadExerciseStats(it)
        }
    }

    val infiniteTransition = rememberInfiniteTransition()
    val offsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 30f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    val totalMinutes = (overallStats?.totalDuration ?: 0) / 60
    val avgAccuracy = if (exerciseStats.isNotEmpty()) {
        exerciseStats.mapNotNull { it.avgAccuracy }.average().toInt()
    } else 0

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(BackgroundCard, BackgroundDark),
                    radius = 1000f
                )
            )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = { navController.navigate("about") }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_about),
                    contentDescription = null,
                    tint = TextSecondary
                )
            }
        }
        Box(
            modifier = Modifier
                .size(400.dp)
                .background(Primary.copy(alpha = 0.05f), RoundedCornerShape(50.dp))
                .blur(50.dp)
                .offset(y = offsetY.dp)
                .align(Alignment.TopEnd)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            Text(
                text = "Привет,",
                fontSize = 20.sp,
                color = TextSecondary
            )
            Text(
                text = authViewModel.getDisplayName(),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    value = overallStats?.totalSessions?.toString() ?: "0",
                    label = "Тренировок",
                    color = Primary,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    value = "$totalMinutes",
                    label = "Минут",
                    color = Secondary,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    value = "${overallStats?.currentStreak ?: 0}",
                    label = "Дней подряд",
                    color = Tertiary,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SmallStatCard(
                    value = "${overallStats?.totalExercises ?: 0}",
                    label = "Упражнений",
                    icon = R.drawable.ic_exercise,
                    modifier = Modifier.weight(1f)
                )
                SmallStatCard(
                    value = "$avgAccuracy%",
                    label = "Точность",
                    icon = R.drawable.ic_accuracy,
                    modifier = Modifier.weight(1f)
                )
                SmallStatCard(
                    value = "${overallStats?.uniqueExercises ?: 0}",
                    label = "Видов",
                    icon = R.drawable.ic_stats,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            MenuItem(
                icon = R.drawable.ic_exercise,
                title = "Упражнения",
                subtitle = "Выберите и выполняйте",
                color = Primary,
                onClick = { navController.navigate("exercises") }
            )

            Spacer(modifier = Modifier.height(12.dp))

            MenuItem(
                icon = R.drawable.ic_stats,
                title = "Статистика",
                subtitle = "Отслеживайте прогресс",
                color = Secondary,
                onClick = { navController.navigate("stats") }
            )

            Spacer(modifier = Modifier.height(12.dp))

            MenuItem(
                icon = R.drawable.ic_profile,
                title = "Профиль",
                subtitle = "Настройки и информация",
                color = Tertiary,
                onClick = { navController.navigate("profile") }
            )

            Spacer(modifier = Modifier.height(32.dp))

            TextButton(
                onClick = {
                    authViewModel.logout()
                    navController.navigate("login") {
                        popUpTo("main_menu") { inclusive = true }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Выйти", color = Error, fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun SmallStatCard(
    value: String,
    label: String,
    icon: Int,
    modifier: Modifier = Modifier
) {
    GlassCard(modifier = modifier) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = label,
                fontSize = 10.sp,
                color = TextSecondary
            )
        }
    }
}