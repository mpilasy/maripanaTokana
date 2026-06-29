package orinasa.njarasoa.maripanatokana.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import orinasa.njarasoa.maripanatokana.domain.model.GeocodingSource
import orinasa.njarasoa.maripanatokana.domain.model.WeatherSource


@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsState()
    val pendingApiKey by viewModel.pendingApiKey.collectAsState()
    val testState by viewModel.testState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0E0B3D), Color(0xFF1A1565))
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
            }
            Spacer(Modifier.width(8.dp))
            Text("Settings", color = MaterialTheme.colorScheme.onSurface, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(24.dp))

        // — Weather Source —
        SectionHeader("Weather Source")
        Column(Modifier.selectableGroup()) {
            WeatherSource.entries.forEach { source ->
                val selected = settings.weatherSource == source
                Row(
                    Modifier
                        .fillMaxWidth()
                        .selectable(selected = selected, onClick = { viewModel.updateWeatherSource(source) }, role = Role.RadioButton)
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = selected,
                        onClick = null,
                        colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.onSurface, unselectedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)),
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(source.displayName(), color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp)
                        if (source.requiresApiKey()) {
                            Text("Requires API key", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 12.sp)
                        }
                    }
                }
            }
        }
        if (settings.weatherSource.requiresApiKey()) {
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = pendingApiKey,
                onValueChange = { viewModel.updatePendingApiKey(it) },
                label = { Text("API Key", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedBorderColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                ),
            )
            if (pendingApiKey.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        onClick = { viewModel.testApiKey() },
                        enabled = testState !is ApiKeyTestState.Loading,
                    ) {
                        if (testState is ApiKeyTestState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            )
                        } else {
                            Text("Test", color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    when (val state = testState) {
                        is ApiKeyTestState.Success ->
                            Text("✓ Saved", color = Color(0xFF66BB6A), fontSize = 13.sp)
                        is ApiKeyTestState.Failure ->
                            Text(state.message, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                        else -> {}
                    }
                }
            }
        }


        Spacer(Modifier.height(32.dp))

        // — Alerts —
        SectionHeader("Alerts")
        Row(
            Modifier.fillMaxWidth().padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Show weather alerts", color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp, modifier = Modifier.weight(1f))
            Switch(
                checked = settings.alertsEnabled,
                onCheckedChange = { viewModel.updateAlertsEnabled(it) },
                colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.onSurface, checkedTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)),
            )
        }
        if (settings.alertsEnabled) {
            AlertCheckRow("NWS alerts (USA)", settings.alertsNwsEnabled) { viewModel.updateAlertsNwsEnabled(it) }
            AlertCheckRow("GDACS alerts (global disasters)", settings.alertsGdacsEnabled) { viewModel.updateAlertsGdacsEnabled(it) }
            AlertCheckRow("MeteoAlarm (Europe)", settings.alertsMeteoAlarmEnabled) { viewModel.updateAlertsMeteoAlarmEnabled(it) }
            AlertCheckRow("JMA (Japan)", settings.alertsJmaEnabled) { viewModel.updateAlertsJmaEnabled(it) }
            AlertCheckRow("ECCC (Canada)", settings.alertsEcccEnabled) { viewModel.updateAlertsEcccEnabled(it) }
            AlertCheckRow("BOM (Australia)", settings.alertsBomEnabled) { viewModel.updateAlertsBomEnabled(it) }
            AlertCheckRow("NHC (Atlantic & Pacific hurricanes)", settings.alertsNhcEnabled) { viewModel.updateAlertsNhcEnabled(it) }
            AlertCheckRow("WMO SWIC (global)", settings.alertsWmoSwicEnabled) { viewModel.updateAlertsWmoSwicEnabled(it) }
            AlertCheckRow("Derived alerts (from weather codes)", settings.alertsDerivedEnabled) { viewModel.updateAlertsDerivedEnabled(it) }
        }

        Spacer(Modifier.height(32.dp))

        // — Geocoding Source —
        SectionHeader("Location / Geocoding")
        Column(Modifier.selectableGroup()) {
            GeocodingSource.entries.forEach { source ->
                val selected = settings.geocodingSource == source
                Row(
                    Modifier
                        .fillMaxWidth()
                        .selectable(selected = selected, onClick = { viewModel.updateGeocodingSource(source) }, role = Role.RadioButton)
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = selected,
                        onClick = null,
                        colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.onSurface, unselectedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)),
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(source.displayName(), color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp)
                        if (source == GeocodingSource.NOMINATIM) {
                            Text("Works on all builds including F-Droid, no API key required", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(title.uppercase(), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun AlertCheckRow(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = 2.dp, bottom = 2.dp)
            .selectable(selected = checked, onClick = { onChecked(!checked) }, role = Role.Checkbox),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = null,
            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.onSurface, uncheckedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), checkmarkColor = Color.Black),
        )
        Spacer(Modifier.width(12.dp))
        Text(label, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 14.sp)
    }
}

private fun WeatherSource.displayName() = when (this) {
    WeatherSource.OPEN_METEO -> "Open-Meteo (default)"
    WeatherSource.PIRATE_WEATHER -> "Pirate Weather"
}

private fun WeatherSource.requiresApiKey() = this == WeatherSource.PIRATE_WEATHER

private fun GeocodingSource.displayName() = when (this) {
    GeocodingSource.SYSTEM_GEOCODER -> "System Geocoder"
    GeocodingSource.NOMINATIM -> "Nominatim (OpenStreetMap)"
}
