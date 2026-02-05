# SignatureLens - Iconic Rangefinder Camera Android App - Product Specification

## 1. Core Purpose & Value Proposition

**SignatureLens** is a streamlined Android camera application that captures photos with an automatic, single iconic visual style inspired by the distinctive rendering of classic precision rangefinder cameras. Users open the app, point, shoot, and receive a final **HEIC** file that already embodies the aesthetic—no preset selection, no post-processing workflow, no adjustment sliders. The philosophy: **one distinctive look, applied intelligently to every capture**.

The app serves photographers who want to establish a consistent, recognizable visual identity across their work without creative friction or technical decisions at the moment of capture.

***

## 2. The Iconic "Rangefinder" Aesthetic

The app defines and implements a single unified style drawing from characteristics shared across classic precision rangefinder cameras and their associated color science:

#### Color Rendering
- **Signature warmth:** Deliberate magenta/yellow bias, especially visible in skin tones and neutral grays
- **Natural saturation:** Colors feel film-like—rich but not oversaturated; excellent color separation without cartoonish pop
- **Skin tone excellence:** Renders human faces with warmth and flattery regardless of ambient lighting conditions
- **Neutral whites preserved:** Materials and architecture maintain accurate, uncolored rendering

#### Tonal Character
- **Lifted blacks:** Shadows retain detail; images feel inviting rather than harsh
- **Protected highlights:** Bright areas roll off gently without clipping
- **Mid-tone emphasis:** Punch and dimensionality in the tonal midrange
- **Gentle contrast curve:** Not punchy or aggressive; refined and understated

#### Optical Feel
- **High sharpness perception:** Micro-contrast creates a sense of precision and clarity
- **Subtle vignette:** Optional very light edge darkening (not overdone) for focus and intimacy
- **Film grain texture:** Minimal, barely perceptible—present enough to evoke analog character but not distracting
- **Bokeh-friendly:** When background blur is present, it renders smoothly without artificial separation

***

## 3. Capture Workflow

### Pre-Capture (Preview Mode)
- User opens **SignatureLens**; camera viewfinder displays with the rangefinder style applied **in real-time** as a preview overlay.
- The preview shows exactly what the final saved **HEIC** file will look like.
- No adjustment UI visible; the frame is clean, minimal, photographic.
- **Exposure indicator** (optional, subtle): small status showing if scene is properly exposed; user adjusts framing/distance if needed.
- **Grid overlay** (optional toggle): standard rule-of-thirds grid for composition guidance.

### Capture Action
- User taps the large, prominent shutter button (center bottom or corner).
- Haptic feedback confirms capture.
- Optional brief shutter sound (can be muted in settings).

