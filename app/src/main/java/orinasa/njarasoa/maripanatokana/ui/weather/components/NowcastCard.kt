package orinasa.njarasoa.maripanatokana.ui.weather.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
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
    val maxPrecipMm = (displayItems.maxOfOrNull { it.precipitation.mm } ?: 0.1).coerceAtLeast(0.1)
    val maxPrecipObj = Precipitation.fromMm(maxPrecipMm)
    val yLabelText = if (isMetricPrimary) maxPrecipObj.displayMetric() else maxPrecipObj.displayImperial()

    val minPrecipMm = displayItems.minOfOrNull { it.precipitation.mm } ?: 0.0
    val minPrecipObj = Precipitation.fromMm(minPrecipMm)
    val yMinLabelText = if (isMetricPrimary) minPrecipObj.displayMetric() else minPrecipObj.displayImperial()

    val textMeasurer = rememberTextMeasurer()
    val timeLabelStyle = TextStyle(
        color = Color(0xB3FFFFFF),
        fontSize = 9.sp,
        fontFamily = LocalDisplayFont.current
    )
    val maxLabelStyle = TextStyle(
        color = Color(0xFF38BDF8),
        fontSize = 9.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = LocalDisplayFont.current
    )
    val minLabelStyle = TextStyle(
        color = Color(0x66FFFFFF),
        fontSize = 9.sp,
        fontFamily = LocalDisplayFont.current
    )

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
            val gridDashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)

            val startTime = displayItems.first().time
            val endTime = displayItems.last().time
            val durationMs = (endTime - startTime).coerceAtLeast(1L)
            val ticks30Min = generate30MinTicks(startTime, endTime)

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(95.dp)
            ) {
                val width = size.width
                val height = size.height
                val chartHeight = height - 18.dp.toPx()

                // Draw top dashed guideline
                drawLine(
                    color = Color(0x33FFFFFF),
                    start = Offset(0f, 4.dp.toPx()),
                    end = Offset(width, 4.dp.toPx()),
                    pathEffect = gridDashEffect
                )

                // Draw bottom baseline
                drawLine(
                    color = Color(0x33FFFFFF),
                    start = Offset(0f, chartHeight),
                    end = Offset(width, chartHeight)
                )

                // Draw 30-min vertical tick lines & aligned X-axis labels
                ticks30Min.forEach { tickTime ->
                    val ratioX = (tickTime - startTime).toFloat() / durationMs.toFloat()
                    val tickX = ratioX * width
                    drawLine(
                        color = Color(0x20FFFFFF),
                        start = Offset(tickX, 0f),
                        end = Offset(tickX, chartHeight),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = gridDashEffect
                    )
                    val text = formatTime(tickTime)
                    val layout = textMeasurer.measure(text, timeLabelStyle)
                    val textX = (tickX - layout.size.width / 2f).coerceIn(0f, width - layout.size.width)
                    drawText(layout, topLeft = Offset(textX, chartHeight + 4.dp.toPx()))
                }

                val points = displayItems.map { item ->
                    val ratioX = (item.time - startTime).toFloat() / durationMs.toFloat()
                    val x = ratioX * width
                    val fraction = (item.precipitation.mm / maxPrecipMm).toFloat().coerceIn(0f, 1f)
                    val y = chartHeight - (fraction * (chartHeight - 16.dp.toPx()))
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
                    moveTo(points[0].x, chartHeight)
                    lineTo(points[0].x, points[0].y)
                    for (i in 0 until controlPoints.size) {
                        val cp = controlPoints[i]
                        val pNext = points[i + 1]
                        cubicTo(cp.cp1.x, cp.cp1.y, cp.cp2.x, cp.cp2.y, pNext.x, pNext.y)
                    }
                    lineTo(points.last().x, chartHeight)
                    close()
                }

                drawPath(path = fillPath, brush = chartFillBrush)
                drawPath(path = strokePath, color = chartLineColor, style = Stroke(width = 2.dp.toPx()))

                // Position max legend right at the peak point (middle if multiple)
                val minY = points.minOf { it.y }
                val maxIndices = points.indices.filter { points[it].y == minY }
                val maxMiddleIdx = maxIndices[maxIndices.size / 2]
                val maxPoint = points[maxMiddleIdx]

                val maxTextLayout = textMeasurer.measure(yLabelText, maxLabelStyle)
                val maxTextX = (maxPoint.x - maxTextLayout.size.width / 2f).coerceIn(4.dp.toPx(), width - maxTextLayout.size.width - 4.dp.toPx())
                val maxTextY = (maxPoint.y + 4.dp.toPx()).coerceIn(2.dp.toPx(), chartHeight - maxTextLayout.size.height)
                drawText(maxTextLayout, topLeft = Offset(maxTextX, maxTextY))

                // Position min legend right at the baseline min point (middle if multiple)
                val maxY = points.maxOf { it.y }
                val minIndices = points.indices.filter { points[it].y == maxY }
                val minMiddleIdx = minIndices[minIndices.size / 2]
                val minPoint = points[minMiddleIdx]

                // Only draw min legend if it doesn't overlap horizontally with max legend
                val minTextLayout = textMeasurer.measure(yMinLabelText, minLabelStyle)
                val minTextX = (minPoint.x - minTextLayout.size.width / 2f).coerceIn(4.dp.toPx(), width - minTextLayout.size.width - 4.dp.toPx())
                val minTextY = (minPoint.y - minTextLayout.size.height - 2.dp.toPx()).coerceAtLeast(0f)
                
                val horizontalOverlap = Math.abs((maxTextX + maxTextLayout.size.width / 2f) - (minTextX + minTextLayout.size.width / 2f)) < 40.dp.toPx()
                if (!horizontalOverlap || Math.abs(maxTextY - minTextY) > 12.dp.toPx()) {
                    drawText(minTextLayout, topLeft = Offset(minTextX, minTextY))
                }
            }
        }
    }
}
