package com.example.lfk.views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.lfk.models.*
import com.example.lfk.viewmodel.StatsViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    navController: NavController,
    statsViewModel: StatsViewModel = viewModel(),
    authToken: String?
) {
    val overallStats by statsViewModel.overallStats.observeAsState()
    val dailyStats by statsViewModel.dailyStats.observeAsState()
    val weeklyStats by statsViewModel.weeklyStats.observeAsState(emptyList())
    val monthlyStats by statsViewModel.monthlyStats.observeAsState(emptyList())
    val exerciseStats by statsViewModel.exerciseStats.observeAsState(emptyList())
    val workoutHistory by statsViewModel.workoutHistory.observeAsState(emptyList())

    val isLoading by statsViewModel.isLoading.observeAsState(false)
    val error by statsViewModel.error.observeAsState()
    val currentTab by statsViewModel.currentTab.observeAsState(0)
    val selectedDate by statsViewModel.selectedDate.observeAsState("")

    val tabs = listOf(
        "Общая",
        "Дневная",
        "Недельная",
        "Месячная",
        "Упражнения",
        "История"
    )

    val tabIcons = listOf(
        Icons.Default.Info,
        Icons.Default.Today,
        Icons.Default.DateRange,
        Icons.Default.CalendarToday,
        Icons.Default.FitnessCenter,
        Icons.Default.History
    )

    // Загружаем статистику при первом запуске
    LaunchedEffect(Unit) {
        if (authToken != null) {
            statsViewModel.loadAllStats(authToken)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "📊 Статистика",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (authToken != null) {
                                statsViewModel.loadAllStats(authToken)
                            }
                        }
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Обновить")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = 0.8f))
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Табы
                ScrollableTabRow(
                    selectedTabIndex = currentTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    edgePadding = 0.dp,
                    divider = {}
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = currentTab == index,
                            onClick = { statsViewModel.setCurrentTab(index) },
                            text = {
                                Text(
                                    text = title,
                                    fontSize = 12.sp,
                                    maxLines = 1
                                )
                            },
                            icon = {
                                Icon(
                                    imageVector = tabIcons[index],
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            selectedContentColor = MaterialTheme.colorScheme.primary,
                            unselectedContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Контент в зависимости от выбранного таба
                when (currentTab) {
                    0 -> OverallStatsContent(overallStats)
                    1 -> DailyStatsContent(
                        dailyStats = dailyStats,
                        selectedDate = selectedDate,
                        onDateChange = { newDate ->
                            statsViewModel.setSelectedDate(newDate)
                            if (authToken != null) {
                                statsViewModel.loadDailyStats(authToken, newDate)
                            }
                        }
                    )
                    2 -> WeeklyStatsContent(weeklyStats)
                    3 -> MonthlyStatsContent(monthlyStats)
                    4 -> ExerciseStatsContent(exerciseStats)
                    5 -> WorkoutHistoryContent(workoutHistory)
                }
            }

            // Ошибка
            error?.let {
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    action = {
                        TextButton(
                            onClick = { statsViewModel.clearError() }
                        ) {
                            Text("OK")
                        }
                    }
                ) {
                    Text(it)
                }
            }
        }
    }
}

@Composable
fun StatsCard(
    icon: ImageVector,
    title: String,
    value: String,
    color: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                Text(
                    text = value,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
        }
    }
}

