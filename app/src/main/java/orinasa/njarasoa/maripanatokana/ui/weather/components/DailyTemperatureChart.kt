// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

package orinasa.njarasoa.maripanatokana.ui.weather.components

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
import orinasa.njarasoa.maripanatokana.ui.theme.LocalDisplayFont
import java.util.Calendar

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

    val allTemps = maxTemps + minTemps
    val minTemp = remember(allTemps) { allTemps.minOrNull() ?: 0.0 }
    val maxTemp = remember(allTemps) { allTemps.maxOrNull() ?: 0.0 }
    val tempRange = remember(minTemp, maxTemp) {
        val range = maxTemp - minTemp
        if (range == 0.0) 1.0 else range
    }

    // Add padding to range
    val paddedMin = minTemp - (tempRange * 0.2)
    val paddedMax = maxTemp + (tempRange * 0.2)
    val paddedRange = paddedMax - paddedMin

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
            if (pointsCount < 2) return@Canvas

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
                style = Stroke(width = 2.dp.toPx())
            )

            // Draw low line
            drawPath(
                path = minPath,
                color = Color(0xFF64B5F6),
                style = Stroke(width = 2.dp.toPx())
            )

            // Draw dots
            maxPoints.forEach { point ->
                drawCircle(color = Color(0xFFFF7043), radius = 3.dp.toPx(), center = point)
            }
            minPoints.forEach { point ->
                drawCircle(color = Color(0xFF64B5F6), radius = 3.dp.toPx(), center = point)
            }

            // The warmest high above its point, the coldest low below its point
            fun drawLabel(text: String, color: Color, point: Offset, above: Boolean) {
                val layout = textMeasurer.measure(dualLabel(localizeDigits(text), color), TextStyle(color = color, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = displayFont))
                val x = (point.x - layout.size.width / 2f).coerceIn(4.dp.toPx(), width - layout.size.width - 4.dp.toPx())
                val y = if (above) (point.y - layout.size.height - 2.dp.toPx()).coerceAtLeast(0f)
                    else (point.y + 2.dp.toPx()).coerceAtMost(height - layout.size.height)
                drawText(layout, topLeft = Offset(x, y))
            }
            val warmest = maxTemps.indices.maxByOrNull { maxTemps[it] }
            val coldest = minTemps.indices.minByOrNull { minTemps[it] }
            if (warmest != null) drawLabel(forecasts[warmest].tempMax.displayDual(metricPrimary).let { (p, s) -> "$p · $s" }, Color(0xFFFF7043), maxPoints[warmest], above = true)
            if (coldest != null) drawLabel(forecasts[coldest].tempMin.displayDual(metricPrimary).let { (p, s) -> "$p · $s" }, Color(0xFF64B5F6), minPoints[coldest], above = false)

            // The part of the row scrolled into view, as a light band
            if (showBand && totalScrollWidth > width) {
                val vpLeft = (scrollOffset / totalScrollWidth * width).coerceIn(0f, width)
                val vpRight = ((scrollOffset + width) / totalScrollWidth * width).coerceIn(0f, width)
                drawRoundRect(Color.White.copy(alpha = 0.06f), Offset(vpLeft, 0f), Size(vpRight - vpLeft, height), androidx.compose.ui.geometry.CornerRadius(6.dp.toPx()))
            }
        }
    }
}
