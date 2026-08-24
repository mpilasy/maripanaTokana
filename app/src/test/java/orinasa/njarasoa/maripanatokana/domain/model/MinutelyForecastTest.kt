package orinasa.njarasoa.maripanatokana.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class MinutelyForecastTest {

    @Test
    fun testMinutelyForecastCreation() {
        val now = System.currentTimeMillis()
        val forecast = MinutelyForecast(
            time = now,
            precipitation = Precipitation.fromMm(1.5)
        )

        assertEquals(now, forecast.time)
        assertEquals(1.5, forecast.precipitation.mm, 0.001)
    }

    @Test
    fun testWeatherDataWithMinutelyForecast() {
        val now = System.currentTimeMillis()
        val minutelyItems = listOf(
            MinutelyForecast(now, Precipitation.fromMm(0.0)),
            MinutelyForecast(now + 15 * 60 * 1000L, Precipitation.fromMm(0.8))
        )

        val weatherData = WeatherData(
            temperature = Temperature.fromCelsius(20.0),
            feelsLike = Temperature.fromCelsius(20.0),
            tempMin = Temperature.fromCelsius(15.0),
            tempMax = Temperature.fromCelsius(25.0),
            weatherCode = 0,
            iconCode = "01d",
            locationName = "Test",
            pressure = Pressure.fromHPa(1013.0),
            humidity = 60,
            dewPoint = Temperature.fromCelsius(12.0),
            windSpeed = WindSpeed.fromMetersPerSecond(3.0),
            windDeg = 180,
            windGust = null,
            rain = null,
            snow = null,
            cloudCover = 20,
            uvIndex = 3.0,
            visibility = 10000,
            sunrise = 0,
            sunset = 0,
            minutelyForecast = minutelyItems
        )

        assertEquals(2, weatherData.minutelyForecast.size)
        assertEquals(0.8, weatherData.minutelyForecast[1].precipitation.mm, 0.001)
    }
}
