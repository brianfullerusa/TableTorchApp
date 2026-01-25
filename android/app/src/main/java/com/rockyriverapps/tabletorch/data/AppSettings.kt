package com.rockyriverapps.tabletorch.data

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.rockyriverapps.tabletorch.ui.theme.TorchColors
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

/**
 * Data class representing all app settings.
 * Mirrors the iOS AppSettings for feature parity.
 *
 * Marked as @Immutable to help Compose optimize recomposition.
 * Uses ImmutableList for selectedColors to ensure stability.
 */
@Immutable
data class AppSettings(
    val defaultBrightness: Float = 0.85f,
    val useDefaultBrightnessOnLaunch: Boolean = true,
    val selectedColors: ImmutableList<Long> = TorchColors.defaultColorsImmutable,
    val isAngleBasedBrightnessActive: Boolean = false,
    val lastSelectedColorIndex: Int = 1, // Default to Soft White (index 1)
    val preventScreenLock: Boolean = true
) {
    /**
     * Get the currently selected color as a Compose Color
     */
    fun getCurrentColor(): Color {
        val colorValue = selectedColors.getOrElse(lastSelectedColorIndex) {
            TorchColors.defaultColorsImmutable[1]
        }
        return Color(colorValue.toULong())
    }

    /**
     * Get a color at a specific index as a Compose Color
     */
    fun getColorAt(index: Int): Color {
        val colorValue = selectedColors.getOrElse(index) {
            TorchColors.defaultColorsImmutable.getOrElse(index) { TorchColors.defaultColorsImmutable[0] }
        }
        return Color(colorValue.toULong())
    }

    companion object {
        val DEFAULT = AppSettings()
    }
}
