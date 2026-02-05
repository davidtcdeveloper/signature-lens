package com.signaturelens.camera

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import com.signaturelens.core.settings.FlashMode

/**
 * Manages camera settings (exposure, flash) configuration.
 * Abstracts Camera2 API specifics from the main repository.
 */
class CameraSettings(private val cameraManager: CameraManager, private val cameraId: String) {
    var exposureCompensation: Int = 0
        private set
    
    @Volatile
    var flashModeValue: Int = 0  // CaptureRequest.FLASH_MODE_OFF
        private set

    /**
     * Map UI exposure EV value (-3 to +3) to Camera2 exposure compensation.
     * Device may support different ranges; this respects device limits.
     */
    fun setExposureComp(evValue: Float) {
        val characteristics = cameraManager.getCameraCharacteristics(cameraId)
        val exposureRange = characteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE)
        
        if (exposureRange != null) {
            val stepObj = characteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_STEP)
            val step = stepObj?.toFloat() ?: 0.1f
            val clamped = evValue.coerceIn(exposureRange.lower.toFloat(), exposureRange.upper.toFloat())
            exposureCompensation = (clamped / step).toInt()
        }
    }

    fun setFlashMode(mode: FlashMode) {
        flashModeValue = when (mode) {
            FlashMode.OFF -> 0   // CaptureRequest.FLASH_MODE_OFF
            FlashMode.AUTO -> 2  // CaptureRequest.FLASH_MODE_AUTO
            FlashMode.ON -> 1    // CaptureRequest.FLASH_MODE_ON
        }
    }
}
