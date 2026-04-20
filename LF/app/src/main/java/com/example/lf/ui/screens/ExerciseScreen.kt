package com.example.lf.ui.screens

import android.Manifest
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.lf.exercises.FingerTouchingExercise
import com.example.lf.exercises.FistExercise
import com.example.lf.exercises.FistPalmExercise
import com.example.lf.viewmodel.AuthViewModel
import com.example.lf.viewmodel.StatsViewModel
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.framework.image.MediaImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker.HandLandmarkerOptions
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

@androidx.annotation.OptIn(ExperimentalGetImage::class)
@OptIn(ExperimentalGetImage::class)
@Composable
fun ExerciseScreen(
    navController: NavController,
    exerciseId: String,
    authViewModel: AuthViewModel,
    statsViewModel: StatsViewModel
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
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

    var processedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var progressPercent by remember { mutableStateOf(0) }
    var messageText by remember { mutableStateOf("") }
    var countdown by remember { mutableStateOf<Int?>(null) }
    var isCompleted by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var isMediaPipeReady by remember { mutableStateOf(false) }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted -> hasCameraPermission = isGranted }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Вспомогательная функция (не используется в основном потоке, оставлена для отладки)
    fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap? {
        return try {
            val bitmap = Bitmap.createBitmap(
                imageProxy.width,
                imageProxy.height,
                Bitmap.Config.ARGB_8888
            )
            val planes = imageProxy.planes
            val buffer = planes[0].buffer
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)
            val pixels = IntArray(imageProxy.width * imageProxy.height)
            for (i in pixels.indices) {
                val gray = bytes[i % bytes.size].toInt() and 0xFF
                pixels[i] = android.graphics.Color.rgb(gray, gray, gray)
            }
            bitmap.setPixels(pixels, 0, imageProxy.width, 0, 0, imageProxy.width, imageProxy.height)
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    LaunchedEffect(hasCameraPermission) {
        if (!hasCameraPermission) return@LaunchedEffect

        try {
            val options = HandLandmarkerOptions.builder()
                .setBaseOptions(BaseOptions.builder().build())
                .setRunningMode(RunningMode.LIVE_STREAM)
                .setNumHands(1)
                .setResultListener { result: HandLandmarkerResult, mpImage: MPImage? ->
                    val landmarks = result.landmarks()
                    if (landmarks.isNotEmpty()) {
                        val handLandmarks = landmarks[0]
                        val frameWidth = 480
                        val frameHeight = 360

                        try {
                            val (fingerStates, tipPositions) = exercise.getFingerStates(handLandmarks, frameWidth, frameHeight)
                            val (isCorrect, msg) = exercise.checkFingers(fingerStates, handLandmarks, frameWidth, frameHeight)

                            val bitmap = Bitmap.createBitmap(frameWidth, frameHeight, Bitmap.Config.ARGB_8888)
                            val canvas = Canvas(bitmap)
                            canvas.drawColor(Color.WHITE)

                            exercise.drawFeedback(
                                canvas, fingerStates, tipPositions, isCorrect, msg, frameWidth, frameHeight
                            )

                            processedBitmap = bitmap
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
                            e.printStackTrace()
                        }
                    } else {
                        if (!isCompleted) {
                            val bitmap = Bitmap.createBitmap(480, 360, Bitmap.Config.ARGB_8888)
                            val canvas = Canvas(bitmap)
                            canvas.drawColor(Color.BLACK)
                            val paint = Paint().apply {
                                color = Color.WHITE
                                textSize = 30f
                                textAlign = Paint.Align.CENTER
                            }
                            canvas.drawText("Рука не обнаружена", 240f, 180f, paint)
                            processedBitmap = bitmap
                            messageText = "Покажите руку перед камерой"
                        }
                    }
                }
                .build()

            val handLandmarker = HandLandmarker.createFromOptions(context, options)
            val cameraProvider = ProcessCameraProvider.getInstance(context).get()

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setTargetResolution(android.util.Size(480, 360))
                .build()

            val executor = Executors.newSingleThreadExecutor()

            imageAnalysis.setAnalyzer(executor) { imageProxy: ImageProxy ->
                val timestamp = System.currentTimeMillis()
                try {
                    val mediaImage = imageProxy.image
                    if (mediaImage != null) {
                        val mpImage = MediaImageBuilder(mediaImage).build()
                        handLandmarker.detectAsync(mpImage, timestamp)
                        mpImage.close()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    imageProxy.close()
                }
            }

            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA,
                imageAnalysis
            )

            isMediaPipeReady = true

        } catch (e: Exception) {
            e.printStackTrace()
            messageText = "Ошибка: ${e.message}"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color(0xFF0F0F2A))
            .padding(16.dp)
    ) {
        Text(
            text = "🎯 ${exercise.name}",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = androidx.compose.ui.graphics.Color.White,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = androidx.compose.ui.graphics.Color.Black
            )
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (processedBitmap != null) {
                    androidx.compose.foundation.Image(
                        bitmap = processedBitmap!!.asImageBitmap(),
                        contentDescription = "Processed",
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isMediaPipeReady) "Ожидание руки..." else "Запуск камеры...",
                            color = androidx.compose.ui.graphics.Color.White
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = androidx.compose.ui.graphics.Color(0xFF4CAF50).copy(alpha = 0.1f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "📊 Прогресс: $progressPercent%",
                    fontWeight = FontWeight.Bold,
                    color = androidx.compose.ui.graphics.Color.White
                )

                LinearProgressIndicator(
                    progress = progressPercent / 100f,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    color = androidx.compose.ui.graphics.Color(0xFF4CAF50)
                )

                Text(
                    text = messageText,
                    color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.8f)
                )

                countdown?.let {
                    Text(
                        text = "⏱️ Таймер: $it с",
                        color = androidx.compose.ui.graphics.Color(0xFFFF9800),
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { navController.popBackStack() },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = androidx.compose.ui.graphics.Color.Red
            )
        ) {
            Text("Завершить упражнение")
        }
    }
}