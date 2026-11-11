// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

package orinasa.njarasoa.maripanatokana.ui.weather.components

import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.em
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.AnnotatedString
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
import orinasa.njarasoa.maripanatokana.ui.weather.LocalShareCopy
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.font.FontFamily
import orinasa.njarasoa.maripanatokana.ui.weather.chart.ChartDomain
import orinasa.njarasoa.maripanatokana.ui.weather.chart.ChartStyle
import orinasa.njarasoa.maripanatokana.ui.weather.chart.chartDash
import orinasa.njarasoa.maripanatokana.ui.weather.chart.chartDomain
import orinasa.njarasoa.maripanatokana.ui.weather.chart.chartNightRuns

internal enum class OverviewStyle { LINE, BARS }

/** The scale of the second line of an [OverviewChart]. */
internal enum class SecondScale {
    /** A scale of its own, from its values: another unit, as the dew point under the humidity */
    OWN,
    /** The scale of the main values: the same unit, as the gusts with the wind */
    SHARED,
    /** 0 to 100 %: the chance of rain over the precipitation */
    PERCENT,
}

/** The fixed 0-100 % scale of a second line. */
private val PERCENT_SCALE = ChartDomain(0.0, 100.0, listOf(0.0, 50.0, 100.0))

/**
 * A chart label "primary · secondary" (both units) on one line, with the secondary unit smaller and
 * dimmer and no dot, as everywhere in the app the primary unit stands out. Other labels are kept as they are.
 */
