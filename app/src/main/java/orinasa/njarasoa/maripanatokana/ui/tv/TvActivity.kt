// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

package orinasa.njarasoa.maripanatokana.ui.tv

import android.graphics.Paint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dagger.hilt.android.AndroidEntryPoint
import orinasa.njarasoa.maripanatokana.R
import orinasa.njarasoa.maripanatokana.data.remote.wmoDescriptionRes
import orinasa.njarasoa.maripanatokana.domain.model.ChartHour
import orinasa.njarasoa.maripanatokana.domain.model.WeatherData
import orinasa.njarasoa.maripanatokana.domain.model.isDaytime
import orinasa.njarasoa.maripanatokana.domain.repository.WeatherRepository
import orinasa.njarasoa.maripanatokana.ui.theme.MaripanaTokanaTheme
import orinasa.njarasoa.maripanatokana.ui.theme.temperatureColor
import orinasa.njarasoa.maripanatokana.ui.weather.components.WeatherIcon
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

// The prototype shows one fixed place in metric first; places and units come later
private const val PLACE_NAME = "Roseville"
private const val PLACE_LAT = 45.0061
private const val PLACE_LON = -93.1566
private const val METRIC_PRIMARY = true

private const val HOUR_COUNT = 8
private const val DAY_COUNT = 10
private const val CHART_HOURS = 72
private const val CHART_LEAD_HOURS = 24
private const val HOUR_MS = 3_600_000L

private val Background = Color(0xFF12123A)
private val Card = Color(0xFF26265A)
private val Dim = Color.White.copy(alpha = 0.65f)

private sealed interface TvState {
    data object Loading : TvState
    data object Failed : TvState
    data class Ready(val data: WeatherData) : TvState
}

/** The TV prototype: one screen of weather for a fixed place, and a chart. Only a TV launcher starts it. */
@AndroidEntryPoint
class TvActivity : ComponentActivity() {

    @Inject
    lateinit var repository: WeatherRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaripanaTokanaTheme(darkTheme = true) {
                var attempt by remember { mutableIntStateOf(0) }
                var state by remember { mutableStateOf<TvState>(TvState.Loading) }
                LaunchedEffect(attempt) {
                    state = TvState.Loading
                    state = repository.getWeather(PLACE_LAT, PLACE_LON).fold({ TvState.Ready(it) }, { TvState.Failed })
                }
                Box(Modifier.fillMaxSize().background(Background)) {
                    when (val s = state) {
                        TvState.Loading -> Message(stringResource(R.string.loading))
                        TvState.Failed -> FailedScreen { attempt++ }
                        is TvState.Ready -> TvApp(s.data)
                    }
                }
            }
        }
    }
}

@Composable
private fun BoxMessageText(text: String) {
    Text(text, color = Color.White, fontSize = 28.sp)
}

@Composable
private fun Message(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { BoxMessageText(text) }
}

@Composable
private fun FailedScreen(onRetry: () -> Unit) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    Column(Modifier.fillMaxSize(), Arrangement.Center, Alignment.CenterHorizontally) {
        BoxMessageText(stringResource(R.string.error_fetch_weather))
        Spacer(Modifier.height(24.dp))
        FocusCard(Modifier.focusRequester(focus).padding(0.dp), onClick = onRetry) {
            Text(stringResource(R.string.error_retry), color = Color.White, fontSize = 22.sp, modifier = Modifier.padding(16.dp))
        }
    }
}

/** Which screen shows. [focusKey] is the card that had focus, so Back returns to it. */
@Composable
private fun TvApp(data: WeatherData) {
    var chartIndex by remember { mutableStateOf<Int?>(null) }
    var focusKey by remember { mutableStateOf("hour0") }
    val index = chartIndex
    if (index == null) {
        HomeScreen(data, focusKey) { key, time ->
            focusKey = key
            chartIndex = data.chartHourly.indexOfFirst { it.time >= time }.coerceAtLeast(0)
        }
    } else {
        BackHandler { chartIndex = null }
        ChartScreen(data, index)
    }
}

/** A formatter that shows the place's local time; the epoch millis are shifted by [offsetSeconds]. */
private fun placeFormat(pattern: String, offsetSeconds: Int): (Long) -> String {
    val format = SimpleDateFormat(pattern, Locale.getDefault()).apply { timeZone = TimeZone.getTimeZone("UTC") }
    return { millis -> format.format(Date(millis + offsetSeconds * 1000L)) }
}

