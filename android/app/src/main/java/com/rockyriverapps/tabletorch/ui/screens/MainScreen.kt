package com.rockyriverapps.tabletorch.ui.screens

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.InfiniteRepeatableSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import android.provider.Settings
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.unit.dp
import com.rockyriverapps.tabletorch.R
import androidx.core.view.WindowCompat
import com.rockyriverapps.tabletorch.data.AppSettings
import com.rockyriverapps.tabletorch.models.ParticleShape
import com.rockyriverapps.tabletorch.ui.components.BrightnessIndicator
import com.rockyriverapps.tabletorch.ui.components.FloatingColorBar
import com.rockyriverapps.tabletorch.ui.components.SettingsSheetContent
import com.rockyriverapps.tabletorch.util.findActivity
import androidx.compose.runtime.Stable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sin

/**
 * Groups all callback parameters for [MainScreen] into a single stable data class
 * to reduce the number of parameters and improve recomposition stability.
 */
@Stable
data class MainScreenCallbacks(
    val onBrightnessChange: (Float) -> Unit,
    val onColorSelect: (Int) -> Unit,
    val onDefaultBrightnessChange: (Float) -> Unit,
    val onUseDefaultBrightnessOnLaunchChange: (Boolean) -> Unit,
    val onPreventScreenLockChange: (Boolean) -> Unit,
    val onAngleBasedBrightnessChange: (Boolean) -> Unit,
    val onColorChange: (Int, Long) -> Unit,
    val onRestoreDefaultColors: () -> Unit,
    val onPaletteSelect: (String) -> Unit,
    val onNavigateToPalettes: () -> Unit,
    val onShowQuickColorBarChange: (Boolean) -> Unit,
    val onAlwaysShowBrightnessChange: (Boolean) -> Unit,
    val onEnableBreathingAnimationChange: (Boolean) -> Unit,
    val onBreathingDepthChange: (Float) -> Unit,
    val onBreathingCycleDurationChange: (Float) -> Unit,
    val onEnableEmberParticlesChange: (Boolean) -> Unit,
    val onParticleShapeChange: (ParticleShape) -> Unit
)

