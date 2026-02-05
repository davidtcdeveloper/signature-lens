# Rule Loading Guide for SignatureLens

## Purpose

This file tells you (the LLM) which rule files to load based on the current task context. Load rules progressively—only what's directly relevant—to avoid context pollution.

---

## Core Rules (Load Strategy)

1. **ALWAYS load `general.md` first** - Foundation for all Android/Kotlin work
2. **Load domain-specific rules** based on the task (see triggers below)
3. **Load supporting rules** as needed (e.g., testing when implementing features)
4. **Keep loaded rules minimal** - Only what's directly relevant to current work
5. **Refresh rules** when switching between major tasks or contexts

---

## Rule Loading Triggers

### 📋 general.md - Core Android/Kotlin Engineering
**Load when:**
- Always (foundation for all tasks)
- Starting any new feature or module
- Making architectural decisions
- Code review or refactoring

**Keywords:** architecture, SOLID, clean code, Kotlin, Android, best practices, design patterns

**Priority:** ALWAYS LOAD FIRST

---

### 📸 camera.md - Camera2 API & Preview Pipeline
**Load when:**
- Implementing camera preview or capture
- Working with Camera2 API, ImageReader, CaptureSession
- Debugging camera issues or performance
- Implementing exposure, focus, or capture controls

**Keywords:** Camera2, preview, capture, ImageReader, CameraDevice, CaptureSession, YUV, frames

**Companion rules:** general.md, performance.md (if optimizing)

**Spec reference:** §10.2 Preview Pipeline, §10.3 Capture Pipeline, §14 Camera2 Pitfalls

---

### 🎨 compose.md - Jetpack Compose UI
**Load when:**
- Creating new Composables or screens
- Building UI components or layouts
- Implementing state management in UI
- Working with Compose animations or gestures
- Integrating AndroidView (TextureView for camera)

**Keywords:** Composable, @Composable, remember, State, ViewModel, UI, layout, Material3

**Companion rules:** general.md, viewmodel.md

**Spec reference:** §11 UI/Compose, §6.2 Compose Style

---

### 🧠 viewmodel.md - ViewModel & State Management
**Load when:**
- Creating or modifying ViewModels
- Implementing UI state with StateFlow/Flow
- Handling user interactions and business logic
- Coordinating between UI and repositories

**Keywords:** ViewModel, StateFlow, Flow, MutableStateFlow, viewModelScope, UI state

**Companion rules:** general.md, compose.md, koin.md

**Spec reference:** §10.1 ViewModel + DI

---

### 💉 koin.md - Dependency Injection
**Load when:**
- Setting up DI modules
- Injecting dependencies into ViewModels or repositories
- Creating test modules with fakes
- Troubleshooting dependency resolution

**Keywords:** Koin, module, single, viewModel, inject, dependency injection, DI

**Companion rules:** general.md, testing.md (for test modules)

**Spec reference:** §10.1 Koin modules, §12.3 Koin in Tests

---

### 🎮 opengl.md - OpenGL ES Shaders & Graphics
**Load when:**
- Implementing fragment shaders or rendering pipeline
- Working with 3D LUT, tone mapping, vignette, grain
- Optimizing GPU performance
- Debugging shader compilation or rendering issues

**Keywords:** OpenGL, GLES, shader, fragment, texture, LUT, GPU, rendering

**Companion rules:** general.md, performance.md

**Spec reference:** §10.2 Preview Pipeline, §14 OpenGL Pitfalls

---

### 📦 encoding.md - HEIC/JPEG Encoding & MediaStore
**Load when:**
- Implementing MediaCodec HEIC encoding
- Handling JPEG fallback
- Saving files to MediaStore
- Embedding EXIF metadata
- Troubleshooting codec or storage issues

**Keywords:** MediaCodec, HEIC, HEVC, JPEG, encode, MediaStore, EXIF, storage

**Companion rules:** general.md, camera.md

**Spec reference:** §10.3 Capture & Encoding, §14 HEIC Pitfalls

---

### 🧪 testing.md - Testing Strategy & Patterns
**Load when:**
- Writing unit tests
- Writing instrumented/integration tests
- Setting up Compose UI tests
- Creating fake/mock dependencies
- Debugging test failures

**Keywords:** test, @Test, JUnit, AndroidX Test, Compose UI Test, instrumentation, assertion

**Companion rules:** general.md, koin.md (for test modules)

**Spec reference:** §12 Testing Strategy

---