@Composable
private fun HomeScreen(data: WeatherData, focusKey: String, onOpen: (key: String, time: Long) -> Unit) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    val hourFormat = remember(data.utcOffsetSeconds) { placeFormat("HH:mm", data.utcOffsetSeconds) }
    val dayFormat = remember(data.utcOffsetSeconds) { placeFormat("EEE d", data.utcOffsetSeconds) }
    fun cardModifier(key: String) = if (key == focusKey) Modifier.focusRequester(focus) else Modifier

    Row(Modifier.fillMaxSize().padding(horizontal = 48.dp, vertical = 27.dp), horizontalArrangement = Arrangement.spacedBy(32.dp)) {
        CurrentPanel(data, Modifier.weight(0.3f))
        Column(Modifier.weight(0.7f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            CardRow(Modifier.weight(1f)) {
                data.hourlyForecast.take(HOUR_COUNT).forEachIndexed { i, hour ->
                    val day = data.dailySunrise.indexOfLast { it <= hour.time }.coerceAtLeast(0)
                    val night = !isDaytime(hour.time, data.dailySunrise.getOrElse(day) { 0L }, data.dailySunset.getOrElse(day) { 0L })
                    val (primary, secondary) = hour.temperature.displayDual(METRIC_PRIMARY)
                    FocusCard(cardModifier("hour$i").weight(1f), onClick = { onOpen("hour$i", hour.time) }) {
                        ForecastCell(hourFormat(hour.time), hour.weatherCode, night) {
                            TempText(primary, secondary, temperatureColor(hour.temperature.celsius))
                        }
                    }
                }
            }
            CardRow(Modifier.weight(1.25f)) {
                data.dailyForecast.take(DAY_COUNT).forEachIndexed { i, day ->
                    val (highP, highS) = day.tempMax.displayDual(METRIC_PRIMARY)
                    val (lowP, lowS) = day.tempMin.displayDual(METRIC_PRIMARY)
                    // Noon of the day, so the chart opens in the daytime
                    FocusCard(cardModifier("day$i").weight(1f), onClick = { onOpen("day$i", day.date + 12 * HOUR_MS) }) {
                        ForecastCell(dayFormat(day.date), day.weatherCode, night = false) {
                            TempText(highP, highS, temperatureColor(day.tempMax.celsius))
                            Spacer(Modifier.height(6.dp))
                            TempText(lowP, lowS, temperatureColor(day.tempMin.celsius))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CurrentPanel(data: WeatherData, modifier: Modifier) {
    val (temp, tempSecond) = data.temperature.displayDual(METRIC_PRIMARY)
    val (feels, feelsSecond) = data.feelsLike.displayDual(METRIC_PRIMARY)
    Column(modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
        Text(PLACE_NAME, color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        WeatherIcon(data.weatherCode, night = !isDaytime(System.currentTimeMillis() / 1000, data.sunrise, data.sunset), size = 96.dp)
        Spacer(Modifier.height(8.dp))
        Text(temp, color = temperatureColor(data.temperature.celsius), fontSize = 72.sp, fontWeight = FontWeight.Bold)
        Text(tempSecond, color = Dim, fontSize = 28.sp)
        Spacer(Modifier.height(12.dp))
        Text(stringResource(wmoDescriptionRes(data.weatherCode)), color = Color.White, fontSize = 22.sp)
        Text("${stringResource(R.string.feels_like)}: $feels · $feelsSecond", color = Dim, fontSize = 18.sp)
    }
}

@Composable
private fun CardRow(modifier: Modifier, content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), content = content)
}

/** A card that the D-pad can reach: focus draws a white border, and OK runs [onClick]. */
@Composable
private fun FocusCard(modifier: Modifier, onClick: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier
            .background(if (focused) Card.copy(alpha = 1f) else Card.copy(alpha = 0.55f), shape)
            .border(3.dp, if (focused) Color.White else Color.Transparent, shape)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        content = content,
    )
}

@Composable
private fun ForecastCell(label: String, code: Int, night: Boolean, values: @Composable () -> Unit) {
    Text(label, color = Dim, fontSize = 15.sp, maxLines = 1)
    Spacer(Modifier.height(6.dp))
    WeatherIcon(code, night = night, size = 40.dp)
    Spacer(Modifier.height(6.dp))
    values()
}

@Composable
private fun TempText(primary: String, secondary: String, color: Color) {
    Text(primary, color = color, fontSize = 19.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    Text(secondary, color = color.copy(alpha = 0.6f), fontSize = 14.sp, maxLines = 1)
}

/** Left and right move the selected hour over the place's hours, up and down a whole day. */
@Composable
private fun ChartScreen(data: WeatherData, startIndex: Int) {
    val hours = data.chartHourly
    if (hours.isEmpty()) {
        Message(stringResource(R.string.error_fetch_weather))
        return
    }
    var selected by remember { mutableIntStateOf(startIndex.coerceIn(hours.indices)) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    val moves = mapOf(Key.DirectionLeft to -1, Key.DirectionRight to 1, Key.DirectionUp to -24, Key.DirectionDown to 24)
    val titleFormat = remember(data.utcOffsetSeconds) { placeFormat("EEEE d MMM, HH:mm", data.utcOffsetSeconds) }
    val hourFormat = remember(data.utcOffsetSeconds) { placeFormat("EEE HH:mm", data.utcOffsetSeconds) }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp, vertical = 27.dp)
            .focusRequester(focus)
            .onKeyEvent { event ->
                val move = moves[event.key]
                if (move != null && event.type == KeyEventType.KeyDown) {
                    selected = (selected + move).coerceIn(hours.indices)
                    true
                } else {
                    move != null
                }
            }
            .focusable(),
    ) {
        ChartHeader(hours[selected], titleFormat(hours[selected].time))
        Spacer(Modifier.height(16.dp))
        TemperatureLine(hours, selected, data.utcOffsetSeconds, hourFormat, Modifier.weight(1f).fillMaxWidth())
    }
}

@Composable
private fun ChartHeader(hour: ChartHour, title: String) {
    Text(title, color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(40.dp), verticalAlignment = Alignment.CenterVertically) {
        WeatherIcon(hour.weatherCode, night = hour.isNight, size = 56.dp)
        hour.temperature?.let { t ->
            val (p, s) = t.displayDual(METRIC_PRIMARY)
            HeaderValue(stringResource(R.string.detail_temperature), p, s, temperatureColor(t.celsius))
        }
        hour.precipitation?.let { r ->
            val (p, s) = r.displayDual(METRIC_PRIMARY)
            HeaderValue(stringResource(R.string.detail_precipitation), p, s, Color.White)
        }
        hour.windSpeed?.let { w ->
            val (p, s) = w.displayDual(METRIC_PRIMARY)
            HeaderValue(stringResource(R.string.detail_wind), p, s, Color.White)
        }
    }
}

@Composable
private fun HeaderValue(label: String, primary: String, secondary: String, color: Color) {
    Column {
        Text(label, color = Dim, fontSize = 16.sp)
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(primary, color = color, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Text(secondary, color = Dim, fontSize = 18.sp)
        }
    }
}

/** The temperature of [CHART_HOURS] hours around the selection, with the selected hour marked. */
@Composable
private fun TemperatureLine(
    hours: List<ChartHour>, selected: Int, offsetSeconds: Int, hourFormat: (Long) -> String, modifier: Modifier,
) {
    val first = (selected - CHART_LEAD_HOURS).coerceIn(0, (hours.size - CHART_HOURS).coerceAtLeast(0))
    val shown = hours.subList(first, (first + CHART_HOURS).coerceAtMost(hours.size))
    val temps = shown.map { it.temperature?.celsius }
    val known = temps.filterNotNull()
    if (known.isEmpty()) return
    val min = known.min() - 1
    val max = known.max() + 1
    Canvas(modifier) {
        val bottom = size.height - 32.dp.toPx()
        fun x(i: Int) = size.width * i / (CHART_HOURS - 1)
        fun y(c: Double) = (bottom - (c - min) / (max - min) * bottom).toFloat()

        val label = Paint().apply { color = Dim.toArgb(); textSize = 16.sp.toPx(); isAntiAlias = true }
        shown.forEachIndexed { i, h ->
            val localHour = (((h.time / 1000 + offsetSeconds) / 3600) % 24).toInt()
            if (localHour % 12 == 0) {
                drawLine(Color.White.copy(alpha = 0.15f), Offset(x(i), 0f), Offset(x(i), bottom), 1.dp.toPx())
                drawContext.canvas.nativeCanvas.drawText(hourFormat(h.time), x(i), size.height - 6.dp.toPx(), label)
            }
        }
        val path = Path()
        var started = false
        temps.forEachIndexed { i, c ->
            if (c == null) return@forEachIndexed
            if (started) path.lineTo(x(i), y(c)) else path.moveTo(x(i), y(c))
            started = true
        }
        drawPath(path, Color.White, style = Stroke(3.dp.toPx()))

        val mark = selected - first
        drawLine(Color.White.copy(alpha = 0.6f), Offset(x(mark), 0f), Offset(x(mark), bottom), 2.dp.toPx())
        temps[mark]?.let { c -> drawCircle(temperatureColor(c), 8.dp.toPx(), Offset(x(mark), y(c))) }
    }
}
