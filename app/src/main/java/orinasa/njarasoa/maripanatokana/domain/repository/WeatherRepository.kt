// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

package orinasa.njarasoa.maripanatokana.domain.repository

import orinasa.njarasoa.maripanatokana.data.remote.GeocodingResult
import orinasa.njarasoa.maripanatokana.domain.model.WeatherAlert
import orinasa.njarasoa.maripanatokana.domain.model.WeatherData

/** [failedSources]: display names of enabled, applicable alert sources that threw. */
data class AlertsResult(val alerts: List<WeatherAlert>, val failedSources: List<String>)

interface WeatherRepository {
    suspend fun getWeather(lat: Double, lon: Double): Result<WeatherData>
    suspend fun fetchAlerts(lat: Double, lon: Double): Result<AlertsResult>
    suspend fun searchLocation(query: String): Result<List<GeocodingResult>>
    /** Place name and region in Latin letters, or null when the geocoder has none. */
    suspend fun romanPlaceName(lat: Double, lon: Double): Pair<String, String?>?
    /** The place's name and region/country line in the app's current language, or null when the lookup fails. */
    suspend fun localPlaceName(lat: Double, lon: Double): Pair<String, String?>?
}