@Composable
fun OverallStatsContent(stats: OverallStats?) {
    if (stats == null) {
        EmptyStatsPlaceholder()
        return
    }

    val totalHours = stats.total_duration / 3600
    val totalMinutes = (stats.total_duration % 3600) / 60

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            StatsCard(
                icon = Icons.Default.FitnessCenter,
                title = "Всего тренировок",
                value = stats.total_sessions.toString(),
                color = Color(0xFF6200EE)
            )
        }

        item {
            StatsCard(
                icon = Icons.Default.Repeat,
                title = "Всего упражнений",
                value = stats.total_exercises.toString(),
                color = Color(0xFF03DAC5)
            )
        }

        item {
            StatsCard(
                icon = Icons.Default.RepeatOne,
                title = "Всего повторений",
                value = stats.total_repetitions.toString(),
                color = Color(0xFFFF9800)
            )
        }

        item {
            StatsCard(
                icon = Icons.Default.Timer,
                title = "Общее время",
                value = "${totalHours}ч ${totalMinutes}мин",
                color = Color(0xFF4CAF50)
            )
        }

        item {
            StatsCard(
                icon = Icons.Default.Category,
                title = "Уникальных упражнений",
                value = stats.unique_exercises.toString(),
                color = Color(0xFFE91E63)
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF4A90E2).copy(alpha = 0.1f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Whatshot, // Заменил Whatshot на WhatsApp
                            contentDescription = null,
                            tint = Color(0xFF4A90E2),
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Текущая серия",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                        Text(
                            text = "${stats.current_streak} дней",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4A90E2)
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFF5A623).copy(alpha = 0.1f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star, // Заменил EmojiEvents на Star
                            contentDescription = null,
                            tint = Color(0xFFF5A623),
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Макс. серия",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                        Text(
                            text = "${stats.longest_streak} дней",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF5A623)
                        )
                    }
                }
            }
        }

        stats.last_workout_at?.let { lastWorkout ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF9C27B0).copy(alpha = 0.1f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = Color(0xFF9C27B0),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Последняя тренировка",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                            Text(
                                text = formatDate(lastWorkout),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF9C27B0)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DailyStatsContent(
    dailyStats: DailyStats?,
    selectedDate: String,
    onDateChange: (String) -> Unit
) {
    Column {
        // Выбор даты
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(selectedDate)
                        val calendar = Calendar.getInstance()
                        calendar.time = date ?: Date()
                        calendar.add(Calendar.DAY_OF_YEAR, -1)
                        val newDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
                        onDateChange(newDate)
                    }
                ) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Предыдущий день")
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CalendarToday,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = selectedDate,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = {
                        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(selectedDate)
                        val calendar = Calendar.getInstance()
                        calendar.time = date ?: Date()
                        calendar.add(Calendar.DAY_OF_YEAR, 1)

                        // Не даем выбрать будущую дату
                        if (calendar.time <= Date()) {
                            val newDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
                            onDateChange(newDate)
                        }
                    }
                ) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Следующий день")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (dailyStats == null) {
            EmptyStatsPlaceholder("Нет данных за выбранный день")
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    StatsCard(
                        icon = Icons.Default.FitnessCenter,
                        title = "Тренировок",
                        value = dailyStats.total_sessions.toString(),
                        color = Color(0xFF6200EE)
                    )
                }

                item {
                    StatsCard(
                        icon = Icons.Default.Repeat,
                        title = "Упражнений",
                        value = dailyStats.total_exercises.toString(),
                        color = Color(0xFF03DAC5)
                    )
                }

                item {
                    val minutes = dailyStats.total_duration_seconds / 60
                    val seconds = dailyStats.total_duration_seconds % 60
                    StatsCard(
                        icon = Icons.Default.Timer,
                        title = "Время",
                        value = "${minutes}мин ${seconds}сек",
                        color = Color(0xFF4CAF50)
                    )
                }

                item {
                    StatsCard(
                        icon = Icons.Default.LocalFireDepartment,
                        title = "Сожжено калорий",
                        value = String.format("%.1f ккал", dailyStats.calories_burned),
                        color = Color(0xFFFF9800)
                    )
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (dailyStats.completed)
                                Color(0xFF4CAF50).copy(alpha = 0.1f)
                            else
                                Color(0xFFF44336).copy(alpha = 0.1f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (dailyStats.completed) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                contentDescription = null,
                                tint = if (dailyStats.completed) Color(0xFF4CAF50) else Color(0xFFF44336),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (dailyStats.completed) "День выполнен! ✅" else "День не выполнен ❌",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (dailyStats.completed) Color(0xFF4CAF50) else Color(0xFFF44336)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WeeklyStatsContent(stats: List<DailyStatItem>) {
    if (stats.isEmpty()) {
        EmptyStatsPlaceholder("Нет данных за неделю")
        return
    }

    var totalSessions = 0
    var totalExercises = 0
    var totalDuration = 0

    stats.forEach { day ->
        totalSessions += day.total_sessions
        totalExercises += day.total_exercises
        totalDuration += day.total_duration_seconds
    }

    val totalHours = totalDuration / 3600
    val totalMinutes = (totalDuration % 3600) / 60

    Column {
        // Итоги за неделю
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "📊 ИТОГИ ЗА НЕДЕЛЮ",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = totalSessions.toString(),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF6200EE)
                        )
                        Text(
                            text = "тренировок",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = totalExercises.toString(),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF03DAC5)
                        )
                        Text(
                            text = "упражнений",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${totalHours}ч ${totalMinutes}мин",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4CAF50)
                        )
                        Text(
                            text = "всего",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Дни недели
        Text(
            text = "ДНИ НЕДЕЛИ:",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(stats) { day ->
                DayStatItem(day)
            }
        }
    }
}

