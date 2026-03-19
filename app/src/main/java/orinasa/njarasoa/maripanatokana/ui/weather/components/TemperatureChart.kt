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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import orinasa.njarasoa.maripanatokana.domain.model.HourlyForecast
import orinasa.njarasoa.maripanatokana.ui.theme.SkyBlue

@Composable
fun TemperatureChart(
    forecasts: List<HourlyForecast>,
    metricPrimary: Boolean,
    modifier: Modifier = Modifier,
    lineColor: Color = SkyBlue,
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
    val paddedMin = minTemp - (tempRange * 0.1)
    val paddedMax = maxTemp + (tempRange * 0.1)
    val paddedRange = paddedMax - paddedMin

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val pointsCount = temps.size
            if (pointsCount < 2) return@Canvas

            val stepX = width / (pointsCount - 1)
            
            val path = Path()
            val fillPath = Path()

            temps.forEachIndexed { i, temp ->
                val x = i * stepX
                val y = height - ((temp - paddedMin) / paddedRange * height).toFloat()

                if (i == 0) {
                    path.moveTo(x, y)
                    fillPath.moveTo(x, height)
                    fillPath.lineTo(x, y)
                } else {
                    path.lineTo(x, y)
                    fillPath.lineTo(x, y)
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
                    colors = listOf(lineColor.copy(alpha = 0.3f), Color.Transparent),
                    startY = 0f,
                    endY = height
                )
            )

            // Draw line
            drawPath(
                path = path,
                color = lineColor,
                style = Stroke(width = 2.dp.toPx())
            )
        }
    }
}
