// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

package orinasa.njarasoa.maripanatokana.ui.weather.components

import androidx.compose.ui.text.style.TextDirection
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import orinasa.njarasoa.maripanatokana.ui.weather.LocalShareCopy
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import orinasa.njarasoa.maripanatokana.R
import orinasa.njarasoa.maripanatokana.domain.model.HourlyForecast
import orinasa.njarasoa.maripanatokana.ui.theme.LocalDisplayFont
import orinasa.njarasoa.maripanatokana.ui.weather.chart.ChartStyle
import orinasa.njarasoa.maripanatokana.ui.weather.chart.chartDash
import orinasa.njarasoa.maripanatokana.ui.weather.chart.chartDomain
import orinasa.njarasoa.maripanatokana.ui.weather.chart.chartMidnights

/**
 * The 24 hours' temperature under the hourly cards, with the dew point as a thin line on the same scale,
 * the night hours darker, and a line at each local midnight.
 */
@Composable
fun TemperatureChart(
    forecasts: List<HourlyForecast>,
    metricPrimary: Boolean,
    itemWidth: Dp,
    spacing: Dp,
    modifier: Modifier = Modifier,
    lineColor: Color = ChartStyle.temperature,
    scrollOffset: Float = 0f,
    totalScrollWidth: Float = 0f,
    localizeDigits: (String) -> String = { it },
    // The place's UTC offset, for the midnight and noon lines
    utcOffsetSeconds: Int = 0,
    // The night flag of each hour, for a dark band
    night: List<Boolean> = emptyList(),
    // The hour under the finger: a line, and a dot on each line
    selected: Int? = null,
) {
    if (forecasts.isEmpty()) return

    val textMeasurer = rememberTextMeasurer()
    val displayFont = LocalDisplayFont.current

    val temps = remember(forecasts, metricPrimary) {
        forecasts.map { if (metricPrimary) it.temperature.celsius else it.temperature.fahrenheit }
    }
    val dews = remember(forecasts, metricPrimary) {
        forecasts.map { f -> f.dewPoint?.takeIf { it.celsius.isFinite() }?.let { if (metricPrimary) it.celsius else it.fahrenheit } }
    }

    // One scale for both lines, with round ends as in the chart view. When the dew point is far below the
    // temperature, the scale grows to keep it, and the temperature line uses less of the height.
    val domain = remember(temps, dews, metricPrimary) { chartDomain(temps + dews, fromZero = false, minSpan = if (metricPrimary) 5.0 else 9.0) }

    val times = remember(forecasts) { forecasts.map { it.time } }
    val midnightIndices = remember(times, utcOffsetSeconds) { chartMidnights(times, utcOffsetSeconds) }
    // Noon at the place, as the midnights
    val noonIndices = remember(times, utcOffsetSeconds) {
        times.indices.filter { Math.floorMod((times[it] + utcOffsetSeconds * 1000L) / 3_600_000L, 24L) == 12L }
    }

    val gridColor = MaterialTheme.colorScheme.onSurface
    val dewKey = stringResource(R.string.detail_dewpoint)

    val highTemp = forecasts.map { it.temperature }.filter { it.celsius.isFinite() }.maxByOrNull { it.celsius }
    val lowTemp = forecasts.map { it.temperature }.filter { it.celsius.isFinite() }.minByOrNull { it.celsius }
    val summaryModifier = if (highTemp != null && lowTemp != null) {
        val high = if (metricPrimary) highTemp.displayCelsius() else highTemp.displayFahrenheit()
        val low = if (metricPrimary) lowTemp.displayCelsius() else lowTemp.displayFahrenheit()
        val description = stringResource(R.string.chart_temperature_summary, localizeDigits(high), localizeDigits(low))
        Modifier.semantics { contentDescription = description }
    } else Modifier

    Box(modifier = modifier.then(summaryModifier)) {
        // No band in a share image: it shows every item
        val showBand = !LocalShareCopy.current
        Canvas(modifier = Modifier.fillMaxSize()) {
            val height = size.height
            val width = size.width
            val pointsCount = temps.size
            if (pointsCount < 2 || domain == null) return@Canvas

            val itemWidthPx = itemWidth.toPx()
            val spacingPx = spacing.toPx()
            val xScale = if (totalScrollWidth > 0f) width / totalScrollWidth else 1f
            fun xFor(i: Int) = (i * (itemWidthPx + spacingPx) + itemWidthPx / 2) * xScale
            // Room above the line for the high's label and below it for the low's
            val labelRoom = 13.dp.toPx()
            fun yFor(v: Double) = labelRoom + (height - 2 * labelRoom) * (1 - ((v - domain.min) / (domain.max - domain.min)).toFloat())

            val midnightLabelStyle = TextStyle(color = gridColor.copy(alpha = 0.7f), fontSize = 8.sp)
            val noonLabelStyle = TextStyle(color = gridColor.copy(alpha = 0.45f), fontSize = 8.sp)

            // The night hours and a line at each midnight, with its label
            drawNightAndMidnights(night, midnightIndices, ::xFor, (itemWidthPx + spacingPx) * xScale)
            midnightIndices.forEach { idx ->
                val layout = textMeasurer.measure("00:00", midnightLabelStyle)
                drawText(layout, topLeft = Offset(xFor(idx) - layout.size.width / 2f, 2.dp.toPx()))
            }

            // Draw Noon vertical lines + label
            noonIndices.forEach { idx ->
                val x = xFor(idx)
                drawLine(
                    color = gridColor.copy(alpha = 0.1f),
                    start = Offset(x, 0f),
                    end = Offset(x, height),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx()), 0f)
                )
                val layout = textMeasurer.measure("12:00", noonLabelStyle)
                drawText(layout, topLeft = Offset(x - layout.size.width / 2f, 2.dp.toPx()))
            }

            // A faint line at freezing (0 °C, 32 °F) when it is inside the chart
            val freezing = if (metricPrimary) 0.0 else 32.0
            if (freezing > domain.min && freezing < domain.max) {
                val fy = yFor(freezing)
                drawLine(ChartStyle.freezing, Offset(0f, fy), Offset(width, fy), ChartStyle.freezingWidth.toPx(), pathEffect = chartDash(ChartStyle.freezingDash))
            }
            val points = temps.mapIndexed { i, temp -> Offset(xFor(i), yFor(temp)) }
            val dewPoints = dews.map { dew -> dew?.let { yFor(it) } }

            val controlPoints = computeMonotoneCubicControlPoints(points)

            val path = Path().apply {
                moveTo(points[0].x, points[0].y)
                for (i in 0 until controlPoints.size) {
                    val cp = controlPoints[i]
                    val pNext = points[i + 1]
                    cubicTo(cp.cp1.x, cp.cp1.y, cp.cp2.x, cp.cp2.y, pNext.x, pNext.y)
                }
            }

            val fillPath = Path().apply {
                moveTo(points[0].x, height)
                lineTo(points[0].x, points[0].y)
                for (i in 0 until controlPoints.size) {
                    val cp = controlPoints[i]
                    val pNext = points[i + 1]
                    cubicTo(cp.cp1.x, cp.cp1.y, cp.cp2.x, cp.cp2.y, pNext.x, pNext.y)
                }
                lineTo(points.last().x, height)
                close()
            }

            // Draw area fill
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(lineColor.copy(alpha = 0.15f), Color.Transparent),
                    startY = 0f,
                    endY = height
                )
            )

            // The dew point under the temperature line; a missing value leaves a gap
            drawThinLine(dewPoints, ::xFor, ChartStyle.dewPoint)

            // The temperature line, with no dot at each hour, as in the chart view
            drawPath(
                path = path,
                color = lineColor,
                style = Stroke(width = ChartStyle.lineWidth.toPx())
            )

            // The high's label above its peak
            val minY = points.minOf { it.y }
            val maxIndices = points.indices.filter { points[it].y == minY }
            val maxMiddleIdx = maxIndices[maxIndices.size / 2]
            val maxPoint = points[maxMiddleIdx]
            val maxTempObj = forecasts[maxMiddleIdx].temperature
            val maxTempText = maxTempObj.displayDual(metricPrimary).let { (p, s) -> "$p · $s" }
            // Ltr: keep the unit after the number in Arabic ("١٤°C", not "C°١٤")
            val maxLabelStyle = TextStyle(color = Color(0xFFFF8A65), fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = displayFont, textDirection = TextDirection.Ltr)
            val maxLayout = textMeasurer.measure(dualLabel(maxTempText, maxLabelStyle.color), maxLabelStyle)
            // Near an edge the label grows inwards, as on the web, so it does not cover the line or pass the edge
            val maxTextX = chartLabelLeft(maxPoint.x, maxLayout.size.width.toFloat(), width, 4.dp.toPx())
            // Above the peak, or under the line where it would cover a 00:00 or 12:00 label
            val timeLabelBoxes = (midnightIndices.map { it to "00:00" } + noonIndices.map { it to "12:00" }).map { (idx, text) ->
                val x = xFor(idx)
                val layout = textMeasurer.measure(text, midnightLabelStyle)
                Rect(Offset(x - layout.size.width / 2f, 2.dp.toPx()), Size(layout.size.width.toFloat(), layout.size.height.toFloat()))
            }
            val maxTextAboveY = (maxPoint.y - maxLayout.size.height - 2.dp.toPx()).coerceAtLeast(0f)
            val aboveBox = Rect(Offset(maxTextX, maxTextAboveY), Size(maxLayout.size.width.toFloat(), maxLayout.size.height.toFloat()))
            val maxTextY = if (timeLabelBoxes.any { it.overlaps(aboveBox) }) maxPoint.y + 4.dp.toPx() else maxTextAboveY
            drawText(maxLayout, topLeft = Offset(maxTextX, maxTextY))

            // The low's label below its trough
            val maxY = points.maxOf { it.y }
            val minIndices = points.indices.filter { points[it].y == maxY }
            val minMiddleIdx = minIndices[minIndices.size / 2]
            val minPoint = points[minMiddleIdx]
            val minTempObj = forecasts[minMiddleIdx].temperature
            val minTempText = minTempObj.displayDual(metricPrimary).let { (p, s) -> "$p · $s" }
            val minLabelStyle = TextStyle(color = Color(0xFF64B5F6), fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = displayFont, textDirection = TextDirection.Ltr)
            val minLayout = textMeasurer.measure(dualLabel(minTempText, minLabelStyle.color), minLabelStyle)
            val minTextX = chartLabelLeft(minPoint.x, minLayout.size.width.toFloat(), width, 4.dp.toPx())
            val minTextY = (minPoint.y + 2.dp.toPx()).coerceAtMost(height - minLayout.size.height)
            if (Math.abs(maxPoint.x - minPoint.x) > 30.dp.toPx() || Math.abs(maxTextY - minTextY) > 12.dp.toPx()) {
                drawText(minLayout, topLeft = Offset(minTextX, minTextY))
            }

            // The dew point key in the row of the time labels, where nothing is in the way
            if (dewPoints.any { it != null }) {
                val highBox = Rect(Offset(maxTextX, maxTextY), Size(maxLayout.size.width.toFloat(), maxLayout.size.height.toFloat()))
                drawChartKey(textMeasurer, displayFont, dewKey, ChartStyle.dewPoint, timeLabelBoxes + highBox)
            }

            // The part of the row scrolled into view, as a light band
            if (showBand && totalScrollWidth > width) {
                val vpLeft = (scrollOffset / totalScrollWidth * width).coerceIn(0f, width)
                val vpRight = ((scrollOffset + width) / totalScrollWidth * width).coerceIn(0f, width)
                drawRoundRect(Color.White.copy(alpha = 0.06f), Offset(vpLeft, 0f), Size(vpRight - vpLeft, height), androidx.compose.ui.geometry.CornerRadius(6.dp.toPx()))
            }

            if (selected != null && selected in points.indices) {
                drawChartSelection(xFor(selected), listOf(dewPoints[selected], points[selected].y))
            }
        }
    }
}

/** A thin line through the heights [ys] (one for each item at [x]); a missing value leaves a gap, and each run is a monotone curve. */
private fun DrawScope.drawThinLine(ys: List<Float?>, x: (Int) -> Float, color: Color) {
    var run = mutableListOf<Offset>()
    fun flush() {
        if (run.size >= 2) {
            val cps = computeMonotoneCubicControlPoints(run)
            val path = Path().apply {
                moveTo(run[0].x, run[0].y)
                cps.forEachIndexed { k, cp -> cubicTo(cp.cp1.x, cp.cp1.y, cp.cp2.x, cp.cp2.y, run[k + 1].x, run[k + 1].y) }
            }
            drawPath(path, color, style = Stroke(ChartStyle.thinLineWidth.toPx()))
        }
        run = mutableListOf()
    }
    ys.forEachIndexed { i, y -> if (y == null) flush() else run += Offset(x(i), y) }
    flush()
}
