package com.example.lfk.views

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.lfk.models.Exercise
import com.example.lfk.models.ExerciseData
import com.example.lfk.viewmodel.ExerciseViewModel

/**
 * Экран выбора упражнения
 */
@Composable
fun ExerciseSelectionScreen(
    navController: NavController,
    exerciseViewModel: ExerciseViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "ВЫБОР УПРАЖНЕНИЯ",
            fontSize = 24.sp,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        HorizontalDivider(
            modifier = Modifier.padding(bottom = 16.dp),
            thickness = DividerDefaults.Thickness,
            color = DividerDefaults.color
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(ExerciseData.exercises) { exercise ->
                ExerciseCard(
                    exercise = exercise,
                    onClick = {
                        exerciseViewModel.resetState()
                        navController.navigate("exercise/${exercise.type}")
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Назад в главное меню")
                }
            }
        }
    }
}

/**
 * Карточка упражнения
 */
@Composable
fun ExerciseCard(
    exercise: Exercise,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = exercise.name,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = exercise.description,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}