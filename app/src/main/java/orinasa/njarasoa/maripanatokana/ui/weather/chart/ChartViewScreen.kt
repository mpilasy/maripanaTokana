// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

package orinasa.njarasoa.maripanatokana.ui.weather.chart

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import orinasa.njarasoa.maripanatokana.R
import orinasa.njarasoa.maripanatokana.domain.model.ChartHour
import orinasa.njarasoa.maripanatokana.domain.model.WeatherData
import orinasa.njarasoa.maripanatokana.domain.model.showWindArrow
import orinasa.njarasoa.maripanatokana.ui.theme.Hairline
import orinasa.njarasoa.maripanatokana.ui.theme.LocalBodyFont
import orinasa.njarasoa.maripanatokana.ui.theme.LocalDisplayFont
import orinasa.njarasoa.maripanatokana.ui.weather.buildLocationTimeZone
import orinasa.njarasoa.maripanatokana.ui.weather.components.colorsFor
import orinasa.njarasoa.maripanatokana.ui.weather.components.computeMonotoneCubicControlPoints
import orinasa.njarasoa.maripanatokana.ui.weather.components.weatherIconRes
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt

private const val HOUR = 3_600_000L

/** The panels from top to bottom, with their height in dp. The value panels have a title line at the top. */
private enum class ChartPanel(val heightDp: Int, val titled: Boolean) {
    // The time axis at the top: the bottom one mirrored
    TOP_TIME(36, false),
    ICONS(46, false),
    TEMPERATURE(132, true),
    PRECIPITATION(88, true),
    WIND(104, true),
    PRESSURE(80, true),
    HUMIDITY(80, true),
    TIME(36, false),
}

private val AXIS_WIDTH = 52.dp
private val PANEL_GAP = 6.dp
private val TITLE_HEIGHT = 16.dp
// The room above the wind line for the direction arrows
private val ARROW_ROW = 14.dp

/** The values of one hour in the primary unit, as the charts draw them. */
private class ChartSeries(hours: List<ChartHour>, metricPrimary: Boolean) {
    val temperature = hours.map { it.temperature?.let { t -> if (metricPrimary) t.celsius else t.fahrenheit } }
    val dewPoint = hours.map { it.dewPoint?.let { t -> if (metricPrimary) t.celsius else t.fahrenheit } }
    val precipitation = hours.map { it.precipitation?.let { p -> if (metricPrimary) p.mm else p.inches } }
    val chance = hours.map { it.precipProbability?.toDouble() }
    val wind = hours.map { it.windSpeed?.let { w -> if (metricPrimary) w.metersPerSecond else w.mph } }
    val gust = hours.map { it.windGust?.let { w -> if (metricPrimary) w.metersPerSecond else w.mph } }
    val pressure = hours.map { it.pressure?.let { p -> if (metricPrimary) p.hPa else p.inHg } }
    val humidity = hours.map { it.humidity?.toDouble() }
}

/**
 * Up to 240 hours of the forecast in stacked panels that scroll sideways together, with the values of the
 * selected hour at the top. It opens at the current hour. The web has the same view in ChartView.svelte.
 */
@Composable
fun ChartViewScreen(
    data: WeatherData,
    metricPrimary: Boolean,
    localizeDigits: (String) -> String,
    onToggleUnits: () -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    // The data can be some hours old: start again at the current hour
    val hours = remember(data.chartHourly) {
        data.chartHourly.slice(chartHours(data.chartHourly.map { it.time }, System.currentTimeMillis()))
    }
    var selected by remember(hours) { mutableIntStateOf(0) }
    val bodyFont = LocalBodyFont.current
    val onSurface = MaterialTheme.colorScheme.onSurface

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colors = listOf(Color(0xFF0E0B3D), Color(0xFF1A1565)))),
    ) {
        Image(
            painter = painterResource(R.drawable.bg_blue_marble),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().alpha(0.12f),
        )
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 16.dp, top = 8.dp)) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.settings_back), tint = onSurface)
                }
                Spacer(Modifier.width(4.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.chart_view_title), color = onSurface, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = bodyFont)
                    if (data.locationName.isNotBlank()) {
                        Text(data.locationName, color = onSurface.copy(alpha = 0.7f), fontSize = 13.sp, fontFamily = bodyFont, maxLines = 1)
                    }
                }
            }
            hours.getOrNull(selected)?.let { hour ->
                SelectionHeader(hour, data.utcOffsetSeconds, metricPrimary, localizeDigits, onToggleUnits)
            }
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(top = 4.dp, bottom = 16.dp)) {
                // The hours run left to right in every language, as in the other charts of the app
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    ChartPanels(hours, data.utcOffsetSeconds, metricPrimary, localizeDigits) { selected = it }
                }
            }
        }
    }
}

