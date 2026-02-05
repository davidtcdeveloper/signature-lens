# SignatureLens Spec 3 – Rangefinder Style GPU Pipeline

## 1. Purpose

This spec defines **Iteration 3**: implementing the **GPU‑accelerated rangefinder look** for both preview and captures.

It corresponds to §16 Phase 3 (Tasks 3.1–3.4) in `0-early-spec.md`:

- 3.1 CMake/JNI for libyuv (YUV→RGB conversion).
- 3.2 OpenGL ES fragment shader with 3D LUT, tone mapping, vignette.
- 3.3 Integration into the preview pipeline.
- 3.4 Applying the shader to full‑resolution captures.

The goal is that:

- **Preview** shows the iconic look in real time.  
- **Captured JPEGs** (still temp in this phase) visually match the preview look.

HEIC encoding and MediaStore integration will be added in Spec 4.

## 2. Prerequisites & Dependencies

Requires completion of:

- Spec 1 – Foundation (project, Koin, Compose shell, permissions).
- Spec 2 – Camera Preview & Basic Capture:
  - Stable live Camera2 preview.
  - Basic capture path producing JPEG temp files.

Environment assumptions:

- NDK and CMake toolchain available for building native code.

## 3. In Scope

1. **Native YUV→RGB Conversion (Task 3.1)**  
   - Implement `core-native` module as described in §5:
     - `CMakeLists.txt` configured for libyuv.
     - `yuv_converter.cpp` with JNI‑exposed functions.
   - Provide a Kotlin/Java wrapper in `com.signaturelens.processing`:
     - `convertYuvToRgb(yBuffer, uBuffer, vBuffer, width, height, outBuffer)` or similar.
   - Ensure correctness via unit tests on small test buffers.

2. **OpenGL ES Fragment Shader (Task 3.2)**  
   - Implement shaders (e.g., in `assets` or `res/raw`), and GL code in `color_pipeline.cpp` + Kotlin wrapper.
   - Shader responsibilities per §9.2 and §10.2:
     - Apply 3D LUT for color grading (warm bias, magenta skin tones).
     - Apply tone curve (lifted blacks, protected highlights, S‑curve).
     - Add subtle vignette and grain.
   - Design shader parameters so they can later be adjusted for face‑aware tuning (Spec 5).

3. **Preview Integration (Task 3.3)**  
   - Update the preview pipeline:
     - Camera2 → `YUV_420_888` → libyuv (YUV→RGB) → OpenGL shader → texture → `TextureView` → Compose.
   - Ensure real‑time performance: target 28–30 FPS on Galaxy S23/S24 (§13).
   - Provide a clean API from Kotlin to drive the GL pipeline (e.g., `StyleRenderer` class).

4. **Styled Capture Integration (Task 3.4)**  
   - Modify the capture path from Spec 2:
     - Instead of saving a “raw” JPEG, route the captured frame through the same style pipeline as preview.
   - Ensure that:
     - The look applied to full‑res captures matches preview closely (WYSIWYG).
     - Processing time remains within §13 targets (2–5 s for full resolution).

## 4. Out of Scope

- HEIC encoding (`MediaCodec`) and final MediaStore integration.
- Face detection and any scene/portrait tuning.
- UI controls, review overlay, or detailed UX polish beyond what is needed to verify the look.
- Extensive configuration UI for look parameters (the aesthetic is intentionally constrained).

## 5. Design & Implementation Guidelines

### 5.1 Native Layer & JNI

- Folder structure as per §5:
  - `core-native/CMakeLists.txt`
  - `core-native/src/main/cpp/yuv_converter.cpp`
  - `core-native/src/main/cpp/color_pipeline.cpp`
- Use libyuv for efficient YUV→RGB conversions:
  - Optimize for the preview resolution; full‑res path may allow more time but should still be efficient.

Typical questions:

- **Q:** Can I use a pure Kotlin/Java implementation for YUV→RGB?  
  **A:** For this spec, use libyuv as specified. A temporary pure Kotlin path may exist for bootstrapping, but the final path must use libyuv for performance.

