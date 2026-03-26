package com.rockyriverapps.tabletorch.data

import com.rockyriverapps.tabletorch.models.ParticleShape
import kotlinx.coroutines.flow.StateFlow

/**
 * Repository interface for app preferences.
 * Abstracts the persistence layer to enable test doubles in unit tests.
 */
interface PreferencesRepository {
    val settingsFlow: StateFlow<AppSettings>

    suspend fun updateDefaultBrightness(brightness: Float)
    suspend fun updateUseDefaultBrightnessOnLaunch(enabled: Boolean)
    suspend fun updateAngleBasedBrightness(enabled: Boolean)
    suspend fun updateLastSelectedColorIndex(index: Int)
    suspend fun updatePreventScreenLock(enabled: Boolean)
    suspend fun updateColor(index: Int, colorValue: Long)
    suspend fun restoreDefaultColors()
    suspend fun updateShowQuickColorBar(enabled: Boolean)
    suspend fun updateAlwaysShowBrightness(enabled: Boolean)
    suspend fun updateEnableBreathingAnimation(enabled: Boolean)
    suspend fun updateBreathingDepth(depth: Float)
    suspend fun updateBreathingCycleDuration(duration: Float)
    suspend fun updateEnableEmberParticles(enabled: Boolean)
    suspend fun updateParticleShape(shape: ParticleShape)
    suspend fun switchPalette(paletteId: String, paletteColors: List<Long>)
    suspend fun saveCustomPalettes(palettes: List<ColorPalette>)
    suspend fun updateActivePaletteId(paletteId: String)
}
