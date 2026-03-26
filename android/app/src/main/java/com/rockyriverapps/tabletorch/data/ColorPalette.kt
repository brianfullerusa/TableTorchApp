package com.rockyriverapps.tabletorch.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.res.stringResource
import com.rockyriverapps.tabletorch.R
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Represents a color palette containing 6 torch colors.
 * Palettes can be built-in presets or user-created custom palettes.
 *
 * @param id Unique identifier for the palette, used for tracking the active palette
 * @param name Display name shown in the palette chip row
 * @param colors Exactly 6 color values stored as ARGB Long values
 * @param isBuiltIn True for preset palettes that cannot be deleted or renamed
 */
@Immutable
data class ColorPalette(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val colors: ImmutableList<Long>,
    val isBuiltIn: Boolean = false
) {
    /**
     * Serialize this palette to a JSON object for DataStore persistence.
     */
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put(KEY_ID, id)
            put(KEY_NAME, name)
            put(KEY_COLORS, JSONArray().apply {
                colors.forEach { put(it) }
            })
            put(KEY_IS_BUILT_IN, isBuiltIn)
        }
    }

    companion object {
        /** Number of color slots in each palette */
        const val PALETTE_SIZE = 6

        private const val KEY_ID = "id"
        private const val KEY_NAME = "name"
        private const val KEY_COLORS = "colors"
        private const val KEY_IS_BUILT_IN = "isBuiltIn"

        // ====================================================================
        // Built-in Preset IDs (stable across app launches)
        // ====================================================================

        const val PRESET_LOW_LIGHT_ID = "preset-low-light"
        const val PRESET_BRIGHT_ID = "preset-bright"
        const val PRESET_PARTY_ID = "preset-party"

        // ====================================================================
        // Built-in Presets (matching iOS)
        // ====================================================================

        /** Low Light: warm reds and ambers for intimate settings */
        val LowLight = ColorPalette(
            id = PRESET_LOW_LIGHT_ID,
            name = "Low Light",
            colors = persistentListOf(
                0xFFCC0000L,   // Deep red
                0xFFE64A19L,   // Warm red
                0xFFFF8F00L,   // Bright orange
                0xFFFFB74DL,   // Golden amber
                0xFFFFCC80L,   // Soft peach
                0xFFFFF3E0L    // Warm white
            ),
            isBuiltIn = true
        )

        /** Bright: full spectrum for general use — uses TorchColors as single source of truth */
        val Bright = ColorPalette(
            id = PRESET_BRIGHT_ID,
            name = "Bright",
            colors = com.rockyriverapps.tabletorch.ui.theme.TorchColors.defaultColorsImmutable,
            isBuiltIn = true
        )

        /** Party: neon magentas, purples, and blues for festive atmospheres */
        val Party = ColorPalette(
            id = PRESET_PARTY_ID,
            name = "Party",
            colors = persistentListOf(
                0xFFFF1493L,   // Hot magenta
                0xFFFF69B4L,   // Neon pink
                0xFF9B30FFL,   // Electric purple
                0xFF7B1FA2L,   // Deep violet
                0xFF2979FFL,   // Electric blue
                0xFFBA68C8L    // Orchid
            ),
            isBuiltIn = true
        )

        /** All built-in presets in display order */
        val builtInPresets: ImmutableList<ColorPalette> = persistentListOf(
            LowLight,
            Bright,
            Party
        )

        /** Default active palette ID */
        const val DEFAULT_ACTIVE_PALETTE_ID = PRESET_BRIGHT_ID

        /**
         * Deserialize a palette from a JSON object.
         * Returns null if the JSON is malformed.
         */
        fun fromJson(json: JSONObject): ColorPalette? {
            return try {
                val colorsArray = json.getJSONArray(KEY_COLORS)
                val rawColors = (0 until colorsArray.length()).map { index ->
                    colorsArray.getLong(index)
                }

                // Ensure exactly PALETTE_SIZE colors: pad with defaults or truncate
                val colors = when {
                    rawColors.size == PALETTE_SIZE -> rawColors
                    rawColors.size > PALETTE_SIZE -> rawColors.take(PALETTE_SIZE)
                    else -> rawColors + List(PALETTE_SIZE - rawColors.size) {
                        com.rockyriverapps.tabletorch.ui.theme.TorchColors.defaultColorsImmutable
                            .getOrElse(rawColors.size + it) { 0xFFFFFFFFL }
                    }
                }.toImmutableList()

                ColorPalette(
                    id = json.getString(KEY_ID),
                    name = json.getString(KEY_NAME),
                    colors = colors,
                    isBuiltIn = json.optBoolean(KEY_IS_BUILT_IN, false)
                )
            } catch (e: Exception) {
                null
            }
        }

        /**
         * Serialize a list of palettes to a JSON string for DataStore storage.
         */
        fun listToJson(palettes: List<ColorPalette>): String {
            val jsonArray = JSONArray()
            palettes.forEach { jsonArray.put(it.toJson()) }
            return jsonArray.toString()
        }

        /**
         * Deserialize a list of palettes from a JSON string.
         * Returns an empty list if the string is null, empty, or malformed.
         */
        fun listFromJson(jsonString: String?): ImmutableList<ColorPalette> {
            if (jsonString.isNullOrBlank()) return persistentListOf()
            return try {
                val jsonArray = JSONArray(jsonString)
                (0 until jsonArray.length()).mapNotNull { index ->
                    fromJson(jsonArray.getJSONObject(index))
                }.toImmutableList()
            } catch (e: Exception) {
                persistentListOf()
            }
        }
    }
}

/**
 * Resolve the display name for a palette, using localized string resources
 * for built-in palettes and the stored name for custom palettes.
 */
@Composable
fun ColorPalette.displayName(): String = when (id) {
    ColorPalette.PRESET_LOW_LIGHT_ID -> stringResource(R.string.palette_low_light)
    ColorPalette.PRESET_BRIGHT_ID -> stringResource(R.string.palette_bright)
    ColorPalette.PRESET_PARTY_ID -> stringResource(R.string.palette_party)
    else -> name
}
