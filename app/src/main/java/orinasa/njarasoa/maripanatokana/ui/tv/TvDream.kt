// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

package orinasa.njarasoa.maripanatokana.ui.tv

import android.content.Context
import android.service.dreams.DreamService
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import orinasa.njarasoa.maripanatokana.R
import orinasa.njarasoa.maripanatokana.data.remote.wmoDescriptionRes
import orinasa.njarasoa.maripanatokana.domain.model.WeatherData
import orinasa.njarasoa.maripanatokana.domain.model.isDaytime
import orinasa.njarasoa.maripanatokana.domain.repository.LocationRepository
import orinasa.njarasoa.maripanatokana.domain.repository.WeatherRepository
import orinasa.njarasoa.maripanatokana.ui.theme.MaripanaTokanaTheme
import orinasa.njarasoa.maripanatokana.ui.theme.temperatureColor
import orinasa.njarasoa.maripanatokana.ui.weather.components.WeatherIcon
import java.text.SimpleDateFormat
import java.util.Date
import javax.inject.Inject
import kotlin.random.Random

private const val DREAM_HOURS = 6
// The text moves a little each minute, so it does not mark the screen
private const val DRIFT_MS = 60_000L
private const val DRIFT_DP = 60

/** The screensaver: the time, and the weather of the place the TV app shows. */
@AndroidEntryPoint
class TvDreamService : DreamService(), LifecycleOwner, SavedStateRegistryOwner {

    @Inject
    lateinit var repository: WeatherRepository

    @Inject
    lateinit var locationRepository: LocationRepository

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateController = SavedStateRegistryController.create(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry

    override fun onCreate() {
        super.onCreate()
        savedStateController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        isInteractive = false
        isFullscreen = true
        val prefs = getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
        val places = TvPlaceStore(prefs)
        val display = TvDisplayPrefs(prefs)
        val view = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@TvDreamService)
            setViewTreeSavedStateRegistryOwner(this@TvDreamService)
            setContent {
                MaripanaTokanaTheme(darkTheme = true) {
                    TvLocalized(display.localeIndex, display.metricPrimary) {
                        DreamScreen { loadWeather(places) }
                    }
                }
            }
        }
        setContentView(view)
    }

    /** The weather of the TV app's place: its saved place, or else the device's last location. */
    private suspend fun loadWeather(places: TvPlaceStore): WeatherData? {
        val place = places.saved().find { it.id == places.activeId() }
        val coordinates = place?.let { it.latitude to it.longitude }
            ?: withTimeoutOrNull(10_000L) { locationRepository.getLastLocation().getOrNull() }
            ?: return null
        val loaded = repository.getWeather(coordinates.first, coordinates.second).getOrNull() ?: return null
        val data = if (place != null) loaded.copy(locationName = place.displayName) else loaded
        val alerts = repository.fetchAlerts(coordinates.first, coordinates.second).getOrNull()?.alerts.orEmpty()
        return data.copy(alerts = alerts)
    }

    override fun onDreamingStarted() {
        super.onDreamingStarted()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    override fun onDreamingStopped() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        super.onDreamingStopped()
    }

    override fun onDestroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        super.onDestroy()
    }
}

@Composable
private fun DreamScreen(load: suspend () -> WeatherData?) {
    val data by produceState<WeatherData?>(null) {
        while (true) {
            load()?.let { value = it }
            delay(REFRESH_MS)
        }
    }
    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            delay(5_000)
            value = System.currentTimeMillis()
        }
    }
    val drift by produceState(0 to 0) {
        while (true) {
            delay(DRIFT_MS)
            value = Random.nextInt(-DRIFT_DP, DRIFT_DP) to Random.nextInt(-DRIFT_DP, DRIFT_DP)
        }
    }
    // The clock shows the TV's own time
    val locale = LocalConfiguration.current.locales[0]
    val clock = remember(locale) { SimpleDateFormat("HH:mm", locale) }
    Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        Column(Modifier.offset(drift.first.dp, drift.second.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(clock.format(Date(now)), color = Color.White.copy(alpha = 0.85f), fontSize = 96.sp, fontWeight = FontWeight.Light)
            data?.let { DreamWeather(it) }
        }
    }
}

@Composable
private fun DreamWeather(data: WeatherData) {
    val metric = LocalMetricPrimary.current
    val (temp, tempSecond) = data.temperature.displayDual(metric)
    val hourFormat = rememberPlaceFormat("HH:mm", data.utcOffsetSeconds)
    Spacer(Modifier.height(8.dp))
    Text(data.locationName, localize = false, color = Dim, fontSize = 28.sp)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        WeatherIcon(data.weatherCode, night = !isDaytime(System.currentTimeMillis() / 1000, data.sunrise, data.sunset), size = 72.dp)
        Text(temp, color = temperatureColor(data.temperature.celsius), fontSize = 64.sp, fontWeight = FontWeight.Bold)
        Text(tempSecond, color = Dim, fontSize = 28.sp)
    }
    Text(stringResource(wmoDescriptionRes(data.weatherCode)), color = Dim, fontSize = 22.sp)
    data.alerts.maxByOrNull { it.level.ordinal }?.let { worst ->
        Spacer(Modifier.height(8.dp))
        Text(worst.headline ?: worst.titleKey, localize = false, color = alertColor(worst.level), fontSize = 22.sp, maxLines = 1)
    }
    Spacer(Modifier.height(20.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
        data.hourlyForecast.take(DREAM_HOURS).forEach { hour ->
            val day = data.dailySunrise.indexOfLast { it <= hour.time }.coerceAtLeast(0)
            val night = !isDaytime(hour.time, data.dailySunrise.getOrElse(day) { 0L }, data.dailySunset.getOrElse(day) { 0L })
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(4.dp)) {
                Text(hourFormat(hour.time), color = Dim, fontSize = 18.sp)
                WeatherIcon(hour.weatherCode, night = night, size = 44.dp)
                Text(hour.temperature.displayDual(metric).first, color = temperatureColor(hour.temperature.celsius), fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
