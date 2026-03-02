package com.example.lfk.views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.lfk.models.ExerciseCategory
import com.example.lfk.models.ServerExercise  // Импортируем правильный класс
import  com.example.lfk.ui.theme.ColorUtils
import com.example.lfk.viewmodel.AuthViewModel
import com.example.lfk.viewmodel.ExerciseListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseSelectionScreen(
    navController: NavController,
    authViewModel: AuthViewModel,
    exerciseListViewModel: ExerciseListViewModel = viewModel()
) {
    val authToken by authViewModel.authToken.observeAsState()
    val categories by exerciseListViewModel.categories.observeAsState(emptyList())
    val isLoading by exerciseListViewModel.isLoading.observeAsState(false)
    val error by exerciseListViewModel.error.observeAsState()

    var selectedCategoryIndex by remember { mutableStateOf(0) }
    var showErrorDialog by remember { mutableStateOf(false) }

    // Загружаем упражнения при первом запуске
    LaunchedEffect(Unit) {
        if (authToken != null) {
            exerciseListViewModel.loadExercises(authToken!!)
        }
    }

    // Показываем диалог с ошибкой
    LaunchedEffect(error) {
        if (error != null) {
            showErrorDialog = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ВЫБОР УПРАЖНЕНИЯ") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        if (authToken != null) {
                            exerciseListViewModel.loadExercises(authToken!!)
                        }
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Обновить")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading) {
                // Индикатор загрузки
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (categories.isEmpty()) {
                // Пустое состояние
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "😴",
                        fontSize = 48.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Нет доступных упражнений",
                        fontSize = 18.sp,
                        color = Color.Gray
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Горизонтальные вкладки категорий
                    CategoryTabs(
                        categories = categories,
                        selectedIndex = selectedCategoryIndex,
                        onCategorySelected = { selectedCategoryIndex = it }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Список упражнений выбранной категории
                    if (categories.isNotEmpty() && selectedCategoryIndex < categories.size) {
                        ExerciseList(
                            category = categories[selectedCategoryIndex],
                            onExerciseClick = { exercise ->
                                navController.navigate("exercise/${exercise.exercise_id}")
                            }
                        )
                    }
                }
            }
        }
    }

    // Диалог с ошибкой
    if (showErrorDialog) {
        AlertDialog(
            onDismissRequest = {
                showErrorDialog = false
                exerciseListViewModel.clearError()
            },
            title = { Text("Ошибка") },
            text = { Text(error ?: "Неизвестная ошибка") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showErrorDialog = false
                        exerciseListViewModel.clearError()
                        if (authToken != null) {
                            exerciseListViewModel.loadExercises(authToken!!)
                        }
                    }
                ) {
                    Text("Повторить")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showErrorDialog = false
                    exerciseListViewModel.clearError()
                }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
fun CategoryTabs(
    categories: List<ExerciseCategory>,
    selectedIndex: Int,
    onCategorySelected: (Int) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(categories.size) { index ->
            val category = categories[index]
            val isSelected = index == selectedIndex

            CategoryChip(
                category = category,
                isSelected = isSelected,
                onClick = { onCategorySelected(index) }
            )
        }
    }
}

@Composable
fun CategoryChip(
    category: ExerciseCategory,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) {
        ColorUtils.parseCategoryColor(category.color)
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    val contentColor = if (isSelected) {
        Color.White
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = Modifier
            .wrapContentWidth()
            .height(48.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = ColorUtils.getCategoryIcon(category.icon),
                fontSize = 20.sp,
                color = contentColor
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = category.name,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = contentColor
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "(${category.exercises.size})",
                fontSize = 12.sp,
                color = contentColor.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun ExerciseList(
    category: ExerciseCategory,
    onExerciseClick: (ServerExercise) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(category.exercises) { exercise ->
            ExerciseCard(
                exercise = exercise,
                categoryColor = ColorUtils.parseCategoryColor(category.color),
                onClick = { onExerciseClick(exercise) }
            )
        }
    }
}

@Composable
fun ExerciseCard(
    exercise: ServerExercise,
    categoryColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 120.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Цветная полоска категории слева
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(categoryColor)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Контент карточки
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Заголовок с иконкой
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = exercise.category_icon ?: "",
                        fontSize = 24.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = exercise.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Описание
                Text(
                    text = exercise.description,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Мета информация
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Группы мышц
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        exercise.target_muscles.take(2).forEach { muscle ->
                            MuscleChip(muscle)
                        }
                        if (exercise.target_muscles.size > 2) {
                            Text(
                                text = "+${exercise.target_muscles.size - 2}",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }

                    // Сложность
                    DifficultyIndicator(level = exercise.difficulty_level)
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Длительность
                Text(
                    text = "⏱️ ${exercise.duration_seconds} сек",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun MuscleChip(muscle: String) {
    Card(
        shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        )
    ) {
        Text(
            text = muscle,
            fontSize = 10.sp,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun DifficultyIndicator(level: Int) {
    val color = ColorUtils.getDifficultyColor(level)
    val text = when (level) {
        1 -> "Легкое"
        2 -> "Несложное"
        3 -> "Среднее"
        4 -> "Сложное"
        5 -> "Очень сложное"
        else -> "Неизвестно"
    }

    Card(
        shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            color = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}