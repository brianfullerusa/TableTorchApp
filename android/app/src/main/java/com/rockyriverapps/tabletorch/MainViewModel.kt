package com.rockyriverapps.tabletorch

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rockyriverapps.tabletorch.data.AppSettings
import com.rockyriverapps.tabletorch.data.ColorPalette
import com.rockyriverapps.tabletorch.data.PreferencesManager
import com.rockyriverapps.tabletorch.models.ParticleShape
import com.rockyriverapps.tabletorch.sensors.TiltSensorManager
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import kotlin.math.PI
import kotlin.math.abs

/**
 * ViewModel for the main TableTorch application.
 * Manages app settings, brightness control, tilt sensor integration, and palette operations.
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

        /** Maximum number of custom palettes allowed */
        const val MAX_CUSTOM_PALETTES = 50
    }

    // ============================================================================
    // Settings State — exposes PreferencesManager's eagerly-started flow directly
    // ============================================================================

    val settings: StateFlow<AppSettings> = preferencesManager.settingsFlow

    // ============================================================================
    // Brightness State (Activity observes and applies to Window)
    // ============================================================================

    private val _currentBrightness = MutableStateFlow(AppSettings.DEFAULT.defaultBrightness)

    /**
     * StateFlow of current brightness value (0.0 - 1.0).
     * Activity should observe this and apply to Window.
     */
    val currentBrightness: StateFlow<Float> = _currentBrightness.asStateFlow()

    /** Mutex to serialize palette CRUD operations and prevent read-modify-write races */
    private val paletteMutex = Mutex()

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

        // Apply initial brightness once real persisted settings arrive
        viewModelScope.launch {
            val initialSettings = preferencesManager.settingsFlow.first()
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
    private fun updateBrightnessForTilt(tiltAngleRadians: Double) {
        // Linear interpolation from 100% at flat to 30% at vertical
        val normalizedTilt = (tiltAngleRadians / (PI / 2)).coerceIn(0.0, 1.0)
        val brightness = (1.0 - TILT_BRIGHTNESS_RANGE * normalizedTilt).toFloat()
        val clampedBrightness = brightness.coerceIn(TILT_MIN_BRIGHTNESS, 1f)

        // Only update if change is significant (prevents feedback loops and flicker)
        if (abs(_currentBrightness.value - clampedBrightness) > BRIGHTNESS_CHANGE_THRESHOLD) {
            _currentBrightness.value = clampedBrightness
        }
    }

    // ============================================================================
    // Tilt Sensor Operations (encapsulated)
    // ============================================================================

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
            paletteMutex.withLock {
                preferencesManager.updateColor(index, colorValue)

                // Sync change back to the active custom palette so it stays up to date
                val snapshot = settings.value
                val activePalette = snapshot.customPalettes.find {
                    it.id == snapshot.activePaletteId
                }
                if (activePalette != null) {
                    val updatedColors = snapshot.selectedColors.toMutableList()
                    updatedColors[index] = colorValue
                    val updatedPalette = activePalette.copy(
                        colors = updatedColors.toImmutableList()
                    )
                    val updatedPalettes = snapshot.customPalettes.map {
                        if (it.id == activePalette.id) updatedPalette else it
                    }
                    preferencesManager.saveCustomPalettes(updatedPalettes)
                }
            }
        }
    }

    fun restoreDefaultColors() {
        viewModelScope.launch {
            paletteMutex.withLock {
                preferencesManager.restoreDefaultColors()
                preferencesManager.updateActivePaletteId(ColorPalette.PRESET_BRIGHT_ID)
            }
        }
    }

    fun updateShowQuickColorBar(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.updateShowQuickColorBar(enabled)
        }
    }

    fun updateAlwaysShowBrightness(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.updateAlwaysShowBrightness(enabled)
        }
    }

    // ============================================================================
    // Visual Effects Operations
    // ============================================================================

    fun updateEnableBreathingAnimation(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.updateEnableBreathingAnimation(enabled)
        }
    }

    fun updateBreathingDepth(depth: Float) {
        viewModelScope.launch {
            preferencesManager.updateBreathingDepth(depth)
        }
    }

    fun updateBreathingCycleDuration(duration: Float) {
        viewModelScope.launch {
            preferencesManager.updateBreathingCycleDuration(duration)
        }
    }

    fun updateEnableEmberParticles(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.updateEnableEmberParticles(enabled)
        }
    }

    fun updateParticleShape(shape: ParticleShape) {
        viewModelScope.launch {
            preferencesManager.updateParticleShape(shape)
        }
    }

    // ============================================================================
    // Palette Operations (serialized with Mutex to prevent read-modify-write races)
    // ============================================================================

    fun switchPalette(paletteId: String) {
        viewModelScope.launch {
            paletteMutex.withLock {
                val allPalettes = settings.value.getAllPalettes()
                val palette = allPalettes.find { it.id == paletteId } ?: return@withLock
                preferencesManager.switchPalette(paletteId, palette.colors)
            }
        }
    }

    fun createCustomPalette(name: String, colors: List<Long>) {
        viewModelScope.launch {
            paletteMutex.withLock {
                if (settings.value.customPalettes.size >= MAX_CUSTOM_PALETTES) {
                    return@withLock
                }
                val newPalette = ColorPalette(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    colors = colors.take(ColorPalette.PALETTE_SIZE).toImmutableList(),
                    isBuiltIn = false
                )
                val updatedPalettes = settings.value.customPalettes + newPalette
                preferencesManager.saveCustomPalettes(updatedPalettes)
                preferencesManager.switchPalette(newPalette.id, newPalette.colors)
            }
        }
    }

    fun duplicatePalette(palette: ColorPalette) {
        viewModelScope.launch {
            paletteMutex.withLock {
                if (settings.value.customPalettes.size >= MAX_CUSTOM_PALETTES) {
                    return@withLock
                }
                val duplicated = ColorPalette(
                    id = UUID.randomUUID().toString(),
                    name = "${palette.name} Copy",
                    colors = palette.colors,
                    isBuiltIn = false
                )
                val updatedPalettes = settings.value.customPalettes + duplicated
                preferencesManager.saveCustomPalettes(updatedPalettes)
                preferencesManager.switchPalette(duplicated.id, duplicated.colors)
            }
        }
    }

    fun renamePalette(paletteId: String, newName: String) {
        viewModelScope.launch {
            paletteMutex.withLock {
                val updatedPalettes = settings.value.customPalettes.map { palette ->
                    if (palette.id == paletteId) palette.copy(name = newName) else palette
                }
                preferencesManager.saveCustomPalettes(updatedPalettes)
            }
        }
    }

    fun deletePalette(paletteId: String) {
        viewModelScope.launch {
            paletteMutex.withLock {
                val snapshot = settings.value
                val updatedPalettes = snapshot.customPalettes.filter { it.id != paletteId }
                preferencesManager.saveCustomPalettes(updatedPalettes)

                // If we deleted the active palette, switch to Bright
                if (snapshot.activePaletteId == paletteId) {
                    preferencesManager.switchPalette(ColorPalette.Bright.id, ColorPalette.Bright.colors)
                }
            }
        }
    }

    // ============================================================================
    // Cleanup
    // ============================================================================

    override fun onCleared() {
        super.onCleared()
        tiltSensorManager.stopListening()
    }

    /**
     * Factory for creating MainViewModel with dependencies.
     */
    class Factory(
        private val preferencesManager: PreferencesManager,
        private val appContext: Context
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                // TiltSensorManager is created here so it is only instantiated once,
                // when the ViewModel is first created — not on every Activity recreation.
                val tiltSensorManager = TiltSensorManager(appContext)
                return MainViewModel(preferencesManager, tiltSensorManager) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
