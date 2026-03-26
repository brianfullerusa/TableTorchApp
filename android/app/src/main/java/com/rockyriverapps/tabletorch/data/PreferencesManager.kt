package com.rockyriverapps.tabletorch.data

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.rockyriverapps.tabletorch.models.ParticleShape
import com.rockyriverapps.tabletorch.ui.theme.TorchColors
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import androidx.datastore.preferences.core.emptyPreferences
import java.io.IOException

// Extension property for DataStore - singleton by design
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "table_torch_settings")

/**
 * Manages app preferences using DataStore.
 * Provides reactive StateFlow of settings and methods to update them.
 *
 * This is a singleton class to prevent memory leaks from multiple instances
 * being created on configuration changes. Use [getInstance] to obtain the instance.
 */
class PreferencesManager private constructor(context: Context) {

    // Use application context to prevent activity leaks
    private val appContext = context.applicationContext

    // Application-scoped coroutine scope - lives for the entire app lifecycle
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        private const val TAG = "PreferencesManager"

        @Volatile
        private var INSTANCE: PreferencesManager? = null

        /**
         * Get the singleton instance of PreferencesManager.
         * Thread-safe initialization using double-checked locking.
         *
         * @param context Any context - will be converted to application context internally
         * @return The singleton PreferencesManager instance
         */
        fun getInstance(context: Context): PreferencesManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PreferencesManager(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }

    private object PreferencesKeys {
        val DEFAULT_BRIGHTNESS = floatPreferencesKey("default_brightness")
        val USE_DEFAULT_BRIGHTNESS_ON_LAUNCH = booleanPreferencesKey("use_default_brightness_on_launch")
        val IS_ANGLE_BASED_BRIGHTNESS_ACTIVE = booleanPreferencesKey("is_angle_based_brightness_active")
        val LAST_SELECTED_COLOR_INDEX = intPreferencesKey("last_selected_color_index")
        val PREVENT_SCREEN_LOCK = booleanPreferencesKey("prevent_screen_lock")
        val SHOW_QUICK_COLOR_BAR = booleanPreferencesKey("show_quick_color_bar")
        val ALWAYS_SHOW_BRIGHTNESS = booleanPreferencesKey("always_show_brightness")

        // Visual effects keys
        val ENABLE_BREATHING_ANIMATION = booleanPreferencesKey("enable_breathing_animation")
        val BREATHING_DEPTH = floatPreferencesKey("breathing_depth")
        val BREATHING_CYCLE_DURATION = floatPreferencesKey("breathing_cycle_duration")
        val ENABLE_EMBER_PARTICLES = booleanPreferencesKey("enable_ember_particles")
        val PARTICLE_SHAPE = stringPreferencesKey("particle_shape")

        // Color keys for each of the 6 torch colors
        fun colorKey(index: Int) = longPreferencesKey("torch_color_$index")

        // Palette keys
        val ACTIVE_PALETTE_ID = stringPreferencesKey("active_palette_id")
        val CUSTOM_PALETTES_JSON = stringPreferencesKey("custom_palettes_json")
    }

    /**
     * StateFlow of current app settings, emits whenever any setting changes.
     * Uses SharingStarted.Eagerly since preferences are needed immediately on app start.
     */
    val settingsFlow: StateFlow<AppSettings> = appContext.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                Log.e(TAG, "Error reading preferences", exception)
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
        val colors = (0 until ColorPalette.PALETTE_SIZE).map { index ->
            preferences[PreferencesKeys.colorKey(index)]
                ?: TorchColors.defaultColorsImmutable[index]
        }.toImmutableList()

        val customPalettesJson = preferences[PreferencesKeys.CUSTOM_PALETTES_JSON]
        val customPalettes = ColorPalette.listFromJson(customPalettesJson)

