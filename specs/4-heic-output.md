# SignatureLens Spec 4 – HEIC Output & MediaStore Integration

## 1. Purpose

This spec defines **Iteration 4**: implementing **HEIC output with JPEG fallback** and **MediaStore integration**, including EXIF metadata.

It corresponds to §16 Phase 4 (Tasks 4.1–4.4) in `0-early-spec.md`:

- 4.1 HEIC codec capability check and fallback logic.
- 4.2 MediaCodec HEIC encoder pipeline.
- 4.3 EXIF metadata embedding.
- 4.4 MediaStore save with correct MIME type and folder.

The goal is that **every captured image**:

- Is styled (per Spec 3).
- Is saved as HEIC where supported, else JPEG.
- Appears correctly in gallery apps under `DCIM/SignatureLens`.

## 2. Prerequisites & Dependencies

Requires completion of:

- Spec 1 – Foundation (project, DI, Compose shell, permissions).
- Spec 2 – Camera Preview & Basic Capture (functional capture path).
- Spec 3 – Rangefinder Style GPU Pipeline (styled preview and captures).

Runtime expectations:

- Device(s) with and without HEIC support for validation, or at least one with HEIC to verify primary path.

## 3. In Scope

1. **HEIC Capability Detection (Task 4.1)**  
   - Implement `isHeicSupported()` as in §14:
     - Use `MediaCodecList` to detect support for `"image/heic"` or `"image/hevc"`.
   - Expose this check via a utility (e.g., `HeicSupportChecker`) in `com.signaturelens.encoding`.

2. **MediaCodec HEIC Encoding Pipeline (Task 4.2)**  
   - Implement an encoder that:
     - Accepts styled image data (e.g., `Bitmap` or encoded buffer) from Spec 3.
     - Uses `MediaCodec` with HEIC/HEVC **image** profile to encode to `.heic`.
   - Provide a clear Kotlin API, e.g.:

```kotlin
interface ImageEncoder {
    suspend fun encodeStyledImage(
        styledBitmap: Bitmap,
        format: FileFormat,
    ): File
}
```

   - Where `FileFormat` is an enum (`HEIC`, `JPEG`) used for fallback.

3. **EXIF Metadata Embedding (Task 4.3)**  
   - Use `ExifInterface` to write:
     - Date/time.
     - Orientation.
     - Camera info.
     - GPS (if permission granted and location is available).
   - Ensure EXIF is present for both HEIC and JPEG outputs (subject to platform capabilities).

4. **MediaStore Save (Task 4.4)**  
   - Implement final file save using `MediaStore`:
     - Target folder: `DCIM/SignatureLens`.
     - Correct MIME types:
       - `image/heic` for HEIC.
       - `image/jpeg` for JPEG.
   - Ensure:
     - Files appear in gallery apps with thumbnails.
     - Display names use a consistent pattern (e.g., `SignatureLens_YYYYMMDD_HHMMSS.ext` per §12.1 example).

5. **CaptureRepository Integration**

   - Introduce or extend `CaptureRepository` in `com.signaturelens.storage` (or similar) to:
     - Orchestrate styled capture → encode → MediaStore insert.
     - Encapsulate HEIC vs JPEG decisions.
   - `PreviewViewModel.capture()` should call into `CaptureRepository` instead of directly handling encoding or MediaStore.

## 4. Out of Scope

- Face detection, scene intelligence, or look parameter adjustments.
- UI controls, capture review overlay, or sharing intents (beyond returning a `Uri`).
- Deep compatibility layers for exotic devices; focus on modern Android 10+ and Samsung S23/S24 first.

## 5. Design & Implementation Guidelines

### 5.1 HEIC Support & Fallback

- Implement a reusable check:

```kotlin
fun isHeicSupported(): Boolean {
    val codecs = MediaCodecList(MediaCodecList.REGULAR_CODECS)
    return codecs.codecInfos.any {
        it.supportedTypes.contains("image/heic") ||
            it.supportedTypes.contains("image/hevc")
    }
}
```

Usage guidelines:

- Only attempt HEIC encoding when `isHeicSupported()` is true.
- Otherwise, fall back to JPEG encoding.
- Consider caching the result in memory for the app session to avoid repeated scanning.

Typical questions:

- **Q:** Should I expose HEIC/JPEG choice to the user?  
  **A:** No. The app is intentionally opinionated; format is automatic (HEIC when possible).

- **Q:** What about devices that support HEVC video but not HEIC image?  
  **A:** The capability check above should handle this; if in doubt, be conservative and fall back to JPEG.

### 5.2 Encoding Pipeline

