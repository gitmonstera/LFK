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
import com.example.lfk.viewmodel.ExerciseListViewModel
import com.example.lfk.viewmodel.ExerciseViewModel
import com.example.lfk.views.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LFKClientTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val authViewModel: AuthViewModel = viewModel()
                    val exerciseViewModel: ExerciseViewModel = viewModel()
                    val exerciseListViewModel: ExerciseListViewModel = viewModel()

                    NavHost(
                        navController = navController,
                        startDestination = "main_menu"
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
                        composable("stats") {
                            StatsScreen(
                                navController = navController,
                                authToken = authViewModel.authToken.value
                            )
                        }
                        composable("exercise_selection") {
                            ExerciseSelectionScreen(
                                navController = navController,
                                authViewModel = authViewModel,
                                exerciseListViewModel = exerciseListViewModel
                            )
                        }
                        composable("exercise/{exerciseId}") { backStackEntry ->
                            val exerciseId = backStackEntry.arguments?.getString("exerciseId") ?: ""
                            ExerciseScreen(
                                navController = navController,
                                exerciseViewModel = exerciseViewModel,
                                exerciseId = exerciseId,
                                exerciseListViewModel = exerciseListViewModel,
                                authViewModel = authViewModel
                            )
                        }
                    }
                }
            }
        }
    }
}