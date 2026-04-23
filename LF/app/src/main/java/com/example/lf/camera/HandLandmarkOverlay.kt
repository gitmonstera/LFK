package com.example.lf.camera

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker

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