# SignatureLens - Product & Technical Specification

**Status:** MVP Specification  
**Version:** 1.2 (AI Agent Optimized)  
**Last Updated:** February 5, 2026

---

## TABLE OF CONTENTS & QUICK REFERENCE

| Section | Purpose | When to Reference |
|---------|---------|-------------------|
| §1 Objective | Vision, success criteria, user goals | Planning, feature decisions |
| §2 Scope | What's in/out of MVP | Scoping new features |
| §3 Tech Stack | Technologies & versions | Setup, dependencies |
| §4 Commands | Build, test, lint commands | Daily development |
| §5 Structure | File organization | Creating new files/modules |
| §6 Code Style | Naming, examples | Writing new code |
| §7 Git Workflow | Branches, commits, PRs | Version control |
| §8 Boundaries | Always/Ask/Never rules | Decision-making |
| §9 User Flow | End-to-end user journey | UX/UI implementation |
| §10 Architecture | ViewModels, pipelines | Core logic implementation |
| §11 UI/Compose | Composables, screens | UI development |
| §12 Testing | Unit, integration, strategy | Writing/running tests |
| §13 Performance | Metrics, targets | Optimization work |
| §14 Risks | Known issues, mitigations | Problem-solving |
| §15 Roadmap | Phased implementation | Planning sprints |

---

## 1. OBJECTIVE & VISION

**What:**  
An Android camera application that captures photos with a single, iconic rangefinder-style aesthetic and saves them as HEIC files. The user sees the look live in the preview and the final file is already styled—no presets, no editing.

**Why:**

- Android users lack a focused, opinionated “one-look” camera app with a timeless aesthetic.
- Creators want a consistent visual identity without fiddling with profiles or post-processing.
- HEIC offers significantly better compression than JPEG on modern Android devices.[web:52][web:61]

**Who:**

- Photography enthusiasts who like rangefinder/film-like rendering.
- Street/documentary photographers who value consistency over options.
- Content creators who want a recognizable, cohesive feed.
- Users who prefer constraints: one look, one tap, no sliders.

**Success Criteria (Acceptance Tests):**

1. **User Journey Test:** User opens app → sees styled preview → taps shutter → reviews image → saves to gallery in < 10s total.
2. **Visual Consistency:** All preview frames match final saved image aesthetic (WYSIWYG verified).
3. **Performance:**
   - Preview: 28–30 FPS sustained, < 33ms latency (measured with systrace).
   - Capture: Shutter-to-HEIC-file < 7s on Galaxy S23/S24 (instrumentation test).
4. **File Quality:**
   - HEIC: 2–5 MB for 12–24 MP (60–70% smaller than JPEG equivalent).
   - EXIF metadata present (time, orientation, GPS if permitted).
5. **Scene Intelligence:**
   - Portrait mode: Face detection triggers, skin tones warmed (unit test with sample images).
   - Landscape: No face detection overhead, color grading applied (unit test).
6. **Reliability:** Zero crashes in core flow across 100 test iterations (instrumented test on device).

---

## 2. SCOPE & CORE FEATURES

### 2.1 In Scope (MVP)

- Single aesthetic applied in:
    - Live camera preview.
    - Final captured stills.
- Output formats:
    - HEIC as default when device supports hardware HEIC.
    - Automatic fallback to JPEG otherwise.[web:52]
- Capture controls:
    - Exposure compensation ±3 EV.
    - Grid overlay toggle.
    - Flash: auto / on / off.
    - Self-timer: off / 3 s / 10 s.
- Face-aware processing:
    - Detect faces and slightly bias tones and contrast for portraits.[web:60]
- Simple scene awareness:
    - Portrait vs. non-portrait differentiations for subtle tuning.
- Metadata and storage:
    - EXIF metadata (date/time, orientation, camera info, GPS where permitted).
    - MediaStore-based save to `DCIM/SignatureLens`.[web:52]

- Architecture & tooling:
    - **Jetpack Compose** for all UI (no XML layouts).[web:68][web:79]
    - **Kotlin Coroutines** for all async / background work.[web:80]
    - **Koin** for dependency injection, including Compose integration.[web:68][web:70][web:73]
    - Automated integration tests (instrumentation) for core flows.[web:60]

