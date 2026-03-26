package com.rockyriverapps.tabletorch.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.rockyriverapps.tabletorch.MainViewModel
import com.rockyriverapps.tabletorch.ui.screens.MainScreen
import com.rockyriverapps.tabletorch.ui.screens.PaletteListScreen

/**
 * Navigation routes for the app.
 * Settings is presented as a bottom sheet from MainScreen.
 * Palettes has its own full-screen route for management.
 */
object Routes {
    const val MAIN = "main"
    const val PALETTES = "palettes"
}

/**
 * Main navigation graph for the app.
 * Accepts the ViewModel directly to avoid prop-drilling dozens of callbacks.
 * Screens remain decoupled — they accept individual callbacks wired here.
 */
@Composable
fun TableTorchNavGraph(
    navController: NavHostController,
    viewModel: MainViewModel
) {
    val settings by viewModel.settings.collectAsState()
    val brightness by viewModel.currentBrightness.collectAsState()

    NavHost(
        navController = navController,
        startDestination = Routes.MAIN,
        enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn() },
        exitTransition = { slideOutHorizontally(targetOffsetX = { -it / 4 }) + fadeOut() },
        popEnterTransition = { slideInHorizontally(initialOffsetX = { -it / 4 }) + fadeIn() },
        popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut() }
    ) {
        composable(Routes.MAIN) { backStackEntry ->
            // Track whether to re-open settings when returning from Palettes
            var openSettings by remember { mutableStateOf(false) }
            LaunchedEffect(backStackEntry) {
                val flag = backStackEntry.savedStateHandle.get<Boolean>("open_settings") == true
                if (flag) {
                    backStackEntry.savedStateHandle.remove<Boolean>("open_settings")
                    openSettings = true
                }
            }

            MainScreen(
                settings = settings,
                brightness = brightness,
                openSettings = openSettings,
                onBrightnessChange = viewModel::setBrightness,
                onColorSelect = viewModel::updateLastSelectedColorIndex,
                onDefaultBrightnessChange = viewModel::updateDefaultBrightness,
                onUseDefaultBrightnessOnLaunchChange = viewModel::updateUseDefaultBrightnessOnLaunch,
                onPreventScreenLockChange = viewModel::updatePreventScreenLock,
                onAngleBasedBrightnessChange = viewModel::updateAngleBasedBrightness,
                onColorChange = viewModel::updateColor,
                onRestoreDefaultColors = viewModel::restoreDefaultColors,
                onPaletteSelect = viewModel::switchPalette,
                onNavigateToPalettes = {
                    navController.navigate(Routes.PALETTES)
                },
                onShowQuickColorBarChange = viewModel::updateShowQuickColorBar,
                onAlwaysShowBrightnessChange = viewModel::updateAlwaysShowBrightness,
                onEnableBreathingAnimationChange = viewModel::updateEnableBreathingAnimation,
                onBreathingDepthChange = viewModel::updateBreathingDepth,
                onBreathingCycleDurationChange = viewModel::updateBreathingCycleDuration,
                onEnableEmberParticlesChange = viewModel::updateEnableEmberParticles,
                onParticleShapeChange = viewModel::updateParticleShape
            )
        }

        composable(Routes.PALETTES) {
            PaletteListScreen(
                palettes = remember(settings.customPalettes) { settings.getAllPalettes() },
                activePaletteId = settings.activePaletteId,
                currentColors = settings.selectedColors,
                onNavigateBack = {
                    navController.previousBackStackEntry?.savedStateHandle?.set("open_settings", true)
                    navController.popBackStack()
                },
                onPaletteSelect = viewModel::switchPalette,
                onCreatePalette = viewModel::createCustomPalette,
                onDuplicatePalette = viewModel::duplicatePalette,
                onRenamePalette = viewModel::renamePalette,
                onDeletePalette = viewModel::deletePalette
            )
        }
    }
}
