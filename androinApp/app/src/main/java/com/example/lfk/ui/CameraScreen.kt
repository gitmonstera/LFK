package com.example.lfk.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lfk.ApiClient
import com.example.lfk.WebSocketManager
import com.example.lfk.WebSocketCallback
import com.example.lfk.model.FrameFeedback
import com.example.lfk.model.StructuredData
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(
    apiClient: ApiClient,
    exerciseId: String,
    exerciseName: String,
    onBack: () -> Unit
) {
    var isConnected by remember { mutableStateOf(false) }
    var handDetected by remember { mutableStateOf(false) }
    var raisedFingers by remember { mutableStateOf(0) }
    var fingerStates by remember { mutableStateOf<List<Boolean>>(emptyList()) }
    var message by remember { mutableStateOf("Подключение...") }
    var processedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var structuredData by remember { mutableStateOf<StructuredData?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()
    val webSocketManager = remember {
        WebSocketManager(
            token = apiClient.authToken ?: "",
            exerciseType = exerciseId
        )
    }

    DisposableEffect(Unit) {
        webSocketManager.connect(object : WebSocketCallback {
            override fun onFeedback(feedback: FrameFeedback) {
                handDetected = feedback.handDetected
                raisedFingers = feedback.raisedFingers
                fingerStates = feedback.fingerStates
                message = feedback.message
                structuredData = feedback.structured

                if (feedback.processedFrame.isNotEmpty()) {
                    try {
                        val decoded = Base64.decode(feedback.processedFrame, Base64.DEFAULT)
                        val bitmap = BitmapFactory.decodeByteArray(decoded, 0, decoded.size)
                        processedBitmap = bitmap
                    } catch (e: Exception) {
                        // ignore
                    }
                }

                isConnected = true
            }

            override fun onError(errorMsg: String) {
                error = errorMsg
                isConnected = false
            }
        })

        onDispose {
            webSocketManager.disconnect()
        }
    }

    // Симуляция отправки кадров (в реальном приложении здесь будет камера)
    LaunchedEffect(isConnected) {
        if (isConnected) {
            var frameCount = 0
            while (true) {
                delay(300) // каждые 300 мс
                frameCount++

                // Здесь должна быть реальная отправка кадра с камеры
                // Пока отправляем пустой кадр для теста
                val dummyFrame = android.util.Base64.encodeToString(
                    "dummy".toByteArray(),
                    android.util.Base64.DEFAULT
                )
                webSocketManager.sendFrame(dummyFrame)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(exerciseName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (error != null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = error!!,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Button(onClick = onBack) {
                        Text("Назад")
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = "Статус: ${if (handDetected) "🖐️ Рука в кадре" else "❌ Рука не обнаружена"}",
                                    fontSize = 18.sp
                                )
                                Text(
                                    text = "Пальцев поднято: $raisedFingers/5",
                                    fontSize = 16.sp
                                )
                                if (fingerStates.isNotEmpty()) {
                                    Text(
                                        text = "Состояние: ${fingerStates.joinToString(" ") { if (it) "⬆️" else "⬇️" }}",
                                        fontSize = 16.sp
                                    )
                                }
                            }
                        }
                    }

                    if (structuredData != null) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Text(
                                        text = "Прогресс",
                                        fontSize = 18.sp,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )

                                    val steps = listOf(
                                        "Сожмите кулак" to "waiting_fist",
                                        "Держите кулак" to "holding_fist",
                                        "Раскройте ладонь" to "waiting_palm",
                                        "Держите ладонь" to "holding_palm"
                                    )

                                    val currentState = structuredData!!.state
                                    val currentStepIndex = steps.indexOfFirst { it.second == currentState }

                                    steps.forEachIndexed { index, (name, _) ->
                                        val status = when {
                                            index < currentStepIndex -> "✅"
                                            index == currentStepIndex -> "⏳"
                                            else -> "⬜"
                                        }
                                        Text("$status $name")
                                    }

                                    Text(
                                        text = "Цикл: ${structuredData!!.currentCycle}/${structuredData!!.totalCycles}",
                                        modifier = Modifier.padding(top = 8.dp)
                                    )

                                    if (structuredData!!.countdown != null) {
                                        Text(
                                            text = "⏱️ Осталось: ${structuredData!!.countdown}с",
                                            fontSize = 20.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = message,
                                modifier = Modifier.padding(16.dp),
                                fontSize = 16.sp
                            )
                        }
                    }

                    if (processedBitmap != null) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Image(
                                    bitmap = processedBitmap!!.asImageBitmap(),
                                    contentDescription = "Processed",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(4f / 3f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}