### 2.2 Out of Scope (MVP)

- Multiple looks / styles / presets.
- Video capture or streaming.
- In-app editors or post-processing sliders.
- RAW/DNG capture.
- Cloud sync, presets marketplace, social feed.
- Advanced pro overlays (histograms, zebras, focus peaking).
- Multi-camera fusion (beyond what the chosen sensor provides).

---

## 3. TECH STACK

| Layer      | Technology                          | Notes                                                                    |
|------------|-------------------------------------|--------------------------------------------------------------------------|
| Platform   | Android 10+ (minSdk 29, target 35)  | HEIC + scoped storage + modern APIs.[web:52]                             |
| Language   | Kotlin 1.9.20+                      | Primary language                                                         |
| UI         | Jetpack Compose + Material 3        | All screens, no XML layouts.[web:68][web:79]                             |
| DI         | Koin + koin-androidx-compose        | ViewModels, repositories, services.[web:68][web:70][web:73]              |
| Camera     | Camera2 API                         | Low-level camera control.[web:57][web:63][web:75]                        |
| Preview    | TextureView / GL surface wrapper    | For camera feed → GPU → Compose integration.[web:51][web:54]            |
| Graphics   | OpenGL ES 3.1                       | 3D LUT color grading, tone mapping, vignette, grain.[web:56]            |
| Encoding   | MediaCodec                          | HEIC/HEVC encoder, JPEG fallback.[web:52]                                |
| File I/O   | MediaStore + ContentResolver        | Scoped storage, gallery integration.[web:52]                             |
| ML         | ML Kit Vision (face detection)      | On-device, asynchronous.[web:60]                                        |
| Native     | libyuv (C++, via JNI)               | Fast YUV↔RGB conversion.[web:62]                                         |
| Async      | Kotlin Coroutines + Flow            | Non-blocking background work.[web:80]                                    |
| Tests      | JUnit, AndroidX Test, Compose UI    | Unit + integration + UI tests.[web:60][web:79]                           |
| Build      | Gradle 8.2+, AGP latest             | Standard Android toolchain.[web:60]                                      |

---

## 4. COMMANDS & BUILD

### 4.1 Build & Run

```bash
# Debug build
./gradlew assembleDebug

# Release build (signed)
./gradlew assembleRelease

# Install debug on connected device
./gradlew installDebug

# Launch main activity
adb shell am start -n com.signaturelens/.ui.MainActivity
```

### 4.2 Tests & Lint

```bash
# JVM unit tests
./gradlew test

# Instrumented Android tests (including Compose UI)
./gradlew connectedAndroidTest

# Lint & static analysis
./gradlew lint
```

---

## 5. PROJECT STRUCTURE

```text
SignatureLens/
├── app/
│   ├── src/main/kotlin/com/signaturelens/
│   │   ├── camera/           # Camera2 wrapper, repository
│   │   ├── processing/       # Color pipeline, JNI hooks, shaders
│   │   ├── storage/          # HEIC/JPEG saving, MediaStore integration
│   │   ├── settings/         # Preferences (DataStore/SharedPrefs)
│   │   ├── di/               # Koin modules
│   │   └── ui/
│   │       ├── screen/       # Compose screens: Preview, Review, Settings
│   │       ├── components/   # Reusable composables (buttons, sliders)
│   │       └── theme/        # Material theme, typography, color
│   ├── src/main/res/
│   │   └── values/           # Strings, colors, themes (no layout XML)
│   └── AndroidManifest.xml
├── core-native/
│   ├── CMakeLists.txt
│   └── src/main/cpp/
│       ├── yuv_converter.cpp
│       └── color_pipeline.cpp
└── build.gradle.kts
```

---

## 6. CODE STYLE & NAMING

### 6.1 Kotlin

- Packages: `com.signaturelens.<feature>`
- Classes: `PascalCase`.
- Methods, locals: `camelCase`.
- Constants: `UPPER_SNAKE_CASE`.

