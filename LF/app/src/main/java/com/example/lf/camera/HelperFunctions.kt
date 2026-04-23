package com.example.lf.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.SystemClock
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageProxy
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker.HandLandmarkerOptions
import kotlin.use

class HelperFunctions(val context: Context) {
    private val cameraFacing = CameraSelector.LENS_FACING_FRONT
    var handLandmarker: HandLandmarker? = null
    private var _detectionResults = mutableStateOf(HandDetectionResult())
    val detectionResults: State<HandDetectionResult> = _detectionResults

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