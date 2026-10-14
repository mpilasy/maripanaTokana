package orinasa.njarasoa.maripanatokana.ui.weather

import android.content.Intent
import orinasa.njarasoa.maripanatokana.domain.model.FetchError
import android.graphics.Bitmap
import androidx.core.graphics.createBitmap
import android.net.Uri
import android.os.Build
import androidx.core.net.toUri
import android.graphics.Canvas
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
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
import orinasa.njarasoa.maripanatokana.domain.model.ActivityIndices
import orinasa.njarasoa.maripanatokana.ui.weather.components.ActivitiesCard
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.layout.onSizeChanged
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
import androidx.compose.ui.text.style.TextDirection
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
import orinasa.njarasoa.maripanatokana.data.location.PlusCode
import orinasa.njarasoa.maripanatokana.data.remote.wmoDescriptionRes
import orinasa.njarasoa.maripanatokana.data.remote.wmoEmoji
import orinasa.njarasoa.maripanatokana.domain.model.AqiStandard
import orinasa.njarasoa.maripanatokana.domain.model.DailyForecast
import orinasa.njarasoa.maripanatokana.domain.model.HourlyForecast
import orinasa.njarasoa.maripanatokana.domain.model.WeatherData
import orinasa.njarasoa.maripanatokana.domain.model.WeatherSource
import orinasa.njarasoa.maripanatokana.ui.theme.DarkNavyColorInt
import orinasa.njarasoa.maripanatokana.ui.theme.LocalBodyFont
import orinasa.njarasoa.maripanatokana.ui.theme.SkyBlue
import orinasa.njarasoa.maripanatokana.ui.theme.Hairline
import orinasa.njarasoa.maripanatokana.ui.theme.HeroGradientBottom
import orinasa.njarasoa.maripanatokana.ui.theme.HeroGradientTop
import orinasa.njarasoa.maripanatokana.ui.theme.SectionSurface
import orinasa.njarasoa.maripanatokana.ui.theme.TileSurface
import orinasa.njarasoa.maripanatokana.ui.theme.TileSurfaceHighlight
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
    onManageLocationsClicked: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenAbout: () -> Unit = {},
    weatherSource: WeatherSource = WeatherSource.OPEN_METEO,
    showGpsCoordinates: Boolean = false,
    isSavedLocation: Boolean = false,
    onGoToCurrentLocation: () -> Unit = {},
    onToggleFavorite: () -> Unit = {},
    onSwipeToNextLocation: () -> Unit = {},
    onSwipeToPreviousLocation: () -> Unit = {},
    canSwipeToNext: Boolean = false,
    canSwipeToPrevious: Boolean = false,
    locationCount: Int = 1,
    locationIndex: Int = 0,
    hasLocationOverride: Boolean = false,
    advancedOverrideLat: Double? = null,
    advancedOverrideLon: Double? = null,
    refreshFailed: Boolean = false,
    refreshError: FetchError? = null,
    advancedMode: Boolean = false,
    scrollState: ScrollState = rememberScrollState(),
) {
    val context = LocalContext.current
    val appLocale = LocalConfiguration.current.locales[0]
    // Bolt: Memoize SimpleDateFormat to avoid expensive recreation on recomposition
    val dateFormat = remember(appLocale) { SimpleDateFormat("EEE d MMM", appLocale) }
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

    // Coordinates of the location being shown; dev override wins, cached GPS only for old cached data
    val (displayLat, displayLon) = if (advancedOverrideLat != null && advancedOverrideLon != null) {
        advancedOverrideLat to advancedOverrideLon
    } else if (data.latitude != null && data.longitude != null) {
        data.latitude to data.longitude
    } else {
        remember(context) {
            val prefs = context.getSharedPreferences("private_prefs", android.content.Context.MODE_PRIVATE)
            prefs.getFloat("lat", 0f).toDouble() to prefs.getFloat("lon", 0f).toDouble()
        }
    }

    // Swipe-to-cycle-locations: draggable is nested-scroll-aware, so inner horizontal
    // LazyRows (hourly/daily forecast) still consume drags themselves; only the leftover
    // (e.g. dragging at a row's scroll edge, or dragging outside any row) reaches here.
    val density = LocalDensity.current
    var horizontalDragAccum by remember { mutableStateOf(0f) }
    val swipeThresholdPx = with(density) { 80.dp.toPx() }
    val horizontalDragState = rememberDraggableState { delta -> horizontalDragAccum += delta }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .draggable(
                orientation = Orientation.Horizontal,
                state = horizontalDragState,
                onDragStarted = { horizontalDragAccum = 0f },
                onDragStopped = {
                    if (horizontalDragAccum <= -swipeThresholdPx) onSwipeToNextLocation()
                    else if (horizontalDragAccum >= swipeThresholdPx) onSwipeToPreviousLocation()
                    horizontalDragAccum = 0f
                },
            )
    ) {
    CompositionLocalProvider(LocalScale provides scale) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 24.sd(scale))
    ) {
        // Fixed header: full-width location block (captured into share images) with the action
        // buttons overlaid on the name line (not captured); only the name shrinks to make room.
        val headerGraphicsLayer = rememberGraphicsLayer()
        // Local script by default; resets for each location
        var showRoman by remember(data.latitude, data.longitude, data.locationName) { mutableStateOf(false) }
        val locationName = if (showRoman) data.romanName ?: data.locationName else data.locationName
        val locationSubtext = if (showRoman && data.romanName != null) data.romanSubtext else data.locationSubtext
        var headerActionsWidth by remember { mutableIntStateOf(0) }
        val density = LocalDensity.current
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.sd(scale)),
        ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
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
                    modifier = Modifier.padding(end = with(density) { headerActionsWidth.toDp() }),
                ) {
                    Text(
                        text = locationName,
                        fontSize = 30f.s(scale),
                        lineHeight = 36f.s(scale),
                        fontWeight = FontWeight.Bold,
                        fontFamily = displayFont,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    // Discreet switch between the local script and Latin letters
                    if (data.romanName != null) {
                        Icon(
                            painter = painterResource(R.drawable.ic_translate),
                            contentDescription = stringResource(R.string.cd_roman_name),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = if (showRoman) 0.85f else 0.45f),
                            modifier = Modifier
                                .padding(start = 6.dp)
                                .clip(CircleShape)
                                .clickable(role = Role.Button) { showRoman = !showRoman }
                                .padding(4.dp)
                                .size(18.dp),
                        )
                    }
                }
                val isRemote = isRemoteTimezone(data.utcOffsetSeconds)
                if (locationSubtext != null || isRemote) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (locationSubtext != null) {
                            Text(
                                text = locationSubtext,
                                fontSize = 14f.s(scale),
                                fontFamily = bodyFont,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        if (isRemote) {
                            Text(
                                text = "\uD83D\uDD53 ${localizeDigits(formatLocationCurrentTime(data.utcOffsetSeconds, appLocale))}",
                                fontSize = 12f.s(scale),
                                fontFamily = displayFont,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                                maxLines = 1,
                                modifier = Modifier
                                    .background(TileSurface, RoundedCornerShape(50))
                                    .padding(horizontal = 8.dp, vertical = 2.dp),
                            )
                        }
                    }
                }

                if (showGpsCoordinates) {
                    Spacer(modifier = Modifier.height(8.dp))
                    // Flows so the Plus Code drops to its own line on narrow screens
                    FlowRow(
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
                            style = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr), // keep 45°00'21"N intact in RTL
                            fontSize = 20f.s(scale),
                            fontWeight = FontWeight.Bold,
                            fontFamily = displayFont,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                            modifier = Modifier.alignByBaseline(),
                        )
                        Text(
                            text = formatDMS(displayLon, "E", "W"),
                            style = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr), // keep 45°00'21"N intact in RTL
                            fontSize = 20f.s(scale),
                            fontWeight = FontWeight.Bold,
                            fontFamily = displayFont,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                            modifier = Modifier.alignByBaseline(),
                        )
                        // Plus Code of the same point, smaller
                        Text(
                            text = remember(displayLat, displayLon) { PlusCode.encode(displayLat, displayLon) },
                            style = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr),
                            fontSize = 14f.s(scale),
                            fontFamily = displayFont,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.alignByBaseline(),
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.sd(scale)))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false).clickable(
                        onClick = onRefresh,
                        role = Role.Button,
                        onClickLabel = stringResource(R.string.cd_refresh),
                    ),
                ) {
                    Text(
                        text = stringResource(R.string.updated_time, localizeDigits(
                            if (android.text.format.DateUtils.isToday(data.timestamp)) screenTimeFormat.format(Date(data.timestamp))
                            else "${dateFormat.format(Date(data.timestamp))}, ${screenTimeFormat.format(Date(data.timestamp))}"
                        )),
                        fontSize = 13f.s(scale),
                        fontFamily = bodyFont,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(13.sd(scale)),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                    )
                }
            }
            if (refreshFailed) {
                Text(
                    text = stringResource(R.string.refresh_failed, localizeDigits(screenTimeFormat.format(Date(data.timestamp)))) +
                        (refreshError?.let { " \u00B7 " + stringResource(it.messageResId) } ?: ""),
                    fontSize = 13f.s(scale),
                    fontFamily = bodyFont,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.clickable { onRefresh() },
                )
            }
        }

            // Header actions
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .onSizeChanged { headerActionsWidth = it.width },
            ) {
                HeaderAction(
                    imageVector = Icons.Default.Search,
                    contentDescription = stringResource(R.string.cd_manage_locations),
                    onClick = onManageLocationsClicked,
                )
                if (isSavedLocation || hasLocationOverride) {
                    HeaderAction(
                        imageVector = if (isSavedLocation) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = stringResource(
                            if (isSavedLocation) R.string.cd_remove_favorite else R.string.cd_add_favorite
                        ),
                        onClick = onToggleFavorite,
                    )
                    HeaderAction(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = stringResource(R.string.cd_go_to_current_location),
                        onClick = onGoToCurrentLocation,
                    )
                }
                HeaderAction(
                    imageVector = Icons.Default.Settings,
                    contentDescription = stringResource(R.string.settings_title),
                    onClick = onOpenSettings,
                )
            }
        }

        // Saved-location pager: \u2039 \u2022 \u25CF \u2022 \u203A
        if (locationCount > 1) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.sd(scale)),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LocationPagerArrow(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = stringResource(R.string.cd_previous_location),
                    enabled = canSwipeToPrevious,
                    onClick = onSwipeToPreviousLocation,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(locationCount) { i ->
                        val active = i == locationIndex
                        Box(
                            modifier = Modifier
                                .size(if (active) 8.dp else 6.dp)
                                .background(
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = if (active) 0.9f else 0.3f),
                                    CircleShape,
                                )
                                .align(Alignment.CenterVertically),
                        )
                    }
                }
                LocationPagerArrow(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = stringResource(R.string.cd_next_location),
                    enabled = canSwipeToNext,
                    onClick = onSwipeToNextLocation,
                )
            }
        }

        // Scrollable content
        Column(
            modifier = Modifier
                .weight(1f)
                // Soft fade where content scrolls under the fixed header
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                .drawWithContent {
                    drawContent()
                    drawRect(
                        brush = Brush.verticalGradient(
                            0f to Color.Transparent,
                            1f to Color.Black,
                            endY = 16.dp.toPx(),
                        ),
                        blendMode = BlendMode.DstIn,
                    )
                }
                .verticalScroll(scrollState)
                .padding(vertical = 16.sd(scale))
        ) {
            // Weather Alert Banner
            WeatherAlertBanner(
                alerts = data.alerts,
                localizeDigits = localizeDigits,
                expanded = openSectionKey == "alerts",
                onToggle = { toggleSection("alerts") },
            )
            if (advancedMode && data.failedAlertSources.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.alert_sources_unavailable, data.failedAlertSources.joinToString(", ")),
                    fontSize = 11f.s(scale),
                    fontFamily = bodyFont,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                )
            }

            // Hero Card
            val graphicsLayer = rememberGraphicsLayer()
            val coroutineScope = rememberCoroutineScope()
            val heroShape = RoundedCornerShape(28.dp)
            Box {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .drawWithContent {
                            graphicsLayer.record {
                                this@drawWithContent.drawContent()
                            }
                            drawContent()
                        }
                        .background(Brush.verticalGradient(listOf(HeroGradientTop, HeroGradientBottom)), heroShape)
                        .border(1.dp, Hairline, heroShape)
                        .padding(horizontal = 24.sd(scale))
                        .padding(top = 20.sd(scale), bottom = 8.sd(scale)),
                ) {
                    // Condition line
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = wmoEmoji(data.weatherCode, isNight = data.timestamp !in (data.sunrise * 1000)..(data.sunset * 1000)),
                            fontSize = 22f.s(scale),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(wmoDescriptionRes(data.weatherCode)),
                            fontSize = 16f.s(scale),
                            fontFamily = bodyFont,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    // Big temperature (left) + precipitation (right)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.sd(scale)),
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        val (tempPrimary, tempSecondary) = data.temperature.displayDualMixed(metricPrimary)
                        // Temperature keeps its natural width; precipitation takes (and wraps within) the rest.
                        Box {
                            DualUnitText(
                                primary = localizeDigits(tempPrimary),
                                secondary = localizeDigits(tempSecondary),
                                primarySize = 64f.s(scale),
                                secondaryScale = 0.4f,
                                onClick = onToggleUnits,
                            )
                        }
                        Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1f).padding(start = 8.dp, bottom = 4.sd(scale))) {
                            if (data.snow != null) {
                                val (snowP, snowS) = data.snow.displayDual(metricPrimary)
                                DualUnitText(
                                    primary = "❄️ ${localizeDigits(snowP)}",
                                    secondary = localizeDigits(snowS),
                                    horizontalAlignment = Alignment.End,
                                    onClick = onToggleUnits,
                                )
                            } else if (data.rain != null) {
                                val (rainP, rainS) = data.rain.displayDual(metricPrimary)
                                DualUnitText(
                                    primary = "🌧️ ${localizeDigits(rainP)}",
                                    secondary = localizeDigits(rainS),
                                    horizontalAlignment = Alignment.End,
                                    onClick = onToggleUnits,
                                )
                            } else {
                                Text(
                                    text = stringResource(R.string.no_precip),
                                    fontSize = 13f.s(scale),
                                    textAlign = TextAlign.End,
                                    fontFamily = bodyFont,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    modifier = Modifier
                                        .background(TileSurface, RoundedCornerShape(50))
                                        .padding(horizontal = 10.dp, vertical = 4.dp),
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.sd(scale)))

                    // Stat strip: feels like · low/high · wind
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min)
                            .background(TileSurface, RoundedCornerShape(16.dp))
                            .padding(vertical = 12.sd(scale)),
                    ) {
                        HeroStat(label = stringResource(R.string.feels_like), modifier = Modifier.weight(1f)) {
                            val (flPrimary, flSecondary) = data.feelsLike.displayDual(metricPrimary)
                            DualUnitText(
                                primary = localizeDigits(flPrimary),
                                secondary = localizeDigits(flSecondary),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                onClick = onToggleUnits,
                            )
                        }
                        HeroStatDivider()
                        HeroStat(label = null, modifier = Modifier.weight(1.2f)) {
                            val (maxP, maxS) = data.tempMax.displayDual(metricPrimary)
                            val (minP, minS) = data.tempMin.displayDual(metricPrimary)
                            DualUnitText(
                                primary = localizeDigits("\u2193$minP \u2191$maxP"),
                                secondary = localizeDigits("\u2193$minS \u2191$maxS"),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                onClick = onToggleUnits,
                            )
                        }
                        HeroStatDivider()
                        val directions = stringArrayResource(R.array.cardinal_directions)
                        val dirLabel = data.windDeg?.let { directions[((it % 360 + 360) % 360 * 16 / 360) % 16] } ?: "--"
                        HeroStat(label = "${stringResource(R.string.detail_wind)} · $dirLabel", modifier = Modifier.weight(1f)) {
                            val (windP, windS) = data.windSpeed.displayDual(metricPrimary)
                            DualUnitText(
                                primary = localizeDigits(windP),
                                secondary = localizeDigits(windS),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                onClick = onToggleUnits,
                            )
                        }
                    }

                    Text(
                        text = "© Orinasa Njarasoa",
                        fontSize = 9f.s(scale),
                        lineHeight = 11f.s(scale),
                        fontFamily = bodyFont,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        textAlign = TextAlign.Center,
                    )
                }
                IconButton(
                    onClick = {
                        coroutineScope.launch {
                            val header = headerGraphicsLayer.toImageBitmap().asAndroidBitmap()
                            val content = graphicsLayer.toImageBitmap().asAndroidBitmap()
                            shareCardBitmap(context, combineBitmaps(header, content))
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(36.dp),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = TileSurface,
                        contentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                    ),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_share),
                        contentDescription = stringResource(R.string.cd_share),
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.sd(scale)))

            // Current Conditions (collapsible)
            CollapsibleSection(
                title = stringResource(R.string.section_current_conditions),
                headerGraphicsLayer = headerGraphicsLayer,
                expanded = openSectionKey == "current_conditions",
                onToggle = { toggleSection("current_conditions") },
            ) {
                DetailsContent(data, metricPrimary, timeFormat, localizeDigits, onToggleUnits)
            }
            Spacer(modifier = Modifier.height(12.sd(scale)))

            // Minutely Nowcast (collapsible, Open-Meteo source only)
            if (weatherSource == WeatherSource.OPEN_METEO && data.minutelyForecast.isNotEmpty()) {
                CollapsibleSection(
                    title = stringResource(R.string.section_nowcast),
                    headerGraphicsLayer = headerGraphicsLayer,
                    expanded = openSectionKey == "nowcast",
                    onToggle = { toggleSection("nowcast") },
                ) {
                    orinasa.njarasoa.maripanatokana.ui.weather.components.NowcastCard(
                        items = data.minutelyForecast,
                        isMetricPrimary = metricPrimary
                    )
                }
                Spacer(modifier = Modifier.height(12.sd(scale)))
            }

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
                Spacer(modifier = Modifier.height(12.sd(scale)))
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
                Spacer(modifier = Modifier.height(12.sd(scale)))
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
                                trailingContent = {
                                    AqiTierBadge(
                                        tier = aqi.primaryTier,
                                        label = aqiTierLabels[aqi.primaryTier.ordinal],
                                        modifier = Modifier.clickable { showAirQualityDetail = true },
                                        fontSize = 14.sp,
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
                            localizeDigits = localizeDigits,
                            modifier = Modifier.fillMaxWidth().height(164.sd(scale)),
                        )
                    }
                }
            }

            // UV Forecast
            if (data.dailyForecast.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.sd(scale)))
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
                            trailingContent = { UvTierBadge(uvIndex = todayUvMax, label = todayUvLabelText, fontSize = 14.sp) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(modifier = Modifier.height(16.sd(scale)))
                        DailyUvForecastList(data.dailyForecast, localizeDigits, data.utcOffsetSeconds)
                    }
                }
            }

            // Activities Index
            Spacer(modifier = Modifier.height(12.sd(scale)))
            CollapsibleSection(
                title = stringResource(R.string.section_activities),
                headerGraphicsLayer = headerGraphicsLayer,
                expanded = openSectionKey == "activities",
                onToggle = { toggleSection("activities") },
            ) {
                val activityIndices = remember(data) { ActivityIndices.fromWeatherData(data) }
                ActivitiesCard(indices = activityIndices)
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
                    .padding(top = 4.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Font icon + name
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(TileSurface)
                        .clickable(
                            onClick = onCycleFont,
                            role = Role.Button,
                            onClickLabel = stringResource(R.string.cd_change_font)
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_font),
                        contentDescription = stringResource(R.string.cd_change_font),
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = fontName.substringBefore(" + "),
                        fontSize = 11f.s(scale),
                        fontFamily = bodyFont,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        maxLines = 1,
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                // Credits + hash centered; tapping opens About (the source link handles its own taps)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable(
                        onClick = onOpenAbout,
                        role = Role.Button,
                        onClickLabel = stringResource(R.string.about_title),
                    ),
                ) {
                    Text(text = creditText, fontSize = 10f.s(scale), lineHeight = 13f.s(scale))
                    Text(
                        text = "v${BuildConfig.VERSION_NAME} \u2022 ${stringResource(R.string.hash_version, BuildConfig.GIT_HASH)}${if (BuildConfig.DEBUG) "-d" else ""} \u2022 ${BuildConfig.BUILD_TIME}",
                        fontSize = 9f.s(scale),
                        lineHeight = 12f.s(scale),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                // Language flag
                Text(
                    text = currentFlag,
                    fontSize = 16f.s(scale),
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(TileSurface)
                        .clickable(
                            onClick = onCycleLanguage,
                            role = Role.Button,
                            onClickLabel = stringResource(R.string.cd_change_language)
                        )
                        .padding(horizontal = 10.dp, vertical = 3.dp),
                )
            }
        }
    }
    } // end CompositionLocalProvider(LocalScale)
    } // end Box
}

