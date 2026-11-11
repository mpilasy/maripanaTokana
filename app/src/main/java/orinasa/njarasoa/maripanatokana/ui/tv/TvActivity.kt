// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

package orinasa.njarasoa.maripanatokana.ui.tv

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import orinasa.njarasoa.maripanatokana.domain.model.SavedLocation
import orinasa.njarasoa.maripanatokana.R
import orinasa.njarasoa.maripanatokana.data.remote.wmoDescriptionRes
import orinasa.njarasoa.maripanatokana.domain.model.WeatherData
import orinasa.njarasoa.maripanatokana.domain.model.isDaytime
import orinasa.njarasoa.maripanatokana.domain.model.LocationFix
import orinasa.njarasoa.maripanatokana.domain.repository.LocationRepository
import orinasa.njarasoa.maripanatokana.domain.repository.WeatherRepository
import orinasa.njarasoa.maripanatokana.ui.theme.MaripanaTokanaTheme
import orinasa.njarasoa.maripanatokana.ui.theme.temperatureColor
import orinasa.njarasoa.maripanatokana.ui.common.components.WeatherIcon
import orinasa.njarasoa.maripanatokana.ui.common.components.colorsFor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone
import javax.inject.Inject

private const val HOUR_COUNT = 8
private const val DAY_COUNT = 10
private const val LOCATION_TIMEOUT_MS = 10_000L
internal const val REFRESH_MS = 30 * 60_000L
private const val RETRY_MS = 5 * 60_000L

internal val Background = Color(0xFF12123A)
internal val Card = Color(0xFF26265A)
internal val Dim = Color.White.copy(alpha = 0.65f)

private val LOCATION_PERMISSIONS = arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)

private sealed interface TvState {
    data object Loading : TvState
    data object Failed : TvState
    /** No saved place is active and the TV does not know where it is. */
    data object NoLocation : TvState
    /** [oldFixTime] is the time of the device fix when it is old (see [LocationFix.isOld]), else null. */
    data class Ready(val data: WeatherData, val oldFixTime: Long? = null) : TvState
}

private sealed interface TvScreen {
    data object Home : TvScreen
    data class Chart(val index: Int) : TvScreen
    data class DailyChart(val index: Int) : TvScreen
    data object Details : TvScreen
    data object AirQuality : TvScreen
    data object Alerts : TvScreen
    data object About : TvScreen
}

/** The TV prototype: the weather of one place, with a chart and details. Only a TV launcher starts it. */
@AndroidEntryPoint
class TvActivity : ComponentActivity() {

    @Inject
    lateinit var repository: WeatherRepository

    @Inject
    lateinit var locationRepository: LocationRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
        val places = TvPlaceStore(prefs)
        setContent {
            MaripanaTokanaTheme(darkTheme = true) {
                TvRoot(repository, locationRepository, places, TvDisplayPrefs(prefs))
            }
        }
    }
}

private fun hasLocationPermission(context: Context) =
    LOCATION_PERMISSIONS.any { context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }

/** False on a device with no enabled location provider. "fused" is a literal: its constant needs API 31. */
private fun canLocate(context: Context): Boolean {
    val manager = context.getSystemService(LocationManager::class.java)
    return listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, "fused").any { manager.isProviderEnabled(it) }
}

private suspend fun deviceLocation(locations: LocationRepository): LocationFix? =
    withTimeoutOrNull(LOCATION_TIMEOUT_MS) {
        locations.getLastLocation().getOrNull() ?: locations.getFreshLocation().getOrNull()
    }

