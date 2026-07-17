package orinasa.njarasoa.maripanatokana.ui.weather.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import orinasa.njarasoa.maripanatokana.R
import orinasa.njarasoa.maripanatokana.domain.model.AirQualityIndex
import orinasa.njarasoa.maripanatokana.domain.model.AqiStandard
import orinasa.njarasoa.maripanatokana.domain.model.AqiTier
import java.util.Locale

private data class PollutantRow(val label: String, val value: Double, val tier: AqiTier?)

/**
 * Full breakdown of the Open-Meteo air-quality "current" call: both AQI standards plus every
 * pollutant concentration the API returned for this location. Pollutants the domain doesn't
 * cover (e.g. ammonia outside the CAMS-Europe domain) are simply omitted.
 */
@Composable
fun AirQualityDetailDialog(
    airQuality: AirQualityIndex,
    onDismissRequest: () -> Unit,
    localizeDigits: (String) -> String = { it },
) {
    val aqiTierLabels = stringArrayResource(R.array.aqi_tier_labels)
    val usAqiLabel = stringResource(R.string.air_quality_us_aqi)
    val euAqiLabel = stringResource(R.string.air_quality_eu_aqi)
    val (aqiP, aqiS) = airQuality.displayDual()
    val (unitP, unitS) = if (airQuality.primaryStandard == AqiStandard.EUROPEAN) euAqiLabel to usAqiLabel else usAqiLabel to euAqiLabel

    val pollutants = listOfNotNull(
        airQuality.pm25?.let { PollutantRow(stringResource(R.string.air_quality_pm2_5), it, airQuality.pm25Tier) },
        airQuality.pm10?.let { PollutantRow(stringResource(R.string.air_quality_pm10), it, airQuality.pm10Tier) },
        airQuality.carbonMonoxide?.let { PollutantRow(stringResource(R.string.air_quality_carbon_monoxide), it, airQuality.carbonMonoxideTier) },
        airQuality.nitrogenDioxide?.let { PollutantRow(stringResource(R.string.air_quality_nitrogen_dioxide), it, airQuality.nitrogenDioxideTier) },
        airQuality.sulphurDioxide?.let { PollutantRow(stringResource(R.string.air_quality_sulphur_dioxide), it, airQuality.sulphurDioxideTier) },
        airQuality.ozone?.let { PollutantRow(stringResource(R.string.air_quality_ozone), it, airQuality.ozoneTier) },
        airQuality.ammonia?.let { PollutantRow(stringResource(R.string.air_quality_ammonia), it, null) },
        airQuality.dust?.let { PollutantRow(stringResource(R.string.air_quality_dust), it, null) },
    )

    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = stringResource(R.string.detail_air_quality),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = unitP,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        )
                        Text(
                            text = localizeDigits(aqiP),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        AqiTierBadge(tier = airQuality.primaryTier, label = aqiTierLabels[airQuality.primaryTier.ordinal])
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = unitS,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        )
                        Text(
                            text = localizeDigits(aqiS),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }

                if (pollutants.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f))
                    Spacer(modifier = Modifier.height(8.dp))
                    for (row in pollutants) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = row.label,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                modifier = Modifier.weight(1f),
                            )
                            row.tier?.let {
                                AqiTierBadge(tier = it, label = aqiTierLabels[it.ordinal], fontSize = 10.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text(
                                text = localizeDigits("%.1f".format(Locale.US, row.value)) + " µg/m³",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }
        }
    }
}
