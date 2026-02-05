package com.signaturelens.core.settings

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class FlashMode {
    OFF, AUTO, ON
}

data class PreviewSettings(
    val exposureComp: Float = 0f,  // -3.0 to +3.0 EV
    val gridEnabled: Boolean = false,
    val flashMode: FlashMode = FlashMode.AUTO,
    val timerSeconds: Int = 0  // 0 = off, 3 or 10
)

/**
 * Repository for persisting user preferences across app sessions.
 * Uses SharedPreferences for simple key-value storage.
 */
class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences = 
        context.getSharedPreferences("signature_lens_settings", Context.MODE_PRIVATE)
    
    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<PreviewSettings> = _settings.asStateFlow()

    private fun loadSettings(): PreviewSettings {
        return PreviewSettings(
            exposureComp = prefs.getFloat(KEY_EXPOSURE_COMP, 0f),
            gridEnabled = prefs.getBoolean(KEY_GRID_ENABLED, false),
            flashMode = FlashMode.values()
                .getOrNull(prefs.getInt(KEY_FLASH_MODE, FlashMode.AUTO.ordinal))
                ?: FlashMode.AUTO,
            timerSeconds = prefs.getInt(KEY_TIMER_SECONDS, 0)
        )
    }

    fun updateExposureComp(value: Float) {
        val clamped = value.coerceIn(-3f, 3f)
        prefs.edit().putFloat(KEY_EXPOSURE_COMP, clamped).apply()
        updateState { copy(exposureComp = clamped) }
    }

    fun toggleGrid(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_GRID_ENABLED, enabled).apply()
        updateState { copy(gridEnabled = enabled) }
    }

    fun setFlashMode(mode: FlashMode) {
        prefs.edit().putInt(KEY_FLASH_MODE, mode.ordinal).apply()
        updateState { copy(flashMode = mode) }
    }

    fun setTimer(seconds: Int) {
        val validSeconds = when (seconds) {
            0, 3, 10 -> seconds
            else -> 0
        }
        prefs.edit().putInt(KEY_TIMER_SECONDS, validSeconds).apply()
        updateState { copy(timerSeconds = validSeconds) }
    }

    private fun updateState(update: PreviewSettings.() -> PreviewSettings) {
        _settings.value = _settings.value.update()
    }

    companion object {
        private const val KEY_EXPOSURE_COMP = "exposure_comp"
        private const val KEY_GRID_ENABLED = "grid_enabled"
        private const val KEY_FLASH_MODE = "flash_mode"
        private const val KEY_TIMER_SECONDS = "timer_seconds"
    }
}
