// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

package orinasa.njarasoa.maripanatokana.ui.weather.chart

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import orinasa.njarasoa.maripanatokana.ui.theme.AppDarkPrimary
import orinasa.njarasoa.maripanatokana.ui.theme.ChillBlue
import orinasa.njarasoa.maripanatokana.ui.theme.FreezingLine
import orinasa.njarasoa.maripanatokana.ui.theme.SkyBlue

/**
 * The colours, line widths and dash patterns of the chart view and of the charts on the main page.
 * A chart with a colour of its own (the heat index, the UV and air quality tiers) keeps it.
 * The web has the same values with the same names in utils/chartStyle.ts.
 */
object ChartStyle {
    val temperature = Color.White
    val dewPoint = ChillBlue
    val precipitation = SkyBlue
    val chance = Color.White.copy(alpha = 0.6f)
    val wind = AppDarkPrimary
    val gust = AppDarkPrimary.copy(alpha = 0.6f)
    val pressure = AppDarkPrimary
    val humidity = SkyBlue
    val freezing = FreezingLine
    // The normal high and low: the colours of the high and low lines at 50 % opacity
    val normalHigh = Color(0x80FF7043)
    val normalLow = Color(0x8064B5F6)
    val night = Color.Black.copy(alpha = 0.18f)
    val midnight = Color.White.copy(alpha = 0.25f)
    val selection = Color.White.copy(alpha = 0.7f)
    val selectionDot = Color.White
    // The soft column of the chart view: the selected hour, one hour wide
    val selectionColumn = Color.White.copy(alpha = 0.10f)

    val lineWidth = 2.dp
    val thinLineWidth = 1.5.dp
    val chanceWidth = 1.2.dp
    val freezingWidth = 1.dp
    val normalWidth = 1.dp
    val midnightWidth = 1.dp
    val selectionWidth = 1.dp
    val selectionDotRadius = 3.5.dp
    val selectionColumnRadius = 8.dp

    val freezingDash = listOf(4.dp, 3.dp)
    val gustDash = listOf(5.dp, 3.dp)
    // Shorter dashes than the freezing line
    val normalDash = listOf(2.dp, 3.dp)
}

/** A dash pattern of [ChartStyle] (dash, gap, ... in dp) as a path effect. */
fun DrawScope.chartDash(pattern: List<Dp>): PathEffect =
    PathEffect.dashPathEffect(pattern.map { it.toPx() }.toFloatArray())
