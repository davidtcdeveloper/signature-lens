<primary_directive>
You are implementing Jetpack Compose UI for a professional camera application.
Per spec §8: ALL UI is Compose. NO XML layouts.

ALWAYS reference spec §11 (UI/Compose) and §6.2 (Compose Style).
ALWAYS check spec §14 (Compose + Camera Integration Pitfalls).
</primary_directive>

<cognitive_anchors>
TRIGGERS: @Composable, Compose, UI, remember, State, LaunchedEffect,
          AndroidView, TextureView, Material3, ViewModel, collectAsState

SIGNAL: Compose UI work detected → Apply Compose patterns
</cognitive_anchors>

---

<rule_1 priority="HIGHEST">
**STATE HOISTING - STATELESS COMPOSABLES**

Composables should be stateless when possible. Hoist state to ViewModel.

```kotlin
// ✅ CORRECT - stateless, reusable
@Composable
fun ExposureSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = -3f..3f,
        modifier = modifier
    )
}

// ✅ CORRECT - state in ViewModel
@Composable
fun PreviewScreen(viewModel: PreviewViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    
    ExposureSlider(
        value = uiState.exposureComp,
        onValueChange = viewModel::setExposure
    )
}

// ❌ WRONG - internal mutable state
@Composable
fun ExposureSlider() {
    var value by remember { mutableStateOf(0f) }  // State trapped in composable
    Slider(value = value, onValueChange = { value = it })
}
```
</rule_1>

<rule_2 priority="HIGHEST">
**VIEWMODEL INTEGRATION - USE KOIN + collectAsState()**

Per spec §10.1 and §11.2: ViewModels expose StateFlow, Composables collect with collectAsState().

```kotlin
// ✅ CORRECT - Koin injection + StateFlow collection
@Composable
fun PreviewScreen(
    viewModel: PreviewViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Box(modifier = Modifier.fillMaxSize()) {
        Text("Preview running: ${uiState.isPreviewRunning}")
    }
}

// ViewModel exposes StateFlow
class PreviewViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(PreviewUiState())
    val uiState: StateFlow<PreviewUiState> = _uiState
}

// ❌ WRONG - direct ViewModel property access (not observable)
@Composable
fun PreviewScreen(viewModel: PreviewViewModel = koinViewModel()) {
    Text("Running: ${viewModel.isRunning}")  // Won't recompose on change!
}
```
</rule_2>

<rule_3 priority="HIGHEST">
**CAMERA INTEGRATION - remember + DisposableEffect**

Per spec §14 Pitfalls: Use remember + DisposableEffect to avoid recomposition flicker with AndroidView.

```kotlin
// ✅ CORRECT - stable TextureView across recompositions
@Composable
fun CameraPreviewSurface(
    onSurfaceReady: (SurfaceTexture) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val textureView = remember { TextureView(context) }
    
    DisposableEffect(textureView) {
        val listener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(surface: SurfaceTexture, w: Int, h: Int) {
                onSurfaceReady(surface)
            }
            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, w: Int, h: Int) {}
            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean = true
            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
        }
        
        textureView.surfaceTextureListener = listener
        
        onDispose {
            textureView.surfaceTextureListener = null
        }
    }
    
    AndroidView(
        factory = { textureView },
        modifier = modifier
    )
}

// ❌ WRONG - recreating TextureView on every recomposition
@Composable
fun CameraPreviewSurface() {
    AndroidView(factory = { TextureView(it) })  // Flickers on recompose!
}
```
</rule_3>

<rule_4 priority="HIGH">
**SIDE EFFECTS - USE LaunchedEffect FOR ONE-TIME ACTIONS**

Use LaunchedEffect for actions that should run once or when keys change.

