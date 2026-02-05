# SignatureLens Spec 2 – Camera Preview & Basic Capture

## 1. Purpose

This spec defines **Iteration 2**: implementing a working **Camera2 preview** and a **basic capture path** (JPEG temp file), without styling or HEIC.

It corresponds to §16 Phase 2 (Tasks 2.1–2.4) in `0-early-spec.md`:

- 2.1 `CameraRepository` with Camera2 API.
- 2.2 `CameraPreviewSurface` composable wrapping `TextureView`.
- 2.3 `PreviewViewModel` wired to the repository.
- 2.4 Shutter → full‑res capture → temporary JPEG save.

Later specs will add the GPU style pipeline and HEIC output; this iteration focuses on **correct, stable camera behavior** and a **minimal end‑to‑end photo flow**.

## 2. Prerequisites & Dependencies

Requires Spec 1 – Foundation to be complete:

- Project builds and runs (`./gradlew assembleDebug`).
- Koin DI initialized with `cameraModule` and `viewModelModule` stubs.
- `MainActivity`, `SignatureLensApp`, `PreviewScreen`, and `PreviewViewModel` placeholders exist.
- CAMERA runtime permission flow is implemented and tested.

Hardware expectations:

- A real device/emulator with a functioning back camera (Camera ID `"0"` is assumed as primary; see Q&A below for overrides).

## 3. In Scope

1. **CameraRepository (Task 2.1)**  
   - Implement `CameraRepository` in `com.signaturelens.camera` to manage:
     - Selecting a camera ID (back camera by default).
     - Opening `CameraDevice` using Camera2 API.
     - Creating a `CameraCaptureSession` with preview and capture surfaces.
     - Exposing suspend functions for:
       - Starting preview on a given `SurfaceTexture` or `Surface`.
       - Stopping/closing the camera and session.
   - Use **Coroutines** and **non‑blocking patterns**:
     - Wrap callback-based Camera2 operations with `suspendCancellableCoroutine` or similar.
   - Ensure all resources (`CameraDevice`, `CameraCaptureSession`, `ImageReader`) are properly closed per §14.

2. **CameraPreviewSurface Composable (Task 2.2)**  
   - Implement `CameraPreviewSurface` in `com.signaturelens.ui.components` (or similar), wrapping `TextureView` or `SurfaceTexture` using `AndroidView`.
   - Responsibilities:
     - Create and remember a single `TextureView` instance across recompositions.
     - Notify caller via `onSurfaceReady(SurfaceTexture)` when ready for preview.
     - Clean up listeners on disposal to avoid leaks/flicker.

3. **PreviewViewModel Wiring (Task 2.3)**  
   - Extend `PreviewViewModel` so that:
     - It depends on a real `CameraRepository` instance from Koin (not a stub).
     - It exposes `StateFlow<PreviewUiState>` as in §10.1, with at least:
       - `isPreviewRunning: Boolean`.
     - `startPreview(surfaceTexture)` triggers CameraRepository preview start and updates state.
     - `stopPreview()` (or equivalent) stops the camera when the screen is disposed or backgrounded.

4. **Basic Capture Flow (Task 2.4)**  
   - Implement a **temporary** still capture path:
     - Shutter tap in `PreviewScreen` calls `PreviewViewModel.capture()`.
     - ViewModel calls a simple capture method (in `CameraRepository` or an early `CaptureRepository`) that:
       - Requests a high‑resolution still frame via Camera2.
       - Receives YUV data in an `ImageReader`.
       - Converts to `Bitmap` (CPU path acceptable in this iteration).
       - Saves a JPEG file to a **temporary** location (e.g., app cache or `DCIM/SignatureLens` with clear “temp” naming).
   - No HEIC, EXIF, or final MediaStore flow yet—this is just a functional capture prototype.

## 4. Out of Scope

- OpenGL / GPU processing, shaders, LUTs, tone mapping.
- HEIC encoding (`MediaCodec`) and EXIF metadata embedding.
- MediaStore final integration for gallery visibility (beyond minimal file write needed to verify capture).
- Face detection and scene‑aware adjustments.
- Final UI polish (controls, review overlay, animations), except minimal shutter wiring.

## 5. Design & Implementation Guidelines

### 5.1 CameraRepository

- Location: `com.signaturelens.camera`.
- Expose a clear API, e.g.:

```kotlin
class CameraRepository(
    private val context: Context,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    suspend fun startPreview(surfaceTexture: SurfaceTexture) { /* ... */ }
    suspend fun stopPreview() { /* ... */ }
    suspend fun captureStill(): File { /* ... */ }
}
```

Key rules:

- Run **all Camera2 work off the main thread**, using `withContext(dispatcher)` where needed.
- Manage lifetime:
  - Keep references to `CameraDevice`, `CameraCaptureSession`, and `ImageReader` fields.
  - Close them in a safe order in `stopPreview()` and when capture operations finish.
- Handle failure explicitly:
  - If camera fails to open or session fails, return a `Result` or throw clear exceptions for ViewModel to catch.

Common questions:

- **Q:** Which camera ID should I use?  
  **A:** Default to the primary back camera, typically `"0"`. If needed, query `CameraManager` and pick the back‑facing ID. Do not expose camera switching in this iteration.

- **Q:** Where should I store temp JPEG files?  
  **A:** Prefer app‑specific cache or `filesDir` for now. Avoid permanent MediaStore writes; those belong in Spec 4. Use a clear prefix like `SignatureLens_temp_*.jpg`.

### 5.2 CameraPreviewSurface

- Follow §14 “Compose + Camera Integration”:
  - Use `remember { TextureView(context) }`.
  - Use `DisposableEffect` to set and clear `SurfaceTextureListener`.
- Suggested signature:

```kotlin
@Composable
fun CameraPreviewSurface(
    modifier: Modifier = Modifier,
    onSurfaceReady: (SurfaceTexture) -> Unit,
)
```

Behavior:

- When the underlying surface becomes available, call `onSurfaceReady`.
- Do **not** start or stop the camera here; delegate that to ViewModel via callbacks.
- Avoid recomposition‑caused flicker by not recreating `TextureView` unnecessarily.

### 5.3 PreviewViewModel

- Extend the skeleton from §10.1:
  - Inject `CameraRepository` via Koin.
  - Maintain immutable `PreviewUiState` with at least `isPreviewRunning`.
- Typical methods:

- `fun startPreview(surfaceTexture: SurfaceTexture)`
- `fun stopPreview()`
- `fun capture()`

Constraints:

- Use `viewModelScope.launch` for all camera calls.
- Use appropriate dispatchers (`Dispatchers.IO` or custom injected dispatcher) for repository work.
- On failures, update state with an error message field (optional) rather than crashing.

Clarifications:

- **Q:** Should `PreviewViewModel` know about file paths?  
  **A:** Minimal awareness is acceptable here (e.g., receiving a `File` or `Uri`), but detailed MediaStore behavior will be handled in Spec 4. Prefer returning `Result<File>` from repository and mapping to a simple UI state (e.g., `lastTempCapturePath`).

### 5.4 Shutter Button Wiring

- In `PreviewScreen` (from Spec 1):
  - Add a minimal shutter button (e.g., `FloatingActionButton`) that calls `viewModel.capture()`.
  - For this iteration, you may:
    - Log or show a simple `Snackbar`/text when capture completes or fails.
    - Skip the full `CaptureReviewOverlay` (that comes in Spec 5).

Clarifications:

- **Q:** Do I need Compose UI tests now?  
  **A:** At least one Compose UI test is recommended to verify:
  - Preview starts when `CameraPreviewSurface` reports a surface.
  - Tapping the shutter doesn’t crash the app.
  More thorough integration tests will be added in later specs.

## 6. Acceptance Criteria

1. **Preview Functionality**
   - On a real device with CAMERA permission granted:
     - Launching the app shows a live camera preview in `PreviewScreen`.
     - There are no crashes due to Camera2 or lifecycle issues during open/close.

2. **Capture Functionality**
   - Tapping the shutter button:
     - Triggers a still capture.
     - Writes a JPEG file to a temp location.
     - Logs or surfaces the path/result for debugging.

3. **Resource Management**
   - `CameraDevice`, `CameraCaptureSession`, and `ImageReader` are closed when:
     - ViewModel `stopPreview()` is called, or
     - Activity goes to background / composable is disposed.
   - No obvious leaks or repeated failures on starting/stopping preview multiple times in a row.

4. **Code Quality**
   - All new code adheres to §6 Code Style and `ai-rules/general.md`.
   - All Camera2 work happens off the main thread.
   - `./gradlew assembleDebug`, `./gradlew test`, and `./gradlew lint` succeed.

## 7. Handover to Next Spec

Spec 3 – Rangefinder Style Pipeline assumes:

- A stable, working preview pipeline: Camera2 → YUV frame → `CameraPreviewSurface` → Compose.
- A basic capture flow that can obtain full‑resolution frames and write them to disk.
- `CameraRepository` and `PreviewViewModel` exist and can be extended to route frames through the GPU style pipeline.

Agents implementing Spec 3 MUST reuse and extend these components rather than replacing them wholesale, unless changes are strictly necessary to meet §10.2 and §13 performance targets.

