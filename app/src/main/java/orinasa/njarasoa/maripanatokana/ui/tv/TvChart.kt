// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

package orinasa.njarasoa.maripanatokana.ui.tv

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import orinasa.njarasoa.maripanatokana.R
import orinasa.njarasoa.maripanatokana.domain.model.AirQualityIndex
import orinasa.njarasoa.maripanatokana.domain.model.AqiStandard
import orinasa.njarasoa.maripanatokana.domain.model.ChartHour
import orinasa.njarasoa.maripanatokana.domain.model.DailyForecast
import orinasa.njarasoa.maripanatokana.domain.model.HourlyAirQuality
import orinasa.njarasoa.maripanatokana.domain.model.Precipitation
import orinasa.njarasoa.maripanatokana.domain.model.Pressure
import orinasa.njarasoa.maripanatokana.domain.model.Temperature
import orinasa.njarasoa.maripanatokana.domain.model.ThermalIndices
import orinasa.njarasoa.maripanatokana.domain.model.WeatherData
import orinasa.njarasoa.maripanatokana.domain.model.WindSpeed
import orinasa.njarasoa.maripanatokana.ui.theme.temperatureColor
import orinasa.njarasoa.maripanatokana.ui.chart.ChartDomain
import orinasa.njarasoa.maripanatokana.ui.chart.ChartStyle
import orinasa.njarasoa.maripanatokana.ui.chart.ChartTile
import orinasa.njarasoa.maripanatokana.ui.chart.chartDash
import orinasa.njarasoa.maripanatokana.ui.chart.chartAirQuality
import orinasa.njarasoa.maripanatokana.ui.chart.chartDomain
import orinasa.njarasoa.maripanatokana.ui.chart.chartTiles
import orinasa.njarasoa.maripanatokana.ui.common.components.WeatherIcon
import orinasa.njarasoa.maripanatokana.ui.common.components.colorsFor
import java.util.Locale
import kotlin.math.roundToInt

private const val CHART_HOURS = 72

// The colours of the lines are those of the phone chart view (ChartStyle), so a value has the same colour everywhere

/** A thin line on the plot of another series, on the same scale: the dew point, heat index, wind chill or gust. */
private class ExtraLine(val values: List<Double?>, val color: Color, val dashed: Boolean = false)

/**
 * A line on the plot of another series with its own scale on the right: the chance of rain (0 to 100 %) on the
 * precipitation, the air quality on the UV index. The air quality is dashed, so that it stands apart from the UV line;
 * both change colour with their tiers. [dots] puts a dot on each value (days only: hours are too close together).
 */
private class RightLine(
    val values: List<Double?>,
    val domain: ChartDomain,
    val color: (Double) -> Color,
    val text: (Double) -> String,
    val dashed: Boolean = false,
    val dots: Boolean = false,
)
private const val CHART_LEAD_HOURS = 24

/**
 * One value of the chart over the hours or the days. [values] has one entry for each hour or day, in the base unit of
 * the value. [low], when there is one, is a second line under [values]: the daily lows under the highs. [extras] are
 * thin lines on the same scale. [right], when there is one, is a line with its own scale. A series [withPrevious]
 * has its card next to the one before it and no plot of its own: the plot of the one before shows it. [text] gives a
 * value in both units (the second is null when the value has one unit).
 */
private class TvSeries(
    val label: Int,
    val values: List<Double?>,
    val fromZero: Boolean,
    val minSpan: Double,
    val color: (Double) -> Color,
    val text: (Double) -> Pair<String, String?>,
    val bars: Boolean = false,
    val low: List<Double?>? = null,
    val lowColor: Color? = null,
    val extras: List<ExtraLine> = emptyList(),
    val right: RightLine? = null,
    val withPrevious: Boolean = false,
)

/**
 * The series that share the temperature plot, after [temperature]: the dew point, and the heat index and wind chill
 * when the data has any. Each has its card, and a thin line on the temperature plot.
 */
