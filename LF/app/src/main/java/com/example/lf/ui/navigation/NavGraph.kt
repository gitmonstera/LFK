package com.example.lf.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.lf.ui.screens.ExercisesScreen
import com.example.lf.ui.screens.LoginScreen
import com.example.lf.ui.screens.MainMenuScreen
import com.example.lf.ui.screens.ProfileScreen
import com.example.lf.ui.screens.RegisterScreen
import com.example.lf.ui.screens.StatsScreen
import com.example.lf.ui.screens.ExerciseScreen
import com.example.lf.viewmodel.AuthViewModel
import com.example.lf.viewmodel.ExerciseViewModel
import com.example.lf.viewmodel.StatsViewModel

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object MainMenu : Screen("main_menu")
    object Profile : Screen("profile")
    object Exercises : Screen("exercises")
    object Exercise : Screen("exercise/{exerciseId}") {
        fun passId(id: String) = "exercise/$id"
    }
    object Stats : Screen("stats")
}

@Composable
fun NavGraph(
    navController: NavHostController = rememberNavController(),
    authViewModel: AuthViewModel,
    exerciseViewModel: ExerciseViewModel,
    statsViewModel: StatsViewModel
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Login.route
    ) {
        composable(Screen.Login.route) {
            LoginScreen(navController, authViewModel)
        }
        composable(Screen.Register.route) {
            RegisterScreen(navController, authViewModel)
        }
        composable(Screen.MainMenu.route) {
            MainMenuScreen(
                navController = navController,
                authViewModel = authViewModel,
                statsViewModel = statsViewModel
            )
        }
        composable(Screen.Profile.route) {
            ProfileScreen(
                navController = navController,
                authViewModel = authViewModel,
                statsViewModel = statsViewModel
            )
        }
        composable(Screen.Exercises.route) {
            ExercisesScreen(
                navController = navController,
                statsViewModel = statsViewModel,
                authViewModel = authViewModel
            )
        }
        composable(Screen.Exercise.route) { backStackEntry ->
            val exerciseId = backStackEntry.arguments?.getString("exerciseId") ?: "fist"
            ExerciseScreen(
                navController = navController,
                exerciseId = exerciseId,
                authViewModel = authViewModel,
                statsViewModel = statsViewModel
            )
        }
        composable(Screen.Stats.route) {
            StatsScreen(
                navController = navController,
                statsViewModel = statsViewModel,
                authViewModel = authViewModel
            )
        }
    }
}