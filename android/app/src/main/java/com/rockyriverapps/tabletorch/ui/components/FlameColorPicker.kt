package com.rockyriverapps.tabletorch.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rockyriverapps.tabletorch.R
import com.rockyriverapps.tabletorch.ui.theme.TorchOrange
import kotlin.math.sqrt
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf

/**
 * Static preset colors and names for the color grid picker.
 * Using ImmutableList for Compose stability.
 * Marked as @Stable since all values are immutable.
 */
@Stable
private object ColorGridPresets {
    val presetColors: ImmutableList<Color> = persistentListOf(
        // Row 1 - Whites and grays
        Color.White, Color(0xFFE0E0E0), Color(0xFFC0C0C0), Color(0xFFA0A0A0),
        Color(0xFF808080), Color(0xFF606060), Color(0xFF404040), Color.Black,
        // Row 2 - Warm whites and yellows
        Color(0xFFFFF8E1), Color(0xFFFFECB3), Color(0xFFFFE082), Color(0xFFFFD54F),
        Color(0xFFFFCA28), Color(0xFFFFC107), Color(0xFFFFB300), Color(0xFFFFA000),
        // Row 3 - Oranges and reds
        Color(0xFFFFCC80), Color(0xFFFFB74D), Color(0xFFFFA726), Color(0xFFFF9800),
        Color(0xFFFF7043), Color(0xFFFF5722), Color(0xFFF44336), Color(0xFFE53935),
        // Row 4 - Pinks and purples
        Color(0xFFF8BBD9), Color(0xFFF48FB1), Color(0xFFEC407A), Color(0xFFE91E63),
        Color(0xFFCE93D8), Color(0xFFBA68C8), Color(0xFF9C27B0), Color(0xFF7B1FA2),
        // Row 5 - Blues
        Color(0xFFBBDEFB), Color(0xFF90CAF9), Color(0xFF64B5F6), Color(0xFF42A5F5),
        Color(0xFF2196F3), Color(0xFF1E88E5), Color(0xFF1976D2), Color(0xFF1565C0),
        // Row 6 - Teals and greens
        Color(0xFFB2DFDB), Color(0xFF80CBC4), Color(0xFF4DB6AC), Color(0xFF26A69A),
        Color(0xFFA5D6A7), Color(0xFF81C784), Color(0xFF66BB6A), Color(0xFF4CAF50),
        // Row 7 - Light greens and limes
        Color(0xFFC5E1A5), Color(0xFFAED581), Color(0xFF9CCC65), Color(0xFF8BC34A),
        Color(0xFFDCE775), Color(0xFFD4E157), Color(0xFFCDDC39), Color(0xFFC0CA33),
        // Row 8 - Browns
        Color(0xFFD7CCC8), Color(0xFFBCAAA4), Color(0xFFA1887F), Color(0xFF8D6E63),
        Color(0xFF795548), Color(0xFF6D4C41), Color(0xFF5D4037), Color(0xFF4E342E)
    )

    // Color names moved to string-array resource (R.array.color_grid_names) for localization
}

/**
 * Full-featured color picker dialog with Grid, Spectrum, and Sliders tabs.
 */