@Composable
private fun HeaderAction(
    imageVector: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick, modifier = Modifier.size(40.dp)) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun LocationPagerArrow(
    imageVector: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(32.dp),
        colors = IconButtonDefaults.iconButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
        ),
    ) {
        Icon(imageVector = imageVector, contentDescription = contentDescription, modifier = Modifier.size(20.dp))
    }
}

/** Shared glass surface for section cards and tiles. */
internal fun Modifier.glass(
    color: Color = TileSurface,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(16.dp),
): Modifier = this
    .background(color, shape)
    .border(1.dp, Hairline, shape)

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
    secondaryScale: Float = 0.75f,
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
                // LTR isolate so RTL locales (Arabic) keep "16.1°C" in number-then-unit order.
                append('\u2066')
                withStyle(SpanStyle(fontSize = primarySize, fontWeight = FontWeight.Bold, fontFamily = displayFont, color = resolvedColor)) {
                    append(primary)
                }
                if (primaryUnit != null) {
                    withStyle(SpanStyle(fontSize = primarySize * 0.55f, fontWeight = FontWeight.Normal, fontFamily = displayFont, color = resolvedColor.copy(alpha = 0.7f))) {
                        append(" $primaryUnit")
                    }
                }
                append('\u2069')
            },
            style = TextStyle(fontFeatureSettings = fontFeatures),
        )
        Text(
            text = buildAnnotatedString {
                append('\u2066')
                withStyle(SpanStyle(fontSize = primarySize * secondaryScale, fontWeight = FontWeight.Normal, fontFamily = displayFont, color = resolvedColor.copy(alpha = 0.55f))) {
                    append(secondary)
                }
                if (secondaryUnit != null) {
                    withStyle(SpanStyle(fontSize = primarySize * 0.55f, fontWeight = FontWeight.Normal, fontFamily = displayFont, color = resolvedColor.copy(alpha = 0.45f))) {
                        append(" $secondaryUnit")
                    }
                }
                append('\u2069')
            },
            style = TextStyle(fontFeatureSettings = fontFeatures),
        )
    }
}

