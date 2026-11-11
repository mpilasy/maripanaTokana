// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

package orinasa.njarasoa.maripanatokana.ui.mobile.weather.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Text
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import orinasa.njarasoa.maripanatokana.domain.model.AqiTier
import orinasa.njarasoa.maripanatokana.ui.common.components.colorsFor

@Composable
fun AqiTierBadge(
    tier: AqiTier,
    label: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 12.sp,
    singleLine: Boolean = false,
) {
    val colors = colorsFor(tier)
    TierPill(label = label, background = colors.background, foreground = colors.text, modifier = modifier, fontSize = fontSize, singleLine = singleLine)
}

/** Shared pill styling for tier badges (AQI, UV Index, ...). */
@Composable
internal fun TierPill(
    label: String,
    background: Color,
    foreground: Color,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 12.sp,
    // In narrow forecast cards: stay on one line, shrinking the text to fit before cutting it short
    singleLine: Boolean = false,
) {
    // A tint of the level's colour, with the text in a lighter shade of it (foreground is kept for callers but unused).
    // The same rule as the activity badges: half white keeps the text at 4.5:1 or more on the tint
    val tint = background.copy(alpha = 0.22f)
    val text = lerp(background, Color.White, 0.5f)
    if (singleLine) {
        BasicText(
            text = label,
            style = TextStyle(color = text, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            autoSize = TextAutoSize.StepBased(minFontSize = 8.sp, maxFontSize = fontSize),
            modifier = modifier
                .padding(horizontal = 4.dp)
                .background(tint, RoundedCornerShape(50))
                .padding(horizontal = 6.dp, vertical = 2.dp),
        )
        return
    }
    Text(
        text = label,
        color = text,
        fontSize = fontSize,
        fontWeight = FontWeight.Medium,
        modifier = modifier
            .background(tint, RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}
