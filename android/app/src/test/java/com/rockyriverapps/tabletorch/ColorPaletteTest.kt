package com.rockyriverapps.tabletorch

import com.rockyriverapps.tabletorch.data.ColorPalette
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ColorPaletteTest {

    private fun samplePalette(): ColorPalette = ColorPalette(
        id = "test-id",
        name = "Test Palette",
        colors = persistentListOf(
            0xFFFF0000L, 0xFF00FF00L, 0xFF0000FFL,
            0xFFFFFF00L, 0xFFFF00FFL, 0xFF00FFFFL
        ),
        isBuiltIn = false
    )

    @Test
    fun `toJson then fromJson returns equivalent object`() {
        val original = samplePalette()
        val json = original.toJson()
        val restored = ColorPalette.fromJson(json)

        assertNotNull(restored)
        assertEquals(original.id, restored!!.id)
        assertEquals(original.name, restored.name)
        assertEquals(original.colors, restored.colors)
        assertEquals(original.isBuiltIn, restored.isBuiltIn)
    }

    @Test
    fun `listToJson then listFromJson round-trip`() {
        val palettes = listOf(samplePalette(), samplePalette().copy(id = "second", name = "Second"))
        val jsonString = ColorPalette.listToJson(palettes)
        val restored = ColorPalette.listFromJson(jsonString)

        assertEquals(2, restored.size)
        assertEquals("test-id", restored[0].id)
        assertEquals("second", restored[1].id)
    }

    @Test
    fun `fromJson with malformed JSON returns null`() {
        val malformed = JSONObject().apply {
            put("garbage", "data")
        }
        val result = ColorPalette.fromJson(malformed)
        assertNull(result)
    }

    @Test
    fun `listFromJson with malformed JSON returns empty list`() {
        val result = ColorPalette.listFromJson("not valid json")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `all built-in presets have exactly PALETTE_SIZE colors`() {
        ColorPalette.builtInPresets.forEach { palette ->
            assertEquals(
                "Palette '${palette.name}' should have ${ColorPalette.PALETTE_SIZE} colors",
                ColorPalette.PALETTE_SIZE,
                palette.colors.size
            )
        }
    }

    @Test
    fun `built-in preset IDs are unique`() {
        val ids = ColorPalette.builtInPresets.map { it.id }
        assertEquals(
            "Built-in preset IDs must be unique",
            ids.size,
            ids.toSet().size
        )
    }
}
