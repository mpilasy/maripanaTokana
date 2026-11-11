// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

package orinasa.njarasoa.maripanatokana.ui.common.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos

/** The moon's lit part for [phase] (0 = new, 0.5 = full); [southern] mirrors it, as it is seen south of the equator. */
@Composable
fun MoonPhaseIcon(phase: Double, southern: Boolean, modifier: Modifier = Modifier, size: Dp = 44.dp) {
    Canvas(modifier.size(size)) { drawMoon(phase, southern, center, this.size.minDimension / 2) }
}

/** Draws the moon at [c] with radius [r]: a faint full disc and its lit part. */
fun DrawScope.drawMoon(phase: Double, southern: Boolean, c: Offset, r: Float) {
    drawCircle(Color(0xFF3A3570), r, c)
    // The terminator is a half ellipse; cos > 0 is a crescent, cos < 0 a gibbous moon
    val k = cos(2 * PI * phase)
    val litRight = (phase < 0.5) != southern
    val rx = (r * kotlin.math.abs(k)).toFloat()
    val path = Path().apply {
        moveTo(c.x, c.y - r)
        arcTo(Rect(c.x - r, c.y - r, c.x + r, c.y + r), -90f, if (litRight) 180f else -180f, false)
        arcTo(Rect(c.x - rx, c.y - r, c.x + rx, c.y + r), 90f, if ((k > 0) == litRight) -180f else 180f, false)
        close()
    }
    drawPath(path, Color(0xFFF2EBD3))
}