@Composable
fun FullColorPickerDialog(
    initialColor: Color,
    onColorSelected: (Color) -> Unit,
    onDismiss: () -> Unit
) {
    // Convert initial color to HSV for easier manipulation
    val hsv = remember(initialColor) {
        floatArrayOf(0f, 0f, 0f).also { arr ->
            android.graphics.Color.colorToHSV(
                android.graphics.Color.argb(
                    (initialColor.alpha * 255).toInt(),
                    (initialColor.red * 255).toInt(),
                    (initialColor.green * 255).toInt(),
                    (initialColor.blue * 255).toInt()
                ),
                arr
            )
        }
    }

    var hue by remember(initialColor) { mutableFloatStateOf(hsv[0]) }
    var saturation by remember(initialColor) { mutableFloatStateOf(hsv[1]) }
    var value by remember(initialColor) { mutableFloatStateOf(hsv[2]) }

    // RGB values for slider mode
    var red by remember { mutableFloatStateOf(initialColor.red) }
    var green by remember { mutableFloatStateOf(initialColor.green) }
    var blue by remember { mutableFloatStateOf(initialColor.blue) }

    var selectedTab by remember { mutableIntStateOf(0) }

    // Current color based on mode - memoized to avoid recomputing on unrelated state changes
    val currentColor by remember {
        derivedStateOf {
            when (selectedTab) {
                2 -> Color(red, green, blue) // Sliders use RGB
                else -> Color.hsv(hue % 360f, saturation, value) // Grid and Spectrum use HSV
            }
        }
    }

    // Sync RGB when HSV changes (for non-slider modes)
    fun syncRgbFromHsv() {
        val c = Color.hsv(hue % 360f, saturation, value)
        red = c.red
        green = c.green
        blue = c.blue
    }

    // Sync HSV when RGB changes (for slider mode)
    fun syncHsvFromRgb() {
        val tempHsv = floatArrayOf(0f, 0f, 0f)
        android.graphics.Color.colorToHSV(
            android.graphics.Color.argb(
                255,
                (red * 255).toInt(),
                (green * 255).toInt(),
                (blue * 255).toInt()
            ),
            tempHsv
        )
        hue = tempHsv[0]
        saturation = tempHsv[1]
        value = tempHsv[2]
    }

    val tabs = listOf(
        stringResource(R.string.tab_grid),
        stringResource(R.string.tab_spectrum),
        stringResource(R.string.tab_sliders)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth(0.95f),
        title = {
            Text(stringResource(R.string.select_color), color = MaterialTheme.colorScheme.onBackground)
        },
        text = {
            Column {
                // Color preview - Current vs New side by side
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Current (original) color
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .clip(RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp))
                                .background(initialColor)
                                .border(
                                    1.dp,
                                    Color.White.copy(alpha = 0.3f),
                                    RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)
                                )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.current_color),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                    // New (selected) color
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .clip(RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp))
                                .background(currentColor)
                                .border(
                                    1.dp,
                                    Color.White.copy(alpha = 0.3f),
                                    RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp)
                                )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.new_color),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Tab row
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary,
                    indicator = { tabPositions ->
                        SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = {
                                // Sync colors when switching tabs
                                when {
                                    selectedTab == 2 && index != 2 -> syncHsvFromRgb()
                                    selectedTab != 2 && index == 2 -> syncRgbFromHsv()
                                }
                                selectedTab = index
                            },
                            modifier = Modifier.padding(horizontal = 0.dp),
                            text = {
                                Text(
                                    title,
                                    color = if (selectedTab == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Tab content
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                ) {
                    when (selectedTab) {
                        0 -> ColorGridPicker(
                            selectedColor = currentColor,
                            onColorSelect = { color ->
                                val tempHsv = floatArrayOf(0f, 0f, 0f)
                                android.graphics.Color.colorToHSV(
                                    android.graphics.Color.argb(
                                        255,
                                        (color.red * 255).toInt(),
                                        (color.green * 255).toInt(),
                                        (color.blue * 255).toInt()
                                    ),
                                    tempHsv
                                )
                                hue = tempHsv[0]
                                saturation = tempHsv[1]
                                value = tempHsv[2]
                                syncRgbFromHsv()
                            }
                        )
                        1 -> ColorSpectrumPicker(
                            hue = hue,
                            saturation = saturation,
                            value = value,
                            onHueChange = { hue = it; syncRgbFromHsv() },
                            onSaturationValueChange = { s, v ->
                                saturation = s
                                value = v
                                syncRgbFromHsv()
                            }
                        )
                        2 -> ColorSlidersPicker(
                            red = red,
                            green = green,
                            blue = blue,
                            onRedChange = { red = it },
                            onGreenChange = { green = it },
                            onBlueChange = { blue = it }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onColorSelected(currentColor) }) {
                Text(stringResource(R.string.select), color = MaterialTheme.colorScheme.primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

/**
 * Grid of preset colors
 */
@Composable
private fun ColorGridPicker(
    selectedColor: Color,
    onColorSelect: (Color) -> Unit
) {
    val colorNames = stringArrayResource(R.array.color_grid_names)

    LazyVerticalGrid(
        columns = GridCells.Fixed(6),
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(ColorGridPresets.presetColors, key = { index, _ -> index }) { index, color ->
            val isSelected = colorDistance(color, selectedColor) < 0.05f
            // Calculate luminance to determine checkmark color (white on dark, black on light)
            val luminance = 0.299f * color.red + 0.587f * color.green + 0.114f * color.blue
            val checkmarkColor = if (luminance > 0.5f) Color.Black else Color.White
            val colorName = colorNames.getOrElse(index) { "Color ${index + 1}" }
            val selectedSuffix = if (isSelected) stringResource(R.string.color_selected_suffix) else ""

            Box(
                modifier = Modifier
                    .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(4.dp))
                    .background(color)
                    .border(
                        width = if (isSelected) 2.dp else 0.dp,
                        color = if (isSelected) TorchOrange else Color.Transparent,
                        shape = RoundedCornerShape(4.dp)
                    )
                    .clickable { onColorSelect(color) }
                    .semantics {
                        contentDescription = "$colorName$selectedSuffix"
                    },
                contentAlignment = Alignment.Center
            ) {
                // Show checkmark on selected color
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = checkmarkColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * Color spectrum picker with hue wheel and saturation/value square
 */
@Composable
private fun ColorSpectrumPicker(
    hue: Float,
    saturation: Float,
    value: Float,
    onHueChange: (Float) -> Unit,
    onSaturationValueChange: (Float, Float) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Saturation/Value square
        Box(
            modifier = Modifier
                .weight(1f)
                .aspectRatio(1f)
                .clip(RoundedCornerShape(8.dp))
        ) {
            SaturationValuePanel(
                hue = hue,
                saturation = saturation,
                value = value,
                onSaturationValueChange = onSaturationValueChange
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Hue slider
        HueSlider(
            hue = hue,
            onHueChange = onHueChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
        )
    }
}

/**
 * Saturation/Value panel - horizontal is saturation, vertical is value
 */
@Composable
private fun SaturationValuePanel(
    hue: Float,
    saturation: Float,
    value: Float,
    onSaturationValueChange: (Float, Float) -> Unit
) {
    val satPercent = (saturation * 100).toInt()
    val valPercent = (value * 100).toInt()
    val satBrightDesc = stringResource(R.string.saturation_brightness_picker, satPercent, valPercent)
    val satBrightState = stringResource(R.string.saturation_brightness_state, satPercent, valPercent)
    val increaseSatLabel = stringResource(R.string.a11y_action_increase_saturation)
    val decreaseSatLabel = stringResource(R.string.a11y_action_decrease_saturation)
    val increaseBrightLabel = stringResource(R.string.a11y_action_increase_color_brightness)
    val decreaseBrightLabel = stringResource(R.string.a11y_action_decrease_color_brightness)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .semantics {
                contentDescription = satBrightDesc
                stateDescription = satBrightState
                customActions = listOf(
                    CustomAccessibilityAction(increaseSatLabel) {
                        onSaturationValueChange((saturation + 0.1f).coerceIn(0f, 1f), value); true
                    },
                    CustomAccessibilityAction(decreaseSatLabel) {
                        onSaturationValueChange((saturation - 0.1f).coerceIn(0f, 1f), value); true
                    },
                    CustomAccessibilityAction(increaseBrightLabel) {
                        onSaturationValueChange(saturation, (value + 0.1f).coerceIn(0f, 1f)); true
                    },
                    CustomAccessibilityAction(decreaseBrightLabel) {
                        onSaturationValueChange(saturation, (value - 0.1f).coerceIn(0f, 1f)); true
                    }
                )
            }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val s = (offset.x / size.width).coerceIn(0f, 1f)
                    val v = 1f - (offset.y / size.height).coerceIn(0f, 1f)
                    onSaturationValueChange(s, v)
                }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    change.consume()
                    val s = (change.position.x / size.width).coerceIn(0f, 1f)
                    val v = 1f - (change.position.y / size.height).coerceIn(0f, 1f)
                    onSaturationValueChange(s, v)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Draw saturation gradient (white to hue color)
            val hueColor = Color.hsv(hue % 360f, 1f, 1f)
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.White, hueColor)
                )
            )
            // Draw value gradient (transparent to black)
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black)
                )
            )

            // Draw selector circle
            val selectorX = saturation * size.width
            val selectorY = (1f - value) * size.height
            drawCircle(
                color = Color.White,
                radius = 12.dp.toPx(),
                center = Offset(selectorX, selectorY),
                style = Stroke(width = 3.dp.toPx())
            )
            drawCircle(
                color = Color.Black,
                radius = 10.dp.toPx(),
                center = Offset(selectorX, selectorY),
                style = Stroke(width = 1.dp.toPx())
            )
        }
    }
}

/**
 * Horizontal hue slider with rainbow gradient
 */
@Composable
private fun HueSlider(
    hue: Float,
    onHueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val hueInt = hue.toInt()
    val hueDesc = stringResource(R.string.hue_slider_description, hueInt)
    val hueState = stringResource(R.string.hue_slider_state, hueInt)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .semantics {
                contentDescription = hueDesc
                stateDescription = hueState
                setProgress(label = null) { targetValue ->
                    val newHue = (targetValue.coerceIn(0f, 1f) * 360f) % 360f
                    onHueChange(newHue)
                    true
                }
            }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val h = (offset.x / size.width * 360f).coerceIn(0f, 360f) % 360f
                    onHueChange(h)
                }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    change.consume()
                    val h = (change.position.x / size.width * 360f).coerceIn(0f, 360f) % 360f
                    onHueChange(h)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Draw rainbow gradient
            val hueColors = (0..360 step 30).map { Color.hsv(it.toFloat(), 1f, 1f) }
            drawRect(
                brush = Brush.horizontalGradient(colors = hueColors)
            )

            // Draw selector
            val selectorX = (hue / 360f) * size.width
            drawCircle(
                color = Color.White,
                radius = 14.dp.toPx(),
                center = Offset(selectorX, size.height / 2),
                style = Stroke(width = 3.dp.toPx())
            )
            drawCircle(
                color = Color.hsv(hue % 360f, 1f, 1f),
                radius = 10.dp.toPx(),
                center = Offset(selectorX, size.height / 2)
            )
        }
    }
}

