package com.example.lfk.views

import android.Manifest
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import android.widget.FrameLayout
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavController
import com.example.lfk.api.ApiClient
import com.example.lfk.models.ServerExercise
import com.example.lfk.models.StructuredData
import com.example.lfk.viewmodel.AuthViewModel
import com.example.lfk.viewmodel.ExerciseListViewModel
import com.example.lfk.viewmodel.ExerciseViewModel
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

// Оптимизированные UI состояния - обновляются только когда реально нужно
data class UiState(
    val handDetected: Boolean = false,
    val raisedFingers: Int = 0,
    val fingerStates: List<Boolean> = emptyList(),
    val message: String = "",
    val structuredData: StructuredData? = null,
    val setsCompleted: Int = 0,
    val totalCycles: Int = 5,
    val sendCount: Int = 0,
    val frameCount: Int = 0,
    val currentProcessedImage: Bitmap? = null
)

@OptIn(ExperimentalGetImage::class, ExperimentalCoroutinesApi::class)
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
    val coroutineScope = rememberCoroutineScope()

    // Read-only стейты
    val authToken by authViewModel.authToken.observeAsState()
    val userInfo by authViewModel.userInfo.observeAsState()
    val exercise = exerciseListViewModel.getExerciseById(exerciseId)

    // ОПТИМИЗАЦИЯ: Один UI стейт вместо нескольких
    var uiState by remember { mutableStateOf(UiState()) }

    // Эти значения редко меняются - отдельные стейты
    var sessionId by remember { mutableStateOf<String?>(null) }
    var isSending by remember { mutableStateOf(false) }
    var webSocketConnected by remember { mutableStateOf(false) }
    var exerciseCompleted by remember { mutableStateOf(false) }
    var showCompletionDialog by remember { mutableStateOf(false) }
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }

    // ОПТИМИЗАЦИЯ: Тяжелые объекты в remember с фабрикой
    val cameraStuff = remember {
        CameraStuff(
            context = context,
            lifecycleOwner = lifecycleOwner
        )
    }

    val backgroundProcessor = remember {
        BackgroundProcessor(
            exerciseViewModel = exerciseViewModel,
            cameraStuff = cameraStuff,
            onStateUpdate = { newState ->
                // Обновляем UI стейт пачкой
                uiState = uiState.copy(
                    handDetected = newState.handDetected,
                    raisedFingers = newState.raisedFingers,
                    fingerStates = newState.fingerStates,
                    message = newState.message,
                    structuredData = newState.structuredData,
                    currentProcessedImage = newState.currentProcessedImage
                )
            },
            onCountUpdate = { sendCnt, frameCnt ->
                uiState = uiState.copy(
                    sendCount = sendCnt,
                    frameCount = frameCnt
                )
            },
            onCycleComplete = { completed, total ->
                uiState = uiState.copy(
                    setsCompleted = completed,
                    totalCycles = total
                )
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    val exerciseType = exercise?.exercise_id ?: exerciseId
    val wsUrl = remember(authToken, exerciseType) {
        if (authToken != null) ApiClient.getWebSocketUrl(exerciseType, authToken!!) else ""
    }

    // ОПТИМИЗАЦИЯ: Логи в отдельном эффекте
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // ОПТИМИЗАЦИЯ: Запуск воркера один раз
    LaunchedEffect(authToken, hasCameraPermission, wsUrl) {
        if (authToken != null && hasCameraPermission && wsUrl.isNotEmpty()) {
            backgroundProcessor.start(
                authToken = authToken!!,
                exerciseType = exerciseType,
                wsUrl = wsUrl,
                onConnected = {
                    isSending = true
                    webSocketConnected = true
                },
                onSessionId = { id -> sessionId = id },
                onExerciseCompleted = {
                    exerciseCompleted = true
                    showCompletionDialog = true
                }
            )
        }
    }

    // ОПТИМИЗАЦИЯ: Cleanup
    DisposableEffect(Unit) {
        onDispose {
            backgroundProcessor.stop()
            cameraStuff.cleanup()
        }
    }

    // ОПТИМИЗАЦИЯ: Derived state для UI
    val displayData by remember(uiState, webSocketConnected, userInfo) {
        derivedStateOf {
            DisplayData(
                username = userInfo?.username ?: "",
                handDetected = uiState.handDetected,
                raisedFingers = uiState.raisedFingers,
                fingerStates = uiState.fingerStates,
                message = uiState.message,
                structuredData = uiState.structuredData,
                setsCompleted = uiState.setsCompleted,
                totalCycles = uiState.totalCycles,
                sendCount = uiState.sendCount,
                frameCount = uiState.frameCount,
                processedImage = uiState.currentProcessedImage,
                webSocketConnected = webSocketConnected
            )
        }
    }

    // UI - максимально легкий, только отображение
    ExerciseScreenContent(
        displayData = displayData,
        exercise = exercise,
        previewView = cameraStuff.previewView,
        onFinish = {
            backgroundProcessor.stop()
            sessionId?.let { exerciseViewModel.endWorkout(authToken!!, it) }
            navController.popBackStack()
        },
        showCompletionDialog = showCompletionDialog,
        setsCompleted = uiState.setsCompleted,
        totalCycles = uiState.totalCycles,
        onDialogDismiss = {
            showCompletionDialog = false
            backgroundProcessor.stop()
            navController.popBackStack()
        }
    )
}

// ОПТИМИЗАЦИЯ: Отдельный класс для тяжелых операций с камерой
class CameraStuff(
    private val context: android.content.Context,
    lifecycleOwner: LifecycleOwner
) {
    val previewView = PreviewView(context)
    private val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
    private var imageCapture: ImageCapture? = null

    init {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val cameraProvider = cameraProviderFuture.get() // Не await, а get()
                val preview = Preview.Builder()
                    .setTargetResolution(android.util.Size(320, 240))
                    .build()
                preview.setSurfaceProvider(previewView.surfaceProvider)

                imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .setTargetResolution(android.util.Size(320, 240))
                    .build()

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageCapture
                )
            } catch (e: Exception) {
                Log.e("CameraStuff", "Camera error", e)
            }
        }
    }

    suspend fun captureFrame(): ByteArray? = suspendCoroutine { continuation ->
        imageCapture?.takePicture(
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    try {
                        val buffer = image.planes[0].buffer
                        val bytes = ByteArray(buffer.remaining())
                        buffer.get(bytes)
                        continuation.resume(bytes)
                    } catch (e: Exception) {
                        continuation.resume(null)
                    } finally {
                        image.close()
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    continuation.resume(null)
                }
            }
        ) ?: continuation.resume(null)
    }

    fun cleanup() {
        // Cleanup если нужно
    }
}

