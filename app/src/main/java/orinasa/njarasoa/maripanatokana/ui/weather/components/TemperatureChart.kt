package orinasa.njarasoa.maripanatokana.ui.weather.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import orinasa.njarasoa.maripanatokana.domain.model.HourlyForecast
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
) {
    if (forecasts.isEmpty()) return

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

    val horizontalTicks = remember(paddedMin, paddedMax, minTemp, maxTemp) {
        val start = Math.ceil(paddedMin).toInt()
        val end = Math.floor(paddedMax).toInt()
        (start..end).filter { temp ->
            Math.abs(temp - minTemp) >= 0.2 && Math.abs(temp - maxTemp) >= 0.2
        }
    }

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

    val textMeasurer = rememberTextMeasurer()

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val height = size.height
            val width = size.width
            val pointsCount = temps.size
            if (pointsCount < 2) return@Canvas

            val itemWidthPx = itemWidth.toPx()
            val spacingPx = spacing.toPx()
            val xScale = if (totalScrollWidth > 0f) width / totalScrollWidth else 1f

            // Draw horizontal ticks
            horizontalTicks.forEach { temp ->
                val y = height - ((temp - paddedMin) / paddedRange * height).toFloat()
                drawLine(
                    color = Color.White.copy(alpha = 0.1f),
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 0.5.dp.toPx()
                )
            }

            // Standout Min/Max Lines
            listOf(minTemp, maxTemp).forEach { temp ->
                val y = height - ((temp - paddedMin) / paddedRange * height).toFloat()
                drawLine(
                    color = Color.White.copy(alpha = 0.3f),
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 0.8.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 1.dp.toPx()), 0f)
                )
            }

            val midnightLabelStyle = TextStyle(color = Color.White.copy(alpha = 0.7f), fontSize = 8.sp)
            val noonLabelStyle = TextStyle(color = Color.White.copy(alpha = 0.45f), fontSize = 8.sp)

            // Draw Midnight vertical lines + label
            midnightIndices.forEach { idx ->
                val x = (idx * (itemWidthPx + spacingPx) + itemWidthPx / 2) * xScale
                drawLine(
                    color = Color.White.copy(alpha = 0.4f),
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
                    color = Color.White.copy(alpha = 0.2f),
                    start = Offset(x, 0f),
                    end = Offset(x, height),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx()), 0f)
                )
                val layout = textMeasurer.measure("12:00", noonLabelStyle)
                drawText(layout, topLeft = Offset(x - layout.size.width / 2f, 2.dp.toPx()))
            }

            val path = Path()
            val fillPath = Path()

            val points = temps.mapIndexed { i, temp ->
                val x = (i * (itemWidthPx + spacingPx) + itemWidthPx / 2) * xScale
                val y = height - ((temp - paddedMin) / paddedRange * height).toFloat()
                x to y
            }

            points.forEachIndexed { i, (x, y) ->
                if (i == 0) {
                    path.moveTo(x, y)
                    fillPath.moveTo(x, height)
                    fillPath.lineTo(x, y)
                } else {
                    val (prevX, prevY) = points[i - 1]
                    // Cubic bezier for super smooth look
                    val cp1x = prevX + (x - prevX) / 2f
                    path.cubicTo(
                        cp1x, prevY,
                        cp1x, y,
                        x, y
                    )
                    fillPath.cubicTo(
                        cp1x, prevY,
                        cp1x, y,
                        x, y
                    )
                }
                
                if (i == pointsCount - 1) {
                    fillPath.lineTo(x, height)
                    fillPath.close()
                }
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
            points.forEach { (x, y) ->
                drawCircle(
                    color = lineColor,
                    radius = 3.dp.toPx(),
                    center = Offset(x, y)
                )
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
                    color = Color.White.copy(alpha = 0.7f),
                    start = Offset(vpLeft, 0f),
                    end = Offset(vpLeft, height),
                    strokeWidth = 1.5.dp.toPx()
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.7f),
                    start = Offset(vpRight, 0f),
                    end = Offset(vpRight, height),
                    strokeWidth = 1.5.dp.toPx()
                )
            }
        }
    }
}
