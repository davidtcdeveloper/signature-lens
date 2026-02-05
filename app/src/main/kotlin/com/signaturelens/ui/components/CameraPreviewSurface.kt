package com.signaturelens.ui.components

import android.graphics.SurfaceTexture
import android.view.TextureView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun CameraPreviewSurface(
    modifier: Modifier = Modifier,
    onSurfaceReady: (SurfaceTexture) -> Unit,
) {
    val context = LocalContext.current

    val textureView = remember { TextureView(context) }

    DisposableEffect(textureView) {
        val listener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                onSurfaceReady(surface)
            }

            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
                // Handle size changes if needed
            }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                return true
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
                // Frame updates - no action needed
            }
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
