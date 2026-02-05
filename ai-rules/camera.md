<primary_directive>
You are implementing Camera2 API for a professional camera application.
Camera2 requires precise resource management and thread handling.

ALWAYS reference spec §10.2 (Preview Pipeline) and §10.3 (Capture Pipeline).
ALWAYS check spec §14 (Known Pitfalls - Camera2) before implementing.
</primary_directive>

<cognitive_anchors>
TRIGGERS: Camera2, CameraManager, CameraDevice, CaptureSession, ImageReader,
          preview, capture, YUV, Surface, CaptureRequest

SIGNAL: Camera2 work detected → Load Camera2 patterns and pitfalls
</cognitive_anchors>

---

<rule_1 priority="HIGHEST">
**RESOURCE LIFECYCLE - ALWAYS CLOSE RESOURCES**

Per spec §14 Pitfalls: ImageReader and CameraDevice MUST be closed to prevent memory leaks.

```kotlin
// ✅ CORRECT - use .use {} for auto-cleanup
suspend fun captureFrame(): Image = suspendCoroutine { cont ->
    ImageReader.newInstance(width, height, ImageFormat.YUV_420_888, 2).use { reader ->
        reader.setOnImageAvailableListener({ imageReader ->
            val image = imageReader.acquireLatestImage()
            cont.resume(image)
        }, null)
        
        // Trigger capture...
        // Reader auto-closed on exit
    }
}

// ✅ CORRECT - manual cleanup
class CameraRepository {
    private var camera: CameraDevice? = null
    private var session: CameraCaptureSession? = null
    
    fun cleanup() {
        session?.close()
        camera?.close()
        session = null
        camera = null
    }
}

// ❌ WRONG - leak risk
val reader = ImageReader.newInstance(...)
// If exception thrown before closing, reader leaks!
```
</rule_1>

<rule_2 priority="HIGHEST">
**THREAD SAFETY - CAMERA CALLBACKS ON HANDLER THREAD**

Per spec §14 Pitfalls: Camera2 callbacks run on Handler thread, NOT Main thread.

```kotlin
// ✅ CORRECT - use Handler for camera callbacks
private val cameraHandler = Handler(HandlerThread("CameraThread").apply { start() }.looper)

cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
    override fun onOpened(camera: CameraDevice) {
        // This runs on Handler thread
        // Update UI via ViewModel + StateFlow
        viewModelScope.launch(Dispatchers.Main) {
            _uiState.update { it.copy(cameraReady = true) }
        }
    }
}, cameraHandler)  // ← Handler for callbacks

// ❌ WRONG - null handler = Main thread
cameraManager.openCamera(cameraId, callback, null)  // Blocks UI!
```
</rule_1>

<rule_3 priority="HIGHEST">
**CAPTURE SESSION - ONE ACTIVE SESSION AT A TIME**

Camera2 allows only ONE active CaptureSession per CameraDevice.

```kotlin
// ✅ CORRECT - close old session before creating new
suspend fun startPreview(surface: Surface) {
    currentSession?.close()  // Close old session
    
    cameraDevice.createCaptureSession(
        listOf(surface),
        object : CameraCaptureSession.StateCallback() {
            override fun onConfigured(session: CameraCaptureSession) {
                currentSession = session
                val request = cameraDevice.createCaptureRequest(TEMPLATE_PREVIEW).apply {
                    addTarget(surface)
                }.build()
                
                session.setRepeatingRequest(request, null, null)
            }
        },
        null
    )
}

// ❌ WRONG - creating second session without closing first
cameraDevice.createCaptureSession(...)  // Old session still active → crash
```
</rule_3>

<rule_4 priority="HIGH">
**PREVIEW PIPELINE - YUV → RGB → GPU SHADER**

Per spec §10.2: Preview pipeline transforms YUV frames through libyuv and OpenGL.

```kotlin
// Preview setup (from spec)
class CameraRepository(private val context: Context) {
    
    suspend fun startPreview(textureView: TextureView) = withContext(Dispatchers.IO) {
        val surfaceTexture = textureView.surfaceTexture ?: return@withContext
        val surface = Surface(surfaceTexture)
        
        // Configure for YUV_420_888 (will be converted to RGB)
        val previewReader = ImageReader.newInstance(
            PREVIEW_WIDTH,
            PREVIEW_HEIGHT,
            ImageFormat.YUV_420_888,
            3  // Triple buffer
        )
        
        previewReader.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
            
            // Hand off to processing pipeline
            processPreviewFrame(image)
            image.close()
        }, cameraHandler)
        
        // Create session with preview surface
        createCaptureSession(listOf(surface, previewReader.surface))
    }
}
```
</rule_4>

