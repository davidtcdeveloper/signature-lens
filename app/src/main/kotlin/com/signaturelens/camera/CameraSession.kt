package com.signaturelens.camera

import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.media.ImageReader
import android.view.Surface

import com.signaturelens.core.renderer.RenderThread

/**
 * Immutable holder for active camera resources.
 * All fields are non-null when session is active.
 */
internal data class CameraSession(
    val device: CameraDevice,
    val session: CameraCaptureSession,
    val previewSurface: Surface,
    val renderThread: RenderThread,
    val imageReader: ImageReader
) {
    fun close() {
        session.close()
        device.close()
        previewSurface.release()
        renderThread.stopRendering()
        imageReader.close()
    }
}

/**
 * Represents the camera lifecycle state.
 * Makes state transitions explicit and type-safe.
 */
internal sealed class CameraState {
    data object Idle : CameraState()
    data class Active(val session: CameraSession) : CameraState()
}
