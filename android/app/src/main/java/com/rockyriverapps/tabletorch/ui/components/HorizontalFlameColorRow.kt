package com.rockyriverapps.tabletorch.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rockyriverapps.tabletorch.R
import com.rockyriverapps.tabletorch.ui.theme.TableTorchDimens
import kotlinx.collections.immutable.ImmutableList

/**
 * Horizontal scrolling row of flame color pickers.
 * Each flame displays its assigned color and opens a color picker dialog when tapped.
 *
 * This component dramatically reduces vertical space compared to a grid layout
 * while maintaining full functionality and accessibility.
 */
@Composable
fun HorizontalFlameColorRow(
    colors: ImmutableList<Long>,
    onColorChange: (Int, Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedIndex by remember { mutableIntStateOf(-1) }
    var showDialog by remember { mutableStateOf(false) }

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = TableTorchDimens.SpacingSm),
        horizontalArrangement = Arrangement.spacedBy(TableTorchDimens.SpacingMd)
    ) {
        itemsIndexed(colors, key = { index, _ -> index }) { index, colorValue ->
            FlameColorItem(
                index = index,
                colorValue = colorValue,
                onClick = {
                    selectedIndex = index
                    showDialog = true
                }
            )
        }
    }

    // Show color picker dialog when a flame is tapped
    if (showDialog && selectedIndex >= 0 && selectedIndex < colors.size) {
        val currentColorValue = colors[selectedIndex]
        FullColorPickerDialog(
            initialColor = Color(currentColorValue.toULong()),
            onColorSelected = { newColor ->
                onColorChange(selectedIndex, newColor.value.toLong())
                showDialog = false
                selectedIndex = -1
            },
            onDismiss = {
                showDialog = false
                selectedIndex = -1
            }
        )
    }
}

/**
 * Individual flame item in the horizontal row.
 * Displays a colored flame icon with a number label below.
 * Meets 48dp minimum touch target requirement.
 */
@Composable
private fun FlameColorItem(
    index: Int,
    colorValue: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color = Color(colorValue.toULong())
    val flameNumber = index + 1
    val description = stringResource(R.string.select_color_description, "Torch $flameNumber")

    // Memoize interaction source and ripple to avoid allocations on recomposition
    val interactionSource = remember { MutableInteractionSource() }
    val rippleIndication = remember { ripple(bounded = false, radius = 32.dp) }

    Column(
        modifier = modifier
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = rippleIndication,
                role = Role.Button,
                onClick = onClick
            )
            .padding(TableTorchDimens.SpacingXs)
            .semantics {
                contentDescription = description
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Flame icon with color tint
        Box(
            modifier = Modifier.size(TableTorchDimens.TouchTargetMin),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_flame_filled),
                contentDescription = null, // Handled by parent semantics
                tint = color,
                modifier = Modifier.size(40.dp)
            )
        }

        // Number label below the flame
        Text(
            text = flameNumber.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}
