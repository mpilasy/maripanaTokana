package orinasa.njarasoa.maripanatokana.ui.weather

import android.content.Intent
import android.graphics.Bitmap
import androidx.core.graphics.createBitmap
import android.net.Uri
import androidx.core.net.toUri
import android.graphics.Canvas
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import orinasa.njarasoa.maripanatokana.BuildConfig
import orinasa.njarasoa.maripanatokana.R
import orinasa.njarasoa.maripanatokana.data.remote.wmoDescriptionRes
import orinasa.njarasoa.maripanatokana.data.remote.wmoEmoji
import orinasa.njarasoa.maripanatokana.domain.model.AqiStandard
import orinasa.njarasoa.maripanatokana.domain.model.DailyForecast
import orinasa.njarasoa.maripanatokana.domain.model.HourlyForecast
import orinasa.njarasoa.maripanatokana.domain.model.WeatherData
import orinasa.njarasoa.maripanatokana.domain.model.WeatherSource
import orinasa.njarasoa.maripanatokana.ui.theme.CardBlue
import orinasa.njarasoa.maripanatokana.ui.theme.DarkNavyColorInt
import orinasa.njarasoa.maripanatokana.ui.theme.LocalBodyFont
import orinasa.njarasoa.maripanatokana.ui.theme.SkyBlue
import orinasa.njarasoa.maripanatokana.ui.theme.LocalBodyFontFeatures
import orinasa.njarasoa.maripanatokana.ui.theme.LocalDisplayFont
import orinasa.njarasoa.maripanatokana.ui.weather.components.AirQualityChart
import orinasa.njarasoa.maripanatokana.ui.weather.components.AirQualityDetailDialog
import orinasa.njarasoa.maripanatokana.ui.weather.components.AqiTierBadge
import orinasa.njarasoa.maripanatokana.ui.weather.components.DailyTemperatureChart
import orinasa.njarasoa.maripanatokana.ui.weather.components.DailyUvChart
import orinasa.njarasoa.maripanatokana.ui.weather.components.TemperatureChart
import orinasa.njarasoa.maripanatokana.ui.weather.components.UvTierBadge
import orinasa.njarasoa.maripanatokana.ui.weather.components.WeatherAlertBanner
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

internal enum class ForecastDisplayMode {
    Temperature,
    Wind,
    Precipitation,
    Pressure
}

internal val LocalScale = staticCompositionLocalOf { 1f }
internal fun Float.s(scale: Float) = (this * scale).sp
internal fun Int.sd(scale: Float) = (this * scale).dp