// ОПТИМИЗАЦИЯ: Фоновый процессор для всей логики
class BackgroundProcessor(
    private val exerciseViewModel: ExerciseViewModel,
    private val cameraStuff: CameraStuff,
    private val onStateUpdate: (UiState) -> Unit,
    private val onCountUpdate: (Int, Int) -> Unit,
    private val onCycleComplete: (Int, Int) -> Unit
) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val isRunning = AtomicBoolean(false)
    private val sendCounter = AtomicInteger(0)
    private val frameCounter = AtomicInteger(0)
    private val lastSendTime = AtomicLong(0)
    private val statsSavedForCycle = Collections.synchronizedSet(mutableSetOf<Int>())
    private var lastCycle = -1
    private var sessionId: String? = null

    private val sendInterval = 100L // 10 fps
    private val captureInterval = 33L // 30 fps

    fun start(
        authToken: String,
        exerciseType: String,
        wsUrl: String,
        onConnected: () -> Unit,
        onSessionId: (String) -> Unit,
        onExerciseCompleted: () -> Unit
    ) {
        if (!isRunning.compareAndSet(false, true)) return

        scope.launch {
            // 1. Старт тренировки
            exerciseViewModel.startWorkout(authToken) { id ->
                sessionId = id
                onSessionId(id)
            }

            delay(500)

            // 2. Подключение WebSocket
            exerciseViewModel.connectToExercise(exerciseType, wsUrl)
            delay(2000)

            if (exerciseViewModel.isConnected()) {
                onConnected()

                // 3. Главный цикл обработки
                launch { captureLoop(exerciseType) }
                launch { receiveLoop(authToken, exerciseType, onExerciseCompleted) }
            }
        }
    }

    private suspend fun captureLoop(exerciseType: String) {
        while (isRunning.get()) {
            val startTime = System.currentTimeMillis()
            val frameNumber = frameCounter.incrementAndGet()

            // Захват кадра
            val jpegBytes = cameraStuff.captureFrame()

            // Отправляем каждый 3-й кадр (как в Python)
            jpegBytes?.let { bytes ->
                if (frameNumber % 3 == 0) {
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastSendTime.get() >= sendInterval) {
                        // Отправка в фоне
                        exerciseViewModel.sendFrame(bytes, exerciseType)
                        lastSendTime.set(currentTime)
                        val sendCnt = sendCounter.incrementAndGet()
                        withContext(Dispatchers.Main) {
                            onCountUpdate(sendCnt, frameNumber)
                        }
                    }
                }
            }

            val elapsed = System.currentTimeMillis() - startTime
            if (elapsed < captureInterval) {
                delay(captureInterval - elapsed)
            }
        }
    }

    private suspend fun receiveLoop(
        authToken: String,
        exerciseType: String,
        onExerciseCompleted: () -> Unit
    ) {
        exerciseViewModel.webSocketMessages.collect { response ->
            withContext(Dispatchers.Default) {
                var processedBitmap: Bitmap? = null

                // Тяжелая обработка в фоне
                response.processed_frame?.let { base64 ->
                    try {
                        val decoded = Base64.decode(base64, Base64.DEFAULT)
                        processedBitmap = BitmapFactory.decodeByteArray(decoded, 0, decoded.size)
                    } catch (e: Exception) {
                        Log.e("BackgroundProcessor", "Decode error", e)
                    }
                }

                // Формируем новый стейт
                val newState = UiState(
                    handDetected = response.hand_detected,
                    raisedFingers = response.raised_fingers ?: 0,
                    fingerStates = response.finger_states ?: emptyList(),
                    message = response.message ?: "",
                    structuredData = response.structured,
                    currentProcessedImage = processedBitmap
                )

                // Обновляем UI в main потоке
                withContext(Dispatchers.Main) {
                    onStateUpdate(newState)
                }

                // Обработка циклов
                response.structured?.let { data ->
                    val currentCycle = data.current_cycle ?: 0
                    val total = data.total_cycles ?: 5

                    if (currentCycle > lastCycle && lastCycle >= 0) {
                        val completedCycle = lastCycle
                        if (completedCycle !in statsSavedForCycle && completedCycle > 0) {
                            statsSavedForCycle.add(completedCycle)
                            withContext(Dispatchers.Main) {
                                onCycleComplete(statsSavedForCycle.size, total)
                            }
                        }
                    }

                    if (data.completed == true) {
                        withContext(Dispatchers.Main) {
                            onExerciseCompleted()
                        }
                    }

                    lastCycle = currentCycle
                }
            }
        }
    }

    fun stop() {
        isRunning.set(false)
        scope.cancel()
        exerciseViewModel.disconnect()
    }
}

