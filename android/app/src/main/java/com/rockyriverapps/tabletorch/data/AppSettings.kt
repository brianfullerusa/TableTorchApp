package com.rockyriverapps.tabletorch.data

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.rockyriverapps.tabletorch.models.ParticleShape
import com.rockyriverapps.tabletorch.ui.theme.TorchColors
import com.rockyriverapps.tabletorch.ui.theme.toComposeColor
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

/**
 * Data class representing all app settings.
 * Mirrors the iOS AppSettings for feature parity.
 *
 * Marked as @Immutable to help Compose optimize recomposition.
 * Uses ImmutableList for selectedColors and customPalettes to ensure stability.
 */
@Immutable
data class AppSettings(
    val defaultBrightness: Float = 0.85f,
    val useDefaultBrightnessOnLaunch: Boolean = true,
    val selectedColors: ImmutableList<Long> = TorchColors.defaultColorsImmutable,
    val isAngleBasedBrightnessActive: Boolean = false,
    val lastSelectedColorIndex: Int = 1, // Default to Soft White (index 1)
    val preventScreenLock: Boolean = true,
    val showQuickColorBar: Boolean = true,
    val alwaysShowBrightness: Boolean = false,
    val enableBreathingAnimation: Boolean = false,
    val breathingDepth: Float = 0.12f,
    val breathingCycleDuration: Float = 4f,
    val enableEmberParticles: Boolean = false,
    val particleShape: ParticleShape = ParticleShape.EMBERS,
    val activePaletteId: String = ColorPalette.DEFAULT_ACTIVE_PALETTE_ID,
    val customPalettes: ImmutableList<ColorPalette> = persistentListOf()
) {
    /**
     * Get the currently selected color as a Compose Color
     */
    fun getCurrentColor(): Color {
        val colorValue = selectedColors.getOrElse(lastSelectedColorIndex) {
            TorchColors.defaultColorsImmutable[1]
        }
        return colorValue.toComposeColor()
    }

    /**
     * Get a color at a specific index as a Compose Color
     */
    fun getColorAt(index: Int): Color {
        val colorValue = selectedColors.getOrElse(index) {
            TorchColors.defaultColorsImmutable.getOrElse(index) { TorchColors.defaultColorsImmutable[0] }
        }
        return colorValue.toComposeColor()
    }

    /**
     * Get all available palettes: built-in presets + user custom palettes.
     * Returns them in display order: built-ins first, then custom.
     */
    fun getAllPalettes(): ImmutableList<ColorPalette> {
        return (ColorPalette.builtInPresets + customPalettes).toImmutableList()
    }

    /**
     * Get the currently active palette, falling back to Bright if not found.
     */
    fun getActivePalette(): ColorPalette {
        return getAllPalettes().find { it.id == activePaletteId }
            ?: ColorPalette.Bright
    }

    companion object {
        val DEFAULT = AppSettings()
    }
}
