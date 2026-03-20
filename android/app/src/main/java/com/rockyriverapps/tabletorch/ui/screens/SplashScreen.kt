package com.rockyriverapps.tabletorch.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rockyriverapps.tabletorch.R
import com.rockyriverapps.tabletorch.ui.theme.CinzelFont
import com.rockyriverapps.tabletorch.ui.theme.TableTorchTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Animated splash screen matching the iOS 3.2-second timeline.
 *
 * Phase timeline:
 * - Phase 1 (0.0-0.4s): Dark anticipation - pure black
 * - Phase 2 (0.4-0.6s): White-hot spark point appears and flashes
 * - Phase 3 (0.6-1.8s): Spark particles burst outward, flame grows from base
 * - Phase 4 (1.8-2.5s): Title "Table Torch" illuminates with warm golden gradient
 * - Phase 5 (2.5-3.2s): Settle and hold
 *
 * The flame uses layered sine waves at irrational frequencies for organic,
 * non-repeating flickering motion.
 */
@Composable
fun SplashScreen(
    onTimeout: () -> Unit
) {
    // Elapsed time in seconds, driven by frame callbacks
    var elapsedTime by remember { mutableFloatStateOf(0f) }
    var startFadeOut by remember { mutableStateOf(false) }
    var hasFinished by remember { mutableStateOf(false) }

    // Fade out animation for the entire screen when completing
    val screenAlpha by animateFloatAsState(
        targetValue = if (startFadeOut) 0f else 1f,
        animationSpec = tween(durationMillis = 300),
        label = "splash_fade"
    )

    // Frame-based time tracking for smooth animation
    LaunchedEffect(Unit) {
        val startNanos = withFrameNanos { it }
        while (!hasFinished) {
            withFrameNanos { frameNanos ->
                elapsedTime = (frameNanos - startNanos) / 1_000_000_000f
                if (elapsedTime >= TOTAL_DURATION && !startFadeOut) {
                    startFadeOut = true
                }
                if (elapsedTime >= TOTAL_DURATION + 0.3f && !hasFinished) {
                    hasFinished = true
                    onTimeout()
                }
            }
        }
    }

    val tapToSkipDescription = stringResource(R.string.tap_to_skip)

    // Compute phase-dependent values
    val sparkAlpha = computeSparkAlpha(elapsedTime)
    val flameScale = computeFlameScale(elapsedTime)
    val titleAlpha = computeTitleAlpha(elapsedTime)
    val sparkParticles = remember { generateSparkParticles() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .alpha(screenAlpha)
            .semantics {
                contentDescription = tapToSkipDescription
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                if (!hasFinished) {
                    hasFinished = true
                    onTimeout()
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Flame canvas with spark particles
            Box(
                modifier = Modifier.size(200.dp),
                contentAlignment = Alignment.Center
            ) {
                FlameCanvas(
                    elapsedTime = elapsedTime,
                    flameScale = flameScale,
                    sparkAlpha = sparkAlpha,
                    sparkParticles = sparkParticles,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Title with golden gradient, animated alpha
            Text(
                text = stringResource(R.string.splash_title),
                fontFamily = CinzelFont,
                fontWeight = FontWeight.Bold,
                fontSize = 34.sp,
                letterSpacing = 0.8.sp,
                color = Color.White, // Base color; gradient applied via style
                modifier = Modifier.alpha(titleAlpha),
                style = androidx.compose.ui.text.TextStyle(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFFFD54F), // Warm golden
                            Color(0xFFFFA726), // Amber
                            Color(0xFFFF8F00), // Deep amber
                            Color(0xFFFFD54F)  // Warm golden
                        )
                    ),
                    fontFamily = CinzelFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 34.sp,
                    letterSpacing = 0.8.sp
                )
            )
        }
    }
}

// ============================================================================
// Phase Timeline Constants
// ============================================================================

private const val TOTAL_DURATION = 3.2f

private const val PHASE_DARK_END = 0.4f
private const val PHASE_SPARK_END = 0.6f
private const val PHASE_BURST_END = 1.8f
private const val PHASE_TITLE_END = 2.5f

