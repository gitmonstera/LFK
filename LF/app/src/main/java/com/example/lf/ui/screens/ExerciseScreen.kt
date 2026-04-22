package com.example.lf.ui.screens

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.SystemClock
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.lf.exercises.*
import com.example.lf.viewmodel.AuthViewModel
import com.example.lf.viewmodel.StatsViewModel
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker.HandLandmarkerOptions
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

// ==================== HelperFunctions (из референса) ====================
class HelperFunctions(val context: Context) {
    private val cameraFacing = CameraSelector.LENS_FACING_FRONT
    var handLandmarker: HandLandmarker? = null
    private var _detectionResults = mutableStateOf(HandDetectionResult())
    val detectionResults: State<HandDetectionResult> = _detectionResults

    // Колбэк для передачи результатов в ExerciseScreen
    var onResults: ((HandDetectionResult) -> Unit)? = null

    fun modelInitialization() {
        try {
            val baseOptions = BaseOptions.builder()
                .setDelegate(Delegate.GPU)
                .setModelAssetPath("hand_landmarker.task")
                .build()
            val optionsBuilder = HandLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setNumHands(1)
                .setMinHandDetectionConfidence(0.5f)
                .setMinHandPresenceConfidence(0.5f)
                .setMinTrackingConfidence(0.5f)
                .setRunningMode(RunningMode.LIVE_STREAM)
                .setResultListener { result, inputImage ->
                    val detectionResult = HandDetectionResult(
                        landmarks = result.landmarks(),
                        imageWidth = inputImage.width,
                        imageHeight = inputImage.height,
                        isFrontCamera = cameraFacing == CameraSelector.LENS_FACING_FRONT
                    )
                    _detectionResults.value = detectionResult
                    onResults?.invoke(detectionResult)
                }
                .setErrorListener { error ->
                    Log.e("HandDetection", "MediaPipe error: ${error.message}")
                }
            handLandmarker = HandLandmarker.createFromOptions(context, optionsBuilder.build())
            Log.d("HandDetection", "MediaPipe initialized")
        } catch (e: Exception) {
            Log.e("HandDetection", "Initialization error: ${e.message}")
        }
    }

    fun detectHand(imageProxy: ImageProxy) {
        val frameTime = SystemClock.uptimeMillis()
        val bitmapBuffer = Bitmap.createBitmap(
            imageProxy.width, imageProxy.height, Bitmap.Config.ARGB_8888
        )
        imageProxy.use { bitmapBuffer.copyPixelsFromBuffer(imageProxy.planes[0].buffer) }
        imageProxy.close()

        val matrix = Matrix().apply {
            postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
            if (cameraFacing == CameraSelector.LENS_FACING_FRONT) {
                postScale(-1f, 1f, imageProxy.width.toFloat(), imageProxy.height.toFloat())
            }
        }
        val rotatedBitmap = Bitmap.createBitmap(
            bitmapBuffer, 0, 0, bitmapBuffer.width, bitmapBuffer.height, matrix, true
        )
        val mpImage = BitmapImageBuilder(rotatedBitmap).build()
        handLandmarker?.detectAsync(mpImage, frameTime)
    }

    fun close() {
        handLandmarker?.close()
    }
}

// ==================== HandDetectionResult ====================
data class HandDetectionResult(
    val landmarks: List<List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark>> = emptyList(),
    val imageWidth: Int = 0,
    val imageHeight: Int = 0,
    val isFrontCamera: Boolean = true
)

// ==================== CameraPreview (из референса) ====================
@Composable
fun CameraPreview(
    helperFunctions: HelperFunctions,
    lensFacing: Int = CameraSelector.LENS_FACING_FRONT
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val preview = Preview.Builder().build()
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_START
        }
    }

    val imageAnalysis = remember {
        ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build()
            .also {
                it.setAnalyzer(ContextCompat.getMainExecutor(context)) { imageProxy ->
                    helperFunctions.detectHand(imageProxy)
                }
            }
    }

    val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

    LaunchedEffect(lensFacing) {
        val cameraProvider = context.getCameraProvider()
        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageAnalysis
            )
            preview.setSurfaceProvider(previewView.surfaceProvider)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    Box(Modifier.fillMaxSize()) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )
        HandLandmarkOverlay(
            detectionResults = helperFunctions.detectionResults.value,
            modifier = Modifier.fillMaxSize()
        )
    }
}

// ==================== HandLandmarkOverlay (из референса) ====================
@Composable
fun HandLandmarkOverlay(
    detectionResults: HandDetectionResult,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        if (detectionResults.landmarks.isEmpty()) return@Canvas

        val imageWidth = detectionResults.imageWidth
        val imageHeight = detectionResults.imageHeight
        if (imageWidth == 0 || imageHeight == 0) return@Canvas

        val scaleFactor = maxOf(
            size.width / imageWidth.toFloat(),
            size.height / imageHeight.toFloat()
        )

        detectionResults.landmarks.forEach { handLandmarks ->
            // Рисуем связи между точками
            HandLandmarker.HAND_CONNECTIONS.forEach { connection ->
                val startPoint = handLandmarks[connection.start()]
                val endPoint = handLandmarks[connection.end()]

                drawLine(
                    color = Color(0xFF00FF00),
                    start = Offset(
                        startPoint.x() * imageWidth * scaleFactor,
                        startPoint.y() * imageHeight * scaleFactor
                    ),
                    end = Offset(
                        endPoint.x() * imageWidth * scaleFactor,
                        endPoint.y() * imageHeight * scaleFactor
                    ),
                    strokeWidth = 8f
                )
            }

            // Рисуем точки
            handLandmarks.forEach { landmark ->
                drawCircle(
                    color = Color.Yellow,
                    radius = 8f,
                    center = Offset(
                        landmark.x() * imageWidth * scaleFactor,
                        landmark.y() * imageHeight * scaleFactor
                    )
                )
            }
        }
    }
}

// Вспомогательная функция для получения CameraProvider
private suspend fun Context.getCameraProvider(): ProcessCameraProvider = suspendCoroutine { cont ->
    ProcessCameraProvider.getInstance(this).also { cameraProvider ->
        cameraProvider.addListener(
            { cont.resume(cameraProvider.get()) },
            ContextCompat.getMainExecutor(this)
        )
    }
}

// ==================== ExerciseScreen ====================
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

    // Создаём HelperFunctions
    val helper = remember { HelperFunctions(context) }

    // Инициализируем модель
    LaunchedEffect(Unit) {
        helper.modelInitialization()
        addLog("MediaPipe инициализирован")
    }

    // Подписываемся на результаты детекции и обновляем UI
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

    // UI
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