```kotlin
// ✅ GOOD
class CameraRepository(
    private val context: Context,
) {
    suspend fun startPreview(surface: Surface) { /* ... */ }
}

const val MAX_EXPOSURE_COMP = 3.0f

// ❌ BAD
class camera_repo {
    fun Preview(surface: Surface) { /* ... */ }
}
```

### 6.2 Compose

- Composables: `PascalCase`, `@Composable` prefix implied.
- Prefer `StateFlow`/`Flow` to expose state from ViewModels, collected via `collectAsState()`.

```kotlin
@Composable
fun PreviewScreen(
    viewModel: PreviewViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    // ...
}
```

---

## 7. GIT WORKFLOW

### 7.1 Branch Naming

```text
feature/compose-preview
feature/heic-encoding
feature/koin-di
feature/ml-face-detection
bugfix/preview-latency
chore/update-deps
```

### 7.2 Commit Messages

```text
feat(ui): implement compose preview screen with controls

feat(di): add Koin modules for camera and viewmodels

fix(encoding): fallback to jpeg when heic codec missing

test(integration): add compose ui test for capture flow
```

### 7.3 PR Requirements

- All tests pass (`test`, `connectedAndroidTest`).
- Lint passes, no new critical warnings.
- Spec references updated if behavior changes.
- At least one reviewer approval.
- No direct usage of deprecated APIs.

---

## 8. BOUNDARIES (ALWAYS / ASK / NEVER)

### ✅ Always

- Use **Coroutines** for any blocking or long-running work (camera I/O, processing, encoding).[web:80]
- Use **Koin** for all app-level dependency injection (repositories, ViewModels).[web:68][web:70][web:73]
- Use **Jetpack Compose** for all UI—no XML layouts.[web:68][web:79]
- Close camera resources (`CameraDevice`, `CameraCaptureSession`, `ImageReader`) properly.[web:57]
- Save via MediaStore with correct MIME type and display name.[web:52]
- Embed EXIF metadata (time, orientation; GPS if permission granted).

### ⚠️ Ask First

- Changing min/target SDK.
- Adding large new dependencies (e.g., new ML models, heavy SDKs).
- Modifying the fundamental aesthetic (tone curve, LUT) significantly.
- Adding network features (analytics, remote config, uploads).

### 🚫 Never

- Use deprecated `android.hardware.Camera`.
- Block the UI thread for processing or encoding.
- Store photos outside of scoped storage / MediaStore on Android 10+.
- Bypass runtime permission checks.
- Directly reference `Context` from Composables (use DI / ViewModels).
- Commit hardcoded API keys, tokens, or secrets to version control.
- Modify `gradle/` or `.github/workflows/` without explicit approval.
- Remove or skip failing tests without documenting why.

---

## 9. FUNCTIONAL SPECIFICATION

### 9.1 User Flow

1. Launch app → permissions (camera, optional location).
2. Live preview appears with the iconic look applied.
3. User composes:
   - Adjusts exposure compensation.
   - Toggles grid.
   - Optionally sets timer.
4. Taps shutter.
5. App:
   - Captures full-res frame.
   - Processes through color pipeline.
   - Encodes to HEIC (or JPEG fallback).
   - Writes to MediaStore.
6. Review overlay appears:
   - Confirm/save (default).
   - Retake.
   - Share (with JPEG conversion if share target doesn’t support HEIC).
7. Returns to preview.

### 9.2 Look Characteristics (High-Level)

- **Color:**
    - Slight warm bias (+ few hundred K).
    - Slight magenta push in skin tones.
    - Natural saturation, good color separation.
- **Tone:**
    - Lifted blacks (shadow details preserved).
    - Protected highlights (soft roll-off).
    - Mid-tone contrast increased (gentle S-curve).
- **Texture:**
    - Subtle micro-contrast (3D pop).
    - Light film-like grain.
- **Vignette:**
    - Shallow vignette for focus, never heavy.

---

## 10. ARCHITECTURE & PIPELINES

### 10.1 ViewModel + DI (Koin)

**PreviewViewModel**:

