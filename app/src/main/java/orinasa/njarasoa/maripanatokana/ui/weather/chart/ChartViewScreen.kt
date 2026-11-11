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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import orinasa.njarasoa.maripanatokana.R
import orinasa.njarasoa.maripanatokana.domain.model.AqiStandard
import orinasa.njarasoa.maripanatokana.domain.model.ChartHour
import orinasa.njarasoa.maripanatokana.domain.model.HourlyAirQuality
import orinasa.njarasoa.maripanatokana.domain.model.Moon
import orinasa.njarasoa.maripanatokana.domain.model.Temperature
import orinasa.njarasoa.maripanatokana.domain.model.ThermalIndices
import orinasa.njarasoa.maripanatokana.domain.model.WeatherData
import orinasa.njarasoa.maripanatokana.domain.model.showWindArrow
import orinasa.njarasoa.maripanatokana.ui.theme.Hairline
import orinasa.njarasoa.maripanatokana.ui.theme.LocalBodyFont
import orinasa.njarasoa.maripanatokana.ui.theme.LocalBodyFontFeatures
import orinasa.njarasoa.maripanatokana.ui.theme.LocalDisplayFont
import orinasa.njarasoa.maripanatokana.ui.theme.SectionSurface
import orinasa.njarasoa.maripanatokana.ui.theme.TileSurface
import orinasa.njarasoa.maripanatokana.ui.theme.temperatureColor
import orinasa.njarasoa.maripanatokana.ui.weather.DualUnitText
import orinasa.njarasoa.maripanatokana.ui.weather.HeroStatDivider
import orinasa.njarasoa.maripanatokana.ui.weather.PrecipChance
import orinasa.njarasoa.maripanatokana.ui.weather.buildLocationTimeZone
import orinasa.njarasoa.maripanatokana.ui.weather.components.drawMoon
import orinasa.njarasoa.maripanatokana.ui.weather.glass
import orinasa.njarasoa.maripanatokana.ui.weather.components.colorsFor
import orinasa.njarasoa.maripanatokana.ui.weather.components.computeMonotoneCubicControlPoints
import orinasa.njarasoa.maripanatokana.ui.weather.components.dualLabel
import orinasa.njarasoa.maripanatokana.ui.weather.components.weatherIconRes
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt

private const val HOUR = 3_600_000L
// The weather icons, wind arrows and hour labels show every this many local hours (00, 04, 08…)
private const val LABEL_HOURS = 4

/**
 * The panels from top to bottom, with their height in dp. The value panels have a title line at the top and a band
 * behind them. The UV index panel, with the air quality on it, is shown only when an hour has a UV index or air quality.
 */
private enum class ChartPanel(val heightDp: Int, val titled: Boolean) {
    // The time axis at the top: the bottom one mirrored
    TOP_TIME(36, false),
    ICONS(46, false),
    TEMPERATURE(156, true),
    PRECIPITATION(100, true),
    WIND(128, true),
    PRESSURE(104, true),
    HUMIDITY(104, true),
    UV_INDEX(104, true),
    TIME(36, false),
}

private val AXIS_WIDTH = 52.dp
private val PANEL_GAP = 10.dp
private val TITLE_HEIGHT = 22.dp
// The panel bands start this far from the sides of the card, and the titles this far from the start of the band
private val BAND_INSET = 6.dp
private val TITLE_INSET = 10.dp
// The room above the wind line for the direction arrows
private val ARROW_ROW = 14.dp
// The room above and under each primary line for the labels of the day high and low, and above the rain bars
private val EXTREME_ROW = 12.dp
// The moon after each day label of the time axes: its radius, and the gap on each side of it
private val MOON_R = 5.dp
private val MOON_GAP = 4.dp
// The cards, as the section cards of the main screen
private val CARD_SHAPE = RoundedCornerShape(20.dp)
private val CARD_MARGIN = 12.dp
// The size of the values in the selection card, the height of the line under a value, and the padding of a label
private val VALUE_SIZE = 13.sp
private val EXTRA_LINE = 16.dp
private val LABEL_PADDING = 2.dp

/** The label of a tile: its text, its size in sp and the number of lines of its row. */
private class TileLabel(val text: String, val size: Float, val lines: Int)

private val ChartTile.labelRes: Int
    get() = when (this) {
        ChartTile.TEMPERATURE -> R.string.detail_temperature
        ChartTile.DEW_POINT -> R.string.detail_dewpoint
        ChartTile.PRECIPITATION -> R.string.detail_precipitation
        ChartTile.WIND -> R.string.detail_wind
        ChartTile.WIND_GUST -> R.string.detail_wind_gust
        ChartTile.PRESSURE -> R.string.detail_pressure
        ChartTile.HUMIDITY -> R.string.detail_humidity
        ChartTile.UV_INDEX -> R.string.detail_uv_index
        ChartTile.AIR_QUALITY -> R.string.detail_air_quality
    }

