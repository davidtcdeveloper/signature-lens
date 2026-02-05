# SignatureLens Spec 5 – Intelligence, Controls & Polish

## 1. Purpose

This spec defines **Iteration 5**: adding **scene intelligence**, **core UI controls**, **capture review**, and **final validation**.

It corresponds to §16 Phase 5 (Tasks 5.1–5.6) in `0-early-spec.md`:

- 5.1 ML Kit face detection.
- 5.2 Face‑aware shader parameter adjustments.
- 5.3 UI controls (exposure, grid, flash, timer).
- 5.4 Capture review overlay (confirm/retake/share).
- 5.5 End‑to‑end integration tests.
- 5.6 Performance profiling and optimization.

After this iteration, the app should meet the **MVP success criteria** in §1 and performance targets in §13.

## 2. Prerequisites & Dependencies

Requires completion of:

- Spec 1 – Foundation.
- Spec 2 – Camera Preview & Basic Capture.
- Spec 3 – Rangefinder Style GPU Pipeline.
- Spec 4 – HEIC Output & MediaStore Integration.

At this point:

- Preview is styled and stable.
- Captures are styled, encoded to HEIC/JPEG, and saved to MediaStore with EXIF.

## 3. In Scope

1. **ML Kit Face Detection (Task 5.1)**  
   - Integrate ML Kit Vision face detection as per §2.1 and §14:
     - Run on downscaled frames (not full resolution).
     - Throttle to at most ~10 FPS or every Nth frame to avoid preview stutter.
   - Expose face presence information to the shader pipeline (e.g., via a simple flag or light metadata).

2. **Face‑Aware Shader Adjustments (Task 5.2)**  
   - Modify the style pipeline (from Spec 3) so that:
     - When faces are detected, skin tones receive a slightly warmer, friendlier rendering (e.g., mild warm/magenta bias, slight local contrast tweaks).
     - When no faces are present, rendering remains neutral per base look.
   - Ensure changes are subtle and do not break the overall aesthetic.

3. **UI Controls (Task 5.3)**  
   - Implement in `PreviewScreen`:
     - Exposure compensation slider (±3 EV).
     - Grid overlay toggle.
     - Flash mode selector: auto / on / off.
     - Self‑timer: off / 3 s / 10 s.
   - All controls must:
     - Be represented in `PreviewUiState`.
     - Persist via a simple settings layer (DataStore or SharedPrefs) so they survive process death, per §12.2.

4. **Capture Review Overlay (Task 5.4)**  
   - Implement `CaptureReviewOverlay` composable:
     - Displays the last captured image (from `Uri`).
     - Provides actions:
       - Confirm/save (default; may simply dismiss overlay since file is already in MediaStore).
       - Retake (returns to preview without re‑saving).
       - Share (fires share intent, converting to JPEG if necessary).
   - Integrate with `PreviewViewModel` state (`lastCapture` etc.) as sketched in §11.2.

5. **Integration Tests (Task 5.5)**  
   - Add instrumentation tests for key flows per §12.2:
     - Launch → preview visible → shutter tap → review → confirm → file in MediaStore with correct MIME.
     - Settings (grid, exposure) persisted across process death.
   - Use Koin test modules with fakes where appropriate (§12.3).

6. **Performance Profiling & Optimization (Task 5.6)**  
   - Measure and, if necessary, optimize:
     - Preview FPS (target 28–30).
     - Camera→screen latency (< 33 ms).
     - Capture processing and encoding times.
     - Memory usage (< 250 MB peak).
   - Document any device‑specific compromises (e.g., reduced preview resolution) within code comments.

## 4. Out of Scope

- New looks or presets beyond the single defined aesthetic.
- Advanced pro features (histograms, zebras, focus peaking).
- Network‑based features (analytics, sync, remote config).

## 5. Design & Implementation Guidelines

### 5.1 Face Detection Integration

- Use ML Kit Vision face detection as specified in §3:
  - Run on a copy of preview frames at a reduced resolution.
  - Perform detection on a background dispatcher (e.g., `Dispatchers.Default`).
- Throttling strategies (choose one and document):
  - Run detection every Nth frame (e.g., every 3rd or 5th).
  - Or run at a fixed cadence (e.g., ~10 FPS max) using timestamps.

Data flow:

- Detection result → `PreviewViewModel` → style pipeline:
  - ViewModel updates a simple flag in `PreviewUiState` (e.g., `hasFaces`).
  - Renderer queries this flag or receives it as a parameter for each frame.

Typical questions:

- **Q:** Should I detect multiple faces or just presence?  
  **A:** For MVP, mere presence (boolean) is enough. You can extend to count/focus later if needed.

- **Q:** Do I run detection on full‑res frames?  
  **A:** No. Always use downscaled images and throttle to avoid performance regressions.

### 5.2 Face‑Aware Style Tuning

- In the shader or GPU pipeline:
  - Introduce a uniform (e.g., `uHasFaces`) to toggle subtle adjustments:
    - Slight increase in warmth and magenta for skin regions.
    - Gentle micro‑contrast to increase perceived “pop”.
  - Consider simple heuristics for skin region approximation (e.g., hue/luminance ranges).

