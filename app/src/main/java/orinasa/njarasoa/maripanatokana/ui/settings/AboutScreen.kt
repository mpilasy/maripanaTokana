package orinasa.njarasoa.maripanatokana.ui.settings

import androidx.annotation.RawRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import orinasa.njarasoa.maripanatokana.BuildConfig
import orinasa.njarasoa.maripanatokana.R
import orinasa.njarasoa.maripanatokana.ui.theme.Hairline
import orinasa.njarasoa.maripanatokana.ui.theme.LocalDisplayFont
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * One acknowledged service, asset or library. [nameRes] is used instead of [name] when the
 * entry already has a translated label (e.g. alert sources with their region).
 * [textRes] opens a bundled licence text in-app instead of [url].
 * Keep in sync with web/src/lib/credits.ts.
 */
private data class Credit(
    val name: String = "",
    @StringRes val nameRes: Int? = null,
    val license: String,
    val url: String? = null,
    val note: String? = null,
    @StringRes val noteRes: Int? = null,
    @RawRes val textRes: Int? = null,
)

private data class CreditSection(@StringRes val titleRes: Int, val items: List<Credit>)

private const val FONT_FAMILIES =
    "Orbitron, Outfit, Rajdhani, Inter, Oxanium, Nunito, Space Grotesk, DM Sans, Sora, Source Sans 3, " +
        "Manrope, Rubik, Josefin Sans, Lato, Cormorant Garamond, Fira Sans, Playfair Display, Work Sans, " +
        "Quicksand, Nunito Sans, Comfortaa, Karla, Baloo 2, Poppins, Exo 2, Barlow, Michroma, Saira, Jost, " +
        "Atkinson Hyperlegible, Fira Code, Montserrat, Open Sans, Space Mono, Plus Jakarta Sans, Archivo, " +
        "Archivo Narrow, Lora"

private val isStandardFlavor = BuildConfig.FLAVOR == "standard"

private val creditSections = listOf(
    CreditSection(
        R.string.about_section_weather,
        listOf(
            Credit("Open-Meteo", license = "CC BY 4.0", url = "https://open-meteo.com/en/licence"),
            // No published data licence; credit its upstream sources.
            Credit("Pirate Weather", license = "NOAA, ECMWF, FMI, CMC", url = "https://pirateweather.net", noteRes = R.string.settings_requires_api_key),
        ),
    ),
    CreditSection(
        R.string.about_section_air_quality,
        listOf(
            Credit("Open-Meteo Air Quality", license = "CC BY 4.0", url = "https://open-meteo.com/en/docs/air-quality-api",
                note = "Generated using Copernicus Atmosphere Monitoring Service information"),
        ),
    ),
    CreditSection(
        R.string.about_section_geocoding,
        listOf(
            Credit("Nominatim / OpenStreetMap", license = "ODbL", url = "https://www.openstreetmap.org/copyright",
                note = "© OpenStreetMap contributors"),
            Credit("Open-Meteo Geocoding", license = "CC BY 4.0", url = "https://open-meteo.com/en/docs/geocoding-api",
                note = "Location data based on GeoNames (CC BY 4.0)"),
            Credit(nameRes = R.string.settings_geocoder_system, license = "Android"),
            Credit("Plus Codes (Open Location Code)", license = "Apache-2.0", url = "https://github.com/google/open-location-code",
                note = "Open format; our own implementation"),
        ),
    ),
    CreditSection(
        R.string.about_section_alerts,
        listOf(
            Credit(nameRes = R.string.settings_alert_nws, license = "Public domain", url = "https://www.weather.gov/disclaimer"),
            Credit(nameRes = R.string.settings_alert_gdacs, license = "CC BY 4.0", url = "https://www.gdacs.org/About/termofuse.aspx",
                note = "Global Disaster Alert and Coordination System, GDACS"),
            Credit(nameRes = R.string.settings_alert_meteoalarm, license = "CC BY 4.0", url = "https://api.meteoalarm.org",
                note = "Data provided by EUMETNET members"),
            Credit(nameRes = R.string.settings_alert_jma, license = "Public Data License 1.0", url = "https://www.jma.go.jp/jma/en/copyright.html",
                note = "Source: Japan Meteorological Agency website"),
            Credit(nameRes = R.string.settings_alert_eccc, license = "ECCC Data Servers End-use Licence", url = "https://eccc-msc.github.io/open-data/licence/readme_en/",
                note = "Data Source: Environment and Climate Change Canada"),
            Credit(nameRes = R.string.settings_alert_bom, license = "© Commonwealth of Australia", url = "https://www.bom.gov.au/copyright",
                note = "Bureau of Meteorology"),
            Credit(nameRes = R.string.settings_alert_nhc, license = "Public domain", url = "https://www.nhc.noaa.gov"),
        ),
    ),
    CreditSection(
        R.string.about_section_imagery,
        listOf(
            Credit("NASA Blue Marble", license = "Public domain", url = "https://visibleearth.nasa.gov/collection/1484/blue-marble"),
            Credit("Meteocons", license = "MIT", url = "https://github.com/basmilius/weather-icons", note = "Weather icons by Bas Milius"),
        ),
    ),
    CreditSection(
        R.string.about_section_fonts,
        listOf(
            Credit("Google Fonts", license = "SIL OFL 1.1", note = FONT_FAMILIES, textRes = R.raw.font_licenses),
        ),
    ),
    CreditSection(
        R.string.about_section_libraries,
        listOfNotNull(
            Credit("Kotlin, kotlinx.coroutines, kotlinx.serialization", license = "Apache-2.0", url = "https://kotlinlang.org"),
            Credit("Jetpack Compose, Material 3", license = "Apache-2.0", url = "https://developer.android.com/compose"),
            Credit("AndroidX (Core, Activity, Lifecycle, WorkManager, Glance, ExifInterface)", license = "Apache-2.0", url = "https://developer.android.com/jetpack/androidx"),
            Credit("Hilt / Dagger", license = "Apache-2.0", url = "https://dagger.dev/hilt"),
            Credit("Retrofit", license = "Apache-2.0", url = "https://square.github.io/retrofit"),
            Credit("OkHttp", license = "Apache-2.0", url = "https://square.github.io/okhttp"),
            Credit("Retrofit kotlinx.serialization converter", license = "Apache-2.0", url = "https://github.com/JakeWharton/retrofit2-kotlinx-serialization-converter"),
            if (isStandardFlavor) Credit("Accompanist Permissions", license = "Apache-2.0", url = "https://google.github.io/accompanist") else null,
            if (isStandardFlavor) Credit("Google Play services Location", license = "Android SDK License", url = "https://developers.google.com/android/guides/overview") else null,
        ),
    ),
)

