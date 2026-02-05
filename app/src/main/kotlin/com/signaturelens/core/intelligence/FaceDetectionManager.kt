package com.signaturelens.core.intelligence

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
            val downscaledBitmap = YuvConverter.downscaleImageToBitmap(image, maxWidth = 480)
            val inputImage = InputImage.fromBitmap(downscaledBitmap, 0)
            val detectedFaces = detector.process(inputImage).result
            downscaledBitmap.recycle()
            detectedFaces.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }

    fun close() {
        detector.close()
    }
}