/**
 * RGB Sliders picker with editable hex input
 */
@Composable
private fun ColorSlidersPicker(
    red: Float,
    green: Float,
    blue: Float,
    onRedChange: (Float) -> Unit,
    onGreenChange: (Float) -> Unit,
    onBlueChange: (Float) -> Unit
) {
    // Hex input state - updates when RGB values change
    var hexInput by remember(red, green, blue) {
        mutableStateOf(
            String.format(
                "%02X%02X%02X",
                (red * 255).toInt(),
                (green * 255).toInt(),
                (blue * 255).toInt()
            )
        )
    }
    var isHexError by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Red slider
        ColorChannelSlider(
            label = stringResource(R.string.red),
            value = red,
            onValueChange = onRedChange,
            gradientColors = listOf(
                Color(0f, green, blue),
                Color(1f, green, blue)
            ),
            thumbColor = Color.Red
        )

        // Green slider
        ColorChannelSlider(
            label = stringResource(R.string.green),
            value = green,
            onValueChange = onGreenChange,
            gradientColors = listOf(
                Color(red, 0f, blue),
                Color(red, 1f, blue)
            ),
            thumbColor = Color.Green
        )

        // Blue slider
        ColorChannelSlider(
            label = stringResource(R.string.blue),
            value = blue,
            onValueChange = onBlueChange,
            gradientColors = listOf(
                Color(red, green, 0f),
                Color(red, green, 1f)
            ),
            thumbColor = Color.Blue
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Editable hex input field
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "#",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(4.dp))
            OutlinedTextField(
                value = hexInput,
                onValueChange = { newValue ->
                    // Filter to only allow valid hex characters (0-9, A-F)
                    val filtered = newValue.uppercase().filter { it in "0123456789ABCDEF" }.take(6)
                    hexInput = filtered

                    // Validate and update RGB sliders if valid 6-character hex
                    if (filtered.length == 6) {
                        try {
                            val r = filtered.substring(0, 2).toInt(16) / 255f
                            val g = filtered.substring(2, 4).toInt(16) / 255f
                            val b = filtered.substring(4, 6).toInt(16) / 255f
                            onRedChange(r)
                            onGreenChange(g)
                            onBlueChange(b)
                            isHexError = false
                        } catch (e: Exception) {
                            isHexError = true
                        }
                    } else {
                        isHexError = filtered.isNotEmpty()
                    }
                },
                modifier = Modifier.width(120.dp),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onBackground,
                    letterSpacing = 2.sp
                ),
                singleLine = true,
                isError = isHexError,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                    errorBorderColor = Color.Red,
                    cursorColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}

