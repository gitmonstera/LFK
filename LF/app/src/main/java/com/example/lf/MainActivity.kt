package com.example.lf

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.example.lf.ui.navigation.NavGraph
import com.example.lf.ui.theme.LFTheme
import com.example.lf.viewmodel.AuthViewModel
import com.example.lf.viewmodel.ExerciseViewModel
import com.example.lf.viewmodel.StatsViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LFTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    val authViewModel: AuthViewModel = viewModel(
                        factory = AuthViewModelFactory(applicationContext)
                    )
                    val exerciseViewModel: ExerciseViewModel = viewModel()
                    val statsViewModel: StatsViewModel = viewModel()

                    NavGraph(
                        navController = navController,
                        authViewModel = authViewModel,
                        exerciseViewModel = exerciseViewModel,
                        statsViewModel = statsViewModel
                    )
                }
            }
        }
    }
}

// Factory для AuthViewModel с контекстом
class AuthViewModelFactory(private val context: android.content.Context) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}