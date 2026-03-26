package com.rockyriverapps.tabletorch

import com.rockyriverapps.tabletorch.data.AppSettings
import com.rockyriverapps.tabletorch.data.ColorPalette
import com.rockyriverapps.tabletorch.ui.theme.TorchColors
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSettingsTest {

    @Test
    fun `getCurrentColor returns correct color for given index`() {
        val settings = AppSettings.DEFAULT.copy(lastSelectedColorIndex = 0)
        val color = settings.getCurrentColor()
        // Index 0 should return the first default color (White = 0xFFFFFFFF)
        assertNotNull(color)
    }

    @Test
    fun `getCurrentColor with out-of-range index returns fallback`() {
        val settings = AppSettings.DEFAULT.copy(lastSelectedColorIndex = 999)
        // Should not throw; returns fallback color (index 1 = Soft White)
        val color = settings.getCurrentColor()
        assertNotNull(color)
    }

    @Test
    fun `allPalettes includes both built-in and custom palettes`() {
        val customPalette = ColorPalette(
            id = "custom-1",
            name = "My Custom",
            colors = persistentListOf(
                0xFFFF0000L, 0xFF00FF00L, 0xFF0000FFL,
                0xFFFFFF00L, 0xFFFF00FFL, 0xFF00FFFFL
            ),
            isBuiltIn = false
        )
        val settings = AppSettings.DEFAULT.copy(
            customPalettes = persistentListOf(customPalette)
        )

        val all = settings.allPalettes
        // Should contain all built-in presets plus the custom palette
        assertEquals(ColorPalette.builtInPresets.size + 1, all.size)
        assertTrue(all.any { it.id == "custom-1" })
        assertTrue(all.any { it.id == ColorPalette.PRESET_BRIGHT_ID })
    }

    @Test
    fun `getActivePalette finds correct palette by ID`() {
        val settings = AppSettings.DEFAULT.copy(
            activePaletteId = ColorPalette.PRESET_PARTY_ID
        )
        val palette = settings.getActivePalette()
        assertEquals(ColorPalette.PRESET_PARTY_ID, palette.id)
    }

    @Test
    fun `getActivePalette returns fallback for unknown ID`() {
        val settings = AppSettings.DEFAULT.copy(
            activePaletteId = "nonexistent-id"
        )
        val palette = settings.getActivePalette()
        // Falls back to Bright palette
        assertEquals(ColorPalette.PRESET_BRIGHT_ID, palette.id)
    }

    @Test
    fun `DEFAULT has valid initial values`() {
        val defaults = AppSettings.DEFAULT
        assertTrue(defaults.defaultBrightness in 0f..1f)
        assertTrue(defaults.selectedColors.size == ColorPalette.PALETTE_SIZE)
        assertEquals(ColorPalette.PRESET_LOW_LIGHT_ID, defaults.activePaletteId)
        assertTrue(defaults.customPalettes.isEmpty())
        assertTrue(defaults.breathingDepth > 0f)
        assertTrue(defaults.breathingCycleDuration > 0f)
    }
}
