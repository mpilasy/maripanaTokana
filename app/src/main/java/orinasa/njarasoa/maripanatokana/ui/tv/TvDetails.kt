// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

package orinasa.njarasoa.maripanatokana.ui.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import orinasa.njarasoa.maripanatokana.R
import orinasa.njarasoa.maripanatokana.domain.model.AirQualityIndex
import orinasa.njarasoa.maripanatokana.domain.model.AlertLevel
import orinasa.njarasoa.maripanatokana.domain.model.AqiStandard
import orinasa.njarasoa.maripanatokana.domain.model.AqiTier
import orinasa.njarasoa.maripanatokana.domain.model.Moon
import orinasa.njarasoa.maripanatokana.domain.model.Seasons
import orinasa.njarasoa.maripanatokana.domain.model.WeatherData
import orinasa.njarasoa.maripanatokana.ui.common.latitudeText
import orinasa.njarasoa.maripanatokana.ui.common.components.MoonPhaseIcon
import orinasa.njarasoa.maripanatokana.ui.common.components.colorsFor
import java.util.Locale
import kotlin.math.roundToInt

private const val SCROLL_STEP_DP = 160
private val TileShape = RoundedCornerShape(12.dp)

internal fun alertColor(level: AlertLevel) = when (level) {
    AlertLevel.WATCH -> Color(0xFFFFC107)
    AlertLevel.WARNING -> Color(0xFFFF7043)
    AlertLevel.EMERGENCY -> Color(0xFFEF5350)
}

/** A focus target for a screen that has nothing to select, so that Back reaches the screen. */
@Composable
private fun rememberScreenFocus(): FocusRequester {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    return focus
}