- **Q:** How should I handle color spaces?  
  **A:** Assume standard camera YUV → sRGB pipeline unless device‑specific corrections are required. Focus on correctness and consistency; more advanced color management can be out of scope for MVP.

### 5.2 Shader & Style Design

- Implement the look described in §9.2:
  - Warm bias (+few hundred K) and subtle magenta push in skin tones.
  - Lifted blacks, protected highlights, S‑curve midtone contrast.
  - Subtle micro‑contrast, light grain, and shallow vignette.
- Recommended architecture:
  - 3D LUT texture (e.g., 64×64×64) sampled in fragment shader.
  - Additional math on luminance for tone curve and vignette.
- Ensure performance:
  - Use `mediump` where reasonable (see §14 OpenGL Pitfalls).
  - Avoid unnecessary texture fetches and branching.

Typical questions:

- **Q:** Where do LUT assets live?  
  **A:** Store in `assets/` or `res/raw/`, then load into a GL texture at initialization.

- **Q:** How do I validate the look?  
  **A:** Add a simple testing path:
  - Render a known test image through the shader.
  - Capture output to a bitmap and compare (visually or via simple metrics) to a reference.

### 5.3 Preview Pipeline Integration

- Connect the style pipeline into the existing preview flow from Spec 2:

1. Camera2 provides frames (`YUV_420_888`) into an `ImageReader` or similar.
2. Native libyuv converts to RGB buffer or texture‑ready format.
3. OpenGL shader renders the styled frame into a texture bound to the `TextureView` surface.

- Consider threading model:
  - Heavy work must NOT run on main thread.
  - Use a dedicated GL thread or background handler thread with Coroutines integration.

Clarifications:

- **Q:** Should the ViewModel own GL resources?  
  **A:** No. Keep GL resource management in a dedicated rendering class (e.g., `StyleRenderer`) that the ViewModel orchestrates via high‑level calls. ViewModel should remain a state + coordination layer.

- **Q:** How do I pause rendering when app goes background?  
  **A:** Tie renderer lifecycle to the preview surface and ViewModel’s `stopPreview()`, ensuring GL context cleanup when appropriate.

### 5.4 Styled Capture Path

- Reuse as much of the preview style pipeline as possible:
  - Capture full‑res frame.
  - Convert via libyuv.
  - Run through the shader at capture resolution (may be slower than preview but must meet §13).
  - Write out a **styled JPEG** temp file.

Guidelines:

- If full‑res GPU pipeline is too heavy, consider:
  - Smaller offscreen render target with careful upscaling, but maintain perceived quality.
  - Or a CPU approximation that is “close enough” while still following the tonal and color spec.
  - Document any approximations clearly in comments.

Clarifications:

- **Q:** Do I need HEIC here?  
  **A:** No. Continue writing JPEG temp files in this spec. HEIC and MediaStore happen in Spec 4.

## 6. Acceptance Criteria

1. **Preview Styling**
   - On a supported device, live preview shows the rangefinder aesthetic as described in §9.2.
   - Frame rate averages ≥ 28 FPS on S23/S24 (measured via profiler or simple timing).

2. **Capture Styling**
   - Captured JPEG temp files exhibit the same aesthetic as preview (no “flat” captures).
   - Capture processing time for full‑res frames remains within §13 (2–5 s).

3. **Native Integration**
   - JNI functions for YUV→RGB and style rendering compile and run without crashes.
   - At least one unit/instrumentation test validates YUV→RGB correctness on test data.

4. **Code Quality & Stability**
   - No GL context leaks or repeated initialization on every frame.
   - Camera and GL resources are cleaned up on app/background transitions.
   - `./gradlew assembleDebug`, `./gradlew test`, and `./gradlew lint` succeed.

## 7. Handover to Next Spec

Spec 4 – HEIC Output & MediaStore assumes:

- Preview frames and captured images already pass through the style pipeline.
- There exists a well‑defined API to obtain a **styled full‑resolution bitmap/buffer** ready for encoding.
- The app can already produce styled JPEG temp files for testing.

The next iteration will replace the temporary JPEG path with a robust **HEIC + JPEG fallback + MediaStore** pipeline while preserving the visual consistency and performance guarantees established here.