```kotlin
// ✅ CORRECT - start preview once when surface ready
@Composable
fun PreviewScreen(viewModel: PreviewViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    var surfaceTexture by remember { mutableStateOf<SurfaceTexture?>(null) }
    
    LaunchedEffect(surfaceTexture) {
        surfaceTexture?.let { surface ->
            viewModel.startPreview(surface)
        }
    }
    
    CameraPreviewSurface(
        onSurfaceReady = { surfaceTexture = it }
    )
}

// ❌ WRONG - calling suspend function directly (will crash)
@Composable
fun PreviewScreen(viewModel: PreviewViewModel = koinViewModel()) {
    val surface = getSurface()
    viewModel.startPreview(surface)  // Crash! Can't call suspend from Composable
}
```
</rule_4>

<rule_5 priority="HIGH">
**PERFORMANCE - AVOID UNNECESSARY RECOMPOSITIONS**

Use remember, derivedStateOf, and stable keys to minimize recompositions.

```kotlin
// ✅ CORRECT - derived state computed only when dependency changes
@Composable
fun PreviewScreen(viewModel: PreviewViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    
    val exposureText = remember(uiState.exposureComp) {
        "${uiState.exposureComp} EV"
    }
    
    Text(exposureText)
}

// ✅ CORRECT - skip recomposition when content unchanged
@Composable
fun CaptureButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(onClick = onClick, modifier = modifier) {
        Text("Capture")
    }
}

// ❌ WRONG - recreating lambda on every recomposition
@Composable
fun CaptureControls(viewModel: PreviewViewModel = koinViewModel()) {
    Button(onClick = { viewModel.capture() }) {  // New lambda every time!
        Text("Capture")
    }
}

// ✅ BETTER - stable reference
@Composable
fun CaptureControls(viewModel: PreviewViewModel = koinViewModel()) {
    Button(onClick = viewModel::capture) {  // Stable method reference
        Text("Capture")
    }
}
```
</rule_5>

<rule_6 priority="MEDIUM">
**COMPOSABLE STRUCTURE - SINGLE RESPONSIBILITY**

Per spec §11.2: Break complex screens into focused, reusable composables.

```kotlin
// ✅ CORRECT - focused, testable components
@Composable
fun PreviewScreen(viewModel: PreviewViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    
    Box(modifier = Modifier.fillMaxSize()) {
        CameraPreviewSurface(
            onSurfaceReady = viewModel::startPreview,
            modifier = Modifier.fillMaxSize()
        )
        
        CaptureControls(
            exposure = uiState.exposureComp,
            onExposureChange = viewModel::setExposure,
            onShutterClick = viewModel::capture,
            modifier = Modifier.align(Alignment.BottomCenter)
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

@Composable
fun CaptureControls(
    exposure: Float,
    onExposureChange: (Float) -> Unit,
    onShutterClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        ExposureSlider(exposure, onExposureChange)
        ShutterButton(onClick = onShutterClick)
    }
}

// ❌ WRONG - God composable
@Composable
fun CameraScreen() {
    // 500 lines mixing preview, controls, overlay, navigation...
}
```
</rule_6>

---

<pattern name="screen_with_viewmodel">
**Complete Screen Pattern**

```kotlin
// Screen composable
@Composable
fun PreviewScreen(
    viewModel: PreviewViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    
    PreviewContent(
        uiState = uiState,
        onExposureChange = viewModel::setExposure,
        onShutterClick = viewModel::capture,
        onSurfaceReady = viewModel::startPreview
    )
    
    // Handle one-time effects
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            // Show snackbar or toast
        }
    }
}

// Stateless content composable (testable without ViewModel)
@Composable
private fun PreviewContent(
    uiState: PreviewUiState,
    onExposureChange: (Float) -> Unit,
    onShutterClick: () -> Unit,
    onSurfaceReady: (SurfaceTexture) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        CameraPreviewSurface(
            onSurfaceReady = onSurfaceReady,
            modifier = Modifier.fillMaxSize()
        )
        
        if (uiState.isPreviewRunning) {
            CaptureControls(
                exposure = uiState.exposureComp,
                onExposureChange = onExposureChange,
                onShutterClick = onShutterClick
            )
        }
    }
}
```
</pattern>

<pattern name="compose_testing">
**Compose UI Testing Pattern**

