package com.rockyriverapps.tabletorch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rockyriverapps.tabletorch.data.AppSettings
import com.rockyriverapps.tabletorch.data.PreferencesManager
import com.rockyriverapps.tabletorch.sensors.TiltSensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs

/**
 * ViewModel for the main TableTorch application.
 * Manages app settings, brightness control, and tilt sensor integration.
 *
 * This ViewModel does NOT hold references to Window or Activity to prevent leaks.
 * It exposes brightness values that the Activity observes and applies to the Window.
 */
class MainViewModel(
    private val preferencesManager: PreferencesManager,
    private val tiltSensorManager: TiltSensorManager
) : ViewModel() {

    companion object {
        /** Minimum brightness to prevent completely black screen */
        private const val MIN_BRIGHTNESS = 0.01f

        /** Minimum brightness when tilt control is active (30%) */
        private const val TILT_MIN_BRIGHTNESS = 0.30f

        /** Maximum brightness change from tilt (100% - 30% = 70%) */
        private const val TILT_BRIGHTNESS_RANGE = 0.7

        /** Threshold for brightness change to prevent flicker (1% change) */
        private const val BRIGHTNESS_CHANGE_THRESHOLD = 0.01f

        /** Default brightness value */
        private const val DEFAULT_BRIGHTNESS = 0.85f
    }

    // ============================================================================
    // Settings State (MUST be declared before init block)
    // ============================================================================

    /**
     * StateFlow of current app settings.
     * Uses WhileSubscribed to stop collecting when no observers (e.g., app backgrounded).
     */
    val settings: StateFlow<AppSettings> = preferencesManager.settingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppSettings.DEFAULT
        )

    // ============================================================================
    // Brightness State (Activity observes and applies to Window)
    // ============================================================================

    private val _currentBrightness = MutableStateFlow(DEFAULT_BRIGHTNESS)

    /**
     * StateFlow of current brightness value (0.0 - 1.0).
     * Activity should observe this and apply to Window.
     */
    val currentBrightness: StateFlow<Float> = _currentBrightness.asStateFlow()

    init {
        // Observe settings changes to control sensor lifecycle
        viewModelScope.launch {
            settings
                .map { it.isAngleBasedBrightnessActive }
                .distinctUntilChanged()
                .collect { isActive ->
                    if (isActive) {
                        tiltSensorManager.startListening()
                    } else {
                        tiltSensorManager.stopListening()
                    }
                }
        }

        // Collect tilt angle and update brightness when tilt control is active
        viewModelScope.launch {
            tiltSensorManager.tiltAngle.collectLatest { angle ->
                if (settings.value.isAngleBasedBrightnessActive) {
                    updateBrightnessForTilt(angle)
                }
            }
        }

        // Apply initial brightness based on settings
        viewModelScope.launch {
            val initialSettings = settings.value
            if (initialSettings.useDefaultBrightnessOnLaunch && !initialSettings.isAngleBasedBrightnessActive) {
                _currentBrightness.value = initialSettings.defaultBrightness.coerceIn(MIN_BRIGHTNESS, 1f)
            }
        }
    }

    // ============================================================================
    // Brightness Operations
    // ============================================================================

    /**
     * Set the screen brightness.
     * Updates the brightness StateFlow which Activity observes and applies.
     * @param brightness Value between 0.0 (dim) and 1.0 (bright)
     */
    fun setBrightness(brightness: Float) {
        _currentBrightness.value = brightness.coerceIn(MIN_BRIGHTNESS, 1f)
    }

    /**
     * Calculate and apply brightness based on device tilt angle.
     * Matches iOS behavior: flat (0) = 100%, vertical (PI/2) = 30%
     * @param tiltAngleRadians Angle in radians (0 = flat, PI/2 = vertical)
     */
    fun updateBrightnessForTilt(tiltAngleRadians: Double) {
        // Linear interpolation from 100% at flat to 30% at vertical
        val normalizedTilt = (tiltAngleRadians / (PI / 2)).coerceIn(0.0, 1.0)
        val brightness = (1.0 - TILT_BRIGHTNESS_RANGE * normalizedTilt).toFloat()
        val clampedBrightness = brightness.coerceIn(TILT_MIN_BRIGHTNESS, 1f)

        // Only update if change is significant (prevents feedback loops and flicker)
        if (abs(_currentBrightness.value - clampedBrightness) > BRIGHTNESS_CHANGE_THRESHOLD) {
            _currentBrightness.value = clampedBrightness
        }
    }

    /**
     * Apply initial brightness based on settings.
     * Called when Activity starts.
     */
    fun applyInitialBrightness() {
        val currentSettings = settings.value
        if (currentSettings.useDefaultBrightnessOnLaunch && !currentSettings.isAngleBasedBrightnessActive) {
            _currentBrightness.value = currentSettings.defaultBrightness.coerceIn(MIN_BRIGHTNESS, 1f)
        }
    }

    // ============================================================================
    // Tilt Sensor Operations (encapsulated)
    // ============================================================================

    /**
     * Check if tilt sensor is available on this device.
     */
    fun isTiltSensorAvailable(): Boolean = tiltSensorManager.isAvailable()

    /**
     * Start or stop the tilt sensor based on whether angle-based brightness is active.
     * Only starts the sensor if the feature is enabled in settings.
     * @param forceStart If true, start regardless of settings (for resume scenarios)
     */
    fun updateTiltSensorState(forceStart: Boolean = false) {
        if (forceStart && settings.value.isAngleBasedBrightnessActive) {
            tiltSensorManager.startListening()
        } else if (!settings.value.isAngleBasedBrightnessActive) {
            tiltSensorManager.stopListening()
        }
    }

    /**
     * Start the tilt sensor if the feature is enabled.
     * Should be called from Activity onResume.
     */
    fun startTiltSensorIfEnabled() {
        if (settings.value.isAngleBasedBrightnessActive) {
            tiltSensorManager.startListening()
        }
    }

    /**
     * Stop the tilt sensor.
     * Should be called from Activity onPause.
     */
    fun stopTiltSensor() {
        tiltSensorManager.stopListening()
    }

    // ============================================================================
    // Settings Operations
    // ============================================================================

    fun updateLastSelectedColorIndex(index: Int) {
        viewModelScope.launch {
            preferencesManager.updateLastSelectedColorIndex(index)
        }
    }

    fun updateDefaultBrightness(brightness: Float) {
        viewModelScope.launch {
            preferencesManager.updateDefaultBrightness(brightness)
        }
    }

    fun updateUseDefaultBrightnessOnLaunch(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.updateUseDefaultBrightnessOnLaunch(enabled)
        }
    }

    fun updatePreventScreenLock(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.updatePreventScreenLock(enabled)
        }
    }

    fun updateAngleBasedBrightness(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.updateAngleBasedBrightness(enabled)
        }
    }

    fun updateColor(index: Int, colorValue: Long) {
        viewModelScope.launch {
            preferencesManager.updateColor(index, colorValue)
        }
    }

    fun restoreDefaultColors() {
        viewModelScope.launch {
            preferencesManager.restoreDefaultColors()
        }
    }

    // ============================================================================
    // Cleanup
    // ============================================================================

    override fun onCleared() {
        super.onCleared()
        // Note: PreferencesManager is a singleton, so we don't close it here
        tiltSensorManager.stopListening()
    }

    /**
     * Factory for creating MainViewModel with dependencies.
     */
    class Factory(
        private val preferencesManager: PreferencesManager,
        private val tiltSensorManager: TiltSensorManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                return MainViewModel(preferencesManager, tiltSensorManager) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
