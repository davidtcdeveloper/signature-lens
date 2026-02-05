package com.signaturelens.ui.screen

import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for PreviewViewModel.
 * Phase 1: Basic permission state tests.
 */
class PreviewViewModelTest {
    
    @Test
    fun `initial state has no permission`() {
        val viewModel = PreviewViewModel()
        val state = viewModel.uiState.value
        
        assertFalse(state.hasPermission)
        assertFalse(state.isPermissionDenied)
    }
    
    @Test
    fun `onPermissionGranted sets hasPermission to true`() = runTest {
        val viewModel = PreviewViewModel()
        
        viewModel.onPermissionGranted()
        
        val state = viewModel.uiState.value
        assertTrue(state.hasPermission)
        assertFalse(state.isPermissionDenied)
    }
    
    @Test
    fun `onPermissionDenied sets isPermissionDenied to true`() = runTest {
        val viewModel = PreviewViewModel()
        
        viewModel.onPermissionDenied()
        
        val state = viewModel.uiState.value
        assertFalse(state.hasPermission)
        assertTrue(state.isPermissionDenied)
    }
}
