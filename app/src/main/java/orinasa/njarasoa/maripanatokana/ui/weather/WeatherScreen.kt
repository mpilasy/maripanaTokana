package orinasa.njarasoa.maripanatokana.ui.weather

import android.Manifest
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import orinasa.njarasoa.maripanatokana.BuildConfig
import orinasa.njarasoa.maripanatokana.R
import orinasa.njarasoa.maripanatokana.data.remote.wmoDescriptionRes
import orinasa.njarasoa.maripanatokana.data.remote.wmoEmoji
import orinasa.njarasoa.maripanatokana.domain.model.DailyForecast
import orinasa.njarasoa.maripanatokana.domain.model.HourlyForecast
import orinasa.njarasoa.maripanatokana.domain.model.WeatherData
import orinasa.njarasoa.maripanatokana.ui.theme.LocalBodyFont
import orinasa.njarasoa.maripanatokana.ui.theme.LocalDisplayFont
import orinasa.njarasoa.maripanatokana.ui.theme.fontPairings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val LocalScale = staticCompositionLocalOf { 1f }
private fun Float.s(scale: Float) = (this * scale).sp
private fun Int.sd(scale: Float) = (this * scale).dp

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun WeatherScreen(
    viewModel: WeatherViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val metricPrimary by viewModel.metricPrimary.collectAsState()
    val fontIndex by viewModel.fontIndex.collectAsState()
    val localeIndex by viewModel.localeIndex.collectAsState()

    val pairing = fontPairings[fontIndex]
    val localeTag = supportedLocales[localeIndex].tag
    val baseContext = LocalContext.current
    val localizedContext = remember(localeTag, baseContext) {
        val locale = java.util.Locale.forLanguageTag(localeTag)
        val config = android.content.res.Configuration(baseContext.resources.configuration)
        config.setLocale(locale)
        val resContext = baseContext.createConfigurationContext(config)
        object : android.content.ContextWrapper(baseContext) {
            override fun getResources() = resContext.resources
        }
    }

    val locationPermissionsState = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        )
    )

    LaunchedEffect(locationPermissionsState.allPermissionsGranted) {
        if (locationPermissionsState.allPermissionsGranted) {
            viewModel.fetchWeather()
        }
    }

    // Refresh when app comes to foreground if data is >30 min old
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.refreshIfStale()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val layoutDirection = if (androidx.core.text.TextUtilsCompat.getLayoutDirectionFromLocale(
            java.util.Locale.forLanguageTag(localeTag)
        ) == android.view.View.LAYOUT_DIRECTION_RTL) androidx.compose.ui.unit.LayoutDirection.Rtl
        else androidx.compose.ui.unit.LayoutDirection.Ltr

    CompositionLocalProvider(
        LocalContext provides localizedContext,
        LocalDisplayFont provides pairing.display,
        LocalBodyFont provides pairing.body,
        androidx.compose.ui.platform.LocalLayoutDirection provides layoutDirection,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0E0B3D),
                            Color(0xFF1A1565)
                        )
                    )
                )
        ) {
            // Blue Marble background
            Image(
                painter = painterResource(R.drawable.bg_blue_marble),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(0.12f),
            )

            when (val state = uiState) {
                is WeatherUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = Color.White
                    )
                }

                is WeatherUiState.PermissionRequired -> {
                    // System-locale context (phone language, not app language)
                    val systemContext = remember(baseContext) {
                        val cfg = android.content.res.Configuration(baseContext.resources.configuration)
                        cfg.setLocale(java.util.Locale.getDefault())
                        baseContext.createConfigurationContext(cfg)
                    }
                    val sysTitle = systemContext.getString(R.string.permission_title)
                    val sysMessage = systemContext.getString(R.string.permission_message)
                    val sysGrant = systemContext.getString(R.string.grant_permission)
                    val appTitle = stringResource(R.string.permission_title)
                    val appMessage = stringResource(R.string.permission_message)
                    val appGrant = stringResource(R.string.grant_permission)

                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = appTitle,
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                        )
                        if (sysTitle != appTitle) {
                            Text(
                                text = sysTitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.5f),
                                textAlign = TextAlign.Center,
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = appMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        if (sysMessage != appMessage) {
                            Text(
                                text = sysMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.4f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(onClick = { locationPermissionsState.launchMultiplePermissionRequest() }) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(appGrant)
                                if (sysGrant != appGrant) {
                                    Text(
                                        text = sysGrant,
                                        style = MaterialTheme.typography.labelSmall,
                                    )
                                }
                            }
                        }
                    }
                }

                is WeatherUiState.Success -> {
                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = { viewModel.refresh() },
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        WeatherContent(
                            data = state.data,
                            metricPrimary = metricPrimary,
                            fontName = pairing.name,
                            currentFlag = supportedLocales[localeIndex].flag,
                            localizeDigits = supportedLocales[localeIndex]::localizeDigits,
                            osTimeFormat = android.text.format.DateFormat.getTimeFormat(baseContext),
                            onToggleUnits = { viewModel.toggleUnits() },
                            onCycleFont = { viewModel.cycleFont() },
                            onCycleLanguage = { viewModel.cycleLanguage() },
                        )
                    }
                }

                is WeatherUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.error_title),
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(state.messageResId),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(onClick = { viewModel.fetchWeather() }) {
                            Text(stringResource(R.string.error_retry))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DualUnitText(
    primary: String,
    secondary: String,
    primarySize: TextUnit = 16f.s(LocalScale.current),
    color: Color = Color.White,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    onClick: (() -> Unit)? = null,
) {
    val displayFont = LocalDisplayFont.current
    Column(
        horizontalAlignment = horizontalAlignment,
        modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    ) {
        Text(
            text = primary,
            fontSize = primarySize,
            fontWeight = FontWeight.Bold,
            fontFamily = displayFont,
            color = color,
        )
        Text(
            text = secondary,
            fontSize = primarySize * 0.75f,
            fontWeight = FontWeight.Normal,
            fontFamily = displayFont,
            color = color.copy(alpha = 0.55f),
        )
    }
}

@Composable
private fun CollapsibleSection(
    title: String,
    headerGraphicsLayer: GraphicsLayer,
    initialExpanded: Boolean = false,
    content: @Composable () -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(initialExpanded) }
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
                .clickable { expanded = !expanded }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                fontSize = 20f.s(scale),
                fontWeight = FontWeight.Bold,
                fontFamily = bodyFont,
                color = Color.White,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = if (expanded) stringResource(R.string.cd_collapse)
                                     else stringResource(R.string.cd_expand),
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.rotate(rotation),
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
                        contentColor = Color.White.copy(alpha = 0.4f),
                    ),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_share),
                        contentDescription = stringResource(R.string.cd_share),
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
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
private fun WeatherContent(
    data: WeatherData,
    metricPrimary: Boolean,
    fontName: String,
    currentFlag: String,
    localizeDigits: (String) -> String,
    osTimeFormat: java.text.DateFormat,
    onToggleUnits: () -> Unit,
    onCycleFont: () -> Unit,
    onCycleLanguage: () -> Unit,
) {
    val context = LocalContext.current
    val appLocale = context.resources.configuration.locales[0]
    val dateFormat = SimpleDateFormat("EEEE, d MMMM yyyy", appLocale)
    val timeFormat = SimpleDateFormat("HH:mm", Locale.US)
    val displayFont = LocalDisplayFont.current
    val bodyFont = LocalBodyFont.current

    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val scale = when {
        screenWidthDp >= 400 -> 1f
        screenWidthDp >= 320 -> 0.8f
        else -> 0.7f
    }

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
            Text(
                text = data.locationName,
                fontSize = 32f.s(scale),
                fontWeight = FontWeight.Bold,
                fontFamily = displayFont,
                color = Color.White
            )
            Text(
                text = localizeDigits(dateFormat.format(Date(data.timestamp))),
                fontSize = 16f.s(scale),
                fontFamily = bodyFont,
                color = Color.White.copy(alpha = 0.7f)
            )
            Text(
                text = localizeDigits(stringResource(R.string.updated_time, osTimeFormat.format(Date(data.timestamp)))),
                fontSize = 12f.s(scale),
                fontFamily = bodyFont,
                color = Color.White.copy(alpha = 0.4f)
            )
        }

        // Scrollable content
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 24.sd(scale))
        ) {
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
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2A1FA5))
                ) {
                    Column(
                        modifier = Modifier.padding(24.sd(scale))
                    ) {
                        // Top row: emoji+description (left) + temperature (right)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
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
                                    color = Color.White.copy(alpha = 0.9f),
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
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column {
                                Text(
                                    text = stringResource(R.string.feels_like),
                                    fontSize = 14f.s(scale),
                                    fontFamily = bodyFont,
                                    color = Color.White.copy(alpha = 0.7f)
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
                                        color = Color.White.copy(alpha = 0.5f),
                                    )
                                }
                            }
                        }
                    }
                    Text(
                        text = "\u00A9 Orinasa Njarasoa",
                        fontSize = 9f.s(scale),
                        lineHeight = 11f.s(scale),
                        fontFamily = bodyFont,
                        color = Color.White.copy(alpha = 0.2f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 7.dp),
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
                        .align(Alignment.TopStart)
                        .padding(4.dp)
                        .size(32.dp),
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = Color.White.copy(alpha = 0.6f),
                    ),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_share),
                        contentDescription = stringResource(R.string.cd_share),
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.sd(scale)))

            // Hourly Forecast
            if (data.hourlyForecast.isNotEmpty()) {
                CollapsibleSection(title = stringResource(R.string.section_hourly_forecast), headerGraphicsLayer = headerGraphicsLayer, initialExpanded = true) {
                    HourlyForecastRow(data.hourlyForecast, metricPrimary, data.dailySunrise, data.dailySunset, localizeDigits, onToggleUnits)
                }
                Spacer(modifier = Modifier.height(24.sd(scale)))
            }

            // Weekly Forecast
            if (data.dailyForecast.isNotEmpty()) {
                CollapsibleSection(title = stringResource(R.string.section_this_week), headerGraphicsLayer = headerGraphicsLayer) {
                    DailyForecastList(data.dailyForecast, metricPrimary, localizeDigits, onToggleUnits)
                }
                Spacer(modifier = Modifier.height(24.sd(scale)))
            }

            // Current Conditions (collapsible)
            CollapsibleSection(title = stringResource(R.string.section_current_conditions), headerGraphicsLayer = headerGraphicsLayer) {
                DetailsContent(data, metricPrimary, timeFormat, localizeDigits, onToggleUnits)
            }
        }

        // Fixed footer
        val linkStyle = SpanStyle(color = Color.White.copy(alpha = 0.5f), textDecoration = TextDecoration.Underline)
        val creditText = buildAnnotatedString {
            withStyle(SpanStyle(color = Color.White.copy(alpha = 0.3f))) {
                append(buildString {
                    append(stringResource(R.string.credits_weather_data))
                    append(" ")
                })
            }
            withLink(LinkAnnotation.Url("https://open-meteo.com")) {
                withStyle(linkStyle) { append("Open-Meteo") }
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
                    modifier = Modifier.clickable(onClick = onCycleFont),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_font),
                        contentDescription = stringResource(R.string.cd_change_font),
                        modifier = Modifier.size(14.dp),
                        tint = Color.White.copy(alpha = 0.4f),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = fontName.replace(" + ", "\n"),
                        fontSize = 9f.s(scale),
                        lineHeight = 11f.s(scale),
                        fontFamily = bodyFont,
                        color = Color.White.copy(alpha = 0.4f),
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                // Credits + hash centered
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = creditText, fontSize = 9f.s(scale), lineHeight = 11f.s(scale))
                    Text(
                        text = "${stringResource(R.string.hash_version, BuildConfig.GIT_HASH)}${if (BuildConfig.DEBUG) "-d" else ""} \u2022 ${BuildConfig.BUILD_TIME}",
                        fontSize = 9f.s(scale),
                        lineHeight = 11f.s(scale),
                        color = Color.White.copy(alpha = 0.25f),
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                // Language flag
                Text(
                    text = currentFlag,
                    fontSize = 16f.s(scale),
                    modifier = Modifier.clickable(onClick = onCycleLanguage),
                )
            }
        }
    }
    } // end CompositionLocalProvider(LocalScale)
}