@Composable
internal fun WeatherContent(
    data: WeatherData,
    metricPrimary: Boolean,
    fontName: String,
    currentFlag: String,
    localizeDigits: (String) -> String,
    osTimeFormat: java.text.DateFormat,
    onToggleUnits: () -> Unit,
    onCycleFont: () -> Unit,
    onCycleLanguage: () -> Unit,
    onRefresh: () -> Unit,
    onLocationClicked: () -> Unit = {},
    onEditLocationClicked: () -> Unit = {},
    onResetToCurrentLocation: () -> Unit = {},
    onManageLocationsClicked: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    weatherSource: WeatherSource = WeatherSource.OPEN_METEO,
    showGpsCoordinates: Boolean = false,
    isSavedLocation: Boolean = false,
    onGoToCurrentLocation: () -> Unit = {},
    expertModeActive: Boolean = false,
    hasLocationOverride: Boolean = false,
    devOverrideLat: Double? = null,
    devOverrideLon: Double? = null,
) {
    val context = LocalContext.current
    val appLocale = LocalConfiguration.current.locales[0]
    // Bolt: Memoize SimpleDateFormat to avoid expensive recreation on recomposition
    val dateFormat = remember(appLocale) { SimpleDateFormat("EEEE, d MMMM yyyy", appLocale) }
    val screenTimeFormat = remember(appLocale) {
        SimpleDateFormat(
            if (android.text.format.DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm a",
            appLocale
        )
    }
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.US) }
    val displayFont = LocalDisplayFont.current
    val bodyFont = LocalBodyFont.current

    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val scale = when {
        screenWidthDp >= 400 -> 1f
        screenWidthDp >= 320 -> 0.8f
        else -> 0.7f
    }

    // Accordion: only one card open at a time; switching to a new location collapses everything.
    val locationKey = "${data.locationName}|${data.locationSubtext}"
    var openSectionKey by rememberSaveable { mutableStateOf<String?>("hourly_forecast") }
    var lastLocationKey by rememberSaveable { mutableStateOf(locationKey) }
    if (locationKey != lastLocationKey) {
        lastLocationKey = locationKey
        openSectionKey = null
    }
    fun toggleSection(key: String) {
        openSectionKey = if (openSectionKey == key) null else key
    }

    // Use dev override coordinates directly when set (reactive via StateFlow), else fall back to cached GPS
    val (displayLat, displayLon) = if (devOverrideLat != null && devOverrideLon != null) {
        devOverrideLat to devOverrideLon
    } else {
        remember(context) {
            val prefs = context.getSharedPreferences("widget_prefs", android.content.Context.MODE_PRIVATE)
            prefs.getFloat("lat", 0f).toDouble() to prefs.getFloat("lon", 0f).toDouble()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
    CompositionLocalProvider(LocalScale provides scale) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 24.sd(scale))
    ) {
        // Fixed header
        val headerGraphicsLayer = rememberGraphicsLayer()
        Column(
            modifier = Modifier
                .padding(top = 24.sd(scale))
                .drawWithContent {
                    headerGraphicsLayer.record {
                        this@drawWithContent.drawContent()
                    }
                    drawLayer(headerGraphicsLayer)
                },
        ) {
            Column(
                modifier = Modifier.clickable(
                    onClick = onLocationClicked,
                    role = Role.Button,
                    onClickLabel = "Toggle GPS coordinates"
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Text(
                            text = data.locationName,
                            fontSize = 32f.s(scale),
                            fontWeight = FontWeight.Bold,
                            fontFamily = displayFont,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (data.locationSubtext != null) {
                            Text(
                                text = data.locationSubtext,
                                fontSize = 13f.s(scale),
                                fontFamily = bodyFont,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                modifier = Modifier.padding(top = 0.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onManageLocationsClicked,
                        modifier = Modifier.size(32.sd(scale))
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = stringResource(R.string.cd_manage_locations),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.sd(scale))
                        )
                    }

                    if (isSavedLocation) {
                        IconButton(
                            onClick = onGoToCurrentLocation,
                            modifier = Modifier.size(32.sd(scale))
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = stringResource(R.string.cd_go_to_current_location),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                modifier = Modifier.size(18.sd(scale))
                            )
                        }
                    }

                    if (expertModeActive) {
                        IconButton(
                            onClick = onEditLocationClicked,
                            modifier = Modifier.size(32.sd(scale))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Location",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                modifier = Modifier.size(18.sd(scale))
                            )
                        }

                        if (hasLocationOverride) {
                            IconButton(
                                onClick = onResetToCurrentLocation,
                                modifier = Modifier.size(32.sd(scale))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = "Go to current location",
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    modifier = Modifier.size(18.sd(scale))
                                )
                            }
                        }
                    }
                }
                
                if (showGpsCoordinates) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val geoUri = "geo:$displayLat,$displayLon?q=$displayLat,$displayLon".toUri()
                                val mapIntent = Intent(Intent.ACTION_VIEW, geoUri)
                                context.startActivity(Intent.createChooser(mapIntent, null))
                            },
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = formatDMS(displayLat, "N", "S"),
                            fontSize = 20f.s(scale),
                            fontWeight = FontWeight.Bold,
                            fontFamily = displayFont,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        )
                        Text(
                            text = formatDMS(displayLon, "E", "W"),
                            fontSize = 20f.s(scale),
                            fontWeight = FontWeight.Bold,
                            fontFamily = displayFont,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        )
                    }
                }
            }
            Column {
                Text(
                    text = stringResource(R.string.updated_time, localizeDigits("${dateFormat.format(Date(data.timestamp))}, ${screenTimeFormat.format(Date(data.timestamp))}")),
                    fontSize = 13f.s(scale),
                    fontFamily = bodyFont,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                if (isRemoteTimezone(data.utcOffsetSeconds)) {
                    Text(
                        text = "\uD83D\uDD53 Local: ${localizeDigits(formatLocationCurrentTime(data.utcOffsetSeconds, appLocale))}",
                        fontSize = 13f.s(scale),
                        fontFamily = displayFont,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }

        // Scrollable content
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 24.sd(scale))
        ) {
            // Weather Alert Banner
            WeatherAlertBanner(
                alerts = data.alerts,
                localizeDigits = localizeDigits,
                expanded = openSectionKey == "alerts",
                onToggle = { toggleSection("alerts") },
            )

            // Hero Card
            val graphicsLayer = rememberGraphicsLayer()
            val coroutineScope = rememberCoroutineScope()
            Box {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .drawWithContent {
                            graphicsLayer.record {
                                this@drawWithContent.drawContent()
                            }
                            drawLayer(graphicsLayer)
                        },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBlue.copy(alpha = 0.6f))
                ) {
                    Column(
                        modifier = Modifier.padding(24.sd(scale))
                    ) {
                        // Top row: emoji+description (left) + temperature (right)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(
                                    text = wmoEmoji(data.weatherCode, isNight = data.timestamp !in (data.sunrise * 1000)..(data.sunset * 1000)),
                                    fontSize = 48f.s(scale),
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = stringResource(wmoDescriptionRes(data.weatherCode)),
                                    fontSize = 16f.s(scale),
                                    fontFamily = bodyFont,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                                    textAlign = TextAlign.Center,
                                )
                            }
                            val (tempPrimary, tempSecondary) = data.temperature.displayDualMixed(metricPrimary)
                            DualUnitText(
                                primary = localizeDigits(tempPrimary),
                                secondary = localizeDigits(tempSecondary),
                                primarySize = 48f.s(scale),
                                horizontalAlignment = Alignment.End,
                                onClick = onToggleUnits,
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Bottom row: feels like (left) + precipitation (right)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.feels_like),
                                    fontSize = 14f.s(scale),
                                    fontFamily = bodyFont,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                                val (flPrimary, flSecondary) = data.feelsLike.displayDual(metricPrimary)
                                DualUnitText(primary = localizeDigits(flPrimary), secondary = localizeDigits(flSecondary), onClick = onToggleUnits)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                if (data.snow != null) {
                                    val (snowP, snowS) = data.snow.displayDual(metricPrimary)
                                    DualUnitText(
                                        primary = "\u2744\uFE0F ${localizeDigits(snowP)}",
                                        secondary = localizeDigits(snowS),
                                        onClick = onToggleUnits,
                                    )
                                } else if (data.rain != null) {
                                    val (rainP, rainS) = data.rain.displayDual(metricPrimary)
                                    DualUnitText(
                                        primary = "\uD83C\uDF27\uFE0F ${localizeDigits(rainP)}",
                                        secondary = localizeDigits(rainS),
                                        onClick = onToggleUnits,
                                    )
                                } else {
                                    Text(
                                        text = stringResource(R.string.no_precip),
                                        fontSize = 14f.s(scale),
                                        fontFamily = bodyFont,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Third row: high/low (left) + wind (right)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                val (maxP, maxS) = data.tempMax.displayDual(metricPrimary)
                                val (minP, minS) = data.tempMin.displayDual(metricPrimary)
                                DualUnitText(
                                    primary = localizeDigits("\u2193$minP / \u2191$maxP"),
                                    secondary = localizeDigits("\u2193$minS / \u2191$maxS"),
                                    onClick = onToggleUnits,
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                val (windP, windS) = data.windSpeed.displayDual(metricPrimary)
                                val directions = stringArrayResource(R.array.cardinal_directions)
                                val dirIndex = ((data.windDeg % 360 + 360) % 360 * 16 / 360) % 16
                                DualUnitText(
                                    primary = localizeDigits("$windP ${directions[dirIndex]}"),
                                    secondary = localizeDigits(windS),
                                    onClick = onToggleUnits,
                                )
                            }
                        }
                    }
                    Text(
                        text = "\u00A9 Orinasa Njarasoa",
                        fontSize = 9f.s(scale),
                        lineHeight = 11f.s(scale),
                        fontFamily = bodyFont,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 7.dp),
                        textAlign = TextAlign.Center,
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.sd(scale)),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                val header = headerGraphicsLayer.toImageBitmap().asAndroidBitmap()
                                val content = graphicsLayer.toImageBitmap().asAndroidBitmap()
                                shareCardBitmap(context, combineBitmaps(header, content))
                            }
                        },
                        modifier = Modifier.size(32.dp),
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        ),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_share),
                            contentDescription = stringResource(R.string.cd_share),
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(end = 12.sd(scale))
                            .clickable { onRefresh() }
                    ) {
                        Text(
                            text = localizeDigits(stringResource(R.string.updated_time, osTimeFormat.format(Date(data.timestamp)))),
                            fontSize = 11f.s(scale),
                            fontFamily = bodyFont,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.cd_refresh),
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.sd(scale)))

            // Current Conditions (collapsible)
            CollapsibleSection(
                title = stringResource(R.string.section_current_conditions),
                headerGraphicsLayer = headerGraphicsLayer,
                expanded = openSectionKey == "current_conditions",
                onToggle = { toggleSection("current_conditions") },
            ) {
                DetailsContent(data, metricPrimary, timeFormat, localizeDigits, onToggleUnits)
            }
            Spacer(modifier = Modifier.height(24.sd(scale)))

            // Hourly Forecast
            if (data.hourlyForecast.isNotEmpty()) {
                CollapsibleSection(
                    title = stringResource(R.string.section_hourly_forecast),
                    headerGraphicsLayer = headerGraphicsLayer,
                    expanded = openSectionKey == "hourly_forecast",
                    onToggle = { toggleSection("hourly_forecast") },
                ) {
                    HourlyForecastRow(data.hourlyForecast, metricPrimary, data.dailySunrise, data.dailySunset, localizeDigits, onToggleUnits, data.utcOffsetSeconds)
                }
                Spacer(modifier = Modifier.height(24.sd(scale)))
            }

            // Weekly Forecast
            if (data.dailyForecast.isNotEmpty()) {
                CollapsibleSection(
                    title = stringResource(R.string.section_this_week),
                    headerGraphicsLayer = headerGraphicsLayer,
                    expanded = openSectionKey == "this_week",
                    onToggle = { toggleSection("this_week") },
                ) {
                    DailyForecastList(data.dailyForecast, metricPrimary, localizeDigits, onToggleUnits, data.utcOffsetSeconds)
                }
                Spacer(modifier = Modifier.height(24.sd(scale)))
            }

            // Air Quality Forecast
            if (data.hourlyAirQuality.isNotEmpty()) {
                CollapsibleSection(
                    title = stringResource(R.string.section_air_quality_forecast),
                    headerGraphicsLayer = headerGraphicsLayer,
                    expanded = openSectionKey == "air_quality_forecast",
                    onToggle = { toggleSection("air_quality_forecast") },
                ) {
                    Column {
                        data.airQuality?.let { aqi ->
                            var showAirQualityDetail by remember { mutableStateOf(false) }
                            val aqiTierLabels = stringArrayResource(R.array.aqi_tier_labels)
                            val usAqiLabel = stringResource(R.string.air_quality_us_aqi)
                            val euAqiLabel = stringResource(R.string.air_quality_eu_aqi)
                            val (aqiP, aqiS) = aqi.displayDual()
                            val (unitP, unitS) = if (aqi.primaryStandard == AqiStandard.EUROPEAN) euAqiLabel to usAqiLabel else usAqiLabel to euAqiLabel
                            DetailCard(
                                value = localizeDigits(aqiP),
                                secondaryValue = localizeDigits(aqiS),
                                subtitleContent = {
                                    AqiTierBadge(
                                        tier = aqi.primaryTier,
                                        label = aqiTierLabels[aqi.primaryTier.ordinal],
                                        modifier = Modifier.clickable { showAirQualityDetail = true },
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                unit = unitP,
                                secondaryUnit = unitS,
                            )
                            Spacer(modifier = Modifier.height(16.sd(scale)))
                            if (showAirQualityDetail) {
                                AirQualityDetailDialog(
                                    airQuality = aqi,
                                    onDismissRequest = { showAirQualityDetail = false },
                                    localizeDigits = localizeDigits,
                                )
                            }
                        }
                        AirQualityChart(
                            forecasts = data.hourlyAirQuality,
                            primaryStandard = data.airQuality?.primaryStandard ?: AqiStandard.US,
                            modifier = Modifier.fillMaxWidth().height(164.sd(scale)),
                        )
                    }
                }
            }

            // UV Forecast
            if (data.dailyForecast.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.sd(scale)))
                CollapsibleSection(
                    title = stringResource(R.string.section_uv_forecast),
                    headerGraphicsLayer = headerGraphicsLayer,
                    expanded = openSectionKey == "uv_forecast",
                    onToggle = { toggleSection("uv_forecast") },
                ) {
                    Column {
                        val uvLabels = stringArrayResource(R.array.uv_labels)
                        val todayUvMax = data.dailyForecast.firstOrNull()?.uvIndexMax ?: 0.0
                        val todayUvLabelText = when {
                            todayUvMax < 3 -> uvLabels[0]
                            todayUvMax < 6 -> uvLabels[1]
                            todayUvMax < 8 -> uvLabels[2]
                            todayUvMax < 11 -> uvLabels[3]
                            else -> uvLabels[4]
                        }
                        DetailCard(
                            value = localizeDigits("%.1f".format(Locale.US, todayUvMax)),
                            subtitleContent = { UvTierBadge(uvIndex = todayUvMax, label = todayUvLabelText) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(modifier = Modifier.height(16.sd(scale)))
                        DailyUvForecastList(data.dailyForecast, localizeDigits, data.utcOffsetSeconds)
                    }
                }
            }
        }

        // Fixed footer
        val cs = MaterialTheme.colorScheme
        val linkStyle = SpanStyle(color = cs.onSurface.copy(alpha = 0.5f), textDecoration = TextDecoration.Underline)
        val (sourceLabel, sourceUrl) = when (weatherSource) {
            WeatherSource.OPEN_METEO -> "Open-Meteo" to "https://open-meteo.com"
            WeatherSource.PIRATE_WEATHER -> "Pirate Weather" to "https://pirateweather.net"
        }
        val creditText = buildAnnotatedString {
            withStyle(SpanStyle(color = cs.onSurface.copy(alpha = 0.3f))) {
                append(stringResource(R.string.credits_weather_data))
                append(" ")
            }
            withLink(LinkAnnotation.Url(sourceUrl)) {
                withStyle(linkStyle) { append(sourceLabel) }
            }
        }
        CompositionLocalProvider(
            androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Ltr,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Font icon + name
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable(
                        onClick = onCycleFont,
                        role = Role.Button,
                        onClickLabel = stringResource(R.string.cd_change_font)
                    ),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_font),
                        contentDescription = stringResource(R.string.cd_change_font),
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = fontName.replace(" + ", "\n"),
                        fontSize = 9f.s(scale),
                        lineHeight = 11f.s(scale),
                        fontFamily = bodyFont,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                // Credits + hash centered
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = creditText, fontSize = 9f.s(scale), lineHeight = 11f.s(scale))
                    Text(
                        text = "v${BuildConfig.VERSION_NAME} \u2022 ${stringResource(R.string.hash_version, BuildConfig.GIT_HASH)}${if (BuildConfig.DEBUG) "-d" else ""} \u2022 ${BuildConfig.BUILD_TIME}",
                        fontSize = 9f.s(scale),
                        lineHeight = 11f.s(scale),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                // Language flag
                Text(
                    text = currentFlag,
                    fontSize = 16f.s(scale),
                    modifier = Modifier.clickable(
                        onClick = onCycleLanguage,
                        role = Role.Button,
                        onClickLabel = stringResource(R.string.cd_change_language)
                    ),
                )
            }
        }
    }
    } // end CompositionLocalProvider(LocalScale)
    IconButton(
        onClick = onOpenSettings,
        modifier = Modifier
            .align(Alignment.TopEnd)
            .statusBarsPadding()
            .padding(top = 16.dp, end = 8.dp)
            .size(48.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Settings,
            contentDescription = "Settings",
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.size(24.dp)
        )
    }
    } // end Box
}

