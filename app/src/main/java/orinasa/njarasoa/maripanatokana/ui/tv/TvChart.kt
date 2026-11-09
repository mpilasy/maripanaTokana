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
import orinasa.njarasoa.maripanatokana.domain.model.HourlyAirQuality
import orinasa.njarasoa.maripanatokana.domain.model.Precipitation
import orinasa.njarasoa.maripanatokana.domain.model.Pressure
import orinasa.njarasoa.maripanatokana.domain.model.Temperature
import orinasa.njarasoa.maripanatokana.domain.model.WeatherData
import orinasa.njarasoa.maripanatokana.domain.model.WindSpeed
import orinasa.njarasoa.maripanatokana.ui.theme.temperatureColor
import orinasa.njarasoa.maripanatokana.ui.weather.chart.ChartStyle
import orinasa.njarasoa.maripanatokana.ui.weather.chart.ChartTile
import orinasa.njarasoa.maripanatokana.ui.weather.chart.chartAirQuality
import orinasa.njarasoa.maripanatokana.ui.weather.chart.chartDomain
import orinasa.njarasoa.maripanatokana.ui.weather.chart.chartTiles
import orinasa.njarasoa.maripanatokana.ui.weather.components.WeatherIcon
import orinasa.njarasoa.maripanatokana.ui.weather.components.colorsFor
import java.util.Locale
import kotlin.math.roundToInt

private const val CHART_HOURS = 72
private const val CHART_LEAD_HOURS = 24

/**
 * One value of the chart over the hours. [values] has one entry for each hour, in the base unit of the value.
 * [text] gives a value in both units (the second is null when the value has one unit).
 */
private class TvSeries(
    val tile: ChartTile,
    val values: List<Double?>,
    val fromZero: Boolean,
    val minSpan: Double,
    val color: (Double) -> Color,
    val text: (Double) -> Pair<String, String?>,
    val bars: Boolean = false,
)

private fun temperatureText(c: Double, metric: Boolean): Pair<String, String?> = Temperature.fromCelsius(c).displayDual(metric)

private fun windText(ms: Double, metric: Boolean): Pair<String, String?> = WindSpeed.fromMetersPerSecond(ms).displayDual(metric)

private fun airQualitySeries(hours: List<ChartHour>, entries: List<HourlyAirQuality>, standard: AqiStandard, usLabel: String, euLabel: String): TvSeries {
    val european = standard == AqiStandard.EUROPEAN
    val matches = chartAirQuality(hours.map { it.time }, entries.map { it.time })
    val values = matches.map { i -> i?.let { (if (european) entries[it].europeanValue else entries[it].usValue).toDouble() } }
    return TvSeries(
        ChartTile.AIR_QUALITY, values, fromZero = true, minSpan = 50.0,
        color = { colorsFor(AirQualityIndex.tierFor(it.roundToInt(), standard)).background },
        text = { "${it.roundToInt()} ${if (european) euLabel else usLabel}" to null },
    )
}

private fun seriesFor(tile: ChartTile, data: WeatherData, metric: Boolean, usLabel: String, euLabel: String): TvSeries {
    val hours = data.chartHourly
    return when (tile) {
        ChartTile.TEMPERATURE -> TvSeries(tile, hours.map { it.temperature?.celsius }, false, 5.0, { temperatureColor(it) }, { temperatureText(it, metric) })
        ChartTile.DEW_POINT -> TvSeries(tile, hours.map { it.dewPoint?.celsius }, false, 5.0, { temperatureColor(it) }, { temperatureText(it, metric) })
        ChartTile.PRECIPITATION -> TvSeries(
            tile, hours.map { it.precipitation?.mm }, true, 1.0, { ChartStyle.precipitation },
            { Precipitation.fromMm(it).displayDual(metric) }, bars = true,
        )
        ChartTile.WIND -> TvSeries(tile, hours.map { it.windSpeed?.metersPerSecond }, true, 5.0, { ChartStyle.wind }, { windText(it, metric) })
        ChartTile.WIND_GUST -> TvSeries(tile, hours.map { it.windGust?.metersPerSecond }, true, 5.0, { ChartStyle.wind }, { windText(it, metric) })
        ChartTile.PRESSURE -> TvSeries(
            tile, hours.map { it.pressure?.hPa }, false, 4.0, { ChartStyle.pressure },
            { Pressure.fromHPa(it).displayDual(metric) },
        )
        ChartTile.HUMIDITY -> TvSeries(tile, hours.map { it.humidity?.toDouble() }, true, 100.0, { ChartStyle.humidity }, { "${it.roundToInt()}%" to null })
        ChartTile.UV_INDEX -> TvSeries(
            tile, hours.map { h -> h.uvIndex?.takeIf { it.isFinite() } }, true, 5.0, { colorsFor(it).background },
            { "%.1f".format(Locale.US, it) to null },
        )
        ChartTile.AIR_QUALITY -> airQualitySeries(hours, data.hourlyAirQuality, data.airQuality?.primaryStandard ?: AqiStandard.US, usLabel, euLabel)
    }
}

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
        Message(stringResource(R.string.error_fetch_weather), data.locationName)
        return
    }
    val usLabel = stringResource(R.string.air_quality_us_aqi)
    val euLabel = stringResource(R.string.air_quality_eu_aqi)
    val metric = LocalMetricPrimary.current
    val series = remember(data, metric) {
        chartTiles(hours, data.hourlyAirQuality.isNotEmpty()).map { seriesFor(it, data, metric, usLabel, euLabel) }
    }
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
                        shownSeries = (shownSeries + 1) % series.size
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
            SeriesPlot(s, hours, selected, data.utcOffsetSeconds, hourFormat, Modifier.weight(1f).fillMaxWidth())
        }
    }
}

