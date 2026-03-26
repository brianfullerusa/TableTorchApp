package com.rockyriverapps.tabletorch

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.compose.rememberNavController
import com.rockyriverapps.tabletorch.data.AppSettings
import com.rockyriverapps.tabletorch.data.PreferencesManager
import com.rockyriverapps.tabletorch.navigation.TableTorchNavGraph
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

        // Initialize ViewModel with dependencies
        // TiltSensorManager is created lazily inside the Factory to avoid leaking
        // a new instance on every Activity recreation (ViewModelProvider caches the VM).
        val preferencesManager = PreferencesManager.getInstance(applicationContext)

        viewModel = ViewModelProvider(
            this,
            MainViewModel.Factory(preferencesManager, applicationContext)
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
                // var showSplash by remember { mutableStateOf(true) }
                val navController = rememberNavController()

                Box(modifier = Modifier.fillMaxSize()) {
                    // Main content (always rendered underneath)
                    // if (!showSplash) {
                        TableTorchNavGraph(
                            navController = navController,
                            viewModel = viewModel
                        )
                    // }

                    // Splash screen overlay
                    // AnimatedVisibility(
                    //     visible = showSplash,
                    //     exit = fadeOut()
                    // ) {
                    //     SplashScreen(
                    //         onTimeout = { showSplash = false }
                    //     )
                    // }
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
                originalBrightness = AppSettings.DEFAULT.defaultBrightness
            }
        }
        // Sensor lifecycle is managed by ViewModel based on settings
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
