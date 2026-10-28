// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

package orinasa.njarasoa.maripanatokana.ui.weather.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import orinasa.njarasoa.maripanatokana.ui.theme.LocalDisplayFont
import kotlin.math.roundToInt

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
    // Shown when every value is zero, so an empty chart reads as none rather than missing data
    emptyLabel: String? = null,
    // A second line on its own scale (another unit), e.g. the dew point under the humidity; LINE only
    secondValues: List<Double?>? = null,
    secondColor: Color = Color.White,
    secondHighLabel: String? = null,
    secondLowLabel: String? = null,
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
            fun yFor(v: Double, lo: Double = min, hi: Double = max): Float {
                val bottom = if (zeroBased) h - 1.5.dp.toPx() else h - pad
                return if (hi > lo) (bottom - (v - lo) / (hi - lo) * (bottom - pad)).toFloat() else (bottom + pad) / 2
            }
            fun colorAt(i: Int) = pointColors?.getOrNull(i) ?: color

            val points = values.mapIndexedNotNull { i, v -> if (v != null && v.isFinite()) i to Offset(xFor(i), yFor(v)) else null }

            // Labels near an edge grow inwards so they stay inside the chart. A label that would
            // overlap [avoid] (another label's left and right edges) goes next to it, on its row.
            fun drawLabel(text: String, i: Int, y: Float, labelColor: Color = colorAt(i), avoid: Pair<Float, Float>? = null, avoidY: Float = y): Pair<Float, Float> {
                val layout = textMeasurer.measure(text, TextStyle(color = labelColor, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = displayFont))
                val x = xFor(i)
                val width = layout.size.width.toFloat()
                var left = when {
                    x < w / 4 -> (x - 4.dp.toPx()).coerceAtLeast(0f)
                    x > w * 3 / 4 -> (x + 4.dp.toPx() - width).coerceAtMost(w - width)
                    else -> x - width / 2f
                }
                var top = y
                val gap = 6.dp.toPx()
                if (avoid != null && left < avoid.second + gap && left + width > avoid.first - gap) {
                    left = if (avoid.second + gap + width <= w) avoid.second + gap else (avoid.first - gap - width).coerceAtLeast(0f)
                    top = avoidY
                }
                drawText(layout, topLeft = Offset(left, top.coerceIn(0f, h - layout.size.height)))
                return left to left + width
            }

            if (style == OverviewStyle.BARS) {
                drawLine(Color.White.copy(alpha = 0.2f), Offset(0f, h - 0.5f), Offset(w, h - 0.5f), 1.dp.toPx())
                val barWidth = maxOf(2.dp.toPx(), itemWidth.toPx() * xScale * 0.6f)
                points.forEach { (i, p) ->
                    if ((values[i] ?: 0.0) > 0) {
                        drawRoundRect(colorAt(i), Offset(p.x - barWidth / 2, p.y), Size(barWidth, h - p.y), CornerRadius(1.5.dp.toPx()))
                    }
                }
                if (valid.isNotEmpty() && max <= 0 && emptyLabel != null) {
                    val layout = textMeasurer.measure(emptyLabel, TextStyle(color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = displayFont))
                    drawText(layout, topLeft = Offset((w - layout.size.width) / 2, (h - layout.size.height) / 2))
                }
                if (max > 0 && highLabel != null && highIndex >= 0) {
                    drawLabel(highLabel, highIndex, yFor(max) - 12.dp.toPx())
                }
            } else {
                fun drawCurve(offsets: List<Offset>, lineColor: Color) {
                    if (offsets.size < 2) return
                    val cps = computeMonotoneCubicControlPoints(offsets)
                    val path = Path().apply {
                        moveTo(offsets[0].x, offsets[0].y)
                        cps.forEachIndexed { k, cp -> cubicTo(cp.cp1.x, cp.cp1.y, cp.cp2.x, cp.cp2.y, offsets[k + 1].x, offsets[k + 1].y) }
                    }
                    drawPath(path, lineColor, style = Stroke(2.dp.toPx()))
                }
                // The second line first, so the main line stays on top
                val second = secondValues?.let { vs ->
                    val valid2 = vs.filterNotNull().filter { it.isFinite() }
                    val hi2 = valid2.maxOrNull() ?: return@let null
                    val lo2 = valid2.min()
                    val points2 = vs.mapIndexedNotNull { i, v -> if (v != null && v.isFinite()) i to Offset(xFor(i), yFor(v, lo2, hi2)) else null }
                    drawCurve(points2.map { it.second }, secondColor)
                    points2.forEach { (_, p) -> drawCircle(secondColor, 2.5.dp.toPx(), p) }
                    Triple(vs, lo2, hi2)
                }
                drawCurve(points.map { it.second }, color.copy(alpha = if (pointColors != null) 0.5f else 1f))
                points.forEach { (i, p) -> drawCircle(colorAt(i), 3.dp.toPx(), p) }
                val highY = yFor(max) - 14.dp.toPx()
                val highSpan = if (highLabel != null && highIndex >= 0) drawLabel(highLabel, highIndex, highY) else null
                if (zeroBased) drawLine(Color.White.copy(alpha = 0.2f), Offset(0f, h - 0.5f), Offset(w, h - 0.5f), 1.dp.toPx())
                // Below the point, or above it when the point sits on the zero line
                val lowY = if (zeroBased) yFor(lowest) - 14.dp.toPx() else yFor(lowest) + 3.dp.toPx()
                val lowSpan = if (lowLabel != null && lowIndex >= 0 && max > lowest) drawLabel(lowLabel, lowIndex, lowY) else null
                // The second line's labels last, so no line covers them
                second?.let { (vs, lo2, hi2) ->
                    if (secondHighLabel != null) {
                        drawLabel(secondHighLabel, vs.indexOf(hi2), yFor(hi2, lo2, hi2) - 14.dp.toPx(), secondColor, highSpan, highY)
                    }
                    if (secondLowLabel != null && hi2 > lo2) {
                        drawLabel(secondLowLabel, vs.indexOf(lo2), yFor(lo2, lo2, hi2) + 3.dp.toPx(), secondColor, lowSpan, lowY)
                    }
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

/**
 * Lets the user slide a finger along the chart in [content]: a line and a label (the hour or
 * the day of [label]) follow the finger, and the row of cards scrolls to the item under it.
 * Only a horizontal slide starts it, so the page still scrolls up and down.
 */
@Composable
internal fun ChartScrub(
    count: Int,
    itemWidth: Dp,
    spacing: Dp,
    totalScrollWidth: Float,
    scrollState: ScrollState,
    label: (Int) -> String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    var index by remember { mutableStateOf<Int?>(null) }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val textMeasurer = rememberTextMeasurer()
    val displayFont = LocalDisplayFont.current
    val density = LocalDensity.current
    val step = with(density) { (itemWidth + spacing).toPx() }
    val half = with(density) { itemWidth.toPx() / 2 }
    fun xScale(w: Float) = if (totalScrollWidth > 0f) w / totalScrollWidth else 1f
    fun select(x: Float, w: Float) {
        if (count == 0) return
        val i = ((x / xScale(w) - half) / step).roundToInt().coerceIn(0, count - 1)
        if (i == index) return
        index = i
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        // The item in the middle of the row
        scope.launch { scrollState.scrollTo((i * step + half - scrollState.viewportSize / 2f).roundToInt()) }
    }

    Box(
        modifier = modifier
            .pointerInput(count, totalScrollWidth, step) {
                detectHorizontalDragGestures(
                    onDragStart = { select(it.x, size.width.toFloat()) },
                    onDragEnd = { index = null },
                    onDragCancel = { index = null },
                ) { change, _ ->
                    change.consume()
                    select(change.position.x, size.width.toFloat())
                }
            }
            .drawWithContent {
                drawContent()
                val i = index ?: return@drawWithContent
                val w = size.width
                val x = (i * step + half) * xScale(w)
                drawLine(Color.White.copy(alpha = 0.6f), Offset(x, 0f), Offset(x, size.height), 1.dp.toPx())
                val layout = textMeasurer.measure(label(i), TextStyle(color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = displayFont))
                val padX = 6.dp.toPx()
                val padY = 2.dp.toPx()
                val pillWidth = layout.size.width + padX * 2
                val left = (x - pillWidth / 2).coerceIn(0f, (w - pillWidth).coerceAtLeast(0f))
                drawRoundRect(Color.Black.copy(alpha = 0.75f), Offset(left, 0f), Size(pillWidth, layout.size.height + padY * 2), CornerRadius(6.dp.toPx()))
                drawText(layout, topLeft = Offset(left + padX, padY))
            },
    ) {
        content()
    }
}
