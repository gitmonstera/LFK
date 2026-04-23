package com.example.lf.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.lf.R
import com.example.lf.api.DailyStatsResponse
import com.example.lf.api.ExerciseStatResponse
import com.example.lf.api.OverallStatsResponse
import com.example.lf.api.WorkoutHistoryResponse
import com.example.lf.ui.components.GlassCard
import com.example.lf.ui.components.HistoryItem
import com.example.lf.ui.components.PeriodButton
import com.example.lf.ui.components.StatValueCard
import com.example.lf.ui.components.TopExerciseItem
import com.example.lf.ui.theme.BackgroundCard
import com.example.lf.ui.theme.BackgroundDark
import com.example.lf.ui.theme.Primary
import com.example.lf.ui.theme.PrimaryLight
import com.example.lf.ui.theme.Secondary
import com.example.lf.ui.theme.Success
import com.example.lf.ui.theme.TextPrimary
import com.example.lf.ui.theme.TextSecondary
import com.example.lf.viewmodel.AuthViewModel
import com.example.lf.viewmodel.StatsViewModel

@Composable
fun StatsScreen(
    navController: NavController,
    statsViewModel: StatsViewModel,
    authViewModel: AuthViewModel
) {
    var selectedPeriod by remember { mutableStateOf("week") }
    val token by authViewModel.authToken.collectAsStateWithLifecycle()

    val overallStats by statsViewModel.overallStats.observeAsState()
    val weeklyStats by statsViewModel.weeklyStats.observeAsState(emptyList())
    val monthlyStats by statsViewModel.monthlyStats.observeAsState(emptyList())
    val exerciseStats by statsViewModel.exerciseStats.observeAsState(emptyList())
    val workoutHistory by statsViewModel.workoutHistory.observeAsState(emptyList())
    val isLoading by statsViewModel.isLoading.observeAsState(false)

    val currentToken = token

    LaunchedEffect(currentToken, selectedPeriod) {
        currentToken?.let {
            when (selectedPeriod) {
                "week" -> statsViewModel.loadWeeklyStats(it)
                "month" -> statsViewModel.loadMonthlyStats(it)
                "all" -> statsViewModel.loadOverallStats(it)
            }
            statsViewModel.loadExerciseStats(it)
            statsViewModel.loadWorkoutHistory(it)
        }
    }

    val totalMinutes = (overallStats?.totalDuration ?: 0) / 60
    val avgAccuracy = if (exerciseStats.isNotEmpty()) {
        exerciseStats.mapNotNull { it.avgAccuracy }.average().toInt()
    } else 0

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(BackgroundDark, BackgroundCard)
                )
            )
    ) {

        Box(
            modifier = Modifier
                .size(400.dp)
                .background(Secondary.copy(alpha = 0.05f), RoundedCornerShape(50.dp))
                .blur(50.dp)
                .align(Alignment.BottomStart)
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
                Text(
                    text = "Статистика",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PeriodButton(
                    text = "Неделя",
                    isSelected = selectedPeriod == "week",
                    onClick = { selectedPeriod = "week" }
                )
                PeriodButton(
                    text = "Месяц",
                    isSelected = selectedPeriod == "month",
                    onClick = { selectedPeriod = "month" }
                )
                PeriodButton(
                    text = "Все время",
                    isSelected = selectedPeriod == "all",
                    onClick = { selectedPeriod = "all" }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Primary)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {

                    item {
                        StatsOverviewCard(
                            totalSessions = overallStats?.totalSessions ?: 0,
                            totalMinutes = totalMinutes,
                            avgAccuracy = avgAccuracy
                        )
                    }

                    when (selectedPeriod) {
                        "week" -> {
                            if (weeklyStats.isNotEmpty()) {
                                item { WeeklyChartCard(weeklyStats = weeklyStats) }
                            }
                        }
                        "month" -> {
                            if (monthlyStats.isNotEmpty()) {
                                item { MonthlyStatsCard(monthlyStats = monthlyStats) }
                            }
                        }
                        "all" -> {
                            if (overallStats != null) {
                                item { AllTimeStatsCard(stats = overallStats!!) }
                            }
                        }
                    }

                    if (exerciseStats.isNotEmpty()) {
                        item { TopExercisesCard(exerciseStats = exerciseStats) }
                    }

                    if (workoutHistory.isNotEmpty()) {
                        item { HistoryCard(workoutHistory = workoutHistory) }
                    }
                }
            }
        }
    }
}

