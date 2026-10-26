// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

package orinasa.njarasoa.maripanatokana.data.repository

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import orinasa.njarasoa.maripanatokana.data.settings.AppSettingsRepository
import orinasa.njarasoa.maripanatokana.data.source.GeocodingDataSource
import orinasa.njarasoa.maripanatokana.data.source.GeocodingSourceSelector
import orinasa.njarasoa.maripanatokana.data.source.WeatherDataSource
import orinasa.njarasoa.maripanatokana.data.source.WeatherSourceSelector
import orinasa.njarasoa.maripanatokana.domain.model.AppSettings
import orinasa.njarasoa.maripanatokana.domain.model.Precipitation
import orinasa.njarasoa.maripanatokana.domain.model.Pressure
import orinasa.njarasoa.maripanatokana.domain.model.Temperature
import orinasa.njarasoa.maripanatokana.domain.model.WeatherData
import orinasa.njarasoa.maripanatokana.domain.model.WindSpeed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GetWeatherTest {
    private val forecastSource = mockk<WeatherDataSource>()
    private val geocodingSource = mockk<GeocodingDataSource>()

    private val forecast = WeatherData(
        temperature = Temperature.fromCelsius(21.5),
        feelsLike = Temperature.fromCelsius(20.0),
        tempMin = Temperature.fromCelsius(15.0),
        tempMax = Temperature.fromCelsius(25.0),
        weatherCode = 3,
        iconCode = "03d",
        locationName = "",
        pressure = Pressure.fromHPa(1013.0),
        humidity = 50,
        dewPoint = Temperature.fromCelsius(10.0),
        windSpeed = WindSpeed.fromMetersPerSecond(4.0),
        windDeg = null,
        windGust = null,
        rain = Precipitation.fromMm(0.0),
        snow = null,
        cloudCover = 10,
        uvIndex = null,
        visibility = null,
        sunrise = 1L,
        sunset = 2L,
        hourlyForecast = emptyList(),
        timestamp = 1L,
    )

    private fun repo(): WeatherRepositoryImpl {
        val settingsRepo = mockk<AppSettingsRepository>()
        every { settingsRepo.current } returns AppSettings()
        val weatherSelector = mockk<WeatherSourceSelector>()
        every { weatherSelector.current() } returns forecastSource
        val geocodingSelector = mockk<GeocodingSourceSelector>()
        every { geocodingSelector.current() } returns geocodingSource
        return WeatherRepositoryImpl(
            context = mockk(relaxed = true),
            nwsApiService = mockk(),
            gdacsApiService = mockk(),
            meteoAlarmApiService = mockk(),
            jmaApiService = mockk(),
            ecccApiService = mockk(),
            bomApiService = mockk(),
            nhcApiService = mockk(),
            settingsRepository = settingsRepo,
            weatherSourceSelector = weatherSelector,
            geocodingSelector = geocodingSelector,
            countryResolver = mockk(),
        )
    }

    @Test
    fun `place lookup runs while the forecast loads`() = runTest {
        val lookupStarted = CompletableDeferred<Unit>()
        coEvery { geocodingSource.reverseGeocode(any(), any(), any()) } coAnswers {
            lookupStarted.complete(Unit)
            "Antananarivo" to "Madagascar"
        }
        // One after the other, the lookup could not start until the forecast returns
        coEvery { forecastSource.getForecast(any(), any()) } coAnswers {
            withTimeoutOrNull(5_000) { lookupStarted.await() } ?: error("place lookup did not start")
            forecast
        }

        val data = repo().getWeather(-18.88, 47.51).getOrThrow()
        assertEquals("Antananarivo", data.locationName)
        assertEquals("Madagascar", data.locationSubtext)
        assertEquals(-18.88, data.latitude!!, 0.0)
    }

    @Test
    fun `failed place lookup falls back to coordinates`() = runTest {
        coEvery { geocodingSource.reverseGeocode(any(), any(), any()) } throws java.io.IOException("offline")
        coEvery { forecastSource.getForecast(any(), any()) } returns forecast

        val data = repo().getWeather(-18.88, 47.51).getOrThrow()
        assertEquals("-18.88, 47.51", data.locationName)
    }

    @Test
    fun `failed forecast fails the fetch`() = runTest {
        coEvery { geocodingSource.reverseGeocode(any(), any(), any()) } returns ("Antananarivo" to null)
        coEvery { forecastSource.getForecast(any(), any()) } throws java.io.IOException("offline")

        assertTrue(repo().getWeather(-18.88, 47.51).isFailure)
    }
}
