package orinasa.njarasoa.maripanatokana.ui.weather.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import orinasa.njarasoa.maripanatokana.domain.model.HourlyForecast

@Composable
fun TemperatureChart(
    forecasts: List<HourlyForecast>,
    metricPrimary: Boolean,
    itemWidth: Dp,
    spacing: Dp,
    modifier: Modifier = Modifier,
    lineColor: Color = Color.White,
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

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val height = size.height
            val pointsCount = temps.size
            if (pointsCount < 2) return@Canvas

            val itemWidthPx = itemWidth.toPx()
            val spacingPx = spacing.toPx()
            
            val path = Path()
            val fillPath = Path()

            temps.forEachIndexed { i, temp ->
                val x = i * (itemWidthPx + spacingPx) + itemWidthPx / 2
                val y = height - ((temp - paddedMin) / paddedRange * height).toFloat()

                if (i == 0) {
                    path.moveTo(x, y)
                    fillPath.moveTo(x, height)
                    fillPath.lineTo(x, y)
                } else {
                    val prevTemp = temps[i-1]
                    val prevX = (i - 1) * (itemWidthPx + spacingPx) + itemWidthPx / 2
                    val prevY = height - ((prevTemp - paddedMin) / paddedRange * height).toFloat()
                    
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
        }
    }
}
