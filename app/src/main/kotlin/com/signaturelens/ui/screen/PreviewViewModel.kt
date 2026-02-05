package com.signaturelens.ui.screen

import android.graphics.SurfaceTexture
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.signaturelens.camera.CameraRepository
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
    val lastCapturedFile: File? = null,
    val errorMessage: String? = null
)

/**
 * ViewModel for the camera preview screen.
 */
class PreviewViewModel(
    private val cameraRepository: CameraRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(PreviewUiState())
    val uiState: StateFlow<PreviewUiState> = _uiState
    
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
            try {
                val file = cameraRepository.captureStill()
                _uiState.update { 
                    it.copy(
                        lastCapturedFile = file,
                        errorMessage = null
                    ) 
                }
                Log.d(TAG, "Captured image: ${file.absolutePath}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to capture image", e)
                _uiState.update { it.copy(errorMessage = "Failed to capture: ${e.message}") }
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        cameraRepository.cleanup()
    }
    
    companion object {
        private const val TAG = "PreviewViewModel"
    }
}
