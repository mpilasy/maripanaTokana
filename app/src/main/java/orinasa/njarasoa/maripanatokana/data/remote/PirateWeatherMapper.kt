package orinasa.njarasoa.maripanatokana.data.remote

import orinasa.njarasoa.maripanatokana.domain.model.DailyForecast
import orinasa.njarasoa.maripanatokana.domain.model.HourlyForecast
import orinasa.njarasoa.maripanatokana.domain.model.Precipitation
import orinasa.njarasoa.maripanatokana.domain.model.Pressure
import orinasa.njarasoa.maripanatokana.domain.model.Temperature
import orinasa.njarasoa.maripanatokana.domain.model.WeatherData
import orinasa.njarasoa.maripanatokana.domain.model.WindSpeed

private fun pirateIconToWmo(icon: String): Int = when (icon) {
    "clear-day", "clear-night" -> 0
    "partly-cloudy-day", "partly-cloudy-night" -> 2
    "cloudy" -> 3
    "fog" -> 45
    "rain" -> 63
    "sleet" -> 77
    "snow" -> 73
    else -> 0
}

fun PirateWeatherResponse.toDomain(): WeatherData {
    val utcOffset = (offsetHours * 3600).toInt()
    val c = currently
    val wmoCode = pirateIconToWmo(c.icon)
    val isDay = c.icon.endsWith("-day")

    val nowMillis = System.currentTimeMillis()
    val startIndex = hourly.data.indexOfFirst { it.time * 1000 >= nowMillis }.takeIf { it != -1 } ?: 0
    val hourlyForecast = hourly.data.subList(startIndex, minOf(startIndex + 24, hourly.data.size)).map { h ->
        HourlyForecast(
            time = h.time * 1000,
            temperature = Temperature.fromCelsius(h.temperature),
            weatherCode = pirateIconToWmo(h.icon),
            precipProbability = (h.precipProbability * 100).toInt(),
            windSpeed = WindSpeed.fromMetersPerSecond(h.windSpeed),
            windDirection = h.windBearing,
            pressure = Pressure.fromHPa(h.pressure),
            precipitation = Precipitation.fromMm(h.precipIntensity),
        )
    }

    val dailyForecast = daily.data.map { d ->
        DailyForecast(
            date = d.time * 1000,
            tempMax = Temperature.fromCelsius(d.temperatureMax),
            tempMin = Temperature.fromCelsius(d.temperatureMin),
            weatherCode = pirateIconToWmo(d.icon),
            precipProbability = (d.precipProbability * 100).toInt(),
            windSpeed = WindSpeed.fromMetersPerSecond(d.windSpeed),
            windDirection = d.windBearing,
            precipitation = Precipitation.fromMm(d.precipIntensity),
        )
    }

    val partial = WeatherData(
        utcOffsetSeconds = utcOffset,
        temperature = Temperature.fromCelsius(c.temperature),
        feelsLike = Temperature.fromCelsius(c.apparentTemperature),
        tempMin = Temperature.fromCelsius(daily.data.firstOrNull()?.temperatureMin ?: c.temperature),
        tempMax = Temperature.fromCelsius(daily.data.firstOrNull()?.temperatureMax ?: c.temperature),
        weatherCode = wmoCode,
        iconCode = wmoIconCode(wmoCode, isDay),
        locationName = "",
        pressure = Pressure.fromHPa(c.pressure),
        humidity = (c.humidity * 100).toInt(),
        dewPoint = Temperature.fromCelsius(c.dewPoint),
        windSpeed = WindSpeed.fromMetersPerSecond(c.windSpeed),
        windDeg = c.windBearing,
        windGust = c.windGust?.let { WindSpeed.fromMetersPerSecond(it) },
        rain = if (c.precipType == "rain" && c.precipIntensity > 0) Precipitation.fromMm(c.precipIntensity) else null,
        snow = if (c.precipType == "snow" && c.precipIntensity > 0) Precipitation.fromMm(c.precipIntensity) else null,
        cloudCover = (c.cloudCover * 100).toInt(),
        uvIndex = c.uvIndex,
        visibility = (c.visibility * 1000).toInt(),
        sunrise = daily.data.firstOrNull()?.sunriseTime ?: 0L,
        sunset = daily.data.firstOrNull()?.sunsetTime ?: 0L,
        dailySunrise = daily.data.map { it.sunriseTime * 1000 },
        dailySunset = daily.data.map { it.sunsetTime * 1000 },
        hourlyForecast = hourlyForecast,
        dailyForecast = dailyForecast,
    )
    return partial
}