private fun withTemperatureExtras(
    temperature: TvSeries, dew: List<Double?>?, heat: List<Double?>, chill: List<Double?>, metric: Boolean,
): List<TvSeries> {
    val parts = listOfNotNull(
        dew?.let { Triple(R.string.detail_dewpoint, it, ExtraLine(it, ChartStyle.dewPoint, dashed = true)) },
        heat.takeIf { v -> v.any { it != null } }?.let { Triple(R.string.detail_heat_index, it, ExtraLine(it, ChartStyle.heatIndex)) },
        chill.takeIf { v -> v.any { it != null } }?.let { Triple(R.string.detail_wind_chill, it, ExtraLine(it, ChartStyle.windChill)) },
    )
    val main = TvSeries(
        temperature.label, temperature.values, temperature.fromZero, temperature.minSpan, temperature.color, temperature.text,
        low = temperature.low, lowColor = temperature.lowColor, extras = parts.map { it.third },
    )
    return listOf(main) + parts.map { (label, values, line) ->
        TvSeries(label, values, false, 5.0, { line.color }, { temperatureText(it, metric) }, withPrevious = true)
    }
}

private fun temperatureText(c: Double, metric: Boolean): Pair<String, String?> = Temperature.fromCelsius(c).displayDual(metric)

private fun windText(ms: Double, metric: Boolean): Pair<String, String?> = WindSpeed.fromMetersPerSecond(ms).displayDual(metric)

private fun airQualitySeries(hours: List<ChartHour>, entries: List<HourlyAirQuality>, standard: AqiStandard, usLabel: String, euLabel: String): TvSeries {
    val european = standard == AqiStandard.EUROPEAN
    val matches = chartAirQuality(hours.map { it.time }, entries.map { it.time })
    val values = matches.map { i -> i?.let { (if (european) entries[it].europeanValue else entries[it].usValue).toDouble() } }
    return TvSeries(
        tileLabel(ChartTile.AIR_QUALITY), values, fromZero = true, minSpan = 50.0,
        color = { colorsFor(AirQualityIndex.tierFor(it.roundToInt(), standard)).background },
        text = { "${it.roundToInt()} ${if (european) euLabel else usLabel}" to null },
    )
}

private fun seriesFor(tile: ChartTile, data: WeatherData, metric: Boolean, usLabel: String, euLabel: String): TvSeries {
    val hours = data.chartHourly
    val label = tileLabel(tile)
    return when (tile) {
        ChartTile.TEMPERATURE -> TvSeries(label, hours.map { it.temperature?.celsius }, false, 5.0, { temperatureColor(it) }, { temperatureText(it, metric) })
        ChartTile.DEW_POINT -> TvSeries(label, hours.map { it.dewPoint?.celsius }, false, 5.0, { temperatureColor(it) }, { temperatureText(it, metric) })
        ChartTile.PRECIPITATION -> TvSeries(
            label, hours.map { it.precipitation?.mm }, true, 1.0, { ChartStyle.precipitation },
            { Precipitation.fromMm(it).displayDual(metric) }, bars = true,
        )
        ChartTile.WIND -> TvSeries(label, hours.map { it.windSpeed?.metersPerSecond }, true, 5.0, { ChartStyle.wind }, { windText(it, metric) })
        ChartTile.WIND_GUST -> TvSeries(label, hours.map { it.windGust?.metersPerSecond }, true, 5.0, { ChartStyle.wind }, { windText(it, metric) })
        ChartTile.PRESSURE -> TvSeries(
            label, hours.map { it.pressure?.hPa }, false, 4.0, { ChartStyle.pressure },
            { Pressure.fromHPa(it).displayDual(metric) },
        )
        ChartTile.HUMIDITY -> TvSeries(label, hours.map { it.humidity?.toDouble() }, true, 100.0, { ChartStyle.humidity }, { "${it.roundToInt()}%" to null })
        ChartTile.UV_INDEX -> TvSeries(
            label, hours.map { h -> h.uvIndex?.takeIf { it.isFinite() } }, true, 5.0, { colorsFor(it).background },
            { "%.1f".format(Locale.US, it) to null },
        )
        ChartTile.AIR_QUALITY -> airQualitySeries(hours, data.hourlyAirQuality, data.airQuality?.primaryStandard ?: AqiStandard.US, usLabel, euLabel)
    }
}