// Irrational frequency multipliers for organic, non-repeating flame motion
private val FREQ_1 = PI.toFloat()          // 3.14159...
private val FREQ_2 = sqrt(2f)             // 1.41421...
private val FREQ_3 = sqrt(3f)             // 1.73205...
private val FREQ_4 = sqrt(5f)             // 2.23607...
private val FREQ_5 = (1f + sqrt(5f)) / 2f // Golden ratio 1.61803...

// ============================================================================
// Phase Computation Functions
// ============================================================================

/**
 * Compute spark point alpha. Bright flash during Phase 2, then fades.
 */
private fun computeSparkAlpha(time: Float): Float {
    return when {
        time < PHASE_DARK_END -> 0f
        time < PHASE_SPARK_END -> {
            // Quick flash up and slight fade
            val t = (time - PHASE_DARK_END) / (PHASE_SPARK_END - PHASE_DARK_END)
            if (t < 0.5f) t * 2f else 1f
        }
        time < PHASE_BURST_END -> {
            // Spark fades as flame takes over
            val t = (time - PHASE_SPARK_END) / (PHASE_BURST_END - PHASE_SPARK_END)
            (1f - t * 0.7f).coerceIn(0f, 1f)
        }
        else -> 0.3f // Residual glow
    }
}

/**
 * Compute flame scale. Grows from 0 during Phase 3, settles at full size.
 */
private fun computeFlameScale(time: Float): Float {
    return when {
        time < PHASE_SPARK_END -> 0f
        time < PHASE_BURST_END -> {
            // Flame grows with ease-out curve
            val t = (time - PHASE_SPARK_END) / (PHASE_BURST_END - PHASE_SPARK_END)
            easeOutCubic(t)
        }
        else -> 1f
    }
}

/**
 * Compute title alpha. Fades in during Phase 4.
 */
private fun computeTitleAlpha(time: Float): Float {
    return when {
        time < PHASE_BURST_END -> 0f
        time < PHASE_TITLE_END -> {
            val t = (time - PHASE_BURST_END) / (PHASE_TITLE_END - PHASE_BURST_END)
            easeOutCubic(t)
        }
        else -> 1f
    }
}

private fun easeOutCubic(t: Float): Float {
    val t1 = 1f - t
    return 1f - t1 * t1 * t1
}

// ============================================================================
// Spark Particles
// ============================================================================

/**
 * Data for a single spark particle that bursts outward from the flame center.
 */
private data class SparkParticle(
    val angle: Float,      // Direction in radians
    val speed: Float,      // Pixels per second
    val startDelay: Float, // Delay before this particle appears (seconds from Phase 2 start)
    val lifetime: Float,   // How long the particle lives (seconds)
    val size: Float        // Radius in dp-ish units
)

/**
 * Generate 6 spark particles with varied angles and speeds for visual interest.
 */
private fun generateSparkParticles(): List<SparkParticle> {
    val count = 6
    return List(count) { i ->
        val baseAngle = (2f * PI.toFloat() * i / count) + (PI.toFloat() / 6f) // Offset for visual balance
        SparkParticle(
            angle = baseAngle + (i * 0.15f), // Slight variation
            speed = 180f + (i % 3) * 40f,
            startDelay = i * 0.03f,
            lifetime = 0.6f + (i % 2) * 0.2f,
            size = 3f + (i % 3) * 1.5f
        )
    }
}

// ============================================================================
// FlameCanvas Composable
// ============================================================================

/**
 * Canvas-based flame rendering with layered gradients and organic motion.
 *
 * The flame consists of three layers:
 * - Outer: deep orange with wide shape
 * - Middle: golden-amber, slightly narrower
 * - Inner: white/cream core, narrow
 *
 * Each layer oscillates independently using sine waves with irrational
 * frequency multipliers, producing organic non-repeating motion.
 */
