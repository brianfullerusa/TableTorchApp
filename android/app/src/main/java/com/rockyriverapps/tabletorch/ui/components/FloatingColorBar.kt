package com.rockyriverapps.tabletorch.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.InfiniteRepeatableSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.rockyriverapps.tabletorch.R
import com.rockyriverapps.tabletorch.ui.theme.toComposeColor
import kotlinx.collections.immutable.ImmutableList
import kotlin.math.sin

/**
 * Compute an adaptive chrome color that tints the UI background to harmonize
 * with the active torch color. Extracts the hue from the torch color and
 * produces a dark, semi-transparent tint.
 */
fun adaptiveChromeColor(torchColor: Color): Color {
    val r = torchColor.red
    val g = torchColor.green
    val b = torchColor.blue

    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val delta = max - min

    // Calculate hue (0-360)
    val hue = when {
        delta == 0f -> 0f
        max == r -> 60f * (((g - b) / delta) % 6f)
        max == g -> 60f * (((b - r) / delta) + 2f)
        else -> 60f * (((r - g) / delta) + 4f)
    }.let { if (it < 0f) it + 360f else it }

    // Calculate saturation
    val lightness = (max + min) / 2f
    val saturation = if (delta == 0f) 0f else delta / (1f - kotlin.math.abs(2f * lightness - 1f))

    // Produce a dark, desaturated version of the hue
    return Color.hsl(
        hue = hue,
        saturation = (saturation * 0.3f).coerceIn(0f, 1f),
        lightness = 0.15f
    ).copy(alpha = 0.65f)
}

/**
 * Floating pill-shaped color bar that sits near the bottom of the screen.
 * Displays 6 circular color tokens with a settings gear icon on the right.
 * The background tints to harmonize with the active torch color.
 *
 * Matches the iOS 3.0 design: pill shape, adaptive chrome, selection ring,
 * and scale animation for selected/unselected states.
 */
@Composable
fun FloatingColorBar(
    colors: ImmutableList<Long>,
    selectedIndex: Int,
    torchColor: Color,
    onColorSelect: (Int) -> Unit,
    onSettingsClick: () -> Unit,
    isReducedMotionEnabled: Boolean = false,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val chromeColor = remember(torchColor) { adaptiveChromeColor(torchColor) }

    // Determine icon tint based on torch color luminance for contrast
    val iconTint = if (torchColor.luminance() > 0.5f) {
        Color.Black.copy(alpha = 0.7f)
    } else {
        Color.White.copy(alpha = 0.9f)
    }

    Box(
        modifier = modifier
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(28.dp),
                ambientColor = Color.Black.copy(alpha = 0.3f),
                spotColor = Color.Black.copy(alpha = 0.3f)
            )
            .clip(RoundedCornerShape(28.dp))
            .background(
                color = Color.Black.copy(alpha = 0.45f)
                    .compositeOver(chromeColor)
            )
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        // Flicker animation for the selected flame (disabled when reduced motion is on)
        val flickerPhase = if (isReducedMotionEnabled) {
            0f
        } else {
            val infiniteTransition = rememberInfiniteTransition(label = "flameFlicker")
            val animatedPhase by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 2f * Math.PI.toFloat(),
                animationSpec = InfiniteRepeatableSpec(
                    animation = tween(durationMillis = 1800, easing = LinearEasing)
                ),
                label = "flickerPhase"
            )
            animatedPhase
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            colors.forEachIndexed { index, colorValue ->
                val color = colorValue.toComposeColor()
                val isSelected = index == selectedIndex

                val scale by animateFloatAsState(
                    targetValue = if (isSelected) 1.15f else 0.88f,
                    animationSpec = spring(dampingRatio = 0.6f, stiffness = 300f),
                    label = "flameScale"
                )

                // Subtle Y-axis bob for the selected flame
                val flickerOffset = if (isSelected) {
                    (-1.5f * sin(flickerPhase)).dp
                } else {
                    0.dp
                }

                val description = if (isSelected) {
                    stringResource(R.string.color_button_selected, index + 1)
                } else {
                    stringResource(R.string.color_button_description, index + 1)
                }

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .offset(y = flickerOffset)
                        .scale(scale)
                        .clickable(
                            interactionSource = remember(index) { MutableInteractionSource() },
                            indication = null
                        ) {
                            view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                            onColorSelect(index)
                        }
                        .semantics {
                            contentDescription = description
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(
                            id = if (isSelected) R.drawable.ic_flame_filled else R.drawable.ic_flame_outline
                        ),
                        contentDescription = null,
                        tint = color.copy(alpha = if (isSelected) 1f else 0.7f),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            // Settings gear icon
            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = stringResource(R.string.settings),
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

/**
 * Composites this color over another color.
 * Simple alpha blending for overlay effect.
 */
private fun Color.compositeOver(background: Color): Color {
    val fgA = this.alpha
    val bgA = background.alpha
    val outA = fgA + bgA * (1f - fgA)
    if (outA == 0f) return Color.Transparent
    return Color(
        red = (this.red * fgA + background.red * bgA * (1f - fgA)) / outA,
        green = (this.green * fgA + background.green * bgA * (1f - fgA)) / outA,
        blue = (this.blue * fgA + background.blue * bgA * (1f - fgA)) / outA,
        alpha = outA
    )
}
