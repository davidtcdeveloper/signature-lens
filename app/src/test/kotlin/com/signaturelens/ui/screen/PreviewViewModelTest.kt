package com.signaturelens.ui.screen

import android.graphics.SurfaceTexture
import com.signaturelens.camera.CameraRepository
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class PreviewViewModelTest {
    
    private val testDispatcher = StandardTestDispatcher()
    
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
    
    @Test
    fun `initial state has no permission and preview not running`() {
        val mockRepository = mockk<CameraRepository>(relaxed = true)
        val viewModel = PreviewViewModel(mockRepository)
        val state = viewModel.uiState.value
        
        assertFalse(state.hasPermission)
        assertFalse(state.isPermissionDenied)
        assertFalse(state.isPreviewRunning)
    }
    
    @Test
    fun `onPermissionGranted sets hasPermission to true`() {
        val mockRepository = mockk<CameraRepository>(relaxed = true)
        val viewModel = PreviewViewModel(mockRepository)
        
        viewModel.onPermissionGranted()
        
        val state = viewModel.uiState.value
        assertTrue(state.hasPermission)
        assertFalse(state.isPermissionDenied)
    }
    
    @Test
    fun `onPermissionDenied sets isPermissionDenied to true`() {
        val mockRepository = mockk<CameraRepository>(relaxed = true)
        val viewModel = PreviewViewModel(mockRepository)
        
        viewModel.onPermissionDenied()
        
        val state = viewModel.uiState.value
        assertFalse(state.hasPermission)
        assertTrue(state.isPermissionDenied)
    }
}