/**
 * The values of the selected hour, each in both units, on one or more lines. Screen readers read it
 * again when the selection moves; a tap changes the units, as everywhere in the app.
 */
@Composable
private fun SelectionHeader(hour: ChartHour, utcOffsetSeconds: Int, metricPrimary: Boolean, localizeDigits: (String) -> String, onToggleUnits: () -> Unit) {
    val appLocale = LocalConfiguration.current.locales[0]
    val timeFormat = remember(appLocale, utcOffsetSeconds) {
        SimpleDateFormat("EEE d MMM, HH:mm", appLocale).apply { timeZone = buildLocationTimeZone(utcOffsetSeconds) }
    }
    val directions = stringArrayResource(R.array.cardinal_directions)
    val onSurface = MaterialTheme.colorScheme.onSurface
    val bodyFont = LocalBodyFont.current
    val displayFont = LocalDisplayFont.current
    val percent = { v: Int -> localizeDigits("$v%") }

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, onClickLabel = stringResource(R.string.cd_toggle_units), onClick = onToggleUnits)
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(localizeDigits(timeFormat.format(Date(hour.time))), color = onSurface, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = bodyFont)
        Image(painterResource(weatherIconRes(hour.weatherCode, hour.isNight)), contentDescription = null, modifier = Modifier.height(28.dp).width(28.dp))
        @Composable
        fun value(label: String?, dual: Pair<String, String>?, extra: String? = null) {
            Text(
                buildAnnotatedString {
                    if (label != null) {
                        withStyle(SpanStyle(color = onSurface.copy(alpha = 0.55f), fontSize = 11.sp, fontFamily = bodyFont)) { append("$label ") }
                    }
                    // Ltr isolate: keep "12°C" in number-then-unit order in Arabic
                    append('⁦')
                    if (dual != null) {
                        withStyle(SpanStyle(color = onSurface, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = displayFont)) { append(localizeDigits(dual.first)) }
                        withStyle(SpanStyle(color = onSurface.copy(alpha = 0.6f), fontSize = 10.sp, fontFamily = displayFont)) { append(" " + localizeDigits(dual.second)) }
                    }
                    append('⁩')
                    if (extra != null) {
                        withStyle(SpanStyle(color = onSurface.copy(alpha = 0.8f), fontSize = 12.sp, fontFamily = bodyFont)) { append(" $extra") }
                    }
                },
            )
        }
        hour.temperature?.let { value(null, it.displayDual(metricPrimary)) }
        hour.dewPoint?.let { value(stringResource(R.string.detail_dewpoint), it.displayDual(metricPrimary)) }
        hour.precipitation?.let { p ->
            value(stringResource(R.string.detail_precipitation), p.displayDual(metricPrimary),
                hour.precipProbability?.let { stringResource(R.string.precip_chance, percent(it)) })
        }
        hour.windSpeed?.let { w ->
            val direction = hour.windDirection?.takeIf { showWindArrow(w) }?.let { directions[((it % 360 + 360) / 22.5 + 0.5).toInt() % 16] }
            value(stringResource(R.string.detail_wind), w.displayDual(metricPrimary), direction)
        }
        hour.windGust?.let { value(stringResource(R.string.detail_wind_gust), it.displayDual(metricPrimary)) }
        hour.pressure?.let { value(stringResource(R.string.detail_pressure), it.displayDual(metricPrimary)) }
        hour.humidity?.let { value(stringResource(R.string.detail_humidity), null, percent(it)) }
        hour.uvIndex?.let { uv ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.detail_uv_index) + " ", color = onSurface.copy(alpha = 0.55f), fontSize = 11.sp, fontFamily = bodyFont)
                // The tier of the value as it shows: 2.6 shows as 3, which is moderate
                val shown = uv.roundToInt()
                val colors = colorsFor(shown.toDouble())
                Text(
                    localizeDigits(shown.toString()),
                    color = colors.text,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = displayFont,
                    modifier = Modifier.background(colors.background, RoundedCornerShape(6.dp)).padding(horizontal = 6.dp),
                )
            }
        }
    }
}