/** Loads the weather of the active place, and shows it or the list of places. */
@Composable
private fun TvRoot(
    repository: WeatherRepository, locations: LocationRepository, store: TvPlaceStore, display: TvDisplayPrefs,
) {
    val context = LocalContext.current
    var saved by remember { mutableStateOf(store.saved()) }
    var activeId by remember { mutableStateOf(store.activeId()) }
    var attempt by remember { mutableIntStateOf(0) }
    var state by remember { mutableStateOf<TvState>(TvState.Loading) }
    var showPlaces by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var metric by remember { mutableStateOf(display.metricPrimary) }
    var localeIndex by remember { mutableIntStateOf(display.localeIndex) }
    var askedPermission by remember { mutableStateOf(false) }
    val locatable = remember { canLocate(context) }
    val permissions = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { attempt++ }

    // The language is a key: the place names and the alert text come in the language of the app
    /** Loads the weather of ([lat], [lon]) and then its alerts; false when the weather fails. */
    suspend fun load(lat: Double, lon: Double, place: SavedLocation?, oldFixTime: Long? = null): Boolean = repository.getWeather(lat, lon).fold(
        onSuccess = { loaded ->
            val data = if (place != null) loaded.copy(locationName = place.displayName, locationSubtext = place.subtext) else loaded
            state = TvState.Ready(data, oldFixTime)
            repository.fetchAlerts(lat, lon).onSuccess { result ->
                state = TvState.Ready(data.copy(alerts = result.alerts, failedAlertSources = result.failedSources), oldFixTime)
            }
            true
        },
        onFailure = { false },
    )

    LaunchedEffect(activeId, attempt, localeIndex) {
        state = TvState.Loading
        val place = saved.find { it.id == activeId }
        if (place == null && locatable && !hasLocationPermission(context) && !askedPermission) {
            askedPermission = true
            permissions.launch(LOCATION_PERMISSIONS)
            return@LaunchedEffect
        }
        val fix = if (place == null && locatable && hasLocationPermission(context)) deviceLocation(locations) else null
        val coordinates = when {
            place != null -> place.latitude to place.longitude
            fix != null -> fix.latitude to fix.longitude
            else -> null
        }
        if (coordinates == null) {
            state = TvState.NoLocation
            return@LaunchedEffect
        }
        val (lat, lon) = coordinates
        if (!load(lat, lon, place, fix?.takeIf { it.isOld() }?.timeMs)) state = TvState.Failed
    }

    // A TV can stay on this screen for hours: while the app is on screen, the weather loads again when it is 30 minutes old
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner, activeId, localeIndex) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                val ready = state as? TvState.Ready
                val data = ready?.data
                val wait = if (data == null) REFRESH_MS else REFRESH_MS - (System.currentTimeMillis() - data.timestamp)
                if (data == null || wait > 0) {
                    delay(wait.coerceAtLeast(RETRY_MS / 5))
                    continue
                }
                val lat = data.latitude
                val lon = data.longitude
                if (lat == null || lon == null || !load(lat, lon, saved.find { it.id == activeId }, ready.oldFixTime)) delay(RETRY_MS)
            }
        }
    }

    fun activate(id: String?) {
        store.setActiveId(id)
        activeId = id
        showPlaces = false
    }

    TvLocalized(localeIndex, metric) {
        Box(Modifier.fillMaxSize().background(Background)) {
            val s = state
            if (showSettings) {
                BackHandler { showSettings = false }
                SettingsScreen(
                    display = display,
                    metric = metric,
                    localeIndex = localeIndex,
                    onMetric = { metric = it },
                    onLocale = { localeIndex = it },
                )
            } else if (showPlaces || s == TvState.NoLocation) {
                BackHandler(enabled = s != TvState.NoLocation) { showPlaces = false }
                PlacesScreen(
                    saved = saved,
                    activeId = activeId,
                    showDevice = locatable,
                    locationFailed = locatable && s == TvState.NoLocation,
                    search = repository::searchLocation,
                    onDevice = { activate(null) },
                    onSaved = { activate(it.id) },
                    onAdd = { result ->
                        val place = result.toSavedLocation()
                        if (saved.none { it.id == place.id }) {
                            saved = saved + place
                            store.setSaved(saved)
                        }
                        activate(place.id)
                    },
                    onRemove = { place ->
                        saved = saved.filterNot { it.id == place.id }
                        store.setSaved(saved)
                        if (activeId == place.id) activate(null)
                    },
                )
            } else {
                when (s) {
                    TvState.Loading -> Message(stringResource(R.string.loading))
                    TvState.Failed -> FailedScreen(onRetry = { attempt++ }, onPlaces = { showPlaces = true })
                    is TvState.Ready -> TvApp(s.data, s.oldFixTime, onPlaces = { showPlaces = true }, onSettings = { showSettings = true })
                    TvState.NoLocation -> Unit
                }
            }
        }
    }
}

@Composable
internal fun Message(text: String) {
    Column(Modifier.fillMaxSize(), Arrangement.Center, Alignment.CenterHorizontally) {
        Text(text, color = Color.White, fontSize = 28.sp)
    }
}

@Composable
private fun FailedScreen(onRetry: () -> Unit, onPlaces: () -> Unit) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    Column(Modifier.fillMaxSize(), Arrangement.Center, Alignment.CenterHorizontally) {
        Text(stringResource(R.string.error_fetch_weather), color = Color.White, fontSize = 28.sp)
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            FocusCard(Modifier.focusRequester(focus), onClick = onRetry) {
                Text(stringResource(R.string.error_retry), color = Color.White, fontSize = 22.sp, modifier = Modifier.padding(16.dp))
            }
            FocusCard(Modifier, onClick = onPlaces) {
                Text(stringResource(R.string.locations_title), color = Color.White, fontSize = 22.sp, modifier = Modifier.padding(16.dp))
            }
        }
    }
}