@Composable
fun AboutScreen(onBack: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    var openTextRes by rememberSaveable { mutableStateOf<Int?>(null) }
    val licenseRes = openTextRes
    if (licenseRes != null) {
        LicenseTextScreen(title = stringResource(R.string.about_section_fonts), textRes = licenseRes, onBack = { openTextRes = null })
    } else SettingsPage(title = stringResource(R.string.about_title), onBack = onBack) {
        // App identity
        SettingsGroup {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    fontFamily = LocalDisplayFont.current,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "v${BuildConfig.VERSION_NAME}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "© 2026 Orinasa Njarasoa",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                )
                Text(
                    text = stringResource(R.string.about_app_license),
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.about_source_code) + " ↗",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .clickable(role = Role.Button) { uriHandler.openUri("https://github.com/mpilasy/maripanaTokana") }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }

        creditSections.forEach { section ->
            Spacer(Modifier.height(24.dp))
            SectionHeader(stringResource(section.titleRes))
            SettingsGroup {
                section.items.forEachIndexed { index, item ->
                    if (index > 0) {
                        Box(Modifier.fillMaxWidth().height(1.dp).background(Hairline))
                    }
                    val onOpen: (() -> Unit)? = when {
                        item.textRes != null -> { { openTextRes = item.textRes } }
                        item.url != null -> { { uriHandler.openUri(item.url) } }
                        else -> null
                    }
                    CreditRow(item, onOpen = onOpen)
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun CreditRow(item: Credit, onOpen: (() -> Unit)?) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onOpen != null) Modifier.clickable(onClick = onOpen, role = Role.Button) else Modifier)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.nameRes?.let { stringResource(it) } ?: item.name,
                fontSize = 15.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Medium,
                color = onSurface,
            )
            val note = item.noteRes?.let { stringResource(it) } ?: item.note
            if (note != null) {
                Text(text = note, fontSize = 12.sp, lineHeight = 16.sp, color = onSurface.copy(alpha = 0.55f))
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = item.license + if (onOpen != null) " ↗" else "",
            fontSize = 12.sp,
            lineHeight = 16.sp,
            textAlign = TextAlign.End,
            color = onSurface.copy(alpha = 0.65f),
            modifier = Modifier.width(110.dp),
        )
    }
}

@Composable
private fun LicenseTextScreen(title: String, @RawRes textRes: Int, onBack: () -> Unit) {
    val context = LocalContext.current
    val text by produceState<String?>(null, textRes) {
        value = withContext(Dispatchers.IO) {
            context.resources.openRawResource(textRes).bufferedReader().use { it.readText() }
        }
    }
    SettingsPage(title = title, onBack = onBack) {
        SettingsGroup {
            val loaded = text
            if (loaded == null) {
                Box(Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onSurface)
                }
            } else {
                Text(
                    text = loaded,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    modifier = Modifier.padding(vertical = 12.dp),
                )
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
