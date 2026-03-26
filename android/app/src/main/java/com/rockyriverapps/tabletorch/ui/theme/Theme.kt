package com.rockyriverapps.tabletorch.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
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

@Composable
fun TableTorchTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = DarkTableTorchColorScheme
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
