package com.rockyriverapps.tabletorch.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.rockyriverapps.tabletorch.models.ParticleShape
import com.rockyriverapps.tabletorch.ui.theme.toComposeColor
import kotlinx.collections.immutable.ImmutableList
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Mutable snapshot of a single particle for rendering.
 * Fields are updated in-place each frame to avoid per-frame allocations.
 */
private class ParticleState(
    var x: Float = 0f,
    var y: Float = 0f,
    var size: Float = 0f,
    var color: Color = Color.Transparent,
    var opacity: Float = 0f,
    var lifetime: Float = 0f,
    var age: Float = 0f,
    var driftSpeed: Float = 0f,
    var wobblePhase: Float = 0f,
    var wobbleAmplitude: Float = 0f
) {
    fun copyFrom(p: MutableParticle) {
        x = p.x; y = p.y; size = p.size; color = p.color
        opacity = p.opacity; lifetime = p.lifetime; age = p.age
        driftSpeed = p.driftSpeed; wobblePhase = p.wobblePhase
        wobbleAmplitude = p.wobbleAmplitude
    }
}

/**
 * Composable that renders an animated particle effect overlay.
 *
 * Particles spawn from the bottom of the screen and drift upward with a horizontal wobble.
 * They shrink and fade as they age. Colors cycle through the non-selected palette colors
 * every 3 seconds with slight HSB variation for visual richness.
 *
 * @param selectedColors The full list of color values in the palette
 * @param selectedColorIndex The currently selected color index (excluded from particle colors)
 * @param particleShape The shape to render for each particle
 * @param modifier Modifier for the canvas
 */