<rule_5 priority="HIGH">
**CAPTURE PIPELINE - STILL IMAGE CAPTURE**

Per spec §10.3: High-resolution still capture with color pipeline and HEIC encoding.

```kotlin
suspend fun captureStillImage(): CaptureResult = suspendCoroutine { cont ->
    // Setup ImageReader for full-res capture
    ImageReader.newInstance(
        CAPTURE_WIDTH,
        CAPTURE_HEIGHT,
        ImageFormat.YUV_420_888,
        2
    ).use { captureReader ->
        
        captureReader.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage()
            
            // Process full-res through color pipeline
            viewModelScope.launch(Dispatchers.Default) {
                val processedImage = applyColorPipeline(image)
                val heicUri = encodeToHeic(processedImage)
                cont.resume(CaptureResult.Success(heicUri))
                image.close()
            }
        }, cameraHandler)
        
        // Trigger still capture
        val captureRequest = camera.createCaptureRequest(TEMPLATE_STILL_CAPTURE).apply {
            addTarget(captureReader.surface)
            set(CaptureRequest.CONTROL_CAPTURE_INTENT, CONTROL_CAPTURE_INTENT_STILL_CAPTURE)
        }.build()
        
        session.capture(captureRequest, null, cameraHandler)
    }
}
```
</rule_5>

<rule_6 priority="MEDIUM">
**EXPOSURE COMPENSATION - MANUAL CONTROL**

Per spec §2.1: Exposure compensation ±3 EV.

```kotlin
fun setExposureCompensation(value: Float) {
    val range = cameraCharacteristics.get(CONTROL_AE_COMPENSATION_RANGE) ?: return
    val step = cameraCharacteristics.get(CONTROL_AE_COMPENSATION_STEP) ?: return
    
    // Convert EV to camera units
    val clampedValue = value.coerceIn(-3f, 3f)
    val compensationSteps = (clampedValue / step.toFloat()).toInt()
    val finalValue = compensationSteps.coerceIn(range.lower, range.upper)
    
    // Update capture request
    val request = previewRequestBuilder.apply {
        set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, finalValue)
    }.build()
    
    session?.setRepeatingRequest(request, null, cameraHandler)
}
```
</rule_6>

---

<pattern name="camera_initialization">
**Complete Camera Initialization Flow**

```kotlin
class CameraRepository(private val context: Context) {
    private val cameraManager = context.getSystemService(CameraManager::class.java)
    private val cameraHandler = Handler(HandlerThread("Camera").apply { start() }.looper)
    
    private var camera: CameraDevice? = null
    private var session: CameraCaptureSession? = null
    private var previewRequestBuilder: CaptureRequest.Builder? = null
    
    suspend fun initialize(cameraId: String = "0") = suspendCoroutine { cont ->
        // 1. Check permission
        if (ContextCompat.checkSelfPermission(context, CAMERA) != PERMISSION_GRANTED) {
            cont.resumeWithException(SecurityException("Camera permission required"))
            return@suspendCoroutine
        }
        
        // 2. Open camera
        cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
            override fun onOpened(device: CameraDevice) {
                camera = device
                cont.resume(Unit)
            }
            
            override fun onDisconnected(device: CameraDevice) {
                cleanup()
            }
            
            override fun onError(device: CameraDevice, error: Int) {
                cleanup()
                cont.resumeWithException(CameraException("Error: $error"))
            }
        }, cameraHandler)
    }
    
    suspend fun startPreview(surface: Surface) = suspendCoroutine { cont ->
        val device = camera ?: run {
            cont.resumeWithException(IllegalStateException("Camera not initialized"))
            return@suspendCoroutine
        }
        
        // 3. Create capture session
        device.createCaptureSession(
            listOf(surface),
            object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(captureSession: CameraCaptureSession) {
                    session = captureSession
                    
                    // 4. Create repeating request
                    previewRequestBuilder = device.createCaptureRequest(TEMPLATE_PREVIEW).apply {
                        addTarget(surface)
                        set(CONTROL_AF_MODE, CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                        set(CONTROL_AE_MODE, CONTROL_AE_MODE_ON)
                    }
                    
                    captureSession.setRepeatingRequest(
                        previewRequestBuilder!!.build(),
                        null,
                        cameraHandler
                    )
                    
                    cont.resume(Unit)
                }
                
                override fun onConfigureFailed(session: CameraCaptureSession) {
                    cont.resumeWithException(CameraException("Session config failed"))
                }
            },
            cameraHandler
        )
    }
    
    fun cleanup() {
        session?.close()
        camera?.close()
        session = null
        camera = null
    }
}
```
</pattern>

