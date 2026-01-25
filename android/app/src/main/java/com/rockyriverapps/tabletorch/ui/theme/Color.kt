package com.rockyriverapps.tabletorch.ui.theme

import androidx.compose.ui.graphics.Color
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

// TableTorch Brand Colors
val TorchOrange = Color(0xFFFFA500)
val TorchOrangeContainer = Color(0xFF5C3D00)
val TorchSecondary = Color(0xFFFFD699)
val TorchSecondaryContainer = Color(0xFF4A3520)
val TorchError = Color(0xFFFFB4AB)
val TorchErrorContainer = Color(0xFF93000A)
val TorchOutline = Color(0xFF958F8A)
val TorchOutlineVariant = Color(0xFF4A4542)
val TorchBackground = Color(0xFF000000)
val TorchSurface = Color(0xFF1C1C1E)
val TorchOnSurface = Color(0xFFFFFFFF)

// Light theme colors
val TorchOrangeLight = Color(0xFFE68A00)
val TorchBackgroundLight = Color(0xFFFFFBFF)
val TorchSurfaceLight = Color(0xFFFFF8F5)
val TorchOnSurfaceLight = Color(0xFF1C1B1B)
val TorchSecondaryLight = Color(0xFF8B5A00)
val TorchSecondaryContainerLight = Color(0xFFFFDDB3)
val TorchErrorLight = Color(0xFFBA1A1A)
val TorchErrorContainerLight = Color(0xFFFFDAD6)
val TorchOutlineLight = Color(0xFF857370)
val TorchOutlineVariantLight = Color(0xFFD8C2BE)

/**
 * Extension function to determine the appropriate contrasting text color
 * based on the luminance of the background color.
 * Uses the relative luminance formula for perceived brightness.
 * Returns Black for light backgrounds and White for dark backgrounds.
 */
fun Color.contrastingTextColor(): Color {
    val luminance = (0.299f * red + 0.587f * green + 0.114f * blue)
    return if (luminance > 0.5f) Color.Black else Color.White
}

// Default Torch Colors (matching iOS)
object TorchColors {
    val White = Color(0xFFFFFFFF)
    val SoftWhite = Color(0xFFFFC896)      // RGB(255, 200, 150)
    val MintGreen = Color(0xFF98FF98)      // RGB(152, 255, 152)
    val SteelBlue = Color(0xFF4682B4)      // RGB(70, 130, 180)
    val Red = Color(0xFFFF0000)            // RGB(255, 0, 0)
    val DarkRed = Color(0xFF800000)        // RGB(128, 0, 0)

    /** Immutable list of default colors for Compose stability */
    val defaultColorsImmutable: ImmutableList<Long> = persistentListOf(
        White.value.toLong(),
        SoftWhite.value.toLong(),
        MintGreen.value.toLong(),
        SteelBlue.value.toLong(),
        Red.value.toLong(),
        DarkRed.value.toLong()
    )

    /** Regular list for backward compatibility */
    val defaultColors: List<Long> = defaultColorsImmutable
}