internal fun dualLabel(text: String, color: Color): AnnotatedString {
    val i = text.indexOf(" · ")
    if (i < 0) return AnnotatedString(text)
    return buildAnnotatedString {
        append(text.substring(0, i))
        withStyle(SpanStyle(color = color.copy(alpha = color.alpha * 0.7f), fontSize = 0.8.em, fontWeight = FontWeight.Medium)) {
            append(" " + text.substring(i + 3))
        }
    }
}

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
    // A second line, e.g. the dew point under the humidity, on the scale [secondScale]
    secondValues: List<Double?>? = null,
    secondColor: Color = Color.White,
    // Labels of the second line's highest and lowest points; LINE and SecondScale.OWN only
    secondHighLabel: String? = null,
    secondLowLabel: String? = null,
    secondScale: SecondScale = SecondScale.OWN,
    secondWidth: Dp = ChartStyle.lineWidth,
    secondDashed: Boolean = false,
    // The name of the second line, for a small key at the top of the chart when there is room
    secondKey: String? = null,
    // For temperatures: a faint line at freezing (0 °C, 32 °F) when it is inside the chart; LINE only
    freezingAt: Double? = null,
    // The smallest height of the scale, in the unit of the values
    minSpan: Double = 0.0,
    // Hourly charts: the night flag of each item, for a dark band, and the items at local midnight, for a line
    night: List<Boolean> = emptyList(),
    midnights: List<Int> = emptyList(),
    // The item under the finger: a line, and a dot on each line
    selected: Int? = null,
) {
    val textMeasurer = rememberTextMeasurer()
    val displayFont = LocalDisplayFont.current
    val valid = values.filterNotNull().filter { it.isFinite() }
    val max = valid.maxOrNull() ?: 0.0
    val zeroBased = style == OverviewStyle.BARS || fromZero
    val lowest = valid.minOrNull() ?: 0.0
    val highIndex = values.indexOfFirst { it == max }
    val lowIndex = values.indexOfFirst { it == lowest }
    // Round ends (steps of 1, 2 or 5 times a power of 10), as in the chart view
    val shared = if (secondScale == SecondScale.SHARED) secondValues.orEmpty() else emptyList()
    val domain = chartDomain(values + shared, zeroBased, minSpan)
    val secondDomain = secondValues?.let { vs ->
        when (secondScale) {
            SecondScale.OWN -> chartDomain(vs, fromZero = false, minSpan = 0.0)
            SecondScale.SHARED -> domain
            SecondScale.PERCENT -> PERCENT_SCALE
        }
    }

    Column(modifier = modifier) {
        legend?.let { TierLegend(it, modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)) }
        // No band in a share image: it shows every item
        val showBand = !LocalShareCopy.current
        Canvas(modifier = Modifier.fillMaxWidth().height(height)) {
            val h = size.height
            val w = size.width
            // Room above and below the line for the labels
            val pad = 12.dp.toPx()
            val xScale = if (totalScrollWidth > 0f) w / totalScrollWidth else 1f
            fun xFor(i: Int) = (i * (itemWidth.toPx() + spacing.toPx()) + itemWidth.toPx() / 2) * xScale
            fun yFor(v: Double, d: ChartDomain? = domain): Float {
                val bottom = if (zeroBased) h - 1.5.dp.toPx() else h - pad
                return if (d != null && d.max > d.min) (bottom - (v - d.min) / (d.max - d.min) * (bottom - pad)).toFloat() else bottom
            }
            fun colorAt(i: Int) = pointColors?.getOrNull(i) ?: color

            drawNightAndMidnights(night, midnights, ::xFor, (itemWidth.toPx() + spacing.toPx()) * xScale)

            val points = values.mapIndexedNotNull { i, v -> if (v != null && v.isFinite()) i to Offset(xFor(i), yFor(v)) else null }
            val secondPoints = secondValues.orEmpty().mapIndexedNotNull { i, v ->
                if (v != null && v.isFinite() && secondDomain != null) i to Offset(xFor(i), yFor(v, secondDomain)) else null
            }
            // The boxes of the labels, so the key does not cover them
            val labelBoxes = mutableListOf<Rect>()

            // Labels near an edge grow inwards so they stay inside the chart. A label that would
            // overlap [avoid] (another label's box) goes next to it, on its row.
            fun drawLabel(text: String, i: Int, y: Float, labelColor: Color = colorAt(i), avoid: Rect? = null, avoidY: Float = y): Rect {
                // Ltr: keep the unit after the number in Arabic ("١٤°C", not "C°١٤")
                val layout = textMeasurer.measure(dualLabel(text, labelColor), TextStyle(color = labelColor, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = displayFont, textDirection = TextDirection.Ltr))
                val width = layout.size.width.toFloat()
                var left = chartLabelLeft(xFor(i), width, w, 4.dp.toPx())
                var top = y
                val gap = 6.dp.toPx()
                if (avoid != null && left < avoid.right + gap && left + width > avoid.left - gap) {
                    left = if (avoid.right + gap + width <= w) avoid.right + gap else (avoid.left - gap - width).coerceAtLeast(0f)
                    top = avoidY
                }
                val box = Rect(Offset(left, top.coerceIn(0f, h - layout.size.height)), Size(width, layout.size.height.toFloat()))
                drawText(layout, topLeft = box.topLeft)
                labelBoxes += box
                return box
            }

            // A missing value leaves a gap: each run of points with no missing value is its own curve
            fun drawCurve(points: List<Pair<Int, Offset>>, lineColor: Color, width: Float = ChartStyle.lineWidth.toPx(), effect: PathEffect? = null) {
                val runs = mutableListOf<MutableList<Offset>>()
                points.forEachIndexed { k, (i, p) ->
                    if (k == 0 || i != points[k - 1].first + 1) runs += mutableListOf<Offset>()
                    runs.last() += p
                }
                runs.filter { it.size >= 2 }.forEach { offsets ->
                    val cps = computeMonotoneCubicControlPoints(offsets)
                    val path = Path().apply {
                        moveTo(offsets[0].x, offsets[0].y)
                        cps.forEachIndexed { k, cp -> cubicTo(cp.cp1.x, cp.cp1.y, cp.cp2.x, cp.cp2.y, offsets[k + 1].x, offsets[k + 1].y) }
                    }
                    drawPath(path, lineColor, style = Stroke(width, pathEffect = effect))
                }
            }
            fun drawSecondLine() = drawCurve(secondPoints, secondColor, secondWidth.toPx(), if (secondDashed) chartDash(ChartStyle.dewPointDash) else null)

            if (style == OverviewStyle.BARS) {
                drawLine(Color.White.copy(alpha = 0.2f), Offset(0f, h - 0.5f), Offset(w, h - 0.5f), 1.dp.toPx())
                val barWidth = maxOf(2.dp.toPx(), itemWidth.toPx() * xScale * 0.6f)
                points.forEach { (i, p) ->
                    if ((values[i] ?: 0.0) > 0) {
                        drawRoundRect(colorAt(i), Offset(p.x - barWidth / 2, p.y), Size(barWidth, h - p.y), CornerRadius(1.5.dp.toPx()))
                    }
                }
                // The second line over the bars
                drawSecondLine()
                if (valid.isNotEmpty() && max <= 0 && emptyLabel != null) {
                    val layout = textMeasurer.measure(emptyLabel, TextStyle(color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = displayFont))
                    val topLeft = Offset((w - layout.size.width) / 2, (h - layout.size.height) / 2)
                    drawText(layout, topLeft = topLeft)
                    labelBoxes += Rect(topLeft, Size(layout.size.width.toFloat(), layout.size.height.toFloat()))
                }
                if (max > 0 && highLabel != null && highIndex >= 0) {
                    drawLabel(highLabel, highIndex, yFor(max) - 12.dp.toPx())
                }
            } else {
                // The second line first, so the main line stays on top
                drawSecondLine()
                if (freezingAt != null && domain != null && freezingAt > domain.min && freezingAt < domain.max) {
                    val fy = yFor(freezingAt)
                    drawLine(ChartStyle.freezing, Offset(0f, fy), Offset(w, fy), ChartStyle.freezingWidth.toPx(), pathEffect = chartDash(ChartStyle.freezingDash))
                }
                drawCurve(points, color.copy(alpha = if (pointColors != null) 0.5f else 1f))
                // Dots only for the colours of the tiers: a line of one colour has none, as in the chart view
                if (pointColors != null) points.forEach { (i, p) -> drawCircle(colorAt(i), 3.dp.toPx(), p) }
                val highY = yFor(max) - 14.dp.toPx()
                val highBox = if (highLabel != null && highIndex >= 0) drawLabel(highLabel, highIndex, highY) else null
                if (zeroBased) drawLine(Color.White.copy(alpha = 0.2f), Offset(0f, h - 0.5f), Offset(w, h - 0.5f), 1.dp.toPx())
                // Below the point, or above it when the point sits on the zero line
                val lowY = if (zeroBased) yFor(lowest) - 14.dp.toPx() else yFor(lowest) + 3.dp.toPx()
                val lowBox = if (lowLabel != null && lowIndex >= 0 && max > lowest) drawLabel(lowLabel, lowIndex, lowY) else null
                // The second line's labels last, so no line covers them
                val valid2 = secondValues.orEmpty().filterNotNull().filter { it.isFinite() }
                if (secondScale == SecondScale.OWN && secondDomain != null && valid2.isNotEmpty()) {
                    val vs = secondValues.orEmpty()
                    val hi2 = valid2.max()
                    val lo2 = valid2.min()
                    if (secondHighLabel != null) {
                        drawLabel(secondHighLabel, vs.indexOf(hi2), yFor(hi2, secondDomain) - 14.dp.toPx(), secondColor, highBox, highY)
                    }
                    if (secondLowLabel != null && hi2 > lo2) {
                        drawLabel(secondLowLabel, vs.indexOf(lo2), yFor(lo2, secondDomain) + 3.dp.toPx(), secondColor, lowBox, lowY)
                    }
                }
            }

            if (secondKey != null && secondPoints.isNotEmpty()) {
                drawChartKey(textMeasurer, displayFont, secondKey, secondColor, labelBoxes)
            }

            // The part of the row scrolled into view, as a light band
            if (showBand && totalScrollWidth > w) {
                val vpLeft = (scrollOffset / totalScrollWidth * w).coerceIn(0f, w)
                val vpRight = ((scrollOffset + w) / totalScrollWidth * w).coerceIn(0f, w)
                drawRoundRect(Color.White.copy(alpha = 0.06f), Offset(vpLeft, 0f), Size(vpRight - vpLeft, h), androidx.compose.ui.geometry.CornerRadius(6.dp.toPx()))
            }

            // The selected item; the bars are not a line, so they get no dot
            if (selected != null && selected in values.indices) {
                val main = if (style == OverviewStyle.LINE) points.firstOrNull { it.first == selected }?.second?.y else null
                val second = secondPoints.firstOrNull { it.first == selected }?.second?.y
                drawChartSelection(xFor(selected), listOf(second, main))
            }
        }
    }
}

