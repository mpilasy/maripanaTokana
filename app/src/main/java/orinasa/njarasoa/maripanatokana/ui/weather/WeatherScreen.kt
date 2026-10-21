package orinasa.njarasoa.maripanatokana.ui.weather

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
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
import orinasa.njarasoa.maripanatokana.ui.settings.AboutScreen
import orinasa.njarasoa.maripanatokana.ui.settings.HelpScreen
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import orinasa.njarasoa.maripanatokana.R
import orinasa.njarasoa.maripanatokana.ui.permission.PermissionHandler
import orinasa.njarasoa.maripanatokana.ui.theme.LocalBodyFont
import orinasa.njarasoa.maripanatokana.ui.theme.LocalBodyFontFeatures
import orinasa.njarasoa.maripanatokana.ui.theme.LocalDisplayFont
import orinasa.njarasoa.maripanatokana.ui.theme.buildTypography
import orinasa.njarasoa.maripanatokana.ui.theme.fontPairings
import orinasa.njarasoa.maripanatokana.ui.weather.components.RenameLocationDialog
import orinasa.njarasoa.maripanatokana.ui.weather.components.SavedLocationsDialog
import androidx.compose.runtime.saveable.rememberSaveable
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun WeatherScreen(
    viewModel: WeatherViewModel = hiltViewModel(),
    permissionHandler: PermissionHandler
) {
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var showAbout by rememberSaveable { mutableStateOf(false) }
    var showHelp by rememberSaveable { mutableStateOf(false) }
    // Hoisted so the main screen's scroll position survives a trip through Settings/About.
    val mainScrollState = rememberSaveable(saver = ScrollState.Saver) { ScrollState(0) }

    // App language, font and layout direction for every screen and dialog: the Activity's own
    // resources keep the language it was launched with, so anything outside these providers
    // stays in that language after a mid-session switch.
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
        if (showHelp) {
            HelpScreen(onBack = { showHelp = false }, languageFlag = supportedLocales[localeIndex].flag, onCycleLanguage = viewModel::cycleLanguage)
        } else if (showAbout) {
            AboutScreen(onBack = { showAbout = false }, onOpenHelp = { showHelp = true }, languageFlag = supportedLocales[localeIndex].flag, onCycleLanguage = viewModel::cycleLanguage)
        } else if (showSettings) {
            SettingsScreen(onBack = { showSettings = false }, onOpenAbout = { showAbout = true })
        } else {
            WeatherMainScreen(
                viewModel = viewModel,
                permissionHandler = permissionHandler,
                scrollState = mainScrollState,
                onOpenSettings = { showSettings = true },
                onOpenAbout = { showAbout = true },
                onOpenHelp = { showHelp = true },
            )
        }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WeatherMainScreen(
    viewModel: WeatherViewModel,
    permissionHandler: PermissionHandler,
    scrollState: ScrollState,
    onOpenSettings: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenHelp: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val refreshFailed by viewModel.refreshFailed.collectAsState()
    val refreshError by viewModel.refreshError.collectAsState()
    val advancedModeActive by viewModel.advancedModeActive.collectAsState()
    val metricPrimary by viewModel.metricPrimary.collectAsState()
    val fontIndex by viewModel.fontIndex.collectAsState()
    val localeIndex by viewModel.localeIndex.collectAsState()

    val pairing = fontPairings[fontIndex]
    val baseContext = LocalContext.current
    val currentConfig = LocalConfiguration.current
    val requestPermission = permissionHandler.rememberPermissionRequester {
        viewModel.fetchWeather()
    }
    val isPermissionGranted = permissionHandler.isPermissionGranted()
    val isPermanentlyDenied = permissionHandler.isPermissionPermanentlyDenied()
    LaunchedEffect(Unit) {
        // Auto-request the dialog on first composition only if the dialog can still be shown.
        if (!isPermissionGranted && !isPermanentlyDenied) requestPermission()
        // A picked place needs no location access: show it even when permission is missing
        if (!isPermissionGranted && viewModel.showsChosenPlace()) viewModel.fetchWeather()
    }

    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val advancedOverrideLat by viewModel.advancedOverrideLat.collectAsStateWithLifecycle()
    val advancedOverrideLon by viewModel.advancedOverrideLon.collectAsStateWithLifecycle()

    val showSavedLocationsDialog by viewModel.showSavedLocationsDialog.collectAsStateWithLifecycle()
    val savedLocations by viewModel.savedLocations.collectAsStateWithLifecycle()
    val activeLocationId by viewModel.activeLocationId.collectAsStateWithLifecycle()

    // Swipe-to-cycle-locations: Current Location + saved locations, in list order, no wraparound.
    val locationCycle = remember(savedLocations) { listOf<String?>(null) + savedLocations.map { it.id } }
    val locationCycleIndex = locationCycle.indexOf(activeLocationId)

    if (showSavedLocationsDialog) {
        SavedLocationsDialog(
            searchResults = searchResults,
            onDismissRequest = { viewModel.setShowSavedLocationsDialog(false) },
            onSelectSearchResult = { result ->
                // With weather on screen a pick is a preview; without (no location) it is saved, so it stays
                if (uiState is WeatherUiState.Success) viewModel.setLocationOverride(result.latitude, result.longitude, result.displayName())
                else viewModel.addSavedLocation(result)
            },
            searchQuery = { viewModel.searchLocation(it) },
        )
    }

    var showRenameDialog by remember { mutableStateOf(false) }
    val activeSavedLocation = savedLocations.find { it.id == activeLocationId }
    if (showRenameDialog && activeSavedLocation != null) {
        RenameLocationDialog(
            location = activeSavedLocation,
            onDismissRequest = { showRenameDialog = false },
            onRename = { label ->
                viewModel.renameSavedLocation(activeSavedLocation.id, label)
                showRenameDialog = false
            },
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

    // Switching location or refreshing via Loading used to dispose the content (and its scroll); keep that.
    val isLoading = uiState is WeatherUiState.Loading
    LaunchedEffect(isLoading) { if (isLoading) scrollState.scrollTo(0) }

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
                        Spacer(modifier = Modifier.height(12.dp))
                        val appChoosePlace = stringResource(R.string.choose_place)
                        val sysChoosePlace = systemContext.getString(R.string.choose_place)
                        OutlinedButton(onClick = { viewModel.onManageLocationsClicked() }) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(appChoosePlace, color = MaterialTheme.colorScheme.onSurface)
                                if (sysChoosePlace != appChoosePlace) {
                                    Text(
                                        text = sysChoosePlace,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
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
                        val weatherSource by viewModel.weatherSource.collectAsStateWithLifecycle()
                        WeatherContent(
                            data = state.data,
                            metricPrimary = metricPrimary,
                            refreshFailed = refreshFailed,
                            refreshError = refreshError,
                            advancedMode = advancedModeActive,
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
                            onLocationLongClicked = if (activeSavedLocation != null && advancedOverrideLat == null) {
                                { showRenameDialog = true }
                            } else null,
                            onManageLocationsClicked = viewModel::onManageLocationsClicked,
                            onOpenSettings = onOpenSettings,
                            onOpenAbout = onOpenAbout,
                            onOpenHelp = onOpenHelp,
                            scrollState = scrollState,
                            weatherSource = weatherSource,
                            isSavedLocation = activeLocationId != null,
                            onGoToCurrentLocation = { viewModel.switchToLocation(null) },
                            onToggleFavorite = {
                                if (activeLocationId != null) viewModel.unfavoriteCurrentLocation()
                                else viewModel.favoriteCurrentLocation()
                            },
                            onSwipeToNextLocation = {
                                if (locationCycleIndex in 0 until locationCycle.lastIndex) {
                                    viewModel.switchToLocation(locationCycle[locationCycleIndex + 1])
                                }
                            },
                            onSwipeToPreviousLocation = {
                                if (locationCycleIndex > 0) {
                                    viewModel.switchToLocation(locationCycle[locationCycleIndex - 1])
                                }
                            },
                            canSwipeToNext = locationCycleIndex in 0 until locationCycle.lastIndex,
                            canSwipeToPrevious = locationCycleIndex > 0,
                            locationCount = locationCycle.size,
                            locationIndex = locationCycleIndex,
                            locationNames = listOf<String?>(null) + savedLocations.map { it.displayName },
                            onSelectLocation = { i ->
                                if (i != locationCycleIndex) viewModel.switchToLocation(locationCycle[i])
                            },
                            hasLocationOverride = advancedOverrideLat != null,
                            advancedOverrideLat = advancedOverrideLat,
                            advancedOverrideLon = advancedOverrideLon,
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
                        Spacer(modifier = Modifier.height(12.dp))
                        val appChoosePlace = stringResource(R.string.choose_place)
                        val sysChoosePlace = systemContext.getString(R.string.choose_place)
                        OutlinedButton(onClick = { viewModel.onManageLocationsClicked() }) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(appChoosePlace, color = MaterialTheme.colorScheme.onSurface)
                                if (sysChoosePlace != appChoosePlace) {
                                    Text(
                                        text = sysChoosePlace,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
}
