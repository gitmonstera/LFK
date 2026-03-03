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
import com.example.lfk.api.ApiClient
import com.example.lfk.models.StructuredData
import com.example.lfk.viewmodel.AuthViewModel
import com.example.lfk.viewmodel.ExerciseListViewModel
import com.example.lfk.viewmodel.ExerciseViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*
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
    val coroutineScope = rememberCoroutineScope()

    val authToken by authViewModel.authToken.observeAsState()
    val userInfo by authViewModel.userInfo.observeAsState()
    val exercise = exerciseListViewModel.getExerciseById(exerciseId)

    val processedImage by exerciseViewModel.processedImage.observeAsState()
    val handDetected by exerciseViewModel.handDetected.observeAsState(false)
    val raisedFingers by exerciseViewModel.raisedFingers.observeAsState(0)
    val fingerStates by exerciseViewModel.fingerStates.observeAsState(emptyList())
    val message by exerciseViewModel.message.observeAsState("")
    val structuredData by exerciseViewModel.structuredData.observeAsState()

    // Camera setup
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    // Для превью камеры
    val previewView = remember { PreviewView(context) }

    // Для захвата JPEG
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }

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

    // Local state
    var sessionId by remember { mutableStateOf<String?>(null) }
    var isSending by remember { mutableStateOf(false) }
    var webSocketConnected by remember { mutableStateOf(false) }
    var logs by remember { mutableStateOf(listOf<String>()) }
    var frameCount by remember { mutableStateOf(0) }
    var sendCount by remember { mutableStateOf(0) }

    fun addLog(message: String) {
        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val newLogs = logs.toMutableList()
        newLogs.add(0, "[$time] $message")
        if (newLogs.size > 10) newLogs.removeAt(newLogs.lastIndex)
        logs = newLogs
        Log.d("ExerciseScreen", message)
    }

    // Определяем тип упражнения
    val exerciseType = exercise?.exercise_id ?: exerciseId
    val wsUrl = if (authToken != null) {
        ApiClient.getWebSocketUrl(exerciseType, authToken!!)
    } else {
        ""
    }

    // Request permission
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Start workout and connect
    LaunchedEffect(authToken, hasCameraPermission) {
        if (authToken != null && hasCameraPermission) {
            addLog("Начинаем тренировку...")
            exerciseViewModel.startWorkout(authToken!!) { id ->
                sessionId = id
                addLog("Тренировка начата, ID: $id")

                exerciseViewModel.connectToExercise(exerciseType, wsUrl)

                coroutineScope.launch {
                    delay(2000)
                    webSocketConnected = exerciseViewModel.isConnected()
                    if (webSocketConnected) {
                        addLog("WebSocket подключен!")
                        isSending = true
                    } else {
                        addLog("WebSocket НЕ подключен!")
                    }
                }
            }
        }
    }

    // Setup camera with ImageCapture для получения JPEG напрямую
    // Setup camera with ImageCapture для получения JPEG напрямую
    LaunchedEffect(cameraProviderFuture, hasCameraPermission) {
        if (!hasCameraPermission) return@LaunchedEffect

        try {
            val cameraProvider = cameraProviderFuture.get()

            // Preview для отображения на экране
            val preview = Preview.Builder()
                .setTargetResolution(android.util.Size(640, 480))
                .build()

            preview.setSurfaceProvider(previewView.surfaceProvider)

            // ImageCapture для получения JPEG
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setTargetResolution(android.util.Size(640, 480))
                .build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageCapture
            )

            addLog("✅ Камера готова: 640x480")

        } catch (e: Exception) {
            addLog("❌ Ошибка камеры: ${e.message}")
        }
    }

