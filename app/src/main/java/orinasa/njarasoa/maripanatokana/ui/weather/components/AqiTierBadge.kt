package orinasa.njarasoa.maripanatokana.ui.weather.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import orinasa.njarasoa.maripanatokana.domain.model.AqiTier

// AirNow AQI category colors (airnow.gov/aqi/aqi-basics). Our AqiTier enum has 5 buckets rather
// than AirNow's 6 — UNHEALTHY covers AirNow's Orange "Unhealthy for Sensitive Groups" (101-150)
// and Red "Unhealthy" (151-200) range, mapped here to Red.
private data class AqiTierColors(val background: Color, val text: Color)

private fun colorsFor(tier: AqiTier): AqiTierColors = when (tier) {
    AqiTier.GOOD -> AqiTierColors(Color(0xFF00E400), Color(0xFF1A1A1A))
    AqiTier.MODERATE -> AqiTierColors(Color(0xFFFFFF00), Color(0xFF1A1A1A))
    AqiTier.UNHEALTHY -> AqiTierColors(Color(0xFFFF0000), Color.White)
    AqiTier.VERY_UNHEALTHY -> AqiTierColors(Color(0xFF8F3F97), Color.White)
    AqiTier.HAZARDOUS -> AqiTierColors(Color(0xFF7E0023), Color.White)
}

@Composable
fun AqiTierBadge(
    tier: AqiTier,
    label: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 12.sp,
) {
    val colors = colorsFor(tier)
    Text(
        text = label,
        color = colors.text,
        fontSize = fontSize,
        fontWeight = FontWeight.Medium,
        modifier = modifier
            .background(colors.background, RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}
