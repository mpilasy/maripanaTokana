// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

package orinasa.njarasoa.maripanatokana.ui.common.about

import androidx.annotation.RawRes
import androidx.annotation.StringRes
import orinasa.njarasoa.maripanatokana.BuildConfig
import orinasa.njarasoa.maripanatokana.R

/**
 * One acknowledged service, asset or library. [nameRes] is used instead of [name] when the
 * entry already has a translated label (e.g. alert sources with their region).
 * [textRes] opens a bundled licence text in-app instead of [url].
 * Keep in sync with web/src/lib/credits.ts.
 */
internal data class Credit(
    val name: String = "",
    @StringRes val nameRes: Int? = null,
    val license: String,
    val url: String? = null,
    val note: String? = null,
    @StringRes val noteRes: Int? = null,
    @RawRes val textRes: Int? = null,
    /** True for the Open-Meteo forecast: the name shows the chosen weather model after it. */
    val showsWeatherModel: Boolean = false,
)

internal data class CreditSection(@StringRes val titleRes: Int, val items: List<Credit>)

private const val FONT_FAMILIES =
    "Orbitron, Outfit, Rajdhani, Inter, Oxanium, Nunito, Space Grotesk, DM Sans, Sora, Source Sans 3, " +
        "Manrope, Rubik, Josefin Sans, Lato, Cormorant Garamond, Fira Sans, Playfair Display, Work Sans, " +
        "Quicksand, Nunito Sans, Comfortaa, Karla, Baloo 2, Poppins, Exo 2, Barlow, Michroma, Saira, Jost, " +
        "Atkinson Hyperlegible, Fira Code, Montserrat, Open Sans, Space Mono, Plus Jakarta Sans, Archivo, " +
        "Archivo Narrow, Lora"

private val isStandardFlavor = BuildConfig.FLAVOR == "standard"

internal val creditSections = listOf(
    CreditSection(
        R.string.about_section_weather,
        listOf(
            Credit("Open-Meteo", license = "CC BY 4.0", url = "https://open-meteo.com/en/licence", showsWeatherModel = true),
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
            Credit("Meteocons", license = "MIT", note = "Weather icons by Bas Milius", textRes = R.raw.meteocons_license),
            // Android only: the web app does not use these icons
            Credit("Material Design icons", license = "Apache-2.0", note = "Interface icons by Google", textRes = R.raw.apache_license),
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