/** The values of one hour in the primary unit, as the charts draw them. */
private class ChartSeries(hours: List<ChartHour>, private val metricPrimary: Boolean) {
    private fun Temperature.primary() = if (metricPrimary) celsius else fahrenheit
    val temperature = hours.map { it.temperature?.primary() }
    val dewPoint = hours.map { it.dewPoint?.primary() }
    // The NWS heat index and wind chill of each hour; null where they do not apply
    val heatIndex = hours.map { h -> h.temperature?.let { ThermalIndices.heatIndex(it, h.humidity) }?.primary() }
    val windChill = hours.map { h -> h.temperature?.let { t -> h.windSpeed?.let { ThermalIndices.windChill(t, it) } }?.primary() }
    val uv = hours.map { it.uvIndex?.takeIf { v -> v.isFinite() } }
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
    hourlyAirQuality: List<HourlyAirQuality>,
    aqiStandard: AqiStandard,
    localizeDigits: (String) -> String,
    onToggleUnits: () -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    // The data can be some hours old: start again at the current hour
    val hours = remember(data.chartHourly) {
        data.chartHourly.slice(chartHours(data.chartHourly.map { it.time }, System.currentTimeMillis()))
    }
    // The air quality of each hour; null when no entry is near
    val airQuality = remember(hours, hourlyAirQuality) {
        chartAirQuality(hours.map { it.time }, hourlyAirQuality.map { it.time }).map { it?.let(hourlyAirQuality::get) }
    }
    val tiles = remember(hours, airQuality) { chartTiles(hours, airQuality.any { it != null }) }
    var selected by remember(hours) { mutableIntStateOf(0) }
    val bodyFont = LocalBodyFont.current
    val displayFont = LocalDisplayFont.current
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
                // The place name as on the main screen, with the title under it; no name: the title is the big line
                Column(modifier = Modifier.weight(1f)) {
                    val title = stringResource(R.string.chart_view_title)
                    val name = data.locationName.ifBlank { title }
                    Text(
                        name, color = onSurface, fontSize = 30.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold,
                        fontFamily = displayFont, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    if (data.locationName.isNotBlank()) {
                        Text(title, color = onSurface.copy(alpha = 0.6f), fontSize = 14.sp, fontFamily = bodyFont, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            hours.getOrNull(selected)?.let { hour ->
                SelectionHeader(hour, tiles, airQuality.getOrNull(selected), aqiStandard, data.utcOffsetSeconds, metricPrimary, localizeDigits, onToggleUnits)
            }
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(top = 8.dp, bottom = 16.dp)) {
                // The chart card: the axis column and the panels, with no title row
                Box(
                    modifier = Modifier
                        .padding(horizontal = CARD_MARGIN)
                        .fillMaxWidth()
                        .glass(SectionSurface, CARD_SHAPE)
                        .clip(CARD_SHAPE)
                        .padding(vertical = 8.dp),
                ) {
                    // The hours run left to right in every language, as in the other charts of the app
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        ChartPanels(hours, airQuality, aqiStandard, data.utcOffsetSeconds, (data.latitude ?: 0.0) < 0, metricPrimary, localizeDigits) { selected = it }
                    }
                }
            }
        }
    }
}

/**
 * The card of the selected hour: its time and weather icon in the title row, then the values as tiles in rows of up
 * to 5 (chartTileRows), each in both units. The card is compact, so the charts get most of the screen, and it has the
 * same tiles (chartTiles) and size for every hour. Screen readers read it again when the selection moves; a tap
 * changes the units, as everywhere in the app.
 */