### ⚡ performance.md - Performance Optimization
**Load when:**
- Profiling or optimizing performance
- Meeting FPS/latency targets
- Reducing memory usage
- Optimizing GPU/CPU work
- Debugging stuttering or lag

**Keywords:** performance, FPS, latency, profiler, memory, optimization, systrace

**Companion rules:** camera.md, opengl.md (for specific domains)

**Spec reference:** §13 Performance Targets

---

### 📝 commits.md - Git Workflow & Commit Standards
**Load when:**
- Making commits
- Creating pull requests
- Reviewing commit history
- Setting up git hooks

**Keywords:** commit, git, PR, pull request, branch, version control

**Companion rules:** general.md

**Spec reference:** §7 Git Workflow

---

## Quick Reference Scenarios

### 🚀 Starting a new feature
```
Load: general.md, compose.md, viewmodel.md, koin.md
Reference: @specs/0-early-spec.md §16 (find relevant atomic task)
```

### 📸 Implementing camera preview
```
Load: general.md, camera.md, opengl.md, compose.md
Reference: @specs/0-early-spec.md §10.2, §14 (Camera2 pitfalls)
```

### 💾 Implementing HEIC capture & save
```
Load: general.md, camera.md, encoding.md
Reference: @specs/0-early-spec.md §10.3, §14 (HEIC pitfalls)
```

### 🧪 Writing tests for a feature
```
Load: general.md, testing.md, koin.md
Reference: @specs/0-early-spec.md §12
```

### 🎨 Building a new UI screen
```
Load: general.md, compose.md, viewmodel.md
Reference: @specs/0-early-spec.md §11
```

### ⚡ Optimizing performance
```
Load: general.md, performance.md, [domain-specific: camera/opengl]
Reference: @specs/0-early-spec.md §13
```

### 🔍 Code review
```
Load: general.md, commits.md
Reference: @specs/0-early-spec.md §8 (Boundaries)
```

### 🐛 Debugging an issue
```
Load: general.md, [domain-specific rule based on issue area]
Reference: @specs/0-early-spec.md §14 (Known Pitfalls)
```

---

## Loading Pattern Example

**User Request:** "Implement exposure compensation control in the camera preview"

**Your thought process:**
1. This involves camera controls → load `camera.md`
2. UI control in Compose → load `compose.md`
3. State management → load `viewmodel.md`
4. Foundation → load `general.md` (always)

**Rules to load:**
- ✅ general.md (always)
- ✅ camera.md (Camera2 API)
- ✅ compose.md (UI component)
- ✅ viewmodel.md (state management)

**Spec sections to reference:**
- §2.1 (Exposure compensation in scope)
- §10.1 (ViewModel pattern)
- §11.2 (PreviewScreen controls)

---

## Rule File Manifest

| Rule File | Lines | Focus | Priority |
|-----------|-------|-------|----------|
| general.md | ~400 | Core Kotlin/Android patterns | ALWAYS |
| camera.md | ~350 | Camera2 API, capture pipeline | High |
| compose.md | ~300 | Jetpack Compose UI | High |
| viewmodel.md | ~250 | State management, ViewModels | High |
| koin.md | ~200 | Dependency injection | Medium |
| opengl.md | ~300 | Shaders, GPU rendering | Medium |
| encoding.md | ~250 | HEIC/JPEG, MediaStore | Medium |
| testing.md | ~350 | Testing patterns, assertions | Medium |
| performance.md | ~250 | Optimization, profiling | As needed |
| commits.md | ~150 | Git workflow | As needed |

---

## Progressive Disclosure in Action

**Don't do this (context pollution):**
```
❌ Load all 10 rule files for every task
❌ Dump entire spec into context window
❌ Re-load rules that aren't relevant to current work
```

**Do this (focused context):**
```
✅ Load 2-4 relevant rule files based on task
✅ Reference specific spec sections by number (§10.2)
✅ Refresh rules when switching task domains
```

---

## Self-Check Before Proceeding

Before you start coding, ask yourself:

1. ✓ Have I loaded general.md?
2. ✓ Which domain does this task fall into? (Camera? UI? Testing?)
3. ✓ Have I loaded the 1-3 domain-specific rules needed?
4. ✓ Do I know which spec sections to reference?
5. ✓ Am I clear on acceptance criteria for this task?

If you answered NO to any question, load the appropriate rules before proceeding.

---

## Remember

- **Spec is truth:** @specs/0-early-spec.md defines *what* to build
- **Rules guide how:** These files define *how* to build it well
- **Context is precious:** Load only what you need, when you need it
- **Ask first:** Clarify ambiguities before coding (per general.md)