/**
 * Hourly charts on the main page: a dark band over the night hours (the whole slot of each hour, [slot] px wide,
 * around its point at [x]) and a line at each local midnight, as in the chart view.
 */
internal fun DrawScope.drawNightAndMidnights(night: List<Boolean>, midnights: List<Int>, x: (Int) -> Float, slot: Float) {
    chartNightRuns(night).forEach { run ->
        val left = (x(run.first) - slot / 2).coerceAtLeast(0f)
        val right = (x(run.last) + slot / 2).coerceAtMost(size.width)
        if (right > left) drawRect(ChartStyle.night, Offset(left, 0f), Size(right - left, size.height))
    }
    midnights.forEach { i ->
        val mx = x(i)
        drawLine(ChartStyle.midnight, Offset(mx, 0f), Offset(mx, size.height), ChartStyle.midnightWidth.toPx())
    }
}

/** The selected item at [x]: a line from top to bottom, and a dot on each line at the heights [ys] (null: no value). */
internal fun DrawScope.drawChartSelection(x: Float, ys: List<Float?>) {
    drawLine(ChartStyle.selection, Offset(x, 0f), Offset(x, size.height), ChartStyle.selectionWidth.toPx())
    ys.forEach { y -> if (y != null) drawCircle(ChartStyle.selectionDot, ChartStyle.selectionDotRadius.toPx(), Offset(x, y)) }
}