/**
 * Main screen of the TableTorch app.
 *
 * Redesigned to match iOS 3.0: full-screen immersive torch color fills the entire
 * screen with no margins or rounded corners. A floating pill-shaped color bar hovers
 * near the bottom. A vertical brightness indicator appears on the right edge during
 * swipe gestures. A subtle radial glow emanates from the center.
 *
 * Gestures:
 * - Vertical swipe to adjust brightness (disabled when tilt is active)
 * - Double-tap to toggle max brightness
 *
 * Settings open as a ModalBottomSheet instead of navigating to a separate screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    settings: AppSettings,
    brightness: Float,
    callbacks: MainScreenCallbacks,
    openSettings: Boolean = false,
    onSettingsOpened: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val currentColor = settings.getCurrentColor()
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val navigationBarPadding = WindowInsets.navigationBars.asPaddingValues()

    // Keep a fresh reference to brightness inside pointerInput lambdas
    val currentBrightness by rememberUpdatedState(brightness)

    // Bottom sheet state for settings
    var showSettingsSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    // Re-open settings sheet when returning from Palettes screen
    LaunchedEffect(openSettings) {
        if (openSettings) {
            showSettingsSheet = true
            onSettingsOpened()
        }
    }

    // Brightness indicator visibility state
    var showBrightnessIndicator by remember { mutableStateOf(false) }

    // Double-tap state: store previous brightness before toggling to max
    var previousBrightness by remember { mutableFloatStateOf(brightness) }
    var isMaxBrightness by remember { mutableStateOf(false) }

    // Track brightness changes from drag/tilt so previousBrightness stays fresh
    LaunchedEffect(brightness) {
        if (!isMaxBrightness) {
            previousBrightness = brightness
        }
    }

    // Clear max brightness state when tilt mode activates
    LaunchedEffect(settings.isAngleBasedBrightnessActive) {
        if (settings.isAngleBasedBrightnessActive) {
            isMaxBrightness = false
        }
    }

    // Swipe gesture tracking
    var isDragging by remember { mutableStateOf(false) }
    var accumulatedDrag by remember { mutableFloatStateOf(0f) }

    // Haptic feedback thresholds for brightness (25%, 50%, 75%)
    val hapticThresholds = remember { listOf(0.25f, 0.50f, 0.75f) }
    var previousDragBrightness by remember { mutableFloatStateOf(brightness) }

    // Auto-hide brightness indicator after 1.2 seconds of inactivity
    LaunchedEffect(showBrightnessIndicator, isDragging) {
        if (showBrightnessIndicator && !isDragging && !settings.alwaysShowBrightness) {
            delay(1200L)
            showBrightnessIndicator = false
        }
    }

    // Keep indicator visible when always-show is enabled
    LaunchedEffect(settings.alwaysShowBrightness) {
        if (settings.alwaysShowBrightness) {
            showBrightnessIndicator = true
        }
    }

    // Dynamically adjust system bar icon color based on displayed color luminance
    DisposableEffect(currentColor) {
        val activity = view.context.findActivity()
        if (activity != null) {
            val window = activity.window
            val insetsController = WindowCompat.getInsetsController(window, view)
            val isLight = currentColor.luminance() > 0.5f
            insetsController.isAppearanceLightStatusBars = isLight
            insetsController.isAppearanceLightNavigationBars = isLight
        }
        onDispose { }
    }

    val brightnessPercent = (currentBrightness * 100).toInt()
    val torchScreenDesc = stringResource(R.string.a11y_torch_screen, brightnessPercent)

    // Accessibility action labels for brightness control
    val increaseBrightnessLabel = stringResource(R.string.a11y_action_increase_brightness)
    val decreaseBrightnessLabel = stringResource(R.string.a11y_action_decrease_brightness)
    val maxBrightnessLabel = stringResource(R.string.a11y_action_toggle_max_brightness)

    // Check system reduced motion preference
    val context = LocalContext.current
    val isReducedMotionEnabled = remember {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        ) == 0f
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .semantics {
                contentDescription = torchScreenDesc
                customActions = listOf(
                    CustomAccessibilityAction(increaseBrightnessLabel) {
                        if (!settings.isAngleBasedBrightnessActive) {
                            val newBrightness = (currentBrightness + 0.10f).coerceIn(0.01f, 1f)
                            callbacks.onBrightnessChange(newBrightness)
                            showBrightnessIndicator = true
                        }
                        true
                    },
                    CustomAccessibilityAction(decreaseBrightnessLabel) {
                        if (!settings.isAngleBasedBrightnessActive) {
                            val newBrightness = (currentBrightness - 0.10f).coerceIn(0.01f, 1f)
                            callbacks.onBrightnessChange(newBrightness)
                            showBrightnessIndicator = true
                        }
                        true
                    },
                    CustomAccessibilityAction(maxBrightnessLabel) {
                        if (!settings.isAngleBasedBrightnessActive) {
                            if (isMaxBrightness) {
                                callbacks.onBrightnessChange(previousBrightness)
                                isMaxBrightness = false
                            } else {
                                previousBrightness = currentBrightness
                                callbacks.onBrightnessChange(1f)
                                isMaxBrightness = true
                            }
                            showBrightnessIndicator = true
                        }
                        true
                    }
                )
            }
            // Full-screen immersive torch color - no margins, no rounded corners
            .background(currentColor)
            // Subtle radial glow effect emanating from center
            .drawBehind {
                val center = Offset(size.width / 2f, size.height / 2f)
                val maxRadius = maxOf(size.width, size.height) * 0.7f

                // Slightly lighter center, fading to transparent at edges
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.06f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = maxRadius
                    ),
                    center = center,
                    radius = maxRadius
                )
            }
            // Double-tap gesture to toggle max brightness
            .pointerInput(settings.isAngleBasedBrightnessActive) {
                detectTapGestures(
                    onDoubleTap = {
                        if (!settings.isAngleBasedBrightnessActive) {
                            view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                            if (isMaxBrightness) {
                                // Restore previous brightness
                                callbacks.onBrightnessChange(previousBrightness)
                                isMaxBrightness = false
                            } else {
                                // Save current and go to max
                                previousBrightness = currentBrightness
                                callbacks.onBrightnessChange(1f)
                                isMaxBrightness = true
                            }
                            showBrightnessIndicator = true
                        }
                    }
                )
            }
            // Vertical swipe gesture for brightness adjustment
            .pointerInput(settings.isAngleBasedBrightnessActive) {
                if (settings.isAngleBasedBrightnessActive) return@pointerInput

                detectDragGestures(
                    onDragStart = {
                        isDragging = true
                        accumulatedDrag = 0f
                        previousDragBrightness = currentBrightness
                        showBrightnessIndicator = true
                    },
                    onDragEnd = {
                        isDragging = false
                        isMaxBrightness = false
                    },
                    onDragCancel = {
                        isDragging = false
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        accumulatedDrag += dragAmount.y

                        // Require minimum 10dp drag distance before activating
                        if (abs(accumulatedDrag) < viewConfiguration.touchSlop) return@detectDragGestures

                        // Swipe up = increase brightness (negative Y), swipe down = decrease
                        val delta = -dragAmount.y / size.height
                        val newBrightness = (currentBrightness + delta).coerceIn(0.01f, 1f)
                        callbacks.onBrightnessChange(newBrightness)

                        // Haptic feedback when crossing 25%, 50%, or 75% thresholds
                        for (threshold in hapticThresholds) {
                            val crossed =
                                (previousDragBrightness < threshold && newBrightness >= threshold) ||
                                (previousDragBrightness > threshold && newBrightness <= threshold)
                            if (crossed) {
                                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                break
                            }
                        }

                        // Stronger haptic at min/max boundaries
                        if ((previousDragBrightness > 0.01f && newBrightness <= 0.01f) ||
                            (previousDragBrightness < 1f && newBrightness >= 1f)
                        ) {
                            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        }
                        previousDragBrightness = newBrightness
                    }
                )
            }
    ) {
        // Breathing animation overlay - dims the screen with a sine wave
        // Respects system reduced motion / "Remove animations" preference
        if (settings.enableBreathingAnimation) {
            if (isReducedMotionEnabled) {
                // Static dim at half breathing depth when animations are disabled
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = settings.breathingDepth / 2f))
                        .clearAndSetSemantics {}
                )
            } else {
                // key() restarts the transition when cycle duration changes
                key(settings.breathingCycleDuration) {
                    val infiniteTransition = rememberInfiniteTransition(label = "breathing")
                    val breathingPhase by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 2f * Math.PI.toFloat(),
                        animationSpec = InfiniteRepeatableSpec(
                            animation = tween(
                                durationMillis = (settings.breathingCycleDuration * 1000).toInt(),
                                easing = LinearEasing
                            )
                        ),
                        label = "breathing_phase"
                    )
                    // Sine wave oscillation: 0 at peak brightness, depth at minimum
                    val dimAmount = settings.breathingDepth * ((1f - sin(breathingPhase)) / 2f)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = dimAmount))
                            .clearAndSetSemantics {}
                    )
                }
            }
        }

        // Brightness indicator - right edge, vertically centered
        BrightnessIndicator(
            brightness = brightness,
            torchColor = currentColor,
            visible = showBrightnessIndicator || settings.alwaysShowBrightness,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 4.dp)
        )

        // Floating color bar - bottom center
        AnimatedVisibility(
            visible = settings.showQuickColorBar,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(
                    bottom = 20.dp + navigationBarPadding.calculateBottomPadding()
                )
        ) {
            FloatingColorBar(
                colors = settings.selectedColors,
                selectedIndex = settings.lastSelectedColorIndex,
                torchColor = currentColor,
                onColorSelect = callbacks.onColorSelect,
                onSettingsClick = { showSettingsSheet = true },
                isReducedMotionEnabled = isReducedMotionEnabled
            )
        }

        // Standalone settings icon - visible only when Quick Color Bar is hidden
        AnimatedVisibility(
            visible = !settings.showQuickColorBar,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(
                    end = 16.dp,
                    bottom = 20.dp + navigationBarPadding.calculateBottomPadding()
                )
        ) {
            IconButton(
                onClick = { showSettingsSheet = true },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.35f))
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = stringResource(R.string.settings_title),
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }

    // Settings bottom sheet
    if (showSettingsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSettingsSheet = false },
            sheetState = sheetState,
            containerColor = Color(0xFF1C1C1E).copy(alpha = 0.95f),
            scrimColor = Color.Black.copy(alpha = 0.4f),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            dragHandle = {
                // Drag indicator matching iOS style with 48dp touch target
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 36.dp, height = 5.dp)
                            .clip(RoundedCornerShape(2.5.dp))
                            .background(Color.White.copy(alpha = 0.3f))
                    )
                }
            }
        ) {
            SettingsSheetContent(
                settings = settings,
                onDefaultBrightnessChange = callbacks.onDefaultBrightnessChange,
                onUseDefaultBrightnessOnLaunchChange = callbacks.onUseDefaultBrightnessOnLaunchChange,
                onPreventScreenLockChange = callbacks.onPreventScreenLockChange,
                onAngleBasedBrightnessChange = callbacks.onAngleBasedBrightnessChange,
                onColorChange = callbacks.onColorChange,
                onColorSelect = callbacks.onColorSelect,
                onRestoreDefaultColors = callbacks.onRestoreDefaultColors,
                onPaletteSelect = callbacks.onPaletteSelect,
                onNavigateToPalettes = {
                    // Dismiss sheet first, then navigate to palettes screen
                    scope.launch {
                        sheetState.hide()
                        showSettingsSheet = false
                        callbacks.onNavigateToPalettes()
                    }
                },
                onShowQuickColorBarChange = callbacks.onShowQuickColorBarChange,
                onAlwaysShowBrightnessChange = callbacks.onAlwaysShowBrightnessChange,
                onEnableBreathingAnimationChange = callbacks.onEnableBreathingAnimationChange,
                onBreathingDepthChange = callbacks.onBreathingDepthChange,
                onBreathingCycleDurationChange = callbacks.onBreathingCycleDurationChange,
                onEnableEmberParticlesChange = callbacks.onEnableEmberParticlesChange,
                onParticleShapeChange = callbacks.onParticleShapeChange,
                onDismiss = {
                    scope.launch {
                        sheetState.hide()
                        showSettingsSheet = false
                    }
                }
            )
        }
    }
}