/**
 * The panels in one canvas: a fixed axis column at the start, and the hours, which scroll sideways.
 * The selected hour is the hour under the selection line, which moves from the start of the plot to the middle and,
 * at the end, to the end of the plot (chartLineOffset). Gestures: one finger scrolls the hours, with a fling (a
 * vertical drag goes to the page; the first movement decides). A tap moves the hour under it to the selection line.
 */
@Composable
private fun ChartPanels(
    hours: List<ChartHour>,
    utcOffsetSeconds: Int,
    metricPrimary: Boolean,
    localizeDigits: (String) -> String,
    onSelect: (Int) -> Unit,
) {
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val textMeasurer = rememberTextMeasurer(cacheSize = 128)
    val displayFont = LocalDisplayFont.current
    val appLocale = LocalConfiguration.current.locales[0]
    val count = hours.size
    val axisPx = with(density) { AXIS_WIDTH.toPx() }
    val slotPx = with(density) { CHART_SLOT.dp.toPx() }

    // The start of the plot; it opens at 0, with the current hour (the first hour) under the selection line
    var scrollPx by remember(hours) { mutableFloatStateOf(0f) }
    var widthPx by remember { mutableIntStateOf(0) }
    // The plot width, read when it is used: the gesture and selection code is made once
    fun viewPx() = maxOf(0f, widthPx - axisPx)
    val scope = rememberCoroutineScope()
    // A positive delta moves to later hours (reverseDirection below), so animateScrollBy takes the distance in px
    val scrollState = rememberScrollableState { delta ->
        val old = scrollPx
        scrollPx = (old + delta).coerceIn(0f, chartMaxScroll(slotPx, count, viewPx()))
        scrollPx - old
    }
    // A tap moves the hour under it to the selection line, with a short animation
    fun scrollToTap(x: Float) {
        if (x < axisPx) return
        val i = chartIndexForX(scrollPx + x - axisPx, slotPx, count)
        if (i < 0) return
        scope.launch { scrollState.animateScrollBy(chartScrollForIndex(i, slotPx, count, viewPx()) - scrollPx) }
    }
    // The selected hour is the hour under the selection line: a tick each time a new hour comes under it
    val onSelectNow by rememberUpdatedState(onSelect)
    LaunchedEffect(hours) {
        var first = true
        snapshotFlow { chartLineIndex(scrollPx, slotPx, count, viewPx()) }.collect { i ->
            if (i < 0) return@collect
            onSelectNow(i)
            if (!first) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            first = false
        }
    }

    val series = remember(hours, metricPrimary) { ChartSeries(hours, metricPrimary) }
    // Each scale is computed once from all the hours, so it stays the same while the hours scroll
    val tDomain = remember(series) { chartDomain(series.temperature + series.dewPoint, fromZero = false, minSpan = if (metricPrimary) 5.0 else 9.0) }
    val pDomain = remember(series) { chartDomain(series.precipitation, fromZero = true, minSpan = if (metricPrimary) 1.0 else 0.04) }
    val wDomain = remember(series) { chartDomain(series.wind + series.gust, fromZero = true, minSpan = if (metricPrimary) 5.0 else 10.0) }
    val prDomain = remember(series) { chartDomain(series.pressure, fromZero = false, minSpan = if (metricPrimary) 4.0 else 0.12) }
    val times = remember(hours) { hours.map { it.time } }
    val days = remember(hours, utcOffsetSeconds) { chartDaySpans(times, utcOffsetSeconds) }
    val localHours = remember(hours, utcOffsetSeconds) { times.map { Math.floorMod((it + utcOffsetSeconds * 1000L) / HOUR, 24L).toInt() } }
    val locationZone = remember(utcOffsetSeconds) { buildLocationTimeZone(utcOffsetSeconds) }
    val hourFormat = remember(locationZone) { SimpleDateFormat("HH:mm", Locale.US).apply { timeZone = locationZone } }
    val dayFormat = remember(appLocale, locationZone) { SimpleDateFormat("EEE d MMM", appLocale).apply { timeZone = locationZone } }
    // One painter for each icon in use
    val iconRes = remember(hours) { hours.map { weatherIconRes(it.weatherCode, it.isNight) } }
    val painters: Map<Int, Painter> = iconRes.distinct().associateWith { res -> key(res) { painterResource(res) } }

    val titles = ChartTitles(
        temperature = stringResource(R.string.detail_temperature) + "  " + units(metricPrimary, "°C", "°F"),
        dewPoint = stringResource(R.string.detail_dewpoint),
        precipitation = stringResource(R.string.detail_precipitation) + "  " + units(metricPrimary, "mm", "in"),
        chance = stringResource(R.string.precip_chance, "%"),
        wind = stringResource(R.string.detail_wind) + "  " + units(metricPrimary, "m/s", "mph"),
        gust = stringResource(R.string.detail_wind_gust),
        pressure = stringResource(R.string.detail_pressure) + "  " + units(metricPrimary, "hPa", "inHg"),
        humidity = stringResource(R.string.detail_humidity) + "  %",
    )
    val summary = stringResource(R.string.chart_view_summary, localizeDigits(count.toString()))
    val totalHeight = ChartPanel.entries.sumOf { it.heightDp } + PANEL_GAP.value * (ChartPanel.entries.size - 1)
    val onSurface = MaterialTheme.colorScheme.onSurface

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(totalHeight.dp)
            .onSizeChanged { widthPx = it.width }
            .semantics { contentDescription = summary }
            // One finger scrolls the hours, with a fling; a vertical drag goes to the page (the first movement decides)
            .scrollable(scrollState, Orientation.Horizontal, reverseDirection = true)
            .pointerInput(hours, density) { detectTapGestures(onTap = { scrollToTap(it.x) }) },
    ) {
        if (count == 0) return@Canvas
        val viewW = size.width - axisPx
        val slot = slotPx
        val scroll = scrollPx.coerceIn(0f, chartMaxScroll(slot, count, viewW))
        val lineOffset = chartLineOffset(scroll, slot, count, viewW)
        val lineX = axisPx + lineOffset
        val drawn = chartVisibleRange(scroll, viewW, slot, count)
        fun cx(i: Int) = axisPx + i * slot + slot / 2 - scroll
        // The hour position under the selection line, for the dots on the lines (0 is the middle of the first hour)
        val linePos = (scroll + lineOffset) / slot - 0.5f
        val chartsTop = panelTop(ChartPanel.ICONS)
        val chartsBottom = panelTop(ChartPanel.HUMIDITY) + ChartPanel.HUMIDITY.heightDp.dp.toPx()

        val tArea = plotArea(ChartPanel.TEMPERATURE)
        val pArea = plotArea(ChartPanel.PRECIPITATION)
        val wArea = plotArea(ChartPanel.WIND, extraTop = ARROW_ROW.toPx())
        val prArea = plotArea(ChartPanel.PRESSURE)
        val hArea = plotArea(ChartPanel.HUMIDITY)

        clipRect(left = axisPx) {
            // Days: alternate backgrounds, and a stronger line at each midnight in every panel
            days.forEachIndexed { k, span ->
                val left = if (k == 0) cx(span.startIndex) - slot / 2 else cx(span.startIndex)
                val right = days.getOrNull(k + 1)?.let { cx(it.startIndex) } ?: (cx(span.endIndex - 1) + slot / 2)
                if (right < axisPx || left > size.width) return@forEachIndexed
                if (k % 2 == 1) drawRect(Color.White.copy(alpha = 0.03f), Offset(left, 0f), Size(right - left, size.height))
                if (k > 0) drawVerticalLine(left, ChartStyle.midnight, ChartStyle.midnightWidth.toPx())
            }
            // Night hours, faintly darker
            var runStart = -1
            for (i in drawn.first..drawn.last + 1) {
                val night = i <= drawn.last && hours[i].isNight
                if (night && runStart < 0) runStart = i
                if (!night && runStart >= 0) {
                    val left = cx(runStart) - slot / 2
                    drawRect(ChartStyle.night, Offset(left, chartsTop), Size(cx(i - 1) + slot / 2 - left, chartsBottom - chartsTop))
                    runStart = -1
                }
            }

            drawIcons(hours, drawn, localHours, ::cx, panelTop(ChartPanel.ICONS), painters, textMeasurer, displayFont, localizeDigits)

            // Temperature with the dew point on the same scale, and a dashed line at freezing
            tDomain?.let { d ->
                drawGrid(d, tArea)
                val freezing = if (metricPrimary) 0.0 else 32.0
                if (freezing > d.min && freezing < d.max) {
                    val fy = tArea.y(freezing, d)
                    drawLine(ChartStyle.freezing, Offset(axisPx, fy), Offset(size.width, fy), ChartStyle.freezingWidth.toPx(), pathEffect = chartDash(ChartStyle.freezingDash))
                }
                drawSeries(series.dewPoint, drawn, ::cx, { tArea.y(it, d) }, ChartStyle.dewPoint, ChartStyle.thinLineWidth.toPx())
                drawSeries(series.temperature, drawn, ::cx, { tArea.y(it, d) }, ChartStyle.temperature, ChartStyle.lineWidth.toPx())
                drawSelectedDot(series.dewPoint, linePos, lineX) { tArea.y(it, d) }
                drawSelectedDot(series.temperature, linePos, lineX) { tArea.y(it, d) }
            }

            // Precipitation as bars, the chance of rain as a thin line on 0-100 %
            pDomain?.let { d ->
                drawGrid(d, pArea)
                val barWidth = maxOf(2.dp.toPx(), slot * 0.6f)
                for (i in drawn) {
                    val v = series.precipitation[i] ?: continue
                    if (v <= 0) continue
                    val top = pArea.y(v, d)
                    drawRoundRect(ChartStyle.precipitation, Offset(cx(i) - barWidth / 2, top), Size(barWidth, pArea.bottom - top), CornerRadius(1.5.dp.toPx()))
                }
            }
            drawSeries(series.chance, drawn, ::cx, { pArea.y(it, PERCENT_DOMAIN) }, ChartStyle.chance, ChartStyle.chanceWidth.toPx())
            drawSelectedDot(series.chance, linePos, lineX) { pArea.y(it, PERCENT_DOMAIN) }

            // Wind and gusts on one scale, with the direction every 3 hours
            wDomain?.let { d ->
                drawGrid(d, wArea)
                drawSeries(series.gust, drawn, ::cx, { wArea.y(it, d) }, ChartStyle.gust, ChartStyle.thinLineWidth.toPx(), dashed = true)
                drawSeries(series.wind, drawn, ::cx, { wArea.y(it, d) }, ChartStyle.wind, ChartStyle.lineWidth.toPx())
                drawSelectedDot(series.gust, linePos, lineX) { wArea.y(it, d) }
                drawSelectedDot(series.wind, linePos, lineX) { wArea.y(it, d) }
            }
            val arrowY = wArea.top - ARROW_ROW.toPx() / 2
            for (i in drawn) {
                if (localHours[i] % 3 != 0) continue
                val speed = hours[i].windSpeed ?: continue
                val direction = hours[i].windDirection ?: continue
                if (showWindArrow(speed)) drawWindArrow(Offset(cx(i), arrowY), direction)
            }

            prDomain?.let { d ->
                drawGrid(d, prArea)
                drawSeries(series.pressure, drawn, ::cx, { prArea.y(it, d) }, ChartStyle.pressure, ChartStyle.lineWidth.toPx())
                drawSelectedDot(series.pressure, linePos, lineX) { prArea.y(it, d) }
            }

            drawGrid(PERCENT_DOMAIN, hArea)
            drawSeries(series.humidity, drawn, ::cx, { hArea.y(it, PERCENT_DOMAIN) }, ChartStyle.humidity, ChartStyle.lineWidth.toPx())
            drawSelectedDot(series.humidity, linePos, lineX) { hArea.y(it, PERCENT_DOMAIN) }

            // Time axes at the top and the bottom: an hour label every 3 or 6 hours, and the day at each midnight
            // (the day at the start stays in view). The top axis is the bottom one mirrored: the labels are above their ticks.
            val hourStyle = TextStyle(color = onSurface.copy(alpha = 0.6f), fontSize = 10.sp, fontFamily = displayFont)
            val dayStyle = TextStyle(color = onSurface, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = displayFont)
            val tick = 3.dp.toPx()
            val dayOffset = 18.dp.toPx()
            fun drawTimeAxis(panel: ChartPanel, mirrored: Boolean) {
                val top = panelTop(panel)
                val bottom = top + panel.heightDp.dp.toPx()
                for (i in drawn) {
                    if (localHours[i] % 3 != 0) continue
                    val tickTop = if (mirrored) bottom - tick else top
                    drawLine(Color.White.copy(alpha = 0.3f), Offset(cx(i), tickTop), Offset(cx(i), tickTop + tick), 1.dp.toPx())
                    val layout = textMeasurer.measure(localizeDigits(hourFormat.format(Date(times[i]))), hourStyle)
                    val y = if (mirrored) bottom - tick - layout.size.height else top + tick
                    drawText(layout, topLeft = Offset(cx(i) - layout.size.width / 2f, y))
                }
                days.forEachIndexed { k, span ->
                    val start = if (k == 0) cx(span.startIndex) - slot / 2 else cx(span.startIndex)
                    val end = days.getOrNull(k + 1)?.let { cx(it.startIndex) } ?: (cx(span.endIndex - 1) + slot / 2)
                    if (end < axisPx || start > size.width) return@forEachIndexed
                    val layout = textMeasurer.measure(localizeDigits(dayFormat.format(Date(span.date))), dayStyle)
                    val pad = 4.dp.toPx()
                    val left = maxOf(start, axisPx) + pad
                    val y = if (mirrored) bottom - dayOffset - layout.size.height else top + dayOffset
                    if (left + layout.size.width + pad <= end) drawText(layout, topLeft = Offset(left, y))
                }
            }
            drawTimeAxis(ChartPanel.TOP_TIME, mirrored = true)
            drawTimeAxis(ChartPanel.TIME, mirrored = false)

            // The selection line: the selected hour is under it, in every panel
            drawVerticalLine(lineX, ChartStyle.selection, ChartStyle.selectionWidth.toPx())

            // Titles on top of the lines, at the start of each panel; they do not scroll
            drawTitle(ChartPanel.TEMPERATURE, titles.temperature, titles.dewPoint, ChartStyle.dewPoint, axisPx, textMeasurer, onSurface)
            drawTitle(ChartPanel.PRECIPITATION, titles.precipitation, titles.chance, ChartStyle.chance, axisPx, textMeasurer, onSurface)
            drawTitle(ChartPanel.WIND, titles.wind, titles.gust, ChartStyle.gust, axisPx, textMeasurer, onSurface)
            drawTitle(ChartPanel.PRESSURE, titles.pressure, null, Color.Unspecified, axisPx, textMeasurer, onSurface)
            drawTitle(ChartPanel.HUMIDITY, titles.humidity, null, Color.Unspecified, axisPx, textMeasurer, onSurface)
            // The chance of rain scale at the end of the panel
            val chanceStyle = TextStyle(color = ChartStyle.chance, fontSize = 8.sp, fontFamily = displayFont)
            listOf(50.0, 100.0).forEach { v ->
                val layout = textMeasurer.measure(localizeDigits("${v.toInt()}%"), chanceStyle)
                drawText(layout, topLeft = Offset(size.width - layout.size.width - 2.dp.toPx(), pArea.y(v, PERCENT_DOMAIN) - layout.size.height / 2f))
            }
        }

        // The axis column: the values of each scale in the primary unit, with the other unit small under them
        val axis = AxisLabels(textMeasurer, displayFont, onSurface, axisPx, localizeDigits)
        tDomain?.let { axis.draw(this, it, tArea, if (metricPrimary) ::celsiusToFahrenheit else ::fahrenheitToCelsius, 0) }
        pDomain?.let { axis.draw(this, it, pArea, if (metricPrimary) { v -> v * 0.03937 } else { v -> v / 0.03937 }, if (metricPrimary) 2 else 1) }
        wDomain?.let { axis.draw(this, it, wArea, if (metricPrimary) { v -> v * 2.23694 } else { v -> v / 2.23694 }, if (metricPrimary) 0 else 1) }
        prDomain?.let { axis.draw(this, it, prArea, if (metricPrimary) { v -> v * 0.02953 } else { v -> v / 0.02953 }, if (metricPrimary) 2 else 0) }
        axis.draw(this, PERCENT_DOMAIN, hArea, null, 0, suffix = "%")
    }
}

