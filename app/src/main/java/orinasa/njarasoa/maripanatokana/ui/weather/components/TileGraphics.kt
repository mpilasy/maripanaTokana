// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

package orinasa.njarasoa.maripanatokana.ui.weather.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import orinasa.njarasoa.maripanatokana.ui.theme.AppDarkOnPrimary
import orinasa.njarasoa.maripanatokana.ui.theme.AppDarkPrimary
import orinasa.njarasoa.maripanatokana.ui.theme.LocalBodyFont
import orinasa.njarasoa.maripanatokana.ui.theme.TileSurface
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * A 270° arc, open at the bottom, with a dot at [fraction] (0-1). Filled up to the dot in [color], or,
 * with [segments], drawn as equal coloured segments (one per level) so the whole scale always shows.
 */
@Composable
fun Gauge(
    // A null size leaves the size to the modifier
    fraction: Float, color: Color, modifier: Modifier = Modifier, size: Dp? = 40.dp, segments: List<Color>? = null,
    // Shown in the middle of the dial, e.g. the reading itself
    content: (@Composable () -> Unit)? = null,
) {
    val f = if (fraction.isFinite()) fraction.coerceIn(0f, 1f) else 0f
    Box(if (size != null) modifier.size(size) else modifier, contentAlignment = Alignment.Center) {
    Canvas(Modifier.fillMaxSize()) {
        val u = this.size.minDimension / 48f
        // The ring and the dot stop growing on a large dial
        val k = minOf(u, 1.6f * density)
        val stroke = 4 * k
        val inset = 4 * k
        val arcSize = Size(this.size.width - inset * 2, this.size.height - inset * 2)
        val topLeft = Offset(inset, inset)
        if (segments != null) {
            // A small gap keeps the levels apart; the reading's own level is drawn brightest
            val span = 270f / segments.size
            val current = (f * segments.size).toInt().coerceAtMost(segments.size - 1)
            segments.forEachIndexed { i, c ->
                drawArc(c.copy(alpha = if (i == current) 1f else 0.4f), 135f + i * span + 2f, span - 4f, false, topLeft, arcSize, style = Stroke(stroke))
            }
        } else {
            drawArc(Color.White.copy(alpha = 0.18f), 135f, 270f, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            if (f > 0f) drawArc(color, 135f, 270f * f, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
        }
        // Where the value sits on the dial
        val a = Math.toRadians(135.0 + 270.0 * f)
        val r = arcSize.width / 2
        val end = Offset(center.x + r * cos(a).toFloat(), center.y + r * sin(a).toFloat())
        drawCircle(color, 4.5f * k + 1.25f * k, end)
        drawCircle(Color.White, 4.5f * k - 1.25f * k, end)
    }
    content?.invoke()
    }
}

/**
 * A compass ring for a card's middle: a tick at north and an arrowhead on the ring at where the
 * wind comes from (degrees, 0 = north), pointing in. The middle is free for [content].
 */
@Composable
fun CompassDial(degrees: Int, modifier: Modifier = Modifier, content: (@Composable () -> Unit)? = null) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val u = size.minDimension / 48f
            // The ring stops growing on a large dial, like the gauges
            val k = minOf(u, 1.6f * density)
            val r = size.minDimension / 2 - 4 * k
            drawCircle(Color.White.copy(alpha = 0.18f), r, center, style = Stroke(2.5f * k))
            drawLine(Color.White.copy(alpha = 0.6f), Offset(center.x, center.y - r - 3 * k), Offset(center.x, center.y - r + 4 * k), 2 * k, StrokeCap.Round)
            rotate(degrees.toFloat(), center) {
                val tip = center.y - r + 7 * k
                val back = center.y - r - 3 * k
                drawPath(Path().apply {
                    moveTo(center.x, tip)
                    lineTo(center.x + 5 * k, back)
                    lineTo(center.x - 5 * k, back)
                    close()
                }, Color(0xFF64B5F6))
            }
        }
        content?.invoke()
    }
}

