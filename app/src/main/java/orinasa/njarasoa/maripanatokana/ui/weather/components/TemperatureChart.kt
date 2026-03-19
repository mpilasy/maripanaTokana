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
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import orinasa.njarasoa.maripanatokana.domain.model.HourlyForecast
import orinasa.njarasoa.maripanatokana.ui.theme.SkyBlue
import kotlin.math.roundToInt

@Composable
fun TemperatureChart(
    forecasts: List<HourlyForecast>,
    metricPrimary: Boolean,
    itemWidth: Dp,
    spacing: Dp,
    localizeDigits: (String) -> String,
    fontFamily: FontFamily,
    scale: Float,
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

    // Add padding to range for labels
    val paddedMin = minTemp - (tempRange * 0.3)
    val paddedMax = maxTemp + (tempRange * 0.3)
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

            val textPaint = android.graphics.Paint().apply {
                color = Color.White.copy(alpha = 0.9f).toArgb()
                textSize = (11 * scale).dp.toPx()
                textAlign = android.graphics.Paint.Align.CENTER
                typeface = android.graphics.Typeface.DEFAULT // Fallback, would be better to use font family
                isAntiAlias = true
            }

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
                    
                    path.quadraticBezierTo(
                        prevX + (itemWidthPx + spacingPx) / 2, prevY,
                        x, y
                    )
                    fillPath.quadraticBezierTo(
                        prevX + (itemWidthPx + spacingPx) / 2, prevY,
                        x, y
                    )
                }

                // Draw temperature label
                val label = localizeDigits("${temp.roundToInt()}°")
                drawContext.canvas.nativeCanvas.drawText(
                    label,
                    x,
                    y - (8 * scale).dp.toPx(),
                    textPaint
                )
                
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

            // Draw line
            drawPath(
                path = path,
                color = lineColor.copy(alpha = 0.4f),
                style = Stroke(width = 1.5.dp.toPx())
            )
        }
    }
}