Per spec §12.2: Use createAndroidComposeRule for integration tests.

```kotlin
@get:Rule
val composeRule = createAndroidComposeRule<MainActivity>()

@Test
fun captureButton_triggersCapture() {
    // Arrange
    composeRule.setContent {
        CaptureButton(onClick = { /* captured */ })
    }
    
    // Act
    composeRule.onNodeWithText("Capture").performClick()
    
    // Assert
    composeRule.onNodeWithText("Processing...").assertExists()
}

@Test
fun exposureSlider_updatesState() {
    val viewModel = PreviewViewModel(fakeCameraRepo, fakeCaptureRepo)
    
    composeRule.setContent {
        ExposureSlider(
            value = viewModel.uiState.value.exposureComp,
            onValueChange = viewModel::setExposure
        )
    }
    
    composeRule.onNodeWithTag("ExposureSlider")
        .performTouchInput { swipeRight() }
    
    assertEquals(3.0f, viewModel.uiState.value.exposureComp, 0.1f)
}
```
</pattern>

---

<checklist>
Compose Implementation Checklist:

☐ Composables are stateless (state hoisted to ViewModel)
☐ ViewModel injected via koinViewModel()
☐ StateFlow collected via collectAsState()
☐ Side effects in LaunchedEffect (not in composable body)
☐ TextureView/AndroidView wrapped with remember + DisposableEffect
☐ No suspend functions called directly from composables
☐ Expensive calculations wrapped in remember/derivedStateOf
☐ Composables follow single-responsibility (< 100 lines)
☐ Test tags added for UI testing
☐ No direct Context access (use LocalContext.current)
☐ Modifiers passed as parameters (not hardcoded)
☐ Preview annotations for design-time rendering
</checklist>

<avoid>
❌ Calling suspend functions directly from composables
❌ Accessing ViewModel properties directly (use StateFlow)
❌ Creating new TextureView on every recomposition
❌ Mutable state inside composables (use hoisting)
❌ Recreating lambdas unnecessarily
❌ God composables (> 200 lines)
❌ Hardcoded modifiers (prevents reuse)
❌ Missing DisposableEffect for cleanup
❌ Using GlobalScope.launch (use rememberCoroutineScope)
❌ Not testing Compose UI flows
</avoid>

---

<pitfalls_from_spec>
**From spec §14 - Compose + Camera Integration:**

**Problem:** AndroidView with TextureView causes recomposition flicker

**Solution:** Use remember and DisposableEffect to maintain single TextureView instance
```kotlin
@Composable
fun CameraPreviewSurface() {
    val textureView = remember { TextureView(context) }
    DisposableEffect(textureView) {
        // Setup
        onDispose { /* cleanup */ }
    }
    AndroidView({ textureView })
}
```
</pitfalls_from_spec>

---

<material3_patterns>
**Material 3 Components (Per Spec)**

```kotlin
// Theme setup (spec §11)
@Composable
fun SignatureLensTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(),  // Minimal, photography-focused
        typography = Typography,
        content = content
    )
}

// Button style
@Composable
fun ShutterButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilledIconButton(
        onClick = onClick,
        modifier = modifier.size(72.dp),
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Icon(Icons.Default.Circle, "Capture")
    }
}
```
</material3_patterns>

---

## Quick Reference

**Basic screen setup:**
1. Inject ViewModel with koinViewModel()
2. Collect StateFlow with collectAsState()
3. Hoist state, pass callbacks
4. Break into focused composables

**Camera integration:**
1. remember { TextureView(context) }
2. DisposableEffect for lifecycle
3. AndroidView with factory
4. Callback to ViewModel for surface

**Side effects:**
- LaunchedEffect: Run once or on key change
- DisposableEffect: Setup/cleanup
- rememberCoroutineScope: Launch coroutines

**Testing:**
- Use createAndroidComposeRule
- Add testTag to composables
- performClick(), assertExists(), etc.

---

Remember: Per spec §8, ALL UI is Compose. No XML layouts. Keep composables small, stateless, and testable.
