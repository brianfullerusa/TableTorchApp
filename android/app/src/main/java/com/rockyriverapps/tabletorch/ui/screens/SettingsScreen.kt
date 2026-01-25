package com.rockyriverapps.tabletorch.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.rockyriverapps.tabletorch.R
import com.rockyriverapps.tabletorch.data.AppSettings
import com.rockyriverapps.tabletorch.ui.components.HorizontalFlameColorRow
import kotlinx.collections.immutable.ImmutableList
import com.rockyriverapps.tabletorch.ui.theme.TableTorchDimens
import com.rockyriverapps.tabletorch.ui.theme.TorchBackground
import com.rockyriverapps.tabletorch.ui.theme.TorchSliderDefaults
import com.rockyriverapps.tabletorch.ui.theme.TorchSurface

/**
 * Settings screen for configuring torch colors and behavior.
 * Uses a card-based Material 3 layout for visual organization and hierarchy.
 * All settings fit on a single page through efficient use of space.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    onNavigateBack: () -> Unit,
    onDefaultBrightnessChange: (Float) -> Unit,
    onUseDefaultBrightnessOnLaunchChange: (Boolean) -> Unit,
    onPreventScreenLockChange: (Boolean) -> Unit,
    onAngleBasedBrightnessChange: (Boolean) -> Unit,
    onColorChange: (Int, Long) -> Unit,
    onRestoreDefaultColors: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_title),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = TorchBackground
                )
            )
        },
        containerColor = TorchBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(TorchBackground)
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = TableTorchDimens.SpacingMd,
                    vertical = TableTorchDimens.SpacingSm
                ),
            verticalArrangement = Arrangement.spacedBy(TableTorchDimens.SpacingMd)
        ) {
            // Torch Colors Card
            TorchColorsCard(
                selectedColors = settings.selectedColors,
                onColorChange = onColorChange,
                onRestoreDefaultColors = onRestoreDefaultColors
            )

            // Brightness Settings Card
            BrightnessSettingsCard(
                defaultBrightness = settings.defaultBrightness,
                useDefaultBrightnessOnLaunch = settings.useDefaultBrightnessOnLaunch,
                preventScreenLock = settings.preventScreenLock,
                onDefaultBrightnessChange = onDefaultBrightnessChange,
                onUseDefaultBrightnessOnLaunchChange = onUseDefaultBrightnessOnLaunchChange,
                onPreventScreenLockChange = onPreventScreenLockChange
            )

            // Tilt Control Card
            TiltControlCard(
                isAngleBasedBrightnessActive = settings.isAngleBasedBrightnessActive,
                onAngleBasedBrightnessChange = onAngleBasedBrightnessChange
            )

            // Bottom spacing for edge-to-edge navigation support
            Spacer(modifier = Modifier.height(TableTorchDimens.SpacingMd))
        }
    }
}

/**
 * Card component for Torch Colors section.
 * Displays 6 color pickers in a horizontal scrolling row for maximum space efficiency.
 */
