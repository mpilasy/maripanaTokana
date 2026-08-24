package orinasa.njarasoa.maripanatokana.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ActivityIndicesTest {

    private fun mockWeatherData(
        tempC: Double = 15.0,
        humidity: Int = 50,
        windMs: Double = 3.0,
        rainMm: Double = 0.0,
        uvIndex: Double = 2.0
    ): WeatherData {
        return WeatherData(
            temperature = Temperature.fromCelsius(tempC),
            feelsLike = Temperature.fromCelsius(tempC),
            tempMin = Temperature.fromCelsius(tempC - 5),
            tempMax = Temperature.fromCelsius(tempC + 5),
            weatherCode = 0,
            iconCode = "01d",
            locationName = "Test City",
            pressure = Pressure.fromHPa(1013.0),
            humidity = humidity,
            dewPoint = Temperature.fromCelsius(tempC - 2),
            windSpeed = WindSpeed.fromMetersPerSecond(windMs),
            windDeg = 180,
            windGust = null,
            rain = if (rainMm > 0) Precipitation.fromMm(rainMm) else null,
            snow = null,
            cloudCover = 10,
            uvIndex = uvIndex,
            visibility = 10000,
            sunrise = 0,
            sunset = 0,
            hourlyForecast = emptyList(),
            dailyForecast = emptyList(),
            timestamp = System.currentTimeMillis()
        )
    }

    @Test
    fun idealConditions_returnExcellentTiers() {
        val data = mockWeatherData(tempC = 20.0, humidity = 45, windMs = 3.0, rainMm = 0.0, uvIndex = 2.0)
        val indices = ActivityIndices.fromWeatherData(data)
        assertEquals(ActivityTier.EXCELLENT, indices.running)
        assertEquals(ActivityTier.EXCELLENT, indices.laundry)
        assertEquals(ActivityTier.EXCELLENT, indices.uvSafety)
    }

    @Test
    fun rainyConditions_returnPoorTiersForOutdoorActivities() {
        val data = mockWeatherData(tempC = 15.0, humidity = 90, windMs = 3.0, rainMm = 5.0, uvIndex = 2.0)
        val indices = ActivityIndices.fromWeatherData(data)
        assertEquals(ActivityTier.POOR, indices.running)
        assertEquals(ActivityTier.POOR, indices.laundry)
    }

    @Test
    fun highUv_returnsPoorUvSafetyTier() {
        val data = mockWeatherData(uvIndex = 9.0)
        val indices = ActivityIndices.fromWeatherData(data)
        assertEquals(ActivityTier.POOR, indices.uvSafety)
    }
}
