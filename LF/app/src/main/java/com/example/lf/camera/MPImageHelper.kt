package com.example.lf.camera

import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.media.Image
import com.google.mediapipe.tasks.vision.core.RunningMode
import java.nio.ByteBuffer

object MPImageHelper {

    /**
     * Конвертирует android.media.Image в MPImage для MediaPipe
     */
    fun imageToMPImage(image: Image): Any? {
        return try {
            val planes = image.planes
            val buffer = planes[0].buffer
            val pixelStride = planes[0].pixelStride
            val rowStride = planes[0].rowStride
            val width = image.width
            val height = image.height

            // Создаем Bitmap для RGB изображения
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

            if (image.format == ImageFormat.YUV_420_888) {
                // Конвертируем YUV в RGB
                val yBuffer = planes[0].buffer
                val uBuffer = planes[1].buffer
                val vBuffer = planes[2].buffer

                val ySize = yBuffer.remaining()
                val uSize = uBuffer.remaining()
                val vSize = vBuffer.remaining()

                val yuvBytes = ByteArray(ySize + uSize + vSize)
                yBuffer.get(yuvBytes, 0, ySize)
                uBuffer.get(yuvBytes, ySize, uSize)
                vBuffer.get(yuvBytes, ySize + uSize, vSize)

                val rgba = IntArray(width * height)
                var yIndex = 0
                var uvIndex = ySize

                for (y in 0 until height) {
                    for (x in 0 until width) {
                        val yVal = (yuvBytes[yIndex++].toInt() and 0xFF) - 16
                        val uVal = (yuvBytes[uvIndex].toInt() and 0xFF) - 128
                        val vVal = (yuvBytes[uvIndex + 1].toInt() and 0xFF) - 128
                        uvIndex += 2

                        var r = (1.164f * yVal + 1.596f * vVal).toInt()
                        var g = (1.164f * yVal - 0.813f * vVal - 0.391f * uVal).toInt()
                        var b = (1.164f * yVal + 2.018f * uVal).toInt()

                        r = r.coerceIn(0, 255)
                        g = g.coerceIn(0, 255)
                        b = b.coerceIn(0, 255)

                        rgba[y * width + x] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
                    }
                }
                bitmap.setPixels(rgba, 0, width, 0, 0, width, height)
            } else {
                // Для других форматов используем простую конвертацию
                val pixels = IntArray(width * height)
                for (y in 0 until height) {
                    for (x in 0 until width) {
                        val pixelValue = buffer.get(y * rowStride + x * pixelStride).toInt() and 0xFF
                        val color = android.graphics.Color.rgb(pixelValue, pixelValue, pixelValue)
                        pixels[y * width + x] = color
                    }
                }
                bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
            }

            // Возвращаем Bitmap вместо MPImage, так как MediaPipe tasks-vision работает напрямую с Bitmap
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}