/**
 * The values of the daily chart: one entry for each day of [days]. The heat index is the one of the high, the wind
 * chill the one of the low. The gust and the air quality are the highest hourly values of the day at the place, where
 * the hours reach.
 */
private fun dailySeries(data: WeatherData, metric: Boolean, usLabel: String, euLabel: String): List<TvSeries> {
    val days = data.dailyForecast
    val hours = data.chartHourly
    fun day(ms: Long) = Math.floorDiv(ms + data.utcOffsetSeconds * 1000L, 86_400_000L)
    val standard = data.airQuality?.primaryStandard ?: AqiStandard.US
    val european = standard == AqiStandard.EUROPEAN
    val airByDay = data.hourlyAirQuality.groupBy { day(it.time) }
        .mapValues { (_, h) -> h.maxOf { if (european) it.europeanValue else it.usValue }.toDouble() }
    val air = days.map { airByDay[day(it.date)] }.takeIf { v -> v.any { it != null } }
    val gustByDay = hours.filter { it.windGust != null }.groupBy { day(it.time) }.mapValues { (_, h) -> h.maxOf { it.windGust!!.metersPerSecond } }
    val gust = days.map { gustByDay[day(it.date)] }.takeIf { v -> v.any { it != null } }
    val chance = days.map { it.precipProbability.toDouble() }
    val temperature = TvSeries(
        R.string.detail_temperature, days.map { it.tempMax.celsius }, false, 5.0, { ChartStyle.high }, { temperatureText(it, metric) },
        low = days.map { it.tempMin.celsius }, lowColor = ChartStyle.low,
    )
    val wind = days.map { it.windSpeed.metersPerSecond }
    // The air quality on the UV plot
    val airParts = air?.let { airQualityParts(it, standard, usLabel, euLabel, days = true) }
    val uvWithAirQuality = listOfNotNull(
        TvSeries(
            R.string.detail_uv_index, days.map { d -> d.uvIndexMax.takeIf { it.isFinite() } }, true, 5.0, { colorsFor(it).background },
            { "%.1f".format(Locale.US, it) to null }, right = airParts?.second,
        ),
        airParts?.first,
    )
    return withTemperatureExtras(
        temperature, days.map { it.dewPoint?.celsius }.takeIf { v -> v.any { it != null } },
        days.map { ThermalIndices.heatIndex(it)?.celsius }, days.map { ThermalIndices.windChill(it)?.celsius }, metric,
    ) + listOf(
        // One plot for the amount and the chance of rain
        TvSeries(
            R.string.detail_precipitation, days.map { it.precipitation.mm }, true, 1.0, { ChartStyle.precipitation },
            { Precipitation.fromMm(it).displayDual(metric) }, bars = true, right = chanceLine(chance, days = true),
        ),
        chanceSeries(chance),
        TvSeries(
            R.string.detail_wind, wind, true, 5.0, { ChartStyle.wind }, { windText(it, metric) },
            extras = listOfNotNull(gust?.let { ExtraLine(it, ChartStyle.gust) }),
        ),
    ) + listOfNotNull(
        gust?.let { TvSeries(R.string.detail_wind_gust, it, true, 5.0, { ChartStyle.gust }, { v -> windText(v, metric) }, withPrevious = true) },
    ) + uvWithAirQuality + listOf(
        TvSeries(R.string.detail_humidity, days.map { it.humidity?.toDouble() }, true, 100.0, { ChartStyle.humidity }, { "${it.roundToInt()}%" to null }),
    )
}

/**
 * The series of the hourly chart. The temperature plot also has the dew point, heat index and wind chill, the
 * precipitation plot the chance of rain, the wind plot the gust, and the UV plot the air quality.
 */
