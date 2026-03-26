package com.rockyriverapps.tabletorch.ui.components

import androidx.compose.runtime.key
import com.rockyriverapps.tabletorch.data.displayName
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.rockyriverapps.tabletorch.R
import com.rockyriverapps.tabletorch.data.ColorPalette
import com.rockyriverapps.tabletorch.ui.theme.toComposeColor
import com.rockyriverapps.tabletorch.ui.theme.TableTorchDimens
import com.rockyriverapps.tabletorch.ui.theme.TorchSurface
import kotlinx.collections.immutable.ImmutableList

/**
 * Horizontally scrollable row of palette chips for quick switching.
 * Each chip shows the palette name and small color preview dots.
 * The active palette is highlighted with the primary accent color.
 *
 * @param palettes All available palettes (built-in + custom)
 * @param activePaletteId The ID of the currently active palette
 * @param onPaletteSelect Callback when a palette chip is tapped
 * @param modifier Modifier for the row container
 */
@Composable
fun PaletteChipRow(
    palettes: ImmutableList<ColorPalette>,
    activePaletteId: String,
    onPaletteSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(vertical = TableTorchDimens.SpacingXs),
        horizontalArrangement = Arrangement.spacedBy(TableTorchDimens.SpacingSm)
    ) {
        palettes.forEach { palette ->
            key(palette.id) {
                PaletteChip(
                    palette = palette,
                    isSelected = palette.id == activePaletteId,
                    onClick = { onPaletteSelect(palette.id) }
                )
            }
        }
    }
}

/**
 * Individual palette chip: a pill-shaped button displaying the palette name
 * and a row of small color dots representing the palette's colors.
 *
 * Selected chips receive an accent border and a slightly different background
 * to clearly indicate the active palette.
 */
@Composable
private fun PaletteChip(
    palette: ColorPalette,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val chipShape = RoundedCornerShape(20.dp)
    val selectedStateText = if (isSelected) {
        stringResource(R.string.a11y_state_selected)
    } else {
        stringResource(R.string.a11y_state_not_selected)
    }
    val localizedName = palette.displayName()
    val chipDescription = stringResource(R.string.a11y_palette_chip, localizedName)

    Row(
        modifier = modifier
            .height(48.dp)
            .clip(chipShape)
            .then(
                if (isSelected) {
                    Modifier
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                        .border(
                            width = 1.5.dp,
                            color = MaterialTheme.colorScheme.primary,
                            shape = chipShape
                        )
                } else {
                    Modifier
                        .background(TorchSurface)
                        .border(
                            width = 1.dp,
                            color = Color.White.copy(alpha = 0.15f),
                            shape = chipShape
                        )
                }
            )
            .clickable(role = Role.Button) { onClick() }
            .padding(horizontal = 12.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = chipDescription
                stateDescription = selectedStateText
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Palette name
        Text(
            text = localizedName,
            style = MaterialTheme.typography.labelMedium,
            color = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            },
            maxLines = 1
        )

        Spacer(modifier = Modifier.width(2.dp))

        // Color preview dots (show first 4 colors for space efficiency)
        Row(
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val previewCount = minOf(palette.colors.size, 4)
            for (i in 0 until previewCount) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(palette.colors[i].toComposeColor())
                        .border(
                            width = 0.5.dp,
                            color = Color.White.copy(alpha = 0.3f),
                            shape = CircleShape
                        )
                )
            }
        }
    }
}
