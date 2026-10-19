package orinasa.njarasoa.maripanatokana.ui.weather.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import orinasa.njarasoa.maripanatokana.ui.theme.LocalDisplayFont

internal enum class OverviewStyle { LINE, BARS }

/**
 * The chart under a row of forecast cards: every item at once, squeezed to the card's width,
 * with the part scrolled into view highlighted. Callers pass values in the primary unit and
 * ready-made labels (with both units) for the highest and lowest points.
 */
@Composable
internal fun OverviewChart(
    values: List<Double?>,
    itemWidth: Dp,
    spacing: Dp,
    scrollOffset: Float,
    totalScrollWidth: Float,
    modifier: Modifier = Modifier,
    height: Dp = 48.dp,
    style: OverviewStyle = OverviewStyle.LINE,
    // Scale from zero, with zero on the bottom edge: for amounts where 0 means none (wind, UV, AQI)
    fromZero: Boolean = false,
    color: Color = Color.White,
    // One color per point, e.g. by tier; overrides [color] for the dots and bars
    pointColors: List<Color>? = null,
    highLabel: String? = null,
    lowLabel: String? = null,
    legend: List<Pair<Color, String>>? = null,
) {
    val textMeasurer = rememberTextMeasurer()
    val displayFont = LocalDisplayFont.current
    val valid = values.filterNotNull().filter { it.isFinite() }
    val max = valid.maxOrNull() ?: 0.0
    val zeroBased = style == OverviewStyle.BARS || fromZero
    val min = if (zeroBased) 0.0 else valid.minOrNull() ?: 0.0
    val lowest = valid.minOrNull() ?: 0.0
    val highIndex = values.indexOfFirst { it == max }
    val lowIndex = values.indexOfFirst { it == lowest }

    Column(modifier = modifier) {
        legend?.let { TierLegend(it, modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)) }
        Canvas(modifier = Modifier.fillMaxWidth().height(height)) {
            val h = size.height
            val w = size.width
            // Room above and below the line for the labels
            val pad = 12.dp.toPx()
            val xScale = if (totalScrollWidth > 0f) w / totalScrollWidth else 1f
            fun xFor(i: Int) = (i * (itemWidth.toPx() + spacing.toPx()) + itemWidth.toPx() / 2) * xScale
            fun yFor(v: Double): Float {
                val bottom = if (zeroBased) h - 1.5.dp.toPx() else h - pad
                return if (max > min) (bottom - (v - min) / (max - min) * (bottom - pad)).toFloat() else (bottom + pad) / 2
            }
            fun colorAt(i: Int) = pointColors?.getOrNull(i) ?: color

            val points = values.mapIndexedNotNull { i, v -> if (v != null && v.isFinite()) i to Offset(xFor(i), yFor(v)) else null }

            // Labels near an edge grow inwards so they stay inside the chart
            fun drawLabel(text: String, i: Int, y: Float) {
                val layout = textMeasurer.measure(text, TextStyle(color = colorAt(i), fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = displayFont))
                val x = xFor(i)
                val left = when {
                    x < w / 4 -> (x - 4.dp.toPx()).coerceAtLeast(0f)
                    x > w * 3 / 4 -> (x + 4.dp.toPx() - layout.size.width).coerceAtMost(w - layout.size.width)
                    else -> x - layout.size.width / 2f
                }
                drawText(layout, topLeft = Offset(left, y.coerceIn(0f, h - layout.size.height)))
            }

            if (style == OverviewStyle.BARS) {
                drawLine(Color.White.copy(alpha = 0.2f), Offset(0f, h - 0.5f), Offset(w, h - 0.5f), 1.dp.toPx())
                val barWidth = maxOf(2.dp.toPx(), itemWidth.toPx() * xScale * 0.6f)
                points.forEach { (i, p) ->
                    if ((values[i] ?: 0.0) > 0) {
                        drawRoundRect(colorAt(i), Offset(p.x - barWidth / 2, p.y), Size(barWidth, h - p.y), CornerRadius(1.5.dp.toPx()))
                    }
                }
                if (max > 0 && highLabel != null && highIndex >= 0) {
                    drawLabel(highLabel, highIndex, yFor(max) - 12.dp.toPx())
                }
            } else {
                if (points.size >= 2) {
                    val offsets = points.map { it.second }
                    val cps = computeMonotoneCubicControlPoints(offsets)
                    val path = Path().apply {
                        moveTo(offsets[0].x, offsets[0].y)
                        cps.forEachIndexed { k, cp -> cubicTo(cp.cp1.x, cp.cp1.y, cp.cp2.x, cp.cp2.y, offsets[k + 1].x, offsets[k + 1].y) }
                    }
                    drawPath(path, color.copy(alpha = if (pointColors != null) 0.5f else 1f), style = Stroke(2.dp.toPx()))
                }
                points.forEach { (i, p) -> drawCircle(colorAt(i), 3.dp.toPx(), p) }
                if (highLabel != null && highIndex >= 0) drawLabel(highLabel, highIndex, yFor(max) - 14.dp.toPx())
                if (zeroBased) drawLine(Color.White.copy(alpha = 0.2f), Offset(0f, h - 0.5f), Offset(w, h - 0.5f), 1.dp.toPx())
                // Below the point, or above it when the point sits on the zero line
                if (lowLabel != null && lowIndex >= 0 && max > lowest) {
                    drawLabel(lowLabel, lowIndex, if (zeroBased) yFor(lowest) - 14.dp.toPx() else yFor(lowest) + 3.dp.toPx())
                }
            }

            // The part of the row scrolled into view, as a light band
            if (totalScrollWidth > w) {
                val vpLeft = (scrollOffset / totalScrollWidth * w).coerceIn(0f, w)
                val vpRight = ((scrollOffset + w) / totalScrollWidth * w).coerceIn(0f, w)
                drawRoundRect(Color.White.copy(alpha = 0.06f), Offset(vpLeft, 0f), Size(vpRight - vpLeft, h), androidx.compose.ui.geometry.CornerRadius(6.dp.toPx()))
            }
        }
    }
}