@Composable
private fun FlameCanvas(
    elapsedTime: Float,
    flameScale: Float,
    sparkAlpha: Float,
    sparkParticles: List<SparkParticle>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val centerX = size.width / 2f
        val centerY = size.height * 0.55f // Flame center slightly above canvas center

        // Draw spark particles during Phase 2-3
        if (elapsedTime >= PHASE_DARK_END && elapsedTime < PHASE_BURST_END + 0.3f) {
            drawSparkParticles(
                centerX = centerX,
                centerY = centerY,
                elapsedTime = elapsedTime,
                particles = sparkParticles
            )
        }

        // Draw white-hot spark point during Phase 2
        if (sparkAlpha > 0f && elapsedTime < PHASE_BURST_END) {
            val sparkSize = if (elapsedTime < PHASE_SPARK_END) {
                val t = (elapsedTime - PHASE_DARK_END) / (PHASE_SPARK_END - PHASE_DARK_END)
                8f + t * 12f
            } else {
                val t = (elapsedTime - PHASE_SPARK_END) / (PHASE_BURST_END - PHASE_SPARK_END)
                20f - t * 14f
            }

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White,
                        Color(0xFFFFF8E1).copy(alpha = sparkAlpha * 0.8f),
                        Color(0xFFFFB74D).copy(alpha = sparkAlpha * 0.3f),
                        Color.Transparent
                    ),
                    center = Offset(centerX, centerY),
                    radius = sparkSize
                ),
                center = Offset(centerX, centerY),
                radius = sparkSize
            )
        }

        // Draw flame layers when scale > 0
        if (flameScale > 0f) {
            val flameHeight = size.height * 0.55f * flameScale
            val flameWidth = size.width * 0.35f * flameScale

            // Compute organic flicker offsets using layered sine waves
            val flickerX = computeFlickerX(elapsedTime)
            val flickerY = computeFlickerY(elapsedTime)
            val scaleFlicker = computeScaleFlicker(elapsedTime)

            val adjustedWidth = flameWidth * (1f + scaleFlicker * 0.08f)
            val adjustedHeight = flameHeight * (1f + scaleFlicker * 0.05f)

            // Outer flame layer (deep orange)
            drawFlameLayer(
                centerX = centerX + flickerX * 1.2f,
                baseY = centerY + adjustedHeight * 0.4f,
                width = adjustedWidth * 1.1f,
                height = adjustedHeight,
                colors = listOf(
                    Color(0xFFFF6D00).copy(alpha = 0.0f),
                    Color(0xFFFF6D00).copy(alpha = 0.4f),
                    Color(0xFFFF8F00).copy(alpha = 0.7f),
                    Color(0xFFFFB74D).copy(alpha = 0.3f)
                )
            )

            // Middle flame layer (golden-amber)
            drawFlameLayer(
                centerX = centerX + flickerX * 0.8f,
                baseY = centerY + adjustedHeight * 0.35f,
                width = adjustedWidth * 0.75f,
                height = adjustedHeight * 0.85f,
                colors = listOf(
                    Color(0xFFFF8F00).copy(alpha = 0.0f),
                    Color(0xFFFFA726).copy(alpha = 0.6f),
                    Color(0xFFFFCA28).copy(alpha = 0.9f),
                    Color(0xFFFFD54F).copy(alpha = 0.5f)
                )
            )

            // Inner flame layer (white/cream core)
            drawFlameLayer(
                centerX = centerX + flickerX * 0.4f,
                baseY = centerY + adjustedHeight * 0.3f,
                width = adjustedWidth * 0.4f,
                height = adjustedHeight * 0.6f,
                colors = listOf(
                    Color(0xFFFFF8E1).copy(alpha = 0.0f),
                    Color(0xFFFFF8E1).copy(alpha = 0.7f),
                    Color.White.copy(alpha = 0.95f),
                    Color(0xFFFFF8E1).copy(alpha = 0.4f)
                )
            )

            // Bright core glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.6f * flameScale),
                        Color(0xFFFFD54F).copy(alpha = 0.2f * flameScale),
                        Color.Transparent
                    ),
                    center = Offset(centerX + flickerX * 0.3f, centerY + flickerY * 0.2f),
                    radius = adjustedWidth * 0.5f
                ),
                center = Offset(centerX + flickerX * 0.3f, centerY + flickerY * 0.2f),
                radius = adjustedWidth * 0.5f
            )
        }
    }
}

