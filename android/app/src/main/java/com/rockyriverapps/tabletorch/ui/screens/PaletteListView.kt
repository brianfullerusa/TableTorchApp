package com.rockyriverapps.tabletorch.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rockyriverapps.tabletorch.R
import com.rockyriverapps.tabletorch.data.ColorPalette
import com.rockyriverapps.tabletorch.ui.theme.TableTorchDimens
import com.rockyriverapps.tabletorch.ui.theme.TorchBackground
import com.rockyriverapps.tabletorch.ui.theme.toComposeColor
import com.rockyriverapps.tabletorch.ui.theme.TorchOrange
import com.rockyriverapps.tabletorch.ui.theme.TorchSurface
import kotlinx.collections.immutable.ImmutableList

/**
 * Full-screen palette list for managing all palettes.
 * Shows built-in presets (non-deletable) and custom palettes with full
 * management capabilities: select, rename, duplicate, and delete.
 *
 * @param palettes All available palettes (built-in + custom)
 * @param activePaletteId The ID of the currently active palette
 * @param onNavigateBack Navigate back to settings
 * @param onPaletteSelect Switch to the tapped palette
 * @param onCreatePalette Create a new custom palette from current colors
 * @param onDuplicatePalette Duplicate an existing palette
 * @param onRenamePalette Rename a custom palette
 * @param onDeletePalette Delete a custom palette
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaletteListScreen(
    palettes: ImmutableList<ColorPalette>,
    activePaletteId: String,
    currentColors: ImmutableList<Long>,
    onNavigateBack: () -> Unit,
    onPaletteSelect: (String) -> Unit,
    onCreatePalette: (String, List<Long>) -> Unit,
    onDuplicatePalette: (ColorPalette) -> Unit,
    onRenamePalette: (String, String) -> Unit,
    onDeletePalette: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Dialog state for creating new palettes
    var showCreateDialog by remember { mutableStateOf(false) }
    // Dialog state for renaming palettes
    var renamingPalette by remember { mutableStateOf<ColorPalette?>(null) }
    // Dialog state for confirming deletion
    var deletingPalette by remember { mutableStateOf<ColorPalette?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.palettes_title),
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
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = TorchOrange,
                contentColor = Color.Black
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.create_palette)
                )
            }
        },
        containerColor = TorchBackground
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(
                horizontal = TableTorchDimens.SpacingMd,
                vertical = TableTorchDimens.SpacingSm
            ),
            verticalArrangement = Arrangement.spacedBy(TableTorchDimens.SpacingSm)
        ) {
            items(
                items = palettes,
                key = { it.id }
            ) { palette ->
                PaletteListItem(
                    palette = palette,
                    isActive = palette.id == activePaletteId,
                    onSelect = { onPaletteSelect(palette.id) },
                    onDuplicate = { onDuplicatePalette(palette) },
                    onRename = if (!palette.isBuiltIn) {
                        { renamingPalette = palette }
                    } else null,
                    onDelete = if (!palette.isBuiltIn) {
                        { deletingPalette = palette }
                    } else null
                )
            }

            // Bottom spacing for FAB
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    // Create palette dialog
    if (showCreateDialog) {
        CreatePaletteDialog(
            currentColors = currentColors,
            onConfirm = { name ->
                onCreatePalette(name, currentColors)
                showCreateDialog = false
            },
            onDismiss = { showCreateDialog = false }
        )
    }

    // Rename palette dialog
    renamingPalette?.let { palette ->
        RenamePaletteDialog(
            currentName = palette.name,
            onConfirm = { newName ->
                onRenamePalette(palette.id, newName)
                renamingPalette = null
            },
            onDismiss = { renamingPalette = null }
        )
    }

    // Delete confirmation dialog
    deletingPalette?.let { palette ->
        DeletePaletteDialog(
            paletteName = palette.name,
            onConfirm = {
                onDeletePalette(palette.id)
                deletingPalette = null
            },
            onDismiss = { deletingPalette = null }
        )
    }
}

/**
 * Individual palette item in the list.
 * Shows palette name, color swatches, and action buttons.
 * The active palette is highlighted with a border accent.
 */
