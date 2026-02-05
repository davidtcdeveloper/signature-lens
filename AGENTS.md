# Agent Guide for SignatureLens

## Purpose

Agents act as senior Android/Kotlin engineers specialized in camera applications. Keep responses concise, clarify uncertainty before coding, and align all suggestions with the rules and specifications linked below.

## Rule Index

- **@ai-rules/rule-loading.md** — ALWAYS load this file first to understand which other rules to load
- **@specs/0-early-spec.md** — Complete technical specification (reference specific sections as needed)

## Repository Overview

**SignatureLens** is an Android camera application that captures photos with a single, iconic rangefinder-style aesthetic. The app applies the look in real-time preview and saves styled HEIC files—no presets, no editing, just one distinctive visual identity.

**Deep product and architecture context:**
- Full specification: @specs/0-early-spec.md
- Architecture: Jetpack Compose UI + Camera2 API + OpenGL ES processing pipeline
- Philosophy: Constraint-driven design (one look, applied intelligently)
- Target: Android 10+ (API 29+), optimized for Samsung Galaxy S23/S24

**Key technical decisions:**
- **UI Framework:** 100% Jetpack Compose (no XML layouts)
- **DI:** Koin for dependency injection (including Compose integration)
- **Async:** Kotlin Coroutines + Flow for all background work
- **Camera:** Camera2 API (low-level control, not CameraX)
- **Processing:** OpenGL ES 3.1 shaders (3D LUT color grading, tone mapping)
- **Encoding:** MediaCodec for HEIC/HEVC image format
- **ML:** ML Kit Vision for on-device face detection

## Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug

# Run unit tests
./gradlew test

# Run instrumented Android tests (requires device/emulator)
./gradlew connectedAndroidTest

# Lint all code
./gradlew lint

# Format code (if using ktlint)
./gradlew ktlintFormat

# Launch main activity (after install)
adb shell am start -n com.signaturelens/.ui.MainActivity
```

## Code Style

- **Kotlin conventions:** 4-space indentation, PascalCase for classes, camelCase for functions/properties
- **Packages:** `com.signaturelens.<feature>` structure
- **Constants:** UPPER_SNAKE_CASE
- **Composables:** PascalCase, use `@Composable` annotation
- **Never:** block UI thread, use deprecated android.hardware.Camera, bypass permissions
- **Always:** use Coroutines for blocking work, close camera resources, use DI for dependencies

Detailed style guide: @specs/0-early-spec.md §6

## Architecture & Patterns

**MVVM + Repository Pattern:**
- ViewModels expose `StateFlow<UiState>` to Composables
- Repositories handle data/device operations (Camera2, MediaStore)
- Koin provides dependencies via `viewModel { }` and `single { }`

**Preview Pipeline:**
Camera2 → YUV frames → JNI/libyuv → RGB → OpenGL shader → TextureView → Compose

**Capture Pipeline:**
Camera2 still capture → Full-res frame → Color pipeline → MediaCodec HEIC encode → MediaStore

**Dependency Injection:**
```kotlin
val cameraModule = module {
    single { CameraRepository(androidContext()) }
    single { CaptureRepository(get(), get()) }
}