/** Which screen shows. [focusKey] is the card that had focus, so Back returns to it. */
@Composable
private fun TvApp(data: WeatherData, oldFixTime: Long?, onPlaces: () -> Unit, onSettings: () -> Unit) {
    var screen by remember { mutableStateOf<TvScreen>(TvScreen.Home) }
    var focusKey by remember { mutableStateOf("hour0") }
    if (screen != TvScreen.Home) BackHandler { screen = TvScreen.Home }
    when (val s = screen) {
        TvScreen.Home -> HomeScreen(data, oldFixTime, focusKey, onPlaces, onSettings) { key, next ->
            focusKey = key
            screen = next
        }
        is TvScreen.Chart -> ChartScreen(data, s.index)
        is TvScreen.DailyChart -> DailyChartScreen(data, s.index)
        TvScreen.Details -> DetailsScreen(data)
        TvScreen.AirQuality -> data.airQuality?.let { AirQualityScreen(it, data.locationName) }
        TvScreen.Alerts -> AlertsScreen(data)
        TvScreen.About -> AboutPage()
    }
}

/** A formatter that shows the place's local time; the epoch millis are shifted by [offsetSeconds]. */
@Composable
internal fun rememberPlaceFormat(pattern: String, offsetSeconds: Int): (Long) -> String {
    val locale = LocalConfiguration.current.locales[0]
    return remember(pattern, offsetSeconds, locale) {
        val format = SimpleDateFormat(pattern, locale).apply { timeZone = TimeZone.getTimeZone("UTC") }
        // No dots ("lun.", "nov."): the digit helper would make them decimal commas
        val shift: (Long) -> String = { millis -> format.format(Date(millis + offsetSeconds * 1000L)).replace(".", "") }
        shift
    }
}

