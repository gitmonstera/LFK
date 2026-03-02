package com.example.lfk

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.lfk.ui.theme.LFKClientTheme
import com.example.lfk.viewmodel.AuthViewModel
import com.example.lfk.viewmodel.ExerciseViewModel
import com.example.lfk.views.*

/**
 * Главная активность приложения.
 * Управляет навигацией между экранами и инициализацией ViewModel.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LFKClientTheme {
                // Surface - контейнер для всего контента
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Навигация
                    val navController = rememberNavController()

                    // ViewModel'и
                    val authViewModel: AuthViewModel = viewModel()
                    val exerciseViewModel: ExerciseViewModel = viewModel()

                    // Настройка навигационных маршрутов
                    NavHost(
                        navController = navController,
                        startDestination = "main_menu"  // Стартовый экран
                    ) {
                        composable("main_menu") {
                            MainMenuScreen(
                                navController = navController,
                                authViewModel = authViewModel
                            )
                        }
                        composable("login") {
                            LoginScreen(
                                navController = navController,
                                authViewModel = authViewModel
                            )
                        }
                        composable("register") {
                            RegisterScreen(
                                navController = navController,
                                authViewModel = authViewModel
                            )
                        }
                        composable("profile") {
                            ProfileScreen(
                                navController = navController,
                                authViewModel = authViewModel
                            )
                        }
                        composable("exercise_selection") {
                            ExerciseSelectionScreen(
                                navController = navController,
                                exerciseViewModel = exerciseViewModel
                            )
                        }
                        composable("exercise/{exerciseType}") { backStackEntry ->
                            val exerciseType = backStackEntry.arguments?.getString("exerciseType") ?: "fist"
                            ExerciseScreen(
                                navController = navController,
                                exerciseViewModel = exerciseViewModel,
                                exerciseType = exerciseType,
                                authViewModel = authViewModel
                            )
                        }
                    }
                }
            }
        }
    }
}