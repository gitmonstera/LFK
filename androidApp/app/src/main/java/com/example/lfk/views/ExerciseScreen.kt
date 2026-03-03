package com.example.lfk.views

import android.Manifest
import android.graphics.ImageFormat
import android.graphics.YuvImage
import android.util.Log
import android.widget.FrameLayout
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.lfk.models.ServerExercise
import com.example.lfk.models.StructuredData
import com.example.lfk.viewmodel.AuthViewModel
import com.example.lfk.viewmodel.ExerciseListViewModel
import com.example.lfk.viewmodel.ExerciseViewModel
import kotlinx.coroutines.delay
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors

@Composable
fun ExerciseScreen(
    navController: NavController,
    exerciseViewModel: ExerciseViewModel,
    exerciseId: String,
    exerciseListViewModel: ExerciseListViewModel,
    authViewModel: AuthViewModel
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val authToken by authViewModel.authToken.observeAsState()
    val userInfo by authViewModel.userInfo.observeAsState()
    val exercise = exerciseListViewModel.getExerciseById(exerciseId)

    val processedImage by exerciseViewModel.processedImage.observeAsState()
    val handDetected by exerciseViewModel.handDetected.observeAsState(false)
    val raisedFingers by exerciseViewModel.raisedFingers.observeAsState(0)
    val fingerStates by exerciseViewModel.fingerStates.observeAsState(emptyList())
    val message by exerciseViewModel.message.observeAsState("")
    val structuredData by exerciseViewModel.structuredData.observeAsState()
    val setsCompleted by exerciseViewModel.setsCompleted.observeAsState(0)
    val totalCycles by exerciseViewModel.totalCycles.observeAsState(5)
    val exerciseCompleted by exerciseViewModel.exerciseCompleted.observeAsState(false)

    // Camera setup
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    // Permission handling
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    // State
    var isSending by remember { mutableStateOf(false) }
    var sessionId by remember { mutableStateOf<String?>(null) }
    var showCompletionDialog by remember { mutableStateOf(false) }
    var lastCycle by remember { mutableStateOf(-1) }

    // Определяем тип упражнения из ID или используем ID как тип
    val exerciseType = exercise?.exercise_id ?: exerciseId

    // WebSocket URL
    val wsUrl = "ws://10.0.2.2:8080/ws/exercise/$exerciseType"

    // Request permission on first launch
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Start workout and connect WebSocket
    LaunchedEffect(authToken, hasCameraPermission) {
        if (authToken != null && hasCameraPermission) {
            exerciseViewModel.startWorkout(authToken!!) { id ->
                sessionId = id
                exerciseViewModel.connectToExercise(exerciseType, authToken!!, wsUrl)
                isSending = true
            }
        }
    }

    // Setup camera
    LaunchedEffect(cameraProviderFuture, hasCameraPermission) {
        if (!hasCameraPermission) return@LaunchedEffect

        val cameraProvider = cameraProviderFuture.get()
        val preview = Preview.Builder().build()

        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()

        val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageCapture
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Поток отправки изображений с интервалом 100 мс
    LaunchedEffect(isSending) {
        val frameInterval = 100L // 100 мс между кадрами
        var lastFrameTime = 0L

        while (isSending && exerciseViewModel.isConnected()) {
            val currentTime = System.currentTimeMillis()

            if (currentTime - lastFrameTime >= frameInterval) {
                imageCapture?.takePicture(
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageCapturedCallback() {
                        override fun onCaptureSuccess(image: ImageProxy) {
                            try {
                                val buffer = image.planes[0].buffer
                                val bytes = ByteArray(buffer.remaining())
                                buffer.get(bytes)

                                // Конвертируем в JPEG
                                val yuvImage = YuvImage(
                                    bytes,
                                    ImageFormat.NV21,
                                    image.width,
                                    image.height,
                                    null
                                )
                                val out = ByteArrayOutputStream()
                                yuvImage.compressToJpeg(
                                    android.graphics.Rect(0, 0, image.width, image.height),
                                    50,
                                    out
                                )
                                val jpegBytes = out.toByteArray()

                                // Отправляем через WebSocket
                                exerciseViewModel.sendFrame(jpegBytes, exerciseType)

                                image.close()
                            } catch (e: Exception) {
                                Log.e("ExerciseScreen", "Error capturing image", e)
                            }
                        }

                        override fun onError(exception: ImageCaptureException) {
                            Log.e("ExerciseScreen", "Image capture error", exception)
                        }
                    }
                )

                lastFrameTime = System.currentTimeMillis()
            }

            // Ждем немного перед следующей проверкой
            delay(10)
        }
    }

    // Track cycle completion for fist-palm exercise
    LaunchedEffect(structuredData) {
        if (exerciseType.contains("palm") && structuredData != null) {
            val currentCycle = structuredData?.current_cycle ?: 0

            // If cycle increased, previous cycle is completed
            if (currentCycle > lastCycle && lastCycle >= 0 && lastCycle > 0) {
                // Save statistics for completed cycle
                sessionId?.let { sid ->
                    exerciseViewModel.addExerciseSet(
                        token = authToken!!,
                        sessionId = sid,
                        exerciseId = exerciseType,
                        repetitions = 5,
                        duration = 60,
                        accuracy = 95.0
                    )
                    exerciseViewModel.incrementSetsCompleted()
                }
            }

            lastCycle = currentCycle
        }
    }

    // Handle exercise completion
    LaunchedEffect(exerciseCompleted) {
        if (exerciseCompleted) {
            // Save final set
            sessionId?.let { sid ->
                exerciseViewModel.addExerciseSet(
                    token = authToken!!,
                    sessionId = sid,
                    exerciseId = exerciseType,
                    repetitions = 5,
                    duration = 60,
                    accuracy = 95.0
                )

                // End workout
                exerciseViewModel.endWorkout(authToken!!, sid)

                // Show dialog
                showCompletionDialog = true
            }
        }
    }

    // UI
    if (!hasCameraPermission) {
        // Permission not granted
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Требуется разрешение на использование камеры")
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                Text("Предоставить разрешение")
            }
        }
    } else if (exercise == null) {
        // Exercise not loaded yet
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text("Загрузка упражнения...")
            }
        }
    } else {
        // Main UI
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Text(
                text = "🎯 ${exercise.name}",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // User info
            Text(
                text = "Пользователь: ${userInfo?.username ?: ""}",
                fontSize = 14.sp,
                color = Color.Gray
            )

            Text(
                text = "Подходов: $setsCompleted/$totalCycles",
                fontSize = 14.sp,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Camera preview or processed image
            if (processedImage != null) {
                Image(
                    bitmap = processedImage!!.asImageBitmap(),
                    contentDescription = "Processed frame",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
            } else {
                AndroidView(
                    factory = { ctx ->
                        PreviewView(ctx).apply {
                            layoutParams = FrameLayout.LayoutParams(
                                FrameLayout.LayoutParams.MATCH_PARENT,
                                FrameLayout.LayoutParams.MATCH_PARENT
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Hand status
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (handDetected)
                        Color(0xFF4CAF50).copy(alpha = 0.1f)
                    else
                        Color(0xFFF44336).copy(alpha = 0.1f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (handDetected) "🖐️" else "❌",
                        fontSize = 24.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (handDetected) "Рука в кадре" else "Рука не обнаружена",
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // For regular exercises - finger state
            if (!exerciseType.contains("palm")) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "👆 Пальцев поднято: $raisedFingers/5",
                            fontWeight = FontWeight.Bold
                        )

                        if (fingerStates.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                val fingerNames = listOf("Б", "У", "С", "Бз", "М")
                                fingerStates.forEachIndexed { index, isRaised ->
                                    Text(
                                        text = "${fingerNames[index]}${if (isRaised) "⬆️" else "⬇️"}",
                                        color = if (isRaised) Color.Green else Color.Red
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // For fist-palm exercise
            if (exerciseType.contains("palm") && structuredData != null) {
                Spacer(modifier = Modifier.height(8.dp))
                FistPalmProgress(structuredData!!)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Message from server
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        message.contains("✅") -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                        message.contains("❌") -> Color(0xFFF44336).copy(alpha = 0.1f)
                        else -> Color(0xFFFFC107).copy(alpha = 0.1f)
                    }
                )
            ) {
                Text(
                    text = message,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Back button
            OutlinedButton(
                onClick = {
                    isSending = false
                    exerciseViewModel.disconnect()
                    sessionId?.let {
                        exerciseViewModel.endWorkout(authToken!!, it)
                    }
                    navController.popBackStack()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Завершить упражнение")
            }
        }
    }

    // Completion dialog
    if (showCompletionDialog) {
        AlertDialog(
            onDismissRequest = { showCompletionDialog = false },
            title = { Text("Упражнение выполнено!") },
            text = {
                Text("Хотите выполнить это упражнение еще раз?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCompletionDialog = false
                        // Reset state and start again
                        exerciseViewModel.resetState()
                        exerciseViewModel.resetExercise(authToken!!, exerciseType)

                        // Start new workout
                        exerciseViewModel.startWorkout(authToken!!) { id ->
                            sessionId = id
                            exerciseViewModel.connectToExercise(exerciseType, authToken!!, wsUrl)
                            lastCycle = -1
                        }
                    }
                ) {
                    Text("Да")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showCompletionDialog = false
                        isSending = false
                        exerciseViewModel.disconnect()
                        navController.popBackStack()
                    }
                ) {
                    Text("Нет")
                }
            }
        )
    }
}

@Composable
fun FistPalmProgress(structuredData: StructuredData) {
    val steps = listOf(
        "Сожмите кулак" to "waiting_fist",
        "Держите кулак" to "holding_fist",
        "Раскройте ладонь" to "waiting_palm",
        "Держите ладонь" to "holding_palm"
    )

    val currentState = structuredData.state ?: "unknown"
    val currentCycle = structuredData.current_cycle ?: 0
    val totalCycles = structuredData.total_cycles ?: 5
    val countdown = structuredData.countdown
    val progress = structuredData.progress_percent ?: 0f

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Text(
                text = "📋 ПРОГРЕСС УПРАЖНЕНИЯ",
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Exercise steps
            steps.forEachIndexed { index, (name, state) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    when {
                        index < steps.indexOfFirst { it.second == currentState } -> {
                            Text("✅", color = Color.Green)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(name)
                        }
                        state == currentState -> {
                            Text("▶️", color = Color.Yellow)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (state.contains("holding") && countdown != null)
                                    "$name [$countdown с]"
                                else
                                    name,
                                color = Color.Yellow
                            )
                        }
                        else -> {
                            Text("⬜")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(name, color = Color.Gray)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text("🔄 Цикл: $currentCycle/$totalCycles")

            // ИСПРАВЛЕНО: используем currentState вместо state
            if (currentState.contains("holding") && countdown != null) {
                Spacer(modifier = Modifier.height(4.dp))

                // Progress bar
                LinearProgressIndicator(
                    progress = progress / 100f,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "⏱️ Осталось: ${countdown}c ${progress.toInt()}%",
                    fontSize = 12.sp
                )
            }
        }
    }
}