@Composable
internal fun DualUnitText(
    primary: String,
    secondary: String,
    primarySize: TextUnit = 16f.s(LocalScale.current),
    color: Color = Color.Unspecified,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    onClick: (() -> Unit)? = null,
    primaryUnit: String? = null,
    secondaryUnit: String? = null,
) {
    val displayFont = LocalDisplayFont.current
    val fontFeatures = LocalBodyFontFeatures.current
    val resolvedColor = if (color != Color.Unspecified) color else MaterialTheme.colorScheme.onSurface
    Column(
        horizontalAlignment = horizontalAlignment,
        modifier = if (onClick != null) Modifier.clickable(
            onClick = onClick,
            role = Role.Button,
            onClickLabel = stringResource(R.string.cd_toggle_units)
        ) else Modifier
    ) {
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(fontSize = primarySize, fontWeight = FontWeight.Bold, fontFamily = displayFont, color = resolvedColor)) {
                    append(primary)
                }
                if (primaryUnit != null) {
                    withStyle(SpanStyle(fontSize = primarySize * 0.55f, fontWeight = FontWeight.Normal, fontFamily = displayFont, color = resolvedColor.copy(alpha = 0.7f))) {
                        append(" $primaryUnit")
                    }
                }
            },
            style = TextStyle(fontFeatureSettings = fontFeatures),
        )
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(fontSize = primarySize * 0.75f, fontWeight = FontWeight.Normal, fontFamily = displayFont, color = resolvedColor.copy(alpha = 0.55f))) {
                    append(secondary)
                }
                if (secondaryUnit != null) {
                    withStyle(SpanStyle(fontSize = primarySize * 0.55f, fontWeight = FontWeight.Normal, fontFamily = displayFont, color = resolvedColor.copy(alpha = 0.45f))) {
                        append(" $secondaryUnit")
                    }
                }
            },
            style = TextStyle(fontFeatureSettings = fontFeatures),
        )
    }
}