// ОТДЕЛЬНЫЙ LaunchedEffect для запуска цикла отправки
    LaunchedEffect(isSending, webSocketConnected, imageCapture) {
        if (!isSending || !webSocketConnected || imageCapture == null) {
            addLog("⏳ Ожидание условий: isSending=$isSending, webSocket=$webSocketConnected, camera=${imageCapture != null}")
            return@LaunchedEffect
        }

        addLog("🚀 ЗАПУСК ЦИКЛА ОТПРАВКИ КАДРОВ")

        var localFrameCount = 0

        while (isSending && webSocketConnected) {
            try {
                localFrameCount++

                // Захватываем JPEG
                imageCapture?.takePicture(
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageCapturedCallback() {
                        override fun onCaptureSuccess(image: ImageProxy) {
                            try {
                                // Получаем JPEG напрямую
                                val buffer = image.planes[0].buffer
                                val jpegBytes = ByteArray(buffer.remaining())
                                buffer.get(jpegBytes)

                                // Отправляем каждый 3-й кадр как в Python
                                if (localFrameCount % 3 == 0) {
                                    exerciseViewModel.sendFrame(jpegBytes, exerciseType)
                                    sendCount++

                                    if (sendCount % 10 == 0) {
                                        addLog("📤 Отправлено $sendCount кадров (${jpegBytes.size} байт)")
                                    }
                                }

                                frameCount = localFrameCount

                            } catch (e: Exception) {
                                addLog("❌ Ошибка захвата: ${e.message}")
                            } finally {
                                image.close()
                            }
                        }

                        override fun onError(exception: ImageCaptureException) {
                            addLog("❌ Ошибка камеры: ${exception.message}")
                        }
                    }
                )

                // Задержка как в Python (~30 fps)
                delay(33)

            } catch (e: Exception) {
                addLog("❌ Ошибка в цикле: ${e.message}")
                delay(1000)
            }
        }

        addLog("⏹️ Цикл отправки остановлен")
    }

// Добавьте отладку в WebSocketManager.sendFrame

    // UI
    if (!hasCameraPermission) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Требуется разрешение на камеру")
            Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                Text("Разрешить")
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Заголовок
            Text(
                text = "🎯 ${exercise?.name ?: "Упражнение"}",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Статусы
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (webSocketConnected) "🟢 WebSocket OK" else "🔴 WebSocket...",
                    color = if (webSocketConnected) Color.Green else Color.Red
                )
                Text(text = "📸 $sendCount кадров")
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Оригинал с камеры
            Text("Оригинал с камеры:", fontSize = 14.sp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(Color.Black)
            ) {
                AndroidView(
                    factory = { previewView },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )

                // Наложение информации
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopStart)
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(4.dp)
                ) {
                    Column {
                        Text(
                            text = "👤 ${userInfo?.username ?: ""}",
                            color = Color.Green,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "📊 Кадров: $frameCount",
                            color = Color.Green,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Обработанный кадр
            Text("Обработанный кадр:", fontSize = 14.sp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(Color.Black)
            ) {
                if (processedImage != null) {
                    Image(
                        bitmap = processedImage!!.asImageBitmap(),
                        contentDescription = "Processed",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Нет данных", color = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Данные от сервера
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    Text("📊 Данные:", fontWeight = FontWeight.Bold)
                    Text("Рука: ${if (handDetected) "✅" else "❌"}")
                    Text("Пальцев: $raisedFingers/5")
                    Text("Сообщение: $message")

                    structuredData?.let {
                        Text("Состояние: ${it.state}")
                        Text("Цикл: ${it.current_cycle}/${it.total_cycles}")
                        it.countdown?.let { countdown ->
                            Text("Таймер: $countdown с")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Логи
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Black.copy(alpha = 0.1f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    Text("📋 ЛОГИ:", fontWeight = FontWeight.Bold)

                    if (logs.isEmpty()) {
                        Text("Нет логов", fontSize = 12.sp, color = Color.Gray)
                    } else {
                        logs.forEach { log ->
                            Text(
                                text = log,
                                fontSize = 10.sp,
                                color = Color.DarkGray,
                                modifier = Modifier.padding(vertical = 1.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Кнопка завершения
            Button(
                onClick = {
                    addLog("Завершение...")
                    isSending = false
                    exerciseViewModel.disconnect()
                    sessionId?.let {
                        exerciseViewModel.endWorkout(authToken!!, it)
                    }
                    navController.popBackStack()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Red
                )
            ) {
                Text("Завершить упражнение")
            }
        }
    }
}