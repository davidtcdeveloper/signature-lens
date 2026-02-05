package com.signaturelens.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.media.Image
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.view.Surface
import com.signaturelens.core.renderer.RenderThread
import com.signaturelens.core.intelligence.FaceDetectionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
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
    private val faceDetectionManager: FaceDetectionManager? = null,
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

    private val cameraSettings: CameraSettings by lazy {
        CameraSettings(cameraManager, backCameraId)
    }

    suspend fun startPreview(surfaceTexture: SurfaceTexture) = withContext(cameraDispatcher) {
        try {
            // Close existing session if active
            closeExistingSession()

            // Open camera and configure
            val device = openCamera(backCameraId)
            val previewSize = getPreviewSize()
            
            surfaceTexture.setDefaultBufferSize(previewSize.width, previewSize.height)

            // Create and setup frame processing
            val imageReader = createPreviewImageReader(previewSize)
            val renderThread = RenderThread(context, surfaceTexture)
            renderThread.start()
            
            setupFrameListener(imageReader, renderThread)

            // Create session and start preview
            val captureSession = createCaptureSession(device, listOf(imageReader.surface))
            val session = CameraSession(
                device = device,
                session = captureSession,
                previewSurface = imageReader.surface,
                renderThread = renderThread,
                imageReader = imageReader
            )
            
            _state = CameraState.Active(session)
            startPreviewRequest(device, captureSession, imageReader.surface)

            Log.d(TAG, "Preview started successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start preview", e)
            _state = CameraState.Idle
            throw e
        }
    }

    private fun closeExistingSession() {
        if (_state is CameraState.Active) {
            (_state as CameraState.Active).session.close()
        }
    }

    private fun getPreviewSize(): Size {
        val characteristics = cameraManager.getCameraCharacteristics(backCameraId)
        val streamConfigMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        return streamConfigMap?.getOutputSizes(SurfaceTexture::class.java)?.firstOrNull()
            ?: throw IllegalStateException("No preview size found")
    }

    private fun createPreviewImageReader(size: Size): ImageReader {
        return ImageReader.newInstance(size.width, size.height, ImageFormat.YUV_420_888, 2)
    }

    private fun setupFrameListener(imageReader: ImageReader, renderThread: RenderThread) {
        imageReader.setOnImageAvailableListener({ reader ->
            try {
                val image = reader.acquireLatestImage()
                if (image != null) {
                    renderThread.enqueueFrame(image)
                    triggerFaceDetection(image, renderThread)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error acquiring image", e)
            }
        }, cameraHandler)
    }

    private fun triggerFaceDetection(image: Image, renderThread: RenderThread) {
        faceDetectionManager?.let {
            kotlinx.coroutines.GlobalScope.launch(Dispatchers.Default) {
                try {
                    val hasFaces = it.detectFaces(image)
                    renderThread.setFaceDetectionResult(hasFaces)
                } catch (e: Exception) {
                    Log.e(TAG, "Face detection error", e)
                }
            }
        }
    }

    private fun startPreviewRequest(device: CameraDevice, session: CameraCaptureSession, surface: Surface) {
        val previewRequest = device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
            addTarget(surface)
            set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
            set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
        }.build()

        session.setRepeatingRequest(previewRequest, null, cameraHandler)
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

    suspend fun captureStillBitmap(): Bitmap = withContext(cameraDispatcher) {
        val session = when (val currentState = _state) {
            is CameraState.Active -> currentState.session
            CameraState.Idle -> error("Camera not opened")
        }

        try {
            val captureSize = getCaptureSize()
            val reader = createCaptureImageReader(captureSize)
            val newSession = recreateSessionForCapture(session, reader)

            val captureBitmap = performStillCapture(newSession, session, reader)
            
            restartPreviewAfterCapture(newSession)
            reader.close()
            
            captureBitmap
        } catch (e: Exception) {
            Log.e(TAG, "Failed to capture still image", e)
            throw e
        }
    }

    private fun getCaptureSize(): Size {
        val characteristics = cameraManager.getCameraCharacteristics(backCameraId)
        val streamConfigMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        return streamConfigMap?.getOutputSizes(ImageFormat.YUV_420_888)?.maxByOrNull { it.width * it.height }
            ?: error("No YUV output sizes available")
    }

    private fun createCaptureImageReader(size: Size): ImageReader {
        return ImageReader.newInstance(size.width, size.height, ImageFormat.YUV_420_888, 2)
    }

    private suspend fun recreateSessionForCapture(oldSession: CameraSession, reader: ImageReader): CameraSession {
        oldSession.session.close()
        
        val surfaces = listOf(oldSession.previewSurface, reader.surface)
        val captureSession = createCaptureSession(oldSession.device, surfaces)
        
        val newCameraSession = CameraSession(
            device = oldSession.device,
            session = captureSession,
            previewSurface = oldSession.previewSurface,
            renderThread = oldSession.renderThread,
            imageReader = oldSession.imageReader
        )
        
        _state = CameraState.Active(newCameraSession)
        return newCameraSession
    }

    private suspend fun performStillCapture(
        newSession: CameraSession,
        originalSession: CameraSession,
        reader: ImageReader
    ): Bitmap = suspendCancellableCoroutine { continuation ->
        reader.setOnImageAvailableListener({ imageReader ->
            try {
                val image = imageReader.acquireLatestImage()
                if (image != null) {
                    originalSession.renderThread.capture(image) { result ->
                        handleCaptureResult(result, continuation)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to process captured image", e)
                continuation.resumeWithException(e)
            }
        }, cameraHandler)

        continuation.invokeOnCancellation {
            reader.close()
        }

        triggerStillCapture(newSession, reader)
    }

    private fun handleCaptureResult(result: Result<Bitmap>, continuation: kotlinx.coroutines.CancellableContinuation<Bitmap>) {
        if (result.isSuccess) {
            continuation.resume(result.getOrThrow())
        } else {
            continuation.resumeWithException(result.exceptionOrNull() ?: RuntimeException("Unknown error"))
        }
    }

    private fun triggerStillCapture(session: CameraSession, reader: ImageReader) {
        val captureRequest = session.device.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE).apply {
            addTarget(reader.surface)
            set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
            set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
            set(CaptureRequest.JPEG_ORIENTATION, 90)
        }.build()

        session.session.capture(captureRequest, null, cameraHandler)
    }

    private fun restartPreviewAfterCapture(session: CameraSession) {
        val previewRequest = session.device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
            addTarget(session.previewSurface)
            set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
            set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
        }.build()
        session.session.setRepeatingRequest(previewRequest, null, cameraHandler)
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

    // Settings control methods
    fun setExposureCompensation(evValue: Float) {
        cameraSettings.setExposureComp(evValue)
    }

    fun setFlashMode(mode: com.signaturelens.core.settings.FlashMode) {
        cameraSettings.setFlashMode(mode)
    }

    companion object {
        private const val TAG = "CameraRepository"
    }
}
