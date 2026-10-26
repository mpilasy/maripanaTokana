// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

package orinasa.njarasoa.maripanatokana.domain.model

// Each level's range (Low, Moderate, High, Very High, Extreme); the last is capped so it can be drawn
private val UV_DIAL_BOUNDS = listOf(0.0, 3.0, 6.0, 8.0, 11.0, 14.0)

/** Index of a UV reading's level: 0 Low, 1 Moderate, 2 High, 3 Very High, 4 Extreme. */
fun uvLevel(uv: Double): Int = when {
    uv < 3 -> 0
    uv < 6 -> 1
    uv < 8 -> 2
    uv < 11 -> 3
    else -> 4
}

/**
 * Where a UV reading sits on a dial of five equal level segments, 0-1: the level picks the segment and
 * the reading's place within that level's range picks the spot inside it.
 */
fun uvDialPosition(uv: Double): Float {
    if (!uv.isFinite()) return 0f
    val level = uvLevel(uv)
    val b = UV_DIAL_BOUNDS
    // Kept just inside the segment, so a reading on a boundary still shows in its own level
    val within = ((uv - b[level]) / (b[level + 1] - b[level])).coerceIn(0.02, 0.98)
    return ((level + within) / 5).toFloat()
}