- Exposes `StateFlow<PreviewUiState>`.
- Depends on `CameraRepository`, `CaptureRepository`.

```kotlin
data class PreviewUiState(
    val isPreviewRunning: Boolean = false,
    val exposureComp: Float = 0f,
    val gridEnabled: Boolean = false,
    val lastCapture: CaptureResult? = null,
)

class PreviewViewModel(
    private val cameraRepository: CameraRepository,
    private val captureRepository: CaptureRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PreviewUiState())
    val uiState: StateFlow<PreviewUiState> = _uiState

    fun startPreview(surfaceTexture: SurfaceTexture) {
        viewModelScope.launch {
            cameraRepository.startPreview(surfaceTexture)
            _uiState.update { it.copy(isPreviewRunning = true) }
        }
    }

    fun capture() {
        viewModelScope.launch(Dispatchers.Default) {
            val result = captureRepository.captureStyledHeic()
            _uiState.update { it.copy(lastCapture = result) }
        }
    }
}
```

**Koin modules:**

```kotlin
val cameraModule = module {
    single { CameraRepository(androidContext()) }
    single { CaptureRepository(get(), get()) }
}

val viewModelModule = module {
    viewModel { PreviewViewModel(get(), get()) }
}
```

Compose usage:

```kotlin
@Composable
fun PreviewScreen(
    viewModel: PreviewViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    // ...
}
```

### 10.2 Preview Processing Pipeline

**Steps:**

1. Camera2 outputs frames as `YUV_420_888` to `ImageReader`.[web:51][web:54]
2. JNI + libyuv converts YUV to RGB (GPU-friendly format).[web:62]
3. OpenGL ES fragment shader:
   - Applies 3D LUT for color grading (rangefinder aesthetic).
   - Applies tone mapping (lifted shadows, controlled highlights).[web:56]
   - Adds vignette and grain.
4. The resulting texture is rendered to a surface associated with a `TextureView` or `SurfaceTexture`.
5. That surface is hosted inside a Compose UI via `AndroidView` or an integration component.

**Performance Target:**

- Each frame ≤ 33 ms end-to-end:
    - YUV decode: a few ms with libyuv.[web:62]
    - GPU shading: < 10 ms on S23/S24.
    - Present to display: within refresh budget.

### 10.3 Capture & Encoding Pipeline

1. Shutter press triggers high-res still capture on Camera2 session.[web:57]
2. Full-res frame arrives in `ImageReader`.
3. Convert frame to RGB (if YUV).
4. Apply full-resolution color pipeline (GPU or well-optimized CPU).
5. Encode to HEIC using MediaCodec with HEVC image profile.[web:52]
6. If hardware HEIC not available:
    - Fallback to JPEG.
7. Embed EXIF metadata via `ExifInterface`.
8. Insert into MediaStore in `DCIM/SignatureLens` with correct MIME type.

---

## 11. UI: JETPACK COMPOSE

### 11.1 Main Activity

```kotlin
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SignatureLensApp()
        }
    }
}

@Composable
fun SignatureLensApp() {
    SignatureLensTheme {
        PreviewScreen()
    }
}
```

### 11.2 PreviewScreen Composable

Responsibilities:

- Show live styled preview.
- Expose shutter button, exposure slider, grid/flash toggles, timer.
- Show review overlay after capture.

Sketch:

```kotlin
@Composable
fun PreviewScreen(
    viewModel: PreviewViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        CameraPreviewSurface(
            modifier = Modifier.fillMaxSize(),
            onSurfaceReady = { surfaceTexture ->
                viewModel.startPreview(surfaceTexture)
            }
        )

        CaptureControls(
            exposure = uiState.exposureComp,
            onExposureChange = viewModel::onExposureChange,
            onShutterClick = {
                coroutineScope.launch {
                    viewModel.capture()
                }
            },
            gridEnabled = uiState.gridEnabled,
            onGridToggle = viewModel::toggleGrid,
        )

        uiState.lastCapture?.let { capture ->
            CaptureReviewOverlay(
                capture = capture,
                onConfirm = viewModel::confirmCapture,
                onRetake = viewModel::retake
            )
        }
    }
}
```

