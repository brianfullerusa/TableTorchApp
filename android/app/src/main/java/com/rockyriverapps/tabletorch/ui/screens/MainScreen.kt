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
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.rockyriverapps.tabletorch.data.AppSettings
import com.rockyriverapps.tabletorch.models.ParticleShape
import com.rockyriverapps.tabletorch.ui.components.BrightnessIndicator
import com.rockyriverapps.tabletorch.ui.components.EmberParticleView
import com.rockyriverapps.tabletorch.ui.components.FloatingColorBar
import com.rockyriverapps.tabletorch.ui.components.SettingsSheetContent
import com.rockyriverapps.tabletorch.util.findActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sin

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
    openSettings: Boolean = false,
    onBrightnessChange: (Float) -> Unit,
    onColorSelect: (Int) -> Unit,
    onDefaultBrightnessChange: (Float) -> Unit,
    onUseDefaultBrightnessOnLaunchChange: (Boolean) -> Unit,
    onPreventScreenLockChange: (Boolean) -> Unit,
    onAngleBasedBrightnessChange: (Boolean) -> Unit,
    onColorChange: (Int, Long) -> Unit,
    onRestoreDefaultColors: () -> Unit,
    onPaletteSelect: (String) -> Unit,
    onNavigateToPalettes: () -> Unit,
    onShowQuickColorBarChange: (Boolean) -> Unit,
    onAlwaysShowBrightnessChange: (Boolean) -> Unit,
    onEnableBreathingAnimationChange: (Boolean) -> Unit,
    onBreathingDepthChange: (Float) -> Unit,
    onBreathingCycleDurationChange: (Float) -> Unit,
    onEnableEmberParticlesChange: (Boolean) -> Unit,
    onParticleShapeChange: (ParticleShape) -> Unit,
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
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Re-open settings sheet when returning from Palettes screen
    LaunchedEffect(openSettings) {
        if (openSettings) {
            showSettingsSheet = true
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

    Box(
        modifier = modifier
            .fillMaxSize()
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
                                onBrightnessChange(previousBrightness)
                                isMaxBrightness = false
                            } else {
                                // Save current and go to max
                                previousBrightness = currentBrightness
                                onBrightnessChange(1f)
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
                        onBrightnessChange(newBrightness)

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
        if (settings.enableBreathingAnimation) {
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
                )
            }
        }

        // Ember particle overlay
        if (settings.enableEmberParticles) {
            EmberParticleView(
                selectedColors = settings.selectedColors,
                selectedColorIndex = settings.lastSelectedColorIndex,
                particleShape = settings.particleShape,
                modifier = Modifier.fillMaxSize()
            )
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
                onColorSelect = onColorSelect,
                onSettingsClick = { showSettingsSheet = true }
            )
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
                // Drag indicator matching iOS style
                Box(
                    modifier = Modifier
                        .padding(top = 8.dp, bottom = 4.dp)
                        .size(width = 36.dp, height = 5.dp)
                        .clip(RoundedCornerShape(2.5.dp))
                        .background(Color.White.copy(alpha = 0.3f))
                )
            }
        ) {
            SettingsSheetContent(
                settings = settings,
                onDefaultBrightnessChange = onDefaultBrightnessChange,
                onUseDefaultBrightnessOnLaunchChange = onUseDefaultBrightnessOnLaunchChange,
                onPreventScreenLockChange = onPreventScreenLockChange,
                onAngleBasedBrightnessChange = onAngleBasedBrightnessChange,
                onColorChange = onColorChange,
                onRestoreDefaultColors = onRestoreDefaultColors,
                onPaletteSelect = onPaletteSelect,
                onNavigateToPalettes = {
                    // Dismiss sheet first, then navigate to palettes screen
                    scope.launch {
                        sheetState.hide()
                        showSettingsSheet = false
                        onNavigateToPalettes()
                    }
                },
                onShowQuickColorBarChange = onShowQuickColorBarChange,
                onAlwaysShowBrightnessChange = onAlwaysShowBrightnessChange,
                onEnableBreathingAnimationChange = onEnableBreathingAnimationChange,
                onBreathingDepthChange = onBreathingDepthChange,
                onBreathingCycleDurationChange = onBreathingCycleDurationChange,
                onEnableEmberParticlesChange = onEnableEmberParticlesChange,
                onParticleShapeChange = onParticleShapeChange,
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