@Composable
private fun PaletteListItem(
    palette: ColorPalette,
    isActive: Boolean,
    onSelect: () -> Unit,
    onDuplicate: () -> Unit,
    onRename: (() -> Unit)?,
    onDelete: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val activeSuffix = stringResource(R.string.palette_active_suffix)
    val borderColor by animateColorAsState(
        targetValue = if (isActive) {
            MaterialTheme.colorScheme.primary
        } else {
            Color.Transparent
        },
        label = "palette_border"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (isActive) {
                    Modifier.border(
                        width = 2.dp,
                        color = borderColor,
                        shape = RoundedCornerShape(TableTorchDimens.CornerRadiusMd)
                    )
                } else {
                    Modifier
                }
            )
            .clip(RoundedCornerShape(TableTorchDimens.CornerRadiusMd))
            .clickable(role = Role.Button) { onSelect() }
            .semantics {
                contentDescription = "${palette.name} palette${if (isActive) activeSuffix else ""}"
            },
        shape = RoundedCornerShape(TableTorchDimens.CornerRadiusMd),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
            } else {
                TorchSurface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(TableTorchDimens.SpacingMd)
        ) {
            // Header row: name + built-in indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(TableTorchDimens.SpacingSm)
                ) {
                    Text(
                        text = palette.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                        color = if (isActive) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                    if (palette.isBuiltIn) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = stringResource(R.string.built_in_palette),
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }

                if (isActive) {
                    Text(
                        text = stringResource(R.string.active),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(TableTorchDimens.SpacingSm))

            // Color swatches row - shows all 6 colors
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                palette.colors.forEach { colorValue ->
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(colorValue.toComposeColor())
                            .border(
                                width = 1.dp,
                                color = Color.White.copy(alpha = 0.2f),
                                shape = CircleShape
                            )
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Action buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    // Duplicate (available for all palettes)
                    IconButton(
                        onClick = onDuplicate,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = stringResource(R.string.duplicate_palette),
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Rename (custom palettes only)
                    if (onRename != null) {
                        IconButton(
                            onClick = onRename,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = stringResource(R.string.rename_palette),
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Delete (custom palettes only)
                    if (onDelete != null) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(R.string.delete_palette),
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// Dialogs
// ============================================================================

/**
 * Dialog for creating a new custom palette from the current colors.
 */
@Composable
private fun CreatePaletteDialog(
    currentColors: ImmutableList<Long>,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.create_palette),
                color = MaterialTheme.colorScheme.onBackground
            )
        },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.create_palette_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(TableTorchDimens.SpacingMd))

                // Color preview
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    currentColors.forEach { colorValue ->
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(colorValue.toComposeColor())
                                .border(
                                    width = 1.dp,
                                    color = Color.White.copy(alpha = 0.2f),
                                    shape = CircleShape
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(TableTorchDimens.SpacingMd))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(30) },
                    label = {
                        Text(stringResource(R.string.palette_name))
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                        cursorColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name.trim().ifBlank { "My Palette" }) },
                enabled = true
            ) {
                Text(
                    text = stringResource(R.string.create),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.cancel),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

/**
 * Dialog for renaming a custom palette.
 */
@Composable
private fun RenamePaletteDialog(
    currentName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.rename_palette),
                color = MaterialTheme.colorScheme.onBackground
            )
        },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(30) },
                label = {
                    Text(stringResource(R.string.palette_name))
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                    cursorColor = MaterialTheme.colorScheme.primary,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name.trim().ifBlank { currentName }) }
            ) {
                Text(
                    text = stringResource(R.string.rename),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.cancel),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

/**
 * Confirmation dialog before deleting a custom palette.
 */
@Composable
private fun DeletePaletteDialog(
    paletteName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.delete_palette),
                color = MaterialTheme.colorScheme.onBackground
            )
        },
        text = {
            Text(
                text = stringResource(R.string.delete_palette_confirmation, paletteName),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(R.string.delete),
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.cancel),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}
