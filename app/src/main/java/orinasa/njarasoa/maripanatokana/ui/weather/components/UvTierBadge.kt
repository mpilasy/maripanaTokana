package orinasa.njarasoa.maripanatokana.ui.weather.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

// EPA UV Index color scale (epa.gov/sunsafety/uv-index-scale-0): Green/Yellow/Orange/Red/Violet.
// Thresholds match the Low/Moderate/High/Very High/Extreme bands used for uv_labels elsewhere.
private data class UvTierColors(val background: Color, val text: Color)

private fun colorsFor(uvIndex: Double): UvTierColors = when {
    uvIndex < 3 -> UvTierColors(Color(0xFF299501), Color.White)
    uvIndex < 6 -> UvTierColors(Color(0xFFF7E401), Color(0xFF1A1A1A))
    uvIndex < 8 -> UvTierColors(Color(0xFFF85900), Color.White)
    uvIndex < 11 -> UvTierColors(Color(0xFFD8001D), Color.White)
    else -> UvTierColors(Color(0xFF6B49C8), Color.White)
}

@Composable
fun UvTierBadge(
    uvIndex: Double,
    label: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 12.sp,
) {
    val colors = colorsFor(uvIndex)
    TierPill(label = label, background = colors.background, foreground = colors.text, modifier = modifier, fontSize = fontSize)
}
