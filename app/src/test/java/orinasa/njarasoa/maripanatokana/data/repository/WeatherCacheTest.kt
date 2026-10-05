package orinasa.njarasoa.maripanatokana.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import orinasa.njarasoa.maripanatokana.domain.model.HourlyForecast
import orinasa.njarasoa.maripanatokana.domain.model.Precipitation
import orinasa.njarasoa.maripanatokana.domain.model.Pressure
import orinasa.njarasoa.maripanatokana.domain.model.Temperature
import orinasa.njarasoa.maripanatokana.domain.model.WeatherData
import orinasa.njarasoa.maripanatokana.domain.model.WindSpeed

class WeatherCacheTest {
    @get:Rule val tmp = TemporaryFolder()

    private fun data(name: String) = WeatherData(
        temperature = Temperature.fromCelsius(21.5),
        feelsLike = Temperature.fromCelsius(20.0),
        tempMin = Temperature.fromCelsius(15.0),
        tempMax = Temperature.fromCelsius(Double.NaN),
        weatherCode = 3,
        iconCode = "03d",
        locationName = name,
        pressure = Pressure.fromHPa(1013.0),
        humidity = 50,
        dewPoint = Temperature.fromCelsius(10.0),
        windSpeed = WindSpeed.fromMetersPerSecond(4.0),
        windDeg = null,
        windGust = null,
        rain = Precipitation.fromMm(1.5),
        snow = null,
        cloudCover = 10,
        uvIndex = null,
        visibility = null,
        sunrise = 1L,
        sunset = 2L,
        hourlyForecast = listOf(
            HourlyForecast(1000L, Temperature.fromCelsius(18.0), 1, 20, WindSpeed.fromMetersPerSecond(2.0), 90,
                Pressure.fromHPa(1010.0), Precipitation.fromMm(0.0))
        ),
        timestamp = 123456789L,
    )

    @Test
    fun roundTripsPerKeyPreservingTimestamp() {
        val cache = WeatherCache(tmp.newFolder("c"))
        cache.save("gps", data("Here"))
        cache.save("12.5,-3.0", data("There"))

        val gps = cache.load("gps")!!
        assertEquals("Here", gps.locationName)
        assertEquals(123456789L, gps.timestamp)
        assertEquals(21.5, gps.temperature.celsius, 0.0)
        assertEquals(1, gps.hourlyForecast.size)
        assertEquals(true, gps.tempMax.celsius.isNaN())
        assertEquals("There", cache.load("12.5,-3.0")!!.locationName)
    }

    @Test
    fun missingOrCorruptReturnsNull() {
        val dir = tmp.newFolder("d")
        val cache = WeatherCache(dir)
        assertNull(cache.load("gps"))
        dir.resolve("gps.json").writeText("{not json")
        assertNull(cache.load("gps"))
    }
}