val viewModelModule = module {
    viewModel { PreviewViewModel(get(), get()) }
}
```

Full architecture: @specs/0-early-spec.md §10

## Key Integration Points

**Camera2 API:**
- Open camera → Create session → Repeating requests for preview
- ImageReader for capture callbacks
- Always close resources in try-finally or `.use { }`

**OpenGL ES Pipeline:**
- Fragment shaders apply 3D LUT, tone curves, vignette, grain
- Run on GPU for real-time preview (< 33ms per frame)

**HEIC Encoding:**
- Check codec availability with MediaCodecList
- Fall back to JPEG if HEIC not supported
- Embed EXIF metadata (time, orientation, GPS)

**MediaStore:**
- Scoped storage for Android 10+
- Save to `DCIM/SignatureLens` with correct MIME type
- Use ContentResolver for gallery integration

**Testing:**
- JUnit + AndroidX Test for instrumentation
- Compose UI Test for UI flows
- Koin test modules with fakes (override production modules)

Full integration details: @specs/0-early-spec.md §10, §11, §12

## Workflow

1. **Clarify first:** Ask for clarification when requirements are ambiguous
2. **Surface options:** Present 2-3 implementation approaches when trade-offs exist
3. **Reference spec:** Cite specific spec sections (e.g., "Per §10.2 Preview Pipeline...")
4. **Atomic tasks:** Break work into independent, testable tasks (see §16 Roadmap)
5. **Self-verify:** Run checklist from §17 before marking tasks complete
6. **Document changes:** Update specs if behavior diverges from documented approach

**Commit format:** `<type>(<scope>): summary`
- Examples: `feat(camera): add exposure compensation`, `fix(encoding): handle heic fallback`
- Types: feat, fix, refactor, test, docs, chore

Detailed workflow: @specs/0-early-spec.md §7, §17

## Testing

**Unit Tests:**
- Test pure logic (tone curves, color math, file naming)
- Mock/fake dependencies via Koin test modules
- Use `@Test` annotations with JUnit

**Integration Tests:**
- Test full flows on device/emulator
- Use `createAndroidComposeRule<MainActivity>()` for Compose tests
- Verify MediaStore, camera state, performance metrics

**Performance Validation:**
- Systrace for frame latency (target < 33ms)
- Android Studio Profiler for memory/CPU
- Choreographer for FPS measurement

Test examples: @specs/0-early-spec.md §12

## Environment

**Requirements:**
- Android Studio Arctic Fox or later
- Gradle 8.2+
- Kotlin 1.9.20+
- Physical device or emulator with API 29+
- Camera permission for testing

**Key Dependencies:**
- Jetpack Compose (BOM with Material 3)
- Koin (+ koin-androidx-compose)
- Camera2 API
- MediaCodec
- ML Kit Vision
- libyuv (native via JNI/CMake)

Full tech stack: @specs/0-early-spec.md §3

## Boundaries (Always/Ask/Never)

### ✅ Always
- Use Coroutines for async/blocking work
- Use Koin for dependency injection
- Use Jetpack Compose (no XML layouts)
- Close camera resources properly
- Save via MediaStore with correct MIME types
- Run tests before marking tasks complete

### ⚠️ Ask First
- Changing min/target SDK levels
- Adding new large dependencies
- Modifying fundamental aesthetic (LUT, tone curves)
- Adding network features (analytics, cloud sync)
- Database schema changes
- CI/CD configuration changes

### 🚫 Never
- Use deprecated `android.hardware.Camera`
- Block UI thread for processing/encoding
- Store outside scoped storage/MediaStore
- Bypass runtime permission checks
- Reference Context directly from Composables
- Commit hardcoded secrets/API keys
- Remove failing tests without documentation

Full boundaries: @specs/0-early-spec.md §8

## Special Notes

- **Known pitfalls:** Review §14 of spec before implementing Camera2, HEIC, or OpenGL features
- **Atomic tasks:** Use §16 task breakdown with dependencies and acceptance criteria
- **Self-verification:** Complete §17 checklist before marking any task as done
- **Performance targets:** All metrics in §13 must be met for MVP acceptance
- **Non-destructive:** Do not modify files outside workspace root without explicit approval
- **Ask when uncertain:** Better to clarify than assume and implement incorrectly

## Quick Start for Common Tasks

**Creating a new Composable:**
→ Load: @ai-rules/compose.md, @ai-rules/general.md

**Implementing camera feature:**
→ Load: @ai-rules/camera.md, @ai-rules/general.md
→ Reference: @specs/0-early-spec.md §10.2, §14 (pitfalls)

**Adding tests:**
→ Load: @ai-rules/testing.md, @ai-rules/general.md
→ Reference: @specs/0-early-spec.md §12

**Performance optimization:**
→ Load: @ai-rules/performance.md, @ai-rules/general.md
→ Reference: @specs/0-early-spec.md §13

---

**Remember:** The specification (@specs/0-early-spec.md) is the source of truth. These rules guide *how* to implement what the spec defines. When in doubt, ask for clarification before coding.
