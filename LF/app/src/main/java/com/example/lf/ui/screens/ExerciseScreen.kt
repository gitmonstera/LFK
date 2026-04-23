package com.example.lf.ui.screens

import android.Manifest
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.lf.camera.CameraPreview
import com.example.lf.camera.HelperFunctions
import com.example.lf.exercises.arm.FingerTouchingExercise
import com.example.lf.exercises.arm.FistExercise
import com.example.lf.exercises.arm.FistPalmExercise
import com.example.lf.viewmodel.AuthViewModel
import com.example.lf.viewmodel.StatsViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalGetImage::class)
@Composable
fun ExerciseScreen(
    navController: NavController,
    exerciseId: String,
    authViewModel: AuthViewModel,
    statsViewModel: StatsViewModel
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val token by authViewModel.authToken.collectAsStateWithLifecycle()

    val exercise = remember(exerciseId) {
        when (exerciseId) {
            "finger-touching" -> FingerTouchingExercise()
            "fist" -> FistExercise()
            "fist-palm" -> FistPalmExercise()
            else -> FingerTouchingExercise()
        }
    }

    var progressPercent by remember { mutableStateOf(0) }
    var messageText by remember { mutableStateOf("") }
    var countdown by remember { mutableStateOf<Int?>(null) }
    var isCompleted by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    var debugLogs by remember { mutableStateOf(listOf<String>()) }
    fun addLog(msg: String) {
        Log.d("ExerciseScreen", msg)
        debugLogs = (debugLogs + msg).takeLast(15)
    }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        addLog("Разрешение камеры: $isGranted")
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val helper = remember { HelperFunctions(context) }

    LaunchedEffect(Unit) {
        helper.modelInitialization()
        addLog("MediaPipe инициализирован")
    }

    LaunchedEffect(helper) {
        helper.onResults = { result ->
            if (result.landmarks.isNotEmpty()) {
                val hand = result.landmarks[0]
                val width = result.imageWidth
                val height = result.imageHeight
                if (width > 0 && height > 0) {
                    try {
                        val (fingerStates, tipPositions) = exercise.getFingerStates(hand, width, height)
                        val (isCorrect, msg) = exercise.checkFingers(fingerStates, hand, width, height)

                        progressPercent = exercise.getProgressPercent()
                        messageText = msg
                        val structured = exercise.getStructuredData()
                        countdown = structured["countdown"] as? Int

                        if (structured["completed"] == true && !isCompleted && !isSaving) {
                            isCompleted = true
                            isSaving = true
                            coroutineScope.launch {
                                token?.let {
                                    statsViewModel.saveExerciseResult(
                                        token = it,
                                        exerciseId = exerciseId,
                                        repetitions = exercise.totalCycles * 4,
                                        duration = 60,
                                        accuracy = progressPercent.toFloat()
                                    )
                                }
                                delay(1500)
                                navController.popBackStack()
                            }
                        }
                    } catch (e: Exception) {
                        addLog("Ошибка упражнения: ${e.message}")
                    }
                }
            } else {
                messageText = "Рука не обнаружена"
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F2A))
            .padding(16.dp)
    ) {
        Text(
            text = "🎯 ${exercise.name}",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black)
        ) {
            if (hasCameraPermission) {
                CameraPreview(helperFunctions = helper, lensFacing = CameraSelector.LENS_FACING_FRONT)
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Нет доступа к камере", color = Color.White)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("📊 Прогресс: $progressPercent%", color = Color.White)
                LinearProgressIndicator(
                    progress = { progressPercent / 100f },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    color = Color(0xFF4CAF50)
                )
                Text(messageText, color = Color.White.copy(alpha = 0.8f))
                countdown?.let { Text("⏱️ Таймер: $it с", color = Color(0xFFFF9800), fontSize = 12.sp) }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth().height(120.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.8f)),
            shape = RoundedCornerShape(8.dp)
        ) {
            LazyColumn(reverseLayout = true, modifier = Modifier.fillMaxSize().padding(8.dp)) {
                item { Text("🐞 Отладка:", color = Color.Yellow, fontSize = 10.sp) }
                items(debugLogs) { log -> Text(log, color = Color.White.copy(alpha = 0.9f), fontSize = 10.sp) }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                helper.close()
                navController.popBackStack()
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
        ) {
            Text("Завершить")
        }
    }
}