@Composable
internal fun CollapsibleSection(
    title: String,
    headerGraphicsLayer: GraphicsLayer,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit,
) {
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, label = "chevron")
    val bodyFont = LocalBodyFont.current
    val graphicsLayer = rememberGraphicsLayer()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val scale = LocalScale.current

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle() }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                fontSize = 20f.s(scale),
                fontWeight = FontWeight.Bold,
                fontFamily = bodyFont,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (expanded) {
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                    onClick = {
                        coroutineScope.launch {
                            val header = headerGraphicsLayer.toImageBitmap().asAndroidBitmap()
                            val content = graphicsLayer.toImageBitmap().asAndroidBitmap()
                            shareCardBitmap(context, combineBitmaps(header, content))
                        }
                    },
                    modifier = Modifier.size(32.dp),
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    ),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_share),
                        contentDescription = stringResource(R.string.cd_share),
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = if (expanded) stringResource(R.string.cd_collapse)
                                     else stringResource(R.string.cd_expand),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.rotate(rotation),
            )
        }
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically(),
        ) {
            Box(
                modifier = Modifier.drawWithContent {
                    graphicsLayer.record {
                        this@drawWithContent.drawContent()
                    }
                    drawLayer(graphicsLayer)
                }
            ) {
                content()
            }
        }
    }
}