`CameraPreviewSurface` is a composable wrapping Android’s `TextureView`/surface via `AndroidView`.

---

## 12. TESTING STRATEGY

### 12.1 Unit Tests

- Tone curve logic (blacks lifted, highlights protected).
- Color grading parameter math (warm bias, saturation).
- File naming logic.
- Settings persistence.

Example:

```kotlin
@Test
fun fileName_usesExpectedPattern() {
    val ts = 1767225600000L
    val name = generateOutputFileName(ts, FileFormat.HEIC)
    assertThat(name).isEqualTo("SignatureLens_20260131_000000.heic")
}
```

### 12.2 Integration / Instrumentation Tests

- Use AndroidX Test + Compose UI Test framework.[web:79][web:60]

**Key flows to cover:**

1. App launch → preview visible → shutter tap → file appears in MediaStore.
2. Settings (grid, exposure) survive process death.
3. Fakes for CameraRepository in instrumentation to avoid hardware dependency where needed.

**Dependencies:**

```kotlin
androidTestImplementation("androidx.test.ext:junit:1.1.5")
androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
androidTestImplementation("androidx.compose.ui:ui-test-junit4:<latest>")
androidTestImplementation("androidx.test:rules:1.5.0")
androidTestImplementation("androidx.test:runner:1.5.2")
```

**Example Integration Tests:**

```kotlin
@get:Rule
val composeRule = createAndroidComposeRule<MainActivity>()

@Test
fun captureFlow_savesHeicFileToMediaStore() {
    // Setup: Clear previous test files
    
    // Act: Launch → capture → confirm
    composeRule.onNodeWithTag("ShutterButton").performClick()
    composeRule.waitUntil(5000) { 
        composeRule.onNodeWithTag("ReviewOverlay").isDisplayed() 
    }
    composeRule.onNodeWithTag("ConfirmButton").performClick()
    
    // Assert: File exists in MediaStore with correct MIME type
    val cursor = context.contentResolver.query(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        arrayOf(MediaStore.Images.Media.DISPLAY_NAME, MediaStore.Images.Media.MIME_TYPE),
        "${MediaStore.Images.Media.DISPLAY_NAME} LIKE ?",
        arrayOf("SignatureLens_%"),
        "${MediaStore.Images.Media.DATE_ADDED} DESC"
    )
    cursor?.use {
        assertThat(it.moveToFirst()).isTrue()
        val mimeType = it.getString(it.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE))
        assertThat(mimeType).isEqualTo("image/heic")
    }
}

@Test
fun preview_maintainsTargetFps() {
    // Use Choreographer to measure frame times
    val frameTimes = mutableListOf<Long>()
    val callback = Choreographer.FrameCallback { frameTimeNanos ->
        frameTimes.add(frameTimeNanos)
    }
    
    // Measure 60 frames
    repeat(60) {
        Choreographer.getInstance().postFrameCallback(callback)
        Thread.sleep(33) // ~30 FPS expected
    }
    
    // Calculate average FPS
    val intervals = frameTimes.zipWithNext { a, b -> b - a }
    val avgIntervalMs = intervals.average() / 1_000_000.0
    val fps = 1000.0 / avgIntervalMs
    
    assertThat(fps).isAtLeast(28.0)
}

@Test
fun exposureCompensation_affectsCapturedImage() {
    // Capture at -2 EV
    composeRule.onNodeWithTag("ExposureSlider").performTouchInput {
        swipeLeft()
    }
    composeRule.onNodeWithTag("ShutterButton").performClick()
    val darkUri = waitForLatestMediaStoreImage()
    
    // Capture at +2 EV
    composeRule.onNodeWithTag("ExposureSlider").performTouchInput {
        swipeRight()
    }
    composeRule.onNodeWithTag("ShutterButton").performClick()
    val brightUri = waitForLatestMediaStoreImage()
    
    // Compare brightness (simple luminance check)
    val darkBrightness = calculateImageBrightness(darkUri)
    val brightBrightness = calculateImageBrightness(brightUri)
    
    assertThat(brightBrightness).isGreaterThan(darkBrightness)
}
```

