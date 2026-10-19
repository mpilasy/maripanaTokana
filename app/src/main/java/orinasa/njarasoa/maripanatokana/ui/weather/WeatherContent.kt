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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.foundation.combinedClickable
import androidx.compose.material3.rememberTooltipState
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.draw.alpha
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
import orinasa.njarasoa.maripanatokana.domain.model.HourlyAirQuality
import orinasa.njarasoa.maripanatokana.domain.model.PrecipOutlook
import orinasa.njarasoa.maripanatokana.domain.model.dailyAirQuality
import orinasa.njarasoa.maripanatokana.domain.model.aqiDialPosition
import orinasa.njarasoa.maripanatokana.domain.model.AirQualityIndex
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
import orinasa.njarasoa.maripanatokana.ui.weather.components.AirQualityDetailDialog
import orinasa.njarasoa.maripanatokana.ui.weather.components.AqiTierBadge
import orinasa.njarasoa.maripanatokana.ui.weather.components.DailyTemperatureChart
import orinasa.njarasoa.maripanatokana.ui.weather.components.ForecastTabs
import orinasa.njarasoa.maripanatokana.ui.weather.components.WeatherIcon
import orinasa.njarasoa.maripanatokana.ui.weather.components.LevelBar
import orinasa.njarasoa.maripanatokana.ui.weather.components.OverviewChart
import orinasa.njarasoa.maripanatokana.ui.weather.components.OverviewStyle
import orinasa.njarasoa.maripanatokana.ui.weather.components.colorsFor
import orinasa.njarasoa.maripanatokana.domain.model.AqiTier
import orinasa.njarasoa.maripanatokana.ui.theme.AppDarkPrimary
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.foundation.border
import orinasa.njarasoa.maripanatokana.ui.weather.components.Gauge
import orinasa.njarasoa.maripanatokana.ui.weather.components.SunArc
import orinasa.njarasoa.maripanatokana.ui.weather.components.TemperatureRangeBar
import orinasa.njarasoa.maripanatokana.ui.weather.components.WindCompass
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
    Pressure,
    AirQuality,
    Uv,
}

internal val LocalScale = staticCompositionLocalOf { 1f }
internal fun Float.s(scale: Float) = (this * scale).sp
internal fun Int.sd(scale: Float) = (this * scale).dp