@Composable
private fun SelectionHeader(
    hour: ChartHour, tiles: List<ChartTile>, airQuality: HourlyAirQuality?, aqiStandard: AqiStandard, utcOffsetSeconds: Int, metricPrimary: Boolean,
    localizeDigits: (String) -> String, onToggleUnits: () -> Unit,
) {
    val appLocale = LocalConfiguration.current.locales[0]
    val timeFormat = remember(appLocale, utcOffsetSeconds) {
        SimpleDateFormat("EEE d MMM, HH:mm", appLocale).apply { timeZone = buildLocationTimeZone(utcOffsetSeconds) }
    }
    val directions = stringArrayResource(R.array.cardinal_directions)
    val onSurface = MaterialTheme.colorScheme.onSurface
    val bodyFont = LocalBodyFont.current
    val displayFont = LocalDisplayFont.current
    val percent = { v: Int -> localizeDigits("$v%") }

    // The style of the labels at a size; the labels are measured with the same style
    fun labelStyle(size: Float) = TextStyle(fontSize = size.sp, lineHeight = (size * 1.2f).sp, fontFamily = bodyFont, textAlign = TextAlign.Center)
    // One tile: the label at the size of the card (chartLabelFit), then the value; [extra] is the line under the value,
    // which keeps its height when it is empty, so the card does not change size when the selection moves
    @Composable
    fun Tile(modifier: Modifier, label: TileLabel, extra: (@Composable () -> Unit)? = null, value: @Composable () -> Unit) {
        Column(modifier = modifier.fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                label.text, color = onSurface.copy(alpha = 0.6f), style = labelStyle(label.size),
                minLines = label.lines, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = LABEL_PADDING),
            )
            Spacer(Modifier.height(2.dp))
            value()
            extra?.let { Box(Modifier.height(EXTRA_LINE), contentAlignment = Alignment.TopCenter) { it() } }
        }
    }
    // The value as two lines, the primary unit over the other, as the main screen shows it; no value: a dash and an
    // empty line
    @Composable
    fun dual(v: Pair<String, String>?) = DualUnitText(
        primary = v?.let { localizeDigits(it.first) } ?: "–", secondary = v?.let { localizeDigits(it.second) } ?: "\u00A0",
        primarySize = VALUE_SIZE, horizontalAlignment = Alignment.CenterHorizontally,
    )
    // A value in a pill of its colour; it moves up by its padding, so its text lines up with the values next to it
    @Composable
    fun pill(color: Color, content: @Composable () -> Unit) = Box(
        modifier = Modifier.offset(y = (-2).dp).clip(RoundedCornerShape(12.dp)).background(color).padding(horizontal = 10.dp, vertical = 2.dp),
    ) { content() }

    // A value that the hour does not have
    @Composable
    fun missing() = Text("–", color = onSurface, fontSize = VALUE_SIZE, fontWeight = FontWeight.Bold, fontFamily = displayFont, style = TextStyle())

    val cells = tiles.map<ChartTile, @Composable (Modifier, TileLabel) -> Unit> { tile ->
        when (tile) {
            ChartTile.TEMPERATURE -> { m, l ->
                Tile(m, l) {
                    hour.temperature?.let { t ->
                        val (p, s) = t.displayDual(metricPrimary)
                        // Dark text on the light colours of the scale, both units at full opacity
                        pill(temperatureColor(t.celsius)) {
                            DualUnitText(
                                primary = localizeDigits(p), secondary = localizeDigits(s), primarySize = VALUE_SIZE,
                                color = Color(0xFF1A1A1A), secondaryAlpha = 1f, horizontalAlignment = Alignment.CenterHorizontally,
                            )
                        }
                    } ?: dual(null)
                }
            }
            ChartTile.DEW_POINT -> { m, l -> Tile(m, l) { dual(hour.dewPoint?.displayDual(metricPrimary)) } }
            ChartTile.PRECIPITATION -> { m, l ->
                Tile(m, l, extra = {
                    hour.precipProbability?.takeIf { c -> c > 0 }?.let { c ->
                        PrecipChance(c, show = true, fontSize = 11.sp, fontFamily = bodyFont, fontFeatures = LocalBodyFontFeatures.current, localizeDigits = localizeDigits)
                    }
                }) { dual(hour.precipitation?.displayDual(metricPrimary)) }
            }
            ChartTile.WIND -> { m, l ->
                val direction = hour.windSpeed?.takeIf { showWindArrow(it) }?.let { hour.windDirection }
                    ?.let { directions[((it % 360 + 360) / 22.5 + 0.5).toInt() % 16] }
                Tile(m, l, extra = {
                    direction?.let { d ->
                        Text(d, color = onSurface.copy(alpha = 0.7f), fontSize = 11.sp, lineHeight = 13.sp, fontFamily = bodyFont, style = TextStyle(), modifier = Modifier.padding(top = 2.dp))
                    }
                }) { dual(hour.windSpeed?.displayDual(metricPrimary)) }
            }
            ChartTile.WIND_GUST -> { m, l -> Tile(m, l) { dual(hour.windGust?.displayDual(metricPrimary)) } }
            ChartTile.PRESSURE -> { m, l -> Tile(m, l) { dual(hour.pressure?.displayDual(metricPrimary)) } }
            ChartTile.HUMIDITY -> { m, l ->
                Tile(m, l) {
                    hour.humidity?.let { h ->
                        Text(percent(h), color = onSurface, fontSize = VALUE_SIZE, fontWeight = FontWeight.Bold, fontFamily = displayFont, style = TextStyle())
                    } ?: missing()
                }
            }
            ChartTile.UV_INDEX -> { m, l ->
                Tile(m, l) {
                    hour.uvIndex?.takeIf { it.isFinite() }?.let { uv ->
                        // The tier of the value as it shows: 2.6 shows as 3, which is moderate
                        val shown = uv.roundToInt()
                        val colors = colorsFor(shown.toDouble())
                        pill(colors.background) {
                            Text(localizeDigits(shown.toString()), color = colors.text, fontSize = VALUE_SIZE, fontWeight = FontWeight.Bold, fontFamily = displayFont, style = TextStyle())
                        }
                    } ?: missing()
                }
            }
            ChartTile.AIR_QUALITY -> { m, l ->
                Tile(m, l) {
                    airQuality?.let { aq ->
                        // The primary standard in a pill of its tier colour; the panel title names the standard
                        val primary = if (aqiStandard == AqiStandard.EUROPEAN) aq.europeanValue else aq.usValue
                        val colors = colorsFor(aq.tier(aqiStandard))
                        pill(colors.background) {
                            Text(localizeDigits(primary.toString()), color = colors.text, fontSize = VALUE_SIZE, fontWeight = FontWeight.Bold, fontFamily = displayFont, style = TextStyle())
                        }
                    } ?: missing()
                }
            }
        }
    }
    // The tiles spread evenly on the rows
    val rowSizes = chartTileRows(cells.size)
    val rowStarts = rowSizes.runningFold(0) { start, n -> start + n }
    val labels = tiles.map { stringResource(it.labelRes) }
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current

    Column(
        modifier = Modifier
            .padding(start = CARD_MARGIN, end = CARD_MARGIN, top = 8.dp)
            .fillMaxWidth()
            .glass(SectionSurface, CARD_SHAPE)
            .clip(CARD_SHAPE)
            .clickable(role = Role.Button, onClickLabel = stringResource(R.string.cd_toggle_units), onClick = onToggleUnits)
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 12.dp).heightIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                localizeDigits(timeFormat.format(Date(hour.time))), color = onSurface, fontSize = 17.sp, fontWeight = FontWeight.Bold,
                fontFamily = bodyFont, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
            )
            Image(painterResource(weatherIconRes(hour.weatherCode, hour.isNight)), contentDescription = null, modifier = Modifier.size(32.dp))
        }
        BoxWithConstraints(modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp)) {
            // The width of a label in each tile: the row less its dividers, shared by its tiles, less the label padding
            val rowWidth = constraints.maxWidth.toFloat()
            val fit = remember(labels, rowWidth, bodyFont, density) {
                val widths = rowSizes.flatMap { n ->
                    with(density) { List(n) { (rowWidth - (n - 1) * 1.dp.toPx()) / n - 2 * LABEL_PADDING.toPx() } }
                }
                chartLabelFit(labels, widths) { text, size ->
                    measurer.measure(text, labelStyle(size), softWrap = false, maxLines = 1, density = density).size.width.toFloat()
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                rowSizes.forEachIndexed { r, n ->
                    // The labels of a row take as many lines as the longest one, so the values line up
                    val start = rowStarts[r]
                    val lines = fit.lines.subList(start, start + n).maxOrNull() ?: 1
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min)
                            .background(TileSurface, RoundedCornerShape(16.dp))
                            .padding(vertical = 8.dp),
                    ) {
                        for (i in start until start + n) {
                            if (i > start) HeroStatDivider()
                            cells[i](Modifier.weight(1f), TileLabel(labels[i], fit.size, lines))
                        }
                    }
                }
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
    airQuality: List<HourlyAirQuality?>,
    aqiStandard: AqiStandard,
    utcOffsetSeconds: Int,
    // South of the equator, where the moon is seen mirrored
    southern: Boolean,
    metricPrimary: Boolean,
    localizeDigits: (String) -> String,
    onSelect: (Int) -> Unit,
) {
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val textMeasurer = rememberTextMeasurer(cacheSize = 128)
    val displayFont = LocalDisplayFont.current
    val bodyFont = LocalBodyFont.current
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
    val tDomain = remember(series) {
        chartDomain(series.temperature + series.dewPoint + series.heatIndex + series.windChill, fromZero = false, minSpan = if (metricPrimary) 5.0 else 9.0)
    }
    val pDomain = remember(series) { chartDomain(series.precipitation, fromZero = true, minSpan = if (metricPrimary) 1.0 else 0.04) }
    val wDomain = remember(series) { chartDomain(series.wind + series.gust, fromZero = true, minSpan = if (metricPrimary) 5.0 else 10.0) }
    val prDomain = remember(series) { chartDomain(series.pressure, fromZero = false, minSpan = if (metricPrimary) 4.0 else 0.12) }
    val uvDomain = remember(series) { chartDomain(series.uv, fromZero = true, minSpan = 5.0) }
    // The air quality in the primary standard, on its own scale at the end of the UV index panel
    val aqValues = remember(airQuality, aqiStandard) {
        airQuality.map { it?.let { aq -> (if (aqiStandard == AqiStandard.EUROPEAN) aq.europeanValue else aq.usValue).toDouble() } }
    }
    val aqDomain = remember(aqValues) { chartDomain(aqValues, fromZero = true, minSpan = 50.0) }
    // No UV index and no air quality in any hour: no UV index panel
    val showUv = uvDomain != null || aqDomain != null
    val panels = remember(showUv) { ChartPanel.entries.filter { it != ChartPanel.UV_INDEX || showUv } }
    // The last hour with air quality before the first hours without it, where the "no data" label starts
    val aqEnd = remember(aqValues) { (0 until aqValues.size - 1).firstOrNull { aqValues[it] != null && aqValues[it + 1] == null } }
    val times = remember(hours) { hours.map { it.time } }
    val days = remember(hours, utcOffsetSeconds) { chartDaySpans(times, utcOffsetSeconds) }
    // The high and low of each day on the primary line of each panel
    val tempExtremes = remember(series, days) { chartDayExtremes(series.temperature, days) }
    val windExtremes = remember(series, days) { chartDayExtremes(series.wind, days) }
    val pressureExtremes = remember(series, days) { chartDayExtremes(series.pressure, days) }
    val humidityExtremes = remember(series, days) { chartDayExtremes(series.humidity, days) }
    val uvExtremes = remember(series, days) { chartDayExtremes(series.uv, days) }
    // The highest bar of each day, with any number of hours
    val rainPeaks = remember(series, days) { chartDayExtremes(series.precipitation, days, minHours = 1) }
    // The moon of each day: the phase to draw, and on the day of a new or full moon the index of its name in
    // moon_phases (0 or 4). That day shows the moon new or full; the other days show it at the place's noon
    val moons = remember(days) {
        days.map { span ->
            val end = span.date + 24 * HOUR
            when {
                Moon.nextPhase(span.date, 0.5) < end -> 0.5 to 4
                Moon.nextPhase(span.date, 0.0) < end -> 0.0 to 0
                else -> Moon.illumination(span.date + 12 * HOUR).phase to null
            }
        }
    }
    val phaseNames = stringArrayResource(R.array.moon_phases)
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
        heatIndex = stringResource(R.string.detail_heat_index),
        windChill = stringResource(R.string.detail_wind_chill),
        precipitation = stringResource(R.string.detail_precipitation) + "  " + units(metricPrimary, "mm", "in"),
        chance = stringResource(R.string.precip_chance, "%"),
        wind = stringResource(R.string.detail_wind) + "  " + units(metricPrimary, "m/s", "mph"),
        gust = stringResource(R.string.detail_wind_gust),
        pressure = stringResource(R.string.detail_pressure) + "  " + units(metricPrimary, "hPa", "inHg"),
        humidity = stringResource(R.string.detail_humidity) + "  %",
        uvIndex = stringResource(R.string.detail_uv_index),
        airQuality = stringResource(R.string.detail_air_quality) + "  " +
            stringResource(if (aqiStandard == AqiStandard.EUROPEAN) R.string.air_quality_eu_aqi else R.string.air_quality_us_aqi),
    )
    val noData = stringResource(R.string.chart_view_no_data)
    val summary = stringResource(R.string.chart_view_summary, localizeDigits(count.toString()))
    val totalHeight = panels.sumOf { it.heightDp } + PANEL_GAP.value * (panels.size - 1)
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
        val chartsTop = panelTop(panels, ChartPanel.ICONS)
        // The bottom of the last value panel: the UV index when it shows, else the humidity
        val lastPanel = if (showUv) ChartPanel.UV_INDEX else ChartPanel.HUMIDITY
        val chartsBottom = panelTop(panels, lastPanel) + lastPanel.heightDp.dp.toPx()

        val tArea = plotArea(panels, ChartPanel.TEMPERATURE, extraTop = EXTREME_ROW.toPx(), extraBottom = EXTREME_ROW.toPx())
        val pArea = plotArea(panels, ChartPanel.PRECIPITATION, extraTop = EXTREME_ROW.toPx())
        val wArea = plotArea(panels, ChartPanel.WIND, extraTop = (ARROW_ROW + EXTREME_ROW).toPx(), extraBottom = EXTREME_ROW.toPx())
        val prArea = plotArea(panels, ChartPanel.PRESSURE, extraTop = EXTREME_ROW.toPx(), extraBottom = EXTREME_ROW.toPx())
        val hArea = plotArea(panels, ChartPanel.HUMIDITY, extraTop = EXTREME_ROW.toPx(), extraBottom = EXTREME_ROW.toPx())
        val uvArea = plotArea(panels, ChartPanel.UV_INDEX, extraTop = EXTREME_ROW.toPx(), extraBottom = EXTREME_ROW.toPx())

        // The high of each day above its point and the low under it (not with [lows] false), on a primary line ([y]
        // gives the point of a value); a label stays inside the plot, and a low that would cover the high of its day is
        // left out
        val extremeStyle = TextStyle(fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = displayFont, textDirection = TextDirection.Ltr)
        fun drawDayExtremes(
            values: List<Double?>, extremes: List<ChartDayExtremes>, y: (Double) -> Float, color: (Int, Boolean) -> Color,
            lows: Boolean = true, label: (Int) -> String?,
        ) {
            fun measure(i: Int, high: Boolean): androidx.compose.ui.text.TextLayoutResult? {
                if (i !in drawn || values[i] == null) return null
                val text = label(i) ?: return null
                val c = color(i, high)
                return textMeasurer.measure(dualLabel(localizeDigits(text), c), extremeStyle.copy(color = c))
            }
            val edge = 2.dp.toPx()
            fun left(i: Int, width: Int) = (cx(i) - width / 2f).coerceIn(axisPx + edge, maxOf(axisPx + edge, size.width - width - edge))
            for (e in extremes) {
                var highBox: Rect? = null
                measure(e.highIndex, true)?.let { layout ->
                    val i = e.highIndex
                    val topLeft = Offset(left(i, layout.size.width), y(values[i]!!) - 2.dp.toPx() - layout.size.height)
                    highBox = Rect(topLeft, Size(layout.size.width.toFloat(), layout.size.height.toFloat()))
                    drawText(layout, topLeft = topLeft)
                }
                if (!lows) continue
                measure(e.lowIndex, false)?.let { layout ->
                    val i = e.lowIndex
                    val topLeft = Offset(left(i, layout.size.width), y(values[i]!!) + 2.dp.toPx())
                    val box = Rect(topLeft, Size(layout.size.width.toFloat(), layout.size.height.toFloat()))
                    if (highBox?.overlaps(box) != true) drawText(layout, topLeft = topLeft)
                }
            }
        }

        // The band of each titled panel first, under all the rest; it covers the axis column too
        val bandInset = BAND_INSET.toPx()
        panels.filter { it.titled }.forEach { panel ->
            drawRoundRect(
                ChartStyle.panelBand, Offset(bandInset, panelTop(panels, panel)), Size(size.width - 2 * bandInset, panel.heightDp.dp.toPx()),
                CornerRadius(ChartStyle.panelBandRadius.toPx()),
            )
        }

        clipRect(left = axisPx) {
            // Days: alternate backgrounds, and a stronger line at each midnight in every panel
            days.forEachIndexed { k, span ->
                val left = if (k == 0) cx(span.startIndex) - slot / 2 else cx(span.startIndex)
                val right = days.getOrNull(k + 1)?.let { cx(it.startIndex) } ?: (cx(span.endIndex - 1) + slot / 2)
                if (right < axisPx || left > size.width) return@forEachIndexed
                if (k % 2 == 1) drawRect(Color.White.copy(alpha = 0.03f), Offset(left, 0f), Size(right - left, size.height))
                if (k > 0) drawVerticalLine(panels, left, ChartStyle.midnight, ChartStyle.midnightWidth.toPx())
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

            // The grids first, so the selection column and then the lines are on top of them
            tDomain?.let { drawGrid(it, tArea) }
            pDomain?.let { drawGrid(it, pArea) }
            wDomain?.let { drawGrid(it, wArea) }
            prDomain?.let { drawGrid(it, prArea) }
            drawGrid(PERCENT_DOMAIN, hArea)
            // The grid of the UV index; with no UV index, the grid of the air quality
            (uvDomain ?: aqDomain)?.let { drawGrid(it, uvArea) }

            // The selection column: the selected hour is under it, in every panel; the lines are on top of it
            drawRoundRect(
                ChartStyle.selectionColumn, Offset(lineX - slot / 2, chartsTop), Size(slot, chartsBottom - chartsTop),
                CornerRadius(ChartStyle.selectionColumnRadius.toPx()),
            )

            drawIcons(hours, drawn, localHours, ::cx, panelTop(panels, ChartPanel.ICONS), painters, textMeasurer, displayFont, localizeDigits)

            // Temperature with the dew point (dashed), the heat index and the wind chill on the same scale, and a
            // dashed line at freezing
            tDomain?.let { d ->
                val freezing = if (metricPrimary) 0.0 else 32.0
                if (freezing > d.min && freezing < d.max) {
                    val fy = tArea.y(freezing, d)
                    drawLine(ChartStyle.freezing, Offset(axisPx, fy), Offset(size.width, fy), ChartStyle.freezingWidth.toPx(), pathEffect = chartDash(ChartStyle.freezingDash))
                }
                drawSeries(series.dewPoint, drawn, ::cx, { tArea.y(it, d) }, ChartStyle.dewPoint, ChartStyle.thinLineWidth.toPx(), ChartStyle.dewPointDash)
                drawSeries(series.heatIndex, drawn, ::cx, { tArea.y(it, d) }, ChartStyle.heatIndex, ChartStyle.thinLineWidth.toPx())
                drawSeries(series.windChill, drawn, ::cx, { tArea.y(it, d) }, ChartStyle.windChill, ChartStyle.thinLineWidth.toPx())
                drawSeries(series.temperature, drawn, ::cx, { tArea.y(it, d) }, ChartStyle.temperature, ChartStyle.lineWidth.toPx())
                drawSelectedDot(series.dewPoint, linePos, lineX) { tArea.y(it, d) }
                drawSelectedDot(series.heatIndex, linePos, lineX) { tArea.y(it, d) }
                drawSelectedDot(series.windChill, linePos, lineX) { tArea.y(it, d) }
                drawSelectedDot(series.temperature, linePos, lineX) { tArea.y(it, d) }

                // The high and low of each day
                drawDayExtremes(series.temperature, tempExtremes, { tArea.y(it, d) }, { _, high -> if (high) ChartStyle.high else ChartStyle.low }) { i ->
                    hours[i].temperature?.displayDual(metricPrimary)?.let { (p, s) -> "$p · $s" }
                }
            }

            // Precipitation as bars, the chance of rain as a thin line on 0-100 %
            pDomain?.let { d ->
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
            // The value of the highest bar of each day, above the bar, as the day high of the temperature
            pDomain?.let { d ->
                val style = TextStyle(color = ChartStyle.precipitation, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = displayFont, textDirection = TextDirection.Ltr)
                for (e in rainPeaks) {
                    val i = e.highIndex.takeIf { it in drawn } ?: continue
                    val v = series.precipitation[i]?.takeIf { it > 0 } ?: continue
                    val (p, s) = hours[i].precipitation?.displayDual(metricPrimary) ?: continue
                    val layout = textMeasurer.measure(dualLabel(localizeDigits("$p · $s"), style.color), style)
                    drawText(layout, topLeft = Offset(cx(i) - layout.size.width / 2f, pArea.y(v, d) - 2.dp.toPx() - layout.size.height))
                }
            }

            // Wind and gusts on one scale, with the direction every 4 hours
            wDomain?.let { d ->
                drawSeries(series.gust, drawn, ::cx, { wArea.y(it, d) }, ChartStyle.gust, ChartStyle.thinLineWidth.toPx())
                drawSeries(series.wind, drawn, ::cx, { wArea.y(it, d) }, ChartStyle.wind, ChartStyle.lineWidth.toPx())
                drawSelectedDot(series.gust, linePos, lineX) { wArea.y(it, d) }
                drawSelectedDot(series.wind, linePos, lineX) { wArea.y(it, d) }
                drawDayExtremes(series.wind, windExtremes, { wArea.y(it, d) }, { _, _ -> ChartStyle.wind }) { i ->
                    hours[i].windSpeed?.displayDual(metricPrimary)?.let { (p, s) -> "$p · $s" }
                }
            }
            val arrowY = wArea.top - ARROW_ROW.toPx() / 2
            for (i in drawn) {
                if (localHours[i] % LABEL_HOURS != 0) continue
                val speed = hours[i].windSpeed ?: continue
                val direction = hours[i].windDirection ?: continue
                if (showWindArrow(speed)) drawWindArrow(Offset(cx(i), arrowY), direction)
            }

            prDomain?.let { d ->
                drawSeries(series.pressure, drawn, ::cx, { prArea.y(it, d) }, ChartStyle.pressure, ChartStyle.lineWidth.toPx())
                drawSelectedDot(series.pressure, linePos, lineX) { prArea.y(it, d) }
                drawDayExtremes(series.pressure, pressureExtremes, { prArea.y(it, d) }, { _, _ -> ChartStyle.pressure }) { i ->
                    hours[i].pressure?.displayDual(metricPrimary)?.let { (p, s) -> "$p · $s" }
                }
            }

            drawSeries(series.humidity, drawn, ::cx, { hArea.y(it, PERCENT_DOMAIN) }, ChartStyle.humidity, ChartStyle.lineWidth.toPx())
            drawSelectedDot(series.humidity, linePos, lineX) { hArea.y(it, PERCENT_DOMAIN) }
            drawDayExtremes(series.humidity, humidityExtremes, { hArea.y(it, PERCENT_DOMAIN) }, { _, _ -> ChartStyle.humidity }) { i ->
                hours[i].humidity?.let { "$it%" }
            }

            // The air quality, dashed on its own scale: each part of the line in the tier colour of the hour at its
            // start, and a "no data" label after the last hour with air quality
            aqDomain?.let { d ->
                drawTierSeries(
                    aqValues, drawn, ::cx, { uvArea.y(it, d) }, { i -> colorsFor(airQuality[i]!!.tier(aqiStandard)).background },
                    ChartStyle.lineWidth.toPx(), ChartStyle.airQualityDash,
                )
                drawSelectedDot(aqValues, linePos, lineX) { uvArea.y(it, d) }
                aqEnd?.let { i ->
                    val layout = textMeasurer.measure(noData, TextStyle(color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp, fontFamily = bodyFont))
                    // It stays at the start of the plot when the last hour with air quality is out of view
                    val left = maxOf(cx(i), axisPx) + 8.dp.toPx()
                    if (left < size.width) {
                        drawText(layout, topLeft = Offset(left, (uvArea.top + uvArea.bottom - layout.size.height) / 2))
                    }
                }
            }
            // The UV index on top: each part of the line in the tier colour of the hour at its start, and the high of
            // each day (the low is 0 at night)
            uvDomain?.let { d ->
                drawTierSeries(series.uv, drawn, ::cx, { uvArea.y(it, d) }, { i -> colorsFor(series.uv[i]!!).background }, ChartStyle.lineWidth.toPx())
                drawSelectedDot(series.uv, linePos, lineX) { uvArea.y(it, d) }
                drawDayExtremes(series.uv, uvExtremes, { uvArea.y(it, d) }, { i, _ -> colorsFor(series.uv[i]!!).background }, lows = false) { i ->
                    series.uv[i]?.roundToInt()?.takeIf { it > 0 }?.toString()
                }
            }

            // Time axes at the top and the bottom: an hour label every 4 hours, and the day at each midnight
            // (the day at the start stays in view), with the moon of the day and, on the day of a new or full moon, its name,
            // when they fit. The top axis is the bottom one mirrored: the labels are above their ticks.
            val hourStyle = TextStyle(color = onSurface.copy(alpha = 0.6f), fontSize = 10.sp, fontFamily = displayFont)
            val dayStyle = TextStyle(color = onSurface, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = displayFont)
            val phaseStyle = TextStyle(color = onSurface.copy(alpha = 0.7f), fontSize = 10.sp, fontFamily = bodyFont)
            val moonR = MOON_R.toPx()
            val moonGap = MOON_GAP.toPx()
            val tick = 3.dp.toPx()
            val dayOffset = 18.dp.toPx()
            fun drawTimeAxis(panel: ChartPanel, mirrored: Boolean) {
                val top = panelTop(panels, panel)
                val bottom = top + panel.heightDp.dp.toPx()
                for (i in drawn) {
                    if (localHours[i] % LABEL_HOURS != 0) continue
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
                    val room = end - pad - left
                    if (layout.size.width > room) return@forEachIndexed
                    val y = if (mirrored) bottom - dayOffset - layout.size.height else top + dayOffset
                    drawText(layout, topLeft = Offset(left, y))
                    val moonX = left + layout.size.width + moonGap + moonR
                    if (moonX + moonR - left > room) return@forEachIndexed
                    val cy = y + layout.size.height / 2f
                    val (phase, event) = moons[k]
                    drawMoon(phase, southern, Offset(moonX, cy), moonR)
                    if (event == null) return@forEachIndexed
                    val name = textMeasurer.measure(phaseNames[event], phaseStyle)
                    val nameX = moonX + moonR + moonGap
                    if (nameX + name.size.width - left <= room) drawText(name, topLeft = Offset(nameX, cy - name.size.height / 2f))
                }
            }
            drawTimeAxis(ChartPanel.TOP_TIME, mirrored = true)
            drawTimeAxis(ChartPanel.TIME, mirrored = false)

            // The chance of rain scale at the end of the panel
            val chanceStyle = TextStyle(color = ChartStyle.chance, fontSize = 8.sp, fontFamily = displayFont)
            listOf(50.0, 100.0).forEach { v ->
                val layout = textMeasurer.measure(localizeDigits("${v.toInt()}%"), chanceStyle)
                drawText(layout, topLeft = Offset(size.width - layout.size.width - 2.dp.toPx(), pArea.y(v, PERCENT_DOMAIN) - layout.size.height / 2f))
            }
            // The air quality scale at the end of the UV index panel, in the same way, with no label at 0
            aqDomain?.let { d ->
                val aqStyle = chanceStyle.copy(color = Color.White.copy(alpha = 0.6f))
                d.ticks.filter { it > d.min }.forEach { v ->
                    val layout = textMeasurer.measure(localizeDigits(v.roundToInt().toString()), aqStyle)
                    drawText(layout, topLeft = Offset(size.width - layout.size.width - 2.dp.toPx(), uvArea.y(v, d) - layout.size.height / 2f))
                }
            }
        }

        // The axis column: the values of each scale in the primary unit, with the other unit small under them
        val axis = AxisLabels(textMeasurer, displayFont, onSurface, axisPx, localizeDigits)
        tDomain?.let { axis.draw(this, it, tArea, if (metricPrimary) ::celsiusToFahrenheit else ::fahrenheitToCelsius, 0) }
        pDomain?.let { axis.draw(this, it, pArea, if (metricPrimary) { v -> v * 0.03937 } else { v -> v / 0.03937 }, if (metricPrimary) 2 else 1) }
        wDomain?.let { axis.draw(this, it, wArea, if (metricPrimary) { v -> v * 2.23694 } else { v -> v / 2.23694 }, if (metricPrimary) 0 else 1) }
        prDomain?.let { axis.draw(this, it, prArea, if (metricPrimary) { v -> v * 0.02953 } else { v -> v / 0.02953 }, if (metricPrimary) 2 else 0) }
        axis.draw(this, PERCENT_DOMAIN, hArea, null, 0, suffix = "%")
        uvDomain?.let { axis.draw(this, it, uvArea, null, 0) }

        // Titles on top of everything, at the start of each band; they do not scroll. A second line is named only when
        // it has a value
        val temperatureKeys = listOfNotNull(
            titles.dewPoint to ChartStyle.dewPoint,
            (titles.heatIndex to ChartStyle.heatIndex).takeIf { series.heatIndex.any { it != null } },
            (titles.windChill to ChartStyle.windChill).takeIf { series.windChill.any { it != null } },
        )
        drawTitle(panels, ChartPanel.TEMPERATURE, titles.temperature, temperatureKeys, textMeasurer, bodyFont)
        drawTitle(panels, ChartPanel.PRECIPITATION, titles.precipitation, listOf(titles.chance to ChartStyle.chance), textMeasurer, bodyFont)
        drawTitle(panels, ChartPanel.WIND, titles.wind, listOf(titles.gust to ChartStyle.gust), textMeasurer, bodyFont)
        drawTitle(panels, ChartPanel.PRESSURE, titles.pressure, emptyList(), textMeasurer, bodyFont)
        drawTitle(panels, ChartPanel.HUMIDITY, titles.humidity, emptyList(), textMeasurer, bodyFont)
        // The air quality key is neutral, because its line changes colour with the tier
        if (showUv) {
            val aqKey = if (aqDomain != null) listOf(titles.airQuality to Color.White.copy(alpha = 0.6f)) else emptyList()
            drawTitle(panels, ChartPanel.UV_INDEX, titles.uvIndex, aqKey, textMeasurer, bodyFont)
        }
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
    val heatIndex: String, val windChill: String,
    val wind: String, val gust: String,
    val pressure: String, val humidity: String,
    val uvIndex: String, val airQuality: String,
)

/** The part of a panel where the values go, from [top] to [bottom] px. */
private class PlotArea(val top: Float, val bottom: Float) {
    fun y(v: Double, d: ChartDomain): Float =
        if (d.max > d.min) (bottom - (v - d.min) / (d.max - d.min) * (bottom - top)).toFloat() else bottom
}

/** The top of [panel] in px, with [panels] the panels that show. */
private fun DrawScope.panelTop(panels: List<ChartPanel>, panel: ChartPanel): Float =
    panels.takeWhile { it != panel }.sumOf { (it.heightDp.dp + PANEL_GAP).toPx().toDouble() }.toFloat()

private fun DrawScope.plotArea(panels: List<ChartPanel>, panel: ChartPanel, extraTop: Float = 0f, extraBottom: Float = 0f): PlotArea {
    val top = panelTop(panels, panel)
    val title = if (panel.titled) TITLE_HEIGHT.toPx() else 0f
    return PlotArea(top + title + extraTop + 4.dp.toPx(), top + panel.heightDp.dp.toPx() - extraBottom - 4.dp.toPx())
}

/**
 * A vertical line at [x] in the plot area of each panel, with a break in the gaps and title lines between
 * the panels, so that each chart shows where it ends. The time axes have their own short ticks.
 */
private fun DrawScope.drawVerticalLine(panels: List<ChartPanel>, x: Float, color: Color, width: Float) {
    panels.filter { it != ChartPanel.TIME && it != ChartPanel.TOP_TIME }.forEach { panel ->
        val area = plotArea(panels, panel)
        drawLine(color, Offset(x, area.top), Offset(x, area.bottom), width)
    }
}

private fun DrawScope.drawGrid(d: ChartDomain, area: PlotArea) {
    d.ticks.forEach { v ->
        val y = area.y(v, d)
        drawLine(Hairline, Offset(0f, y), Offset(size.width, y), 1f)
    }
}

/** A line through the hours of [range], dashed with [dash]; a missing value leaves a gap, and each run is a monotone curve. */
private fun DrawScope.drawSeries(
    values: List<Double?>, range: IntRange, x: (Int) -> Float, y: (Double) -> Float,
    color: Color, width: Float, dash: List<Dp>? = null,
) {
    val effect = dash?.let { chartDash(it) }
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
 * A line through the hours of [range] as [drawSeries] draws it, but each part between two hours is in the colour of the
 * hour at its start ([color]). The parts of one colour in a row are one path, so a [dash] pattern runs on along them.
 */
private fun DrawScope.drawTierSeries(
    values: List<Double?>, range: IntRange, x: (Int) -> Float, y: (Double) -> Float, color: (Int) -> Color, width: Float,
    dash: List<Dp>? = null,
) {
    val effect = dash?.let { chartDash(it) }
    var run = mutableListOf<Int>()
    fun flush() {
        val points = run.map { Offset(x(it), y(values[it]!!)) }
        if (points.size == 1) drawCircle(color(run[0]), width, points[0])
        if (points.size >= 2) {
            var path: Path? = null
            var pathColor = Color.Unspecified
            // Round ends, so the parts join with no gap
            fun drawPart() = path?.let { drawPath(it, pathColor, style = Stroke(width, cap = StrokeCap.Round, pathEffect = effect)) }
            computeMonotoneCubicControlPoints(points).forEachIndexed { k, cp ->
                val c = color(run[k])
                if (path == null || c != pathColor) {
                    drawPart()
                    path = Path().apply { moveTo(points[k].x, points[k].y) }
                    pathColor = c
                }
                path!!.cubicTo(cp.cp1.x, cp.cp1.y, cp.cp2.x, cp.cp2.y, points[k + 1].x, points[k + 1].y)
            }
            drawPart()
        }
        run = mutableListOf()
    }
    for (i in range) {
        val v = values[i]
        if (v == null || !v.isFinite()) flush() else run += i
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

/** The weather icon every 4 hours (night icons at night), with the UV index under it from 1, in its tier colour. */
private fun DrawScope.drawIcons(
    hours: List<ChartHour>, range: IntRange, localHours: List<Int>, x: (Int) -> Float, top: Float,
    painters: Map<Int, Painter>, textMeasurer: TextMeasurer, font: FontFamily, localizeDigits: (String) -> String,
) {
    val iconSize = 26.dp.toPx()
    for (i in range) {
        if (localHours[i] % LABEL_HOURS != 0) continue
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

/** The panel name and units, and the name of each other line ([keys]: name and colour), in the title row of [panel]. */
private fun DrawScope.drawTitle(
    panels: List<ChartPanel>, panel: ChartPanel, title: String, keys: List<Pair<String, Color>>, textMeasurer: TextMeasurer, font: FontFamily,
) {
    val top = panelTop(panels, panel)
    val text: AnnotatedString = buildAnnotatedString {
        withStyle(SpanStyle(color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp, fontWeight = FontWeight.Bold)) { append(title) }
        keys.forEach { (name, color) -> withStyle(SpanStyle(color = color, fontSize = 11.sp)) { append("   ● $name") } }
    }
    val layout = textMeasurer.measure(text, TextStyle(fontFamily = font, textDirection = TextDirection.Content))
    drawText(layout, topLeft = Offset((BAND_INSET + TITLE_INSET).toPx(), top + (TITLE_HEIGHT.toPx() - layout.size.height) / 2))
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
