package com.example.lf.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.lf.api.ExerciseResponse
import com.example.lf.ui.components.GlassCard
import com.example.lf.ui.theme.*
import com.example.lf.viewmodel.AuthViewModel
import com.example.lf.viewmodel.StatsViewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState

@Composable
fun ExercisesScreen(
    navController: NavController,
    statsViewModel: StatsViewModel,
    authViewModel: AuthViewModel
) {
    val token by authViewModel.authToken.collectAsStateWithLifecycle()
    val exercises by statsViewModel.exercises.observeAsState(emptyList())
    val isLoading by statsViewModel.isLoading.observeAsState(false)

    LaunchedEffect(token) {
        token?.let {
            statsViewModel.loadExercises(it)
        }
    }

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
                .background(Primary.copy(alpha = 0.05f), RoundedCornerShape(50.dp))
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
                Text(
                    text = "Упражнения",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
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
            } else if (exercises.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("😴", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Нет доступных упражнений", color = TextSecondary)
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(exercises) { exercise ->
                        ExerciseCard(
                            exercise = exercise,
                            onClick = {
                                navController.navigate("exercise/${exercise.exerciseId}")
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ExerciseCard(
    exercise: ExerciseResponse,
    onClick: () -> Unit
) {
    val color = when (exercise.category.lowercase()) {
        "руки" -> Primary
        "шея" -> Tertiary
        "моторика" -> Secondary
        else -> Primary
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(color.copy(alpha = 0.15f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = exercise.categoryIcon ?: "🏋️",
                    fontSize = 32.sp
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = exercise.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    text = exercise.description.take(60) + if (exercise.description.length > 60) "..." else "",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DifficultyBadge(level = exercise.difficultyLevel)
                    DurationBadge(seconds = exercise.durationSeconds)
                }

                if (exercise.applicableCodes.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_medical),
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = exercise.applicableCodes.take(3).joinToString(" · "),
                            fontSize = 11.sp,
                            color = TextSecondary,
                            maxLines = 1
                        )
                        if (exercise.applicableCodes.size > 3) {
                            Text(
                                text = "+${exercise.applicableCodes.size - 3}",
                                fontSize = 10.sp,
                                color = Primary
                            )
                        }
                    }
                }
            }

            IconButton(onClick = onClick) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_play),
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Composable
fun DifficultyBadge(level: Int) {
    val (color, text) = when (level) {
        1 -> Pair(Success, "Легкое")
        2 -> Pair(Warning, "Среднее")
        3 -> Pair(Error, "Сложное")
        else -> Pair(TextSecondary, "Неизвестно")
    }

    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun DurationBadge(seconds: Int) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = Primary.copy(alpha = 0.15f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_schedule),
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(10.dp)
            )
            Text(
                text = "$seconds сек",
                fontSize = 10.sp,
                color = Primary
            )
        }
    }
}