@OptIn(ExperimentalFoundationApi::class)
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
    onLocationLongClicked: (() -> Unit)? = null, // set for a favorite: opens its rename dialog
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
    locationNames: List<String?> = emptyList(), // pager order; null = current (GPS) location
    onSelectLocation: (Int) -> Unit = {},
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
    // Sunrise and sunset in the place's own time, like the hourly cards and its clock
    val timeFormat = remember(data.utcOffsetSeconds) { SimpleDateFormat("HH:mm", Locale.US).apply { timeZone = buildLocationTimeZone(data.utcOffsetSeconds) } }
    val displayFont = LocalDisplayFont.current
    val bodyFont = LocalBodyFont.current

    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val scale = when {
        screenWidthDp >= 400 -> 1f
        screenWidthDp >= 320 -> 0.8f
        else -> 0.7f
    }

    // Switching to a new location goes back to the top.
    val locationKey = "${data.locationName}|${data.locationSubtext}"
    var alertsExpanded by rememberSaveable(locationKey) { mutableStateOf(false) }
    var lastLocationKey by rememberSaveable { mutableStateOf(locationKey) }
    LaunchedEffect(locationKey) {
        if (locationKey != lastLocationKey) {
            lastLocationKey = locationKey
            scrollState.scrollTo(0)
        }
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
        val haptic = LocalHapticFeedback.current
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
                modifier = Modifier.combinedClickable(
                    onClick = onLocationClicked,
                    onLongClick = onLocationLongClicked?.let { rename ->
                        {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            rename()
                        }
                    },
                    role = Role.Button,
                    onClickLabel = "Toggle GPS coordinates",
                    onLongClickLabel = stringResource(R.string.rename_title),
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
                        PlusCodeInfo(displayLat, displayLon, Modifier.align(Alignment.CenterVertically))
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
            // Holding either arrow lists every location to jump straight to one
            var pickerOpen by remember { mutableStateOf(false) }
            Box(modifier = Modifier.fillMaxWidth().padding(top = 8.sd(scale)), contentAlignment = Alignment.Center) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LocationPagerArrow(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = stringResource(R.string.cd_previous_location),
                    enabled = canSwipeToPrevious,
                    onClick = onSwipeToPreviousLocation,
                    onLongClick = { pickerOpen = true },
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
                    onLongClick = { pickerOpen = true },
                )
            }
            DropdownMenu(
                expanded = pickerOpen,
                onDismissRequest = { pickerOpen = false },
                containerColor = Color(0xFF1A1565), // same as the location dialog
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Hairline),
            ) {
                // A search preview isn't in the list, so nothing is marked as current then
                val currentIndex = if (hasLocationOverride) -1 else locationIndex
                locationNames.forEachIndexed { i, name ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = name ?: stringResource(R.string.current_location),
                                fontFamily = bodyFont,
                                fontWeight = if (i == currentIndex) FontWeight.Bold else FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (i == currentIndex) 1f else 0.85f),
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = if (name == null) Icons.Default.LocationOn else Icons.Default.Favorite,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                modifier = Modifier.size(18.dp),
                            )
                        },
                        onClick = {
                            pickerOpen = false
                            onSelectLocation(i)
                        },
                    )
                }
            }
            }
        }

        // The glance, then one card per kind of data, in one free-scrolling column
        val showNowcast = weatherSource == WeatherSource.OPEN_METEO && data.minutelyForecast.isNotEmpty()
        val cards = buildList<@Composable () -> Unit> {
            add {
                Column {
                    WeatherAlertBanner(
                        alerts = data.alerts,
                        localizeDigits = localizeDigits,
                        expanded = alertsExpanded,
                        onToggle = { alertsExpanded = !alertsExpanded },
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
                                WeatherIcon(data.weatherCode, night = data.timestamp !in (data.sunrise * 1000)..(data.sunset * 1000), size = 64.sd(scale))
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

                            val (tempPrimary, tempSecondary) = data.temperature.displayDualMixed(metricPrimary)
                            Box(modifier = Modifier.padding(top = 8.sd(scale))) {
                                DualUnitText(
                                    primary = localizeDigits(tempPrimary),
                                    secondary = localizeDigits(tempSecondary),
                                    primarySize = 80f.s(scale),
                                    secondaryScale = 0.32f,
                                    onClick = onToggleUnits,
                                )
                            }
                            val (maxP, maxS) = data.tempMax.displayDual(metricPrimary)
                            val (minP, minS) = data.tempMin.displayDual(metricPrimary)
                            Box(modifier = Modifier.padding(top = 8.sd(scale))) {
                                DualUnitText(
                                    primary = localizeDigits("\u2193$minP \u2191$maxP"),
                                    secondary = localizeDigits("\u2193$minS \u2191$maxS"),
                                    onClick = onToggleUnits,
                                )
                            }
                            GlanceOutlook(data, localizeDigits)
                            NextHoursStrip(data, metricPrimary, localizeDigits, onToggleUnits)

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

                }
            }
            if (data.hourlyForecast.isNotEmpty()) add {
                SectionCard(stringResource(R.string.section_hourly_forecast), headerGraphicsLayer) {
                    if (showNowcast) {
                        Text(
                            text = stringResource(R.string.section_nowcast),
                            fontSize = 14f.s(scale),
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = bodyFont,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
                        )
                        orinasa.njarasoa.maripanatokana.ui.weather.components.NowcastCard(
                            items = data.minutelyForecast,
                            isMetricPrimary = metricPrimary,
                            utcOffsetSeconds = data.utcOffsetSeconds,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    HourlyForecastRow(data.hourlyForecast, metricPrimary, data.dailySunrise, data.dailySunset, localizeDigits, onToggleUnits, data.utcOffsetSeconds,
                        hourlyAirQuality = data.hourlyAirQuality.takeIf { data.airQuality != null }.orEmpty(),
                        aqiStandard = data.airQuality?.primaryStandard ?: AqiStandard.US)
                }
            }
            if (data.dailyForecast.isNotEmpty()) add {
                SectionCard(stringResource(R.string.section_this_week), headerGraphicsLayer) {
                    DailyForecastList(data.dailyForecast, metricPrimary, localizeDigits, onToggleUnits, data.utcOffsetSeconds,
                        hourlyAirQuality = data.hourlyAirQuality.takeIf { data.airQuality != null }.orEmpty(),
                        aqiStandard = data.airQuality?.primaryStandard ?: AqiStandard.US)
                }
            }
            add {
                SectionCard(stringResource(R.string.section_current_conditions), headerGraphicsLayer, bare = true) {
                    DetailsContent(data, metricPrimary, timeFormat, localizeDigits, onToggleUnits, DetailsPart.NOW)
                }
            }
            add {
                SectionCard(stringResource(R.string.section_sun_air), headerGraphicsLayer, bare = true) {
                    DetailsContent(data, metricPrimary, timeFormat, localizeDigits, onToggleUnits, DetailsPart.SKY)
                }
            }
            add {
                SectionCard(stringResource(R.string.section_activities), headerGraphicsLayer, bare = true) {
                    val activityIndices = remember(data) { ActivityIndices.fromWeatherData(data) }
                    ActivitiesCard(indices = activityIndices)
                }
            }
        }
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
                .padding(vertical = 16.sd(scale)),
            verticalArrangement = Arrangement.spacedBy(12.sd(scale)),
        ) {
            cards.forEach { it() }
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
                        .alpha(0.7f)
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
                        .alpha(0.7f)
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
@OptIn(ExperimentalFoundationApi::class)
private fun LocationPagerArrow(
    imageVector: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    // Long-press works even at the end of the list, where the tap does nothing
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .combinedClickable(
                role = Role.Button,
                onClick = { if (enabled) onClick() },
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                },
            ),
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 0.7f else 0.15f),
            modifier = Modifier.size(20.dp),
        )
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
private fun HeroStatDivider() {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .padding(vertical = 4.dp)
            .width(1.dp)
            .background(Hairline)
    )
}

