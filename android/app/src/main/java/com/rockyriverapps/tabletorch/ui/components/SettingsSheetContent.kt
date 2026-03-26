package com.rockyriverapps.tabletorch.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import com.rockyriverapps.tabletorch.R
import com.rockyriverapps.tabletorch.data.AppSettings
import com.rockyriverapps.tabletorch.data.ColorPalette
import com.rockyriverapps.tabletorch.models.ParticleShape
import com.rockyriverapps.tabletorch.ui.theme.TorchSliderDefaults
import com.rockyriverapps.tabletorch.ui.theme.toComposeColor
import kotlinx.collections.immutable.ImmutableList

// ============================================================================
// Color Constants for Settings Sheet Styling
// ============================================================================

/** Warm golden-amber color for section headers, matching iOS design. */
private val SectionHeaderColor = Color(0xFFFFB74D)

/** Warm orange accent for active toggles. */
private val ToggleActiveColor = Color(0xFFFFA726)

/** Subtle text color for descriptions and secondary content. */
private val SubtitleTextColor = Color.White.copy(alpha = 0.6f)

/** Divider color between sections. */
private val DividerColor = Color.White.copy(alpha = 0.08f)

// ============================================================================
// Main Settings Sheet Content
// ============================================================================

/**
 * Reusable composable containing all settings content for the bottom sheet.
 * Designed to be placed inside a ModalBottomSheet with a dark, warm-tinted theme.
 *
 * Sections:
 * 1. Torch Colors - 2x3 color swatch grid with edit actions
 * 2. Brightness - Default brightness slider and related toggles
 * 3. Behavior - Screen lock and quick color bar toggles
 * 4. Visual Effects - Breathing animation and ember particles (future)
 */
@Composable
fun SettingsSheetContent(
    settings: AppSettings,
    onDefaultBrightnessChange: (Float) -> Unit,
    onUseDefaultBrightnessOnLaunchChange: (Boolean) -> Unit,
    onPreventScreenLockChange: (Boolean) -> Unit,
    onAngleBasedBrightnessChange: (Boolean) -> Unit,
    onColorChange: (Int, Long) -> Unit,
    onColorSelect: (Int) -> Unit = {},
    onRestoreDefaultColors: () -> Unit,
    onPaletteSelect: (String) -> Unit = {},
    onNavigateToPalettes: () -> Unit = {},
    onShowQuickColorBarChange: (Boolean) -> Unit = {},
    onAlwaysShowBrightnessChange: (Boolean) -> Unit = {},
    onEnableBreathingAnimationChange: (Boolean) -> Unit = {},
    onBreathingDepthChange: (Float) -> Unit = {},
    onBreathingCycleDurationChange: (Float) -> Unit = {},
    onEnableEmberParticlesChange: (Boolean) -> Unit = {},
    onParticleShapeChange: (ParticleShape) -> Unit = {},
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        // Sheet header with title and Done button
        SheetHeader(onDismiss = onDismiss)

        Spacer(modifier = Modifier.height(16.dp))

        // Section 1: Torch Colors
        SectionHeader(title = stringResource(R.string.section_torch_colors))
        Spacer(modifier = Modifier.height(12.dp))
        TorchColorsSection(
            settings = settings,
            onColorChange = onColorChange,
            onColorSelect = onColorSelect,
            onRestoreDefaultColors = onRestoreDefaultColors,
            onPaletteSelect = onPaletteSelect,
            onNavigateToPalettes = onNavigateToPalettes
        )

        SectionDivider()

        // Section 2: Brightness
        SectionHeader(title = stringResource(R.string.settings_section_brightness))
        Spacer(modifier = Modifier.height(12.dp))
        BrightnessSection(
            defaultBrightness = settings.defaultBrightness,
            useDefaultBrightnessOnLaunch = settings.useDefaultBrightnessOnLaunch,
            isAngleBasedBrightnessActive = settings.isAngleBasedBrightnessActive,
            alwaysShowBrightness = settings.alwaysShowBrightness,
            onDefaultBrightnessChange = onDefaultBrightnessChange,
            onUseDefaultBrightnessOnLaunchChange = onUseDefaultBrightnessOnLaunchChange,
            onAngleBasedBrightnessChange = onAngleBasedBrightnessChange,
            onAlwaysShowBrightnessChange = onAlwaysShowBrightnessChange
        )

        SectionDivider()

        // Section 3: Behavior
        SectionHeader(title = stringResource(R.string.settings_section_behavior))
        Spacer(modifier = Modifier.height(12.dp))
        BehaviorSection(
            preventScreenLock = settings.preventScreenLock,
            showQuickColorBar = settings.showQuickColorBar,
            onPreventScreenLockChange = onPreventScreenLockChange,
            onShowQuickColorBarChange = onShowQuickColorBarChange
        )

        SectionDivider()

        // Section 4: Visual Effects
        SectionHeader(title = stringResource(R.string.settings_section_visual_effects))
        Spacer(modifier = Modifier.height(12.dp))
        VisualEffectsSection(
            settings = settings,
            onEnableBreathingAnimationChange = onEnableBreathingAnimationChange,
            onBreathingDepthChange = onBreathingDepthChange,
            onBreathingCycleDurationChange = onBreathingCycleDurationChange,
            onEnableEmberParticlesChange = onEnableEmberParticlesChange,
            onParticleShapeChange = onParticleShapeChange
        )

        // Bottom padding for safe area
        Spacer(modifier = Modifier.height(32.dp))
    }
}

