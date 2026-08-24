package orinasa.njarasoa.maripanatokana.ui.weather.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import orinasa.njarasoa.maripanatokana.R
import orinasa.njarasoa.maripanatokana.domain.model.MinutelyForecast
import orinasa.njarasoa.maripanatokana.domain.model.Precipitation
import orinasa.njarasoa.maripanatokana.ui.theme.LocalBodyFont
import orinasa.njarasoa.maripanatokana.ui.theme.LocalDisplayFont
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.sqrt

private data class ControlPoints(val cp1: Offset, val cp2: Offset)

private fun formatTime(epochMillis: Long): String {
    val formatter = SimpleDateFormat("HH:mm", Locale.US)
    val result = formatter.format(Date(epochMillis))
    return result
}

private fun computeHeadlineText(items: List<MinutelyForecast>, nowMillis: Long): Int {
    val firstPrecipIndex = items.indexOfFirst { it.precipitation.mm > 0.05 }
    val result = when {
        items.isEmpty() || items.all { it.precipitation.mm <= 0.05 } -> R.string.nowcast_no_precip
        firstPrecipIndex == 0 -> R.string.nowcast_ongoing
        firstPrecipIndex > 0 -> R.string.nowcast_starts_in
        else -> R.string.nowcast_no_precip
    }
    return result
}

private fun computeMinutesUntilStart(items: List<MinutelyForecast>, nowMillis: Long): Int {
    val firstPrecipIndex = items.indexOfFirst { it.precipitation.mm > 0.05 }
    val result = if (firstPrecipIndex > 0) {
        val diffMs = items[firstPrecipIndex].time - nowMillis
        val mins = (diffMs / (60 * 1000L)).toInt().coerceAtLeast(1)
        mins
    } else {
        0
    }
    return result
}

private fun generate30MinTicks(startTime: Long, endTime: Long): List<Long> {
    val result = if (endTime <= startTime) {
        emptyList()
    } else {
        val intervalMs = 30 * 60 * 1000L
        val firstTick = ((startTime + intervalMs - 1) / intervalMs) * intervalMs
        val ticks = mutableListOf<Long>()
        var curr = firstTick
        while (curr <= endTime) {
            ticks.add(curr)
            curr += intervalMs
        }
        ticks
    }
    return result
}

private fun computeMonotoneCubicControlPoints(points: List<Offset>): List<ControlPoints> {
    val n = points.size
    val result = if (n < 2) {
        emptyList()
    } else {
        val dx = FloatArray(n - 1)
        val dy = FloatArray(n - 1)
        val ms = FloatArray(n - 1)

        for (i in 0 until n - 1) {
            dx[i] = (points[i + 1].x - points[i].x).coerceAtLeast(0.0001f)
            dy[i] = points[i + 1].y - points[i].y
            ms[i] = dy[i] / dx[i]
        }

        val ds = FloatArray(n)
        ds[0] = ms[0]
        ds[n - 1] = ms[n - 2]

        for (i in 1 until n - 1) {
            if (ms[i - 1] * ms[i] <= 0f) {
                ds[i] = 0f
            } else {
                ds[i] = (ms[i - 1] + ms[i]) / 2f
            }
        }

        for (i in 0 until n - 1) {
            if (ms[i] == 0f) {
                ds[i] = 0f
                ds[i + 1] = 0f
            } else {
                val alpha = ds[i] / ms[i]
                val beta = ds[i + 1] / ms[i]
                val dist = alpha * alpha + beta * beta
                if (dist > 9f) {
                    val tau = 3f / sqrt(dist)
                    ds[i] = tau * alpha * ms[i]
                    ds[i + 1] = tau * beta * ms[i]
                }
            }
        }

        val list = mutableListOf<ControlPoints>()
        for (i in 0 until n - 1) {
            val h = dx[i]
            val p1 = points[i]
            val p2 = points[i + 1]
            val cp1 = Offset(p1.x + h / 3f, p1.y + ds[i] * h / 3f)
            val cp2 = Offset(p2.x - h / 3f, p2.y - ds[i + 1] * h / 3f)
            list.add(ControlPoints(cp1, cp2))
        }
        list
    }
    return result
}

