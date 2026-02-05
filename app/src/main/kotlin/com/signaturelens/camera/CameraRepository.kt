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
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Repository for camera operations using Camera2 API.
 * 
 * State management:
 * - Uses sealed class CameraState (Idle | Active) for type-safe state transitions
 * - All active resources encapsulated in immutable CameraSession
 * - Single-threaded execution ensures thread safety without blocking
 * - All operations run on dedicated camera thread for consistency
 * - Camera2 callbacks and state changes happen on same thread
 */
class CameraRepository(
    private val context: Context,
) {
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    
    // Single thread for all camera operations - ensures serial execution
    private val cameraThread = HandlerThread("CameraThread").apply { start() }
    private val cameraHandler = Handler(cameraThread.looper)
    private val cameraDispatcher = cameraHandler.asCoroutineDispatcher()
    
    // State only modified on cameraDispatcher - no mutex needed
    private var _state: CameraState = CameraState.Idle

    private val backCameraId: String by lazy {
        cameraManager.cameraIdList.firstOrNull { id ->
            val characteristics = cameraManager.getCameraCharacteristics(id)
            characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
        } ?: "0"
    }

    suspend fun startPreview(surfaceTexture: SurfaceTexture) = withContext(cameraDispatcher) {
        try {
            // Close existing session if active
            if (_state is CameraState.Active) {
                (_state as CameraState.Active).session.close()
            }

                // Open camera
                val device = openCamera(backCameraId)

                // Configure surface texture size
                val characteristics = cameraManager.getCameraCharacteristics(backCameraId)
                val streamConfigMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                val previewSize = streamConfigMap?.getOutputSizes(SurfaceTexture::class.java)?.firstOrNull()
                previewSize?.let {
                    surfaceTexture.setDefaultBufferSize(it.width, it.height)
                }

                // Create preview surface
                val surface = Surface(surfaceTexture)

                // Create capture session
                val captureSession = createCaptureSession(device, listOf(surface))

                // Create immutable session holder
                val session = CameraSession(
                    device = device,
                    session = captureSession,
                    previewSurface = surface
                )

                // Update state
                _state = CameraState.Active(session)

                // Start repeating preview request
                val previewRequest = device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                    addTarget(surface)
                    set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                    set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
                }.build()

                captureSession.setRepeatingRequest(previewRequest, null, cameraHandler)

                Log.d(TAG, "Preview started successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start preview", e)
                _state = CameraState.Idle
                throw e
            }
    }

    suspend fun stopPreview() = withContext(cameraDispatcher) {
        try {
            when (val currentState = _state) {
                is CameraState.Active -> {
                    currentState.session.session.stopRepeating()
                    currentState.session.close()
                    _state = CameraState.Idle
                    Log.d(TAG, "Preview stopped successfully")
                }
                CameraState.Idle -> {
                    Log.d(TAG, "Preview already stopped")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop preview", e)
            _state = CameraState.Idle
        }
    }

    suspend fun captureStill(): File = withContext(cameraDispatcher) {
        val session = when (val currentState = _state) {
            is CameraState.Active -> currentState.session
            CameraState.Idle -> error("Camera not opened")
        }

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
            val newSession = run {
                session.session.close()
                
                val surfaces = listOf(session.previewSurface, reader.surface)
                val captureSession = createCaptureSession(session.device, surfaces)
                
                val newCameraSession = CameraSession(
                    device = session.device,
                    session = captureSession,
                    previewSurface = session.previewSurface
                )
                
                _state = CameraState.Active(newCameraSession)
                newCameraSession
            }

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
                val captureRequest = session.device.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE).apply {
                    addTarget(reader.surface)
                    set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                    set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
                    set(CaptureRequest.JPEG_ORIENTATION, 90)
                }.build()

                newSession.session.capture(captureRequest, null, cameraHandler)
            }

            // Restart preview after capture
            run {
                val previewRequest = newSession.device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                    addTarget(newSession.previewSurface)
                    set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                    set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
                }.build()
                newSession.session.setRepeatingRequest(previewRequest, null, cameraHandler)
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
        if (_state is CameraState.Active) {
            (_state as CameraState.Active).session.close()
        }
        _state = CameraState.Idle
        cameraThread.quitSafely()
    }

    companion object {
        private const val TAG = "CameraRepository"
    }
}
