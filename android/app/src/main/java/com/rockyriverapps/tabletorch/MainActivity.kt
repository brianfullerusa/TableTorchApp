package com.rockyriverapps.tabletorch

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.compose.rememberNavController
import com.rockyriverapps.tabletorch.data.PreferencesManager
import com.rockyriverapps.tabletorch.navigation.TableTorchNavGraph
import com.rockyriverapps.tabletorch.sensors.TiltSensorManager
import com.rockyriverapps.tabletorch.ui.screens.SplashScreen
import com.rockyriverapps.tabletorch.ui.theme.TableTorchTheme
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: MainViewModel

    // Store original brightness to restore on destroy
    private var originalBrightness = -1f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize ViewModel with dependencies (PreferencesManager is a singleton)
        val preferencesManager = PreferencesManager.getInstance(applicationContext)
        val tiltSensorManager = TiltSensorManager(applicationContext)

        viewModel = ViewModelProvider(
            this,
            MainViewModel.Factory(preferencesManager, tiltSensorManager)
        )[MainViewModel::class.java]

        // Observe settings changes for screen lock prevention
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.settings.collectLatest { settings ->
                    if (settings.preventScreenLock) {
                        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                }
            }
        }

        // Observe brightness changes and apply to window
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.currentBrightness.collectLatest { brightness ->
                    applyBrightness(brightness)
                }
            }
        }

        setContent {
            TableTorchTheme {
                var showSplash by remember { mutableStateOf(true) }
                val navController = rememberNavController()

                // Tilt sensor lifecycle and brightness updates are handled in ViewModel init block
                // This keeps sensor logic out of Compose and properly scoped to ViewModel lifecycle

                Box(modifier = Modifier.fillMaxSize()) {
                    // Main content (always rendered underneath)
                    if (!showSplash) {
                        TableTorchNavGraph(
                            navController = navController,
                            settingsFlow = viewModel.settings,
                            brightnessFlow = viewModel.currentBrightness,
                            onBrightnessChange = { brightnessValue ->
                                viewModel.setBrightness(brightnessValue)
                            },
                            onColorSelect = { index ->
                                viewModel.updateLastSelectedColorIndex(index)
                            },
                            onDefaultBrightnessChange = { brightnessValue ->
                                viewModel.updateDefaultBrightness(brightnessValue)
                            },
                            onUseDefaultBrightnessOnLaunchChange = { enabled ->
                                viewModel.updateUseDefaultBrightnessOnLaunch(enabled)
                            },
                            onPreventScreenLockChange = { enabled ->
                                viewModel.updatePreventScreenLock(enabled)
                            },
                            onAngleBasedBrightnessChange = { enabled ->
                                viewModel.updateAngleBasedBrightness(enabled)
                            },
                            onColorChange = { index, colorValue ->
                                viewModel.updateColor(index, colorValue)
                            },
                            onRestoreDefaultColors = {
                                viewModel.restoreDefaultColors()
                            }
                        )
                    }

                    // Splash screen overlay
                    AnimatedVisibility(
                        visible = showSplash,
                        exit = fadeOut()
                    ) {
                        SplashScreen(
                            onTimeout = { showSplash = false }
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Store original brightness for restoration
        if (originalBrightness < 0) {
            originalBrightness = window.attributes.screenBrightness
            if (originalBrightness < 0) {
                originalBrightness = 0.85f // Default if system controlled
            }
        }
        // Sensor lifecycle is managed by ViewModel based on settings
        // Start sensor if coming back from background with feature enabled
        viewModel.startTiltSensorIfEnabled()
    }

    override fun onPause() {
        super.onPause()
        // Stop sensor when going to background to save battery
        viewModel.stopTiltSensor()
    }

    override fun onStop() {
        super.onStop()
        // Restore original brightness when app goes to background
        // This is more reliable than onDestroy which may not have a valid window
        if (originalBrightness >= 0) {
            applyBrightness(originalBrightness)
        } else {
            val params = window.attributes
            params.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            window.attributes = params
        }
    }

    /**
     * Apply brightness to the window.
     * This is the single point where brightness is applied to the UI.
     */
    private fun applyBrightness(brightness: Float) {
        val params = window.attributes
        params.screenBrightness = brightness.coerceIn(0.01f, 1f)
        window.attributes = params
    }
}
