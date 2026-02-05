package com.signaturelens.ui.screen

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/**
 * UI state for the preview screen.
 * Currently minimal for Phase 1.
 */
data class PreviewUiState(
    val hasPermission: Boolean = false,
    val isPermissionDenied: Boolean = false
)

/**
 * ViewModel for the camera preview screen.
 * Phase 1: Handles permission state only.
 * Phase 2: Will add camera preview logic.
 */
class PreviewViewModel : ViewModel() {
    
    private val _uiState = MutableStateFlow(PreviewUiState())
    val uiState: StateFlow<PreviewUiState> = _uiState
    
    fun onPermissionGranted() {
        _uiState.update { it.copy(hasPermission = true, isPermissionDenied = false) }
    }
    
    fun onPermissionDenied() {
        _uiState.update { it.copy(hasPermission = false, isPermissionDenied = true) }
    }
}
