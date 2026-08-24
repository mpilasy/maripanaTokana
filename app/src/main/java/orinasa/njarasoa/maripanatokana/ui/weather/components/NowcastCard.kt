package orinasa.njarasoa.maripanatokana.ui.weather.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import orinasa.njarasoa.maripanatokana.R
import orinasa.njarasoa.maripanatokana.domain.model.MinutelyForecast
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

@Composable
fun NowcastCard(
    items: List<MinutelyForecast>,
    isMetricPrimary: Boolean,
    modifier: Modifier = Modifier,
) {
    val nowMillis = System.currentTimeMillis()
    val headlineRes = computeHeadlineText(items, nowMillis)
    val minutesUntil = computeMinutesUntilStart(items, nowMillis)
    
    val headlineText = if (headlineRes == R.string.nowcast_starts_in) {
        stringResource(headlineRes, minutesUntil)
    } else {
        stringResource(headlineRes)
    }

    val displayItems = items.take(12)
    val maxPrecipMm = displayItems.maxOfOrNull { it.precipitation.mm }?.coerceAtLeast(0.5) ?: 1.0

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

        if (displayItems.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                displayItems.forEach { item ->
                    val fraction = (item.precipitation.mm / maxPrecipMm).toFloat().coerceIn(0.05f, 1.0f)
                    val barColor = if (item.precipitation.mm > 0.05) Color(0xFF38BDF8) else Color(0x33FFFFFF)
                    
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(12.dp)
                                .height((60 * fraction).dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(barColor)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = formatTime(item.time),
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