@Composable
private fun HourlyForecastRow(forecasts: List<HourlyForecast>, metricPrimary: Boolean, dailySunrise: List<Long>, dailySunset: List<Long>, localizeDigits: (String) -> String, onToggleUnits: () -> Unit) {
    val hourFormat = SimpleDateFormat("HH:mm", Locale.US)
    val bodyFont = LocalBodyFont.current
    val scale = LocalScale.current

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.sd(scale)),
        contentPadding = PaddingValues(vertical = 8.dp),
    ) {
        items(forecasts) { item ->
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2A1FA5).copy(alpha = 0.6f)),
            ) {
                Column(
                    modifier = Modifier.padding(12.sd(scale)),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = localizeDigits(hourFormat.format(Date(item.time))),
                        fontSize = 12f.s(scale),
                        fontFamily = bodyFont,
                        color = Color.White.copy(alpha = 0.7f),
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = run {
                            val dayIdx = dailySunrise.indexOfLast { it <= item.time }.coerceAtLeast(0)
                            val sr = dailySunrise.getOrElse(dayIdx) { 0L }
                            val ss = dailySunset.getOrElse(dayIdx) { 0L }
                            wmoEmoji(item.weatherCode, isNight = item.time !in sr..ss)
                        },
                        fontSize = 20f.s(scale),
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    val (tempP, tempS) = item.temperature.displayDual(metricPrimary)
                    DualUnitText(
                        primary = localizeDigits(tempP),
                        secondary = localizeDigits(tempS),
                        primarySize = 14f.s(scale),
                        onClick = onToggleUnits,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (item.precipProbability > 0) localizeDigits("%d%%".format(Locale.US, item.precipProbability)) else "",
                        fontSize = 11f.s(scale),
                        fontFamily = bodyFont,
                        color = Color(0xFF64B5F6),
                    )
                }
            }
        }
    }
}