private fun hourlySeries(data: WeatherData, metric: Boolean, usLabel: String, euLabel: String): List<TvSeries> {
    val hours = data.chartHourly
    val tiles = chartTiles(hours, data.hourlyAirQuality.isNotEmpty())
    val heat = hours.map { h -> h.temperature?.let { ThermalIndices.heatIndex(it, h.humidity)?.celsius } }
    val chill = hours.map { h -> h.temperature?.let { t -> h.windSpeed?.let { ThermalIndices.windChill(t, it)?.celsius } } }
    val chance = hours.map { it.precipProbability?.toDouble() }.takeIf { v -> v.any { it != null } }
    val gust = if (ChartTile.WIND_GUST in tiles) hours.map { it.windGust?.metersPerSecond } else null
    return tiles.flatMap { tile ->
        val one = seriesFor(tile, data, metric, usLabel, euLabel)
        when (tile) {
            ChartTile.TEMPERATURE -> withTemperatureExtras(
                one, if (ChartTile.DEW_POINT in tiles) hours.map { it.dewPoint?.celsius } else null, heat, chill, metric,
            )
            ChartTile.DEW_POINT -> emptyList()
            ChartTile.PRECIPITATION -> if (chance == null) listOf(one) else listOf(
                TvSeries(one.label, one.values, one.fromZero, one.minSpan, one.color, one.text, bars = true, right = chanceLine(chance, days = false)),
                chanceSeries(chance),
            )
            ChartTile.WIND -> if (gust == null) listOf(one) else listOf(
                TvSeries(one.label, one.values, one.fromZero, one.minSpan, one.color, one.text, extras = listOf(ExtraLine(gust, ChartStyle.gust))),
                TvSeries(R.string.detail_wind_gust, gust, true, 5.0, { ChartStyle.gust }, { windText(it, metric) }, withPrevious = true),
            )
            ChartTile.WIND_GUST -> emptyList()
            // The air quality on the UV plot, when there is one
            ChartTile.UV_INDEX -> if (ChartTile.AIR_QUALITY !in tiles) listOf(one) else {
                val (card, line) = airQualityParts(
                    seriesFor(ChartTile.AIR_QUALITY, data, metric, usLabel, euLabel).values,
                    data.airQuality?.primaryStandard ?: AqiStandard.US, usLabel, euLabel, days = false,
                )
                listOf(TvSeries(one.label, one.values, one.fromZero, one.minSpan, one.color, one.text, right = line), card)
            }
            ChartTile.AIR_QUALITY -> if (ChartTile.UV_INDEX in tiles) emptyList() else listOf(one)
            else -> listOf(one)
        }
    }
}

/** The chance of rain on the precipitation plot. */
private fun chanceLine(chance: List<Double?>, days: Boolean) = RightLine(
    chance, ChartDomain(0.0, 100.0, listOf(0.0, 50.0, 100.0)), { ChartStyle.chance }, { "${it.roundToInt()}%" }, dots = days,
)

/** The air quality card, next to the UV index, and its dashed line on the UV plot. */
private fun airQualityParts(values: List<Double?>, standard: AqiStandard, usLabel: String, euLabel: String, days: Boolean): Pair<TvSeries, RightLine?> {
    val european = standard == AqiStandard.EUROPEAN
    val color = { v: Double -> colorsFor(AirQualityIndex.tierFor(v.roundToInt(), standard)).background }
    val text = { v: Double -> "${v.roundToInt()} ${if (european) euLabel else usLabel}" }
    val card = TvSeries(tileLabel(ChartTile.AIR_QUALITY), values, true, 50.0, color, { text(it) to null }, withPrevious = true)
    return card to chartDomain(values, true, 50.0)?.let { RightLine(values, it, color, text, dashed = true, dots = days) }
}

/** The card of the chance of rain, next to the precipitation; the precipitation plot draws it. */
private fun chanceSeries(chance: List<Double?>) = TvSeries(
    R.string.detail_precip_chance, chance, true, 100.0, { ChartStyle.chance }, { "${it.roundToInt()}%" to null }, withPrevious = true,
)