@Composable
private fun Tile(
    label: String, primary: String, secondary: String?, modifier: Modifier, color: Color = Color.White, extra: String? = null,
    icon: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier.fillMaxHeight().background(Card.copy(alpha = 0.55f), TileShape).padding(12.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Long labels (Malagasy) take a second, smaller line rather than being cut
        Text(label, color = Dim, fontSize = 15.sp, lineHeight = 18.sp, maxLines = 2, textAlign = TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        icon?.invoke()
        // Long values ("30.02 inHg") a little smaller, so they fit a tile of the five-tile rows
        Text(primary, color = color, fontSize = if (primary.length > 8) 24.sp else 28.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        if (secondary != null) Text(secondary, color = Dim, fontSize = 18.sp, maxLines = 1)
        if (extra != null) Text(extra, color = Dim, fontSize = 13.sp, lineHeight = 16.sp, maxLines = 2, textAlign = TextAlign.Center)
    }
}

/** The detail values of the current conditions. */
@Composable
internal fun DetailsScreen(data: WeatherData) {
    val focus = rememberScreenFocus()
    val timeFormat = rememberPlaceFormat("HH:mm", data.utcOffsetSeconds)
    val directions = stringArrayResource(R.array.cardinal_directions)
    val uvLabels = stringArrayResource(R.array.uv_labels)
    val phases = stringArrayResource(R.array.moon_phases)
    val tiers = stringArrayResource(R.array.aqi_tier_labels)
    val km = stringResource(R.string.visibility_km)
    val mi = stringResource(R.string.visibility_mi)
    val noPrecip = stringResource(R.string.detail_no_precip)
    // Again with each weather refresh: a TV can show this screen for days
    val moon = remember(data.timestamp) { Moon.illumination(System.currentTimeMillis()) }
    val metric = LocalMetricPrimary.current
    val kilometres = data.visibility?.let { km.format(Locale.US, it / 1000.0) }
    val miles = data.visibility?.let { mi.format(Locale.US, it / 1609.34) }

    val precipitation = data.snow ?: data.rain
    val (precipP, precipS) = precipitation?.displayDual(metric) ?: (noPrecip to null)
    val (windP, windS) = data.windSpeed.displayDual(metric)
    val gust = data.windGust?.displayDual(metric)
    val (pressureP, pressureS) = data.pressure.displayDual(metric)
    val (dewP, dewS) = data.dewPoint.displayDual(metric)
    val direction = data.windDeg?.let { "${directions[((it * 16 + 180) / 360) % 16]} ($it°)" }
    val uvTier = data.uvIndex?.let { uv -> uvLabels[listOf(3.0, 6.0, 8.0, 11.0).count { uv >= it }] }
    val aqi = data.airQuality
    // Five, five and four tiles: four rows would be too short for the moon and the noon sun
    val rows: List<List<@Composable (Modifier) -> Unit>> = listOf(
        listOf(
            { m -> Tile(stringResource(R.string.detail_precipitation), precipP, precipS, m) },
            { m -> Tile(stringResource(R.string.detail_wind), windP, windS, m, extra = direction) },
            { m -> Tile(stringResource(R.string.detail_wind_gust), gust?.first ?: "--", gust?.second, m) },
            { m -> Tile(stringResource(R.string.detail_pressure), pressureP, pressureS, m) },
            { m -> Tile(stringResource(R.string.detail_humidity), data.humidity?.let { "$it%" } ?: "--", null, m, extra = "${stringResource(R.string.detail_dewpoint)}: ${ltr("$dewP · $dewS")}") },
        ),
        listOf(
            { m ->
                Tile(stringResource(R.string.detail_uv_index), data.uvIndex?.let { "%.1f".format(Locale.US, it) } ?: "--", uvTier, m,
                    color = data.uvIndex?.let { colorsFor(it).background } ?: Color.White)
            },
            { m ->
                Tile(stringResource(R.string.detail_visibility), (if (metric) kilometres else miles) ?: "--", if (metric) miles else kilometres, m)
            },
            { m -> Tile(stringResource(R.string.detail_cloud_cover), data.cloudCover?.let { "$it%" } ?: "--", null, m) },
            { m ->
                Tile(stringResource(R.string.detail_moon), phases[Moon.phaseIndex(moon.phase)], null, m) {
                    MoonPhaseIcon(moon.phase, southern = (data.latitude ?: 0.0) < 0, size = 56.dp)
                }
            },
            { m ->
                Tile(stringResource(R.string.detail_air_quality), aqi?.displayDual()?.first ?: "--", aqi?.let { tiers[it.primaryTier.ordinal] }, m,
                    color = aqi?.let { colorsFor(it.primaryTier).background } ?: Color.White)
            },
        ),
        listOf(
            { m -> Tile(stringResource(R.string.detail_sunrise), timeFormat(data.sunrise * 1000), null, m) },
            { m -> Tile(stringResource(R.string.detail_sunset), timeFormat(data.sunset * 1000), null, m) },
            { m -> NoonTile(data, directions, m) },
            { m -> DeclinationTile(data, directions, m) },
        ),
    )
    Column(
        Modifier.fillMaxSize().padding(horizontal = 48.dp, vertical = 27.dp).focusRequester(focus).focusable(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(data.locationName, localize = false, color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        rows.forEach { row ->
            Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { tile -> tile(Modifier.weight(1f)) }
            }
        }
    }
}

/** The noon sun from overhead today and in a year. */
@Composable
private fun NoonTile(data: WeatherData, directions: Array<String>, modifier: Modifier) {
    val lat = data.latitude
    if (lat == null) {
        Tile(stringResource(R.string.noon_from_overhead), "--", null, modifier)
        return
    }
    val polar = data.sunset <= data.sunrise || data.sunset - data.sunrise >= 86_400
    // Today's solar noon is half way between sunrise and sunset
    val ms = if (polar) System.currentTimeMillis() else (data.sunrise + data.sunset) / 2 * 1000
    val text = { l: Double -> latitudeText(l, directions) { it } }
    val (min, max) = Seasons.noonRange(lat)
    // An offset to the south is positive, a latitude to the south negative
    Tile(stringResource(R.string.noon_from_overhead), text(-Seasons.noonOffset(lat, ms)), null, modifier,
        extra = stringResource(R.string.noon_range, text(-min), text(-max)))
}

/** The declination (where on Earth the sun is straight overhead), the days until the next solstice or equinox, and its name and date. */
@Composable
private fun DeclinationTile(data: WeatherData, directions: Array<String>, modifier: Modifier) {
    val now = System.currentTimeMillis()
    val around = remember(now / 3_600_000) { Seasons.around(now) }
    val days = Seasons.daysUntil(now, around.next.time, data.utcOffsetSeconds)
    val name = stringArrayResource(R.array.season_events)[Seasons.seasonNameIndex(around.next.kind, data.latitude ?: 0.0)]
    val dayMonth = rememberPlaceFormat("d MMM", data.utcOffsetSeconds)
    val daysText = if (days == 0) stringResource(R.string.season_today) else "$days ${stringResource(if (days == 1) R.string.day_unit_one else R.string.day_unit_other)}"
    Tile(stringResource(R.string.detail_declination), latitudeText(Seasons.declination(now), directions) { it }, daysText, modifier,
        extra = "$name\u00a0·\u00a0${dayMonth(around.next.time)}")
}

private fun amount(value: Double?): String? =
    value?.let { "%.${if (it >= 100) 0 else 1}f".format(Locale.US, it) }

@Composable
private fun AirQualityRow(name: String, value: String, tierText: String?, color: Color) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(name, localize = false, color = Color.White, fontSize = 16.sp, modifier = Modifier.weight(1f), maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
        Text(value, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
        if (tierText != null) Text(tierText, color = color, fontSize = 14.sp, maxLines = 1, softWrap = false)
    }
}

/** The air quality index in both standards, the pollutants, and the pollen where it is known. */
@Composable
internal fun AirQualityScreen(aqi: AirQualityIndex, place: String) {
    val focus = rememberScreenFocus()
    val tiers = stringArrayResource(R.array.aqi_tier_labels)
    val pollenLabels = stringArrayResource(R.array.uv_labels)
    val (primary, secondary) = aqi.displayDual()
    val european = aqi.primaryStandard == AqiStandard.EUROPEAN
    val usLabel = stringResource(R.string.air_quality_us_aqi)
    val euLabel = stringResource(R.string.air_quality_eu_aqi)
    val pollutants: List<Triple<Int, Double?, AqiTier?>> = listOf(
        Triple(R.string.air_quality_pm2_5, aqi.pm25, aqi.pm25Tier),
        Triple(R.string.air_quality_pm10, aqi.pm10, aqi.pm10Tier),
        Triple(R.string.air_quality_ozone, aqi.ozone, aqi.ozoneTier),
        Triple(R.string.air_quality_nitrogen_dioxide, aqi.nitrogenDioxide, aqi.nitrogenDioxideTier),
        Triple(R.string.air_quality_sulphur_dioxide, aqi.sulphurDioxide, aqi.sulphurDioxideTier),
        Triple(R.string.air_quality_carbon_monoxide, aqi.carbonMonoxide, aqi.carbonMonoxideTier),
        Triple(R.string.air_quality_ammonia, aqi.ammonia, null),
        Triple(R.string.air_quality_dust, aqi.dust, null),
    )
    val pollen = aqi.pollen
    val pollens = listOf(
        R.string.pollen_alder to pollen.alder, R.string.pollen_birch to pollen.birch, R.string.pollen_grass to pollen.grass,
        R.string.pollen_mugwort to pollen.mugwort, R.string.pollen_olive to pollen.olive, R.string.pollen_ragweed to pollen.ragweed,
    )
    Row(
        Modifier.fillMaxSize().padding(horizontal = 48.dp, vertical = 27.dp).focusRequester(focus).focusable(),
        horizontalArrangement = Arrangement.spacedBy(32.dp),
    ) {
        Column(Modifier.weight(0.24f).fillMaxHeight(), verticalArrangement = Arrangement.Center) {
            Text(place, localize = false, color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold, maxLines = 2)
            Text(stringResource(R.string.detail_air_quality), color = Dim, fontSize = 22.sp)
            Spacer(Modifier.height(12.dp))
            Text(primary, color = colorsFor(aqi.primaryTier).background, fontSize = 72.sp, fontWeight = FontWeight.Bold)
            Text(if (european) euLabel else usLabel, color = Color.White, fontSize = 20.sp)
            Text(tiers[aqi.primaryTier.ordinal], color = colorsFor(aqi.primaryTier).background, fontSize = 24.sp)
            Spacer(Modifier.height(12.dp))
            Text("$secondary ${if (european) usLabel else euLabel}", color = Dim, fontSize = 20.sp)
        }
        TileColumn(0.46f) {
            pollutants.forEach { (name, value, tier) ->
                amount(value)?.let {
                    AirQualityRow(stringResource(name), "$it µg/m³", tier?.let { t -> tiers[t.ordinal] }, tier?.let { t -> colorsFor(t).background } ?: Dim)
                }
            }
        }
        if (pollen.hasData) {
            TileColumn(0.3f) {
                Text(stringResource(R.string.detail_pollen), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                pollens.forEach { (name, value) ->
                    amount(value)?.let {
                        val tier = AirQualityIndex.pollenTierFor(value!!)
                        AirQualityRow(stringResource(name), it, pollenLabels[tier.ordinal], colorsFor(tier).background)
                    }
                }
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.TileColumn(weight: Float, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.weight(weight).fillMaxHeight().background(Card.copy(alpha = 0.55f), TileShape).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
        content = content,
    )
}

/** A screen of text that up and down scroll: its text can be longer than the screen. */
@Composable
internal fun ScrollScreen(content: @Composable ColumnScope.() -> Unit) {
    val focus = rememberScreenFocus()
    val scroll = rememberScrollState()
    val scope = rememberCoroutineScope()
    val stepPx = with(androidx.compose.ui.platform.LocalDensity.current) { SCROLL_STEP_DP.dp.toPx() }
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp, vertical = 27.dp)
            .focusRequester(focus)
            .onKeyEvent { event ->
                val direction = when (event.key) {
                    Key.DirectionDown -> 1
                    Key.DirectionUp -> -1
                    else -> return@onKeyEvent false
                }
                if (event.type == KeyEventType.KeyDown) scope.launch { scroll.animateScrollBy(direction * stepPx) }
                true
            }
            .focusable()
            .verticalScroll(scroll),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

/** The alerts with their full text. */
@Composable
internal fun AlertsScreen(data: WeatherData) {
    val timeFormat = rememberPlaceFormat("EEE d MMM, HH:mm", data.utcOffsetSeconds)
    val levels = mapOf(
        AlertLevel.WATCH to stringResource(R.string.alert_level_watch),
        AlertLevel.WARNING to stringResource(R.string.alert_level_warning),
        AlertLevel.EMERGENCY to stringResource(R.string.alert_level_emergency),
    )
    ScrollScreen {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(data.locationName, localize = false, color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(stringResource(R.string.about_section_alerts), color = Dim, fontSize = 24.sp)
        }
        data.alerts.sortedByDescending { it.level.ordinal }.forEach { alert ->
            Column(Modifier.fillMaxWidth().background(Card.copy(alpha = 0.55f), TileShape).padding(20.dp)) {
                Text(
                    listOfNotNull(levels[alert.level], alert.source.uppercase(Locale.ROOT), alert.time?.let(timeFormat)).joinToString(" · "),
                    color = alertColor(alert.level), fontSize = 16.sp, fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(4.dp))
                Text(alert.headline ?: alert.titleKey, localize = false, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(alert.descKey, localize = false, color = Color.White, fontSize = 20.sp, lineHeight = 28.sp)
            }
        }
        if (data.failedAlertSources.isNotEmpty()) {
            Text(stringResource(R.string.alert_sources_unavailable, data.failedAlertSources.joinToString(", ")), color = Dim, fontSize = 16.sp)
        }
    }
}
