// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

package orinasa.njarasoa.maripanatokana.data.remote

import orinasa.njarasoa.maripanatokana.domain.model.DailyForecast
import orinasa.njarasoa.maripanatokana.domain.model.HourlyForecast
import orinasa.njarasoa.maripanatokana.domain.model.MinutelyForecast
import orinasa.njarasoa.maripanatokana.domain.model.Precipitation
import orinasa.njarasoa.maripanatokana.domain.model.Pressure
import orinasa.njarasoa.maripanatokana.domain.model.Temperature
import orinasa.njarasoa.maripanatokana.domain.model.WeatherData
import orinasa.njarasoa.maripanatokana.domain.model.WindSpeed
import kotlin.math.roundToInt

// The same on the web (pirateWeather.ts). Sleet is freezing rain (66), so it gets the sleet icon.
private val PIRATE_ICON_WMO = mapOf(
    "clear-day" to 0, "clear-night" to 0,
    "partly-cloudy-day" to 2, "partly-cloudy-night" to 2,
    "cloudy" to 3,
    "fog" to 45,
    "drizzle" to 53,
    "rain" to 63,
    "sleet" to 66,
    "snow" to 73,
    "thunderstorm" to 95,
    "wind" to 3,
)

private fun pirateIconToWmo(icon: String): Int = PIRATE_ICON_WMO[icon] ?: 0

/** A 0–1 fraction as a whole percentage, with .5 rounded up (as Math.round on the web). */
private fun percent(fraction: Double): Int = (fraction * 100).roundToInt()

/** The forecast in the app's model. The web has the same split in pirateWeather.ts. */
fun PirateWeatherResponse.toDomain(): WeatherData {
    val c = currently
    val wmoCode = pirateIconToWmo(c.icon)
    val isDay = c.icon.endsWith("-day")
    val days = daily.data.take(10)
    val today = daily.data.firstOrNull()

    return WeatherData(
        utcOffsetSeconds = (offsetHours * 3600).toInt(),
        temperature = Temperature.fromCelsius(c.temperature),
        feelsLike = Temperature.fromCelsius(c.apparentTemperature),
        tempMin = Temperature.fromCelsius(today?.temperatureMin ?: c.temperature),
        tempMax = Temperature.fromCelsius(today?.temperatureMax ?: c.temperature),
        weatherCode = wmoCode,
        iconCode = wmoIconCode(wmoCode, isDay),
        locationName = "",
        pressure = Pressure.fromHPa(c.pressure),
        humidity = c.humidity?.let(::percent),
        dewPoint = Temperature.fromCelsius(c.dewPoint),
        windSpeed = WindSpeed.fromMetersPerSecond(c.windSpeed),
        windDeg = c.windBearing,
        windGust = c.windGust?.let { WindSpeed.fromMetersPerSecond(it) },
        rain = if (c.precipType == "rain" && c.precipIntensity > 0) Precipitation.fromMm(c.precipIntensity) else null,
        snow = if (c.precipType == "snow" && c.precipIntensity > 0) Precipitation.fromMm(c.precipIntensity) else null,
        cloudCover = c.cloudCover?.let(::percent),
        uvIndex = c.uvIndex,
        visibility = c.visibility?.let { (it * 1000).roundToInt() },
        sunrise = today?.sunriseTime ?: 0L,
        sunset = today?.sunsetTime ?: 0L,
        dailySunrise = days.map { it.sunriseTime * 1000 },
        dailySunset = days.map { it.sunsetTime * 1000 },
        hourlyForecast = hourlyForecast(hourly.data),
        minutelyForecast = minutely.data.map { MinutelyForecast(it.time * 1000, Precipitation.fromMm(it.precipIntensity)) },
        dailyForecast = dailyForecast(days, hourly.data),
    )
}

/** The next 24 hours, from the first hour that has not passed. */
private fun hourlyForecast(data: List<PirateWeatherHourly>): List<HourlyForecast> {
    val nowMillis = System.currentTimeMillis()
    val startIndex = data.indexOfFirst { it.time * 1000 >= nowMillis }.takeIf { it != -1 } ?: 0
    return data.subList(startIndex, minOf(startIndex + 24, data.size)).map { h ->
        HourlyForecast(
            time = h.time * 1000,
            temperature = Temperature.fromCelsius(h.temperature),
            weatherCode = pirateIconToWmo(h.icon),
            precipProbability = percent(h.precipProbability),
            windSpeed = WindSpeed.fromMetersPerSecond(h.windSpeed),
            windDirection = h.windBearing,
            pressure = Pressure.fromHPa(h.pressure),
            precipitation = Precipitation.fromMm(h.precipIntensity),
            uvIndex = h.uvIndex,
            humidity = h.humidity?.let(::percent),
            dewPoint = h.dewPoint?.let { Temperature.fromCelsius(it) },
        )
    }
}

/** The days, with the night weather taken from the hourly forecast. */
private fun dailyForecast(days: List<PirateWeatherDaily>, hourly: List<PirateWeatherHourly>): List<DailyForecast> {
    val nightCodes = nightWeatherCodes(
        hourly.map { it.time * 1000 },
        hourly.map { pirateIconToWmo(it.icon) },
        days.map { it.sunsetTime * 1000 },
        days.map { it.sunriseTime * 1000 },
    )
    return days.mapIndexed { i, d ->
        DailyForecast(
            date = d.time * 1000,
            tempMax = Temperature.fromCelsius(d.temperatureMax),
            tempMin = Temperature.fromCelsius(d.temperatureMin),
            weatherCode = pirateIconToWmo(d.icon),
            precipProbability = percent(d.precipProbability),
            windSpeed = WindSpeed.fromMetersPerSecond(d.windSpeed),
            windDirection = d.windBearing,
            precipitation = Precipitation.fromMm(d.precipIntensity),
            uvIndexMax = d.uvIndex,
            nightWeatherCode = nightCodes[i],
            pressure = d.pressure?.let { Pressure.fromHPa(it) },
            humidity = d.humidity?.let(::percent),
            dewPoint = d.dewPoint?.let { Temperature.fromCelsius(it) },
        )
    }
}