private fun tileLabel(tile: ChartTile) = when (tile) {
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

/** Left and right move the selected hour over the place's hours, up and down a whole day. OK shows the next value. */
@Composable
internal fun ChartScreen(data: WeatherData, startIndex: Int) {
    val hours = data.chartHourly
    if (hours.isEmpty()) {
        Message(stringResource(R.string.error_fetch_weather))
        return
    }
    val usLabel = stringResource(R.string.air_quality_us_aqi)
    val euLabel = stringResource(R.string.air_quality_eu_aqi)
    val metric = LocalMetricPrimary.current
    val series = remember(data, metric) { hourlySeries(data, metric, usLabel, euLabel) }
    var selected by remember { mutableIntStateOf(startIndex.coerceIn(hours.indices)) }
    var shownSeries by remember { mutableIntStateOf(0) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    val moves = mapOf(Key.DirectionLeft to -1, Key.DirectionRight to 1, Key.DirectionUp to -24, Key.DirectionDown to 24)
    val okKeys = setOf(Key.DirectionCenter, Key.Enter, Key.NumPadEnter)
    val titleFormat = rememberPlaceFormat("EEEE d MMM, HH:mm", data.utcOffsetSeconds)
    val hourFormat = rememberPlaceFormat("EEE HH:mm", data.utcOffsetSeconds)

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp, vertical = 27.dp)
            .focusRequester(focus)
            .onKeyEvent { event ->
                val move = moves[event.key]
                val down = event.type == KeyEventType.KeyDown
                when {
                    move != null -> {
                        if (down) selected = (selected + move).coerceIn(hours.indices)
                        true
                    }
                    event.key in okKeys && down && series.isNotEmpty() -> {
                        // The next series with a plot of its own
                        do shownSeries = (shownSeries + 1) % series.size while (series[shownSeries].withPrevious)
                        // A plot whose values end before the selection (air quality): select its last value
                        val last = series[shownSeries].values.indexOfLast { it != null }
                        if (last in 0 until selected) selected = last
                        true
                    }
                    else -> false
                }
            }
            .focusable(),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            WeatherIcon(hours[selected].weatherCode, night = hours[selected].isNight, size = 40.dp)
            Text(
                data.locationName, localize = false, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false),
            )
            Text(titleFormat(hours[selected].time), color = Dim, fontSize = 22.sp, maxLines = 1, softWrap = false)
        }
        Spacer(Modifier.height(8.dp))
        SeriesStrip(series, selected, shownSeries)
        Spacer(Modifier.height(12.dp))
        series.getOrNull(shownSeries)?.let { s ->
            // A line and a label every 12 hours of the place
            SeriesPlot(s, CHART_HOURS, CHART_LEAD_HOURS, selected, Modifier.weight(1f).fillMaxWidth()) { i ->
                val time = hours[i].time
                hourFormat(time).takeIf { ((time / 1000 + data.utcOffsetSeconds) / 3600) % 24 % 12 == 0L }
            }
        }
    }
}