@Composable
fun EmberParticleView(
    selectedColors: ImmutableList<Long>,
    selectedColorIndex: Int,
    particleShape: ParticleShape,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val minSizePx = with(density) { 4.dp.toPx() }
    val maxSizePx = with(density) { 16.dp.toPx() }

    // Pre-allocated snapshot list reused each frame; count tracks active entries
    var particleSnapshot by remember { mutableStateOf(emptyList<ParticleState>()) }
    // Reusable buffer to avoid per-frame list/object allocation
    val snapshotBuffer = remember { ArrayList<ParticleState>(MAX_PARTICLES) }
    var canvasWidth by remember { mutableStateOf(0f) }
    var canvasHeight by remember { mutableStateOf(0f) }

    // Build the list of non-selected colors for cycling
    val nonSelectedColors = remember(selectedColors, selectedColorIndex) {
        selectedColors.filterIndexed { index, _ -> index != selectedColorIndex }
            .map { it.toComposeColor() }
    }
    // Keep a fresh reference accessible from the long-running LaunchedEffect
    val currentNonSelectedColors by rememberUpdatedState(nonSelectedColors)

    // Frame-based animation loop — all mutation happens here, produces immutable snapshots
    LaunchedEffect(Unit) {
        // Mutable working list, only accessed within this coroutine
        val particles = mutableListOf<MutableParticle>()
        var lastFrameTime = 0L
        var spawnAccumulator = 0f
        var colorCycleTime = 0f

        while (true) {
            withFrameMillis { frameTimeMs ->
                if (lastFrameTime == 0L) {
                    lastFrameTime = frameTimeMs
                    return@withFrameMillis
                }

                val deltaSeconds = ((frameTimeMs - lastFrameTime) / 1000f).coerceAtMost(0.05f)
                lastFrameTime = frameTimeMs

                if (canvasWidth <= 0f || canvasHeight <= 0f) return@withFrameMillis

                // Update color cycle timer
                colorCycleTime += deltaSeconds

                // Determine current particle color from non-selected palette
                val colors = currentNonSelectedColors
                val currentColorIndex = if (colors.isNotEmpty()) {
                    ((colorCycleTime / 3f).toInt()) % colors.size
                } else {
                    0
                }

                // Spawn new particles (~40 per second, max 150)
                spawnAccumulator += deltaSeconds * 40f
                while (spawnAccumulator >= 1f && particles.size < MAX_PARTICLES) {
                    spawnAccumulator -= 1f

                    val baseColor = if (colors.isNotEmpty()) {
                        colors[currentColorIndex]
                    } else {
                        Color.White
                    }

                    // Apply slight HSB variation for visual richness
                    val variedColor = applyColorVariation(baseColor)

                    particles.add(
                        MutableParticle(
                            x = Random.nextFloat() * canvasWidth,
                            y = canvasHeight + maxSizePx,
                            size = minSizePx + Random.nextFloat() * (maxSizePx - minSizePx),
                            color = variedColor,
                            opacity = 1f,
                            lifetime = 3f + Random.nextFloat() * 5f,
                            age = 0f,
                            driftSpeed = 40f + Random.nextFloat() * 60f,
                            wobblePhase = Random.nextFloat() * (2f * PI.toFloat()),
                            wobbleAmplitude = 10f + Random.nextFloat() * 20f
                        )
                    )
                }

                // Update existing particles
                val iterator = particles.iterator()
                while (iterator.hasNext()) {
                    val p = iterator.next()
                    p.age += deltaSeconds

                    if (p.age >= p.lifetime) {
                        iterator.remove()
                        continue
                    }

                    val progress = p.age / p.lifetime

                    // Rise upward
                    p.y -= p.driftSpeed * deltaSeconds

                    // Horizontal wobble using sine wave
                    p.x += sin(p.age * 2f + p.wobblePhase) * p.wobbleAmplitude * deltaSeconds

                    // Shrink as particle ages
                    val initialSize = minSizePx + (maxSizePx - minSizePx) * 0.5f
                    p.size = initialSize * (1f - progress * 0.7f)

                    // Fade out in the last 30% of lifetime
                    p.opacity = if (progress > 0.7f) {
                        1f - ((progress - 0.7f) / 0.3f)
                    } else {
                        1f
                    }
                }

                // Publish snapshot for the Canvas — reuse buffer objects to avoid allocation
                while (snapshotBuffer.size < particles.size) {
                    snapshotBuffer.add(ParticleState())
                }
                for (i in particles.indices) {
                    snapshotBuffer[i].copyFrom(particles[i])
                }
                // Expose only the active portion as a new list reference to trigger recomposition
                particleSnapshot = snapshotBuffer.subList(0, particles.size).toList()
            }
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { intSize ->
                canvasWidth = intSize.width.toFloat()
                canvasHeight = intSize.height.toFloat()
            }
    ) {
        for (particle in particleSnapshot) {
            drawParticle(particle, particleShape)
        }
    }
}

/**
 * Mutable working particle — only mutated inside the LaunchedEffect coroutine.
 */
private class MutableParticle(
    var x: Float,
    var y: Float,
    var size: Float,
    var color: Color,
    var opacity: Float,
    var lifetime: Float,
    var age: Float,
    var driftSpeed: Float,
    var wobblePhase: Float,
    var wobbleAmplitude: Float
)

/**
 * Draw a single particle with the given shape.
 */
private fun DrawScope.drawParticle(particle: ParticleState, shape: ParticleShape) {
    val color = particle.color.copy(alpha = particle.opacity.coerceIn(0f, 1f))
    val center = Offset(particle.x, particle.y)
    val halfSize = particle.size / 2f

    when (shape) {
        ParticleShape.EMBERS -> {
            drawCircle(
                color = color,
                radius = halfSize,
                center = center
            )
        }

        ParticleShape.HEARTS -> {
            drawHeart(center, halfSize, color)
        }

        ParticleShape.STARS -> {
            drawStar(center, halfSize, color)
        }

        ParticleShape.SNOWFLAKES -> {
            drawSnowflake(center, halfSize, color)
        }

        ParticleShape.MUSIC_NOTES -> {
            drawMusicNote(center, halfSize, color)
        }
    }
}

/**
 * Draw a heart shape using circle approximation.
 */
private fun DrawScope.drawHeart(center: Offset, halfSize: Float, color: Color) {
    val r = halfSize * 0.5f
    drawCircle(color = color, radius = r, center = Offset(center.x - r * 0.6f, center.y - r * 0.3f))
    drawCircle(color = color, radius = r, center = Offset(center.x + r * 0.6f, center.y - r * 0.3f))
    drawCircle(color = color, radius = r * 0.8f, center = Offset(center.x, center.y + r * 0.4f))
}

/**
 * Draw a 5-pointed star using small circles at each point.
 */
private fun DrawScope.drawStar(center: Offset, halfSize: Float, color: Color) {
    val points = 5
    val outerRadius = halfSize
    val innerRadius = halfSize * 0.4f

    for (i in 0 until points) {
        val angle = (i * 2 * PI / points - PI / 2).toFloat()
        val px = center.x + cos(angle) * outerRadius
        val py = center.y + sin(angle) * outerRadius
        drawCircle(color = color, radius = halfSize * 0.25f, center = Offset(px, py))
    }
    drawCircle(color = color, radius = innerRadius, center = center)
}

/**
 * Draw a snowflake using 6 arms with dots.
 */
private fun DrawScope.drawSnowflake(center: Offset, halfSize: Float, color: Color) {
    val armCount = 6
    val dotRadius = halfSize * 0.15f

    for (i in 0 until armCount) {
        val angle = (i * PI / 3).toFloat()
        for (j in 1..3) {
            val dist = halfSize * j / 3f
            val px = center.x + cos(angle) * dist
            val py = center.y + sin(angle) * dist
            drawCircle(color = color, radius = dotRadius, center = Offset(px, py))
        }
    }
    drawCircle(color = color, radius = dotRadius * 1.5f, center = center)
}

/**
 * Draw a music note shape.
 */
private fun DrawScope.drawMusicNote(center: Offset, halfSize: Float, color: Color) {
    drawCircle(
        color = color,
        radius = halfSize * 0.4f,
        center = Offset(center.x - halfSize * 0.15f, center.y + halfSize * 0.3f)
    )
    for (i in 0..4) {
        val y = center.y + halfSize * 0.3f - (i * halfSize * 0.3f)
        drawCircle(
            color = color,
            radius = halfSize * 0.1f,
            center = Offset(center.x + halfSize * 0.15f, y)
        )
    }
    drawCircle(
        color = color,
        radius = halfSize * 0.2f,
        center = Offset(center.x + halfSize * 0.35f, center.y - halfSize * 0.8f)
    )
}

/**
 * Apply slight hue and brightness variation to a color for visual richness.
 */
private fun applyColorVariation(color: Color): Color {
    val r = color.red
    val g = color.green
    val b = color.blue

    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val delta = max - min

    var hue = when {
        delta == 0f -> 0f
        max == r -> 60f * (((g - b) / delta) % 6f)
        max == g -> 60f * (((b - r) / delta) + 2f)
        else -> 60f * (((r - g) / delta) + 4f)
    }
    if (hue < 0f) hue += 360f

    val saturation = if (max == 0f) 0f else delta / max
    val value = max

    val hueVariation = (Random.nextFloat() - 0.5f) * 20f
    val brightnessVariation = (Random.nextFloat() - 0.5f) * 0.15f

    val newHue = ((hue + hueVariation) % 360f + 360f) % 360f
    val newValue = (value + brightnessVariation).coerceIn(0f, 1f)

    return hsvToColor(newHue, saturation.coerceIn(0f, 1f), newValue, color.alpha)
}

/**
 * Convert HSV values to a Compose Color.
 */
private fun hsvToColor(hue: Float, saturation: Float, value: Float, alpha: Float): Color {
    val c = value * saturation
    val x = c * (1f - kotlin.math.abs((hue / 60f) % 2f - 1f))
    val m = value - c

    val (r1, g1, b1) = when {
        hue < 60f -> Triple(c, x, 0f)
        hue < 120f -> Triple(x, c, 0f)
        hue < 180f -> Triple(0f, c, x)
        hue < 240f -> Triple(0f, x, c)
        hue < 300f -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }

    return Color(
        red = (r1 + m).coerceIn(0f, 1f),
        green = (g1 + m).coerceIn(0f, 1f),
        blue = (b1 + m).coerceIn(0f, 1f),
        alpha = alpha
    )
}

private const val MAX_PARTICLES = 150