// ОПТИМИЗАЦИЯ: Легковесный data class для UI
data class DisplayData(
    val username: String,
    val handDetected: Boolean,
    val raisedFingers: Int,
    val fingerStates: List<Boolean>,
    val message: String,
    val structuredData: StructuredData?,
    val setsCompleted: Int,
    val totalCycles: Int,
    val sendCount: Int,
    val frameCount: Int,
    val processedImage: Bitmap?,
    val webSocketConnected: Boolean
)

// ОПТИМИЗАЦИЯ: Чистый UI без логики
@Composable
fun ExerciseScreenContent(
    displayData: DisplayData,
    exercise: ServerExercise?,
    previewView: PreviewView,
    onFinish: () -> Unit,
    showCompletionDialog: Boolean,
    setsCompleted: Int,
    totalCycles: Int,
    onDialogDismiss: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        HeaderSection(exercise?.name ?: "Упражнение")

        Spacer(modifier = Modifier.height(8.dp))

        StatusSection(
            webSocketConnected = displayData.webSocketConnected,
            sendCount = displayData.sendCount,
            setsCompleted = displayData.setsCompleted,
            totalCycles = displayData.totalCycles
        )

        Spacer(modifier = Modifier.height(8.dp))

        CameraPreviewSection(
            previewView = previewView,
            username = displayData.username,
            frameCount = displayData.frameCount
        )

        Spacer(modifier = Modifier.height(8.dp))

        ProcessedImageSection(
            processedImage = displayData.processedImage
        )

        Spacer(modifier = Modifier.height(8.dp))

        ServerDataSection(
            handDetected = displayData.handDetected,
            raisedFingers = displayData.raisedFingers,
            fingerStates = displayData.fingerStates,
            message = displayData.message,
            structuredData = displayData.structuredData,
            totalCycles = displayData.totalCycles
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onFinish,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
        ) {
            Text("Завершить упражнение")
        }
    }

    if (showCompletionDialog) {
        CompletionDialog(
            setsCompleted = setsCompleted,
            totalCycles = totalCycles,
            onDismiss = onDialogDismiss
        )
    }
}

