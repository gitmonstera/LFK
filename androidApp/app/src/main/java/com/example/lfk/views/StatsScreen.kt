package com.example.lfk.views

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.lfk.models.*
import com.example.lfk.viewmodel.StatsViewModel
import com.example.lfk.ui.theme.*
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
                    Text(
                        text = "Статистика",
                        fontWeight = FontWeight.Bold
                    )
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
                        .padding(16.dp)
                ) {
                    // Карточка с табами
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            ScrollableTabRow(
                                selectedTabIndex = currentTab,
                                containerColor = Color.Transparent,
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
                                        unselectedContentColor = MaterialTheme.textSecondary
                                    )
                                }
                            }
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
        shape = MaterialTheme.shapes.medium,
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
                    color = MaterialTheme.textSecondary
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
                color = MaterialTheme.colorScheme.primary
            )
        }

        item {
            StatsCard(
                icon = Icons.Default.Repeat,
                title = "Всего упражнений",
                value = stats.total_exercises.toString(),
                color = MaterialTheme.success
            )
        }

        item {
            StatsCard(
                icon = Icons.Default.RepeatOne,
                title = "Всего повторений",
                value = stats.total_repetitions.toString(),
                color = MaterialTheme.warning
            )
        }

        item {
            StatsCard(
                icon = Icons.Default.Timer,
                title = "Общее время",
                value = "${totalHours}ч ${totalMinutes}мин",
                color = MaterialTheme.success
            )
        }

        item {
            StatsCard(
                icon = Icons.Default.Category,
                title = "Уникальных упражнений",
                value = stats.unique_exercises.toString(),
                color = MaterialTheme.info
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Текущая серия",
                            fontSize = 12.sp,
                            color = MaterialTheme.textSecondary
                        )
                        Text(
                            text = "${stats.current_streak} дней",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.warning.copy(alpha = 0.1f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = MaterialTheme.warning,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Макс. серия",
                            fontSize = 12.sp,
                            color = MaterialTheme.textSecondary
                        )
                        Text(
                            text = "${stats.longest_streak} дней",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.warning
                        )
                    }
                }
            }
        }

        stats.last_workout_at?.let { lastWorkout ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.info.copy(alpha = 0.1f)
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
                            tint = MaterialTheme.info,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Последняя тренировка",
                                fontSize = 12.sp,
                                color = MaterialTheme.textSecondary
                            )
                            Text(
                                text = formatStatsDate(lastWorkout),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.info
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
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
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
                    Icon(
                        Icons.Default.ChevronLeft,
                        contentDescription = "Предыдущий день",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable {
                        // Сброс на сегодня
                        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                        onDateChange(today)
                    }
                ) {
                    Icon(
                        Icons.Default.CalendarToday,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = selectedDate,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.textPrimary
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
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = "Следующий день",
                        tint = MaterialTheme.colorScheme.primary
                    )
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
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                item {
                    StatsCard(
                        icon = Icons.Default.Repeat,
                        title = "Упражнений",
                        value = dailyStats.total_exercises.toString(),
                        color = MaterialTheme.success
                    )
                }

                item {
                    val minutes = dailyStats.total_duration_seconds / 60
                    val seconds = dailyStats.total_duration_seconds % 60
                    StatsCard(
                        icon = Icons.Default.Timer,
                        title = "Время",
                        value = "${minutes}мин ${seconds}сек",
                        color = MaterialTheme.success
                    )
                }

                item {
                    StatsCard(
                        icon = Icons.Default.LocalFireDepartment,
                        title = "Сожжено калорий",
                        value = String.format("%.1f ккал", dailyStats.calories_burned),
                        color = MaterialTheme.warning
                    )
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(
                            containerColor = if (dailyStats.completed)
                                MaterialTheme.success.copy(alpha = 0.1f)
                            else
                                MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
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
                                tint = if (dailyStats.completed) MaterialTheme.success else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (dailyStats.completed) "День выполнен! ✅" else "День не выполнен ❌",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (dailyStats.completed) MaterialTheme.success else MaterialTheme.colorScheme.error
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
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
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
                    WeekStatColumn(
                        value = totalSessions.toString(),
                        label = "тренировок",
                        color = MaterialTheme.colorScheme.primary
                    )

                    WeekStatColumn(
                        value = totalExercises.toString(),
                        label = "упражнений",
                        color = MaterialTheme.success
                    )

                    WeekStatColumn(
                        value = "${totalHours}ч ${totalMinutes}мин",
                        label = "всего",
                        color = MaterialTheme.warning
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Дни недели
        Text(
            text = "ДНИ НЕДЕЛИ:",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.textPrimary,
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
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = if (day.completed)
                MaterialTheme.success.copy(alpha = 0.05f)
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
                        if (day.completed) MaterialTheme.success.copy(alpha = 0.2f)
                        else MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
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
                    text = formatStatsDate(day.stat_date),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.textPrimary
                )
                Text(
                    text = "${day.total_sessions} тр, ${day.total_exercises} упр",
                    fontSize = 12.sp,
                    color = MaterialTheme.textSecondary
                )
            }

            // Время
            val minutes = day.total_duration_seconds / 60
            Text(
                text = "${minutes} мин",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.success
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
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
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
                            color = MaterialTheme.colorScheme.primary
                        )

                        WeekStatColumn(
                            value = week.exercises.toString(),
                            label = "упражнений",
                            color = MaterialTheme.success
                        )

                        val hours = week.duration / 3600
                        val minutes = (week.duration % 3600) / 60
                        WeekStatColumn(
                            value = "${hours}ч ${minutes}мин",
                            label = "время",
                            color = MaterialTheme.warning
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
            color = MaterialTheme.textSecondary
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
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
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
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.textPrimary
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
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "сессий",
                        fontSize = 12.sp,
                        color = MaterialTheme.textSecondary
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = exercise.total_repetitions.toString(),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.success
                    )
                    Text(
                        text = "повторений",
                        fontSize = 12.sp,
                        color = MaterialTheme.textSecondary
                    )
                }

                val minutes = exercise.total_duration / 60
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${minutes}мин",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.warning
                    )
                    Text(
                        text = "время",
                        fontSize = 12.sp,
                        color = MaterialTheme.textSecondary
                    )
                }
            }

            Divider(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                exercise.best_accuracy?.let {
                    Column {
                        Text(
                            text = "Лучшая точность",
                            fontSize = 12.sp,
                            color = MaterialTheme.textSecondary
                        )
                        Text(
                            text = String.format("%.1f%%", it),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.warning
                        )
                    }
                }

                exercise.avg_accuracy?.let {
                    Column {
                        Text(
                            text = "Средняя точность",
                            fontSize = 12.sp,
                            color = MaterialTheme.textSecondary
                        )
                        Text(
                            text = String.format("%.1f%%", it),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.success
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
                        tint = MaterialTheme.textSecondary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Последний раз: ${formatStatsDate(it)}",
                        fontSize = 12.sp,
                        color = MaterialTheme.textSecondary
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
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
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
                        text = formatStatsDate(workout.started_at),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.textPrimary
                    )
                }

                Card(
                    shape = MaterialTheme.shapes.small,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.success.copy(alpha = 0.1f)
                    )
                ) {
                    Text(
                        text = String.format("%.1f%%", workout.avg_accuracy),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.success,
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
                    icon = Icons.Default.Repeat,
                    color = MaterialTheme.colorScheme.primary
                )

                StatChip(
                    value = workout.total_reps.toString(),
                    label = "повторений",
                    icon = Icons.Default.RepeatOne,
                    color = MaterialTheme.success
                )

                val minutes = workout.total_duration / 60
                StatChip(
                    value = "${minutes}мин",
                    label = "время",
                    icon = Icons.Default.Timer,
                    color = MaterialTheme.warning
                )
            }

            if (workout.exercises.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Выполненные упражнения:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.textSecondary
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
                            fontSize = 12.sp,
                            color = MaterialTheme.textPrimary
                        )
                        Text(
                            text = "${ex.repetitions} раз",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
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
    icon: ImageVector,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = color
        )
        Spacer(modifier = Modifier.width(4.dp))
        Column {
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.textPrimary
            )
            Text(
                text = label,
                fontSize = 10.sp,
                color = MaterialTheme.textSecondary
            )
        }
    }
}

@Composable
fun EmptyStatsPlaceholder(message: String = "Нет данных для отображения") {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
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
            Text(
                text = "📊",
                fontSize = 48.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                fontSize = 16.sp,
                color = MaterialTheme.textSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

// Переименовал функцию, чтобы избежать конфликта с ProfileScreen
fun formatStatsDate(dateString: String): String {
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