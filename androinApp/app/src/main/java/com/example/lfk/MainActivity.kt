package com.example.lfk

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.lfk.ui.CameraScreen
import com.example.lfk.ui.ExerciseListScreen
import com.example.lfk.ui.LoginScreen

class MainActivity : ComponentActivity() {

    private val apiClient = ApiClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LFKApp()
                }
            }
        }
    }

    @Composable
    fun LFKApp() {
        val navController = rememberNavController()

        NavHost(
            navController = navController,
            startDestination = "login"
        ) {
            composable("login") {
                LoginScreen(
                    apiClient = apiClient,
                    onLoginSuccess = { token, username ->
                        navController.navigate("exercises") {
                            popUpTo("login") { inclusive = true }
                        }
                    },
                    onNavigateToRegister = {
                        // В простой версии регистрация не реализована
                    }
                )
            }

            composable("exercises") {
                ExerciseListScreen(
                    apiClient = apiClient,
                    onExerciseSelected = { exerciseId, exerciseName ->
                        navController.navigate("camera/$exerciseId/$exerciseName")
                    },
                    onLogout = {
                        navController.navigate("login") {
                            popUpTo("exercises") { inclusive = true }
                        }
                    }
                )
            }

            composable("camera/{exerciseId}/{exerciseName}") { backStackEntry ->
                val exerciseId = backStackEntry.arguments?.getString("exerciseId") ?: "fist"
                val exerciseName = backStackEntry.arguments?.getString("exerciseName") ?: "Упражнение"

                CameraScreen(
                    apiClient = apiClient,
                    exerciseId = exerciseId,
                    exerciseName = exerciseName,
                    onBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}