### 12.3 Koin in Tests

Override modules with fakes in instrumentation tests:

```kotlin
val testModule = module(override = true) {
    single<CameraRepository> { FakeCameraRepository() }
}

@get:Rule
val koinTestRule = KoinTestRule.create {
    modules(testModule, viewModelModule)
}
```

This allows testing UI + DI + logic without relying on real camera hardware for all runs.[web:73][web:68]

---

## 13. PERFORMANCE TARGETS

| Metric              | Target                           |
|---------------------|----------------------------------|
| Preview FPS         | 28–30 FPS                        |
| Preview Latency     | < 33 ms camera → screen          |
| Capture Processing  | 2–5 s at full resolution         |
| HEIC Encoding       | 1–2 s with hardware codec        |
| File I/O            | < 500 ms per image               |
| Peak Memory         | < 250 MB                         |
| Battery             | Reasonable drain for 1+ hr usage |

Use Android Studio profilers, GPU profiler, and systrace for measurement.[web:60]

---

## 14. KNOWN PITFALLS & GOTCHAS

### Camera2 API Common Mistakes

**Problem:** ImageReader surfaces not closed → memory leak.  
**Solution:** Always close in try-finally or use `.use {}` extension.

```kotlin
// ❌ BAD
val reader = ImageReader.newInstance(width, height, format, maxImages)
// ... might throw before closing

// ✅ GOOD
ImageReader.newInstance(width, height, format, maxImages).use { reader ->
    // ... work with reader
} // auto-closed
```

**Problem:** CaptureRequest set on wrong thread → crash.  
**Solution:** Camera2 callbacks run on Handler thread; use `viewModelScope.launch(Dispatchers.Main)` for UI updates.

**Problem:** Preview stutters when face detection runs every frame.  
**Solution:** Throttle face detection to every 3rd frame or 10 FPS max. Run on downscaled image.

### HEIC Encoding Gotchas

**Problem:** MediaCodec HEIC encoder not available on all devices.  
**Check:** Query `MediaCodecList` for "image/heic" or "image/hevc" before attempting encode.

```kotlin
fun isHeicSupported(): Boolean {
    val codecs = MediaCodecList(MediaCodecList.REGULAR_CODECS)
    return codecs.codecInfos.any { 
        it.supportedTypes.contains("image/heic") || 
        it.supportedTypes.contains("image/hevc")
    }
}
```

**Problem:** HEIC files don't show thumbnails in some gallery apps.  
**Solution:** Embed proper EXIF thumbnail and set correct MIME type in MediaStore.

### OpenGL ES Shader Issues

**Problem:** 3D LUT texture sampler fails silently if texture isn't power-of-2.  
**Solution:** Pad LUT to 64³ or use `GL_TEXTURE_WRAP` properly.

**Problem:** Fragment shader runs slower than expected.  
**Check:** Use `lowp`/`mediump` precision for color values (not `highp`) to save bandwidth.

### Compose + Camera Integration

**Problem:** `AndroidView` with TextureView causes recomposition flicker.  
**Solution:** Use `remember` and `DisposableEffect` to maintain single TextureView instance across recompositions.

```kotlin
@Composable
fun CameraPreviewSurface() {
    val textureView = remember { TextureView(context) }
    DisposableEffect(textureView) {
        // Setup camera
        onDispose { /* cleanup */ }
    }
    AndroidView({ textureView })
}
```

---

## 15. RISK & MITIGATION

| Risk                                 | Mitigation                                                              |
|--------------------------------------|-------------------------------------------------------------------------|
| HEIC not supported on device         | Capability check; fallback to JPEG.[web:52]                             |
| Preview stutters (< 20 FPS)          | Reduce preview size, simplify shaders, throttle face detection.[web:56] |
| Encoding slow on older devices       | Limit max resolution, allow quality setting, prefer hardware codec.[web:52] |
| Face detection adds jitter           | Run on downscaled frame at lower cadence, only for stills.[web:60]      |
| Scoped storage issues                | Strictly follow MediaStore patterns.[web:52]                            |

