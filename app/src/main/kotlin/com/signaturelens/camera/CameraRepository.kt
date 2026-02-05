package com.signaturelens.camera

import android.content.Context
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Surface
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class CameraRepository(
    private val context: Context,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private val cameraThread = HandlerThread("CameraThread").apply { start() }
    private val cameraHandler = Handler(cameraThread.looper)

    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var imageReader: ImageReader? = null
    private var previewSurface: Surface? = null

    private val backCameraId: String by lazy {
        cameraManager.cameraIdList.firstOrNull { id ->
            val characteristics = cameraManager.getCameraCharacteristics(id)
            characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
        } ?: "0"
    }

    suspend fun startPreview(surfaceTexture: SurfaceTexture) = withContext(dispatcher) {
        try {
            // Open camera if not already open
            if (cameraDevice == null) {
                cameraDevice = openCamera(backCameraId)
            }

            // Configure surface texture size
            val characteristics = cameraManager.getCameraCharacteristics(backCameraId)
            val streamConfigMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            val previewSize = streamConfigMap?.getOutputSizes(SurfaceTexture::class.java)?.firstOrNull()
            previewSize?.let {
                surfaceTexture.setDefaultBufferSize(it.width, it.height)
            }

            // Create preview surface
            previewSurface = Surface(surfaceTexture)

            // Close old session if exists
            captureSession?.close()
            captureSession = null

            // Create capture session with preview surface
            captureSession = createCaptureSession(
                cameraDevice!!,
                listOf(previewSurface!!)
            )

            // Start repeating preview request
            val previewRequest = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                addTarget(previewSurface!!)
                set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
            }.build()

            captureSession?.setRepeatingRequest(previewRequest, null, cameraHandler)

            Log.d(TAG, "Preview started successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start preview", e)
            throw e
        }
    }

    suspend fun stopPreview() = withContext(dispatcher) {
        try {
            captureSession?.stopRepeating()
            captureSession?.close()
            captureSession = null

            cameraDevice?.close()
            cameraDevice = null

            previewSurface?.release()
            previewSurface = null

            imageReader?.close()
            imageReader = null

            Log.d(TAG, "Preview stopped successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop preview", e)
        }
    }

    suspend fun captureStill(): File = withContext(dispatcher) {
        val device = cameraDevice ?: error("Camera not opened")

        try {
            // Get max resolution for still capture
            val characteristics = cameraManager.getCameraCharacteristics(backCameraId)
            val streamConfigMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            val captureSize = streamConfigMap?.getOutputSizes(ImageFormat.JPEG)?.maxByOrNull { it.width * it.height }
                ?: error("No JPEG output sizes available")

            // Create ImageReader for capture
            val reader = ImageReader.newInstance(
                captureSize.width,
                captureSize.height,
                ImageFormat.JPEG,
                2
            )

            // Recreate session with both preview and capture surfaces
            captureSession?.close()
            val surfaces = mutableListOf<Surface>()
            previewSurface?.let { surfaces.add(it) }
            surfaces.add(reader.surface)

            val session = createCaptureSession(device, surfaces)
            captureSession = session

            // Capture still image
            val captureFile = suspendCancellableCoroutine { continuation ->
                reader.setOnImageAvailableListener({ imageReader ->
                    try {
                        val image = imageReader.acquireLatestImage()
                        image?.use {
                            val buffer = it.planes[0].buffer
                            val bytes = ByteArray(buffer.remaining())
                            buffer.get(bytes)

                            // Save to temp file
                            val tempFile = File(
                                context.cacheDir,
                                "SignatureLens_temp_${System.currentTimeMillis()}.jpg"
                            )
                            FileOutputStream(tempFile).use { fos ->
                                fos.write(bytes)
                            }

                            Log.d(TAG, "Captured still image: ${tempFile.absolutePath}")
                            continuation.resume(tempFile)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to process captured image", e)
                        continuation.resumeWithException(e)
                    }
                }, cameraHandler)

                continuation.invokeOnCancellation {
                    reader.close()
                }

                // Create capture request
                val captureRequest = device.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE).apply {
                    addTarget(reader.surface)
                    set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                    set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
                    set(CaptureRequest.JPEG_ORIENTATION, 90) // Portrait orientation
                }.build()

                session.capture(captureRequest, null, cameraHandler)
            }

            // Restart preview after capture
            previewSurface?.let { surface ->
                val previewRequest = device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                    addTarget(surface)
                    set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                    set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
                }.build()
                session.setRepeatingRequest(previewRequest, null, cameraHandler)
            }

            reader.close()
            captureFile
        } catch (e: Exception) {
            Log.e(TAG, "Failed to capture still image", e)
            throw e
        }
    }

    private suspend fun openCamera(cameraId: String): CameraDevice = suspendCancellableCoroutine { continuation ->
        try {
            cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    Log.d(TAG, "Camera opened: $cameraId")
                    continuation.resume(camera)
                }

                override fun onDisconnected(camera: CameraDevice) {
                    Log.w(TAG, "Camera disconnected: $cameraId")
                    camera.close()
                    continuation.resumeWithException(IllegalStateException("Camera disconnected"))
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    Log.e(TAG, "Camera error: $error")
                    camera.close()
                    continuation.resumeWithException(IllegalStateException("Camera error: $error"))
                }
            }, cameraHandler)

            continuation.invokeOnCancellation {
                Log.d(TAG, "Camera open cancelled")
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Camera permission denied", e)
            continuation.resumeWithException(e)
        }
    }

    private suspend fun createCaptureSession(
        camera: CameraDevice,
        surfaces: List<Surface>
    ): CameraCaptureSession = suspendCancellableCoroutine { continuation ->
        camera.createCaptureSession(surfaces, object : CameraCaptureSession.StateCallback() {
            override fun onConfigured(session: CameraCaptureSession) {
                Log.d(TAG, "Capture session configured")
                continuation.resume(session)
            }

            override fun onConfigureFailed(session: CameraCaptureSession) {
                Log.e(TAG, "Capture session configuration failed")
                continuation.resumeWithException(IllegalStateException("Capture session configuration failed"))
            }
        }, cameraHandler)
    }

    fun cleanup() {
        captureSession?.close()
        cameraDevice?.close()
        imageReader?.close()
        previewSurface?.release()
        cameraThread.quitSafely()
    }

    companion object {
        private const val TAG = "CameraRepository"
    }
}
