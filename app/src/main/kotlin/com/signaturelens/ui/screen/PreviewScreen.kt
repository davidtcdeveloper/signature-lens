package com.signaturelens.ui.screen

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.signaturelens.R
import com.signaturelens.ui.components.CameraPreviewSurface
import org.koin.androidx.compose.koinViewModel

@Composable
fun PreviewScreen(
    viewModel: PreviewViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // Camera permission launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.onPermissionGranted()
        } else {
            viewModel.onPermissionDenied()
        }
    }
    
    // Request permission on first composition
    LaunchedEffect(Unit) {
        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }
    
    // Stop preview when screen is disposed
    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopPreview()
        }
    }
    
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when {
            uiState.hasPermission -> {
                PermissionGrantedContent(
                    viewModel = viewModel,
                    uiState = uiState
                )
            }
            uiState.isPermissionDenied -> {
                PermissionDeniedContent(
                    onRetry = {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                )
            }
            else -> {
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
private fun PermissionGrantedContent(
    viewModel: PreviewViewModel,
    uiState: PreviewUiState
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Camera preview surface
        CameraPreviewSurface(
            modifier = Modifier.fillMaxSize(),
            onSurfaceReady = { surfaceTexture ->
                viewModel.startPreview(surfaceTexture)
            }
        )
        
        // Shutter button
        FloatingActionButton(
            onClick = { viewModel.capture() },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(32.dp),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = "Capture photo",
                modifier = Modifier.size(32.dp)
            )
        }
        
        // Error message
        uiState.errorMessage?.let { error ->
            Snackbar(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp)
            ) {
                Text(error)
            }
        }
        
        // Capture feedback
        uiState.lastCapturedFile?.let { file ->
            Snackbar(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp),
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text("Captured: ${file.name}")
            }
        }
    }
}

@Composable
private fun PermissionDeniedContent(onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.permission_denied),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.permission_camera_rationale),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(stringResource(R.string.grant_permission))
        }
    }
}