---

## 16. ROADMAP (MVP) - ATOMIC TASKS

### Phase 1 – Foundation

**Task 1.1:** Initialize project with Gradle, Kotlin 1.9.20+, AGP 8.2+  
**Acceptance:** `./gradlew build` succeeds, app module created.  
**Dependencies:** None  
**Spec References:** §3 Tech Stack, §4 Commands

**Task 1.2:** Setup Koin DI with `cameraModule` and `viewModelModule`  
**Acceptance:** `KoinApplication` initializes in `Application.onCreate()`, test module override works.  
**Dependencies:** 1.1  
**Spec References:** §10.1 Architecture, §12.3 Koin in Tests

**Task 1.3:** Create MainActivity with basic Compose shell (Material3 theme)  
**Acceptance:** App launches, shows "SignatureLens" text, no crashes.  
**Dependencies:** 1.2  
**Spec References:** §11.1 Main Activity, §6.2 Compose

**Task 1.4:** Implement runtime permissions (Camera, optional Location)  
**Acceptance:** App requests camera permission on first launch, handles denial gracefully.  
**Dependencies:** 1.3  
**Spec References:** §9.1 User Flow

### Phase 2 – Camera Preview

**Task 2.1:** Build `CameraRepository` with Camera2 API (open camera, create session)  
**Acceptance:** Unit test with fake `CameraDevice` succeeds; real device preview starts without crash.  
**Dependencies:** 1.2  
**Spec References:** §10.2 Preview Pipeline, §14 Pitfalls (Camera2)

**Task 2.2:** Create `CameraPreviewSurface` Composable wrapping TextureView  
**Acceptance:** Displays live camera feed, no recomposition flicker (validated manually).  
**Dependencies:** 2.1, 1.3  
**Spec References:** §11.2 PreviewScreen, §14 Pitfalls (Compose+Camera)

**Task 2.3:** Wire PreviewViewModel to CameraRepository, expose `StateFlow<PreviewUiState>`  
**Acceptance:** Compose UI test verifies preview state updates when camera starts.  
**Dependencies:** 2.2  
**Spec References:** §10.1 ViewModel

**Task 2.4:** Implement shutter button → capture full-res frame (YUV to bitmap, save as JPEG temp)  
**Acceptance:** Tapping shutter saves `/sdcard/DCIM/temp.jpg`, verified via instrumentation test.  
**Dependencies:** 2.3  
**Spec References:** §10.3 Capture Pipeline (initial, no HEIC yet)

### Phase 3 – Rangefinder Style (GPU Pipeline)

**Task 3.1:** Setup CMake/JNI for libyuv integration (YUV→RGB conversion)  
**Acceptance:** JNI method `convertYuvToRgb()` callable from Kotlin, unit test passes with sample YUV buffer.  
**Dependencies:** None (parallel to Phase 2)  
**Spec References:** §3 Tech Stack (Native), §10.2 Preview Pipeline

**Task 3.2:** Implement OpenGL ES fragment shader with 3D LUT, tone mapping, vignette  
**Acceptance:** Shader compiles, applies to test texture, output matches reference image (screenshot test).  
**Dependencies:** None (parallel)  
**Spec References:** §10.2 Preview Pipeline, §9.2 Look Characteristics, §14 Pitfalls (OpenGL)

**Task 3.3:** Integrate shader into preview pipeline (YUV→RGB→GL→TextureView)  
**Acceptance:** Live preview shows rangefinder look in real-time, 28+ FPS on S23 (profiler verified).  
**Dependencies:** 2.2, 3.1, 3.2  
**Spec References:** §10.2 Preview Pipeline, §13 Performance

**Task 3.4:** Apply shader to full-res capture (not just preview)  
**Acceptance:** Saved JPEG has rangefinder look matching preview (visual comparison test).  
**Dependencies:** 2.4, 3.2  
**Spec References:** §10.3 Capture Pipeline

### Phase 4 – HEIC Output

**Task 4.1:** Check device HEIC codec support, implement fallback logic  
**Acceptance:** `isHeicSupported()` returns correct value on test devices (S23=true, older=false).  
**Dependencies:** None  
**Spec References:** §14 Pitfalls (HEIC Encoding)

