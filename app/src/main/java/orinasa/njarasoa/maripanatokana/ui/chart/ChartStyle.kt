// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

package orinasa.njarasoa.maripanatokana.ui.chart

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import orinasa.njarasoa.maripanatokana.ui.theme.AppDarkPrimary
import orinasa.njarasoa.maripanatokana.ui.theme.FreezingLine
import orinasa.njarasoa.maripanatokana.ui.theme.SkyBlue

/**
 * The colours, line widths and dash patterns of the chart view and of the charts on the main page.
 * The UV and air quality lines take the colour of their tier. The TV charts use the same colours.
 * The web has the same values with the same names in utils/chartStyle.ts.
 */
object ChartStyle {
    val temperature = Color.White
    // Drawn dashed in the chart view (dewPointDash)
    val dewPoint = Color(0xFF81C784)
    val heatIndex = Color(0xFFF48FB1)
    val windChill = Color(0xFFCE93D8)
    val precipitation = SkyBlue
    val chance = Color.White.copy(alpha = 0.6f)
    val wind = AppDarkPrimary
    val gust = Color(0xFFFFB74D)
    val pressure = AppDarkPrimary
    val humidity = SkyBlue
    val freezing = FreezingLine
    // The daily high and low labels of the temperature
    val high = Color(0xFFFF7043)
    val low = Color(0xFF64B5F6)
    // The normal high and low: the colours of high and low at 50 % opacity
    val normalHigh = Color(0x80FF7043)
    val normalLow = Color(0x8064B5F6)
    val night = Color.Black.copy(alpha = 0.18f)
    val midnight = Color.White.copy(alpha = 0.25f)
    val selection = Color.White.copy(alpha = 0.7f)
    val selectionDot = Color.White
    // The soft column of the chart view: the selected hour, one hour wide
    val selectionColumn = Color.White.copy(alpha = 0.10f)
    // The band of each titled panel of the chart view, under the axis labels and the hours
    val panelBand = Color.White.copy(alpha = 0.04f)

    val lineWidth = 2.dp
    val thinLineWidth = 1.5.dp
    val chanceWidth = 1.2.dp
    val freezingWidth = 1.dp
    val normalWidth = 1.dp
    val midnightWidth = 1.dp
    val selectionWidth = 1.dp
    val selectionDotRadius = 3.5.dp
    val selectionColumnRadius = 8.dp
    val panelBandRadius = 12.dp

    val freezingDash = listOf(4.dp, 3.dp)
    val dewPointDash = listOf(5.dp, 3.dp)
    val airQualityDash = listOf(6.dp, 4.dp)
    // Shorter dashes than the freezing line
    val normalDash = listOf(2.dp, 3.dp)
}

/** A dash pattern of [ChartStyle] (dash, gap, ... in dp) as a path effect. */
fun DrawScope.chartDash(pattern: List<Dp>): PathEffect =
    PathEffect.dashPathEffect(pattern.map { it.toPx() }.toFloatArray())