<pattern name="face_detection_throttled">
**Face Detection with Throttling**

Per spec §14 Pitfalls: Run face detection at 10 FPS max on downscaled frames.

```kotlin
class FaceDetectionProcessor {
    private val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(PERFORMANCE_MODE_FAST)
            .build()
    )
    
    private var lastDetectionTime = 0L
    private val detectionIntervalMs = 100L  // 10 FPS max
    
    fun processFrame(image: Image, onFacesDetected: (List<Face>) -> Unit) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastDetectionTime < detectionIntervalMs) {
            return  // Skip frame, too soon
        }
        lastDetectionTime = now
        
        // Downscale for performance
        val downscaled = downscaleImage(image, scaleFactor = 0.25f)
        
        val inputImage = InputImage.fromMediaImage(downscaled, 0)
        detector.process(inputImage)
            .addOnSuccessListener { faces ->
                onFacesDetected(faces)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Face detection failed", e)
            }
    }
}
```
</pattern>

---

<checklist>
Camera2 Implementation Checklist:

☐ Camera permission checked before opening camera
☐ CameraDevice opened with Handler (not null)
☐ Old CaptureSession closed before creating new one
☐ ImageReader configured with correct format (YUV_420_888)
☐ ImageReader has setOnImageAvailableListener
☐ Images acquired from ImageReader are closed after use
☐ All camera resources closed in cleanup/onDispose
☐ Preview runs on repeating request, not single capture
☐ Capture uses TEMPLATE_STILL_CAPTURE (not preview template)
☐ Face detection throttled to 10 FPS on downscaled frames
☐ Thread safety: UI updates use Dispatchers.Main
☐ Error handling for onError, onDisconnected callbacks
</checklist>

<avoid>
❌ Opening camera without Handler → blocks UI
❌ Forgetting to close ImageReader → memory leak
❌ Creating new session without closing old → crash
❌ Acquiring Image without closing → leak
❌ Running face detection every frame → performance hit
❌ Using TEMPLATE_PREVIEW for still capture → low quality
❌ Accessing camera on Main thread → ANR
❌ Not checking camera permission → SecurityException
❌ Hardcoding camera ID without checking availability
❌ Forgetting to close CameraDevice on cleanup
</avoid>

---

<pitfalls_from_spec>
**From spec §14 - Camera2 Common Mistakes:**

1. **ImageReader surfaces not closed → memory leak**
   Solution: Use .use {} or try-finally

2. **CaptureRequest set on wrong thread → crash**
   Solution: Use Handler thread for all camera callbacks

3. **Preview stutters when face detection runs every frame**
   Solution: Throttle to every 3rd frame or 10 FPS max, downscale image

4. **Multiple sessions created → crash**
   Solution: Close previous session before creating new one

5. **Blocking UI thread with camera operations**
   Solution: Always use suspendCoroutine + Handler for camera APIs
</pitfalls_from_spec>

---

<performance_targets>
From spec §13:

- Preview FPS: 28-30 FPS sustained
- Preview Latency: < 33ms camera → screen
- Capture Processing: 2-5s at full resolution
- Peak Memory: < 250 MB

Monitor with:
- Android Studio Profiler (CPU/Memory)
- systrace for frame timing
- Logcat for dropped frames
</performance_targets>

---

## Quick Reference

**Starting preview:**
1. Check permission
2. Open camera with Handler
3. Create capture session with Surface
4. Set repeating request with TEMPLATE_PREVIEW

**Capturing still image:**
1. Create ImageReader for full-res
2. Add capture callback listener
3. Submit capture request with TEMPLATE_STILL_CAPTURE
4. Process image in callback
5. Close image after processing

**Cleanup:**
1. Close CaptureSession
2. Close CameraDevice
3. Close ImageReader
4. Null out references

**Face detection:**
1. Throttle to 10 FPS
2. Downscale frame to 25%
3. Process asynchronously
4. Update UI on Main thread

---

Remember: Camera2 is low-level and unforgiving. Follow patterns exactly, reference spec §14 pitfalls, and test on real devices frequently.