@Composable
private fun DailyForecastList(forecasts: List<DailyForecast>, metricPrimary: Boolean, localizeDigits: (String) -> String, onToggleUnits: () -> Unit) {
    val appLocale = LocalContext.current.resources.configuration.locales[0]
    val dayFormat = SimpleDateFormat("EEEE", appLocale)
    val dayMonthFormat = SimpleDateFormat("d MMM", appLocale)
    val bodyFont = LocalBodyFont.current
    val scale = LocalScale.current

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        forecasts.forEach { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Color(0xFF2A1FA5).copy(alpha = 0.3f),
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
                        color = Color.White,
                    )
                    Text(
                        text = localizeDigits(dayMonthFormat.format(Date(item.date))),
                        fontSize = 10f.s(scale),
                        fontFamily = bodyFont,
                        color = Color.White.copy(alpha = 0.4f),
                    )
                }
                Text(
                    text = "${wmoEmoji(item.weatherCode)} ${stringResource(wmoDescriptionRes(item.weatherCode))}",
                    fontSize = 12f.s(scale),
                    fontFamily = bodyFont,
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = if (item.precipProbability > 0) localizeDigits("%d%%".format(Locale.US, item.precipProbability)) else "",
                    fontSize = 11f.s(scale),
                    fontFamily = bodyFont,
                    color = Color(0xFF64B5F6),
                )
                Spacer(modifier = Modifier.width(8.dp))
                val (maxP, maxS) = item.tempMax.displayDual(metricPrimary)
                val (minP, minS) = item.tempMin.displayDual(metricPrimary)
                DualUnitText(
                    primary = localizeDigits("\u2191$maxP \u2193$minP"),
                    secondary = localizeDigits("\u2191$maxS \u2193$minS"),
                    primarySize = 13f.s(scale),
                    onClick = onToggleUnits,
                )
            }
        }
    }
}