/** The chart of the days: left and right move the selected day. OK shows the next value. */
@Composable
internal fun DailyChartScreen(data: WeatherData, startIndex: Int) {
    val days = data.dailyForecast
    if (days.isEmpty()) {
        Message(stringResource(R.string.error_fetch_weather))
        return
    }
    val metric = LocalMetricPrimary.current
    val usLabel = stringResource(R.string.air_quality_us_aqi)
    val euLabel = stringResource(R.string.air_quality_eu_aqi)
    val series = remember(data, metric) { dailySeries(data, metric, usLabel, euLabel) }
    var selected by remember { mutableIntStateOf(startIndex.coerceIn(days.indices)) }
    var shownSeries by remember { mutableIntStateOf(0) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    val moves = mapOf(Key.DirectionLeft to -1, Key.DirectionRight to 1)
    val okKeys = setOf(Key.DirectionCenter, Key.Enter, Key.NumPadEnter)
    val titleFormat = rememberPlaceFormat("EEEE d MMM", data.utcOffsetSeconds)
    val dayFormat = rememberPlaceFormat("EEE d", data.utcOffsetSeconds)

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp, vertical = 27.dp)
            .focusRequester(focus)
            .onKeyEvent { event ->
                val move = moves[event.key]
                val down = event.type == KeyEventType.KeyDown
                when {
                    move != null -> {
                        if (down) selected = (selected + move).coerceIn(days.indices)
                        true
                    }
                    event.key in okKeys && down -> {
                        // The next series with a plot of its own
                        do shownSeries = (shownSeries + 1) % series.size while (series[shownSeries].withPrevious)
                        // A plot whose values end before the selection (air quality): select its last value
                        val last = series[shownSeries].values.indexOfLast { it != null }
                        if (last in 0 until selected) selected = last
                        true
                    }
                    else -> false
                }
            }
            .focusable(),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            WeatherIcon(days[selected].weatherCode, night = false, size = 40.dp)
            Text(
                data.locationName, localize = false, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false),
            )
            Text(titleFormat(days[selected].date), color = Dim, fontSize = 22.sp, maxLines = 1, softWrap = false)
        }
        Spacer(Modifier.height(8.dp))
        SeriesStrip(series, selected, shownSeries)
        Spacer(Modifier.height(12.dp))
        // Every day has its line and its label
        SeriesPlot(series[shownSeries], days.size, 0, selected, Modifier.weight(1f).fillMaxWidth()) { dayFormat(days[it].date) }
    }
}