        val defaults = AppSettings.DEFAULT
        AppSettings(
            defaultBrightness = preferences[PreferencesKeys.DEFAULT_BRIGHTNESS] ?: defaults.defaultBrightness,
            useDefaultBrightnessOnLaunch = preferences[PreferencesKeys.USE_DEFAULT_BRIGHTNESS_ON_LAUNCH] ?: defaults.useDefaultBrightnessOnLaunch,
            selectedColors = colors,
            isAngleBasedBrightnessActive = preferences[PreferencesKeys.IS_ANGLE_BASED_BRIGHTNESS_ACTIVE] ?: defaults.isAngleBasedBrightnessActive,
            lastSelectedColorIndex = preferences[PreferencesKeys.LAST_SELECTED_COLOR_INDEX] ?: defaults.lastSelectedColorIndex,
            preventScreenLock = preferences[PreferencesKeys.PREVENT_SCREEN_LOCK] ?: defaults.preventScreenLock,
            showQuickColorBar = preferences[PreferencesKeys.SHOW_QUICK_COLOR_BAR] ?: defaults.showQuickColorBar,
            alwaysShowBrightness = preferences[PreferencesKeys.ALWAYS_SHOW_BRIGHTNESS] ?: defaults.alwaysShowBrightness,
            enableBreathingAnimation = preferences[PreferencesKeys.ENABLE_BREATHING_ANIMATION] ?: defaults.enableBreathingAnimation,
            breathingDepth = preferences[PreferencesKeys.BREATHING_DEPTH] ?: defaults.breathingDepth,
            breathingCycleDuration = preferences[PreferencesKeys.BREATHING_CYCLE_DURATION] ?: defaults.breathingCycleDuration,
            enableEmberParticles = preferences[PreferencesKeys.ENABLE_EMBER_PARTICLES] ?: defaults.enableEmberParticles,
            particleShape = try {
                ParticleShape.valueOf(preferences[PreferencesKeys.PARTICLE_SHAPE] ?: defaults.particleShape.name)
            } catch (_: IllegalArgumentException) {
                defaults.particleShape
            },
            activePaletteId = preferences[PreferencesKeys.ACTIVE_PALETTE_ID] ?: defaults.activePaletteId,
            customPalettes = customPalettes
        )
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = AppSettings.DEFAULT
    )

    /**
     * Update the default brightness value
     */
    suspend fun updateDefaultBrightness(brightness: Float) {
        try {
            appContext.dataStore.edit { preferences ->
                preferences[PreferencesKeys.DEFAULT_BRIGHTNESS] = brightness.coerceIn(0f, 1f)
            }
        } catch (e: IOException) {
            Log.e(TAG, "Failed to save default brightness preference", e)
        }
    }

    /**
     * Update whether to use default brightness on app launch
     */
    suspend fun updateUseDefaultBrightnessOnLaunch(enabled: Boolean) {
        try {
            appContext.dataStore.edit { preferences ->
                preferences[PreferencesKeys.USE_DEFAULT_BRIGHTNESS_ON_LAUNCH] = enabled
            }
        } catch (e: IOException) {
            Log.e(TAG, "Failed to save use default brightness on launch preference", e)
        }
    }

    /**
     * Update the angle-based brightness toggle
     */
    suspend fun updateAngleBasedBrightness(enabled: Boolean) {
        try {
            appContext.dataStore.edit { preferences ->
                preferences[PreferencesKeys.IS_ANGLE_BASED_BRIGHTNESS_ACTIVE] = enabled
            }
        } catch (e: IOException) {
            Log.e(TAG, "Failed to save angle-based brightness preference", e)
        }
    }

    /**
     * Update the last selected color index
     */
    suspend fun updateLastSelectedColorIndex(index: Int) {
        try {
            appContext.dataStore.edit { preferences ->
                preferences[PreferencesKeys.LAST_SELECTED_COLOR_INDEX] = index.coerceIn(0, ColorPalette.PALETTE_SIZE - 1)
            }
        } catch (e: IOException) {
            Log.e(TAG, "Failed to save last selected color index preference", e)
        }
    }

    /**
     * Update the prevent screen lock toggle
     */
    suspend fun updatePreventScreenLock(enabled: Boolean) {
        try {
            appContext.dataStore.edit { preferences ->
                preferences[PreferencesKeys.PREVENT_SCREEN_LOCK] = enabled
            }
        } catch (e: IOException) {
            Log.e(TAG, "Failed to save prevent screen lock preference", e)
        }
    }

    /**
     * Update a specific torch color
     */
    suspend fun updateColor(index: Int, colorValue: Long) {
        if (index in 0 until ColorPalette.PALETTE_SIZE) {
            try {
                appContext.dataStore.edit { preferences ->
                    preferences[PreferencesKeys.colorKey(index)] = colorValue
                }
            } catch (e: IOException) {
                Log.e(TAG, "Failed to save color preference for index $index", e)
            }
        }
    }

    /**
     * Restore all colors to their defaults
     */
    suspend fun restoreDefaultColors() {
        try {
            appContext.dataStore.edit { preferences ->
                TorchColors.defaultColors.forEachIndexed { index, colorValue ->
                    preferences[PreferencesKeys.colorKey(index)] = colorValue
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Failed to restore default colors", e)
        }
    }

    /**
     * Update the show quick color bar toggle
     */
    suspend fun updateShowQuickColorBar(enabled: Boolean) {
        try {
            appContext.dataStore.edit { preferences ->
                preferences[PreferencesKeys.SHOW_QUICK_COLOR_BAR] = enabled
            }
        } catch (e: IOException) {
            Log.e(TAG, "Failed to save show quick color bar preference", e)
        }
    }

    /**
     * Update the always show brightness indicator toggle
     */
    suspend fun updateAlwaysShowBrightness(enabled: Boolean) {
        try {
            appContext.dataStore.edit { preferences ->
                preferences[PreferencesKeys.ALWAYS_SHOW_BRIGHTNESS] = enabled
            }
        } catch (e: IOException) {
            Log.e(TAG, "Failed to save always show brightness preference", e)
        }
    }

    // ========================================================================
    // Visual Effects Operations
    // ========================================================================

    suspend fun updateEnableBreathingAnimation(enabled: Boolean) {
        try {
            appContext.dataStore.edit { preferences ->
                preferences[PreferencesKeys.ENABLE_BREATHING_ANIMATION] = enabled
            }
        } catch (e: IOException) {
            Log.e(TAG, "Failed to save breathing animation preference", e)
        }
    }

    suspend fun updateBreathingDepth(depth: Float) {
        try {
            appContext.dataStore.edit { preferences ->
                preferences[PreferencesKeys.BREATHING_DEPTH] = depth.coerceIn(0.02f, 0.40f)
            }
        } catch (e: IOException) {
            Log.e(TAG, "Failed to save breathing depth preference", e)
        }
    }

    suspend fun updateBreathingCycleDuration(duration: Float) {
        try {
            appContext.dataStore.edit { preferences ->
                preferences[PreferencesKeys.BREATHING_CYCLE_DURATION] = duration.coerceIn(1f, 10f)
            }
        } catch (e: IOException) {
            Log.e(TAG, "Failed to save breathing cycle duration preference", e)
        }
    }

    suspend fun updateEnableEmberParticles(enabled: Boolean) {
        try {
            appContext.dataStore.edit { preferences ->
                preferences[PreferencesKeys.ENABLE_EMBER_PARTICLES] = enabled
            }
        } catch (e: IOException) {
            Log.e(TAG, "Failed to save ember particles preference", e)
        }
    }

    suspend fun updateParticleShape(shape: ParticleShape) {
        try {
            appContext.dataStore.edit { preferences ->
                preferences[PreferencesKeys.PARTICLE_SHAPE] = shape.name
            }
        } catch (e: IOException) {
            Log.e(TAG, "Failed to save particle shape preference", e)
        }
    }

    // ========================================================================
    // Palette Operations
    // ========================================================================

    /**
     * Update the active palette ID and apply the palette colors to the torch color slots.
     * This keeps the existing per-color DataStore keys in sync with the active palette.
     */
    suspend fun switchPalette(paletteId: String, paletteColors: List<Long>) {
        try {
            appContext.dataStore.edit { preferences ->
                preferences[PreferencesKeys.ACTIVE_PALETTE_ID] = paletteId
                // Apply palette colors to the individual color slots
                paletteColors.forEachIndexed { index, colorValue ->
                    if (index < ColorPalette.PALETTE_SIZE) {
                        preferences[PreferencesKeys.colorKey(index)] = colorValue
                    }
                }
                // Reset color index to 0 when switching palettes to avoid
                // stale selection pointing at a very different color
                preferences[PreferencesKeys.LAST_SELECTED_COLOR_INDEX] = 0
            }
        } catch (e: IOException) {
            Log.e(TAG, "Failed to switch palette", e)
        }
    }

    /**
     * Save the list of custom palettes as a JSON string.
     */
    suspend fun saveCustomPalettes(palettes: List<ColorPalette>) {
        try {
            val json = ColorPalette.listToJson(palettes)
            appContext.dataStore.edit { preferences ->
                preferences[PreferencesKeys.CUSTOM_PALETTES_JSON] = json
            }
        } catch (e: IOException) {
            Log.e(TAG, "Failed to save custom palettes", e)
        }
    }

    /**
     * Update the active palette ID only (without changing colors).
     * Used when the current colors already match the palette.
     */
    suspend fun updateActivePaletteId(paletteId: String) {
        try {
            appContext.dataStore.edit { preferences ->
                preferences[PreferencesKeys.ACTIVE_PALETTE_ID] = paletteId
            }
        } catch (e: IOException) {
            Log.e(TAG, "Failed to update active palette ID", e)
        }
    }
}
