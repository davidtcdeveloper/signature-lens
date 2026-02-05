# SignatureLens

> A focused Android camera app with one iconic rangefinder-style aesthetic

SignatureLens is a constraint-driven camera application for Android that captures photos with a single, distinctive visual style inspired by classic precision rangefinder cameras. Users see the look applied in real-time preview and capture HEIC files that are already styled—no presets, no editing, no creative friction.

## Philosophy

**One look. One tap. Done.**

- Single iconic aesthetic applied to every capture
- Real-time WYSIWYG preview
- HEIC output for superior compression and quality
- Intelligent scene adaptation (portraits, landscapes, architecture)
- Built with Jetpack Compose, Camera2 API, and OpenGL ES

## Target Users

- Photography enthusiasts who appreciate rangefinder/film-like rendering
- Street and documentary photographers who value consistency
- Content creators seeking a cohesive visual identity
- Anyone who prefers creative constraints over infinite options

## Tech Stack

- **Platform:** Android 10+ (API 29+)
- **Language:** Kotlin
- **UI:** Jetpack Compose + Material 3
- **DI:** Koin
- **Camera:** Camera2 API
- **Processing:** OpenGL ES 3.1 (3D LUT color grading, tone mapping)
- **Encoding:** MediaCodec (HEIC/HEVC)
- **ML:** ML Kit Vision (face detection)
- **Async:** Kotlin Coroutines + Flow

## Status

🚧 **Pre-development / Specification Phase**

The project is currently in the planning stage with a comprehensive technical specification.

## Documentation

📄 **Full Specification:** [specs/0-early-spec.md](specs/0-early-spec.md)

The spec includes:
- Complete technical architecture
- Build commands and testing strategy
- Code style guide with examples
- Atomic task roadmap with dependencies
- Known pitfalls and domain expertise
- Performance targets and acceptance criteria

The specification is optimized for both AI agent development and human developers.

## Quick Reference

```bash
# Build (when implemented)
./gradlew assembleDebug

# Run tests
./gradlew test connectedAndroidTest

# Lint
./gradlew lint
```

## License

See [LICENSE](LICENSE) for details.

---

**Vision:** Turn your Android device into a rangefinder camera with one distinctive aesthetic—captured, styled, and saved as HEIC, ready to share in seconds.