@Composable
internal fun HourlyForecastRow(forecasts: List<HourlyForecast>, metricPrimary: Boolean, dailySunrise: List<Long>, dailySunset: List<Long>, localizeDigits: (String) -> String, onToggleUnits: () -> Unit, utcOffsetSeconds: Int = 0) {
    // Bolt: Memoize SimpleDateFormat for location time
    val locationHourFormat = remember(utcOffsetSeconds) { 
        SimpleDateFormat("HH:mm", Locale.US).apply {
            val sign = if (utcOffsetSeconds >= 0) "+" else "-"
            val absOffset = Math.abs(utcOffsetSeconds)
            val hh = absOffset / 3600
            val mm = (absOffset % 3600) / 60
            timeZone = TimeZone.getTimeZone(String.format(Locale.US, "GMT%s%02d:%02d", sign, hh, mm))
        }
    }
    val bodyFont = LocalBodyFont.current
    val fontFeatures = LocalBodyFontFeatures.current
    val scale = LocalScale.current
    var displayMode by remember { mutableStateOf(ForecastDisplayMode.Temperature) }
    val isRemote = isRemoteTimezone(utcOffsetSeconds)

    val scrollState = rememberScrollState()
    val itemWidth = 72.sd(scale)
    val itemSpacing = 12.sd(scale)
    val density = LocalDensity.current
    val totalScrollWidthPx = with(density) {
        (itemWidth * forecasts.size + itemSpacing * (forecasts.size - 1)).toPx()
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(itemSpacing),
            modifier = Modifier.horizontalScroll(scrollState).padding(vertical = 8.dp)
        ) {
            forecasts.forEach { item ->
                Card(
                    modifier = Modifier.width(itemWidth),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBlue.copy(alpha = 0.45f)),
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 12.sd(scale)),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = localizeDigits(locationHourFormat.format(Date(item.time))),
                            fontSize = 12f.s(scale),
                            fontFamily = bodyFont,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            style = TextStyle(fontFeatureSettings = fontFeatures),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (isRemote) {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = localizeDigits(formatHourInDeviceTime(item.time)),
                                    fontSize = 9f.s(scale),
                                    fontFamily = bodyFont,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                                    style = TextStyle(fontFeatureSettings = fontFeatures),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.align(Alignment.Center)
                                )
                                Text(
                                    text = "\uD83D\uDCF1",
                                    fontSize = 8f.s(scale),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                                    modifier = Modifier.align(Alignment.CenterEnd)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = run {
                                val dayIdx = dailySunrise.indexOfLast { it <= item.time }.coerceAtLeast(0)
                                val sr = dailySunrise.getOrElse(dayIdx) { 0L }
                                val ss = dailySunset.getOrElse(dayIdx) { 0L }
                                wmoEmoji(item.weatherCode, isNight = item.time !in sr..ss)
                            },
                            fontSize = 20f.s(scale),
                            modifier = Modifier.clickable(
                                role = Role.Button,
                                onClickLabel = stringResource(R.string.cd_cycle_mode),
                                onClick = {
                                    displayMode = when(displayMode) {
                                        ForecastDisplayMode.Temperature -> ForecastDisplayMode.Wind
                                        ForecastDisplayMode.Wind -> ForecastDisplayMode.Precipitation
                                        ForecastDisplayMode.Precipitation -> ForecastDisplayMode.Pressure
                                        ForecastDisplayMode.Pressure -> ForecastDisplayMode.Temperature
                                    }
                                }
                            )
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))

                        when(displayMode) {
                            ForecastDisplayMode.Temperature -> {
                                val (tempP, tempS) = item.temperature.displayDual(metricPrimary)
                                DualUnitText(
                                    primary = localizeDigits(tempP),
                                    secondary = localizeDigits(tempS),
                                    primarySize = 14f.s(scale),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    onClick = onToggleUnits,
                                )
                            }
                            ForecastDisplayMode.Wind -> {
                                val (windP, windS) = item.windSpeed.displayDual(metricPrimary)
                                val directions = stringArrayResource(R.array.cardinal_directions)
                                val dirIndex = (((item.windDirection % 360 + 360) / 22.5 + 0.5).toInt() % 16)
                                DualUnitText(
                                    primary = localizeDigits("$windP ${directions[dirIndex]}"),
                                    secondary = localizeDigits(windS),
                                    primarySize = 14f.s(scale),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    onClick = onToggleUnits,
                                )
                            }
                            ForecastDisplayMode.Precipitation -> {
                                val (rainP, rainS) = item.precipitation.displayDual(metricPrimary)
                                DualUnitText(
                                    primary = localizeDigits(rainP),
                                    secondary = localizeDigits(rainS),
                                    primarySize = 14f.s(scale),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    onClick = onToggleUnits,
                                )
                            }
                            ForecastDisplayMode.Pressure -> {
                                val (pressP, pressS) = item.pressure.displayDual(metricPrimary)
                                DualUnitText(
                                    primary = localizeDigits(pressP),
                                    secondary = localizeDigits(pressS),
                                    primarySize = 14f.s(scale),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    onClick = onToggleUnits,
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (item.precipProbability > 0) localizeDigits("%d%%".format(Locale.US, item.precipProbability)) else "",
                            fontSize = 11f.s(scale),
                            fontFamily = bodyFont,
                            color = SkyBlue,
                            style = TextStyle(fontFeatureSettings = fontFeatures),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        if (displayMode == ForecastDisplayMode.Temperature) {
            TemperatureChart(
                forecasts = forecasts,
                metricPrimary = metricPrimary,
                itemWidth = itemWidth,
                spacing = itemSpacing,
                scrollOffset = scrollState.value.toFloat(),
                totalScrollWidth = totalScrollWidthPx,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.sd(scale))
                    .padding(bottom = 8.dp)
            )
        }
    }
}

@Composable
internal fun DailyForecastList(forecasts: List<DailyForecast>, metricPrimary: Boolean, localizeDigits: (String) -> String, onToggleUnits: () -> Unit, utcOffsetSeconds: Int) {
    val appLocale = LocalConfiguration.current.locales[0]
    // Bolt: Memoize SimpleDateFormat
    // Day names must use the forecast location's timezone, not the device's — item.date is an
    // absolute instant representing local midnight AT THE LOCATION, so formatting it in the
    // device's default timezone can shift the displayed calendar day when the two zones differ
    // (e.g. a dev-mode location override far from the device's real timezone).
    val locationTimeZone = remember(utcOffsetSeconds) { buildLocationTimeZone(utcOffsetSeconds) }
    val dayFormat = remember(appLocale, locationTimeZone) { SimpleDateFormat("EEE", appLocale).apply { timeZone = locationTimeZone } }
    val dayMonthFormat = remember(appLocale, locationTimeZone) { SimpleDateFormat("d MMM", appLocale).apply { timeZone = locationTimeZone } }
    val bodyFont = LocalBodyFont.current
    val fontFeatures = LocalBodyFontFeatures.current
    val scale = LocalScale.current
    var displayMode by remember { mutableStateOf(ForecastDisplayMode.Temperature) }

    val scrollState = rememberScrollState()
    val itemWidth = 96.sd(scale)
    val itemSpacing = 12.sd(scale)
    val density = LocalDensity.current
    val totalScrollWidthPx = with(density) {
        (itemWidth * forecasts.size + itemSpacing * (forecasts.size - 1)).toPx()
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(itemSpacing),
            modifier = Modifier.horizontalScroll(scrollState).padding(vertical = 8.dp)
        ) {
        forecasts.forEach { item ->
            // Bolt: Add key to enable smart recomposition
            androidx.compose.runtime.key(item.date) {
                Card(
                    modifier = Modifier.width(itemWidth),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBlue.copy(alpha = 0.45f)),
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 12.sd(scale)),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = dayFormat.format(Date(item.date)),
                            fontSize = 12f.s(scale),
                            fontWeight = FontWeight.Medium,
                            fontFamily = bodyFont,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(
                            text = localizeDigits(dayMonthFormat.format(Date(item.date))),
                            fontSize = 9f.s(scale),
                            fontFamily = bodyFont,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = wmoEmoji(item.weatherCode),
                            fontSize = 20f.s(scale),
                            modifier = Modifier.clickable(
                                role = Role.Button,
                                onClickLabel = stringResource(R.string.cd_cycle_mode),
                                onClick = {
                                    displayMode = when (displayMode) {
                                        ForecastDisplayMode.Temperature -> ForecastDisplayMode.Wind
                                        ForecastDisplayMode.Wind -> ForecastDisplayMode.Precipitation
                                        ForecastDisplayMode.Precipitation -> ForecastDisplayMode.Temperature
                                        else -> ForecastDisplayMode.Temperature
                                    }
                                }
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        when (displayMode) {
                            ForecastDisplayMode.Temperature -> {
                                val (maxP, maxS) = item.tempMax.displayDual(metricPrimary)
                                val (minP, minS) = item.tempMin.displayDual(metricPrimary)
                                DualUnitText(
                                    primary = localizeDigits("\u2193$minP \u2191$maxP"),
                                    secondary = localizeDigits("\u2193$minS \u2191$maxS"),
                                    primarySize = 13f.s(scale),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    onClick = onToggleUnits,
                                )
                            }
                            ForecastDisplayMode.Wind -> {
                                val (windP, windS) = item.windSpeed.displayDual(metricPrimary)
                                val directions = stringArrayResource(R.array.cardinal_directions)
                                val dirIndex = (((item.windDirection % 360 + 360) / 22.5 + 0.5).toInt() % 16)
                                DualUnitText(
                                    primary = localizeDigits("$windP ${directions[dirIndex]}"),
                                    secondary = localizeDigits(windS),
                                    primarySize = 13f.s(scale),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    onClick = onToggleUnits,
                                )
                            }
                            ForecastDisplayMode.Precipitation -> {
                                val (rainP, rainS) = item.precipitation.displayDual(metricPrimary)
                                DualUnitText(
                                    primary = localizeDigits(rainP),
                                    secondary = localizeDigits(rainS),
                                    primarySize = 13f.s(scale),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    onClick = onToggleUnits,
                                )
                            }
                            else -> {}
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (item.precipProbability > 0) localizeDigits("%d%%".format(Locale.US, item.precipProbability)) else "",
                            fontSize = 11f.s(scale),
                            fontFamily = bodyFont,
                            color = SkyBlue,
                            style = TextStyle(fontFeatureSettings = fontFeatures),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
        }

        if (displayMode == ForecastDisplayMode.Temperature) {
            DailyTemperatureChart(
                forecasts = forecasts,
                metricPrimary = metricPrimary,
                itemWidth = itemWidth,
                spacing = itemSpacing,
                scrollOffset = scrollState.value.toFloat(),
                totalScrollWidth = totalScrollWidthPx,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.sd(scale))
                    .padding(bottom = 8.dp)
            )
        }
    }
}

@Composable
internal fun DailyUvForecastList(forecasts: List<DailyForecast>, localizeDigits: (String) -> String, utcOffsetSeconds: Int) {
    val appLocale = LocalConfiguration.current.locales[0]
    // See DailyForecastList's comment: day names must use the forecast location's timezone.
    val locationTimeZone = remember(utcOffsetSeconds) { buildLocationTimeZone(utcOffsetSeconds) }
    val dayFormat = remember(appLocale, locationTimeZone) { SimpleDateFormat("EEEE", appLocale).apply { timeZone = locationTimeZone } }
    val dayMonthFormat = remember(appLocale, locationTimeZone) { SimpleDateFormat("d MMM", appLocale).apply { timeZone = locationTimeZone } }
    val bodyFont = LocalBodyFont.current
    val scale = LocalScale.current
    val uvLabels = stringArrayResource(R.array.uv_labels)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (forecasts.isNotEmpty()) {
            DailyUvChart(
                forecasts = forecasts,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        CardBlue.copy(alpha = 0.3f),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 16.sd(scale), vertical = 12.sd(scale))
                    .height(74.sd(scale))
            )
        }
        forecasts.forEach { item ->
            androidx.compose.runtime.key(item.date) {
                val label = when {
                    item.uvIndexMax < 3 -> uvLabels[0]
                    item.uvIndexMax < 6 -> uvLabels[1]
                    item.uvIndexMax < 8 -> uvLabels[2]
                    item.uvIndexMax < 11 -> uvLabels[3]
                    else -> uvLabels[4]
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            CardBlue.copy(alpha = 0.3f),
                            RoundedCornerShape(12.dp),
                        )
                        .padding(horizontal = 16.sd(scale), vertical = 12.sd(scale)),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.width(100.sd(scale))) {
                        Text(
                            text = dayFormat.format(Date(item.date)),
                            fontSize = 14f.s(scale),
                            fontWeight = FontWeight.Medium,
                            fontFamily = bodyFont,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = localizeDigits(dayMonthFormat.format(Date(item.date))),
                            fontSize = 10f.s(scale),
                            fontFamily = bodyFont,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = localizeDigits("%.1f".format(Locale.US, item.uvIndexMax)),
                        fontSize = 14f.s(scale),
                        fontWeight = FontWeight.Medium,
                        fontFamily = bodyFont,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    UvTierBadge(uvIndex = item.uvIndexMax, label = label)
                }
            }
        }
    }
}

@Composable
internal fun DetailsContent(data: WeatherData, metricPrimary: Boolean, timeFormat: SimpleDateFormat, localizeDigits: (String) -> String, onToggleUnits: () -> Unit) {
    val directions = stringArrayResource(R.array.cardinal_directions)
    val uvLabels = stringArrayResource(R.array.uv_labels)
    val bodyFont = LocalBodyFont.current
    val fontFeatures = LocalBodyFontFeatures.current
    val scale = LocalScale.current
    Column {
        Spacer(modifier = Modifier.height(8.dp))

        // Temperature / Precipitation / Cloud Cover (merged full-width card)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardBlue.copy(alpha = 0.6f))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.sd(scale)),
            ) {
                // Temperature (left)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.detail_temperature),
                        fontSize = 14f.s(scale),
                        fontFamily = bodyFont,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    val (tempP, tempS) = data.temperature.displayDual(metricPrimary)
                    DualUnitText(primary = localizeDigits(tempP), secondary = localizeDigits(tempS), primarySize = 20f.s(scale), onClick = onToggleUnits)
                    val (flP, _) = data.feelsLike.displayDual(metricPrimary)
                    Text(
                        text = "${stringResource(R.string.feels_like)} ${localizeDigits(flP)}",
                        fontSize = 12f.s(scale),
                        fontFamily = bodyFont,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                }
                // Precipitation + Cloud Cover (right)
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Text(
                        text = stringResource(R.string.detail_precipitation),
                        fontSize = 14f.s(scale),
                        fontFamily = bodyFont,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    if (data.snow != null) {
                        val (snowP, snowS) = data.snow.displayDual(metricPrimary)
                        DualUnitText(primary = localizeDigits("\u2744\uFE0F $snowP"), secondary = localizeDigits(snowS), primarySize = 20f.s(scale), horizontalAlignment = Alignment.End, onClick = onToggleUnits)
                    } else if (data.rain != null) {
                        val (rainP, rainS) = data.rain.displayDual(metricPrimary)
                        DualUnitText(primary = localizeDigits("\uD83C\uDF27\uFE0F $rainP"), secondary = localizeDigits(rainS), primarySize = 20f.s(scale), horizontalAlignment = Alignment.End, onClick = onToggleUnits)
                    } else {
                        Text(
                            text = stringResource(R.string.detail_no_precip),
                            fontSize = 20f.s(scale),
                            fontWeight = FontWeight.Bold,
                            fontFamily = LocalDisplayFont.current,
                            color = MaterialTheme.colorScheme.onSurface,
                            style = TextStyle(fontFeatureSettings = fontFeatures),
                            textAlign = TextAlign.End,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${stringResource(R.string.detail_cloud_cover)}: ${localizeDigits("%d%%".format(Locale.US, data.cloudCover))}",
                        fontSize = 12f.s(scale),
                        fontFamily = bodyFont,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.sd(scale)))

        // High / Low
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardBlue.copy(alpha = 0.6f))
        ) {
            val (minP, minS) = data.tempMin.displayDual(metricPrimary)
            val (maxP, maxS) = data.tempMax.displayDual(metricPrimary)
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.sd(scale)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "\u2193",
                    fontSize = 28f.s(scale),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
                Spacer(modifier = Modifier.width(8.sd(scale)))
                DualUnitText(primary = localizeDigits(minP), secondary = localizeDigits(minS), primarySize = 20f.s(scale), onClick = onToggleUnits)
                Spacer(modifier = Modifier.weight(1f))
                DualUnitText(primary = localizeDigits(maxP), secondary = localizeDigits(maxS), primarySize = 20f.s(scale), horizontalAlignment = Alignment.End, onClick = onToggleUnits)
                Spacer(modifier = Modifier.width(8.sd(scale)))
                Text(
                    text = "\u2191",
                    fontSize = 28f.s(scale),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
            }
        }

        Spacer(modifier = Modifier.height(16.sd(scale)))

        // Wind / Wind Gust
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardBlue.copy(alpha = 0.6f))
        ) {
            val (windP, windS) = data.windSpeed.displayDual(metricPrimary)
            val dirIndex = ((data.windDeg % 360 + 360) % 360 * 16 / 360) % 16
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.sd(scale)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                    DualUnitText(primary = localizeDigits(windP), secondary = localizeDigits(windS), primarySize = 20f.s(scale), onClick = onToggleUnits)
                    Text(
                        text = localizeDigits("${directions[dirIndex]} (%d\u00B0)".format(Locale.US, data.windDeg)),
                        fontSize = 12f.s(scale),
                        fontFamily = bodyFont,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                }
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.detail_wind),
                        fontSize = 14f.s(scale),
                        fontFamily = bodyFont,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    )
                }
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    data.windGust?.let { gust ->
                        val (gustP, gustS) = gust.displayDual(metricPrimary)
                        DualUnitText(primary = localizeDigits(gustP), secondary = localizeDigits(gustS), primarySize = 20f.s(scale), horizontalAlignment = Alignment.End, onClick = onToggleUnits)
                        Text(
                            text = stringResource(R.string.detail_wind_gust),
                            fontSize = 12f.s(scale),
                            fontFamily = bodyFont,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            textAlign = TextAlign.End,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.sd(scale)))

        // Sunrise / Sunset
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardBlue.copy(alpha = 0.6f))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.sd(scale)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                    Text(
                        text = localizeDigits(timeFormat.format(Date(data.sunrise * 1000))),
                        fontSize = 20f.s(scale),
                        fontWeight = FontWeight.Bold,
                        fontFamily = LocalDisplayFont.current,
                        color = MaterialTheme.colorScheme.onSurface,
                        style = TextStyle(fontFeatureSettings = fontFeatures),
                    )
                    Text(
                        text = stringResource(R.string.detail_sunrise),
                        fontSize = 12f.s(scale),
                        fontFamily = bodyFont,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                }
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "\u2600\uFE0F",
                        fontSize = 24f.s(scale),
                    )
                }
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Text(
                        text = localizeDigits(timeFormat.format(Date(data.sunset * 1000))),
                        fontSize = 20f.s(scale),
                        fontWeight = FontWeight.Bold,
                        fontFamily = LocalDisplayFont.current,
                        color = MaterialTheme.colorScheme.onSurface,
                        style = TextStyle(fontFeatureSettings = fontFeatures),
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = stringResource(R.string.detail_sunset),
                        fontSize = 12f.s(scale),
                        fontFamily = bodyFont,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.sd(scale)))

        // Pressure / Humidity
        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(16.sd(scale))
        ) {
            val (pressP, pressS) = data.pressure.displayDual(metricPrimary)
            DetailCard(
                title = stringResource(R.string.detail_pressure),
                value = localizeDigits(pressP),
                secondaryValue = localizeDigits(pressS),
                modifier = Modifier.weight(1f).fillMaxHeight(),
                onToggleUnits = onToggleUnits,
            )
            val (dewP, dewS) = data.dewPoint.displayDual(metricPrimary)
            Card(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBlue.copy(alpha = 0.6f))
            ) {
                Column(modifier = Modifier.padding(16.sd(scale))) {
                    Text(
                        text = stringResource(R.string.detail_humidity),
                        fontSize = 14f.s(scale),
                        fontFamily = bodyFont,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = localizeDigits("%d%%".format(Locale.US, data.humidity)),
                        fontSize = 20f.s(scale),
                        fontWeight = FontWeight.Bold,
                        fontFamily = LocalDisplayFont.current,
                        color = MaterialTheme.colorScheme.onSurface,
                        style = TextStyle(fontFeatureSettings = fontFeatures),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    val displayFont = LocalDisplayFont.current
                    val onSurface = MaterialTheme.colorScheme.onSurface
                    Text(
                        text = stringResource(R.string.detail_dewpoint),
                        fontSize = 12f.s(scale),
                        fontFamily = bodyFont,
                        color = onSurface.copy(alpha = 0.5f),
                    )
                    val dewText = buildAnnotatedString {
                        withStyle(SpanStyle(fontSize = 13f.s(scale), fontWeight = FontWeight.Bold, fontFamily = displayFont, color = onSurface)) {
                            append(localizeDigits(dewP))
                        }
                        withStyle(SpanStyle(fontSize = 12f.s(scale), fontFamily = displayFont, color = onSurface.copy(alpha = 0.55f))) {
                            append(" ${localizeDigits(dewS)}")
                        }
                    }
                    Text(
                        text = dewText,
                        modifier = Modifier.clickable(onClick = onToggleUnits),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.sd(scale)))

        // UV Index (current reading) / Visibility — Air Quality now lives on its own forecast card
        val uvLabelText = when {
            data.uvIndex < 3 -> uvLabels[0]
            data.uvIndex < 6 -> uvLabels[1]
            data.uvIndex < 8 -> uvLabels[2]
            data.uvIndex < 11 -> uvLabels[3]
            else -> uvLabels[4]
        }
        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(16.sd(scale))
        ) {
            DetailCard(
                title = stringResource(R.string.detail_uv_index),
                value = localizeDigits("%.1f".format(Locale.US, data.uvIndex)),
                subtitleContent = { UvTierBadge(uvIndex = data.uvIndex, label = uvLabelText) },
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
            DetailCard(
                title = stringResource(R.string.detail_visibility),
                value = localizeDigits(if (metricPrimary) stringResource(R.string.visibility_km).format(Locale.US, data.visibility / 1000.0)
                        else stringResource(R.string.visibility_mi).format(Locale.US, data.visibility / 1609.34)),
                secondaryValue = localizeDigits(if (metricPrimary) stringResource(R.string.visibility_mi).format(Locale.US, data.visibility / 1609.34)
                                 else stringResource(R.string.visibility_km).format(Locale.US, data.visibility / 1000.0)),
                modifier = Modifier.weight(1f).fillMaxHeight(),
                onToggleUnits = onToggleUnits,
            )
        }
    }
}