@Composable
fun DayStatItem(day: DailyStatItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (day.completed)
                Color(0xFF4CAF50).copy(alpha = 0.05f)
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Статус
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(
                        if (day.completed) Color(0xFF4CAF50).copy(alpha = 0.2f)
                        else Color(0xFFF44336).copy(alpha = 0.1f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (day.completed) "✅" else "❌",
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Дата
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = formatDate(day.stat_date),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${day.total_sessions} тр, ${day.total_exercises} упр",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            // Время
            val minutes = day.total_duration_seconds / 60
            Text(
                text = "${minutes} мин",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4CAF50)
            )
        }
    }
}

@Composable
fun MonthlyStatsContent(weeks: List<WeekStats>) {
    if (weeks.isEmpty()) {
        EmptyStatsPlaceholder("Нет данных за месяц")
        return
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(weeks) { week ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "📅 Неделя ${week.week_number}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        WeekStatColumn(
                            value = week.sessions.toString(),
                            label = "тренировок",
                            color = Color(0xFF6200EE)
                        )

                        WeekStatColumn(
                            value = week.exercises.toString(),
                            label = "упражнений",
                            color = Color(0xFF03DAC5)
                        )

                        val hours = week.duration / 3600
                        val minutes = (week.duration % 3600) / 60
                        WeekStatColumn(
                            value = "${hours}ч ${minutes}мин",
                            label = "время",
                            color = Color(0xFF4CAF50)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WeekStatColumn(
    value: String,
    label: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.Gray
        )
    }
}

@Composable
fun ExerciseStatsContent(stats: List<ExerciseStatItem>) {
    if (stats.isEmpty()) {
        EmptyStatsPlaceholder("Нет данных по упражнениям")
        return
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(stats) { exercise ->
            ExerciseStatCard(exercise)
        }
    }
}

@Composable
fun ExerciseStatCard(exercise: ExerciseStatItem) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🏋️",
                    fontSize = 24.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = exercise.exercise_name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = exercise.total_sessions.toString(),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6200EE)
                    )
                    Text(
                        text = "сессий",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = exercise.total_repetitions.toString(),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF03DAC5)
                    )
                    Text(
                        text = "повторений",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }

                val minutes = exercise.total_duration / 60
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${minutes}мин",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50)
                    )
                    Text(
                        text = "время",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                exercise.best_accuracy?.let {
                    Column {
                        Text(
                            text = "Лучшая точность",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                        Text(
                            text = String.format("%.1f%%", it),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF9800)
                        )
                    }
                }

                exercise.avg_accuracy?.let {
                    Column {
                        Text(
                            text = "Средняя точность",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                        Text(
                            text = String.format("%.1f%%", it),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4CAF50)
                        )
                    }
                }
            }

            exercise.last_performed_at?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color.Gray
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Последний раз: ${formatDate(it)}",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
fun WorkoutHistoryContent(history: List<WorkoutHistoryItem>) {
    if (history.isEmpty()) {
        EmptyStatsPlaceholder("Нет истории тренировок")
        return
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(history) { index, workout ->
            WorkoutHistoryCard(
                number = index + 1,
                workout = workout
            )
        }
    }
}

@Composable
fun WorkoutHistoryCard(
    number: Int,
    workout: WorkoutHistoryItem
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = number.toString(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = formatDate(workout.started_at),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
                    )
                ) {
                    Text(
                        text = String.format("%.1f%%", workout.avg_accuracy),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                StatChip(
                    value = workout.total_exercises.toString(),
                    label = "упражнений",
                    icon = Icons.Default.Repeat
                )

                StatChip(
                    value = workout.total_reps.toString(),
                    label = "повторений",
                    icon = Icons.Default.RepeatOne
                )

                val minutes = workout.total_duration / 60
                StatChip(
                    value = "${minutes}мин",
                    label = "время",
                    icon = Icons.Default.Timer
                )
            }

            if (workout.exercises.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Выполненные упражнения:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray
                )

                workout.exercises.forEach { ex ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 8.dp, top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "• ${ex.name}",
                            fontSize = 12.sp
                        )
                        Text(
                            text = "${ex.repetitions} раз",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF6200EE)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatChip(
    value: String,
    label: String,
    icon: ImageVector
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = Color.Gray
        )
        Spacer(modifier = Modifier.width(4.dp))
        Column {
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                fontSize = 10.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun EmptyStatsPlaceholder(message: String = "Нет данных для отображения") {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "📊",
                fontSize = 48.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                fontSize = 16.sp,
                color = Color.Gray,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

fun formatDate(dateString: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
        inputFormat.timeZone = TimeZone.getTimeZone("UTC")
        val date = inputFormat.parse(dateString) ?: return dateString

        val outputFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        outputFormat.format(date)
    } catch (e: Exception) {
        dateString.take(10)
    }
}