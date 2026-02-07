package orinasa.njarasoa.maripanatokana.ui.weather

import android.Manifest
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
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
                val (tempPrimary, tempSecondary) = data.temperature.displayDual(metricPrimary)
                DualUnitText(
                    primary = tempPrimary,
                    secondary = tempSecondary,
                    primarySize = 48.sp,
                )
                Text(
                    text = data.description,
                    fontSize = 20.sp,
                    color = Color.White.copy(alpha = 0.9f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
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
                    Column {
                        Text(
                            text = "Min / Max",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        val (minP, minS) = data.tempMin.displayDual(metricPrimary)
                        val (maxP, maxS) = data.tempMax.displayDual(metricPrimary)
                        DualUnitText(
                            primary = "$minP / $maxP",
                            secondary = "$minS / $maxS",
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Details Grid
        Text(
            text = "Details",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(16.dp))

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
            val (windP, windS) = data.windSpeed.displayDual(metricPrimary)
            DetailCard(
                title = "Wind",
                value = windP,
                secondaryValue = windS,
                subtitle = "${cardinalDirection(data.windDeg)} (${data.windDeg}\u00B0)",
                modifier = Modifier.weight(1f)
            )
            data.windGust?.let { gust ->
                val (gustP, gustS) = gust.displayDual(metricPrimary)
                DetailCard(
                    title = "Wind Gust",
                    value = gustP,
                    secondaryValue = gustS,
                    modifier = Modifier.weight(1f)
                )
            } ?: Spacer(modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            data.rain?.let { rain ->
                val (rainP, rainS) = rain.displayDual(metricPrimary)
                DetailCard(
                    title = "Rain (1h)",
                    value = rainP,
                    secondaryValue = rainS,
                    modifier = Modifier.weight(1f)
                )
            } ?: data.snow?.let { snow ->
                val (snowP, snowS) = snow.displayDual(metricPrimary)
                DetailCard(
                    title = "Snow (1h)",
                    value = snowP,
                    secondaryValue = snowS,
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

        Spacer(modifier = Modifier.height(24.dp))
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