@Composable
private fun DetailsContent(data: WeatherData, metricPrimary: Boolean, timeFormat: SimpleDateFormat, localizeDigits: (String) -> String, onToggleUnits: () -> Unit) {
    val directions = stringArrayResource(R.array.cardinal_directions)
    val uvLabels = stringArrayResource(R.array.uv_labels)
    val bodyFont = LocalBodyFont.current
    val scale = LocalScale.current

    Column {
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(16.sd(scale))
        ) {
            val (minP, minS) = data.tempMin.displayDual(metricPrimary)
            DetailCard(
                title = "\u2193 ${stringResource(R.string.detail_min_temp)}",
                value = localizeDigits(minP),
                secondaryValue = localizeDigits(minS),
                modifier = Modifier.weight(1f).fillMaxHeight(),
                onToggleUnits = onToggleUnits,
            )
            val (maxP, maxS) = data.tempMax.displayDual(metricPrimary)
            DetailCard(
                title = "\u2191 ${stringResource(R.string.detail_max_temp)}",
                value = localizeDigits(maxP),
                secondaryValue = localizeDigits(maxS),
                modifier = Modifier.weight(1f).fillMaxHeight(),
                onToggleUnits = onToggleUnits,
            )
        }

        Spacer(modifier = Modifier.height(16.sd(scale)))

        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(16.sd(scale))
        ) {
            val (windP, windS) = data.windSpeed.displayDual(metricPrimary)
            val dirIndex = ((data.windDeg % 360 + 360) % 360 * 16 / 360) % 16
            DetailCard(
                title = stringResource(R.string.detail_wind),
                value = localizeDigits(windP),
                secondaryValue = localizeDigits(windS),
                subtitle = localizeDigits("${directions[dirIndex]} (%d\u00B0)".format(Locale.US, data.windDeg)),
                modifier = Modifier.weight(1f).fillMaxHeight(),
                onToggleUnits = onToggleUnits,
            )
            data.windGust?.let { gust ->
                val (gustP, gustS) = gust.displayDual(metricPrimary)
                DetailCard(
                    title = stringResource(R.string.detail_wind_gust),
                    value = localizeDigits(gustP),
                    secondaryValue = localizeDigits(gustS),
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    onToggleUnits = onToggleUnits,
                )
            } ?: Spacer(modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(16.sd(scale)))

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
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2A1FA5).copy(alpha = 0.6f))
            ) {
                Column(modifier = Modifier.padding(16.sd(scale))) {
                    Text(
                        text = stringResource(R.string.detail_humidity),
                        fontSize = 14f.s(scale),
                        fontFamily = bodyFont,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = localizeDigits("%d%%".format(Locale.US, data.humidity)),
                        fontSize = 20f.s(scale),
                        fontWeight = FontWeight.Bold,
                        fontFamily = LocalDisplayFont.current,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    val displayFont = LocalDisplayFont.current
                    Text(
                        text = stringResource(R.string.detail_dewpoint),
                        fontSize = 12f.s(scale),
                        fontFamily = bodyFont,
                        color = Color.White.copy(alpha = 0.5f),
                    )
                    val dewText = buildAnnotatedString {
                        withStyle(SpanStyle(fontSize = 13f.s(scale), fontWeight = FontWeight.Bold, fontFamily = displayFont, color = Color.White)) {
                            append(localizeDigits(dewP))
                        }
                        withStyle(SpanStyle(fontSize = 12f.s(scale), fontFamily = displayFont, color = Color.White.copy(alpha = 0.55f))) {
                            append(" ${localizeDigits(dewS)}")
                        }
                    }
                    Text(
                        text = dewText,
                        modifier = if (onToggleUnits != null) Modifier.clickable(onClick = onToggleUnits) else Modifier,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.sd(scale)))

        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(16.sd(scale))
        ) {
            val uvLabelText = when {
                data.uvIndex < 3 -> uvLabels[0]
                data.uvIndex < 6 -> uvLabels[1]
                data.uvIndex < 8 -> uvLabels[2]
                data.uvIndex < 11 -> uvLabels[3]
                else -> uvLabels[4]
            }
            DetailCard(
                title = stringResource(R.string.detail_uv_index),
                value = localizeDigits("%.1f".format(Locale.US, data.uvIndex)),
                subtitle = uvLabelText,
                modifier = Modifier.weight(1f).fillMaxHeight()
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

        Spacer(modifier = Modifier.height(16.sd(scale)))

        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(16.sd(scale))
        ) {
            DetailCard(
                title = stringResource(R.string.detail_sunrise),
                value = localizeDigits(timeFormat.format(Date(data.sunrise * 1000))),
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
            DetailCard(
                title = stringResource(R.string.detail_sunset),
                value = localizeDigits(timeFormat.format(Date(data.sunset * 1000))),
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
        }
    }
}

@Composable
private fun DetailCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    secondaryValue: String? = null,
    subtitle: String? = null,
    onToggleUnits: (() -> Unit)? = null,
) {
    val bodyFont = LocalBodyFont.current
    val scale = LocalScale.current
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A1FA5).copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier.padding(16.sd(scale))
        ) {
            Text(
                text = title,
                fontSize = 14f.s(scale),
                fontFamily = bodyFont,
                color = Color.White.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (secondaryValue != null) {
                DualUnitText(primary = value, secondary = secondaryValue, primarySize = 20f.s(scale), onClick = onToggleUnits)
            } else {
                Text(
                    text = value,
                    fontSize = 20f.s(scale),
                    fontWeight = FontWeight.Bold,
                    fontFamily = LocalDisplayFont.current,
                    color = Color.White
                )
            }
            subtitle?.let {
                Text(
                    text = it,
                    fontSize = 12f.s(scale),
                    fontFamily = bodyFont,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
        }
    }
}

private fun combineBitmaps(header: Bitmap, content: Bitmap): Bitmap {
    val h = header.copy(Bitmap.Config.ARGB_8888, false)
    val c = content.copy(Bitmap.Config.ARGB_8888, false)
    val padding = 24
    val width = maxOf(h.width, c.width) + padding * 2
    val height = h.height + padding + c.height + padding * 2
    val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(result)
    canvas.drawColor(android.graphics.Color.parseColor("#0E0B3D"))
    canvas.drawBitmap(h, padding.toFloat(), padding.toFloat(), null)
    canvas.drawBitmap(c, padding.toFloat(), (h.height + padding * 2).toFloat(), null)
    return result
}

private suspend fun shareCardBitmap(context: android.content.Context, bitmap: Bitmap) {
    val uri = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "shared_images").also { it.mkdirs() }
        val file = File(dir, "weather.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, null))
}