@Composable
private fun HeroStat(
    label: String?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier.fillMaxHeight().padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (label != null) {
            Text(
                text = label,
                fontSize = 12f.s(LocalScale.current),
                fontFamily = LocalBodyFont.current,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(2.dp))
        }
        content()
    }
}

@Composable
private fun HeroStatDivider() {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .padding(vertical = 4.dp)
            .width(1.dp)
            .background(Hairline)
    )
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
    // Scroll a newly opened section into view (skip the initial state so launch doesn't jump).
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    var wasExpanded by remember { mutableStateOf(expanded) }
    LaunchedEffect(expanded) {
        if (expanded && !wasExpanded) {
            delay(250)
            bringIntoViewRequester.bringIntoView()
        }
        wasExpanded = expanded
    }
    val bodyFont = LocalBodyFont.current
    val graphicsLayer = rememberGraphicsLayer()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val scale = LocalScale.current

    val sectionShape = RoundedCornerShape(20.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .bringIntoViewRequester(bringIntoViewRequester)
            .glass(SectionSurface, sectionShape)
            .clip(sectionShape),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle() }
                .padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 6.dp)
                .height(44.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                fontSize = 17f.s(scale),
                fontWeight = FontWeight.Bold,
                fontFamily = bodyFont,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (expanded) {
                IconButton(
                    onClick = {
                        coroutineScope.launch {
                            val header = headerGraphicsLayer.toImageBitmap().asAndroidBitmap()
                            val content = graphicsLayer.toImageBitmap().asAndroidBitmap()
                            shareCardBitmap(context, combineBitmaps(header, content))
                        }
                    },
                    modifier = Modifier.size(36.dp),
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
            }
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(TileSurface, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) stringResource(R.string.cd_collapse)
                                         else stringResource(R.string.cd_expand),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    modifier = Modifier.size(22.dp).rotate(rotation),
                )
            }
        }
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically(),
        ) {
            Box(
                modifier = Modifier
                    .padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
                    .drawWithContent {
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
                    colors = CardDefaults.cardColors(containerColor = if (item === forecasts.first()) TileSurfaceHighlight else TileSurface),
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
                localizeDigits = localizeDigits,
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
    // (e.g. an advanced-mode location override far from the device's real timezone).
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
                    colors = CardDefaults.cardColors(containerColor = if (item === forecasts.first()) TileSurfaceHighlight else TileSurface),
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
                localizeDigits = localizeDigits,
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

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (forecasts.isNotEmpty()) {
            DailyUvChart(
                forecasts = forecasts,
                localizeDigits = localizeDigits,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        TileSurface,
                        RoundedCornerShape(16.dp)
                    )
                    .padding(horizontal = 16.sd(scale), vertical = 12.sd(scale))
                    .height(74.sd(scale))
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(TileSurface, RoundedCornerShape(16.dp)),
        ) {
        forecasts.forEachIndexed { index, item ->
            androidx.compose.runtime.key(item.date) {
                val label = when {
                    item.uvIndexMax < 3 -> uvLabels[0]
                    item.uvIndexMax < 6 -> uvLabels[1]
                    item.uvIndexMax < 8 -> uvLabels[2]
                    item.uvIndexMax < 11 -> uvLabels[3]
                    else -> uvLabels[4]
                }
                if (index > 0) {
                    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(1.dp).background(Hairline))
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.sd(scale), vertical = 10.sd(scale)),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = dayFormat.format(Date(item.date)),
                        fontSize = 14f.s(scale),
                        fontWeight = FontWeight.Medium,
                        fontFamily = bodyFont,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = localizeDigits(dayMonthFormat.format(Date(item.date))),
                        fontSize = 12f.s(scale),
                        fontFamily = bodyFont,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                        maxLines = 1,
                        modifier = Modifier.weight(1f),
                    )
                    UvTierBadge(uvIndex = item.uvIndexMax, label = label)
                    Text(
                        text = localizeDigits("%.1f".format(Locale.US, item.uvIndexMax)),
                        fontSize = 14f.s(scale),
                        fontWeight = FontWeight.Bold,
                        fontFamily = LocalDisplayFont.current,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(40.sd(scale)),
                    )
                }
            }
        }
        }
    }
}

