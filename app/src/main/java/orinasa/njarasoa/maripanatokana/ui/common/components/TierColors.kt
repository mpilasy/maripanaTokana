// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

package orinasa.njarasoa.maripanatokana.ui.common.components

import androidx.compose.ui.graphics.Color
import orinasa.njarasoa.maripanatokana.domain.model.AqiTier
import orinasa.njarasoa.maripanatokana.domain.model.PollenTier

// The tier colours of the AQI, pollen and UV levels. The phone badges and the TV screens use them.

// AirNow AQI category colors (airnow.gov/aqi/aqi-basics). AqiTier has 5 levels, AirNow has 6: UNHEALTHY
// covers Orange "Unhealthy for Sensitive Groups" (101-150) and Red "Unhealthy" (151-200), and takes Red.
internal data class AqiTierColors(val background: Color, val text: Color)

internal fun colorsFor(tier: AqiTier): AqiTierColors = when (tier) {
    AqiTier.GOOD -> AqiTierColors(Color(0xFF00E400), Color(0xFF1A1A1A))
    AqiTier.MODERATE -> AqiTierColors(Color(0xFFFFFF00), Color(0xFF1A1A1A))
    AqiTier.UNHEALTHY -> AqiTierColors(Color(0xFFFF0000), Color.White)
    AqiTier.VERY_UNHEALTHY -> AqiTierColors(Color(0xFF8F3F97), Color.White)
    AqiTier.HAZARDOUS -> AqiTierColors(Color(0xFF7E0023), Color.White)
}

// Reuses the UV Low/Moderate/High/Very High colors (green/yellow/orange/red) — same
// four-tier semantics, so the same color ramp applies.
internal data class PollenTierColors(val background: Color, val text: Color)

internal fun colorsFor(tier: PollenTier): PollenTierColors = when (tier) {
    PollenTier.LOW -> PollenTierColors(Color(0xFF299501), Color.White)
    PollenTier.MODERATE -> PollenTierColors(Color(0xFFF7E401), Color(0xFF1A1A1A))
    PollenTier.HIGH -> PollenTierColors(Color(0xFFF85900), Color.White)
    PollenTier.VERY_HIGH -> PollenTierColors(Color(0xFFD8001D), Color.White)
}

// EPA UV Index color scale (epa.gov/sunsafety/uv-index-scale-0): Green/Yellow/Orange/Red/Violet.
// Thresholds match the Low/Moderate/High/Very High/Extreme bands used for uv_labels elsewhere.
internal data class UvTierColors(val background: Color, val text: Color)

internal fun colorsFor(uvIndex: Double): UvTierColors = when {
    uvIndex < 3 -> UvTierColors(Color(0xFF299501), Color.White)
    uvIndex < 6 -> UvTierColors(Color(0xFFF7E401), Color(0xFF1A1A1A))
    uvIndex < 8 -> UvTierColors(Color(0xFFF85900), Color.White)
    uvIndex < 11 -> UvTierColors(Color(0xFFD8001D), Color.White)
    else -> UvTierColors(Color(0xFF6B49C8), Color.White)
}
