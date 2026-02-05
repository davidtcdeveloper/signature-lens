```markdown
# SignatureLens - Product & Technical Specification

**Status:** MVP Specification  
**Version:** 1.1 (Compose + Koin + Coroutines + Integration tests)  
**Last Updated:** February 4, 2026

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

**Success Criteria:**

- Real-time preview shows the final look (WYSIWYG).[web:51][web:54]
- Shutter-to-file (HEIC) under 7 seconds on a Galaxy S23/S24 class device.[web:60]
- Preview at 28–30 FPS with end-to-end frame latency < 33 ms.[web:56][web:59]
- HEIC files typically 2–5 MB for 12–24 MP shots (≈60–70% smaller than JPEG).[web:52][web:61]
- Portraits, landscapes, and architecture are processed intelligently (face-aware and simple scene-aware tuning).
- Capture → review → save flow takes under 10 seconds for a typical user.

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

| Layer       | Technology                         | Notes                                                                    |
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

Key flows to cover:

1. App launch → preview visible → shutter tap → file appears in MediaStore.
2. Settings (grid, exposure) survive process death.
3. Fakes for CameraRepository in instrumentation to avoid hardware dependency where needed.

Dependencies:

```kotlin
androidTestImplementation("androidx.test.ext:junit:1.1.5")
androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
androidTestImplementation("androidx.compose.ui:ui-test-junit4:<latest>")
androidTestImplementation("androidx.test:rules:1.5.0")
androidTestImplementation("androidx.test:runner:1.5.2")
```

Example Compose test:

```kotlin
@get:Rule
val composeRule = createAndroidComposeRule<MainActivity>()

@Test
fun captureFlow_savesFileToMediaStore() {
    composeRule.onNodeWithTag("ShutterButton").performClick()

    // Wait for capture to complete (state or IdlingResource)
    // Then assert MediaStore contains new item with expected MIME type
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

## 14. RISK & MITIGATION

| Risk                                 | Mitigation                                                              |
|--------------------------------------|-------------------------------------------------------------------------|
| HEIC not supported on device         | Capability check; fallback to JPEG.[web:52]                             |
| Preview stutters (< 20 FPS)          | Reduce preview size, simplify shaders, throttle face detection.[web:56] |
| Encoding slow on older devices       | Limit max resolution, allow quality setting, prefer hardware codec.[web:52] |
| Face detection adds jitter           | Run on downscaled frame at lower cadence, only for stills.[web:60]      |
| Scoped storage issues                | Strictly follow MediaStore patterns.[web:52]                            |

---

## 15. ROADMAP (MVP)

**Phase 1 – Core (2–3 weeks)**  
- Project setup, DI (Koin), basic Compose shell.
- Camera2 + TextureView based preview.
- Basic shutter → capture path (no style yet).

**Phase 2 – Style & Pipeline (3–4 weeks)**  
- libyuv JNI integration.[web:62]  
- OpenGL ES pipeline, LUT-based grading, tone mapping.[web:56]  
- Integrate with preview.

**Phase 3 – HEIC & Storage (2–3 weeks)**  
- MediaCodec HEIC pipeline.[web:52]  
- EXIF metadata, MediaStore saving.

**Phase 4 – Face-Aware + Polish (2–3 weeks)**  
- ML Kit face detection integration.[web:60]  
- Subtle portrait biasing.  
- Settings integration in Compose UI.  
- Performance optimization, integration tests.

Total MVP: ~9–13 weeks for a single experienced dev, less with a small team.[web:60]

---

## 16. SUMMARY

SignatureLens is a focused, constraint-driven camera app:

- One iconic look.
- Live preview with that look.
- HEIC output by default, JPEG fallback.
- Jetpack Compose UI, Koin DI, Coroutines everywhere.
- Automated integration tests to keep camera + UI flows stable.

Save this file as `SignatureLens_SPEC.md` and treat it as the living spec for implementation.