@Composable
fun StatsOverviewCard(
    totalSessions: Int,
    totalMinutes: Int,
    avgAccuracy: Int
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Общая статистика",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatValueCard(
                value = "$totalSessions",
                label = "Тренировок",
                icon = R.drawable.ic_calendar,
                color = Primary
            )
            StatValueCard(
                value = "$totalMinutes",
                label = "Минут",
                icon = R.drawable.ic_schedule,
                color = Secondary
            )
            StatValueCard(
                value = "$avgAccuracy%",
                label = "Точность",
                icon = R.drawable.ic_accuracy,
                color = Success
            )
        }
    }
}

@Composable
fun WeeklyChartCard(weeklyStats: List<DailyStatsResponse>) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Прогресс за неделю",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(16.dp))

        val maxDuration = weeklyStats.maxOfOrNull { it.totalDurationSeconds } ?: 1

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            weeklyStats.forEach { day ->
                val height = (day.totalDurationSeconds.toFloat() / maxDuration * 100).coerceIn(5f, 100f)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .width(30.dp)
                            .height(height.dp)
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(Primary, PrimaryLight)
                                ),
                                RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                            )
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        day.statDate.substring(5, 10),
                        fontSize = 10.sp,
                        color = TextSecondary,
                        maxLines = 1
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))
        Text(
            "⏱️ Активность по дням",
            fontSize = 10.sp,
            color = TextSecondary,
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            maxLines = 1
        )
    }
}

@Composable
fun MonthlyStatsCard(monthlyStats: List<DailyStatsResponse>) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Статистика за месяц",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(20.dp))

        val totalSessions = monthlyStats.sumOf { it.totalSessions }
        val totalMinutes = monthlyStats.sumOf { it.totalDurationSeconds } / 60
        val activeDays = monthlyStats.count { it.completed }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "$totalSessions",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Primary,
                    maxLines = 1
                )
                Text(
                    "Тренировок",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    maxLines = 1
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "$totalMinutes",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Secondary,
                    maxLines = 1
                )
                Text(
                    "Минут",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    maxLines = 1
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "$activeDays",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Success,
                    maxLines = 1
                )
                Text(
                    "Дней",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun AllTimeStatsCard(stats: OverallStatsResponse) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "За все время",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "${stats.totalSessions}",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Primary,
                    maxLines = 1
                )
                Text(
                    "Тренировок",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    maxLines = 1
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "${stats.totalDuration / 60}",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Secondary,
                    maxLines = 1
                )
                Text(
                    "Минут",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    maxLines = 1
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "${stats.currentStreak}",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Success,
                    maxLines = 1
                )
                Text(
                    "Серия",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun TopExercisesCard(exerciseStats: List<ExerciseStatResponse>) {
    if (exerciseStats.isEmpty()) return

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Любимые упражнения",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(20.dp))

        exerciseStats.take(3).forEach { exercise ->
            TopExerciseItem(
                name = exercise.exerciseName,
                count = exercise.totalSessions,
                progress = (exercise.totalSessions / 20f).coerceIn(0f, 1f)
            )
        }
    }
}

@Composable
fun HistoryCard(workoutHistory: List<WorkoutHistoryResponse>) {
    if (workoutHistory.isEmpty()) return

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Последние тренировки",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(20.dp))

        workoutHistory.take(3).forEach { workout ->
            val date = workout.startedAt.substring(0, 10)
            val minutes = workout.totalDuration / 60

            HistoryItem(
                date = date,
                exercises = "${workout.totalExercises} упражнения",
                duration = "$minutes мин",
                accuracy = workout.avgAccuracy.toInt()
            )
        }
    }
}