@Composable
private fun TorchColorsCard(
    selectedColors: ImmutableList<Long>,
    onColorChange: (Int, Long) -> Unit,
    onRestoreDefaultColors: () -> Unit
) {
    SettingsCard(
        title = stringResource(R.string.section_torch_colors),
        subtitle = stringResource(R.string.section_torch_colors_subtitle)
    ) {
        // Horizontal scrolling row of flame color pickers
        HorizontalFlameColorRow(
            colors = selectedColors,
            onColorChange = onColorChange,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(TableTorchDimens.SpacingSm))

        // Restore defaults button - right aligned
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(
                onClick = onRestoreDefaultColors,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = stringResource(R.string.restore_default_colors),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * Card component for Brightness Settings section.
 * Contains default brightness slider and related toggles.
 */
@Composable
private fun BrightnessSettingsCard(
    defaultBrightness: Float,
    useDefaultBrightnessOnLaunch: Boolean,
    preventScreenLock: Boolean,
    onDefaultBrightnessChange: (Float) -> Unit,
    onUseDefaultBrightnessOnLaunchChange: (Boolean) -> Unit,
    onPreventScreenLockChange: (Boolean) -> Unit
) {
    SettingsCard(
        title = stringResource(R.string.section_brightness_settings),
        subtitle = stringResource(R.string.section_brightness_settings_subtitle)
    ) {
        // Default brightness slider
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.default_brightness),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${(defaultBrightness * 100).toInt()}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            val brightnessPercent = (defaultBrightness * 100).toInt()
            Slider(
                value = defaultBrightness,
                onValueChange = onDefaultBrightnessChange,
                valueRange = 0.1f..1f,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = "Default brightness slider, $brightnessPercent percent"
                        stateDescription = "$brightnessPercent percent"
                    },
                colors = TorchSliderDefaults.colors()
            )
        }

        Spacer(modifier = Modifier.height(TableTorchDimens.SpacingXs))

        // Use default brightness on launch toggle
        CardToggleRow(
            title = stringResource(R.string.use_default_brightness_on_launch),
            checked = useDefaultBrightnessOnLaunch,
            onCheckedChange = onUseDefaultBrightnessOnLaunchChange
        )

        Spacer(modifier = Modifier.height(TableTorchDimens.SpacingSm))

        // Prevent screen lock toggle
        CardToggleRow(
            title = stringResource(R.string.prevent_screen_lock),
            description = stringResource(R.string.prevent_screen_lock_description),
            checked = preventScreenLock,
            onCheckedChange = onPreventScreenLockChange
        )
    }
}

/**
 * Card component for Tilt Control section.
 * Contains the angle-based brightness toggle.
 */
@Composable
private fun TiltControlCard(
    isAngleBasedBrightnessActive: Boolean,
    onAngleBasedBrightnessChange: (Boolean) -> Unit
) {
    SettingsCard(
        title = stringResource(R.string.section_tilt_control),
        subtitle = stringResource(R.string.section_tilt_control_subtitle)
    ) {
        CardToggleRow(
            title = stringResource(R.string.enable_tilt_brightness),
            description = stringResource(R.string.tilt_description),
            checked = isAngleBasedBrightnessActive,
            onCheckedChange = onAngleBasedBrightnessChange
        )
    }
}

/**
 * Reusable settings card wrapper with consistent Material 3 styling.
 * Provides elevation, rounded corners, and a text-based header with title and optional subtitle.
 * This design follows modern Material Design patterns where section headers use typography
 * hierarchy rather than icons for visual distinction.
 */
@Composable
private fun SettingsCard(
    title: String,
    subtitle: String? = null,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(TableTorchDimens.CornerRadiusMd),
        colors = CardDefaults.cardColors(
            containerColor = TorchSurface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(TableTorchDimens.SpacingMd)
        ) {
            // Card header with title and optional subtitle
            Column(
                modifier = Modifier.padding(bottom = TableTorchDimens.SpacingSm)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(TableTorchDimens.SpacingXs))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Card content
            content()
        }
    }
}

/**
 * Toggle row component for use inside cards.
 * Provides consistent styling for switch toggles within card layouts.
 * Maintains 48dp minimum touch target for accessibility compliance.
 */
@Composable
private fun CardToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    description: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(TableTorchDimens.SpacingSm))
            .clickable(role = Role.Switch) { onCheckedChange(!checked) }
            .padding(vertical = TableTorchDimens.SpacingSm)
            .semantics(mergeDescendants = true) {
                stateDescription = if (checked) "On" else "Off"
            },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (description != null) {
                Spacer(modifier = Modifier.height(TableTorchDimens.SpacingXs))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.width(TableTorchDimens.SpacingSm))

        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                uncheckedThumbColor = Color.Gray,
                uncheckedTrackColor = Color.Gray.copy(alpha = 0.3f)
            )
        )
    }
}
