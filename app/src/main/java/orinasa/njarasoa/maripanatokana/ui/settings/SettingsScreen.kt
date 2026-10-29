// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

package orinasa.njarasoa.maripanatokana.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import orinasa.njarasoa.maripanatokana.ui.theme.Hairline
import orinasa.njarasoa.maripanatokana.ui.weather.components.LanguageFlagButton
import androidx.compose.foundation.layout.PaddingValues
import orinasa.njarasoa.maripanatokana.ui.theme.SectionSurface
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import orinasa.njarasoa.maripanatokana.R
import orinasa.njarasoa.maripanatokana.domain.model.GeocodingSource
import orinasa.njarasoa.maripanatokana.domain.model.WeatherSource


@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenAbout: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsState()
    val pendingApiKey by viewModel.pendingApiKey.collectAsState()
    val testState by viewModel.testState.collectAsState()

    SettingsPage(title = stringResource(R.string.settings_title), onBack = onBack) {
        // — Advanced Mode —
        SettingsGroup {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.settings_advanced_mode), color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    if (settings.advancedMode) stringResource(R.string.settings_advanced_on_desc)
                    else stringResource(R.string.settings_advanced_off_desc),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                )
            }
            Switch(
                checked = settings.advancedMode,
                onCheckedChange = { viewModel.updateAdvancedMode(it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onSurface,
                    checkedTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                ),
            )
        }
        }

        if (settings.advancedMode) {

        Spacer(Modifier.height(24.dp))

        // — Weather Source —
        SectionHeader(stringResource(R.string.settings_section_weather_source))
        SettingsGroup {
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
                            Text(stringResource(R.string.settings_requires_api_key), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 12.sp, lineHeight = 16.sp)
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
                label = { Text(stringResource(R.string.settings_api_key), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)) },
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
                            Text(stringResource(R.string.settings_test), color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    when (val state = testState) {
                        is ApiKeyTestState.Success ->
                            Text(stringResource(R.string.settings_saved), color = Color(0xFF66BB6A), fontSize = 13.sp)
                        is ApiKeyTestState.Failure ->
                            Text(if (state.code != null) stringResource(state.messageRes, state.code) else stringResource(state.messageRes), color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                        else -> {}
                    }
                }
            }
        }
        }

        Spacer(Modifier.height(24.dp))

        // — Alerts —
        SectionHeader(stringResource(R.string.settings_section_alerts))
        SettingsGroup {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.settings_show_alerts), color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp, modifier = Modifier.weight(1f))
            Switch(
                checked = settings.alertsEnabled,
                onCheckedChange = { viewModel.updateAlertsEnabled(it) },
                colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.onSurface, checkedTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)),
            )
        }
        if (settings.alertsEnabled) {
            AlertCheckRow(stringResource(R.string.settings_alert_nws), settings.alertsNwsEnabled) { viewModel.updateAlertsNwsEnabled(it) }
            AlertCheckRow(stringResource(R.string.settings_alert_gdacs), settings.alertsGdacsEnabled) { viewModel.updateAlertsGdacsEnabled(it) }
            AlertCheckRow(stringResource(R.string.settings_alert_meteoalarm), settings.alertsMeteoAlarmEnabled) { viewModel.updateAlertsMeteoAlarmEnabled(it) }
            AlertCheckRow(stringResource(R.string.settings_alert_jma), settings.alertsJmaEnabled) { viewModel.updateAlertsJmaEnabled(it) }
            AlertCheckRow(stringResource(R.string.settings_alert_eccc), settings.alertsEcccEnabled) { viewModel.updateAlertsEcccEnabled(it) }
            AlertCheckRow(stringResource(R.string.settings_alert_bom), settings.alertsBomEnabled) { viewModel.updateAlertsBomEnabled(it) }
            AlertCheckRow(stringResource(R.string.settings_alert_nhc), settings.alertsNhcEnabled) { viewModel.updateAlertsNhcEnabled(it) }
        }
        }

        Spacer(Modifier.height(24.dp))

        // — Geocoding Source —
        SectionHeader(stringResource(R.string.settings_section_location))
        SettingsGroup {
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
                            Text(stringResource(R.string.settings_geocoder_nominatim_desc), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 12.sp, lineHeight = 16.sp)
                        }
                    }
                }
            }
        }
        }

        Spacer(Modifier.height(24.dp))

        } // end if (settings.advancedMode)

        if (!settings.advancedMode) Spacer(Modifier.height(24.dp))

        // — About —
        SettingsGroup {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenAbout, role = Role.Button)
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.about_title), color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text(stringResource(R.string.about_row_desc), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f), fontSize = 12.sp, lineHeight = 16.sp)
                }
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}

/** Shared shell for Settings-style pages: gradient + Blue Marble background, back header, scrolling column. */
@Composable
internal fun SettingsPage(
    title: String,
    onBack: () -> Unit,
    // The language flag at the top, as in the main screen's footer: a tap shows the next language, holding it lists them
    languageFlag: String? = null,
    onCycleLanguage: (() -> Unit)? = null,
    onSelectLanguage: ((Int) -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    BackHandler(onBack = onBack)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0E0B3D), Color(0xFF1A1565))
                )
            )
    ) {
        Image(
            painter = painterResource(R.drawable.bg_blue_marble),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().alpha(0.12f),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
        ) {
            // Stays at the top while the page scrolls
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp)) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.settings_back), tint = MaterialTheme.colorScheme.onSurface)
                }
                Spacer(Modifier.width(8.dp))
                Text(title, color = MaterialTheme.colorScheme.onSurface, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                if (languageFlag != null && onCycleLanguage != null && onSelectLanguage != null) {
                    LanguageFlagButton(
                        flag = languageFlag,
                        fontSize = 22.sp,
                        padding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        onCycleLanguage = onCycleLanguage,
                        onSelectLanguage = onSelectLanguage,
                    )
                }
            }
            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                Spacer(Modifier.height(8.dp))
                content()
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
internal fun SectionHeader(title: String) {
    Text(
        title.uppercase(),
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(start = 8.dp),
    )
    Spacer(Modifier.height(8.dp))
}

@Composable
internal fun SettingsGroup(content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SectionSurface, shape)
            .border(1.dp, Hairline, shape)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        content()
    }
}

@Composable
private fun AlertCheckRow(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, top = 2.dp, bottom = 2.dp)
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

@Composable
private fun WeatherSource.displayName() = when (this) {
    WeatherSource.OPEN_METEO -> stringResource(R.string.settings_open_meteo_default)
    WeatherSource.PIRATE_WEATHER -> stringResource(R.string.settings_pirate_weather)
}

private fun WeatherSource.requiresApiKey() = this == WeatherSource.PIRATE_WEATHER

@Composable
private fun GeocodingSource.displayName() = when (this) {
    GeocodingSource.SYSTEM_GEOCODER -> stringResource(R.string.settings_geocoder_system)
    GeocodingSource.NOMINATIM -> stringResource(R.string.settings_geocoder_nominatim)
}