private fun units(metricPrimary: Boolean, metric: String, imperial: String) =
    if (metricPrimary) "$metric · $imperial" else "$imperial · $metric"

private fun celsiusToFahrenheit(c: Double) = c * 9 / 5 + 32
private fun fahrenheitToCelsius(f: Double) = (f - 32) * 5 / 9

/** The fixed 0-100 % scale of the chance of rain and the humidity. */
private val PERCENT_DOMAIN = ChartDomain(0.0, 100.0, listOf(0.0, 50.0, 100.0))

private class ChartTitles(
    val temperature: String, val dewPoint: String,
    val precipitation: String, val chance: String,
    val wind: String, val gust: String,
    val pressure: String, val humidity: String,
)

/** The part of a panel where the values go, from [top] to [bottom] px. */
private class PlotArea(val top: Float, val bottom: Float) {
    fun y(v: Double, d: ChartDomain): Float =
        if (d.max > d.min) (bottom - (v - d.min) / (d.max - d.min) * (bottom - top)).toFloat() else bottom
}

/** The top of [panel] in px. */
private fun DrawScope.panelTop(panel: ChartPanel): Float =
    ChartPanel.entries.takeWhile { it != panel }.sumOf { (it.heightDp.dp + PANEL_GAP).toPx().toDouble() }.toFloat()