/** The values of the selected hour or day; the value in the plot has a band behind it. */
@Composable
private fun SeriesStrip(series: List<TvSeries>, selected: Int, shownSeries: Int) {
    // More than ten cards are narrow: a smaller value keeps "29.93 inHg" in one line
    val valueSize = if (series.size > 10) 13.sp else 15.sp
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        series.forEachIndexed { i, s ->
            val value = s.values[selected]
            val text = value?.let(s.text)
            // With a low: the low after the high, in the colour of its line
            val lowText = s.low?.get(selected)?.let(s.text)
            val shape = RoundedCornerShape(8.dp)
            Column(
                Modifier.weight(1f)
                    // A card lights up with the plot that shows it: its own, or that of the first card of its group
                    .background(if ((0..i).last { !series[it].withPrevious } == shownSeries) Card else Color.Transparent, shape)
                    .padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Two lines for every label, so that long labels show in full and the values stay in line
                Text(stringResource(s.label), color = Dim, fontSize = 11.sp, lineHeight = 13.sp, minLines = 2, maxLines = 2,
                    textAlign = TextAlign.Center)
                Row {
                    Text(text?.first ?: "--", color = value?.let(s.color) ?: Dim, fontSize = valueSize, fontWeight = FontWeight.Bold, maxLines = 1)
                    lowText?.let {
                        Text(" / ", color = Dim, fontSize = valueSize, fontWeight = FontWeight.Bold, maxLines = 1)
                        Text(it.first, color = s.lowColor ?: Dim, fontSize = valueSize, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }
                Text(listOfNotNull(text?.second, lowText?.second).joinToString(" / ").ifEmpty { " " }, color = Dim, fontSize = 12.sp, maxLines = 1)
            }
        }
    }
}

/**
 * A line through [points] (value and colour; null for a gap) as one path for each run of one colour, so that a dash
 * pattern runs on along the line instead of starting again at each hour.
 */
private fun DrawScope.dashedLine(
    points: List<Pair<Double, Color>?>, x: (Int) -> Float, y: (Double) -> Float, width: Float, dash: PathEffect?,
) {
    var path: Path? = null
    var color: Color? = null
    fun flush() {
        path?.let { p -> color?.let { drawPath(p, it, style = Stroke(width, cap = StrokeCap.Round, pathEffect = dash)) } }
        path = null
    }
    points.forEachIndexed { i, point ->
        if (point == null) { flush(); return@forEachIndexed }
        val (v, c) = point
        val here = Offset(x(i), y(v))
        if (path == null || c != color) {
            // A new run starts at the end of the last one, so that the line has no break
            val start = points.getOrNull(i - 1)?.let { Offset(x(i - 1), y(it.first)) }
            flush()
            path = Path().apply { (start ?: here).let { moveTo(it.x, it.y) } }
            color = c
        }
        path!!.lineTo(here.x, here.y)
    }
    flush()
}

/**
 * [window] entries of [series] (hours or days), [lead] of them before the selection, with the selected entry marked.
 * [axis] gives the label under an entry, with a line across the plot, or null for none.
 */
@Composable
private fun SeriesPlot(series: TvSeries, window: Int, lead: Int, selected: Int, modifier: Modifier, axis: (Int) -> String?) {
    // No plot after the last value: the air quality ends days before the rest
    val count = (series.values.indexOfLast { it != null } + 1).coerceAtLeast(1)
    val first = (selected - lead).coerceIn(0, (count - window).coerceAtLeast(0))
    val end = (first + window).coerceAtMost(count)
    val values = series.values.subList(first, end)
    val lows = series.low?.subList(first, end)
    val rightLine = series.right
    val rightPart = rightLine?.values?.subList(first, end)
    val extras = series.extras.map { it to it.values.subList(first, end) }
    // The scale of all the values, not only of the window, so that it stays the same while the selection moves
    val domain = chartDomain(
        series.values + series.low.orEmpty() + series.extras.flatMap { it.values }, series.fromZero, series.minSpan,
    ) ?: return
    val shape = LocalTvShaper.current
    Canvas(modifier) {
        val top = 20.dp.toPx()
        val bottom = size.height - 32.dp.toPx()
        // The space on the left is for the scale
        val left = 96.dp.toPx()
        // A few days: half a slot of space at each end, so the bars and the end labels stay inside
        // The space on the right is for the scale of the right line
        val scalePaint = Paint().apply { color = Dim.toArgb(); textSize = 14.sp.toPx(); isAntiAlias = true }
        val rightTicks = rightLine?.domain?.ticks?.map { it to shape(rightLine.text(it)) }.orEmpty()
        val right = size.width - (rightTicks.maxOfOrNull { scalePaint.measureText(it.second) }?.plus(12.dp.toPx()) ?: 0f)
        val inset = if (window < 24) (right - left) / window / 2 else 0f
        val slot = (right - left - 2 * inset) / (window - 1).coerceAtLeast(1)
        fun x(i: Int) = left + inset + slot * i
        fun y(v: Double) = (bottom - (v - domain.min) / (domain.max - domain.min) * (bottom - top)).toFloat()

        val label = Paint().apply { color = Dim.toArgb(); textSize = 14.sp.toPx(); isAntiAlias = true }
        val canvas = drawContext.canvas.nativeCanvas
        // The scale, in both units, with the second unit under the first
        domain.ticks.forEach { tick ->
            val tickY = y(tick)
            drawLine(Color.White.copy(alpha = 0.15f), Offset(left, tickY), Offset(right, tickY), 1.dp.toPx())
            val (primary, secondary) = series.text(tick)
            canvas.drawText(shape(primary), 0f, tickY + 4.dp.toPx(), label)
            secondary?.let { canvas.drawText(shape(it), 0f, tickY + 20.dp.toPx(), label) }
        }
        for (i in 0 until end - first) {
            axis(first + i)?.let { axisText ->
                drawLine(Color.White.copy(alpha = 0.15f), Offset(x(i), top), Offset(x(i), bottom), 1.dp.toPx())
                val text = shape(axisText)
                // Centred on its line, and kept inside the plot
                val textWidth = label.measureText(text)
                canvas.drawText(text, (x(i) - textWidth / 2).coerceIn(left, right - textWidth), size.height - 6.dp.toPx(), label)
            }
        }
        if (series.bars) {
            val width = slot * 0.7f
            values.forEachIndexed { i, v ->
                if (v != null && v > 0) drawRect(series.color(v), Offset(x(i) - width / 2, y(v)), Size(width, bottom - y(v)))
            }
        } else {
            // Each piece of a line has the colour of its value
            // A low line has a colour of its own
            for ((line, lineColor) in listOfNotNull(values to null, lows?.let { it to series.lowColor })) {
                line.zipWithNext().forEachIndexed { i, (a, b) ->
                    if (a != null && b != null) {
                        drawLine(lineColor ?: series.color((a + b) / 2), Offset(x(i), y(a)), Offset(x(i + 1), y(b)), 4.dp.toPx(), StrokeCap.Round)
                    }
                }
            }
        }

        // The thin lines: the dew point dashed
        val dash = chartDash(ChartStyle.dewPointDash)
        for ((line, part) in extras) {
            dashedLine(part.map { v -> v?.let { it to line.color } }, ::x, ::y, 2.dp.toPx(), if (line.dashed) dash else null)
        }

        // The right line on its own scale, with the scale on the right
        if (rightLine != null && rightPart != null) {
            val d = rightLine.domain
            fun rightY(v: Double) = (bottom - (v - d.min) / (d.max - d.min) * (bottom - top)).toFloat()
            rightTicks.forEach { (v, text) -> canvas.drawText(text, size.width - scalePaint.measureText(text), rightY(v) + 4.dp.toPx(), scalePaint) }
            val rightDash = chartDash(ChartStyle.airQualityDash)
            dashedLine(
                rightPart.map { v -> v?.let { it to rightLine.color(it) } }, ::x, ::rightY, 3.dp.toPx(),
                if (rightLine.dashed) rightDash else null,
            )
            if (rightLine.dots) rightPart.forEachIndexed { i, v -> v?.let { drawCircle(rightLine.color(it), 4.dp.toPx(), Offset(x(i), rightY(it))) } }
        }

        // The highest and lowest values in the window; the lowest rain is nearly always 0, so bars show only the highest
        val extreme = Paint().apply { color = Color.White.toArgb(); textSize = 16.sp.toPx(); isAntiAlias = true; isFakeBoldText = true }
        val known = values.withIndex().filter { it.value != null }
        val highest = known.maxByOrNull { it.value!! }
        // With a low line, the lowest of the lows
        val lowest = (lows ?: values).withIndex().filter { it.value != null }.minByOrNull { it.value!! }
        fun label(i: Int, v: Double, above: Boolean) {
            val text = shape(series.text(v).first)
            val width = extreme.measureText(text)
            val textX = (x(i) - width / 2).coerceIn(left, right - width)
            val pointY = y(v)
            var textY = if (above) pointY - 12.dp.toPx() else pointY + 26.dp.toPx()
            // Keep the label inside the plot
            if (textY < top + 4.dp.toPx()) textY = pointY + 26.dp.toPx()
            if (textY > bottom - 4.dp.toPx()) textY = pointY - 12.dp.toPx()
            drawCircle(Color.White, 4.dp.toPx(), Offset(x(i), pointY))
            canvas.drawText(text, textX, textY, extreme)
        }
        highest?.let { (i, v) -> if (!series.bars || v!! > 0) label(i, v!!, above = true) }
        lowest?.let { (i, v) -> if (!series.bars && (lows != null || i != highest?.index)) label(i, v!!, above = false) }

        // The selection can be after the last value (air quality); then it has no mark
        val mark = selected - first
        if (mark !in values.indices) return@Canvas
        drawLine(Color.White.copy(alpha = 0.6f), Offset(x(mark), top), Offset(x(mark), bottom), 2.dp.toPx())
        values[mark]?.let { v -> drawCircle(series.color(v), 8.dp.toPx(), Offset(x(mark), y(v))) }
        lows?.get(mark)?.let { v -> drawCircle(series.lowColor ?: series.color(v), 8.dp.toPx(), Offset(x(mark), y(v))) }
        for ((line, part) in extras) part[mark]?.let { v -> drawCircle(line.color, 6.dp.toPx(), Offset(x(mark), y(v))) }
    }
}
