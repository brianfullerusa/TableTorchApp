package com.rockyriverapps.tabletorch.models

/**
 * Available particle shapes for the ember particle effect.
 * Each shape has a human-readable label and a Unicode symbol for display in the shape picker.
 */
enum class ParticleShape(val label: String, val symbol: String) {
    EMBERS("Embers", "\u25CF"),
    HEARTS("Hearts", "\u2665"),
    STARS("Stars", "\u2605"),
    SNOWFLAKES("Snowflakes", "\u2744"),
    MUSIC_NOTES("Music Notes", "\u266A")
}