private fun DrawScope.plotArea(panel: ChartPanel, extraTop: Float = 0f): PlotArea {
    val top = panelTop(panel)
    val title = if (panel.titled) TITLE_HEIGHT.toPx() else 0f
    return PlotArea(top + title + extraTop + 4.dp.toPx(), top + panel.heightDp.dp.toPx() - 4.dp.toPx())
}

/**
 * A vertical line at [x] in the plot area of each panel, with a break in the gaps and title lines between
 * the panels, so that each chart shows where it ends. The time axes have their own short ticks.
 */
private fun DrawScope.drawVerticalLine(x: Float, color: Color, width: Float) {
    ChartPanel.entries.filter { it != ChartPanel.TIME && it != ChartPanel.TOP_TIME }.forEach { panel ->
        val area = plotArea(panel)
        drawLine(color, Offset(x, area.top), Offset(x, area.bottom), width)
    }
}

private fun DrawScope.drawGrid(d: ChartDomain, area: PlotArea) {
    d.ticks.forEach { v ->
        val y = area.y(v, d)
        drawLine(Hairline, Offset(0f, y), Offset(size.width, y), 1f)
    }
}

/** A line through the hours of [range]; a missing value leaves a gap, and each run is a monotone curve. */
private fun DrawScope.drawSeries(
    values: List<Double?>, range: IntRange, x: (Int) -> Float, y: (Double) -> Float,
    color: Color, width: Float, dashed: Boolean = false,
) {
    val effect = if (dashed) chartDash(ChartStyle.gustDash) else null
    var run = mutableListOf<Offset>()
    fun flush() {
        if (run.size == 1) drawCircle(color, width, run[0])
        if (run.size >= 2) {
            val cps = computeMonotoneCubicControlPoints(run)
            val path = Path().apply {
                moveTo(run[0].x, run[0].y)
                cps.forEachIndexed { k, cp -> cubicTo(cp.cp1.x, cp.cp1.y, cp.cp2.x, cp.cp2.y, run[k + 1].x, run[k + 1].y) }
            }
            drawPath(path, color, style = Stroke(width, pathEffect = effect))
        }
        run = mutableListOf()
    }
    for (i in range) {
        val v = values[i]
        if (v == null || !v.isFinite()) flush() else run += Offset(x(i), y(v))
    }
    flush()
}