### Post-Capture
- Image is processed with the rangefinder style applied.
- Processing time: instantaneous to 2–3 seconds depending on device performance.
- **Result preview screen:** full-screen view of the captured image with the style applied.
  - Swipe left to retake.
  - Swipe right or tap save to store as **HEIC** to device gallery.
  - Tap share to post directly to social media or messaging apps (**HEIC** automatically converted to JPEG if platform doesn't support it).

***

## 4. File Format: HEIC (Primary Output)

**SignatureLens** saves all captured images as **HEIC** files by default:

### Benefits for SignatureLens
- **Superior compression:** 50% smaller file sizes than JPEG at equivalent quality, ideal for high-resolution smartphone cameras.
- **Modern standard:** Native support on Android 10+ (Samsung devices since Galaxy S10 era).
- **Lossless editing capability:** HEIC supports embedded metadata and adjustments, preserving quality for future edits.
- **Transparency support:** Useful for creative compositing (e.g., cutouts) if users edit images later.
- **Consistent with Apple ecosystem:** Seamless sharing with iOS users (who use HEIC natively).

### HEIC Implementation Details
- **Default output:** All captures saved as HEIC with the rangefinder style baked in.
- **Quality setting:** 90–95% compression quality (visually lossless).
- **Metadata:** Embedded EXIF data including capture date/time, device info, GPS coordinates (if enabled), SignatureLens version.
- **Fallback conversion:** When sharing to apps that don't support HEIC (rare on modern Android), automatic real-time conversion to JPEG.
- **User option** (Settings): Toggle to save as JPEG instead of HEIC (for maximum compatibility).

### File Naming Convention
```
SignatureLens_YYYYMMDD_HHMMSS.heic
```
Example: `SignatureLens_20260129_162432.heic`

### Storage Location
- **Default:** Device's standard Camera Roll / DCIM folder.
- **Optional:** Custom folder (`SignatureLens/` in DCIM) for easy organization.

***

## 5. Technical Behavior of the Style

The rangefinder style applies intelligently across different scenarios:

### Portrait/Face Detection
- Automatically detects human faces in frame.
- Boosts warmth specifically in skin tones (magenta/yellow bias).
- Slightly reduces overall contrast locally to skin regions for flattery.
- Preserves eye sharpness and detail.

### Landscape/Nature Scenes
- Enhances color saturation in foliage and sky while maintaining natural appearance.
- Lifts shadows to reveal detail in forests or shaded terrain.
- Applies the signature warmth uniformly across the scene.

### Architecture/Interiors
- Maintains accurate white balance for built environments.
- Emphasizes line definition through micro-contrast.
- Renders materials (stone, concrete, glass, wood) with authentic color.
- Applies warmth subtly to avoid color casts on neutral surfaces.

### Mixed Lighting (Indoor + Outdoor)
- Intelligently balances warm indoor tungsten light and cool outdoor daylight.
- Prevents color casts from strong single-source lighting.
- Preserves detail in both shadow and highlight regions.

***

## 6. User Interface

### Minimal, Photography-Focused Design

**Viewfinder Screen:**
```
┌─────────────────────────────────────┐
│ [Camera Preview with Style Applied] │
│                                     │
│                                     │
│                                     │
│              [Shutter Button]       │ ← Large, prominent
│                                     │
│ [Grid Toggle] [Exposure] [Ratio]    │ ← Swipe up to expand
└─────────────────────────────────────┘
```
- Full-screen camera feed with rangefinder style applied in real-time.
- **Shutter button:** Large, centered at bottom. Clear affordance.
- **Status bar** (minimal): optional exposure meter, battery, time of day.
- **Swipe gestures:**
  - Swipe up: access settings (exposure compensation, grid toggle, flash, self-timer, HEIC toggle).
  - Swipe down: access recent captures.
  - Swipe left: access front/rear camera toggle.

**Settings Menu** (Swipe Up from Viewfinder):
```
Exposure Compensation: ±3EV slider
Grid: on/off
Flash: auto/on/off
Self-Timer: off/3s/10s
Sound: shutter sound on/off
Image Ratio: 4:3 | 16:9 | 1:1 | Full
File Format: HEIC (default) | JPEG
Storage: Camera Roll | SignatureLens Folder
```
- Settings are hidden during capture to maintain focus.

**Capture Review Screen:**
```
┌─────────────────────────────────────┐
│ [Full-Screen Styled HEIC Preview]   │
│                                     │
│ [Swipe left: Retake]                │
│ [Tap Save: → Gallery] [Share]       │
└─────────────────────────────────────┘
```
- Full-screen image with rangefinder style applied.
- Bottom action buttons (or swipe):
  - Retake (swipe left or X button).
  - Save/Confirm (swipe right or checkmark → saves as HEIC).
  - Share (share icon → converts to JPEG if needed).

**Gallery/Recent Captures:**
- Swipe down from viewfinder to see thumbnail strip of last 5–10 **HEIC** images.
- Tap to view full-screen.
- Long-press to delete or share (auto-converts to JPEG for sharing).

***

## 7. Distinctive Behaviors

### Smart Exposure Handling
- Analyzes scene brightness and automatically adjusts exposure to optimize the rangefinder look.
- If backlit, intelligently lifts shadows to maintain lifted-blacks characteristic.
- If very bright (snow/beach), prevents blown highlights by protecting bright regions.
- User can override with exposure compensation slider if needed.

### HEIC-Specific Optimizations
- **Smaller files:** 12MP image ~1–2MB HEIC vs. 4–6MB JPEG.
- **Faster sharing:** Smaller files upload faster to social media.
- **Better quality:** Superior compression preserves rangefinder style details.
- **Future-proof:** HEIC is the modern standard for mobile photography.

### Consistency Across Conditions
- Photos taken in different lighting all carry the same visual signature.
- Enables cohesive photo series without post-processing.

***

## 8. What SignatureLens Does NOT Include

- **No preset switching:** One style, applied uniformly.
- **No adjustment sliders:** No color temperature, saturation, contrast knobs.
- **No filter library:** No alternative looks or "moods."
- **No post-processing editor:** HEIC files come out final.
- **No cropping tool:** Compose in-camera.
- **No AI "enhance" buttons:** The rangefinder style *is* the enhancement.

***

## 9. Image Quality & Processing Pipeline

```
Sensor Data (RAW Bayer) 
    ↓
Demosaic → RGB
    ↓
White Balance (Auto + Rangefinder Bias)
    ↓
Tonal Curve (Lifted Blacks, Protected Highlights)
    ↓
Color Grading (Warmth, Magenta Bias, Natural Saturation)
    ↓
Micro-Contrast / Clarity
    ↓
Film Grain Texture (Subtle)
    ↓
Vignette (Optional, Light)
    ↓
HEIC Encoding (90–95% Quality)
    ↓
Save to Gallery as SignatureLens_YYYYMMDD_HHMMSS.heic
```

**Processing speed:** 1–3 seconds depending on device CPU.
**Resolution:** Full camera sensor resolution (12–50MP depending on Samsung model).

***

## 10. App Icon & Visual Identity

**App Icon:**
```
┌─────────────┐
│  ┌───────┐  │
│  │ Preview │  │ ← Minimal rangefinder frame
│  └───────┘  │
│   Signature │
│     Lens    │
└─────────────┘
```
- Clean rangefinder viewfinder frame graphic.
- Warm gold accent color (signature rangefinder warmth).
- Modern sans-serif typography: "SignatureLens".

**Color Palette:**
- Neutral grays/blacks for UI.
- Warm gold (#D4A574) for primary actions.
- Minimal color use—let styled preview dominate.

**Splash Screen:**
- Brief animation of rangefinder shutter opening to reveal camera preview.
- App name + "Signature Style Applied".

***

## 11. User Personas

### The Rangefinder Enthusiast
- Loves the aesthetic but doesn't own the hardware.
- Wants consistency across Instagram/projects without decisions.

### The Minimalist Shooter
- Prefers constraints; one distinctive look > infinite options.
- Focuses on composition, not technical adjustments.

### The Documentary Photographer
- Appreciates candid, unobtrusive capture tradition.
- Wants timeless, analog-feel images in digital workflow.

### The Content Creator
- Needs signature visual style for cohesive social feed.
- Values instant, shareable **HEIC** results.

***

## 12. Key Differentiators

1. **SignatureLens = One Iconic Look** — No decisions, no presets, no friction.
2. **HEIC by Default** — Modern format, smaller files, better quality.
3. **Real-Time Styled Preview** — See exactly what you'll get.
4. **Intelligent Scene Adaptation** — Works across all conditions.
5. **Instant HEIC Export** — Share-ready files, no editing needed.
6. **Photography Philosophy** — Constraint as creative strength.

***

## 13. Launch Checklist

```
✅ Real-time rangefinder style preview
✅ HEIC encoding with proper metadata
✅ Intelligent scene detection (portrait/landscape/architecture)
✅ Minimal gesture-based UI
✅ Samsung Galaxy optimization (Expert RAW integration?)
✅ File naming convention
✅ Share conversion (HEIC → JPEG)
✅ App icon + branding
✅ Onboarding tutorial (first launch)
✅ Permissions (camera, storage, location)
```

**SignatureLens** turns your Samsung into a rangefinder camera with one distinctive aesthetic—captured, styled, and saved as **HEIC**, ready to share in seconds.