// ============================================================================
// Flame Drawing Helpers
// ============================================================================

/**
 * Draw a single flame layer as a teardrop/candle shape using a Path with
 * cubic bezier curves and a vertical gradient brush.
 */
private fun DrawScope.drawFlameLayer(
    centerX: Float,
    baseY: Float,
    width: Float,
    height: Float,
    colors: List<Color>
) {
    val tipY = baseY - height
    val path = Path().apply {
        // Start at the base center
        moveTo(centerX, baseY)

        // Left side curve - bulges out then tapers to tip
        cubicTo(
            centerX - width * 1.1f, baseY - height * 0.2f,  // Control point 1: wide at base
            centerX - width * 0.6f, baseY - height * 0.7f,  // Control point 2: narrows
            centerX, tipY                                      // End at tip
        )

        // Right side curve - mirror of left side
        cubicTo(
            centerX + width * 0.6f, baseY - height * 0.7f,  // Control point 1: narrows
            centerX + width * 1.1f, baseY - height * 0.2f,  // Control point 2: wide at base
            centerX, baseY                                    // End back at base
        )

        close()
    }

    drawPath(
        path = path,
        brush = Brush.verticalGradient(
            colors = colors,
            startY = baseY,
            endY = tipY
        )
    )
}

/**
 * Draw spark particles bursting outward from the center point.
 */
private fun DrawScope.drawSparkParticles(
    centerX: Float,
    centerY: Float,
    elapsedTime: Float,
    particles: List<SparkParticle>
) {
    val burstStart = PHASE_DARK_END // Particles start at Phase 2
    particles.forEach { particle ->
        val particleTime = elapsedTime - burstStart - particle.startDelay
        if (particleTime > 0f && particleTime < particle.lifetime) {
            val progress = particleTime / particle.lifetime
            val distance = particle.speed * particleTime

            // Ease out the movement
            val easedDistance = distance * easeOutCubic(progress.coerceIn(0f, 1f))

            val px = centerX + cos(particle.angle) * easedDistance
            val py = centerY + sin(particle.angle) * easedDistance

            // Fade out as particle ages
            val alpha = (1f - progress).coerceIn(0f, 1f)

            // Draw spark with radial glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = alpha),
                        Color(0xFFFFD54F).copy(alpha = alpha * 0.5f),
                        Color.Transparent
                    ),
                    center = Offset(px, py),
                    radius = particle.size * 3f
                ),
                center = Offset(px, py),
                radius = particle.size * 3f
            )

            // Bright core
            drawCircle(
                color = Color.White.copy(alpha = alpha),
                center = Offset(px, py),
                radius = particle.size
            )
        }
    }
}

// ============================================================================
// Organic Flicker Functions (Layered Sine Waves at Irrational Frequencies)
// ============================================================================

/**
 * Horizontal flicker offset using layered sine waves.
 * Each frequency is an irrational number to prevent exact repetition.
 */
private fun computeFlickerX(time: Float): Float {
    return (sin(time * FREQ_1 * 2.7f) * 2.5f +
            sin(time * FREQ_2 * 4.1f) * 1.5f +
            sin(time * FREQ_5 * 6.3f) * 0.8f)
}

/**
 * Vertical flicker offset using different irrational frequencies.
 */
private fun computeFlickerY(time: Float): Float {
    return (sin(time * FREQ_3 * 3.1f) * 1.5f +
            sin(time * FREQ_4 * 5.3f) * 1.0f +
            sin(time * FREQ_1 * 7.7f) * 0.5f)
}

/**
 * Scale flicker for flame width/height variation.
 */
private fun computeScaleFlicker(time: Float): Float {
    return (sin(time * FREQ_2 * 3.3f) * 0.5f +
            sin(time * FREQ_3 * 5.7f) * 0.3f +
            sin(time * FREQ_5 * 8.1f) * 0.2f)
}

// ============================================================================
// Previews
// ============================================================================

@Preview(showBackground = true)
@Composable
private fun SplashScreenPreview() {
    TableTorchTheme {
        SplashScreen(onTimeout = {})
    }
}