/**
 * A dot on a line at the selection line ([x]); [position] is the hour position under it. Between two hours the value
 * is on the straight line between them; next to a missing value, the nearest hour gives it.
 */
private fun DrawScope.drawSelectedDot(values: List<Double?>, position: Float, x: Float, y: (Double) -> Float) {
    val i = floor(position).toInt()
    val t = position - i
    val a = values.getOrNull(i)?.takeIf { it.isFinite() }
    val b = values.getOrNull(i + 1)?.takeIf { it.isFinite() }
    val v = if (a != null && b != null) a + (b - a) * t else (if (t < 0.5f) a else b) ?: return
    drawCircle(ChartStyle.selectionDot, ChartStyle.selectionDotRadius.toPx(), Offset(x, y(v)))
}

/** The weather icon every 3 hours (night icons at night), with the UV index under it from 1, in its tier colour. */
private fun DrawScope.drawIcons(
    hours: List<ChartHour>, range: IntRange, localHours: List<Int>, x: (Int) -> Float, top: Float,
    painters: Map<Int, Painter>, textMeasurer: TextMeasurer, font: FontFamily, localizeDigits: (String) -> String,
) {
    val iconSize = 26.dp.toPx()
    for (i in range) {
        if (localHours[i] % 3 != 0) continue
        val hour = hours[i]
        painters[weatherIconRes(hour.weatherCode, hour.isNight)]?.let { painter ->
            translate(x(i) - iconSize / 2, top) { with(painter) { draw(Size(iconSize, iconSize)) } }
        }
        // The value as it shows, and its tier: 2.6 shows as 3, which is moderate
        val uv = hour.uvIndex?.roundToInt() ?: continue
        if (uv < 1) continue
        val colors = colorsFor(uv.toDouble())
        val layout = textMeasurer.measure(localizeDigits(uv.toString()), TextStyle(color = colors.text, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = font))
        val w = layout.size.width + 8.dp.toPx()
        val h = layout.size.height.toFloat()
        val badgeTop = top + iconSize + 2.dp.toPx()
        drawRoundRect(colors.background, Offset(x(i) - w / 2, badgeTop), Size(w, h), CornerRadius(h / 2))
        drawText(layout, topLeft = Offset(x(i) - layout.size.width / 2f, badgeTop))
    }
}