@Composable
internal fun DetailCard(
    value: String,
    modifier: Modifier = Modifier,
    title: String? = null,
    secondaryValue: String? = null,
    subtitle: String? = null,
    subtitleContent: (@Composable () -> Unit)? = null,
    onToggleUnits: (() -> Unit)? = null,
    unit: String? = null,
    secondaryUnit: String? = null,
) {
    val bodyFont = LocalBodyFont.current
    val fontFeatures = LocalBodyFontFeatures.current
    val scale = LocalScale.current
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBlue.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier.padding(16.sd(scale))
        ) {
            if (title != null) {
                Text(
                    text = title,
                    fontSize = 14f.s(scale),
                    fontFamily = bodyFont,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            if (secondaryValue != null) {
                DualUnitText(
                    primary = value,
                    secondary = secondaryValue,
                    primarySize = 20f.s(scale),
                    onClick = onToggleUnits,
                    primaryUnit = unit,
                    secondaryUnit = secondaryUnit,
                )
            } else {
                Text(
                    text = value,
                    fontSize = 20f.s(scale),
                    fontWeight = FontWeight.Bold,
                    fontFamily = LocalDisplayFont.current,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = TextStyle(fontFeatureSettings = fontFeatures),
                )
            }
            if (subtitleContent != null) {
                Spacer(modifier = Modifier.height(4.dp))
                subtitleContent()
            }
            subtitle?.let {
                Text(
                    text = it,
                    fontSize = 12f.s(scale),
                    fontFamily = bodyFont,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

internal suspend fun combineBitmaps(header: Bitmap, content: Bitmap): Bitmap = withContext(Dispatchers.Default) {
    val h = header.copy(Bitmap.Config.ARGB_8888, false)
    val c = content.copy(Bitmap.Config.ARGB_8888, false)
    val padding = 24
    val width = maxOf(h.width, c.width) + padding * 2
    val height = h.height + padding + c.height + padding * 2
    val result = createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(result)
    canvas.drawColor(DarkNavyColorInt)
    canvas.drawBitmap(h, padding.toFloat(), padding.toFloat(), null)
    canvas.drawBitmap(c, padding.toFloat(), (h.height + padding * 2).toFloat(), null)
    result
}

internal suspend fun shareCardBitmap(context: android.content.Context, bitmap: Bitmap) {
    val uri = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "shared_images").also { it.mkdirs() }
        val file = File(dir, "weather.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }

        // PNG's "eXIf" chunk carries EXIF just like JPEG does; ExifInterface writes it directly.
        val exifDateTime = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US).format(Date())
        val exif = ExifInterface(file.absolutePath)
        exif.setAttribute(ExifInterface.TAG_DATETIME, exifDateTime)
        exif.setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, exifDateTime)
        exif.setAttribute(ExifInterface.TAG_DATETIME_DIGITIZED, exifDateTime)
        exif.saveAttributes()

        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, null))
}

/** Check if the location's timezone differs from the device's timezone. */
private fun isRemoteTimezone(utcOffsetSeconds: Int): Boolean {
    val deviceOffsetMs = TimeZone.getDefault().getOffset(System.currentTimeMillis())
    return deviceOffsetMs != utcOffsetSeconds * 1000
}

/** Get the current time at a location given its UTC offset. */
private fun formatLocationCurrentTime(utcOffsetSeconds: Int, locale: Locale): String {
    val utcNow = System.currentTimeMillis()
    val locationMs = utcNow + utcOffsetSeconds * 1000L
    
    // Check if the date is different
    val deviceDate = SimpleDateFormat("yyyyMMdd", Locale.US).apply { timeZone = TimeZone.getDefault() }.format(Date(utcNow))
    val locationDate = SimpleDateFormat("yyyyMMdd", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date(locationMs))

    return if (deviceDate != locationDate) {
        // Show date and time, e.g., "Mon, 14:30"
        val format = SimpleDateFormat("EEE, HH:mm", locale)
        format.timeZone = TimeZone.getTimeZone("UTC")
        format.format(Date(locationMs))
    } else {
        // Show only time
        val format = SimpleDateFormat("HH:mm", Locale.US)
        format.timeZone = TimeZone.getTimeZone("UTC")
        format.format(Date(locationMs))
    }
}

/**
 * Convert an epoch (UTC millis) to the device's actual local time.
 */
private fun formatHourInDeviceTime(epochMillis: Long): String {
    val format = SimpleDateFormat("HH:mm", Locale.US)
    // Default timezone for SimpleDateFormat is the device's local timezone
    return format.format(Date(epochMillis))
}

private fun buildLocationTimeZone(utcOffsetSeconds: Int): TimeZone {
    val sign = if (utcOffsetSeconds >= 0) "+" else "-"
    val absOffset = Math.abs(utcOffsetSeconds)
    val hh = absOffset / 3600
    val mm = (absOffset % 3600) / 60
    return TimeZone.getTimeZone(String.format(Locale.US, "GMT%s%02d:%02d", sign, hh, mm))
}

private fun formatDMS(value: Double, positive: String, negative: String): String {
    val direction = if (value >= 0) positive else negative
    val absolute = Math.abs(value)
    val degrees = absolute.toInt()
    val minutesTotal = (absolute - degrees) * 60
    val minutes = minutesTotal.toInt()
    val seconds = ((minutesTotal - minutes) * 60).toInt()
    return "%d\u00B0%02d'%02d\"%s".format(Locale.US, degrees, minutes, seconds, direction)
}
