// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

package orinasa.njarasoa.maripanatokana.ui.mobile.weather.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import orinasa.njarasoa.maripanatokana.domain.model.PollenTier
import orinasa.njarasoa.maripanatokana.ui.common.components.colorsFor

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