// ОПТИМИЗАЦИЯ: Мелкие компоненты
@Composable
fun HeaderSection(title: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF6200EE).copy(alpha = 0.1f)
        )
    ) {
        Text(
            text = "🎯 $title",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )
    }
}

@Composable
fun StatusSection(
    webSocketConnected: Boolean,
    sendCount: Int,
    setsCompleted: Int,
    totalCycles: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(if (webSocketConnected) Color.Green else Color.Red)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (webSocketConnected) "WebSocket OK" else "WebSocket...")
            }
            Text("📸 $sendCount кадров", fontWeight = FontWeight.Bold)
            Text("📊 $setsCompleted/$totalCycles", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun CameraPreviewSection(
    previewView: PreviewView,
    username: String,
    frameCount: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.Black)
    ) {
        Box(modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
        ) {
            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopStart)
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(8.dp)
            ) {
                Column {
                    Text("👤 $username", color = Color.Green, fontSize = 12.sp)
                    Text("📸 Кадров: $frameCount", color = Color.Green, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun ProcessedImageSection(processedImage: Bitmap?) {
    Text("Обработанный кадр:", fontSize = 14.sp, fontWeight = FontWeight.Bold)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.Black)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            if (processedImage != null) {
                Image(
                    bitmap = processedImage.asImageBitmap(),
                    contentDescription = "Processed",
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Ожидание кадра...", color = Color.White)
                }
            }
        }
    }
}

@Composable
fun ServerDataSection(
    handDetected: Boolean,
    raisedFingers: Int,
    fingerStates: List<Boolean>,
    message: String,
    structuredData: StructuredData?,
    totalCycles: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (handDetected)
                Color(0xFF4CAF50).copy(alpha = 0.1f)
            else
                Color(0xFFF44336).copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text("📊 ДАННЫЕ С СЕРВЕРА:", fontWeight = FontWeight.Bold)
            Divider()

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Рука:")
                Text(
                    if (handDetected) "✅ В КАДРЕ" else "❌ НЕ ОБНАРУЖЕНА",
                    color = if (handDetected) Color.Green else Color.Red,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Пальцев:")
                Text("$raisedFingers/5", fontWeight = FontWeight.Bold)
            }

            if (fingerStates.isNotEmpty()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    val fingerNames = listOf("Б", "У", "С", "Бз", "М")
                    fingerStates.forEachIndexed { index, isRaised ->
                        Text(
                            "${fingerNames[index]} ${if (isRaised) "⬆️" else "⬇️"}",
                            color = if (isRaised) Color.Green else Color.Red,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Divider()
            Text("💬 $message", fontWeight = FontWeight.Bold)

            structuredData?.let {
                Divider()
                Text("📌 Состояние: ${it.state ?: "unknown"}")
                Text("🔄 Цикл: ${it.current_cycle}/$totalCycles")
                it.countdown?.let { countdown ->
                    Text("⏱️ Таймер: $countdown с")
                }
            }
        }
    }
}

@Composable
fun CompletionDialog(
    setsCompleted: Int,
    totalCycles: Int,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🎉 ", fontSize = 28.sp)
                Text("ПОЗДРАВЛЯЕМ!", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Упражнение успешно выполнено!")
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Выполнено циклов:")
                        Text(
                            "$setsCompleted/$totalCycles",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4CAF50)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Молодец! Так держать! 💪")
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
            ) {
                Text("ОТЛИЧНО!")
            }
        }
    )
}