@Composable
fun NowcastCard(
    items: List<MinutelyForecast>,
    isMetricPrimary: Boolean,
    modifier: Modifier = Modifier,
) {
    val nowMillis = System.currentTimeMillis()
    val displayItems = items.filter { it.time >= nowMillis - 15 * 60 * 1000L && it.time <= nowMillis + 2 * 3600 * 1000L }
    val headlineRes = computeHeadlineText(displayItems, nowMillis)
    val minutesUntil = computeMinutesUntilStart(displayItems, nowMillis)
    
    val headlineText = if (headlineRes == R.string.nowcast_starts_in) {
        stringResource(headlineRes, minutesUntil)
    } else {
        stringResource(headlineRes)
    }

    val hasPrecipitation = displayItems.any { it.precipitation.mm > 0.05 }
    val maxPrecipMm = displayItems.maxOfOrNull { it.precipitation.mm }?.coerceAtLeast(0.5) ?: 1.0
    val maxPrecipObj = Precipitation.fromMm(maxPrecipMm)
    val yLabelText = if (isMetricPrimary) maxPrecipObj.displayMetric() else maxPrecipObj.displayImperial()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x992A1FA5))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "🌧️",
                fontSize = 18.sp
            )
            Text(
                text = headlineText,
                fontFamily = LocalBodyFont.current,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
        }

        if (hasPrecipitation && displayItems.size > 1) {
            val chartLineColor = Color(0xFF38BDF8)
            val chartFillBrush = Brush.verticalGradient(
                colors = listOf(Color(0x6638BDF8), Color(0x0038BDF8))
            )
            val gridDashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)

            val startTime = displayItems.first().time
            val endTime = displayItems.last().time
            val durationMs = (endTime - startTime).coerceAtLeast(1L)
            val ticks30Min = generate30MinTicks(startTime, endTime)

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Y-axis peak & min scale indicators
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = yLabelText,
                        fontFamily = LocalDisplayFont.current,
                        fontSize = 9.sp,
                        color = Color(0xB3FFFFFF)
                    )
                    Text(
                        text = if (isMetricPrimary) "0.0 mm" else "0.00 in",
                        fontFamily = LocalDisplayFont.current,
                        fontSize = 9.sp,
                        color = Color(0x66FFFFFF)
                    )
                }

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(70.dp)
                ) {
                    val width = size.width
                    val height = size.height

                    // Draw top dashed guideline
                    drawLine(
                        color = Color(0x33FFFFFF),
                        start = Offset(0f, 6.dp.toPx()),
                        end = Offset(width, 6.dp.toPx()),
                        pathEffect = gridDashEffect
                    )

                    // Draw bottom baseline
                    drawLine(
                        color = Color(0x33FFFFFF),
                        start = Offset(0f, height - 2.dp.toPx()),
                        end = Offset(width, height - 2.dp.toPx())
                    )

                    val points = displayItems.map { item ->
                        val ratioX = (item.time - startTime).toFloat() / durationMs.toFloat()
                        val x = ratioX * width
                        val fraction = (item.precipitation.mm / maxPrecipMm).toFloat().coerceIn(0f, 1f)
                        val y = height - 2.dp.toPx() - (fraction * (height - 14.dp.toPx()))
                        Offset(x, y)
                    }

                    val controlPoints = computeMonotoneCubicControlPoints(points)

                    val strokePath = Path().apply {
                        moveTo(points[0].x, points[0].y)
                        for (i in 0 until controlPoints.size) {
                            val cp = controlPoints[i]
                            val pNext = points[i + 1]
                            cubicTo(cp.cp1.x, cp.cp1.y, cp.cp2.x, cp.cp2.y, pNext.x, pNext.y)
                        }
                    }

                    val fillPath = Path().apply {
                        moveTo(points[0].x, height - 2.dp.toPx())
                        lineTo(points[0].x, points[0].y)
                        for (i in 0 until controlPoints.size) {
                            val cp = controlPoints[i]
                            val pNext = points[i + 1]
                            cubicTo(cp.cp1.x, cp.cp1.y, cp.cp2.x, cp.cp2.y, pNext.x, pNext.y)
                        }
                        lineTo(points.last().x, height - 2.dp.toPx())
                        close()
                    }

                    drawPath(path = fillPath, brush = chartFillBrush)
                    drawPath(path = strokePath, color = chartLineColor, style = Stroke(width = 2.dp.toPx()))
                }

                // 30-minute X-axis time labels
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ticks30Min.forEach { tickTime ->
                        Text(
                            text = formatTime(tickTime),
                            fontFamily = LocalDisplayFont.current,
                            fontSize = 9.sp,
                            color = Color(0xB3FFFFFF)
                        )
                    }
                }
            }
        }
    }
}