**Task 4.2:** Build MediaCodec HEIC encoder pipeline (RGB→HEVC image)  
**Acceptance:** Captures encode to valid `.heic` files viewable in gallery, < 2s encode time on S23.  
**Dependencies:** 4.1, 3.4  
**Spec References:** §10.3 Capture Pipeline, §2.1 In Scope

**Task 4.3:** Embed EXIF metadata (date, orientation, GPS) in HEIC files  
**Acceptance:** `exiftool` confirms metadata presence in saved HEIC files.  
**Dependencies:** 4.2  
**Spec References:** §2.1 In Scope (Metadata)

**Task 4.4:** Save to MediaStore with correct MIME type and folder  
**Acceptance:** Files appear in gallery app under `DCIM/SignatureLens`, instrumentation test verifies.  
**Dependencies:** 4.3  
**Spec References:** §2.1 In Scope (Storage), §14 Pitfalls (HEIC thumbnails)

### Phase 5 – Face Detection & Polish

**Task 5.1:** Integrate ML Kit face detection (on downscaled frames, throttled)  
**Acceptance:** Detects faces in portrait shots, runs at 10 FPS without dropping preview FPS (profiled).  
**Dependencies:** 3.3 (preview running)  
**Spec References:** §2.1 Face-Aware, §14 Pitfalls (face detection throttling)

**Task 5.2:** Adjust shader parameters when faces detected (warm bias, local contrast)  
**Acceptance:** Side-by-side test images show warmer skin tones when face present vs. absent.  
**Dependencies:** 5.1, 3.3  
**Spec References:** §9.2 Look Characteristics

**Task 5.3:** Add UI controls (exposure comp, grid, flash, timer) in Compose  
**Acceptance:** Compose UI test verifies controls visible, state updates on interaction.  
**Dependencies:** 2.3  
**Spec References:** §11.2 PreviewScreen, §2.1 Capture controls

**Task 5.4:** Implement CaptureReviewOverlay (confirm/retake/share)  
**Acceptance:** After capture, overlay appears, tapping confirm saves, retake restarts preview.  
**Dependencies:** 2.4  
**Spec References:** §9.1 User Flow (review overlay), §11.2 PreviewScreen

**Task 5.5:** Write integration tests for end-to-end flow  
**Acceptance:** `captureFlow_savesFileToMediaStore` test passes on real device.  
**Dependencies:** All above  
**Spec References:** §12.2 Integration Tests, §1 Success Criteria

**Task 5.6:** Performance profiling & optimization (reduce latency, memory)  
**Acceptance:** All §13 Performance Targets met on S23/S24, verified via Android Studio Profiler.  
**Dependencies:** All above  
**Spec References:** §13 Performance, §14 Risks

---

**Total MVP:** 6 phases, ~20 atomic tasks. Each task is independently testable and has clear acceptance criteria.

---

## 17. SELF-VERIFICATION CHECKLIST (For AI Agent)

Before marking any task complete, verify:

- [ ] All acceptance criteria from task definition are met
- [ ] Relevant unit/integration tests pass (`./gradlew test connectedAndroidTest`)
- [ ] No new lint errors (`./gradlew lint`)
- [ ] Code follows style guide (§6 Code Style & Naming)
- [ ] Boundaries respected (§8 Always/Ask/Never)
- [ ] No hardcoded secrets or credentials
- [ ] Camera resources properly closed (if applicable)
- [ ] Coroutines used for blocking work (not UI thread)
- [ ] Changes documented if behavior differs from spec

**If any item fails:** Stop, fix, and re-verify before proceeding to next task.

---

## 18. SUMMARY

SignatureLens is a focused, constraint-driven camera app:

- One iconic look.
- Live preview with that look.
- HEIC output by default, JPEG fallback.
- Jetpack Compose UI, Koin DI, Coroutines everywhere.
- Automated integration tests to keep camera + UI flows stable.

Save this file as `SignatureLens_SPEC.md` and treat it as the living spec for implementation.
