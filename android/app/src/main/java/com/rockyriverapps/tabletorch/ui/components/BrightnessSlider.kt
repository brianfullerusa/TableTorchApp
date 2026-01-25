package com.rockyriverapps.tabletorch.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rockyriverapps.tabletorch.R
import com.rockyriverapps.tabletorch.ui.theme.TableTorchTheme
import com.rockyriverapps.tabletorch.ui.theme.TorchOrange
import com.rockyriverapps.tabletorch.ui.theme.TorchSliderDefaults
import kotlin.math.abs

/**
 * Brightness slider component with label and percentage display.
 * Matches the iOS brightness slider appearance and behavior.
 * Shows "Tilt Active" indicator when slider is disabled due to tilt control.
 */
@Composable
fun BrightnessSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    var lastHapticValue by remember { mutableFloatStateOf(value) }

    // Threshold for triggering haptic feedback (10% change)
    val hapticThreshold = 0.1f

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .alpha(if (enabled) 1f else 0.55f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.brightness),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground
            )

            val brightnessPercent = (value * 100).toInt()
            Slider(
                value = value,
                onValueChange = { newValue ->
                    // Trigger haptic feedback for significant value changes
                    if (abs(newValue - lastHapticValue) >= hapticThreshold) {
                        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                        lastHapticValue = newValue
                    }
                    onValueChange(newValue)
                },
                enabled = enabled,
                valueRange = 0.01f..1f,
                modifier = Modifier
                    .weight(1f)
                    .semantics {
                        contentDescription = "Brightness slider, $brightnessPercent percent"
                        stateDescription = "$brightnessPercent percent"
                    },
                colors = TorchSliderDefaults.colors()
            )

            Text(
                text = "${(value * 100).toInt()}%",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Show "Tilt Active" indicator when slider is disabled
        if (!enabled) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "⟳ Tilt Active - tilt device to adjust brightness",
                style = MaterialTheme.typography.bodySmall,
                color = TorchOrange.copy(alpha = 0.7f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}

// ============================================================================
// Previews
// ============================================================================

@Preview(showBackground = true, backgroundColor = 0xFF1C1C1E)
@Composable
private fun BrightnessSliderEnabledPreview() {
    TableTorchTheme {
        BrightnessSlider(
            value = 0.85f,
            onValueChange = {},
            enabled = true
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1C1C1E)
@Composable
private fun BrightnessSliderDisabledPreview() {
    TableTorchTheme {
        BrightnessSlider(
            value = 0.5f,
            onValueChange = {},
            enabled = false
        )
    }
}