/** A card of weather data, always open, with a title and a share button. */
@Composable
internal fun SectionCard(
    title: String,
    headerGraphicsLayer: GraphicsLayer,
    // No surface of its own, for sections whose tiles are already cards
    bare: Boolean = false,
    content: @Composable () -> Unit,
) {
    val bodyFont = LocalBodyFont.current
    val graphicsLayer = rememberGraphicsLayer()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val scale = LocalScale.current

    val sectionShape = RoundedCornerShape(20.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (bare) Modifier else Modifier.glass(SectionSurface, sectionShape))
            .clip(sectionShape),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = if (bare) 4.dp else 16.dp, end = if (bare) 0.dp else 8.dp, top = 6.dp, bottom = 6.dp)
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
        Column(
            modifier = Modifier
                .padding(start = if (bare) 0.dp else 12.dp, end = if (bare) 0.dp else 12.dp, bottom = 12.dp)
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

/** One line on what the next 24 hours hold for rain or snow. */
@Composable
private fun GlanceOutlook(data: WeatherData, localizeDigits: (String) -> String) {
    val outlook = remember(data.hourlyForecast, data.minutelyForecast) { PrecipOutlook.from(data.hourlyForecast, data.minutelyForecast) } ?: return
    val hourFormat = remember(data.utcOffsetSeconds) {
        SimpleDateFormat("HH:mm", Locale.US).apply { timeZone = buildLocationTimeZone(data.utcOffsetSeconds) }
    }
    val time = outlook.time?.let { localizeDigits(hourFormat.format(Date(it))) }.orEmpty()
    val text = when (outlook.kind) {
        PrecipOutlook.Kind.STARTS -> stringResource(if (outlook.snow) R.string.outlook_snow_from else R.string.outlook_rain_from, time)
        PrecipOutlook.Kind.STOPS -> stringResource(if (outlook.snow) R.string.outlook_snow_until else R.string.outlook_rain_until, time)
        PrecipOutlook.Kind.ALL_DAY -> stringResource(if (outlook.snow) R.string.outlook_snow_all_day else R.string.outlook_rain_all_day)
        PrecipOutlook.Kind.DRY -> stringResource(R.string.outlook_dry)
    }
    val scale = LocalScale.current
    Text(
        text = text,
        fontSize = 15f.s(scale),
        fontFamily = LocalBodyFont.current,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
        modifier = Modifier.padding(top = 16.sd(scale)),
    )
}

/** The next three hours: time, icon and temperature. */
@Composable
private fun NextHoursStrip(data: WeatherData, metricPrimary: Boolean, localizeDigits: (String) -> String, onToggleUnits: () -> Unit) {
    val hours = data.hourlyForecast.take(3)
    if (hours.isEmpty()) return
    val hourFormat = remember(data.utcOffsetSeconds) {
        SimpleDateFormat("HH:mm", Locale.US).apply { timeZone = buildLocationTimeZone(data.utcOffsetSeconds) }
    }
    val scale = LocalScale.current
    val bodyFont = LocalBodyFont.current
    Row(
        modifier = Modifier
            .padding(top = 16.sd(scale))
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .background(TileSurface, RoundedCornerShape(16.dp))
            .padding(vertical = 12.sd(scale)),
    ) {
        hours.forEachIndexed { i, hour ->
            if (i > 0) HeroStatDivider()
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = localizeDigits(hourFormat.format(Date(hour.time))),
                    fontSize = 12f.s(scale),
                    fontFamily = bodyFont,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
                val dayIdx = data.dailySunrise.indexOfLast { it <= hour.time }.coerceAtLeast(0)
                val isNight = hour.time !in data.dailySunrise.getOrElse(dayIdx) { 0L }..data.dailySunset.getOrElse(dayIdx) { 0L }
                WeatherIcon(hour.weatherCode, night = isNight, size = 40.sd(scale))
                val (tP, tS) = hour.temperature.displayDual(metricPrimary)
                DualUnitText(
                    primary = localizeDigits(tP),
                    secondary = localizeDigits(tS),
                    primarySize = 15f.s(scale),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    onClick = onToggleUnits,
                )
            }
        }
    }
}