@Composable
internal fun DetailsContent(data: WeatherData, metricPrimary: Boolean, timeFormat: SimpleDateFormat, localizeDigits: (String) -> String, onToggleUnits: () -> Unit) {
    val directions = stringArrayResource(R.array.cardinal_directions)
    val uvLabels = stringArrayResource(R.array.uv_labels)
    val scale = LocalScale.current
    val gap = 10.sd(scale)
    val tile = Modifier.fillMaxHeight()

    @Composable
    fun TileRow(content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(gap),
            content = content,
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(gap)) {
        // Temperature / Precipitation
        TileRow {
            val (tempP, tempS) = data.temperature.displayDual(metricPrimary)
            val (flP, _) = data.feelsLike.displayDual(metricPrimary)
            DetailCard(
                title = stringResource(R.string.detail_temperature),
                value = localizeDigits(tempP),
                secondaryValue = localizeDigits(tempS),
                subtitle = "${stringResource(R.string.feels_like)} ${localizeDigits(flP)}",
                onToggleUnits = onToggleUnits,
                modifier = tile.weight(1f),
            )
            val cloudCover = "${stringResource(R.string.detail_cloud_cover)}: ${data.cloudCover?.let { localizeDigits("%d%%".format(Locale.US, it)) } ?: "--"}"
            when {
                data.snow != null -> {
                    val (snowP, snowS) = data.snow.displayDual(metricPrimary)
                    DetailCard(localizeDigits("❄️ $snowP"), tile.weight(1f),
                        title = stringResource(R.string.detail_precipitation), secondaryValue = localizeDigits(snowS), subtitle = cloudCover, onToggleUnits = onToggleUnits)
                }
                data.rain != null -> {
                    val (rainP, rainS) = data.rain.displayDual(metricPrimary)
                    DetailCard(localizeDigits("🌧️ $rainP"), tile.weight(1f),
                        title = stringResource(R.string.detail_precipitation), secondaryValue = localizeDigits(rainS), subtitle = cloudCover, onToggleUnits = onToggleUnits)
                }
                else -> DetailCard(stringResource(R.string.detail_no_precip), tile.weight(1f),
                    title = stringResource(R.string.detail_precipitation), subtitle = cloudCover)
            }
        }

        // Low / High
        TileRow {
            val (minP, minS) = data.tempMin.displayDual(metricPrimary)
            val (maxP, maxS) = data.tempMax.displayDual(metricPrimary)
            DetailCard(localizeDigits("↓ $minP"), tile.weight(1f),
                title = stringResource(R.string.detail_min_temp), secondaryValue = localizeDigits(minS), onToggleUnits = onToggleUnits)
            DetailCard(localizeDigits("↑ $maxP"), tile.weight(1f),
                title = stringResource(R.string.detail_max_temp), secondaryValue = localizeDigits(maxS), onToggleUnits = onToggleUnits)
        }

        // Wind / Gust
        TileRow {
            val (windP, windS) = data.windSpeed.displayDual(metricPrimary)
            val dirLabel = data.windDeg?.let { directions[((it % 360 + 360) % 360 * 16 / 360) % 16] }
            DetailCard(localizeDigits(windP), tile.weight(1f),
                title = stringResource(R.string.detail_wind), secondaryValue = localizeDigits(windS),
                subtitle = data.windDeg?.let { localizeDigits("$dirLabel (%d°)".format(Locale.US, it)) } ?: "--",
                onToggleUnits = onToggleUnits)
            val gust = data.windGust?.displayDual(metricPrimary)
            DetailCard(gust?.let { localizeDigits(it.first) } ?: "--", tile.weight(1f),
                title = stringResource(R.string.detail_wind_gust), secondaryValue = gust?.let { localizeDigits(it.second) },
                onToggleUnits = onToggleUnits)
        }

        // Sunrise / Sunset
        TileRow {
            DetailCard(localizeDigits(timeFormat.format(Date(data.sunrise * 1000))), tile.weight(1f),
                title = stringResource(R.string.detail_sunrise))
            DetailCard(localizeDigits(timeFormat.format(Date(data.sunset * 1000))), tile.weight(1f),
                title = stringResource(R.string.detail_sunset))
        }

        // Pressure / Humidity
        TileRow {
            val (pressP, pressS) = data.pressure.displayDual(metricPrimary)
            DetailCard(localizeDigits(pressP), tile.weight(1f),
                title = stringResource(R.string.detail_pressure), secondaryValue = localizeDigits(pressS), onToggleUnits = onToggleUnits)
            val (dewP, dewS) = data.dewPoint.displayDual(metricPrimary)
            DetailCard(data.humidity?.let { localizeDigits("%d%%".format(Locale.US, it)) } ?: "--", tile.weight(1f),
                title = stringResource(R.string.detail_humidity),
                subtitle = "${stringResource(R.string.detail_dewpoint)} ${localizeDigits(dewP)} · ${localizeDigits(dewS)}",
                onToggleUnits = onToggleUnits)
        }

        // UV Index (current reading) / Visibility — Air Quality lives on its own forecast card
        TileRow {
            val currentUv = data.uvIndex
            val visibility = data.visibility
            val uvLabelText = when {
                currentUv == null -> ""
                currentUv < 3 -> uvLabels[0]
                currentUv < 6 -> uvLabels[1]
                currentUv < 8 -> uvLabels[2]
                currentUv < 11 -> uvLabels[3]
                else -> uvLabels[4]
            }
            DetailCard(currentUv?.let { localizeDigits("%.1f".format(Locale.US, it)) } ?: "--", tile.weight(1f),
                title = stringResource(R.string.detail_uv_index),
                subtitleContent = currentUv?.let { uv -> @Composable { UvTierBadge(uvIndex = uv, label = uvLabelText) } })
            val km = stringResource(R.string.visibility_km)
            val mi = stringResource(R.string.visibility_mi)
            DetailCard(
                visibility?.let { localizeDigits(if (metricPrimary) km.format(Locale.US, it / 1000.0) else mi.format(Locale.US, it / 1609.34)) } ?: "--",
                tile.weight(1f),
                title = stringResource(R.string.detail_visibility),
                secondaryValue = visibility?.let { localizeDigits(if (metricPrimary) mi.format(Locale.US, it / 1609.34) else km.format(Locale.US, it / 1000.0)) },
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
    trailingContent: (@Composable () -> Unit)? = null,
) {
    val bodyFont = LocalBodyFont.current
    val fontFeatures = LocalBodyFontFeatures.current
    val scale = LocalScale.current
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = TileSurface)
    ) {
        Row(
            modifier = Modifier.padding(16.sd(scale)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
        Column(modifier = Modifier.weight(1f)) {
            if (title != null) {
                Text(
                    text = title,
                    fontSize = 13f.s(scale),
                    fontFamily = bodyFont,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(6.dp))
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
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = it,
                    fontSize = 12f.s(scale),
                    fontFamily = bodyFont,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
        trailingContent?.invoke()
        }
    }
}

internal suspend fun combineBitmaps(header: Bitmap, content: Bitmap): Bitmap = withContext(Dispatchers.Default) {
    // Hardware bitmaps can't be drawn onto a software canvas; copy only those, then recycle the copies.
    val h = softwareBitmap(header)
    val c = softwareBitmap(content)
    val padding = 24
    val width = maxOf(h.width, c.width) + padding * 2
    val height = h.height + padding + c.height + padding * 2
    val result = createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(result)
    canvas.drawColor(DarkNavyColorInt)
    canvas.drawBitmap(h, padding.toFloat(), padding.toFloat(), null)
    canvas.drawBitmap(c, padding.toFloat(), (h.height + padding * 2).toFloat(), null)
    if (h !== header) h.recycle()
    if (c !== content) c.recycle()
    result
}

private fun softwareBitmap(source: Bitmap): Bitmap =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && source.config == Bitmap.Config.HARDWARE) {
        source.copy(Bitmap.Config.ARGB_8888, false)
    } else source

internal suspend fun shareCardBitmap(context: android.content.Context, bitmap: Bitmap) {
    val uri = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "shared_images").also { it.mkdirs() }
        // Prune stale files only; a share target may still be reading a recent one.
        val cutoff = System.currentTimeMillis() - 60 * 60 * 1000L
        dir.listFiles()?.forEach { if (it.lastModified() < cutoff) it.delete() }
        val file = File(dir, "weather_${System.currentTimeMillis()}.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }

        // PNG's "eXIf" chunk carries EXIF just like JPEG does; ExifInterface writes it directly.
        val exifDateTime = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US).format(Date())
        val exif = ExifInterface(file.absolutePath)
        exif.setAttribute(ExifInterface.TAG_DATETIME, exifDateTime)
        exif.setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, exifDateTime)
        exif.setAttribute(ExifInterface.TAG_DATETIME_DIGITIZED, exifDateTime)
        exif.saveAttributes()

        bitmap.recycle()

        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        // ClipData lets the read grant reach the chooser itself, so it can show the preview
        clipData = android.content.ClipData.newRawUri(null, uri)
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

internal fun buildLocationTimeZone(utcOffsetSeconds: Int): TimeZone {
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
