# SignatureLens Spec 1 – Foundation & App Shell

## 1. Purpose

This spec defines the **first implementation iteration** for SignatureLens.
The outcome is a **running Android app shell** with:

- Correct Gradle + Kotlin + AGP setup.
- Koin-based dependency injection bootstrap.
- A minimal Jetpack Compose UI shell.
- Runtime permissions for camera (and optional location) wired in.

No camera, preview, encoding, or styling logic is implemented here.

Per the master spec `0-early-spec.md`, this iteration primarily covers:

- §3 Tech Stack
- §4 Commands & Build
- §5 Project Structure (initial scaffolding)
- §6 Code Style & Naming
- §7 Git Workflow (for how to commit this work)
- §9.1 User Flow (launch + permissions)
- §16 Phase 1 – Tasks 1.1–1.4

Subsequent specs assume everything here is complete and stable.

## 2. Dependencies

- Android toolchain installed (Android Studio, SDK, platform tools).
- No prior code in this repository is required, but if it already exists, the agent MUST:
  - Respect existing `minSdk`/`targetSdk` values if they already match §3.
  - Avoid destructive changes to non-app modules unless explicitly required by this spec.

## 3. In Scope

1. **Project initialization (Task 1.1)**  
   - Create an Android application module `app` if not present.  
   - Configure:
     - Kotlin 1.9.20+.
     - Gradle 8.2+ / AGP (latest compatible as of spec).  
   - Set `minSdk = 29`, `targetSdk = 35` as per §3.

2. **Koin DI bootstrap (Task 1.2, scoped to foundations)**  
   - Add Koin dependencies (including `koin-androidx-compose`).  
   - Create initial Koin modules in `com.signaturelens.di`, even if some bindings are placeholders:
     - `cameraModule` and `viewModelModule` stubs that compile.
   - Create an `Application` class (e.g., `SignatureLensApp`) that initializes Koin.

3. **MainActivity + Compose shell (Task 1.3)**  
   - Create `MainActivity` extending `ComponentActivity` under `com.signaturelens.ui`.  
   - Implement `setContent { SignatureLensApp() }` with:
     - `SignatureLensTheme`.
     - A minimal `PreviewScreen` placeholder that renders static UI only (e.g., app title).

4. **Runtime permissions (Task 1.4)**  
   - Implement camera permission flow per §9.1:
     - Request `CAMERA` permission on first launch (or when preview requires it).
     - Handle denial gracefully with informative UI and an option to retry.
   - Prepare optional location permission **hooks** (for EXIF GPS later) but DO NOT block core flow on location denial.

## 4. Out of Scope

- Camera2 API integration, preview surfaces, or capture.
- HEIC or JPEG encoding.
- OpenGL shaders and YUV/RGB conversion.
- Face detection and scene intelligence.
- Full settings storage (DataStore) beyond what is needed for foundation.
- Any CI/CD or workflow configuration changes (`.github/`, `gradle/` internals).

If the agent encounters existing camera/preview code, it should:

- Leave it untouched unless build fails due to clear misconfiguration conflicting with §3.
- Prefer adding new foundation code rather than refactoring future features in this iteration.

## 5. Implementation Guidelines

### 5.1 Project & Gradle Setup

- **Module structure** must follow §5:
  - `app/` main module with `src/main/kotlin/com/signaturelens/...`.
- Configure Gradle scripts using Kotlin DSL (`build.gradle.kts`) if starting from scratch.
- Ensure:
  - Java/Kotlin compatibility set to at least 1.8 (or modern default).
  - Compose is enabled (`buildFeatures { compose = true }`).
  - Correct Compose BOM and Material 3 dependencies, matching §3 and §11.

Typical doubts and their answers:

- **Q:** Can I change `minSdk` or `targetSdk`?  
  **A:** Only set them to `29`/`35` if not already configured. Do NOT lower `minSdk` or raise beyond `35` without explicit instruction.

- **Q:** Can I add arbitrary libraries (e.g., RxJava, Hilt)?  
  **A:** No. Only add dependencies required by §3 and this spec (Koin, Compose, testing libs, etc.).

### 5.2 Koin Application Setup

- Create `com.signaturelens.di` package and define:
  - `cameraModule` – initially may only register basic scaffolding classes (even empty implementations) so later specs can extend them.
  - `viewModelModule` – register at least one `PreviewViewModel` placeholder that compiles.
- Create an `Application` subclass `SignatureLensApp` in `com.signaturelens`:
  - Initialize Koin with:
    - `androidContext(this)`.
    - `modules(cameraModule, viewModelModule)`.
- Register the `Application` class in `AndroidManifest.xml`.

Common questions:

- **Q:** Should I define full repositories and viewmodels now?  
  **A:** No, just minimal placeholders/interfaces that compile and align with §10.1 naming.

### 5.3 Compose Shell & Navigation Surface

- Implement:
  - `SignatureLensTheme` in `ui/theme`.
  - `SignatureLensApp()` root composable that applies the theme and shows `PreviewScreen()`.
- `PreviewScreen` at this stage:
  - May show simple text or placeholder layout: e.g., app name and a stub shutter button.
  - Should be placed under `com.signaturelens.ui.screen`.
  - Should already take a `PreviewViewModel = koinViewModel()` parameter, even if the ViewModel is minimal.

Clarifications:

- **Q:** Should I integrate `CameraPreviewSurface` now?  
  **A:** No. Only add placeholders where needed; actual camera surface is Spec 2.

### 5.4 Runtime Permissions

- Use modern permission APIs (e.g., `rememberLauncherForActivityResult` + `ActivityResultContracts.RequestPermission`) in Compose.
- Requirements:
  - Request `CAMERA` permission before starting any preview work (to be implemented later).
  - Show clear UI text when permission is denied, guiding user to grant access.
  - Do NOT hard-crash or exit on denial.
  - Implement optional location permission handling **hooks** (used later for EXIF), but keep it non-blocking.

Clarifications:

- **Q:** Can I ask for location permission at launch?  
  **A:** It is allowed but MUST be optional and non-blocking for core camera usage. Keep UX simple and unobtrusive.

## 6. Acceptance Criteria

For this spec to be considered complete:

1. **Build & Run**
   - `./gradlew assembleDebug` succeeds.
   - `./gradlew test` and `./gradlew lint` succeed with no new critical issues.
   - App launches on a device/emulator and shows the Compose shell.

2. **DI & App Startup**
   - Koin initializes correctly at app startup (no DI-related crashes).
   - `PreviewViewModel` can be obtained via `koinViewModel()` in `PreviewScreen`.

3. **Permissions Flow**
   - On first launch (without CAMERA permission), app requests permission.
   - On denial, user sees clear explanation and a way to retry.
   - Location permission (if requested) does not break flow when denied.

4. **Code Quality**
   - Code follows §6 Code Style & Naming and `ai-rules/general.md`.
   - No usage of deprecated `android.hardware.Camera`.
   - No network or HEIC/MediaStore work is introduced yet.

## 7. Handover to Next Spec

Subsequent specs (starting with Spec 2 – Camera Preview) assume:

- The project builds and runs via the commands in §4.
- Koin DI, `MainActivity`, and the Compose shell are in place.
- A placeholder `PreviewViewModel` and `PreviewScreen` exist and can be extended.
- Permissions flow for CAMERA is reliably implemented and tested.

Agents implementing later specs MUST treat this spec as stable foundation and avoid reworking it unless changes are required to satisfy their own acceptance criteria **and** remain consistent with `0-early-spec.md`.

