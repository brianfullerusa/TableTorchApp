package com.rockyriverapps.tabletorch.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rockyriverapps.tabletorch.R

/**
 * Vertical brightness indicator positioned on the right edge of the screen.
 * Shows a sun/moon icon, percentage text, and a narrow fill bar.
 *
 * The indicator adapts its colors to contrast with the torch color using luminance.
 * It fades in/out based on visibility state (shown during brightness adjustment
 * or when always-visible is enabled).
 *
 * Matches the iOS 3.0 design: vertical layout, adaptive contrast, monospace digits.
 */
@Composable
fun BrightnessIndicator(
    brightness: Float,
    torchColor: Color,
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    // Choose contrasting colors based on torch luminance
    val isLightBackground = torchColor.luminance() > 0.5f
    val indicatorColor = if (isLightBackground) {
        Color.Black.copy(alpha = 0.7f)
    } else {
        Color.White.copy(alpha = 0.85f)
    }
    val trackColor = if (isLightBackground) {
        Color.Black.copy(alpha = 0.15f)
    } else {
        Color.White.copy(alpha = 0.15f)
    }
    val fillColor = if (isLightBackground) {
        Color.Black.copy(alpha = 0.5f)
    } else {
        Color.White.copy(alpha = 0.6f)
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier.fillMaxHeight()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom,
            modifier = Modifier
                .fillMaxHeight()
                .padding(end = 12.dp, top = 60.dp, bottom = 140.dp)
        ) {
            // Sun/moon icon based on brightness level
            Icon(
                imageVector = brightnessIcon(brightness),
                contentDescription = stringResource(R.string.brightness_indicator),
                tint = indicatorColor,
                modifier = Modifier
                    .width(22.dp)
                    .height(22.dp)
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Percentage text with monospace digits
            Text(
                text = "${(brightness * 100).toInt()}%",
                color = indicatorColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center,
                lineHeight = 13.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Vertical fill bar (4dp wide) - fills remaining vertical space
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .weight(1f)
                    .clip(RoundedCornerShape(2.dp))
                    .background(trackColor),
                contentAlignment = Alignment.BottomCenter
            ) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight(fraction = brightness.coerceIn(0f, 1f))
                        .clip(RoundedCornerShape(2.dp))
                        .background(fillColor)
                )
            }
        }
    }
}

/**
 * Returns the appropriate Material icon based on the brightness level.
 * - < 25%: NightsStay (moon)
 * - 25-50%: BrightnessLow
 * - 50-75%: BrightnessMedium
 * - 75-100%: BrightnessHigh
 */
private fun brightnessIcon(brightness: Float): ImageVector {
    return when {
        brightness < 0.25f -> Icons.Filled.NightsStay
        brightness < 0.50f -> Icons.Filled.BrightnessLow
        brightness < 0.75f -> Icons.Filled.BrightnessMedium
        else -> Icons.Filled.BrightnessHigh
    }
}
