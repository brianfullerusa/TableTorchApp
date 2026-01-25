package com.rockyriverapps.tabletorch.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.rockyriverapps.tabletorch.util.findActivity

private val DarkTableTorchColorScheme = darkColorScheme(
    primary = TorchOrange,
    onPrimary = Color.Black,
    primaryContainer = TorchOrangeContainer,
    onPrimaryContainer = TorchOrange,
    secondary = TorchSecondary,
    onSecondary = Color.Black,
    secondaryContainer = TorchSecondaryContainer,
    onSecondaryContainer = TorchSecondary,
    tertiary = TorchSecondary,
    onTertiary = Color.Black,
    tertiaryContainer = TorchSecondaryContainer,
    onTertiaryContainer = TorchSecondary,
    error = TorchError,
    onError = Color.Black,
    errorContainer = TorchErrorContainer,
    onErrorContainer = TorchError,
    background = TorchBackground,
    onBackground = TorchOnSurface,
    surface = TorchSurface,
    onSurface = TorchOnSurface,
    surfaceVariant = TorchSurface,
    onSurfaceVariant = TorchOnSurface,
    outline = TorchOutline,
    outlineVariant = TorchOutlineVariant
)

private val LightTableTorchColorScheme = lightColorScheme(
    primary = TorchOrangeLight,
    onPrimary = Color.White,
    primaryContainer = TorchSecondaryContainerLight,
    onPrimaryContainer = TorchOrangeLight,
    secondary = TorchSecondaryLight,
    onSecondary = Color.White,
    secondaryContainer = TorchSecondaryContainerLight,
    onSecondaryContainer = TorchSecondaryLight,
    tertiary = TorchSecondaryLight,
    onTertiary = Color.White,
    tertiaryContainer = TorchSecondaryContainerLight,
    onTertiaryContainer = TorchSecondaryLight,
    error = TorchErrorLight,
    onError = Color.White,
    errorContainer = TorchErrorContainerLight,
    onErrorContainer = TorchErrorLight,
    background = TorchBackgroundLight,
    onBackground = TorchOnSurfaceLight,
    surface = TorchSurfaceLight,
    onSurface = TorchOnSurfaceLight,
    surfaceVariant = TorchSurfaceLight,
    onSurfaceVariant = TorchOnSurfaceLight,
    outline = TorchOutlineLight,
    outlineVariant = TorchOutlineVariantLight
)

@Composable
fun TableTorchTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkTableTorchColorScheme else LightTableTorchColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context.findActivity()
            if (activity != null) {
                val window = activity.window
                val insetsController = WindowCompat.getInsetsController(window, view)

                // Use modern edge-to-edge approach - adjust status/nav bar appearance based on theme
                insetsController.isAppearanceLightStatusBars = !darkTheme
                insetsController.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
