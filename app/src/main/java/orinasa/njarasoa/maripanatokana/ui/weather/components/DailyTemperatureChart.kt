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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import orinasa.njarasoa.maripanatokana.domain.model.DailyForecast
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

    val gridColor = MaterialTheme.colorScheme.onSurface
    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val height = size.height
            val width = size.width
            val pointsCount = forecasts.size
            if (pointsCount < 2) return@Canvas

            val itemWidthPx = itemWidth.toPx()
            val spacingPx = spacing.toPx()
            val xScale = if (totalScrollWidth > 0f) width / totalScrollWidth else 1f
            fun xFor(i: Int) = (i * (itemWidthPx + spacingPx) + itemWidthPx / 2) * xScale

            // Draw horizontal ticks
            horizontalTicks.forEach { temp ->
                val isMajor = temp % 5 == 0
                val y = height - ((temp - paddedMin) / paddedRange * height).toFloat()
                drawLine(
                    color = gridColor.copy(alpha = if (isMajor) 0.25f else 0.1f),
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = (if (isMajor) 0.8.dp else 0.5.dp).toPx()
                )
            }

            // Draw Monday vertical lines
            mondayIndices.forEach { idx ->
                val x = xFor(idx)
                drawLine(
                    color = gridColor.copy(alpha = 0.2f),
                    start = Offset(x, 0f),
                    end = Offset(x, height),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 2.dp.toPx()), 0f)
                )
            }

            val maxPoints = maxTemps.mapIndexed { i, temp ->
                val x = xFor(i)
                val y = height - ((temp - paddedMin) / paddedRange * height).toFloat()
                Offset(x, y)
            }

            val minPoints = minTemps.mapIndexed { i, temp ->
                val x = xFor(i)
                val y = height - ((temp - paddedMin) / paddedRange * height).toFloat()
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

            // Viewport overlay: dim the parts outside the currently visible window
            if (totalScrollWidth > width) {
                val vpLeft = (scrollOffset / totalScrollWidth * width).coerceIn(0f, width)
                val vpRight = ((scrollOffset + width) / totalScrollWidth * width).coerceIn(0f, width)
                if (vpLeft > 0f) {
                    drawRect(
                        color = Color.Black.copy(alpha = 0.35f),
                        topLeft = Offset(0f, 0f),
                        size = Size(vpLeft, height)
                    )
                }
                if (vpRight < width) {
                    drawRect(
                        color = Color.Black.copy(alpha = 0.35f),
                        topLeft = Offset(vpRight, 0f),
                        size = Size(width - vpRight, height)
                    )
                }
                drawLine(
                    color = gridColor.copy(alpha = 0.7f),
                    start = Offset(vpLeft, 0f),
                    end = Offset(vpLeft, height),
                    strokeWidth = 1.5.dp.toPx()
                )
                drawLine(
                    color = gridColor.copy(alpha = 0.7f),
                    start = Offset(vpRight, 0f),
                    end = Offset(vpRight, height),
                    strokeWidth = 1.5.dp.toPx()
                )
            }
        }
    }
}
