package orinasa.njarasoa.maripanatokana.ui.weather

import android.Manifest
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import orinasa.njarasoa.maripanatokana.R
import orinasa.njarasoa.maripanatokana.data.remote.wmoDescription
import orinasa.njarasoa.maripanatokana.data.remote.wmoEmoji
import orinasa.njarasoa.maripanatokana.domain.model.DailyForecast
import orinasa.njarasoa.maripanatokana.domain.model.HourlyForecast
import orinasa.njarasoa.maripanatokana.domain.model.WeatherData
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun WeatherScreen(
    viewModel: WeatherViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val metricPrimary by viewModel.metricPrimary.collectAsState()

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
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Location Permission Required",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "This app needs location access to show weather for your area.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = { locationPermissionsState.launchMultiplePermissionRequest() }) {
                        Text("Grant Permission")
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
                        onToggleUnits = { viewModel.toggleUnits() },
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
                        text = "Error",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = { viewModel.fetchWeather() }) {
                        Text("Retry")
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
    primarySize: TextUnit = 16.sp,
    color: Color = Color.White,
) {
    Column {
        Text(
            text = primary,
            fontSize = primarySize,
            fontWeight = FontWeight.Bold,
            color = color,
        )
        Text(
            text = secondary,
            fontSize = primarySize * 0.75f,
            fontWeight = FontWeight.Normal,
            color = color.copy(alpha = 0.45f),
        )
    }
}

@Composable
private fun CollapsibleSection(
    title: String,
    initialExpanded: Boolean = false,
    content: @Composable () -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(initialExpanded) }
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, label = "chevron")

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
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.rotate(rotation),
            )
        }
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically(),
        ) {
            content()
        }
    }
}

@Composable
private fun WeatherContent(
    data: WeatherData,
    metricPrimary: Boolean,
    onToggleUnits: () -> Unit,
) {
    val dateFormat = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault())
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        // Header with toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = data.locationName,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = dateFormat.format(Date(data.timestamp)),
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Text(
                    text = "Updated ${timeFormat.format(Date(data.timestamp))}",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.4f)
                )
            }
            FilledTonalButton(onClick = onToggleUnits) {
                Text(if (metricPrimary) "\u00B0C" else "\u00B0F")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Hero Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2A1FA5))
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                // Top row: temperature (left) + precipitation (right)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    val (tempPrimary, tempSecondary) = data.temperature.displayDual(metricPrimary)
                    DualUnitText(
                        primary = tempPrimary,
                        secondary = tempSecondary,
                        primarySize = 48.sp,
                    )
                    Column(horizontalAlignment = Alignment.End) {
                        if (data.snow != null) {
                            val (snowP, snowS) = data.snow.displayDual(metricPrimary)
                            DualUnitText(
                                primary = "\u2744\uFE0F $snowP",
                                secondary = snowS,
                            )
                        } else if (data.rain != null) {
                            val (rainP, rainS) = data.rain.displayDual(metricPrimary)
                            DualUnitText(
                                primary = "\uD83C\uDF27\uFE0F $rainP",
                                secondary = rainS,
                            )
                        } else {
                            Text(
                                text = "No precip",
                                fontSize = 14.sp,
                                color = Color.White.copy(alpha = 0.5f),
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Description
                Text(
                    text = "${wmoEmoji(data.weatherCode)} ${data.description}",
                    fontSize = 20.sp,
                    color = Color.White.copy(alpha = 0.9f)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom row: feels like (left) + wind (right)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text(
                            text = "Feels Like",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        val (flPrimary, flSecondary) = data.feelsLike.displayDual(metricPrimary)
                        DualUnitText(primary = flPrimary, secondary = flSecondary)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        val (windP, windS) = data.windSpeed.displayDual(metricPrimary)
                        DualUnitText(
                            primary = "\uD83D\uDCA8 $windP",
                            secondary = windS,
                        )
                        Text(
                            text = "${cardinalDirection(data.windDeg)} (${data.windDeg}\u00B0)",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.6f),
                        )
                    }
                }
            }
        }

        // Secondary panel: Min / Max
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Color(0xFF2A1FA5).copy(alpha = 0.4f),
                    RoundedCornerShape(12.dp),
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            val (minP, minS) = data.tempMin.displayDual(metricPrimary)
            val (maxP, maxS) = data.tempMax.displayDual(metricPrimary)
            DualUnitText(
                primary = "Min $minP  \u00B7  Max $maxP",
                secondary = "Min $minS  \u00B7  Max $maxS",
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Current Conditions (collapsible)
        CollapsibleSection(title = "Current Conditions") {
            DetailsContent(data, metricPrimary, timeFormat)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Hourly Forecast
        if (data.hourlyForecast.isNotEmpty()) {
            CollapsibleSection(title = "Hourly Forecast") {
                HourlyForecastRow(data.hourlyForecast, metricPrimary)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Weekly Forecast
        if (data.dailyForecast.isNotEmpty()) {
            CollapsibleSection(title = "This Week") {
                DailyForecastList(data.dailyForecast, metricPrimary)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Credits
        val linkStyle = SpanStyle(color = Color.White.copy(alpha = 0.5f), textDecoration = TextDecoration.Underline)
        val creditText = buildAnnotatedString {
            withStyle(SpanStyle(color = Color.White.copy(alpha = 0.3f))) {
                append("Weather data by ")
            }
            withLink(LinkAnnotation.Url("https://open-meteo.com")) {
                withStyle(linkStyle) { append("Open-Meteo") }
            }
            withStyle(SpanStyle(color = Color.White.copy(alpha = 0.3f))) {
                append("\nBuilt with ")
            }
            withLink(LinkAnnotation.Url("https://claude.ai")) {
                withStyle(linkStyle) { append("Claude") }
            }
        }
        Text(
            text = creditText,
            fontSize = 11.sp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun HourlyForecastRow(forecasts: List<HourlyForecast>, metricPrimary: Boolean) {
    val hourFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 8.dp),
    ) {
        items(forecasts) { item ->
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2A1FA5).copy(alpha = 0.6f)),
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = hourFormat.format(Date(item.time)),
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.7f),
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = wmoEmoji(item.weatherCode),
                        fontSize = 20.sp,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    val (tempP, tempS) = item.temperature.displayDual(metricPrimary)
                    DualUnitText(
                        primary = tempP,
                        secondary = tempS,
                        primarySize = 14.sp,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${item.precipProbability}%",
                        fontSize = 11.sp,
                        color = Color(0xFF64B5F6),
                    )
                }
            }
        }
    }
}

