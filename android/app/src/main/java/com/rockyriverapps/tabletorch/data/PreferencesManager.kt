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
import androidx.datastore.preferences.preferencesDataStore
import com.rockyriverapps.tabletorch.ui.theme.TorchColors
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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

        // Color keys for each of the 6 torch colors
        fun colorKey(index: Int) = longPreferencesKey("torch_color_$index")
    }

    /**
     * StateFlow of current app settings, emits whenever any setting changes.
     * Uses SharingStarted.Eagerly since preferences are needed immediately on app start.
     */
    val settingsFlow: StateFlow<AppSettings> = appContext.dataStore.data.map { preferences ->
        val colors = (0 until 6).map { index ->
            preferences[PreferencesKeys.colorKey(index)]
                ?: TorchColors.defaultColorsImmutable[index]
        }.toImmutableList()

        AppSettings(
            defaultBrightness = preferences[PreferencesKeys.DEFAULT_BRIGHTNESS] ?: 0.85f,
            useDefaultBrightnessOnLaunch = preferences[PreferencesKeys.USE_DEFAULT_BRIGHTNESS_ON_LAUNCH] ?: true,
            selectedColors = colors,
            isAngleBasedBrightnessActive = preferences[PreferencesKeys.IS_ANGLE_BASED_BRIGHTNESS_ACTIVE] ?: false,
            lastSelectedColorIndex = preferences[PreferencesKeys.LAST_SELECTED_COLOR_INDEX] ?: 1,
            preventScreenLock = preferences[PreferencesKeys.PREVENT_SCREEN_LOCK] ?: true
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
                preferences[PreferencesKeys.LAST_SELECTED_COLOR_INDEX] = index.coerceIn(0, 5)
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
        if (index in 0..5) {
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
}