/** An arrow that points to where the wind comes from, as the compass of the wind cards. */
private fun DrawScope.drawWindArrow(center: Offset, degrees: Int) {
    val u = 1.dp.toPx()
    rotate(degrees.toFloat(), center) {
        val path = Path().apply {
            moveTo(center.x, center.y - 5 * u)
            lineTo(center.x + 3.5f * u, center.y + 4 * u)
            lineTo(center.x, center.y + 2 * u)
            lineTo(center.x - 3.5f * u, center.y + 4 * u)
            close()
        }
        drawPath(path, Color.White.copy(alpha = 0.75f))
    }
}

/** The panel name and units, and the name of the second line in its colour. */
private fun DrawScope.drawTitle(
    panel: ChartPanel, title: String, second: String?, secondColor: Color, left: Float, textMeasurer: TextMeasurer, color: Color,
) {
    val top = panelTop(panel)
    val text: AnnotatedString = buildAnnotatedString {
        withStyle(SpanStyle(color = color.copy(alpha = 0.75f), fontWeight = FontWeight.SemiBold)) { append(title) }
        if (second != null) withStyle(SpanStyle(color = secondColor)) { append("   ● $second") }
    }
    val layout = textMeasurer.measure(text, TextStyle(fontSize = 10.sp, textDirection = TextDirection.Content))
    drawText(layout, topLeft = Offset(left + 6.dp.toPx(), top + 1.dp.toPx()))
}