- Implement HEIC encode path:
  - Accept styled `Bitmap` or RGBA buffer produced by Spec 3.
  - Use `MediaCodec` in asynchronous or synchronous mode (synchronous is acceptable if correctly off‑main thread).
  - Handle encoder configuration (width, height, color format, bit rate, etc.) per platform guidelines.
  - Write encoded output to a file or directly to an OutputStream that backs the MediaStore entry.

- Implement JPEG fallback:
  - Use `Bitmap.compress()` or similar APIs on the styled bitmap.
  - Ensure quality vs. file size balance consistent with §1.2 (2–5 MB at 12–24 MP).

Clarifications:

- **Q:** Can I bypass styled pipeline and encode “flat” images for HEIC?  
  **A:** No. All final images must be styled; HEIC vs JPEG is a container decision only.

- **Q:** Should encoding happen on a background dispatcher?  
  **A:** Yes. Use `Dispatchers.IO` (or injected dispatcher) to ensure no blocking on the main thread.

### 5.3 EXIF Metadata

- After encoding, use `ExifInterface` to open the file and write tags:
  - `TAG_DATETIME_ORIGINAL`, `TAG_ORIENTATION`, `TAG_MAKE`, `TAG_MODEL`, etc.
  - GPS tags when location permission is granted and a location is available.
- Ensure orientation correctness:
  - Respect device rotation and Camera2 sensor orientation.

Typical questions:

- **Q:** Is EXIF mandatory on all formats?  
  **A:** Yes, where supported. If a device cannot embed full EXIF for HEIC, document the limitation in code comments and still ensure JPEG fallback is correct.

### 5.4 MediaStore Integration

- Use `ContentResolver.insert` with `MediaStore.Images.Media.EXTERNAL_CONTENT_URI`:
  - Set `DISPLAY_NAME`, `MIME_TYPE`, and `RELATIVE_PATH` to `DCIM/SignatureLens`.
  - Write image data via the returned `OutputStream`.
  - Close streams and the cursor carefully to avoid leaks.

- Verify gallery visibility:
  - After capture, the file should be visible in common gallery apps.
  - Thumbnails should render correctly (fix by ensuring correct MIME + EXIF thumbnail if needed, see §14).

Clarifications:

- **Q:** Should I keep writing to temp files first then copy to MediaStore?  
  **A:** Preferred approach is to write directly to the MediaStore `OutputStream` after encoding, to avoid redundant I/O. However, a temp file → copy approach is acceptable if simpler, as long as it meets performance targets.

### 5.5 CaptureRepository & ViewModel Contract

- `CaptureRepository` should:
  - Accept a request for capture (e.g., `suspend fun captureStyledImage(): Uri`).
  - Internally:
    - Use existing camera pipeline to obtain a styled full‑res image buffer/bitmap.
    - Encode to HEIC or JPEG.
    - Save via MediaStore and return the resulting `Uri`.

- `PreviewViewModel.capture()` should:
  - Call `CaptureRepository`.
  - Update `PreviewUiState` with:
    - `lastCapture` or similar type containing the `Uri` and format.
  - Surface errors through state (e.g., `errorMessage`) rather than crashing.

## 6. Acceptance Criteria

1. **HEIC Detection & Fallback**
   - On a HEIC‑capable device (e.g., S23/S24), `isHeicSupported()` returns true.
   - On at least one non‑HEIC environment (emulator/older device), it returns false.

2. **Encoding Behavior**
   - On HEIC‑capable devices:
     - Captures are encoded as `.heic` with MIME `image/heic`.
     - Encoding completes within 1–2 s per §13.
   - On non‑HEIC devices:
     - Captures fall back to JPEG with MIME `image/jpeg`.
     - There are no crashes due to unsupported codecs.

3. **Metadata & MediaStore**
   - Saved files contain correct EXIF metadata (time, orientation; GPS when allowed).
   - Files appear under `DCIM/SignatureLens` in gallery apps.
   - Thumbnails are visible; no “broken” entries in gallery.

4. **Integration & Stability**
   - `PreviewViewModel.capture()` uses `CaptureRepository` (no duplicated encoding/storage logic in the ViewModel).
   - `./gradlew assembleDebug`, `./gradlew test`, `./gradlew connectedAndroidTest` (for basic capture flow), and `./gradlew lint` succeed.

## 7. Handover to Next Spec

Spec 5 – Intelligence, Controls & Polish assumes:

- Capture flow already produces **styled**, properly encoded images with correct EXIF and MediaStore integration.
- `CaptureRepository` and `PreviewViewModel` expose a `Uri` or similar handle to the last capture.
- The app’s storage behavior (paths, MIME types, format decisions) is stable.

The next iteration will add **face detection**, **scene‑aware tuning**, **UI controls**, **review overlay**, and **end‑to‑end tests and performance validation**, building on this finalized encoding and storage pipeline.

