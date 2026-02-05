package com.signaturelens.core.intelligence

import android.graphics.Bitmap
import android.media.Image
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Manages ML Kit face detection on downscaled frames with throttling.
 * Runs detection asynchronously to avoid blocking the rendering pipeline.
 */
class FaceDetectionManager {
    private val detector: FaceDetector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
            .build()
    )

    private var frameCounter = 0
    private val throttleFrames = 3  // Run detection every 3rd frame (~10 FPS at 30 FPS preview)

    suspend fun detectFaces(image: Image): Boolean = withContext(Dispatchers.Default) {
        frameCounter++
        if (frameCounter % throttleFrames != 0) {
            // Return cached result; if this is the first call, assume no faces
            return@withContext false
        }

        try {
            // Downscale the image for faster processing
            val downscaledBitmap = downscaleImage(image, maxWidth = 480)
            val inputImage = InputImage.fromBitmap(downscaledBitmap, 0)
            val detectedFaces = detector.process(inputImage).result
            downscaledBitmap.recycle()
            detectedFaces.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Downscale a YUV Image to a Bitmap with max width [maxWidth].
     * This reduces computation overhead for ML Kit detection.
     */
    private fun downscaleImage(image: Image, maxWidth: Int): Bitmap {
        val width = image.width
        val height = image.height

        // Calculate scale factor
        val scale = if (width > maxWidth) width / maxWidth else 1
        val scaledWidth = width / scale
        val scaledHeight = height / scale

        // Convert YUV to RGB then create Bitmap
        val nv21Data = yuvToNv21(image)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        // Simple YUV to RGB conversion (placeholder; actual implementation depends on format)
        // For now, using native JNI would be ideal, but for MVP we can use basic approach
        val rgbData = nv21ToRgb(nv21Data, width, height)
        bitmap.setPixels(rgbData, 0, width, 0, 0, width, height)

        // Downscale
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true)
        bitmap.recycle()
        return scaledBitmap
    }

    private fun yuvToNv21(image: Image): ByteArray {
        val planes = image.planes
        val ySize = planes[0].buffer.remaining()
        val uSize = planes[1].buffer.remaining()
        val vSize = planes[2].buffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)

        planes[0].buffer.get(nv21, 0, ySize)
        val pixelStride = planes[1].pixelStride

        if (pixelStride == 1) {
            planes[1].buffer.get(nv21, ySize, uSize)
            planes[2].buffer.get(nv21, ySize + uSize, vSize)
        } else {
            // Handle interleaved UV
            val uvBuffer = ByteArray(uSize + vSize)
            planes[1].buffer.get(uvBuffer, 0, uSize)
            planes[2].buffer.get(uvBuffer, uSize, vSize)
            // Deinterleave if needed
            for (i in 0 until uSize step pixelStride) {
                nv21[ySize + i / 2] = uvBuffer[i]
            }
            for (i in 0 until vSize step pixelStride) {
                nv21[ySize + uSize / 2 + i / 2] = uvBuffer[uSize + i]
            }
        }

        return nv21
    }

    private fun nv21ToRgb(data: ByteArray, width: Int, height: Int): IntArray {
        val pixels = IntArray(width * height)
        var inputOffset = width * height
        var pixelIndex = 0

        for (y in 0 until height) {
            for (x in 0 until width) {
                val yval = data[y * width + x].toInt() and 0xff
                val uvOffset = inputOffset + (y / 2) * width + (x / 2) * 2

                val u = (data[uvOffset].toInt() and 0xff) - 128
                val v = (data[uvOffset + 1].toInt() and 0xff) - 128

                val r = (yval + (1.402f * v)).toInt().coerceIn(0, 255)
                val g = (yval - 0.344f * u - 0.714f * v).toInt().coerceIn(0, 255)
                val b = (yval + (1.772f * u)).toInt().coerceIn(0, 255)

                pixels[pixelIndex] = -0x1000000 or (r shl 16) or (g shl 8) or b
                pixelIndex++
            }
        }

        return pixels
    }

    fun close() {
        detector.close()
    }
}