@Composable
private fun ColorChannelSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    gradientColors: List<Color>,
    thumbColor: Color
) {
    val valueInt = (value * 255).toInt()

    val channelDesc = stringResource(R.string.color_channel_slider_description, label, valueInt)
    val channelState = stringResource(R.string.color_channel_slider_state, label, valueInt)

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "$valueInt",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    brush = Brush.horizontalGradient(gradientColors)
                )
                .semantics {
                    contentDescription = channelDesc
                    stateDescription = channelState
                    setProgress(label = null) { targetValue ->
                        onValueChange(targetValue.coerceIn(0f, 1f))
                        true
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        onValueChange((offset.x / size.width).coerceIn(0f, 1f))
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        onValueChange((change.position.x / size.width).coerceIn(0f, 1f))
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Draw thumb
                val thumbX = value * size.width
                drawCircle(
                    color = Color.White,
                    radius = 12.dp.toPx(),
                    center = Offset(thumbX, size.height / 2),
                    style = Stroke(width = 2.dp.toPx())
                )
                drawCircle(
                    color = thumbColor,
                    radius = 9.dp.toPx(),
                    center = Offset(thumbX, size.height / 2)
                )
            }
        }
    }
}

/**
 * Calculate color distance for selection comparison
 */
private fun colorDistance(c1: Color, c2: Color): Float {
    val dr = c1.red - c2.red
    val dg = c1.green - c2.green
    val db = c1.blue - c2.blue
    return sqrt(dr * dr + dg * dg + db * db)
}
