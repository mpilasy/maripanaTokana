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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.unit.dp
import orinasa.njarasoa.maripanatokana.R
import orinasa.njarasoa.maripanatokana.domain.model.DailyForecast
import java.util.Calendar

// EPA UV Index tier boundaries (see UvTierBadge.colorsFor) — sampling colorsFor() at these
// values reuses the single source of truth for tier colors instead of re-declaring the hexes.
private val UV_TIER_SAMPLE_VALUES = listOf(0.0, 3.0, 6.0, 8.0, 11.0)

/** Single-series line chart of daily max UV index, styled like [DailyTemperatureChart] but with
 *  EPA-tier-colored dots instead of a fixed line color, since UV severity is what matters here.
 *  Includes its own color-key legend so a viewer always sees the chart with an explanation of
 *  what each dot color means. */
@Composable
fun DailyUvChart(
    forecasts: List<DailyForecast>,
    modifier: Modifier = Modifier,
) {
    if (forecasts.isEmpty()) return

    val uvValues = remember(forecasts) { forecasts.map { it.uvIndexMax } }
    val uvMax = remember(uvValues) { uvValues.maxOrNull() ?: 0.0 }
    val paddedMax = remember(uvMax) { if (uvMax <= 0.0) 1.0 else uvMax * 1.2 }

    val horizontalTicks = remember(paddedMax) {
        (0..Math.floor(paddedMax).toInt()).toList()
    }

    val mondayIndices = remember(forecasts) {
        val calendar = Calendar.getInstance()
        forecasts.mapIndexed { index, forecast ->
            calendar.timeInMillis = forecast.date
            if (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY) index else -1
        }.filter { it != -1 }
    }

    val uvLabels = stringArrayResource(R.array.uv_labels)
    val legendEntries = remember(uvLabels) {
        UV_TIER_SAMPLE_VALUES.mapIndexed { i, sample -> colorsFor(sample).background to uvLabels[i] }
    }

    val gridColor = MaterialTheme.colorScheme.onSurface
    Column(modifier = modifier) {
        TierLegend(legendEntries, modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp))
        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val height = size.height
            val width = size.width
            val pointsCount = forecasts.size
            if (pointsCount < 2) return@Canvas

            // Draw horizontal ticks
            horizontalTicks.forEach { uv ->
                val isMajor = uv % 5 == 0
                val y = height - (uv / paddedMax * height).toFloat()
                drawLine(
                    color = gridColor.copy(alpha = if (isMajor) 0.25f else 0.1f),
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = (if (isMajor) 0.8.dp else 0.5.dp).toPx()
                )
            }

            // Draw Monday vertical lines
            mondayIndices.forEach { idx ->
                val x = (idx.toFloat() / (pointsCount - 1)) * width
                drawLine(
                    color = gridColor.copy(alpha = 0.2f),
                    start = Offset(x, 0f),
                    end = Offset(x, height),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 2.dp.toPx()), 0f)
                )
            }

            val uvPoints = uvValues.mapIndexed { i, uv ->
                val x = (i.toFloat() / (pointsCount - 1)) * width
                val y = height - (uv / paddedMax * height).toFloat()
                Offset(x, y)
            }

            val uvPath = Path()
            uvPoints.forEachIndexed { i, point ->
                if (i == 0) {
                    uvPath.moveTo(point.x, point.y)
                } else {
                    val prev = uvPoints[i - 1]
                    val cp1x = prev.x + (point.x - prev.x) / 2f
                    uvPath.cubicTo(cp1x, prev.y, cp1x, point.y, point.x, point.y)
                }
            }

            drawPath(
                path = uvPath,
                color = gridColor.copy(alpha = 0.5f),
                style = Stroke(width = 2.dp.toPx())
            )

            uvPoints.forEachIndexed { i, point ->
                drawCircle(color = colorsFor(uvValues[i]).background, radius = 3.dp.toPx(), center = point)
            }
        }
        }
    }
}
