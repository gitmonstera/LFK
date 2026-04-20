package com.example.lf.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.lf.ui.components.ProfileInfoItem
import com.example.lf.ui.theme.*
import com.example.lf.viewmodel.AuthViewModel
import com.example.lf.viewmodel.StatsViewModel
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.getValue

@Composable
fun ProfileScreen(
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

    val createdAt = userInfo?.createdAt?.substring(0, 10) ?: "Не указано"
    val displayName = authViewModel.getDisplayName()
    val userEmail = userInfo?.email ?: "email@example.com"
    val firstName = userInfo?.getFirstName() ?: ""
    val lastName = userInfo?.getLastName() ?: ""
    val fullName = if (firstName.isNotEmpty() || lastName.isNotEmpty()) {
        "$firstName $lastName".trim()
    } else {
        null
    }

    val avgAccuracy = if (exerciseStats.isNotEmpty()) {
        exerciseStats.mapNotNull { it.avgAccuracy }.average().toInt()
    } else 0

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(TertiaryDark, BackgroundDark)
                )
            )
    ) {
        Box(
            modifier = Modifier
                .size(300.dp)
                .background(Tertiary.copy(alpha = 0.1f), RoundedCornerShape(50.dp))
                .blur(50.dp)
                .align(Alignment.TopEnd)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_arrow_back),
                        contentDescription = null,
                        tint = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "Профиль",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = { /* Edit profile */ }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_edit),
                        contentDescription = null,
                        tint = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(Primary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = displayName.firstOrNull()?.uppercase() ?: "?",
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = Primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = displayName,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            if (fullName != null) {
                Text(
                    text = fullName,
                    fontSize = 14.sp,
                    color = TextSecondary,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }

            Text(
                text = userEmail,
                fontSize = 12.sp,
                color = TextHint,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            ProfileInfoItem(
                icon = R.drawable.ic_calendar,
                label = "Дата регистрации",
                value = createdAt
            )

            ProfileInfoItem(
                icon = R.drawable.ic_trophy,
                label = "Всего тренировок",
                value = "${overallStats?.totalSessions ?: 0}"
            )

            ProfileInfoItem(
                icon = R.drawable.ic_streak,
                label = "Текущая серия",
                value = "${overallStats?.currentStreak ?: 0} дней"
            )

            ProfileInfoItem(
                icon = R.drawable.ic_achievement,
                label = "Максимальная серия",
                value = "${overallStats?.longestStreak ?: 0} дней"
            )

            ProfileInfoItem(
                icon = R.drawable.ic_schedule,
                label = "Общее время",
                value = "${(overallStats?.totalDuration?.div(60) ?: 0)} мин"
            )

            ProfileInfoItem(
                icon = R.drawable.ic_accuracy,
                label = "Средняя точность",
                value = "$avgAccuracy%"
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    authViewModel.logout()
                    navController.navigate("login") {
                        popUpTo("profile") { inclusive = true }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Error.copy(alpha = 0.15f),
                    contentColor = Error
                )
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_logout),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Выйти из аккаунта", fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}