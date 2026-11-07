// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

package orinasa.njarasoa.maripanatokana.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.roundToInt

/** The temperature colour scale: the stops in °C and their colours, from cold violet to hot red. Same as the web. */
val TEMP_COLOR_STOPS = listOf(
    -20.0 to 0xB39DDB,
    -10.0 to 0x90CAF9,
    0.0 to 0x4FC3F7,
    10.0 to 0x4DD0E1,
    15.0 to 0x81C784,
    20.0 to 0xDCE775,
    25.0 to 0xFFD54F,
    30.0 to 0xFFB74D,
    35.0 to 0xFF8A65,
    40.0 to 0xE57373,
)

/** The colour of [celsius] on the scale: linear between the two stops around it, the end colour outside the scale. */
fun temperatureColor(celsius: Double): Color {
    val first = TEMP_COLOR_STOPS.first()
    val last = TEMP_COLOR_STOPS.last()
    if (celsius.isNaN() || celsius <= first.first) return rgb(first.second)
    if (celsius >= last.first) return rgb(last.second)
    val upper = TEMP_COLOR_STOPS.indexOfFirst { it.first >= celsius }
    val (c0, rgb0) = TEMP_COLOR_STOPS[upper - 1]
    val (c1, rgb1) = TEMP_COLOR_STOPS[upper]
    val t = (celsius - c0) / (c1 - c0)
    fun channel(shift: Int): Int {
        val a = (rgb0 shr shift) and 0xFF
        val b = (rgb1 shr shift) and 0xFF
        return (a + (b - a) * t).roundToInt()
    }
    return Color(channel(16), channel(8), channel(0))
}

private fun rgb(value: Int) = Color(0xFF000000 or value.toLong())
