package orinasa.njarasoa.maripanatokana.ui.weather.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import orinasa.njarasoa.maripanatokana.R
import orinasa.njarasoa.maripanatokana.domain.model.AqiStandard
import orinasa.njarasoa.maripanatokana.domain.model.AqiTier
import orinasa.njarasoa.maripanatokana.domain.model.HourlyAirQuality
import java.util.Calendar

/**
 * 48-hour AQI trend line. Modeled on DailyTemperatureChart's fixed-width, evenly-spaced-points
 * layout (no scroll-sync) rather than TemperatureChart's, since this chart has no companion
 * scrollable card row. Only the dots are tier-colored, per design — the connecting line stays a
 * single neutral color so two adjacent tiers don't need a blended line segment. Includes its own
 * color-key legend so a viewer always sees the chart with an explanation of what each dot color
 * means.
 */
@Composable
fun AirQualityChart(
    forecasts: List<HourlyAirQuality>,
    primaryStandard: AqiStandard,
    modifier: Modifier = Modifier,
    lineColor: Color = Color.White,
) {
    if (forecasts.isEmpty()) return

    val values = remember(forecasts, primaryStandard) {
        forecasts.map { if (primaryStandard == AqiStandard.EUROPEAN) it.europeanValue else it.usValue }
    }

    val aqiTierLabels = stringArrayResource(R.array.aqi_tier_labels)
    val legendEntries = remember(aqiTierLabels) {
        AqiTier.entries.mapIndexed { i, tier -> colorsFor(tier).background to aqiTierLabels[i] }
    }

    val minValue = remember(values) { values.minOrNull() ?: 0 }
    val maxValue = remember(values) { values.maxOrNull() ?: 0 }
    val valueRange = remember(minValue, maxValue) {
        val range = (maxValue - minValue).toDouble()
        if (range == 0.0) 1.0 else range
    }

    val paddedMin = minValue - (valueRange * 0.15)
    val paddedMax = maxValue + (valueRange * 0.15)
    val paddedRange = paddedMax - paddedMin

    // Device-timezone hour-of-day check, matching TemperatureChart's existing convention.
    val midnightIndices = remember(forecasts) {
        val calendar = Calendar.getInstance()
        forecasts.mapIndexed { index, item ->
            calendar.timeInMillis = item.time
            if (calendar.get(Calendar.HOUR_OF_DAY) == 0) index else -1
        }.filter { it != -1 }
    }

    val noonIndices = remember(forecasts) {
        val calendar = Calendar.getInstance()
        forecasts.mapIndexed { index, item ->
            calendar.timeInMillis = item.time
            if (calendar.get(Calendar.HOUR_OF_DAY) == 12) index else -1
        }.filter { it != -1 }
    }

    val dotColors = remember(forecasts, primaryStandard) {
        forecasts.map { colorsFor(it.tier(primaryStandard)).background }
    }

    val textMeasurer = rememberTextMeasurer()
    val gridColor = MaterialTheme.colorScheme.onSurface

    Column(modifier = modifier) {
        TierLegend(legendEntries, modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp))
        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val height = size.height
            val width = size.width
            val pointsCount = values.size
            if (pointsCount < 2) return@Canvas

            val midnightLabelStyle = TextStyle(color = gridColor.copy(alpha = 0.7f), fontSize = 8.sp)
            val noonLabelStyle = TextStyle(color = gridColor.copy(alpha = 0.45f), fontSize = 8.sp)

            midnightIndices.forEach { idx ->
                val x = (idx.toFloat() / (pointsCount - 1)) * width
                drawLine(
                    color = gridColor.copy(alpha = 0.4f),
                    start = Offset(x, 0f),
                    end = Offset(x, height),
                    strokeWidth = 1.dp.toPx()
                )
                val layout = textMeasurer.measure("00:00", midnightLabelStyle)
                drawText(layout, topLeft = Offset(x - layout.size.width / 2f, 2.dp.toPx()))
            }

            noonIndices.forEach { idx ->
                val x = (idx.toFloat() / (pointsCount - 1)) * width
                drawLine(
                    color = gridColor.copy(alpha = 0.2f),
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

            val points = values.mapIndexed { i, value ->
                val x = (i.toFloat() / (pointsCount - 1)) * width
                val y = height - ((value - paddedMin) / paddedRange * height).toFloat()
                Offset(x, y)
            }

            points.forEachIndexed { i, point ->
                if (i == 0) {
                    path.moveTo(point.x, point.y)
                    fillPath.moveTo(point.x, height)
                    fillPath.lineTo(point.x, point.y)
                } else {
                    val prev = points[i - 1]
                    val cp1x = prev.x + (point.x - prev.x) / 2f
                    path.cubicTo(cp1x, prev.y, cp1x, point.y, point.x, point.y)
                    fillPath.cubicTo(cp1x, prev.y, cp1x, point.y, point.x, point.y)
                }
                if (i == pointsCount - 1) {
                    fillPath.lineTo(point.x, height)
                    fillPath.close()
                }
            }

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(lineColor.copy(alpha = 0.12f), Color.Transparent),
                    startY = 0f,
                    endY = height
                )
            )

            drawPath(
                path = path,
                color = lineColor,
                style = Stroke(width = 2.dp.toPx())
            )

            points.forEachIndexed { i, point ->
                drawCircle(
                    color = dotColors[i],
                    radius = 4.dp.toPx(),
                    center = point
                )
            }
        }
        }
    }
}
