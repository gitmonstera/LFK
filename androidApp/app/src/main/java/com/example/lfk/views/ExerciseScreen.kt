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

    // Данные из ViewModel (read-only)
    val processedImage by exerciseViewModel.processedImage.observeAsState()
    val handDetected by exerciseViewModel.handDetected.observeAsState(false)
    val raisedFingers by exerciseViewModel.raisedFingers.observeAsState(0)
    val fingerStates by exerciseViewModel.fingerStates.observeAsState(emptyList())
    val message by exerciseViewModel.message.observeAsState("")
    val structuredData by exerciseViewModel.structuredData.observeAsState()

    // Camera setup
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val previewView = remember { PreviewView(context) }
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

    // Local mutable state
    var sessionId by remember { mutableStateOf<String?>(null) }
    var isSending by remember { mutableStateOf(false) }
    var webSocketConnected by remember { mutableStateOf(false) }
    var logs by remember { mutableStateOf(listOf<String>()) }
    var frameCount by remember { mutableStateOf(0) }
    var sendCount by remember { mutableStateOf(0) }
    var setsCompleted by remember { mutableStateOf(0) }
    var lastCycle by remember { mutableStateOf(-1) }
    var totalCycles by remember { mutableStateOf(5) }
    var exerciseCompleted by remember { mutableStateOf(false) }
    var showCompletionDialog by remember { mutableStateOf(false) }
    var currentProcessedImage by remember { mutableStateOf<Bitmap?>(null) }

    val statsSavedForCycle = remember { mutableSetOf<Int>() }

    fun addLog(message: String) {
        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val newLogs = logs.toMutableList()
        newLogs.add(0, "[$time] $message")
        if (newLogs.size > 10) newLogs.removeAt(newLogs.lastIndex)
        logs = newLogs
        Log.d("ExerciseScreen", message)
    }

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
            addLog("🔄 Начинаем тренировку...")
            exerciseViewModel.startWorkout(authToken!!) { id ->
                sessionId = id
                addLog("✅ Тренировка начата, ID: $id")

                exerciseViewModel.connectToExercise(exerciseType, wsUrl)

                coroutineScope.launch {
                    delay(2000)
                    webSocketConnected = exerciseViewModel.isConnected()
                    if (webSocketConnected) {
                        addLog("✅ WebSocket подключен!")
                        isSending = true
                    } else {
                        addLog("❌ WebSocket НЕ подключен!")
                    }
                }
            }
        }
    }

    // Setup camera
    LaunchedEffect(cameraProviderFuture, hasCameraPermission) {
        if (!hasCameraPermission) return@LaunchedEffect

        try {
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .setTargetResolution(android.util.Size(640, 480))
                .build()
            preview.setSurfaceProvider(previewView.surfaceProvider)

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

    // Цикл отправки кадров
    LaunchedEffect(isSending, webSocketConnected, imageCapture, exerciseCompleted) {
        if (!isSending || !webSocketConnected || imageCapture == null || exerciseCompleted) {
            return@LaunchedEffect
        }

        addLog("🚀 ЗАПУСК ЦИКЛА ОТПРАВКИ КАДРОВ")

        var localFrameCount = 0

        while (isSending && webSocketConnected && !exerciseCompleted) {
            try {
                localFrameCount++

                imageCapture?.takePicture(
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageCapturedCallback() {
                        override fun onCaptureSuccess(image: ImageProxy) {
                            try {
                                val buffer = image.planes[0].buffer
                                val jpegBytes = ByteArray(buffer.remaining())
                                buffer.get(jpegBytes)

                                if (localFrameCount % 3 == 0 && !exerciseCompleted) {
                                    exerciseViewModel.sendFrame(jpegBytes, exerciseType)
                                    sendCount = sendCount + 1

                                    if (sendCount % 10 == 0) {
                                        addLog("📤 Отправлено $sendCount кадров")
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

                delay(33)

            } catch (e: Exception) {
                addLog("❌ Ошибка в цикле: ${e.message}")
                delay(1000)
            }
        }

        addLog("⏹️ Цикл отправки остановлен")
    }

    // Подписка на сообщения WebSocket
    LaunchedEffect(webSocketConnected) {
        if (!webSocketConnected) return@LaunchedEffect

        addLog("📡 Подписка на сообщения...")

        exerciseViewModel.webSocketMessages.collect { response ->
            // Обновляем ТОЛЬКО локальные mutableState переменные
            // handDetected, raisedFingers, fingerStates, message - ЭТО VAL, ИХ НЕЛЬЗЯ МЕНЯТЬ!
            // Вместо этого создадим локальные переменные для UI

            // Для отображения используем данные напрямую из response
            val currentHandDetected = response.hand_detected
            val currentRaisedFingers = response.raised_fingers ?: 0
            val currentFingerStates = response.finger_states ?: emptyList()
            val currentMessage = response.message ?: ""

            // Обновляем изображение (это локальная mutableState переменная)
            response.processed_frame?.let { base64Frame ->
                try {
                    val decodedBytes = Base64.decode(base64Frame, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                    currentProcessedImage = bitmap
                } catch (e: Exception) {
                    Log.e("ExerciseScreen", "Error decoding image", e)
                }
            }

            // Получаем структурированные данные
            val data = response.structured

            if (data != null) {
                val currentCycle = data.current_cycle ?: 0
                val newTotalCycles = data.total_cycles ?: 5
                val completed = data.completed ?: false

                totalCycles = newTotalCycles

                // Отслеживание завершенных циклов
                if (currentCycle > lastCycle && lastCycle >= 0) {
                    val completedCycle = lastCycle
                    if (completedCycle !in statsSavedForCycle && completedCycle > 0) {
                        statsSavedForCycle.add(completedCycle)
                        setsCompleted = statsSavedForCycle.size
                        addLog("✅ ЦИКЛ $completedCycle/$newTotalCycles ЗАВЕРШЕН!")
                    }
                }

                // КРИТИЧЕСКИ ВАЖНО: при завершении упражнения НЕМЕДЛЕННО закрываем всё!
                if (completed && !exerciseCompleted) {
                    addLog("🎯 УПРАЖНЕНИЕ ВЫПОЛНЕНО! НЕМЕДЛЕННОЕ ЗАКРЫТИЕ...")

                    // Сохраняем последний цикл
                    if (newTotalCycles !in statsSavedForCycle) {
                        statsSavedForCycle.add(newTotalCycles)
                        setsCompleted = statsSavedForCycle.size
                        addLog("✅ ЦИКЛ $newTotalCycles/$newTotalCycles ЗАВЕРШЕН!")
                    }

                    // Сохраняем статистику
                    sessionId?.let { sid ->
                        exerciseViewModel.addExerciseSet(
                            token = authToken!!,
                            sessionId = sid,
                            exerciseId = exerciseType,
                            repetitions = 5,
                            duration = 60,
                            accuracy = 95.0
                        )
                    }

                    // Завершаем тренировку на сервере
                    sessionId?.let { sid ->
                        exerciseViewModel.endWorkout(authToken!!, sid)
                    }

                    // НЕМЕДЛЕННО закрываем WebSocket и останавливаем отправку
                    exerciseViewModel.disconnect()
                    webSocketConnected = false
                    isSending = false
                    exerciseCompleted = true

                    // Показываем диалог
                    showCompletionDialog = true
                }

                lastCycle = currentCycle
            }

            if (sendCount % 5 == 0) {
                addLog("📥 Получены данные: рука=$currentHandDetected, сообщение=${currentMessage.take(20)}")
            }
        }
    }

    // Функция завершения
    fun finishWorkout() {
        addLog("Завершение тренировки...")
        isSending = false
        exerciseViewModel.disconnect()
        coroutineScope.launch {
            navController.popBackStack()
        }
    }

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
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF6200EE).copy(alpha = 0.1f)
                )
            ) {
                Text(
                    text = "🎯 ${exercise?.name ?: "Упражнение"}",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Статусы
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFF5F5F5)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(
                                    color = if (webSocketConnected) Color.Green else Color.Red
                                )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (webSocketConnected) "WebSocket OK" else "WebSocket...",
                            fontSize = 14.sp
                        )
                    }

                    Text(
                        text = "📸 $sendCount кадров",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "📊 $setsCompleted/$totalCycles",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6200EE)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Оригинал с камеры
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Black
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
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
                            .background(Color.Black.copy(alpha = 0.6f))
                            .padding(8.dp)
                    ) {
                        Column {
                            Text(
                                text = "👤 ${userInfo?.username ?: ""}",
                                color = Color.Green,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "📸 Кадров: $frameCount",
                                color = Color.Green,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Обработанный кадр
            Text(
                text = "Обработанный кадр:",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Black
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    if (currentProcessedImage != null) {
                        Image(
                            bitmap = currentProcessedImage!!.asImageBitmap(),
                            contentDescription = "Processed",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                        )
                    } else if (processedImage != null) {
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
                            Text(
                                text = "Ожидание кадра...",
                                color = Color.White,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Данные от сервера
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
                    Divider(modifier = Modifier.padding(vertical = 4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Рука:")
                        Text(
                            text = if (handDetected) "✅ В КАДРЕ" else "❌ НЕ ОБНАРУЖЕНА",
                            color = if (handDetected) Color.Green else Color.Red,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Пальцев:")
                        Text(
                            text = "$raisedFingers/5",
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (fingerStates.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            val fingerNames = listOf("Б", "У", "С", "Бз", "М")
                            fingerStates.forEachIndexed { index, isRaised ->
                                Text(
                                    text = "${fingerNames[index]} ${if (isRaised) "⬆️" else "⬇️"}",
                                    color = if (isRaised) Color.Green else Color.Red,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    Divider(modifier = Modifier.padding(vertical = 4.dp))

                    Text(
                        text = "💬 $message",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )

                    structuredData?.let {
                        Divider(modifier = Modifier.padding(vertical = 4.dp))
                        Text("📌 Состояние: ${it.state ?: "unknown"}")
                        Text("🔄 Цикл: ${it.current_cycle}/$totalCycles")
                        it.countdown?.let { countdown ->
                            Text("⏱️ Таймер: $countdown с")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Логи
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Black.copy(alpha = 0.05f)
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
                        logs.take(8).forEach { log ->
                            Text(
                                text = log,
                                fontSize = 10.sp,
                                color = Color.DarkGray,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Кнопка завершения
            Button(
                onClick = { finishWorkout() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Red
                )
            ) {
                Text(
                    text = "Завершить упражнение",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    // Диалог завершения упражнения
    if (showCompletionDialog) {
        AlertDialog(
            onDismissRequest = {
                finishWorkout()
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "🎉 ",
                        fontSize = 28.sp
                    )
                    Text(
                        text = "ПОЗДРАВЛЯЕМ!",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50)
                    )
                }
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Упражнение успешно выполнено!",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

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
                            Text(
                                text = "Выполнено циклов:",
                                fontSize = 14.sp
                            )
                            Text(
                                text = "$setsCompleted/$totalCycles",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4CAF50)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Молодец! Так держать! 💪",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { finishWorkout() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    )
                ) {
                    Text(
                        text = "ОТЛИЧНО!",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        )
    }
}