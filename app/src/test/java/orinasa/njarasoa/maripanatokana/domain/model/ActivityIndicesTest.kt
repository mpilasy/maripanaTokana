// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

package orinasa.njarasoa.maripanatokana.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ActivityIndicesTest {

    private fun mockWeatherData(
        tempC: Double = 15.0,
        humidity: Int = 50,
        windMs: Double = 3.0,
        rainMm: Double = 0.0,
        uvIndex: Double = 2.0,
        dewC: Double = tempC - 2,
        weatherCode: Int = 0,
        visibility: Int? = 10000,
        gustMs: Double? = null,
        // The chance of rain or snow (%) of today, tomorrow and the day after
        dailyChances: List<Int> = emptyList(),
    ): WeatherData {
        return WeatherData(
            temperature = Temperature.fromCelsius(tempC),
            feelsLike = Temperature.fromCelsius(tempC),
            tempMin = Temperature.fromCelsius(tempC - 5),
            tempMax = Temperature.fromCelsius(tempC + 5),
            weatherCode = weatherCode,
            locationName = "Test City",
            pressure = Pressure.fromHPa(1013.0),
            humidity = humidity,
            dewPoint = Temperature.fromCelsius(dewC),
            windSpeed = WindSpeed.fromMetersPerSecond(windMs),
            windDeg = 180,
            windGust = gustMs?.let { WindSpeed.fromMetersPerSecond(it) },
            rain = if (rainMm > 0) Precipitation.fromMm(rainMm) else null,
            snow = null,
            cloudCover = 10,
            uvIndex = uvIndex,
            visibility = visibility,
            sunrise = 0,
            sunset = 0,
            hourlyForecast = emptyList(),
            dailyForecast = dailyChances.mapIndexed { i, chance ->
                DailyForecast(
                    date = i * 86_400_000L, tempMax = Temperature.fromCelsius(tempC + 5), tempMin = Temperature.fromCelsius(tempC - 5),
                    weatherCode = 0, precipProbability = chance, windSpeed = WindSpeed.fromMetersPerSecond(windMs), windDirection = 180,
                    precipitation = Precipitation.fromMm(0.0), uvIndexMax = uvIndex,
                )
            },
            timestamp = System.currentTimeMillis()
        )
    }

    @Test
    fun idealConditions_returnExcellentTiers() {
        val data = mockWeatherData(tempC = 20.0, humidity = 45, windMs = 3.0, rainMm = 0.0, uvIndex = 2.0)
        val indices = ActivityIndices.fromWeatherData(data, isDay = true)
        assertEquals(ActivityTier.EXCELLENT, indices.running)
        assertEquals(ActivityTier.EXCELLENT, indices.laundry)
        assertEquals(ActivityTier.EXCELLENT, indices.uvSafety)
    }

    @Test
    fun rainyConditions_returnPoorTiersForOutdoorActivities() {
        val data = mockWeatherData(tempC = 15.0, humidity = 90, windMs = 3.0, rainMm = 5.0, uvIndex = 2.0)
        val indices = ActivityIndices.fromWeatherData(data, isDay = true)
        assertEquals(ActivityTier.POOR, indices.running)
        assertEquals(ActivityTier.POOR, indices.laundry)
    }

    @Test
    fun highUv_returnsPoorUvSafetyTier() {
        val data = mockWeatherData(uvIndex = 9.0)
        val indices = ActivityIndices.fromWeatherData(data, isDay = true)
        assertEquals(ActivityTier.POOR, indices.uvSafety)
    }

    @Test
    fun day_givesUvTierFromIndex() {
        val data = mockWeatherData(uvIndex = 7.0)
        assertEquals(ActivityTier.FAIR, ActivityIndices.fromWeatherData(data, isDay = true).uvSafety)
    }

    @Test
    fun night_givesNotNeededForSunProtection() {
        val data = mockWeatherData(uvIndex = 9.0)
        val indices = ActivityIndices.fromWeatherData(data, isDay = false)
        assertEquals(ActivityTier.NOT_NEEDED, indices.uvSafety)
        // The other activities do not change at night
        assertEquals(ActivityIndices.fromWeatherData(data, isDay = true).running, indices.running)
        assertEquals(ActivityIndices.fromWeatherData(data, isDay = true).laundry, indices.laundry)
    }

    private fun indices(data: WeatherData) = ActivityIndices.fromWeatherData(data, isDay = true)

    @Test
    fun skinComfort_followsDewPointAndHumidity() {
        assertEquals(ActivityTier.EXCELLENT, indices(mockWeatherData(dewC = 10.0, humidity = 60)).skinComfort)
        assertEquals(ActivityTier.GOOD, indices(mockWeatherData(dewC = 2.0, humidity = 60)).skinComfort)
        assertEquals(ActivityTier.FAIR, indices(mockWeatherData(dewC = -5.0, humidity = 60)).skinComfort)
        assertEquals(ActivityTier.POOR, indices(mockWeatherData(dewC = -12.0, humidity = 60)).skinComfort)
        // Desert heat: the dew point is not low, but the air is very dry
        assertEquals(ActivityTier.POOR, indices(mockWeatherData(tempC = 35.0, dewC = 6.0, humidity = 18)).skinComfort)
    }

    @Test
    fun carWash_followsTheNextDays() {
        assertEquals(ActivityTier.EXCELLENT, indices(mockWeatherData(dailyChances = listOf(0, 10, 10))).carWash)
        assertEquals(ActivityTier.GOOD, indices(mockWeatherData(dailyChances = listOf(0, 10, 25))).carWash)
        assertEquals(ActivityTier.FAIR, indices(mockWeatherData(dailyChances = listOf(0, 10, 60))).carWash)
        assertEquals(ActivityTier.FAIR, indices(mockWeatherData(dailyChances = listOf(0, 35, 0))).carWash)
        assertEquals(ActivityTier.POOR, indices(mockWeatherData(dailyChances = listOf(0, 70, 0))).carWash)
        // Raining now, or frost (the water freezes)
        assertEquals(ActivityTier.POOR, indices(mockWeatherData(rainMm = 0.5, dailyChances = listOf(0, 0, 0))).carWash)
        assertEquals(ActivityTier.POOR, indices(mockWeatherData(tempC = -3.0, dailyChances = listOf(0, 0, 0))).carWash)
    }

    @Test
    fun driving_followsIceSnowFogAndGusts() {
        assertEquals(ActivityTier.EXCELLENT, indices(mockWeatherData()).driving)
        assertEquals(ActivityTier.GOOD, indices(mockWeatherData(weatherCode = 61)).driving)
        assertEquals(ActivityTier.FAIR, indices(mockWeatherData(weatherCode = 45, visibility = 800)).driving)
        assertEquals(ActivityTier.FAIR, indices(mockWeatherData(weatherCode = 71)).driving)
        assertEquals(ActivityTier.POOR, indices(mockWeatherData(weatherCode = 66)).driving)
        assertEquals(ActivityTier.POOR, indices(mockWeatherData(gustMs = 27.0)).driving)
        // Rain at freezing point: black ice
        assertEquals(ActivityTier.FAIR, indices(mockWeatherData(tempC = 0.5, rainMm = 0.3, weatherCode = 61)).driving)
        // No visibility or gust data: the weather code alone decides
        assertEquals(ActivityTier.EXCELLENT, indices(mockWeatherData(visibility = null, gustMs = null)).driving)
    }
}
