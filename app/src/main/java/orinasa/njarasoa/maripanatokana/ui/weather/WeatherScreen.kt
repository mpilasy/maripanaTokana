package orinasa.njarasoa.maripanatokana.ui.weather

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import orinasa.njarasoa.maripanatokana.ui.settings.SettingsScreen
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import orinasa.njarasoa.maripanatokana.R
import orinasa.njarasoa.maripanatokana.ui.permission.PermissionHandler
import orinasa.njarasoa.maripanatokana.ui.theme.LocalBodyFont
import orinasa.njarasoa.maripanatokana.ui.theme.LocalBodyFontFeatures
import orinasa.njarasoa.maripanatokana.ui.theme.LocalDisplayFont
import orinasa.njarasoa.maripanatokana.ui.theme.buildTypography
import orinasa.njarasoa.maripanatokana.ui.theme.fontPairings
import orinasa.njarasoa.maripanatokana.ui.weather.components.LocationOverrideDialog
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherScreen(
    viewModel: WeatherViewModel = hiltViewModel(),
    permissionHandler: PermissionHandler
) {
    var showSettings by remember { mutableStateOf(false) }

    if (showSettings) {
        SettingsScreen(onBack = { showSettings = false })
        return
    }

    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val metricPrimary by viewModel.metricPrimary.collectAsState()
    val fontIndex by viewModel.fontIndex.collectAsState()
    val localeIndex by viewModel.localeIndex.collectAsState()

    val pairing = fontPairings[fontIndex]
    val localeTag = supportedLocales[localeIndex].tag
    val baseContext = LocalContext.current
    val currentConfig = LocalConfiguration.current
    val localizedContext = remember(localeTag, baseContext, currentConfig) {
        val locale = java.util.Locale.forLanguageTag(localeTag)
        val config = android.content.res.Configuration(currentConfig)
        config.setLocale(locale)
        val resContext = baseContext.createConfigurationContext(config)
        object : android.content.ContextWrapper(baseContext) {
            override fun getResources() = resContext.resources
        }
    }

    val requestPermission = permissionHandler.rememberPermissionRequester {
        viewModel.fetchWeather()
    }
    val isPermissionGranted = permissionHandler.isPermissionGranted()
    val isPermanentlyDenied = permissionHandler.isPermissionPermanentlyDenied()
    LaunchedEffect(Unit) {
        // Auto-request the dialog on first composition only if the dialog can still be shown.
        if (!isPermissionGranted && !isPermanentlyDenied) requestPermission()
    }

    val showLocationDialog by viewModel.showLocationOverrideDialog.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val devModeActive by viewModel.devModeActive.collectAsStateWithLifecycle()
    val devOverrideLat by viewModel.devOverrideLat.collectAsStateWithLifecycle()
    val devOverrideLon by viewModel.devOverrideLon.collectAsStateWithLifecycle()

    if (showLocationDialog) {
        LocationOverrideDialog(
            onDismissRequest = { viewModel.setShowLocationOverrideDialog(false) },
            onLocationSelected = { lat, lon, name -> viewModel.setLocationOverride(lat, lon, name) },
            onResetToCurrentLocation = { viewModel.clearLocationOverride() },
            searchQuery = { viewModel.searchLocation(it) },
            searchResults = searchResults,
        )
    }

    // On resume: refresh stale data AND detect if the user revoked location in Settings.
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.refreshIfStale()
                if (viewModel.uiState.value is WeatherUiState.Error) viewModel.fetchWeather()
                val fineGranted = ContextCompat.checkSelfPermission(
                    baseContext, Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
                val coarseGranted = ContextCompat.checkSelfPermission(
                    baseContext, Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
                if (!fineGranted && !coarseGranted) {
                    viewModel.onPermissionRevoked()
                }
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
        LocalConfiguration provides localizedContext.resources.configuration,
        LocalDisplayFont provides pairing.display,
        LocalBodyFont provides pairing.body,
        LocalBodyFontFeatures provides pairing.bodyFontFeatures,
        androidx.compose.ui.platform.LocalLayoutDirection provides layoutDirection,
    ) {
        MaterialTheme(typography = buildTypography(pairing)) {
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

            // Device UI locale — uses Resources.getSystem() so it reflects the actual OS
            // language regardless of any app-level locale override.
            val systemLocale: java.util.Locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                android.content.res.Resources.getSystem().configuration.locales[0]
            } else {
                @Suppress("DEPRECATION")
                android.content.res.Resources.getSystem().configuration.locale
            }

            when (val state = uiState) {
                is WeatherUiState.Loading -> {
                    val acquiringStrings = remember {
                        listOf(
                            "Aiza isika?",       // mg
                            "أين نحن؟",          // ar
                            "Where are we?",     // en
                            "¿Dónde estamos?",   // es
                            "Où sommes-nous ?",  // fr
                            "हम कहाँ हैं?",       // hi
                            "हामी कहाँ छौं?",     // ne
                            "我们在哪里？",          // zh
                        )
                    }
                    var labelIndex by remember { mutableIntStateOf(0) }
                    var elapsedSeconds by remember { mutableIntStateOf(0) }
                    LaunchedEffect(Unit) {
                        while (true) {
                            delay(1000L)
                            elapsedSeconds++
                            if (elapsedSeconds % 2 == 0) {
                                labelIndex = (labelIndex + 1) % acquiringStrings.size
                            }
                        }
                    }
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = acquiringStrings[labelIndex],
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${elapsedSeconds}s",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }

                is WeatherUiState.PermissionRequired -> {
                    // System-locale context (phone language, not app language)
                    val systemContext = remember(baseContext, currentConfig, systemLocale) {
                        val cfg = android.content.res.Configuration(currentConfig)
                        cfg.setLocale(systemLocale)
                        baseContext.createConfigurationContext(cfg)
                    }
                    val sysTitle = systemContext.getString(R.string.permission_title)
                    val sysMessage = if (isPermanentlyDenied)
                        systemContext.getString(R.string.permission_permanently_denied)
                    else
                        systemContext.getString(R.string.permission_message)
                    val sysButton = if (isPermanentlyDenied)
                        systemContext.getString(R.string.open_settings)
                    else
                        systemContext.getString(R.string.grant_permission)
                    val appTitle = stringResource(R.string.permission_title)
                    val appMessage = if (isPermanentlyDenied)
                        stringResource(R.string.permission_permanently_denied)
                    else
                        stringResource(R.string.permission_message)
                    val appButton = if (isPermanentlyDenied)
                        stringResource(R.string.open_settings)
                    else
                        stringResource(R.string.grant_permission)

                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = appTitle,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                        )
                        if (sysTitle != appTitle) {
                            Text(
                                text = sysTitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                textAlign = TextAlign.Center,
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = appMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        if (sysMessage != appMessage) {
                            Text(
                                text = sysMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        val ctx = LocalContext.current
                        Button(onClick = {
                            if (isPermanentlyDenied) {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", ctx.packageName, null)
                                }
                                ctx.startActivity(intent)
                            } else {
                                requestPermission()
                            }
                        }) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(appButton)
                                if (sysButton != appButton) {
                                    Text(
                                        text = sysButton,
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
                        val showGpsCoordinates by viewModel.showGpsCoordinates.collectAsStateWithLifecycle()
                        val weatherSource by viewModel.weatherSource.collectAsStateWithLifecycle()
                        WeatherContent(
                            data = state.data,
                            metricPrimary = metricPrimary,
                            fontName = pairing.name,
                            currentFlag = supportedLocales[localeIndex].flag,
                            localizeDigits = supportedLocales[localeIndex]::localizeDigits,
                            osTimeFormat = SimpleDateFormat(
                                if (android.text.format.DateFormat.is24HourFormat(baseContext)) "HH:mm" else "h:mm a",
                                Locale.US
                            ),
                            onToggleUnits = { viewModel.toggleUnits() },
                            onCycleFont = { viewModel.cycleFont() },
                            onCycleLanguage = { viewModel.cycleLanguage() },
                            onRefresh = { viewModel.refresh() },
                            onLocationClicked = viewModel::onLocationClicked,
                            onWeatherIconTapped = viewModel::onWeatherIconTapped,
                            onEditLocationClicked = viewModel::onEditLocationClicked,
                            onDisableDevMode = viewModel::disableDevMode,
                            onOpenSettings = { showSettings = true },
                            weatherSource = weatherSource,
                            showGpsCoordinates = showGpsCoordinates,
                            devModeActive = devModeActive,
                            devOverrideLat = devOverrideLat,
                            devOverrideLon = devOverrideLon,
                        )
                    }
                }

                is WeatherUiState.Error -> {
                    val systemContext = remember(baseContext, currentConfig, systemLocale) {
                        val cfg = android.content.res.Configuration(currentConfig)
                        cfg.setLocale(systemLocale)
                        baseContext.createConfigurationContext(cfg)
                    }
                    val sysTitle = systemContext.getString(R.string.error_title)
                    val sysMessage = systemContext.getString(state.messageResId)
                    val appTitle = stringResource(R.string.error_title)
                    val appMessage = stringResource(state.messageResId)

                    val lm = baseContext.getSystemService(android.content.Context.LOCATION_SERVICE) as LocationManager
                    val locationServicesOff = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        !lm.isLocationEnabled
                    } else {
                        !lm.isProviderEnabled(LocationManager.GPS_PROVIDER) &&
                        !lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
                    }

                    val ctx = LocalContext.current
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = appTitle,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                        )
                        if (sysTitle != appTitle) {
                            Text(
                                text = sysTitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                textAlign = TextAlign.Center,
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = appMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center,
                        )
                        if (sysMessage != appMessage) {
                            Text(
                                text = sysMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                textAlign = TextAlign.Center,
                            )
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        if (locationServicesOff) {
                            val sysOpenSettings = systemContext.getString(R.string.open_settings)
                            val appOpenSettings = stringResource(R.string.open_settings)
                            Button(onClick = {
                                ctx.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                            }) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(appOpenSettings)
                                    if (sysOpenSettings != appOpenSettings) {
                                        Text(
                                            text = sysOpenSettings,
                                            style = MaterialTheme.typography.labelSmall,
                                        )
                                    }
                                }
                            }
                        } else {
                            val appRetry = stringResource(R.string.error_retry)
                            val sysRetry = systemContext.getString(R.string.error_retry)
                            Button(onClick = { viewModel.fetchWeather() }) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(appRetry)
                                    if (sysRetry != appRetry) {
                                        Text(
                                            text = sysRetry,
                                            style = MaterialTheme.typography.labelSmall,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        } // end MaterialTheme(typography)
    }
}