/** The values of the selected hour; the value in the plot has a band behind it. */
@Composable
private fun SeriesStrip(series: List<TvSeries>, selected: Int, shownSeries: Int) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        series.forEachIndexed { i, s ->
            val value = s.values[selected]
            val text = value?.let(s.text)
            val shape = RoundedCornerShape(8.dp)
            Column(
                Modifier.weight(1f).background(if (i == shownSeries) Card else Color.Transparent, shape).padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(stringResource(tileLabel(s.tile)), color = Dim, fontSize = 11.sp, maxLines = 1, textAlign = TextAlign.Center)
                Text(text?.first ?: "--", color = value?.let(s.color) ?: Dim, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(text?.second ?: " ", color = Dim, fontSize = 12.sp, maxLines = 1)
            }
        }
    }
}

/** [CHART_HOURS] hours of [series] around the selection, with the selected hour marked. */
@Composable
private fun SeriesPlot(
    series: TvSeries, hours: List<ChartHour>, selected: Int, offsetSeconds: Int, hourFormat: (Long) -> String, modifier: Modifier,
) {
    val first = (selected - CHART_LEAD_HOURS).coerceIn(0, (hours.size - CHART_HOURS).coerceAtLeast(0))
    val end = (first + CHART_HOURS).coerceAtMost(hours.size)
    val shown = hours.subList(first, end)
    val values = series.values.subList(first, end)
    val domain = chartDomain(values, series.fromZero, series.minSpan) ?: return
    val shape = LocalTvShaper.current
    Canvas(modifier) {
        val top = 20.dp.toPx()
        val bottom = size.height - 32.dp.toPx()
        // The space on the left is for the scale
        val left = 96.dp.toPx()
        val slot = (size.width - left) / (CHART_HOURS - 1)
        fun x(i: Int) = left + slot * i
        fun y(v: Double) = (bottom - (v - domain.min) / (domain.max - domain.min) * (bottom - top)).toFloat()

        val label = Paint().apply { color = Dim.toArgb(); textSize = 14.sp.toPx(); isAntiAlias = true }
        val canvas = drawContext.canvas.nativeCanvas
        // The scale, in both units, with the second unit under the first
        domain.ticks.forEach { tick ->
            val tickY = y(tick)
            drawLine(Color.White.copy(alpha = 0.15f), Offset(left, tickY), Offset(size.width, tickY), 1.dp.toPx())
            val (primary, secondary) = series.text(tick)
            canvas.drawText(shape(primary), 0f, tickY + 4.dp.toPx(), label)
            secondary?.let { canvas.drawText(shape(it), 0f, tickY + 20.dp.toPx(), label) }
        }
        shown.forEachIndexed { i, h ->
            val localHour = (((h.time / 1000 + offsetSeconds) / 3600) % 24).toInt()
            if (localHour % 12 == 0) {
                drawLine(Color.White.copy(alpha = 0.15f), Offset(x(i), top), Offset(x(i), bottom), 1.dp.toPx())
                val text = shape(hourFormat(h.time))
                // Centred on its line, and kept inside the plot
                val textWidth = label.measureText(text)
                canvas.drawText(text, (x(i) - textWidth / 2).coerceIn(left, size.width - textWidth), size.height - 6.dp.toPx(), label)
            }
        }
        if (series.bars) {
            val width = slot * 0.7f
            values.forEachIndexed { i, v ->
                if (v != null && v > 0) drawRect(series.color(v), Offset(x(i) - width / 2, y(v)), Size(width, bottom - y(v)))
            }
        } else {
            // Each piece of the line has the colour of its value
            values.zipWithNext().forEachIndexed { i, (a, b) ->
                if (a != null && b != null) {
                    drawLine(series.color((a + b) / 2), Offset(x(i), y(a)), Offset(x(i + 1), y(b)), 4.dp.toPx(), StrokeCap.Round)
                }
            }
        }

        // The highest and lowest values in the window; the lowest rain is nearly always 0, so bars show only the highest
        val extreme = Paint().apply { color = Color.White.toArgb(); textSize = 16.sp.toPx(); isAntiAlias = true; isFakeBoldText = true }
        val known = values.withIndex().filter { it.value != null }
        val highest = known.maxByOrNull { it.value!! }
        val lowest = known.minByOrNull { it.value!! }
        fun label(i: Int, v: Double, above: Boolean) {
            val text = shape(series.text(v).first)
            val width = extreme.measureText(text)
            val textX = (x(i) - width / 2).coerceIn(left, size.width - width)
            val pointY = y(v)
            var textY = if (above) pointY - 12.dp.toPx() else pointY + 26.dp.toPx()
            // Keep the label inside the plot
            if (textY < top + 4.dp.toPx()) textY = pointY + 26.dp.toPx()
            if (textY > bottom - 4.dp.toPx()) textY = pointY - 12.dp.toPx()
            drawCircle(Color.White, 4.dp.toPx(), Offset(x(i), pointY))
            canvas.drawText(text, textX, textY, extreme)
        }
        highest?.let { (i, v) -> if (!series.bars || v!! > 0) label(i, v!!, above = true) }
        lowest?.let { (i, v) -> if (!series.bars && i != highest?.index) label(i, v!!, above = false) }

        val mark = selected - first
        drawLine(Color.White.copy(alpha = 0.6f), Offset(x(mark), top), Offset(x(mark), bottom), 2.dp.toPx())
        values[mark]?.let { v -> drawCircle(series.color(v), 8.dp.toPx(), Offset(x(mark), y(v))) }
    }
}
