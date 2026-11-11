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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
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
import orinasa.njarasoa.maripanatokana.domain.model.DailyForecast
import orinasa.njarasoa.maripanatokana.domain.model.NormalTemps
import orinasa.njarasoa.maripanatokana.ui.theme.LocalDisplayFont
import java.util.Calendar
import orinasa.njarasoa.maripanatokana.ui.weather.chart.ChartStyle
import orinasa.njarasoa.maripanatokana.ui.weather.chart.chartDash
import orinasa.njarasoa.maripanatokana.ui.weather.chart.chartDomain

@Composable
fun DailyTemperatureChart(
    forecasts: List<DailyForecast>,
    metricPrimary: Boolean,
    itemWidth: Dp,
    spacing: Dp,
    modifier: Modifier = Modifier,
    scrollOffset: Float = 0f,
    totalScrollWidth: Float = 0f,
    localizeDigits: (String) -> String = { it },
    // The normal high and low of each day, for two faint dotted lines; empty or null where there are none
    normals: List<NormalTemps?> = emptyList(),
    // The day under the finger: a line, and a dot on the high and the low
    selected: Int? = null,
) {
    if (forecasts.isEmpty()) return

    val textMeasurer = rememberTextMeasurer()
    val displayFont = LocalDisplayFont.current

    val maxTemps = remember(forecasts, metricPrimary) {
        forecasts.map { if (metricPrimary) it.tempMax.celsius else it.tempMax.fahrenheit }
    }
    val minTemps = remember(forecasts, metricPrimary) {
        forecasts.map { if (metricPrimary) it.tempMin.celsius else it.tempMin.fahrenheit }
    }

    val normalHighs = remember(normals, metricPrimary) {
        normals.map { n -> n?.high?.let { if (metricPrimary) it else it * 9 / 5 + 32 } }
    }
    val normalLows = remember(normals, metricPrimary) {
        normals.map { n -> n?.low?.let { if (metricPrimary) it else it * 9 / 5 + 32 } }
    }
    val normalLabel = stringResource(R.string.normal_label)

    // The normals are in the range too, so their lines stay inside the chart. Round ends, as in the chart view
    val allTemps = maxTemps + minTemps + normalHighs + normalLows
    val domain = remember(allTemps, metricPrimary) { chartDomain(allTemps, fromZero = false, minSpan = if (metricPrimary) 5.0 else 9.0) }

    val mondayIndices = remember(forecasts) {
        val calendar = Calendar.getInstance()
        forecasts.mapIndexed { index, forecast ->
            calendar.timeInMillis = forecast.date
            if (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY) index else -1
        }.filter { it != -1 }
    }

    val gridColor = MaterialTheme.colorScheme.onSurface
    val highTemp = forecasts.map { it.tempMax }.filter { it.celsius.isFinite() }.maxByOrNull { it.celsius }
    val lowTemp = forecasts.map { it.tempMin }.filter { it.celsius.isFinite() }.minByOrNull { it.celsius }
    val summaryModifier = if (highTemp != null && lowTemp != null) {
        val high = if (metricPrimary) highTemp.displayCelsius() else highTemp.displayFahrenheit()
        val low = if (metricPrimary) lowTemp.displayCelsius() else lowTemp.displayFahrenheit()
        val description = stringResource(R.string.chart_daily_temperature_summary, localizeDigits(high), localizeDigits(low))
        Modifier.semantics { contentDescription = description }
    } else Modifier

    Box(modifier = modifier.then(summaryModifier)) {
        // No band in a share image: it shows every item
        val showBand = !LocalShareCopy.current
        Canvas(modifier = Modifier.fillMaxSize()) {
            val height = size.height
            val width = size.width
            val pointsCount = forecasts.size
            if (pointsCount < 2 || domain == null) return@Canvas
            val paddedMin = domain.min
            val paddedRange = domain.max - domain.min

            val itemWidthPx = itemWidth.toPx()
            val spacingPx = spacing.toPx()
            val xScale = if (totalScrollWidth > 0f) width / totalScrollWidth else 1f
            fun xFor(i: Int) = (i * (itemWidthPx + spacingPx) + itemWidthPx / 2) * xScale

            // Draw Monday vertical lines
            mondayIndices.forEach { idx ->
                val x = xFor(idx)
                drawLine(
                    color = gridColor.copy(alpha = 0.1f),
                    start = Offset(x, 0f),
                    end = Offset(x, height),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 2.dp.toPx()), 0f)
                )
            }

            // Room above the highs for the warmest's label and below the lows for the coldest's
            val labelRoom = 13.dp.toPx()
            // A faint line at freezing (0 °C, 32 °F) when it is inside the chart
            val freezing = if (metricPrimary) 0.0 else 32.0
            if (freezing > domain.min && freezing < domain.max) {
                val fy = labelRoom + (height - 2 * labelRoom) * (1 - ((freezing - paddedMin) / paddedRange).toFloat())
                drawLine(ChartStyle.freezing, Offset(0f, fy), Offset(width, fy), ChartStyle.freezingWidth.toPx(), pathEffect = chartDash(ChartStyle.freezingDash))
            }
            // The normals: faint lines with shorter dashes (2 3) than the freezing line (4 3)
            fun normalPoints(values: List<Double?>) = values.take(pointsCount).mapIndexed { i, temp ->
                temp?.let { Offset(xFor(i), labelRoom + (height - 2 * labelRoom) * (1 - ((it - paddedMin) / paddedRange).toFloat())) }
            }
            val normalHighPoints = normalPoints(normalHighs)
            val normalLowPoints = normalPoints(normalLows)
            val normalDots = chartDash(ChartStyle.normalDash)
            listOf(normalHighPoints to ChartStyle.normalHigh, normalLowPoints to ChartStyle.normalLow).forEach { (points, color) ->
                points.zipWithNext().forEach { (a, b) ->
                    if (a != null && b != null) drawLine(color, a, b, ChartStyle.normalWidth.toPx(), pathEffect = normalDots)
                }
            }

            val maxPoints = maxTemps.mapIndexed { i, temp ->
                val x = xFor(i)
                val y = labelRoom + (height - 2 * labelRoom) * (1 - ((temp - paddedMin) / paddedRange).toFloat())
                Offset(x, y)
            }

            val minPoints = minTemps.mapIndexed { i, temp ->
                val x = xFor(i)
                val y = labelRoom + (height - 2 * labelRoom) * (1 - ((temp - paddedMin) / paddedRange).toFloat())
                Offset(x, y)
            }

            val maxControlPoints = computeMonotoneCubicControlPoints(maxPoints)
            val minControlPoints = computeMonotoneCubicControlPoints(minPoints)

            val maxPath = Path().apply {
                moveTo(maxPoints[0].x, maxPoints[0].y)
                for (i in 0 until maxControlPoints.size) {
                    val cp = maxControlPoints[i]
                    val pNext = maxPoints[i + 1]
                    cubicTo(cp.cp1.x, cp.cp1.y, cp.cp2.x, cp.cp2.y, pNext.x, pNext.y)
                }
            }

            val minPath = Path().apply {
                moveTo(minPoints[0].x, minPoints[0].y)
                for (i in 0 until minControlPoints.size) {
                    val cp = minControlPoints[i]
                    val pNext = minPoints[i + 1]
                    cubicTo(cp.cp1.x, cp.cp1.y, cp.cp2.x, cp.cp2.y, pNext.x, pNext.y)
                }
            }

            val areaPath = Path().apply {
                moveTo(maxPoints[0].x, maxPoints[0].y)
                for (i in 0 until maxControlPoints.size) {
                    val cp = maxControlPoints[i]
                    val pNext = maxPoints[i + 1]
                    cubicTo(cp.cp1.x, cp.cp1.y, cp.cp2.x, cp.cp2.y, pNext.x, pNext.y)
                }
                lineTo(minPoints.last().x, minPoints.last().y)
                for (i in minControlPoints.indices.reversed()) {
                    val cp = minControlPoints[i]
                    val pPrev = minPoints[i]
                    cubicTo(cp.cp2.x, cp.cp2.y, cp.cp1.x, cp.cp1.y, pPrev.x, pPrev.y)
                }
                close()
            }

            // Draw area fill
            drawPath(
                path = areaPath,
                color = gridColor.copy(alpha = 0.1f)
            )

            // Draw high line
            drawPath(
                path = maxPath,
                color = Color(0xFFFF7043),
                style = Stroke(width = ChartStyle.lineWidth.toPx())
            )

            // Draw low line
            drawPath(
                path = minPath,
                color = Color(0xFF64B5F6),
                style = Stroke(width = ChartStyle.lineWidth.toPx())
            )
            // No dot at each day, as in the chart view

            // The warmest high above its point, the coldest low below its point
            fun drawLabel(text: String, color: Color, point: Offset, above: Boolean) {
                // Ltr: keep the unit after the number in Arabic ("١٤°C", not "C°١٤")
                val layout = textMeasurer.measure(dualLabel(localizeDigits(text), color), TextStyle(color = color, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = displayFont, textDirection = TextDirection.Ltr))
                val x = chartLabelLeft(point.x, layout.size.width.toFloat(), width, 4.dp.toPx())
                val y = if (above) (point.y - layout.size.height - 2.dp.toPx()).coerceAtLeast(0f)
                    else (point.y + 2.dp.toPx()).coerceAtMost(height - layout.size.height)
                drawText(layout, topLeft = Offset(x, y))
            }
            val warmest = maxTemps.indices.maxByOrNull { maxTemps[it] }
            val coldest = minTemps.indices.minByOrNull { minTemps[it] }
            if (warmest != null) drawLabel(forecasts[warmest].tempMax.displayDual(metricPrimary).let { (p, s) -> "$p · $s" }, Color(0xFFFF8A65), maxPoints[warmest], above = true)
            if (coldest != null) drawLabel(forecasts[coldest].tempMin.displayDual(metricPrimary).let { (p, s) -> "$p · $s" }, Color(0xFF64B5F6), minPoints[coldest], above = false)

            // "Normal" with a short dashed sample, in the free space above the highs, at the end away from the warmest label
            if (normalHighPoints.any { it != null } || normalLowPoints.any { it != null }) {
                val layout = textMeasurer.measure(normalLabel, TextStyle(color = gridColor.copy(alpha = 0.6f), fontSize = 8.sp, fontFamily = displayFont))
                val pad = 4.dp.toPx()
                val sample = 10.dp.toPx()
                val gap = 3.dp.toPx()
                val labelWidth = sample + gap + layout.size.width
                val atEnd = (warmest ?: 0) < pointsCount / 2
                val left = if (atEnd) width - labelWidth - pad else pad
                val y = ((labelRoom - layout.size.height) / 2).coerceAtLeast(0f)
                val midY = y + layout.size.height / 2f
                drawLine(ChartStyle.normalHigh, Offset(left, midY), Offset(left + sample, midY), ChartStyle.normalWidth.toPx(), pathEffect = normalDots)
                drawText(layout, topLeft = Offset(left + sample + gap, y))
            }

            // The part of the row scrolled into view, as a light band
            if (showBand && totalScrollWidth > width) {
                val vpLeft = (scrollOffset / totalScrollWidth * width).coerceIn(0f, width)
                val vpRight = ((scrollOffset + width) / totalScrollWidth * width).coerceIn(0f, width)
                drawRoundRect(Color.White.copy(alpha = 0.06f), Offset(vpLeft, 0f), Size(vpRight - vpLeft, height), androidx.compose.ui.geometry.CornerRadius(6.dp.toPx()))
            }

            if (selected != null && selected in maxPoints.indices) {
                drawChartSelection(xFor(selected), listOf(maxPoints[selected].y, minPoints[selected].y))
            }
        }
    }
}
