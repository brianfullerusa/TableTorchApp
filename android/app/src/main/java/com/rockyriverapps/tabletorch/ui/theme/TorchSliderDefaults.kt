package com.rockyriverapps.tabletorch.ui.theme

import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable

/**
 * Default slider colors for TableTorch app.
 * Provides consistent styling across all sliders in the application.
 */
object TorchSliderDefaults {
    @Composable
    fun colors() = SliderDefaults.colors(
        thumbColor = TorchOrange,
        activeTrackColor = TorchOrange,
        inactiveTrackColor = TorchOrange.copy(alpha = 0.3f),
        disabledThumbColor = TorchOrange.copy(alpha = 0.55f),
        disabledActiveTrackColor = TorchOrange.copy(alpha = 0.55f),
        disabledInactiveTrackColor = TorchOrange.copy(alpha = 0.2f)
    )
}