/** Draws the labels of a scale in the axis column. */
private class AxisLabels(
    val textMeasurer: TextMeasurer, val font: FontFamily, val color: Color, val width: Float, val localizeDigits: (String) -> String,
) {
    /**
     * The ticks of [d] in [area], each with the value in the other unit ([toOther], with [otherDecimals]) small
     * under it. A label that would cover the one under it is left out.
     */
    fun draw(scope: DrawScope, d: ChartDomain, area: PlotArea, toOther: ((Double) -> Double)?, otherDecimals: Int, suffix: String = "") = with(scope) {
        val step = if (d.ticks.size > 1) d.ticks[1] - d.ticks[0] else 1.0
        val decimals = (0..3).firstOrNull { abs(step * Math.pow(10.0, it.toDouble()) - (step * Math.pow(10.0, it.toDouble())).roundToInt()) < 1e-6 } ?: 3
        var lastTop = Float.MAX_VALUE
        d.ticks.forEach { v ->
            val text = buildAnnotatedString {
                withStyle(SpanStyle(color = color.copy(alpha = 0.8f), fontSize = 10.sp)) { append(localizeDigits(format(v, decimals) + suffix)) }
                if (toOther != null) {
                    withStyle(SpanStyle(color = color.copy(alpha = 0.5f), fontSize = 8.sp)) { append("\n" + localizeDigits(format(toOther(v), otherDecimals))) }
                }
            }
            val layout = textMeasurer.measure(text, TextStyle(fontFamily = font, lineHeight = 10.sp, textDirection = TextDirection.Ltr))
            val y = area.y(v, d)
            val labelTop = (y - 6.dp.toPx()).coerceIn(area.top - 6.dp.toPx(), area.bottom + 4.dp.toPx() - layout.size.height)
            if (labelTop + layout.size.height > lastTop) return@forEach
            drawText(layout, topLeft = Offset(width - layout.size.width - 4.dp.toPx(), labelTop))
            lastTop = labelTop
        }
    }

    private fun format(v: Double, decimals: Int) = String.format(Locale.US, "%.${decimals}f", v)
}
