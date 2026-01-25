package com.rockyriverapps.tabletorch.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.rockyriverapps.tabletorch.R
import com.rockyriverapps.tabletorch.ui.theme.TableTorchTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * A flame-shaped color button for selecting torch colors.
 * Shows filled flame when selected, outline when not.
 * Includes haptic feedback on click and subtle border for unselected buttons.
 */
@Composable
fun ColorButton(
    color: Color,
    isSelected: Boolean,
    index: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val description = if (isSelected) {
        stringResource(R.string.color_button_selected, index + 1)
    } else {
        stringResource(R.string.color_button_description, index + 1)
    }

    Box(
        modifier = modifier
            .size(48.dp)
            .then(
                if (!isSelected) {
                    Modifier.border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.3f),
                        shape = CircleShape
                    )
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        IconButton(
            onClick = {
                view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                onClick()
            },
            modifier = Modifier
                .size(48.dp)
                .semantics {
                    contentDescription = description
                }
        ) {
            Icon(
                painter = painterResource(
                    id = if (isSelected) R.drawable.ic_flame_filled else R.drawable.ic_flame_outline
                ),
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(40.dp)
            )
        }
    }
}

/**
 * Row of 6 color buttons for the main screen.
 */
@Composable
fun ColorButtonsRow(
    colors: ImmutableList<Long>,
    selectedIndex: Int,
    onColorSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.layout.Row(
        modifier = modifier,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
    ) {
        colors.forEachIndexed { index, colorValue ->
            ColorButton(
                color = Color(colorValue.toULong()).copy(
                    alpha = if (index == selectedIndex) 1f else 0.7f
                ),
                isSelected = index == selectedIndex,
                index = index,
                onClick = { onColorSelect(index) }
            )
        }
    }
}

// ============================================================================
// Previews
// ============================================================================

@Preview(showBackground = true, backgroundColor = 0xFF1C1C1E)
@Composable
private fun ColorButtonSelectedPreview() {
    TableTorchTheme {
        ColorButton(
            color = Color(0xFFFFC896),
            isSelected = true,
            index = 1,
            onClick = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1C1C1E)
@Composable
private fun ColorButtonUnselectedPreview() {
    TableTorchTheme {
        ColorButton(
            color = Color(0xFF4682B4),
            isSelected = false,
            index = 3,
            onClick = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1C1C1E)
@Composable
private fun ColorButtonsRowPreview() {
    TableTorchTheme {
        ColorButtonsRow(
            colors = persistentListOf(
                0xFFFFFFFF,
                0xFFFFC896,
                0xFF98FF98,
                0xFF4682B4,
                0xFFFF0000,
                0xFF800000
            ),
            selectedIndex = 1,
            onColorSelect = {}
        )
    }
}