// ============================================================================
// Sheet Header
// ============================================================================

/**
 * Header row with centered "Settings" title and "Done" dismiss button.
 */
@Composable
private fun SheetHeader(onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = stringResource(R.string.settings_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            modifier = Modifier.align(Alignment.Center)
        )

        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(R.string.settings_done),
                tint = Color.White.copy(alpha = 0.9f)
            )
        }
    }
}

// ============================================================================
// Section Header & Divider
// ============================================================================

/**
 * Warm golden-amber section header matching iOS design language.
 */
@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = SectionHeaderColor,
        letterSpacing = MaterialTheme.typography.labelLarge.letterSpacing,
        modifier = Modifier.semantics { heading() }
    )
}

/**
 * Subtle divider between settings sections.
 */
@Composable
private fun SectionDivider() {
    Spacer(modifier = Modifier.height(20.dp))
    HorizontalDivider(
        color = DividerColor,
        thickness = 0.5.dp
    )
    Spacer(modifier = Modifier.height(20.dp))
}

// ============================================================================
// Section 1: Torch Colors
// ============================================================================

/**
 * 2x3 grid of color swatches showing each torch color,
 * followed by a palette chip row for quick switching and action buttons.
 */
@Composable
private fun TorchColorsSection(
    settings: AppSettings,
    onColorChange: (Int, Long) -> Unit,
    onColorSelect: (Int) -> Unit,
    onRestoreDefaultColors: () -> Unit,
    onPaletteSelect: (String) -> Unit,
    onNavigateToPalettes: () -> Unit
) {
    // 2x3 grid of color swatches using regular Column/Row (avoids nested scrollable issues)
    val colors = settings.selectedColors
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        for (row in 0 until 2) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                for (col in 0 until 3) {
                    val index = row * 3 + col
                    if (index < colors.size) {
                        key(index) {
                            Box(modifier = Modifier.weight(1f)) {
                                ColorSwatchCard(
                                    index = index,
                                    colorValue = colors[index],
                                    isActive = index == settings.lastSelectedColorIndex,
                                    onSelect = { onColorSelect(index) },
                                    onColorChange = { newColor -> onColorChange(index, newColor) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Palette chip row for quick switching between palettes
    PaletteChipRow(
        palettes = remember(settings.customPalettes) { settings.allPalettes },
        activePaletteId = settings.activePaletteId,
        onPaletteSelect = onPaletteSelect,
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(8.dp))

    // Action buttons row: Restore defaults + Manage palettes
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(
            onClick = onRestoreDefaultColors,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = stringResource(R.string.restore_default_colors),
                style = MaterialTheme.typography.bodyMedium,
                color = ToggleActiveColor
            )
        }

        TextButton(
            onClick = onNavigateToPalettes,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = stringResource(R.string.manage_palettes),
                style = MaterialTheme.typography.bodyMedium,
                color = ToggleActiveColor
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = ToggleActiveColor,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

/**
 * Individual color swatch card showing the torch color with a label.
 * Active color has a small indicator dot overlay.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ColorSwatchCard(
    index: Int,
    colorValue: Long,
    isActive: Boolean,
    onSelect: () -> Unit,
    onColorChange: (Long) -> Unit
) {
    val color = colorValue.toComposeColor()
    val view = LocalView.current
    val torchLabel = stringResource(R.string.torch_label, index + 1)
    val swatchDescription = if (isActive) {
        stringResource(R.string.a11y_torch_swatch_active, torchLabel)
    } else {
        stringResource(R.string.a11y_torch_swatch, torchLabel)
    }
    val editColorLabel = stringResource(R.string.a11y_action_edit_color)

    // Track whether the color picker dialog is showing
    var showDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(color)
            .then(
                if (isActive) {
                    Modifier.border(2.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(12.dp))
                } else {
                    Modifier.border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                }
            )
            .combinedClickable(
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                    onSelect()
                },
                onLongClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    showDialog = true
                }
            )
            .semantics {
                contentDescription = swatchDescription
                customActions = listOf(
                    CustomAccessibilityAction(editColorLabel) {
                        showDialog = true
                        true
                    }
                )
            },
        contentAlignment = Alignment.BottomCenter
    ) {
        // Active checkmark indicator in top-right corner
        if (isActive) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "\u2713",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Label at the bottom of the swatch
        Text(
            text = torchLabel,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = if (isLightColor(color)) Color.Black.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.8f),
            modifier = Modifier.padding(bottom = 6.dp)
        )

        // Edit icon hint for long-press color editing
        Icon(
            imageVector = Icons.Outlined.Edit,
            contentDescription = null,
            tint = if (color.luminance() > 0.5f) Color.Black.copy(alpha = 0.35f)
                   else Color.White.copy(alpha = 0.35f),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(6.dp)
                .size(14.dp)
        )
    }

    // Color picker dialog (opened via long press)
    if (showDialog) {
        FullColorPickerDialog(
            initialColor = color,
            onColorSelected = { newColor ->
                onColorChange(newColor.toArgb().toLong() and 0xFFFFFFFFL)
                showDialog = false
            },
            onDismiss = { showDialog = false }
        )
    }
}

/**
 * Determine if a color is light (for text contrast).
 */
private fun isLightColor(color: Color): Boolean {
    val luminance = 0.299f * color.red + 0.587f * color.green + 0.114f * color.blue
    return luminance > 0.5f
}

// ============================================================================
// Section 2: Brightness
// ============================================================================

@Composable
private fun BrightnessSection(
    defaultBrightness: Float,
    useDefaultBrightnessOnLaunch: Boolean,
    isAngleBasedBrightnessActive: Boolean,
    alwaysShowBrightness: Boolean,
    onDefaultBrightnessChange: (Float) -> Unit,
    onUseDefaultBrightnessOnLaunchChange: (Boolean) -> Unit,
    onAngleBasedBrightnessChange: (Boolean) -> Unit,
    onAlwaysShowBrightnessChange: (Boolean) -> Unit
) {
    // Default brightness slider
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.default_brightness),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White
            )
            Text(
                text = "${(defaultBrightness * 100).toInt()}%",
                style = MaterialTheme.typography.bodyMedium,
                color = ToggleActiveColor
            )
        }

        val brightnessPercent = (defaultBrightness * 100).toInt()
        val brightnessSliderDesc = stringResource(R.string.a11y_brightness_slider, brightnessPercent)
        val brightnessStateDesc = stringResource(R.string.a11y_brightness_state, brightnessPercent)
        Slider(
            value = defaultBrightness,
            onValueChange = onDefaultBrightnessChange,
            valueRange = 0.1f..1f,
            steps = 8,
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = brightnessSliderDesc
                    stateDescription = brightnessStateDesc
                },
            colors = TorchSliderDefaults.colors()
        )
    }

    Spacer(modifier = Modifier.height(8.dp))

    // Use default on launch toggle
    SheetToggleRow(
        title = stringResource(R.string.use_default_brightness_on_launch),
        checked = useDefaultBrightnessOnLaunch,
        onCheckedChange = onUseDefaultBrightnessOnLaunchChange
    )

    Spacer(modifier = Modifier.height(4.dp))

    // Tilt brightness control toggle
    SheetToggleRow(
        title = stringResource(R.string.enable_tilt_brightness),
        subtitle = stringResource(R.string.tilt_description),
        checked = isAngleBasedBrightnessActive,
        onCheckedChange = onAngleBasedBrightnessChange
    )

    Spacer(modifier = Modifier.height(4.dp))

    // Always show brightness indicator toggle
    SheetToggleRow(
        title = stringResource(R.string.settings_always_show_brightness),
        subtitle = stringResource(R.string.settings_always_show_brightness_desc),
        checked = alwaysShowBrightness,
        onCheckedChange = onAlwaysShowBrightnessChange
    )
}

// ============================================================================
// Section 3: Behavior
// ============================================================================

@Composable
private fun BehaviorSection(
    preventScreenLock: Boolean,
    showQuickColorBar: Boolean,
    onPreventScreenLockChange: (Boolean) -> Unit,
    onShowQuickColorBarChange: (Boolean) -> Unit
) {
    SheetToggleRow(
        title = stringResource(R.string.prevent_screen_lock),
        subtitle = stringResource(R.string.prevent_screen_lock_description),
        checked = preventScreenLock,
        onCheckedChange = onPreventScreenLockChange
    )

    Spacer(modifier = Modifier.height(4.dp))

    // Quick Color Bar toggle
    SheetToggleRow(
        title = stringResource(R.string.settings_quick_color_bar),
        subtitle = stringResource(R.string.settings_quick_color_bar_desc),
        checked = showQuickColorBar,
        onCheckedChange = onShowQuickColorBarChange
    )
}

// ============================================================================
// Section 4: Visual Effects
// ============================================================================

@Composable
private fun VisualEffectsSection(
    settings: AppSettings,
    onEnableBreathingAnimationChange: (Boolean) -> Unit,
    onBreathingDepthChange: (Float) -> Unit,
    onBreathingCycleDurationChange: (Float) -> Unit,
    onEnableEmberParticlesChange: (Boolean) -> Unit,
    onParticleShapeChange: (ParticleShape) -> Unit
) {
    // Breathing animation toggle
    SheetToggleRow(
        title = stringResource(R.string.settings_breathing_animation),
        subtitle = stringResource(R.string.settings_breathing_animation_desc),
        checked = settings.enableBreathingAnimation,
        onCheckedChange = onEnableBreathingAnimationChange
    )

    // Expandable breathing settings when enabled
    AnimatedVisibility(
        visible = settings.enableBreathingAnimation,
        enter = expandVertically(),
        exit = shrinkVertically()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, top = 8.dp)
        ) {
            // Breathing depth slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.settings_breathing_depth),
                    style = MaterialTheme.typography.bodySmall,
                    color = SubtitleTextColor
                )
                Text(
                    text = "${(settings.breathingDepth * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = ToggleActiveColor
                )
            }
            Slider(
                value = settings.breathingDepth,
                onValueChange = onBreathingDepthChange,
                valueRange = 0.02f..0.40f,
                steps = 7,
                modifier = Modifier.fillMaxWidth(),
                colors = TorchSliderDefaults.colors()
            )

            // Breathing cycle duration slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.settings_breathing_cycle),
                    style = MaterialTheme.typography.bodySmall,
                    color = SubtitleTextColor
                )
                Text(
                    text = "${settings.breathingCycleDuration.toInt()}s",
                    style = MaterialTheme.typography.bodySmall,
                    color = ToggleActiveColor
                )
            }
            Slider(
                value = settings.breathingCycleDuration,
                onValueChange = onBreathingCycleDurationChange,
                valueRange = 1f..10f,
                steps = 8,
                modifier = Modifier.fillMaxWidth(),
                colors = TorchSliderDefaults.colors()
            )
        }
    }

}

// ============================================================================
// Shared Toggle Row Component
// ============================================================================

/**
 * Toggle row styled for the settings bottom sheet.
 * Uses warm orange accent color for the active state.
 * Maintains 48dp minimum touch target for accessibility.
 */
@Composable
private fun SheetToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    subtitle: String? = null
) {
    val toggleState = if (checked) stringResource(R.string.toggle_on) else stringResource(R.string.toggle_off)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(role = Role.Switch) { onCheckedChange(!checked) }
            .padding(vertical = 10.dp)
            .semantics(mergeDescendants = true) {
                stateDescription = toggleState
            },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = SubtitleTextColor
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = ToggleActiveColor,
                checkedTrackColor = ToggleActiveColor.copy(alpha = 0.4f),
                uncheckedThumbColor = Color.Gray,
                uncheckedTrackColor = Color.Gray.copy(alpha = 0.3f)
            )
        )
    }
}
