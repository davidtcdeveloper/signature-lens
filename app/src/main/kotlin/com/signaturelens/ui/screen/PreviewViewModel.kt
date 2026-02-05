package com.signaturelens.ui.screen

import android.content.Intent
import android.graphics.SurfaceTexture
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.signaturelens.camera.CameraRepository
import com.signaturelens.core.domain.CaptureRepository
import com.signaturelens.core.settings.SettingsRepository
import com.signaturelens.core.settings.FlashMode
import com.signaturelens.core.settings.PreviewSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

/**
 * UI state for the preview screen.
 */
data class PreviewUiState(
    val hasPermission: Boolean = false,
    val isPermissionDenied: Boolean = false,
    val isPreviewRunning: Boolean = false,
    val lastCapturedUri: Uri? = null,
    val lastCaptureMimeType: String? = null,
    val errorMessage: String? = null,
    // Settings controls
    val exposureComp: Float = 0f,
    val gridEnabled: Boolean = false,
    val flashMode: FlashMode = FlashMode.AUTO,
    val timerSeconds: Int = 0
)

/**
 * ViewModel for the camera preview screen.
 */
class PreviewViewModel(
    private val cameraRepository: CameraRepository,
    private val captureRepository: CaptureRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(PreviewUiState())
    val uiState: StateFlow<PreviewUiState> = _uiState
    
    init {
        // Sync settings into UI state
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _uiState.update { 
                    it.copy(
                        exposureComp = settings.exposureComp,
                        gridEnabled = settings.gridEnabled,
                        flashMode = settings.flashMode,
                        timerSeconds = settings.timerSeconds
                    )
                }
            }
        }
    }
    
    fun onPermissionGranted() {
        _uiState.update { it.copy(hasPermission = true, isPermissionDenied = false) }
    }
    
    fun onPermissionDenied() {
        _uiState.update { it.copy(hasPermission = false, isPermissionDenied = true) }
    }
    
    fun startPreview(surfaceTexture: SurfaceTexture) {
        viewModelScope.launch {
            try {
                cameraRepository.startPreview(surfaceTexture)
                _uiState.update { it.copy(isPreviewRunning = true, errorMessage = null) }
                Log.d(TAG, "Preview started")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start preview", e)
                _uiState.update { it.copy(errorMessage = "Failed to start preview: ${e.message}") }
            }
        }
    }
    
    fun stopPreview() {
        viewModelScope.launch {
            try {
                cameraRepository.stopPreview()
                _uiState.update { it.copy(isPreviewRunning = false) }
                Log.d(TAG, "Preview stopped")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to stop preview", e)
            }
        }
    }
    
    fun capture() {
        viewModelScope.launch {
            // Using CaptureRepository for styled + HEIC/MediaStore save
            val result = captureRepository.captureStyledImage()
            result.fold(
                onSuccess = { uri ->
                    _uiState.update { 
                        it.copy(
                            lastCapturedUri = uri,
                            lastCaptureMimeType = "image/heic",  // Default; can be detected
                            errorMessage = null
                        ) 
                    }
                    Log.d(TAG, "Captured image: $uri")
                },
                onFailure = { e ->
                    Log.e(TAG, "Failed to capture image", e)
                    _uiState.update { it.copy(errorMessage = "Failed to capture: ${e.message}") }
                }
            )
        }
    }
    
    // Settings controls
    fun onExposureChange(value: Float) {
        settingsRepository.updateExposureComp(value)
        cameraRepository.setExposureCompensation(value)
    }
    
    fun toggleGrid(enabled: Boolean) {
        settingsRepository.toggleGrid(enabled)
    }
    
    fun setFlashMode(mode: FlashMode) {
        settingsRepository.setFlashMode(mode)
        cameraRepository.setFlashMode(mode)
    }
    
    fun setTimer(seconds: Int) {
        settingsRepository.setTimer(seconds)
    }
    
    fun clearLastCapture() {
        _uiState.update { it.copy(lastCapturedUri = null, lastCaptureMimeType = null) }
    }
    
    fun deleteLastCapture() {
        val uri = _uiState.value.lastCapturedUri ?: return
        viewModelScope.launch {
            try {
                captureRepository.deleteCapture(uri)
                clearLastCapture()
                Log.d(TAG, "Deleted capture: $uri")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete capture", e)
                _uiState.update { it.copy(errorMessage = "Failed to delete: ${e.message}") }
            }
        }
    }

    fun shareLastCapture(context: android.content.Context) {
        val uri = _uiState.value.lastCapturedUri ?: return
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share image"))
    }
    
    override fun onCleared() {
        super.onCleared()
        cameraRepository.cleanup()
    }
    
    companion object {
        private const val TAG = "PreviewViewModel"
    }
}
