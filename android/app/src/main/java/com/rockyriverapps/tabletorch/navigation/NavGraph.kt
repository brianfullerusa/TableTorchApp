package com.rockyriverapps.tabletorch.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.rockyriverapps.tabletorch.data.AppSettings
import com.rockyriverapps.tabletorch.ui.screens.MainScreen
import com.rockyriverapps.tabletorch.ui.screens.SettingsScreen
import kotlinx.coroutines.flow.StateFlow

/**
 * Navigation routes for the app
 */
object Routes {
    const val MAIN = "main"
    const val SETTINGS = "settings"
}

/**
 * Main navigation graph for the app.
 */
@Composable
fun TableTorchNavGraph(
    navController: NavHostController,
    settingsFlow: StateFlow<AppSettings>,
    brightnessFlow: StateFlow<Float>,
    onBrightnessChange: (Float) -> Unit,
    onColorSelect: (Int) -> Unit,
    onDefaultBrightnessChange: (Float) -> Unit,
    onUseDefaultBrightnessOnLaunchChange: (Boolean) -> Unit,
    onPreventScreenLockChange: (Boolean) -> Unit,
    onAngleBasedBrightnessChange: (Boolean) -> Unit,
    onColorChange: (Int, Long) -> Unit,
    onRestoreDefaultColors: () -> Unit
) {
    val settings by settingsFlow.collectAsState()
    val brightness by brightnessFlow.collectAsState()

    NavHost(
        navController = navController,
        startDestination = Routes.MAIN,
        enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn() },
        exitTransition = { slideOutHorizontally(targetOffsetX = { -it / 4 }) + fadeOut() },
        popEnterTransition = { slideInHorizontally(initialOffsetX = { -it / 4 }) + fadeIn() },
        popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut() }
    ) {
        composable(Routes.MAIN) {
            MainScreen(
                settings = settings,
                brightness = brightness,
                onBrightnessChange = onBrightnessChange,
                onColorSelect = onColorSelect,
                onNavigateToSettings = {
                    navController.navigate(Routes.SETTINGS)
                }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                settings = settings,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onDefaultBrightnessChange = onDefaultBrightnessChange,
                onUseDefaultBrightnessOnLaunchChange = onUseDefaultBrightnessOnLaunchChange,
                onPreventScreenLockChange = onPreventScreenLockChange,
                onAngleBasedBrightnessChange = onAngleBasedBrightnessChange,
                onColorChange = onColorChange,
                onRestoreDefaultColors = onRestoreDefaultColors
            )
        }
    }
}