/** A compass ring whose needle points to where the wind comes from (degrees, 0 = north). */
@Composable
fun WindCompass(degrees: Int, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    Canvas(modifier.size(size)) {
        val u = this.size.minDimension / 48f
        val c = Offset(24 * u, 24 * u)
        drawCircle(Color.White.copy(alpha = 0.12f), 20 * u, c, style = Stroke(2 * u))
        drawLine(Color.White.copy(alpha = 0.5f), Offset(24 * u, 2 * u), Offset(24 * u, 8 * u), 2 * u, StrokeCap.Round)
        rotate(degrees.toFloat(), c) {
            fun shape(vararg p: Pair<Float, Float>) = Path().apply {
                moveTo(p[0].first * u, p[0].second * u)
                p.drop(1).forEach { lineTo(it.first * u, it.second * u) }
                close()
            }
            drawPath(shape(24f to 8f, 29f to 26f, 24f to 22f, 19f to 26f), Color(0xFF64B5F6))
            drawPath(shape(24f to 22f, 29f to 26f, 24f to 40f, 19f to 26f), Color.White.copy(alpha = 0.25f))
        }
    }
}

/** The day as an arc from sunrise to sunset with the sun at [now] (all epoch seconds). */
@Composable
fun SunArc(sunrise: Long, sunset: Long, now: Long, modifier: Modifier = Modifier) {
    val t = if (sunset > sunrise) (now - sunrise).toFloat() / (sunset - sunrise) else -1f
    SkyArc(t, Color(0xFFFFB74D), modifier) { drawCircle(Color(0xFFFFD54F), 5.dp.toPx(), it) }
}

/**
 * A dashed arc from rising (left) to setting (right). While [t] (the part of the way done) is in
 * 0-1, the part done is drawn in [progressColor] and [body] draws the sun or moon at its place.
 */
@Composable
fun SkyArc(t: Float, progressColor: Color, modifier: Modifier = Modifier, body: DrawScope.(Offset) -> Unit) {
    Canvas(modifier) {
        // Half-ellipse across the width, peaking near the top
        val base = size.height * 44f / 52f
        val rx = size.width * 52f / 120f
        val ry = size.height * 36f / 52f
        val cx = size.width / 2
        val oval = Rect(cx - rx, base - ry, cx + rx, base + ry)
        drawLine(Color.White.copy(alpha = 0.15f), Offset(0f, base), Offset(size.width, base), 1.dp.toPx())
        drawArc(Color.White.copy(alpha = 0.25f), 180f, 180f, false, oval.topLeft, oval.size,
            style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 3.dp.toPx()))))
        if (t in 0f..1f) {
            drawArc(progressColor, 180f, 180f * t, false, oval.topLeft, oval.size, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
            val a = PI * (1 - t)
            body(Offset(cx + rx * cos(a).toFloat(), base - ry * sin(a).toFloat()))
        }
    }
}

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

/** A vertical track with a blue-to-orange segment from [low] to [high] (fractions of the week's range, 0 = bottom). */
@Composable
fun TemperatureRangeBar(low: Float, high: Float, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val r = androidx.compose.ui.geometry.CornerRadius(w / 2, w / 2)
        drawRoundRect(Color.White.copy(alpha = 0.08f), cornerRadius = r)
        val top = size.height * (1 - high.coerceIn(0f, 1f))
        val bottom = maxOf(size.height * (1 - low.coerceIn(0f, 1f)), top + w)
        drawRoundRect(
            androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color(0xFFFF7043), Color(0xFF64B5F6)), top, bottom),
            topLeft = Offset(0f, top), size = Size(w, bottom - top), cornerRadius = r,
        )
    }
}

/** A short bar in a level's colour, for cards that sit above a colour legend; the label is for screen readers. */
@Composable
fun LevelBar(color: Color, label: String, modifier: Modifier = Modifier) {
    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .padding(top = 4.dp, bottom = 2.dp)
            .size(width = 28.dp, height = 5.dp)
            .background(color, RoundedCornerShape(3.dp))
            .semantics { contentDescription = label },
    )
}

/** A row of chips that picks what a forecast card shows. */
@Composable
fun <T> ForecastTabs(tabs: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit, modifier: Modifier = Modifier) {
    val bodyFont = LocalBodyFont.current
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()).padding(bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        tabs.forEach { (id, label) ->
            val isSelected = id == selected
            Text(
                text = label,
                fontSize = 13.sp,
                fontFamily = bodyFont,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) AppDarkOnPrimary else Color.White.copy(alpha = 0.7f),
                maxLines = 1,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isSelected) AppDarkPrimary else TileSurface)
                    .clickable { onSelect(id) }
                    .semantics { role = Role.Tab; this.selected = isSelected }
                    .padding(horizontal = 14.dp, vertical = 7.dp),
            )
        }
    }
}