@Composable
private fun DailyForecastList(forecasts: List<DailyForecast>, metricPrimary: Boolean) {
    val dayFormat = SimpleDateFormat("EEEE", Locale.getDefault())

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        forecasts.forEach { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Color(0xFF2A1FA5).copy(alpha = 0.3f),
                        RoundedCornerShape(12.dp),
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = dayFormat.format(Date(item.date)),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    modifier = Modifier.width(100.dp),
                )
                Text(
                    text = "${wmoEmoji(item.weatherCode)} ${wmoDescription(item.weatherCode)}",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "${item.precipProbability}%",
                    fontSize = 11.sp,
                    color = Color(0xFF64B5F6),
                )
                Spacer(modifier = Modifier.width(8.dp))
                val (minP, minS) = item.tempMin.displayDual(metricPrimary)
                val (maxP, maxS) = item.tempMax.displayDual(metricPrimary)
                DualUnitText(
                    primary = "$minP / $maxP",
                    secondary = "$minS / $maxS",
                    primarySize = 13.sp,
                )
            }
        }
    }
}

@Composable
private fun DetailsContent(data: WeatherData, metricPrimary: Boolean, timeFormat: SimpleDateFormat) {
    Column {
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val (pressP, pressS) = data.pressure.displayDual(metricPrimary)
            DetailCard(
                title = "Pressure",
                value = pressP,
                secondaryValue = pressS,
                modifier = Modifier.weight(1f)
            )
            DetailCard(
                title = "Humidity",
                value = "${data.humidity}%",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            data.windGust?.let { gust ->
                val (gustP, gustS) = gust.displayDual(metricPrimary)
                DetailCard(
                    title = "Wind Gust",
                    value = gustP,
                    secondaryValue = gustS,
                    modifier = Modifier.weight(1f)
                )
            } ?: Spacer(modifier = Modifier.weight(1f))

            DetailCard(
                title = "Visibility",
                value = if (metricPrimary) "%.1f km".format(data.visibility / 1000.0)
                        else "%.2f mi".format(data.visibility / 1609.34),
                secondaryValue = if (metricPrimary) "%.2f mi".format(data.visibility / 1609.34)
                                 else "%.1f km".format(data.visibility / 1000.0),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            DetailCard(
                title = "Sunrise",
                value = timeFormat.format(Date(data.sunrise * 1000)),
                modifier = Modifier.weight(1f)
            )
            DetailCard(
                title = "Sunset",
                value = timeFormat.format(Date(data.sunset * 1000)),
                modifier = Modifier.weight(1f)
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
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A1FA5).copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (secondaryValue != null) {
                DualUnitText(primary = value, secondary = secondaryValue)
            } else {
                Text(
                    text = value,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            subtitle?.let {
                Text(
                    text = it,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
        }
    }
}

private fun cardinalDirection(degrees: Int): String {
    val directions = arrayOf("N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE",
        "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW")
    return directions[((degrees % 360 + 360) % 360 * 16 / 360) % 16]
}
