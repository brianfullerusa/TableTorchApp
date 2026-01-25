package com.rockyriverapps.tabletorch.ui.theme

import androidx.compose.runtime.Stable
import androidx.compose.ui.unit.dp

/**
 * Spacing and sizing tokens for TableTorch app.
 * Based on an 8dp grid system for consistent visual rhythm.
 * Marked as @Stable since all values are immutable Dp.
 */
@Stable
object TableTorchDimens {
    val SpacingXs = 4.dp
    val SpacingSm = 8.dp
    val SpacingMd = 16.dp
    val SpacingLg = 24.dp
    val SpacingXl = 32.dp
    val TouchTargetMin = 48.dp
    val CornerRadiusMd = 16.dp
    val CornerRadiusLg = 24.dp
}

/**
 * Standard icon sizes for TableTorch app.
 * Ensures consistent icon sizing across all screens.
 * Marked as @Stable since all values are immutable Dp.
 */
@Stable
object TorchIconSize {
    val Small = 24.dp
    val Medium = 32.dp
    val Large = 40.dp
}