/**
 * A small "● name" key in [color] at the top of a chart: at the end, or at the start when a box of [avoid]
 * is in the way. Left out when there is no room at either end.
 */
internal fun DrawScope.drawChartKey(textMeasurer: TextMeasurer, font: FontFamily, name: String, color: Color, avoid: List<Rect>) {
    val layout = textMeasurer.measure("● $name", TextStyle(color = color, fontSize = 8.sp, fontFamily = font, textDirection = TextDirection.Content))
    val pad = 4.dp.toPx()
    val keySize = Size(layout.size.width.toFloat(), layout.size.height.toFloat())
    if (keySize.width + 2 * pad > size.width) return
    val box = listOf(size.width - keySize.width - pad, pad)
        .map { Rect(Offset(it, 0f), keySize) }
        .firstOrNull { key -> avoid.none { it.overlaps(key) } } ?: return
    drawText(layout, topLeft = box.topLeft)
}

/**
 * Lets the user slide a finger along the chart in [content]: a label (the hour or the day of [label])
 * follows the finger, and the row of cards scrolls to the item under it. [content] gets the item under
 * the finger (null when there is none) and draws the line and the dots of it.
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
    content: @Composable (selected: Int?) -> Unit,
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
        scope.launch { scrollState.animateScrollTo((i * step + half - scrollState.viewportSize / 2f).roundToInt()) }
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
                val layout = textMeasurer.measure(label(i), TextStyle(color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = displayFont))
                val padX = 6.dp.toPx()
                val padY = 2.dp.toPx()
                val pillWidth = layout.size.width + padX * 2
                val left = (x - pillWidth / 2).coerceIn(0f, (w - pillWidth).coerceAtLeast(0f))
                drawRoundRect(Color.Black.copy(alpha = 0.75f), Offset(left, 0f), Size(pillWidth, layout.size.height + padY * 2), CornerRadius(6.dp.toPx()))
                drawText(layout, topLeft = Offset(left + padX, padY))
            },
    ) {
        content(index)
    }
}