Constraints:

- Changes must be **subtle** and not drastically alter the base look.
- When `hasFaces` is false, rendering should default to the base style from Spec 3.

### 5.3 UI Controls & State

- Implement dedicated composables in `ui/components` (per `ai-rules/compose.md`):
  - `ExposureSlider`, `GridToggle`, `FlashModeSelector`, `TimerSelector`, etc.
- Extend `PreviewUiState` with:
  - `exposureComp: Float`.
  - `gridEnabled: Boolean`.
  - `flashMode: FlashMode` (enum).
  - `timerSeconds: Int` or similar.

- Wire to `PreviewViewModel`:
  - Functions like `onExposureChange`, `toggleGrid`, `setFlashMode`, `setTimer`.
  - Ensure settings updates are persisted via a simple repository (e.g., `SettingsRepository`).

Clarifications:

- **Q:** Should settings be global or per‑session only?  
  **A:** Persist them across app restarts for a consistent, opinionated experience.

- **Q:** How does exposure comp interact with Camera2?  
  **A:** Map the UI value (±3 EV) to Camera2 exposure compensation parameters in `CameraRepository` while respecting device limits.

### 5.4 Capture Review Overlay & Share

- Implement `CaptureReviewOverlay` composable that:
  - Takes current capture data (e.g., `CaptureResult` with `Uri` and format).
  - Displays the image.
  - Offers actions:
    - Confirm: clears overlay and returns to preview.
    - Retake: optionally deletes or flags the last capture (document strategy) and restarts preview.
    - Share: fires `Intent.ACTION_SEND` with appropriate MIME:
      - If target might not support HEIC, convert to JPEG for sharing.

- Integrate into `PreviewScreen` similar to §11.2 sketch:
  - Conditionally render overlay when `uiState.lastCapture` is non‑null.
  - Ensure back button behavior is sane (e.g., closing overlay before exiting app).

### 5.5 Integration Tests

- Key instrumentation tests (per §12.2 & code samples):
  - `captureFlow_savesHeicFileToMediaStore()`: ensures final HEIC path works.
  - Settings persistence test: change grid/exposure, kill process, relaunch, verify state restored.
  - Optional: simple face‑aware test with a static face sample to verify that detection code path runs (even if visual diff is not fully automated).

- Use Koin test modules:
  - Inject fakes for camera or storage when helpful.
  - Keep tests deterministic and not overly dependent on physical hardware quirks.

### 5.6 Performance Profiling

- Use tools from §13 and §14:
  - Android Studio profilers for CPU/memory.
  - GPU profiler / systrace for frame pacing.
  - Simple in‑app sampling for FPS if needed.

Targets:

- Preview:
  - FPS: 28–30 on S23/S24.
  - Latency: < 33 ms camera→screen.
- Capture:
  - Processing + encoding within 2–5 s for full‑res frames.
- Memory:
  - Peak usage < 250 MB during normal usage.

If targets are not met:

- Identify hot spots:
  - Overly frequent face detection.
  - Oversized preview resolution.
  - Inefficient GL passes or intermediate copies.
- Apply targeted optimizations:
  - Reduce face detection cadence.
  - Adjust preview resolution.
  - Simplify shaders (e.g., fewer texture samples).

Document any significant trade‑offs in comments.

## 6. Acceptance Criteria

1. **Scene Intelligence**
   - Face detection runs on downscaled frames, throttled to avoid stutter.
   - When faces are present, captured portraits show slightly warmer, more flattering rendering compared to non‑face scenes.

2. **Controls & Review**
   - Exposure, grid, flash, and timer controls:
     - Are visible and responsive in `PreviewScreen`.
     - Persist across app restarts.
   - After capture:
     - Review overlay appears with the last image.
     - Confirm/retake/share actions behave as expected.

3. **End‑to‑End Tests**
   - Integration tests for capture flow and settings persistence pass on a real device.
   - Core flows match the user journey in §1.1 (open → styled preview → capture → review → gallery).

4. **Performance**
   - Measured metrics on S23/S24 meet §13 targets or are within an acceptable documented deviation.
   - No major regressions introduced by face detection or controls.

5. **Quality & Boundaries**
   - All new code respects §6 Code Style, §8 Boundaries, and `ai-rules/*.md`.
   - `./gradlew assembleDebug`, `./gradlew test`, `./gradlew connectedAndroidTest`, and `./gradlew lint` succeed with no new critical issues.

## 7. Completion of MVP

With Spec 5 completed, SignatureLens should:

- Deliver a **single, iconic look** in both preview and capture.
- Encode to **HEIC by default**, with robust JPEG fallback.
- Provide **face‑aware refinement**, **essential camera controls**, and a **smooth capture→review→gallery** flow.
- Meet or closely approach the performance targets in §13.

Future specs (if needed) can focus on refinements, additional tests, or platform‑specific optimizations, but the MVP as defined in `0-early-spec.md` is considered complete after this iteration.

