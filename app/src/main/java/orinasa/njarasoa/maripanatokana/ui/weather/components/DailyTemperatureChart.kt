package orinasa.njarasoa.maripanatokana.ui.weather.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import orinasa.njarasoa.maripanatokana.domain.model.DailyForecast
import java.util.Calendar

@Composable
fun DailyTemperatureChart(
    forecasts: List<DailyForecast>,
    metricPrimary: Boolean,
    modifier: Modifier = Modifier,
) {
    if (forecasts.isEmpty()) return

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

    val horizontalTicks = remember(paddedMin, paddedMax) {
        val start = Math.ceil(paddedMin).toInt()
        val end = Math.floor(paddedMax).toInt()
        (start..end).toList()
    }

    val mondayIndices = remember(forecasts) {
        val calendar = Calendar.getInstance()
        forecasts.mapIndexed { index, forecast ->
            calendar.timeInMillis = forecast.date
            if (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY) index else -1
        }.filter { it != -1 }
    }

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val height = size.height
            val width = size.width
            val pointsCount = forecasts.size
            if (pointsCount < 2) return@Canvas

            // Draw horizontal ticks
            horizontalTicks.forEach { temp ->
                val isMajor = temp % 5 == 0
                val y = height - ((temp - paddedMin) / paddedRange * height).toFloat()
                drawLine(
                    color = Color.White.copy(alpha = if (isMajor) 0.25f else 0.1f),
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = (if (isMajor) 0.8.dp else 0.5.dp).toPx()
                )
            }

            // Draw Monday vertical lines
            mondayIndices.forEach { idx ->
                val x = (idx.toFloat() / (pointsCount - 1)) * width
                drawLine(
                    color = Color.White.copy(alpha = 0.2f),
                    start = Offset(x, 0f),
                    end = Offset(x, height),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 2.dp.toPx()), 0f)
                )
            }

            val maxPath = Path()
            val minPath = Path()
            val areaPath = Path()

            val maxPoints = maxTemps.mapIndexed { i, temp ->
                val x = (i.toFloat() / (pointsCount - 1)) * width
                val y = height - ((temp - paddedMin) / paddedRange * height).toFloat()
                Offset(x, y)
            }

            val minPoints = minTemps.mapIndexed { i, temp ->
                val x = (i.toFloat() / (pointsCount - 1)) * width
                val y = height - ((temp - paddedMin) / paddedRange * height).toFloat()
                Offset(x, y)
            }

            maxPoints.forEachIndexed { i, point ->
                if (i == 0) {
                    maxPath.moveTo(point.x, point.y)
                    areaPath.moveTo(point.x, point.y)
                } else {
                    val prev = maxPoints[i - 1]
                    val cp1x = prev.x + (point.x - prev.x) / 2f
                    maxPath.cubicTo(cp1x, prev.y, cp1x, point.y, point.x, point.y)
                    areaPath.cubicTo(cp1x, prev.y, cp1x, point.y, point.x, point.y)
                }
            }

            // Go back along min points to close area
            for (i in minPoints.indices.reversed()) {
                val point = minPoints[i]
                if (i == minPoints.size - 1) {
                    areaPath.lineTo(point.x, point.y)
                } else {
                    val next = minPoints[i + 1]
                    val cp1x = point.x + (next.x - point.x) / 2f
                    areaPath.cubicTo(cp1x, next.y, cp1x, point.y, point.x, point.y)
                }
            }
            areaPath.close()

            minPoints.forEachIndexed { i, point ->
                if (i == 0) {
                    minPath.moveTo(point.x, point.y)
                } else {
                    val prev = minPoints[i - 1]
                    val cp1x = prev.x + (point.x - prev.x) / 2f
                    minPath.cubicTo(cp1x, prev.y, cp1x, point.y, point.x, point.y)
                }
            }

            // Draw area fill
            drawPath(
                path = areaPath,
                color = Color.White.copy(alpha = 0.1f)
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

            // Draw dots (as requested)
            maxPoints.forEach { point ->
                drawCircle(color = Color(0xFFFF7043), radius = 1.5.dp.toPx(), center = point)
            }
            minPoints.forEach { point ->
                drawCircle(color = Color(0xFF64B5F6), radius = 1.5.dp.toPx(), center = point)
            }
        }
    }
}
