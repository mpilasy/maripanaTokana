package orinasa.njarasoa.maripanatokana.ui.weather.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import orinasa.njarasoa.maripanatokana.domain.model.PollenTier

// Reuses UvTierBadge's Low/Moderate/High/Very High colors (green/yellow/orange/red) — same
// four-tier semantics, so the same color ramp applies.
internal data class PollenTierColors(val background: Color, val text: Color)

internal fun colorsFor(tier: PollenTier): PollenTierColors = when (tier) {
    PollenTier.LOW -> PollenTierColors(Color(0xFF299501), Color.White)
    PollenTier.MODERATE -> PollenTierColors(Color(0xFFF7E401), Color(0xFF1A1A1A))
    PollenTier.HIGH -> PollenTierColors(Color(0xFFF85900), Color.White)
    PollenTier.VERY_HIGH -> PollenTierColors(Color(0xFFD8001D), Color.White)
}

@Composable
fun PollenTierBadge(
    tier: PollenTier,
    label: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 12.sp,
) {
    val colors = colorsFor(tier)
    TierPill(label = label, background = colors.background, foreground = colors.text, modifier = modifier, fontSize = fontSize)
}
