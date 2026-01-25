package com.rockyriverapps.tabletorch.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.rockyriverapps.tabletorch.R
import com.rockyriverapps.tabletorch.util.findActivity
import com.rockyriverapps.tabletorch.data.AppSettings
import com.rockyriverapps.tabletorch.ui.components.BrightnessSlider
import com.rockyriverapps.tabletorch.ui.components.ColorButtonsRow
import com.rockyriverapps.tabletorch.ui.theme.TorchBackground
import com.rockyriverapps.tabletorch.ui.theme.TorchSurface

/**
 * Main screen of the TableTorch app.
 * Displays a maximized color panel with consolidated bottom controls.
 * TopAppBar removed to maximize screen light output - brand identity is on splash screen.
 */
@Composable
fun MainScreen(
    settings: AppSettings,
    brightness: Float,
    onBrightnessChange: (Float) -> Unit,
    onColorSelect: (Int) -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentColor = settings.getCurrentColor()
    val density = LocalDensity.current
    val view = LocalView.current

    // Get system insets for proper edge-to-edge handling
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues()
    val navigationBarPadding = WindowInsets.navigationBars.asPaddingValues()

    // Track the actual height of the bottom control panel
    var bottomPanelHeight by remember { mutableStateOf(0.dp) }

    // Dynamically adjust status bar icon color based on displayed color luminance
    // Light colors get dark icons, dark colors get light icons for better visibility
    DisposableEffect(currentColor) {
        val activity = view.context.findActivity()
        if (activity != null) {
            val window = activity.window
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = currentColor.luminance() > 0.5f
        }
        onDispose { }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(TorchBackground)
    ) {
        // Color display area - maximized to fill available space
        // Uses dynamic padding based on status bar and measured bottom panel height
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    // Add status bar height plus small margin to avoid overlap
                    top = statusBarPadding.calculateTopPadding() + 8.dp,
                    // Use measured bottom panel height plus small gap for visual separation
                    bottom = bottomPanelHeight + 8.dp
                )
                .clip(RoundedCornerShape(24.dp))
                .border(2.dp, currentColor.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
                .background(currentColor)
        )

        // Unified bottom control panel
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .onSizeChanged { size ->
                    // Measure the actual height of this panel to use for color panel padding
                    bottomPanelHeight = with(density) { size.height.toDp() }
                },
            color = TorchSurface.copy(alpha = 0.8f),
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = 16.dp,
                        // Add navigation bar padding at the bottom for gesture navigation
                        bottom = 16.dp + navigationBarPadding.calculateBottomPadding()
                    )
            ) {
                // Brightness slider
                BrightnessSlider(
                    value = brightness,
                    onValueChange = onBrightnessChange,
                    enabled = !settings.isAngleBasedBrightnessActive
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom row with color buttons and settings
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Color selection buttons
                    ColorButtonsRow(
                        colors = settings.selectedColors,
                        selectedIndex = settings.lastSelectedColorIndex,
                        onColorSelect = onColorSelect
                    )

                    // Settings button
                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.settings),
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }
}
