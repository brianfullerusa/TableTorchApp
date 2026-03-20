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

// Default Torch Colors (matching iOS)
object TorchColors {
    /** Immutable list of default colors as ARGB Long values for Compose stability */
    val defaultColorsImmutable: ImmutableList<Long> = persistentListOf(
        0xFFFFFFFFL,   // White
        0xFFFFC896L,   // Soft White
        0xFF98FF98L,   // Mint Green
        0xFF4682B4L,   // Steel Blue
        0xFFFF0000L,   // Red
        0xFF800000L    // Dark Red
    )

    /** Regular list for backward compatibility */
    val defaultColors: List<Long> = defaultColorsImmutable
}

/** Convert a Long ARGB color value to a Compose Color. */
fun Long.toComposeColor(): Color = Color(this.toInt())