@Composable
private fun HomeScreen(data: WeatherData, oldFixTime: Long?, focusKey: String, onPlaces: () -> Unit, onSettings: () -> Unit, onOpen: (key: String, screen: TvScreen) -> Unit) {
    val metric = LocalMetricPrimary.current
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    val hourFormat = rememberPlaceFormat("HH:mm", data.utcOffsetSeconds)
    // Two lines: a day card is too narrow for the name and the date on one line
    val dayFormat = rememberPlaceFormat("EEE\nd", data.utcOffsetSeconds)
    fun cardModifier(key: String) = if (key == focusKey) Modifier.focusRequester(focus) else Modifier
    fun chartAt(time: Long) = TvScreen.Chart(data.chartHourly.indexOfFirst { it.time >= time }.coerceAtLeast(0))

    Row(Modifier.fillMaxSize().padding(horizontal = 48.dp, vertical = 27.dp), horizontalArrangement = Arrangement.spacedBy(32.dp)) {
        CurrentPanel(data, oldFixTime, Modifier.weight(0.3f), ::cardModifier, onPlaces, onSettings, onOpen)
        Column(Modifier.weight(0.7f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            CardRow(Modifier.weight(1f)) {
                data.hourlyForecast.take(HOUR_COUNT).forEachIndexed { i, hour ->
                    val day = data.dailySunrise.indexOfLast { it <= hour.time }.coerceAtLeast(0)
                    val night = !isDaytime(hour.time, data.dailySunrise.getOrElse(day) { 0L }, data.dailySunset.getOrElse(day) { 0L })
                    val (primary, secondary) = hour.temperature.displayDual(metric)
                    FocusCard(cardModifier("hour$i").weight(1f).fillMaxHeight(), onClick = { onOpen("hour$i", chartAt(hour.time)) }) {
                        ForecastCell(hourFormat(hour.time), hour.weatherCode, night, labelSize = 20, iconSize = 64) {
                            TempText(primary, secondary, temperatureColor(hour.temperature.celsius), primarySize = 26, secondarySize = 18)
                        }
                    }
                }
            }
            CardRow(Modifier.weight(1.25f)) {
                data.dailyForecast.take(DAY_COUNT).forEachIndexed { i, day ->
                    val (highP, highS) = day.tempMax.displayDual(metric)
                    val (lowP, lowS) = day.tempMin.displayDual(metric)
                    FocusCard(cardModifier("day$i").weight(1f).fillMaxHeight(), onClick = { onOpen("day$i", TvScreen.DailyChart(i)) }) {
                        ForecastCell(dayFormat(day.date), day.weatherCode, night = false, labelSize = 17, iconSize = 48) {
                            TempText(highP, highS, temperatureColor(day.tempMax.celsius), primarySize = 20, secondarySize = 15)
                            Spacer(Modifier.height(8.dp))
                            TempText(lowP, lowS, temperatureColor(day.tempMin.celsius), primarySize = 20, secondarySize = 15)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CurrentPanel(
    data: WeatherData, oldFixTime: Long?, modifier: Modifier, cardModifier: (String) -> Modifier, onPlaces: () -> Unit, onSettings: () -> Unit, onOpen: (String, TvScreen) -> Unit,
) {
    val metric = LocalMetricPrimary.current
    val (temp, tempSecond) = data.temperature.displayDual(metric)
    val (feels, feelsSecond) = data.feelsLike.displayDual(metric)
    Column(modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically)) {
        PanelButton(cardModifier("place"), onClick = onPlaces) {
            Text(data.locationName, localize = false, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        // The device fix is old: tell when it was taken
        if (oldFixTime != null) {
            val fixFormat = rememberPlaceFormat("HH:mm", data.utcOffsetSeconds)
            Text("${stringResource(R.string.last_known_location)} · ${fixFormat(oldFixTime)}", color = Dim, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        data.alerts.maxByOrNull { it.level.ordinal }?.let { worst ->
            PanelButton(cardModifier("alerts"), onClick = { onOpen("alerts", TvScreen.Alerts) }) {
                val text = if (data.alerts.size == 1) worst.titleKey else stringResource(R.string.alert_count_title, data.alerts.size.toString())
                Text(text, color = alertColor(worst.level), fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        WeatherIcon(data.weatherCode, night = !isDaytime(System.currentTimeMillis() / 1000, data.sunrise, data.sunset), size = 64.dp)
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(temp, color = temperatureColor(data.temperature.celsius), fontSize = 56.sp, fontWeight = FontWeight.Bold, lineHeight = 60.sp)
            Text(tempSecond, color = Dim, fontSize = 24.sp, modifier = Modifier.padding(bottom = 8.dp))
        }
        Text(stringResource(wmoDescriptionRes(data.weatherCode)), color = Color.White, fontSize = 20.sp)
        Text("${stringResource(R.string.feels_like)}: ${ltr("$feels · $feelsSecond")}", color = Dim, fontSize = 16.sp)
        // The place's time, and when the weather loaded
        val now by produceState(System.currentTimeMillis()) {
            while (true) {
                delay(15_000)
                value = System.currentTimeMillis()
            }
        }
        val timeFormat = rememberPlaceFormat("HH:mm", data.utcOffsetSeconds)
        Text("${timeFormat(now)} · ${stringResource(R.string.updated_time, timeFormat(data.timestamp))}", color = Dim, fontSize = 16.sp)
        data.airQuality?.let { aqi ->
            PanelButton(cardModifier("airQuality"), onClick = { onOpen("airQuality", TvScreen.AirQuality) }) {
                Text(stringResource(R.string.detail_air_quality), color = Color.White, fontSize = 16.sp, modifier = Modifier.weight(1f), maxLines = 1)
                Text(aqi.displayDual().first, color = colorsFor(aqi.primaryTier).background, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(stringArrayResource(R.array.aqi_tier_labels)[aqi.primaryTier.ordinal], color = colorsFor(aqi.primaryTier).background, fontSize = 14.sp, maxLines = 1)
            }
        }
        PanelButton(cardModifier("details"), onClick = { onOpen("details", TvScreen.Details) }) {
            Text(stringResource(R.string.section_current_conditions), color = Color.White, fontSize = 16.sp, maxLines = 1)
        }
        // Side by side, so the panel keeps its height
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            PanelButton(cardModifier("settings").weight(1f), onClick = onSettings) {
                Text(stringResource(R.string.settings_title), color = Color.White, fontSize = 16.sp, maxLines = 1)
            }
            PanelButton(cardModifier("about").weight(1f), onClick = { onOpen("about", TvScreen.About) }) {
                Text(stringResource(R.string.about_title), color = Color.White, fontSize = 16.sp, maxLines = 1)
            }
        }
    }
}

@Composable
private fun PanelButton(modifier: Modifier, onClick: () -> Unit, content: @Composable RowScope.() -> Unit) {
    FocusCard(modifier.fillMaxWidth(), onClick) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

@Composable
private fun CardRow(modifier: Modifier, content: @Composable RowScope.() -> Unit) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), content = content)
}

/** A card that the D-pad can reach: focus draws a white border, and OK runs [onClick]. */
@Composable
internal fun FocusCard(modifier: Modifier, onClick: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
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
private fun ForecastCell(label: String, code: Int, night: Boolean, labelSize: Int, iconSize: Int, values: @Composable () -> Unit) {
    Text(label, color = Dim, fontSize = labelSize.sp, lineHeight = (labelSize + 3).sp, textAlign = TextAlign.Center, maxLines = 2)
    Spacer(Modifier.height(8.dp))
    WeatherIcon(code, night = night, size = iconSize.dp)
    Spacer(Modifier.height(8.dp))
    values()
}

@Composable
private fun TempText(primary: String, secondary: String, color: Color, primarySize: Int, secondarySize: Int) {
    Text(primary, color = color, fontSize = primarySize.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    Text(secondary, color = color.copy(alpha = 0.6f), fontSize = secondarySize.sp, maxLines = 1)
}
