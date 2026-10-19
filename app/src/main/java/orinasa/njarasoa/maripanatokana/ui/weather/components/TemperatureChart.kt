package orinasa.njarasoa.maripanatokana.ui.weather.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
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
import orinasa.njarasoa.maripanatokana.domain.model.HourlyForecast
import orinasa.njarasoa.maripanatokana.ui.theme.LocalDisplayFont
import java.util.Calendar

@Composable
fun TemperatureChart(
    forecasts: List<HourlyForecast>,
    metricPrimary: Boolean,
    itemWidth: Dp,
    spacing: Dp,
    modifier: Modifier = Modifier,
    lineColor: Color = Color.White,
    scrollOffset: Float = 0f,
    totalScrollWidth: Float = 0f,
    localizeDigits: (String) -> String = { it },
) {
    if (forecasts.isEmpty()) return

    val textMeasurer = rememberTextMeasurer()
    val displayFont = LocalDisplayFont.current

    val temps = remember(forecasts, metricPrimary) {
        forecasts.map { if (metricPrimary) it.temperature.celsius else it.temperature.fahrenheit }
    }

    val minTemp = remember(temps) { temps.minOrNull() ?: 0.0 }
    val maxTemp = remember(temps) { temps.maxOrNull() ?: 0.0 }
    val tempRange = remember(minTemp, maxTemp) { 
        val range = maxTemp - minTemp
        if (range == 0.0) 1.0 else range
    }

    // Add padding to range
    val paddedMin = minTemp - (tempRange * 0.15)
    val paddedMax = maxTemp + (tempRange * 0.15)
    val paddedRange = paddedMax - paddedMin

    val midnightIndices = remember(forecasts) {
        val calendar = Calendar.getInstance()
        forecasts.mapIndexed { index, forecast ->
            calendar.timeInMillis = forecast.time
            if (calendar.get(Calendar.HOUR_OF_DAY) == 0) index else -1
        }.filter { it != -1 }
    }

    val noonIndices = remember(forecasts) {
        val calendar = Calendar.getInstance()
        forecasts.mapIndexed { index, forecast ->
            calendar.timeInMillis = forecast.time
            if (calendar.get(Calendar.HOUR_OF_DAY) == 12) index else -1
        }.filter { it != -1 }
    }

    val gridColor = MaterialTheme.colorScheme.onSurface

    val highTemp = forecasts.map { it.temperature }.filter { it.celsius.isFinite() }.maxByOrNull { it.celsius }
    val lowTemp = forecasts.map { it.temperature }.filter { it.celsius.isFinite() }.minByOrNull { it.celsius }
    val summaryModifier = if (highTemp != null && lowTemp != null) {
        val high = if (metricPrimary) highTemp.displayCelsius() else highTemp.displayFahrenheit()
        val low = if (metricPrimary) lowTemp.displayCelsius() else lowTemp.displayFahrenheit()
        val description = stringResource(R.string.chart_temperature_summary, localizeDigits(high), localizeDigits(low))
        Modifier.semantics { contentDescription = description }
    } else Modifier

    Box(modifier = modifier.then(summaryModifier)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val height = size.height
            val width = size.width
            val pointsCount = temps.size
            if (pointsCount < 2) return@Canvas

            val itemWidthPx = itemWidth.toPx()
            val spacingPx = spacing.toPx()
            val xScale = if (totalScrollWidth > 0f) width / totalScrollWidth else 1f

            val midnightLabelStyle = TextStyle(color = gridColor.copy(alpha = 0.7f), fontSize = 8.sp)
            val noonLabelStyle = TextStyle(color = gridColor.copy(alpha = 0.45f), fontSize = 8.sp)

            // Draw Midnight vertical lines + label
            midnightIndices.forEach { idx ->
                val x = (idx * (itemWidthPx + spacingPx) + itemWidthPx / 2) * xScale
                drawLine(
                    color = gridColor.copy(alpha = 0.15f),
                    start = Offset(x, 0f),
                    end = Offset(x, height),
                    strokeWidth = 1.dp.toPx()
                )
                val layout = textMeasurer.measure("00:00", midnightLabelStyle)
                drawText(layout, topLeft = Offset(x - layout.size.width / 2f, 2.dp.toPx()))
            }

            // Draw Noon vertical lines + label
            noonIndices.forEach { idx ->
                val x = (idx * (itemWidthPx + spacingPx) + itemWidthPx / 2) * xScale
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

            val points = temps.mapIndexed { i, temp ->
                val x = (i * (itemWidthPx + spacingPx) + itemWidthPx / 2) * xScale
                val y = height - ((temp - paddedMin) / paddedRange * height).toFloat()
                Offset(x, y)
            }

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

            // Draw line - bright solid white
            drawPath(
                path = path,
                color = lineColor,
                style = Stroke(width = 2.dp.toPx())
            )

            // Draw dots at each hour
            points.forEach { point ->
                drawCircle(
                    color = lineColor,
                    radius = 3.dp.toPx(),
                    center = point
                )
            }

            // Draw peak max temperature label (below peak line)
            val minY = points.minOf { it.y }
            val maxIndices = points.indices.filter { points[it].y == minY }
            val maxMiddleIdx = maxIndices[maxIndices.size / 2]
            val maxPoint = points[maxMiddleIdx]
            val maxTempObj = forecasts[maxMiddleIdx].temperature
            val maxTempText = maxTempObj.displayDual(metricPrimary).let { (p, s) -> "$p · $s" }
            val maxLabelStyle = TextStyle(color = Color(0xFFFF7043), fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = displayFont)
            val maxLayout = textMeasurer.measure(maxTempText, maxLabelStyle)
            val maxTextX = (maxPoint.x - maxLayout.size.width / 2f).coerceIn(4.dp.toPx(), width - maxLayout.size.width - 4.dp.toPx())
            val maxTextY = (maxPoint.y + 4.dp.toPx()).coerceIn(2.dp.toPx(), height - maxLayout.size.height)
            drawText(maxLayout, topLeft = Offset(maxTextX, maxTextY))

            // Draw trough min temperature label (above trough line)
            val maxY = points.maxOf { it.y }
            val minIndices = points.indices.filter { points[it].y == maxY }
            val minMiddleIdx = minIndices[minIndices.size / 2]
            val minPoint = points[minMiddleIdx]
            val minTempObj = forecasts[minMiddleIdx].temperature
            val minTempText = minTempObj.displayDual(metricPrimary).let { (p, s) -> "$p · $s" }
            val minLabelStyle = TextStyle(color = Color(0xFF64B5F6), fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = displayFont)
            val minLayout = textMeasurer.measure(minTempText, minLabelStyle)
            val minTextX = (minPoint.x - minLayout.size.width / 2f).coerceIn(4.dp.toPx(), width - minLayout.size.width - 4.dp.toPx())
            val minTextY = (minPoint.y - minLayout.size.height - 2.dp.toPx()).coerceAtLeast(0f)
            if (Math.abs(maxPoint.x - minPoint.x) > 30.dp.toPx() || Math.abs(maxTextY - minTextY) > 12.dp.toPx()) {
                drawText(minLayout, topLeft = Offset(minTextX, minTextY))
            }

            // The part of the row scrolled into view, as a light band
            if (totalScrollWidth > width) {
                val vpLeft = (scrollOffset / totalScrollWidth * width).coerceIn(0f, width)
                val vpRight = ((scrollOffset + width) / totalScrollWidth * width).coerceIn(0f, width)
                drawRoundRect(Color.White.copy(alpha = 0.06f), Offset(vpLeft, 0f), Size(vpRight - vpLeft, height), androidx.compose.ui.geometry.CornerRadius(6.dp.toPx()))
            }
        }
    }
}