@Composable
internal fun HourlyForecastRow(
    forecasts: List<HourlyForecast>, metricPrimary: Boolean, dailySunrise: List<Long>, dailySunset: List<Long>, localizeDigits: (String) -> String, onToggleUnits: () -> Unit, utcOffsetSeconds: Int = 0,
    // Hourly air quality for its tab; the tab is left out without it
    hourlyAirQuality: List<HourlyAirQuality> = emptyList(),
    aqiStandard: AqiStandard = AqiStandard.US,
) {
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
    val itemWidth = 80.sd(scale)
    val itemSpacing = 12.sd(scale)
    val density = LocalDensity.current
    val totalScrollWidthPx = with(density) {
        (itemWidth * forecasts.size + itemSpacing * (forecasts.size - 1)).toPx()
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        ForecastTabs(
            tabs = listOfNotNull(
                ForecastDisplayMode.Temperature to stringResource(R.string.tab_conditions),
                ForecastDisplayMode.Wind to stringResource(R.string.detail_wind),
                ForecastDisplayMode.Precipitation to stringResource(R.string.detail_precipitation),
                ForecastDisplayMode.Pressure to stringResource(R.string.detail_pressure),
                ForecastDisplayMode.Uv to stringResource(R.string.detail_uv_index),
                if (hourlyAirQuality.isNotEmpty()) ForecastDisplayMode.AirQuality to stringResource(R.string.detail_air_quality) else null,
            ),
            selected = displayMode,
            onSelect = { displayMode = it },
        )
        val uvLabels = stringArrayResource(R.array.uv_labels)
        // Each hour's air quality, matched by time; null when missing
        val aqByHour = remember(forecasts, hourlyAirQuality) {
            forecasts.map { f -> hourlyAirQuality.firstOrNull { kotlin.math.abs(it.time - f.time) < 30 * 60 * 1000L } }
        }
        val usAqiLabel = stringResource(R.string.air_quality_us_aqi)
        val euAqiLabel = stringResource(R.string.air_quality_eu_aqi)
        val aqiTierLabels = stringArrayResource(R.array.aqi_tier_labels)
        val european = aqiStandard == AqiStandard.EUROPEAN
        fun aqDual(aq: HourlyAirQuality) =
            if (european) aq.europeanValue to aq.usValue else aq.usValue to aq.europeanValue
        Row(
            horizontalArrangement = Arrangement.spacedBy(itemSpacing),
            modifier = Modifier.horizontalScroll(scrollState).padding(vertical = 8.dp)
        ) {
            forecasts.forEachIndexed { index, item ->
                Card(
                    modifier = Modifier.width(itemWidth),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = if (item === forecasts.first()) TileSurface else Color.Transparent),
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
                        val dayIdx = dailySunrise.indexOfLast { it <= item.time }.coerceAtLeast(0)
                        val sr = dailySunrise.getOrElse(dayIdx) { 0L }
                        val ss = dailySunset.getOrElse(dayIdx) { 0L }
                        WeatherIcon(item.weatherCode, night = item.time !in sr..ss, size = 44.sd(scale))
                        
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
                                DualUnitText(
                                    primary = localizeDigits(windP),
                                    secondary = localizeDigits(windS),
                                    primarySize = 14f.s(scale),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    onClick = onToggleUnits,
                                )
                                WindDirectionLabel(item.windDirection)
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
                            ForecastDisplayMode.Uv -> {
                                val uv = item.uvIndex
                                if (uv != null) {
                                    Text(
                                        text = localizeDigits("%.1f".format(Locale.US, uv)),
                                        fontSize = 15f.s(scale),
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = LocalDisplayFont.current,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    LevelBar(colorsFor(uv).background, uvLabels[uvTierIndex(uv)])
                                } else {
                                    Text(text = "--", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                }
                            }
                            ForecastDisplayMode.AirQuality -> {
                                val aq = aqByHour[index]
                                if (aq != null) {
                                    val (p, s) = aqDual(aq)
                                    DualUnitText(
                                        primary = localizeDigits(p.toString()),
                                        secondary = localizeDigits(s.toString()),
                                        primarySize = 14f.s(scale),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        primaryUnit = if (european) euAqiLabel else usAqiLabel,
                                        secondaryUnit = if (european) usAqiLabel else euAqiLabel,
                                    )
                                    val tier = aq.tier(aqiStandard)
                                    LevelBar(colorsFor(tier).background, aqiTierLabels[tier.ordinal])
                                } else {
                                    Text(text = "--", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                }
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
                    .height(64.sd(scale))
                    .padding(bottom = 8.dp)
            )
        } else {
            // The same kind of chart under the cards for every other tab
            fun dual(pair: Pair<String, String>?) = pair?.let { localizeDigits("${it.first} · ${it.second}") }
            fun <T> extremes(values: List<Double?>, item: (Int) -> T) =
                values.withIndex().filter { it.value != null }.let { v -> v.maxByOrNull { it.value!! }?.index?.let(item) to v.minByOrNull { it.value!! }?.index?.let(item) }
            val chartModifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            run {
                when (displayMode) {
                    ForecastDisplayMode.Wind -> {
                        val values = forecasts.map { if (metricPrimary) it.windSpeed.metersPerSecond else it.windSpeed.mph }
                        val (hi, lo) = extremes(values) { forecasts[it].windSpeed.displayDual(metricPrimary) }
                        OverviewChart(values, itemWidth, itemSpacing, scrollState.value.toFloat(), totalScrollWidthPx, chartModifier,
                            height = 48.sd(scale), fromZero = true, color = AppDarkPrimary, highLabel = dual(hi), lowLabel = dual(lo))
                    }
                    ForecastDisplayMode.Precipitation -> {
                        val values = forecasts.map { it.precipitation.mm }
                        val (hi, _) = extremes(values) { forecasts[it].precipitation.displayDual(metricPrimary) }
                        OverviewChart(values, itemWidth, itemSpacing, scrollState.value.toFloat(), totalScrollWidthPx, chartModifier,
                            height = 48.sd(scale), style = OverviewStyle.BARS, color = SkyBlue, highLabel = dual(hi))
                    }
                    ForecastDisplayMode.Pressure -> {
                        val values = forecasts.map { if (metricPrimary) it.pressure.hPa else it.pressure.inHg }
                        val (hi, lo) = extremes(values) { forecasts[it].pressure.displayDual(metricPrimary) }
                        OverviewChart(values, itemWidth, itemSpacing, scrollState.value.toFloat(), totalScrollWidthPx, chartModifier,
                            height = 48.sd(scale), color = AppDarkPrimary, highLabel = dual(hi), lowLabel = dual(lo))
                    }
                    ForecastDisplayMode.Uv -> {
                        val values = forecasts.map { it.uvIndex }
                        val (hi, lo) = extremes(values) { localizeDigits("UV %.1f".format(Locale.US, forecasts[it].uvIndex)) }
                        OverviewChart(values, itemWidth, itemSpacing, scrollState.value.toFloat(), totalScrollWidthPx, chartModifier,
                            height = 48.sd(scale), fromZero = true,
                            pointColors = values.map { uv -> uv?.let { colorsFor(it).background } ?: Color.Transparent },
                            highLabel = hi, lowLabel = lo,
                            legend = listOf(1.0, 4.0, 7.0, 9.0, 11.0).mapIndexed { k, sample -> colorsFor(sample).background to uvLabels[k] })
                    }
                    ForecastDisplayMode.AirQuality -> {
                        val values = aqByHour.map { aq -> aq?.let { aqDual(it).first.toDouble() } }
                        val (hi, lo) = extremes(values) { i ->
                            aqByHour[i]?.let { aq -> aqDual(aq).let { (p, s) -> "$p ${if (european) euAqiLabel else usAqiLabel} · $s ${if (european) usAqiLabel else euAqiLabel}" } }
                        }
                        OverviewChart(values, itemWidth, itemSpacing, scrollState.value.toFloat(), totalScrollWidthPx, chartModifier,
                            height = 48.sd(scale), fromZero = true,
                            pointColors = aqByHour.map { aq -> aq?.let { colorsFor(it.tier(aqiStandard)).background } ?: Color.Transparent },
                            highLabel = hi?.let(localizeDigits), lowLabel = lo?.let(localizeDigits),
                            legend = AqiTier.entries.map { colorsFor(it).background to aqiTierLabels[it.ordinal] })
                    }
                    else -> {}
                }
            }
        }
    }
}

@Composable
internal fun DailyForecastList(
    forecasts: List<DailyForecast>, metricPrimary: Boolean, localizeDigits: (String) -> String, onToggleUnits: () -> Unit, utcOffsetSeconds: Int,
    // Hourly air quality, summed up per day for its tab; the tab is left out without it
    hourlyAirQuality: List<HourlyAirQuality> = emptyList(),
    aqiStandard: AqiStandard = AqiStandard.US,
) {
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

    val aqByDay = remember(forecasts, hourlyAirQuality, aqiStandard) {
        dailyAirQuality(forecasts.map { it.date }, hourlyAirQuality, aqiStandard)
    }
    val usAqiLabel = stringResource(R.string.air_quality_us_aqi)
    val euAqiLabel = stringResource(R.string.air_quality_eu_aqi)
    val aqiTierLabels = stringArrayResource(R.array.aqi_tier_labels)
    val european = aqiStandard == AqiStandard.EUROPEAN
    val uvLabels = stringArrayResource(R.array.uv_labels)

    // Each day's low-to-high bar is placed on one scale shared by the whole week
    val weekLow = forecasts.map { it.tempMin.celsius }.filter { it.isFinite() }.minOrNull() ?: 0.0
    val weekHigh = forecasts.map { it.tempMax.celsius }.filter { it.isFinite() }.maxOrNull() ?: 0.0
    fun barFraction(celsius: Double): Float =
        if (weekHigh > weekLow && celsius.isFinite()) ((celsius - weekLow) / (weekHigh - weekLow)).toFloat() else 0.5f

    Column(modifier = Modifier.fillMaxWidth()) {
        ForecastTabs(
            tabs = listOf(
                ForecastDisplayMode.Temperature to stringResource(R.string.tab_conditions),
                ForecastDisplayMode.Wind to stringResource(R.string.detail_wind),
                ForecastDisplayMode.Precipitation to stringResource(R.string.detail_precipitation),
                ForecastDisplayMode.Pressure to stringResource(R.string.detail_pressure),
                ForecastDisplayMode.Uv to stringResource(R.string.detail_uv_index),
            ) + listOfNotNull(
                if (hourlyAirQuality.isNotEmpty()) ForecastDisplayMode.AirQuality to stringResource(R.string.detail_air_quality) else null,
            ),
            selected = displayMode,
            onSelect = { displayMode = it },
        )
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
                    colors = CardDefaults.cardColors(containerColor = if (item === forecasts.first()) TileSurface else Color.Transparent),
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
                        WeatherIcon(item.weatherCode, size = 44.sd(scale))
                        Spacer(modifier = Modifier.height(4.dp))
                        when (displayMode) {
                            ForecastDisplayMode.Temperature -> {
                                val (maxP, maxS) = item.tempMax.displayDual(metricPrimary)
                                val (minP, minS) = item.tempMin.displayDual(metricPrimary)
                                DualUnitText(
                                    primary = localizeDigits(maxP),
                                    secondary = localizeDigits(maxS),
                                    primarySize = 13f.s(scale),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    onClick = onToggleUnits,
                                )
                                TemperatureRangeBar(
                                    low = barFraction(item.tempMin.celsius),
                                    high = barFraction(item.tempMax.celsius),
                                    modifier = Modifier.padding(vertical = 4.dp).width(6.dp).height(56.sd(scale)),
                                )
                                DualUnitText(
                                    primary = localizeDigits(minP),
                                    secondary = localizeDigits(minS),
                                    primarySize = 13f.s(scale),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    onClick = onToggleUnits,
                                )
                            }
                            ForecastDisplayMode.Wind -> {
                                val (windP, windS) = item.windSpeed.displayDual(metricPrimary)
                                DualUnitText(
                                    primary = localizeDigits(windP),
                                    secondary = localizeDigits(windS),
                                    primarySize = 13f.s(scale),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    onClick = onToggleUnits,
                                )
                                WindDirectionLabel(item.windDirection)
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
                            ForecastDisplayMode.Pressure -> {
                                val pressure = item.pressure
                                if (pressure != null && pressure.hPa.isFinite()) {
                                    val (pressP, pressS) = pressure.displayDual(metricPrimary)
                                    DualUnitText(
                                        primary = localizeDigits(pressP),
                                        secondary = localizeDigits(pressS),
                                        primarySize = 13f.s(scale),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        onClick = onToggleUnits,
                                    )
                                } else {
                                    Text(text = "--", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                }
                            }
                            ForecastDisplayMode.Uv -> {
                                Text(
                                    text = localizeDigits("%.1f".format(Locale.US, item.uvIndexMax)),
                                    fontSize = 15f.s(scale),
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = LocalDisplayFont.current,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                LevelBar(colorsFor(item.uvIndexMax).background, uvLabels[uvTierIndex(item.uvIndexMax)])
                            }
                            ForecastDisplayMode.AirQuality -> {
                                val aq = aqByDay.getOrNull(forecasts.indexOf(item))
                                if (aq != null) {
                                    DualUnitText(
                                        primary = localizeDigits(aq.first.toString()),
                                        secondary = localizeDigits(aq.second.toString()),
                                        primarySize = 13f.s(scale),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        primaryUnit = if (european) euAqiLabel else usAqiLabel,
                                        secondaryUnit = if (european) usAqiLabel else euAqiLabel,
                                    )
                                    val tier = AirQualityIndex.tierFor(aq.first, aqiStandard)
                                    LevelBar(colorsFor(tier).background, aqiTierLabels[tier.ordinal])
                                } else {
                                    Text(text = "--", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                }
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
                            modifier = Modifier.fillMaxWidth(),
                        )
                        if (displayMode == ForecastDisplayMode.Temperature) {
                            // Kept even when empty so the cards stay the same height
                            Box(modifier = Modifier.height(28.sd(scale)), contentAlignment = Alignment.Center) {
                                item.nightWeatherCode?.let { WeatherIcon(it, night = true, size = 36.sd(scale)) }
                            }
                        }
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
        } else {
            // The same kind of chart under the cards for every other tab
            fun dual(pair: Pair<String, String>?) = pair?.let { localizeDigits("${it.first} · ${it.second}") }
            fun <T> extremes(values: List<Double?>, item: (Int) -> T) =
                values.withIndex().filter { it.value != null }.let { v -> v.maxByOrNull { it.value!! }?.index?.let(item) to v.minByOrNull { it.value!! }?.index?.let(item) }
            val chartModifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            when (displayMode) {
                ForecastDisplayMode.Wind -> {
                    // Each day's strongest wind, so one bar per day
                    val values = forecasts.map { if (metricPrimary) it.windSpeed.metersPerSecond else it.windSpeed.mph }
                    val (hi, _) = extremes(values) { forecasts[it].windSpeed.displayDual(metricPrimary) }
                    OverviewChart(values, itemWidth, itemSpacing, scrollState.value.toFloat(), totalScrollWidthPx, chartModifier,
                        height = 48.sd(scale), style = OverviewStyle.BARS, color = AppDarkPrimary, highLabel = dual(hi))
                }
                ForecastDisplayMode.Pressure -> {
                    // The day's mean, a reading whose trend from day to day matters, so a line
                    val values = forecasts.map { f -> f.pressure?.takeIf { it.hPa.isFinite() }?.let { if (metricPrimary) it.hPa else it.inHg } }
                    val (hi, lo) = extremes(values) { forecasts[it].pressure?.displayDual(metricPrimary) }
                    OverviewChart(values, itemWidth, itemSpacing, scrollState.value.toFloat(), totalScrollWidthPx, chartModifier,
                        height = 48.sd(scale), color = AppDarkPrimary, highLabel = dual(hi), lowLabel = dual(lo))
                }
                ForecastDisplayMode.Precipitation -> {
                    val values = forecasts.map { it.precipitation.mm }
                    val (hi, _) = extremes(values) { forecasts[it].precipitation.displayDual(metricPrimary) }
                    OverviewChart(values, itemWidth, itemSpacing, scrollState.value.toFloat(), totalScrollWidthPx, chartModifier,
                        height = 48.sd(scale), style = OverviewStyle.BARS, color = SkyBlue, highLabel = dual(hi))
                }
                ForecastDisplayMode.AirQuality -> {
                    val values = aqByDay.map { it?.first?.toDouble() }
                    val (hi, lo) = extremes(values) { i ->
                        aqByDay[i]?.let { (p, s) -> localizeDigits("$p ${if (european) euAqiLabel else usAqiLabel} · $s ${if (european) usAqiLabel else euAqiLabel}") }
                    }
                    // Each day's worst value, so one bar per day
                    OverviewChart(values, itemWidth, itemSpacing, scrollState.value.toFloat(), totalScrollWidthPx, chartModifier,
                        height = 48.sd(scale), style = OverviewStyle.BARS,
                        pointColors = aqByDay.map { aq -> aq?.let { colorsFor(AirQualityIndex.tierFor(it.first, aqiStandard)).background } ?: Color.Transparent },
                        highLabel = hi, lowLabel = lo,
                        legend = AqiTier.entries.map { colorsFor(it).background to aqiTierLabels[it.ordinal] })
                }
                ForecastDisplayMode.Uv -> {
                    val values = forecasts.map { it.uvIndexMax }
                    val (hi, lo) = extremes(values) { localizeDigits("UV %.1f".format(Locale.US, forecasts[it].uvIndexMax)) }
                    // Each day's highest value, so one bar per day
                    OverviewChart(values, itemWidth, itemSpacing, scrollState.value.toFloat(), totalScrollWidthPx, chartModifier,
                        height = 48.sd(scale), style = OverviewStyle.BARS,
                        pointColors = values.map { colorsFor(it).background },
                        highLabel = hi, lowLabel = lo,
                        legend = listOf(1.0, 4.0, 7.0, 9.0, 11.0).mapIndexed { k, sample -> colorsFor(sample).background to uvLabels[k] })
                }
                else -> {}
            }
        }
    }
}

/** Index into uv_labels: Low, Moderate, High, Very High, Extreme. */
private fun uvTierIndex(uv: Double) = when {
    uv < 3 -> 0
    uv < 6 -> 1
    uv < 8 -> 2
    uv < 11 -> 3
    else -> 4
}

/** A small compass pointing to where the wind comes from, with the direction's short name. */
@Composable
private fun WindDirectionLabel(degrees: Int) {
    val directions = stringArrayResource(R.array.cardinal_directions)
    val scale = LocalScale.current
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
        WindCompass(degrees, size = 18.dp)
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = directions[((degrees % 360 + 360) / 22.5 + 0.5).toInt() % 16],
            fontSize = 11f.s(scale),
            fontFamily = LocalBodyFont.current,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        )
    }
}

internal enum class DetailsPart { NOW, SKY }

/** NOW: temperature, rain, wind, pressure, humidity; SKY: sun, air quality, UV, visibility. */
@Composable
internal fun DetailsContent(data: WeatherData, metricPrimary: Boolean, timeFormat: SimpleDateFormat, localizeDigits: (String) -> String, onToggleUnits: () -> Unit, part: DetailsPart) {
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
        if (part == DetailsPart.NOW) {
            // Temperature / Precipitation
            TileRow {
                val (tempP, tempS) = data.temperature.displayDual(metricPrimary)
                val (flP, flS) = data.feelsLike.displayDual(metricPrimary)
                DetailCard(
                    title = stringResource(R.string.detail_temperature),
                    value = localizeDigits(tempP),
                    secondaryValue = localizeDigits(tempS),
                    subtitle = "${stringResource(R.string.feels_like)} ${localizeDigits(flP)} · ${localizeDigits(flS)}",
                    onToggleUnits = onToggleUnits,
                    modifier = tile.weight(1f),
                )
                val cloudCover = "${stringResource(R.string.detail_cloud_cover)}: ${data.cloudCover?.let { localizeDigits("%d%%".format(Locale.US, it)) } ?: "--"}"
                when {
                    data.snow != null -> {
                        val (snowP, snowS) = data.snow.displayDual(metricPrimary)
                        DetailCard(localizeDigits(snowP), tile.weight(1f),
                            title = stringResource(R.string.detail_precipitation), secondaryValue = localizeDigits(snowS), subtitle = cloudCover, onToggleUnits = onToggleUnits,
                            cornerContent = { WeatherIcon(71, size = 44.dp) })
                    }
                    data.rain != null -> {
                        val (rainP, rainS) = data.rain.displayDual(metricPrimary)
                        DetailCard(localizeDigits(rainP), tile.weight(1f),
                            title = stringResource(R.string.detail_precipitation), secondaryValue = localizeDigits(rainS), subtitle = cloudCover, onToggleUnits = onToggleUnits,
                            cornerContent = { WeatherIcon(61, size = 44.dp) })
                    }
                    else -> DetailCard(stringResource(R.string.detail_no_precip), tile.weight(1f),
                        title = stringResource(R.string.detail_precipitation), subtitle = cloudCover)
                }
            }

            // Wind / Gust
            TileRow {
                val (windP, windS) = data.windSpeed.displayDual(metricPrimary)
                val dirLabel = data.windDeg?.let { directions[((it % 360 + 360) % 360 * 16 / 360) % 16] }
                DetailCard(localizeDigits(windP), tile.weight(1f),
                    title = stringResource(R.string.detail_wind), secondaryValue = localizeDigits(windS),
                    subtitle = data.windDeg?.let { localizeDigits("$dirLabel (%d°)".format(Locale.US, it)) } ?: "--",
                    onToggleUnits = onToggleUnits,
                    cornerContent = data.windDeg?.let { deg -> @Composable { WindCompass(deg) } })
                val gust = data.windGust?.displayDual(metricPrimary)
                DetailCard(gust?.let { localizeDigits(it.first) } ?: "--", tile.weight(1f),
                    title = stringResource(R.string.detail_wind_gust), secondaryValue = gust?.let { localizeDigits(it.second) },
                    onToggleUnits = onToggleUnits)
            }

            // Pressure / Humidity
            TileRow {
                val (pressP, pressS) = data.pressure.displayDual(metricPrimary)
                DetailCard(localizeDigits(pressP), tile.weight(1f),
                    title = stringResource(R.string.detail_pressure), secondaryValue = localizeDigits(pressS), onToggleUnits = onToggleUnits,
                    cornerContent = {
                        // Sea-level pressure rarely leaves 960-1050 hPa
                        Gauge(((data.pressure.hPa - 960) / 90).toFloat(), AppDarkPrimary)
                    })
                val (dewP, dewS) = data.dewPoint.displayDual(metricPrimary)
                DetailCard(data.humidity?.let { localizeDigits("%d%%".format(Locale.US, it)) } ?: "--", tile.weight(1f),
                    title = stringResource(R.string.detail_humidity),
                    subtitle = "${stringResource(R.string.detail_dewpoint)} ${localizeDigits(dewP)} · ${localizeDigits(dewS)}",
                    onToggleUnits = onToggleUnits,
                    fillFraction = data.humidity?.let { it / 100f })
            }
        } else {
            // Sun / Air quality (the sun spans the row when there is no air-quality reading)
            TileRow {
                SunTile(data.sunrise, data.sunset, timeFormat, localizeDigits, tile.weight(1f))
                data.airQuality?.let { aqi ->
                    var showAirQualityDetail by remember { mutableStateOf(false) }
                    val usAqiLabel = stringResource(R.string.air_quality_us_aqi)
                    val euAqiLabel = stringResource(R.string.air_quality_eu_aqi)
                    val (aqiP, aqiS) = aqi.displayDual()
                    val european = aqi.primaryStandard == AqiStandard.EUROPEAN
                    DetailCard(localizeDigits(aqiP), tile.weight(1f),
                        title = stringResource(R.string.detail_air_quality),
                        secondaryValue = localizeDigits(aqiS),
                        unit = if (european) euAqiLabel else usAqiLabel,
                        secondaryUnit = if (european) usAqiLabel else euAqiLabel,
                        subtitle = stringArrayResource(R.array.aqi_tier_labels)[aqi.primaryTier.ordinal],
                        onClick = { showAirQualityDetail = true },
                        cornerContent = {
                            Gauge(aqiDialPosition(if (european) aqi.europeanValue else aqi.usValue, aqi.primaryStandard), colorsFor(aqi.primaryTier).background,
                            segments = AqiTier.entries.map { colorsFor(it).background })
                        })
                    if (showAirQualityDetail) {
                        AirQualityDetailDialog(
                            airQuality = aqi,
                            onDismissRequest = { showAirQualityDetail = false },
                            localizeDigits = localizeDigits,
                        )
                    }
                }
            }

            // UV Index (current reading) / Visibility
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
                    subtitleContent = currentUv?.let { uv -> @Composable {
                        Column {
                            UvTierBadge(uvIndex = uv, label = uvLabelText)
                            Spacer(modifier = Modifier.height(6.dp))
                            UvTierDots(uv)
                        }
                    } })
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
    // A small graphic in the top corner; the value may run under it
    cornerContent: (@Composable () -> Unit)? = null,
    // Fills the tile from the bottom up to this fraction, like a water level
    fillFraction: Float? = null,
    onClick: (() -> Unit)? = null,
) {
    val bodyFont = LocalBodyFont.current
    val fontFeatures = LocalBodyFontFeatures.current
    val scale = LocalScale.current
    val shape = RoundedCornerShape(16.dp)
    Card(
        modifier = modifier
            .clip(shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .drawBehind {
                fillFraction?.let { f ->
                    val h = size.height * f.coerceIn(0f, 1f)
                    val top = size.height - h
                    drawRect(
                        Brush.verticalGradient(listOf(SkyBlue.copy(alpha = 0f), SkyBlue.copy(alpha = 0.1f), SkyBlue.copy(alpha = 0.2f)), startY = top, endY = size.height),
                        topLeft = Offset(0f, top), size = Size(size.width, h),
                    )
                }
            },
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = TileSurface)
    ) {
        Box {
        cornerContent?.let {
            Box(modifier = Modifier.align(Alignment.TopEnd).padding(10.dp)) { it() }
        }
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
                    // Clear of the corner graphic
                    modifier = if (cornerContent != null) Modifier.padding(end = 34.dp) else Modifier,
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
}

@Composable
private fun SunTile(sunrise: Long, sunset: Long, timeFormat: SimpleDateFormat, localizeDigits: (String) -> String, modifier: Modifier) {
    val bodyFont = LocalBodyFont.current
    val scale = LocalScale.current
    Column(
        modifier = modifier.background(TileSurface, RoundedCornerShape(16.dp)).padding(16.sd(scale)),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = stringResource(R.string.detail_sun),
            fontSize = 13f.s(scale),
            fontFamily = bodyFont,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )
        SunArc(sunrise, sunset, System.currentTimeMillis() / 1000,
            Modifier.fillMaxWidth().heightIn(max = 64.dp).aspectRatio(120f / 52f))
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf(R.string.detail_sunrise to sunrise, R.string.detail_sunset to sunset).forEachIndexed { i, (label, time) ->
                if (i == 1) Spacer(modifier = Modifier.weight(1f))
                Column(horizontalAlignment = if (i == 0) Alignment.Start else Alignment.End) {
                    Text(
                        text = stringResource(label),
                        fontSize = 11f.s(scale),
                        fontFamily = bodyFont,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    )
                    Text(
                        text = localizeDigits(timeFormat.format(Date(time * 1000))),
                        fontSize = 15f.s(scale),
                        fontWeight = FontWeight.Bold,
                        fontFamily = LocalDisplayFont.current,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

/** The five UV tiers as coloured dots, the current one ringed. */
@Composable
private fun UvTierDots(uvIndex: Double) {
    val current = when {
        uvIndex < 3 -> 0
        uvIndex < 6 -> 1
        uvIndex < 8 -> 2
        uvIndex < 11 -> 3
        else -> 4
    }
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        listOf(1.0, 4.0, 7.0, 9.0, 11.0).forEachIndexed { i, sample ->
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(colorsFor(sample).background.copy(alpha = if (i == current) 1f else 0.3f), CircleShape)
                    .then(if (i == current) Modifier.border(2.dp, Color.White.copy(alpha = 0.8f), CircleShape) else Modifier),
            )
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

/** Grid icon beside the coordinates; tapping it shows the point's Plus Code (a grid cell) in a
 * tooltip, holding the icon or the code copies it. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun PlusCodeInfo(lat: Double, lon: Double, modifier: Modifier = Modifier) {
    val code = remember(lat, lon) { PlusCode.encode(lat, lon) }
    val tooltipState = rememberTooltipState(isPersistent = true)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val copiedMessage = stringResource(R.string.plus_code_copied)
    val copy = {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Plus Code", code))
        // Android 13+ confirms clipboard writes itself
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) {
            android.widget.Toast.makeText(context, copiedMessage, android.widget.Toast.LENGTH_SHORT).show()
        }
    }
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = {
            PlainTooltip {
                // Holding the code copies it
                Text(
                    "Plus Code  $code",
                    style = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr),
                    modifier = Modifier.combinedClickable(onClick = {}, onLongClick = copy),
                )
            }
        },
        state = tooltipState,
        modifier = modifier,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_grid),
            contentDescription = stringResource(R.string.cd_plus_code),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            modifier = Modifier
                .clip(CircleShape)
                .combinedClickable(
                    role = Role.Button,
                    onClick = { scope.launch { tooltipState.show() } },
                    onLongClick = copy,
                )
                .padding(4.dp)
                .size(18.dp),
        )
    }
}
