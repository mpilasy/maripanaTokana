package orinasa.njarasoa.maripanatokana.data.remote

import orinasa.njarasoa.maripanatokana.domain.model.DailyForecast
import orinasa.njarasoa.maripanatokana.domain.model.HourlyForecast
import orinasa.njarasoa.maripanatokana.domain.model.Precipitation
import orinasa.njarasoa.maripanatokana.domain.model.Pressure
import orinasa.njarasoa.maripanatokana.domain.model.Temperature
import orinasa.njarasoa.maripanatokana.domain.model.WeatherData
import orinasa.njarasoa.maripanatokana.domain.model.WindSpeed

fun owmIdToWmo(id: Int): Int = when (id) {
    in 200..202, in 210..221, in 230..232 -> 95
    300, 310 -> 51
    301, 311 -> 53
    302, 312, 313, 314, 321 -> 55
    500, 501 -> 61
    502, 503, 504 -> 65
    511 -> 67
    520 -> 80
    521 -> 81
    522, 531 -> 82
    600 -> 71
    601 -> 73
    602 -> 75
    611, 612, 613, 615, 616 -> 77
    620 -> 85
    621, 622 -> 86
    in 700..781 -> 45
    800 -> 0
    801 -> 1
    802 -> 2
    803, 804 -> 3
    else -> 0
}

fun OWMResponse.toDomain(): WeatherData {
    val utcOffset = timezoneOffset
    val c = current
    val w = c.weather.firstOrNull()
    val wmoCode = owmIdToWmo(w?.id ?: 800)
    val isDay = w?.icon?.endsWith("d") != false

    val nowMillis = System.currentTimeMillis()
    val startIndex = hourly.indexOfFirst { it.dt * 1000 >= nowMillis }.takeIf { it != -1 } ?: 0
    val hourlyForecast = hourly.subList(startIndex, minOf(startIndex + 24, hourly.size)).map { h ->
        HourlyForecast(
            time = h.dt * 1000,
            temperature = Temperature.fromCelsius(h.temp),
            weatherCode = owmIdToWmo(h.weather.firstOrNull()?.id ?: 800),
            precipProbability = (h.pop * 100).toInt(),
            windSpeed = WindSpeed.fromMetersPerSecond(h.windSpeed),
            windDirection = h.windDeg,
            pressure = Pressure.fromHPa(h.pressure.toDouble()),
            precipitation = Precipitation.fromMm(h.rain?.oneHour ?: 0.0),
        )
    }

    val dailyForecast = daily.map { d ->
        DailyForecast(
            date = d.dt * 1000,
            tempMax = Temperature.fromCelsius(d.temp.max),
            tempMin = Temperature.fromCelsius(d.temp.min),
            weatherCode = owmIdToWmo(d.weather.firstOrNull()?.id ?: 800),
            precipProbability = (d.pop * 100).toInt(),
            windSpeed = WindSpeed.fromMetersPerSecond(d.windSpeed),
            windDirection = d.windDeg,
            precipitation = Precipitation.fromMm((d.rain ?: 0.0) + (d.snow ?: 0.0)),
        )
    }

    val partial = WeatherData(
        utcOffsetSeconds = utcOffset,
        temperature = Temperature.fromCelsius(c.temp),
        feelsLike = Temperature.fromCelsius(c.feelsLike),
        tempMin = Temperature.fromCelsius(daily.firstOrNull()?.temp?.min ?: c.temp),
        tempMax = Temperature.fromCelsius(daily.firstOrNull()?.temp?.max ?: c.temp),
        weatherCode = wmoCode,
        iconCode = wmoIconCode(wmoCode, isDay),
        locationName = "",
        pressure = Pressure.fromHPa(c.pressure.toDouble()),
        humidity = c.humidity,
        dewPoint = Temperature.fromCelsius(c.dewPoint),
        windSpeed = WindSpeed.fromMetersPerSecond(c.windSpeed),
        windDeg = c.windDeg,
        windGust = c.windGust?.let { WindSpeed.fromMetersPerSecond(it) },
        rain = c.rain?.let { if (it.oneHour > 0) Precipitation.fromMm(it.oneHour) else null },
        snow = c.snow?.let { if (it.oneHour > 0) Precipitation.fromMm(it.oneHour) else null },
        cloudCover = c.clouds,
        uvIndex = c.uvi,
        visibility = c.visibility,
        sunrise = c.sunrise,
        sunset = c.sunset,
        dailySunrise = daily.map { it.sunrise * 1000 },
        dailySunset = daily.map { it.sunset * 1000 },
        hourlyForecast = hourlyForecast,
        dailyForecast = dailyForecast,
    )
    return partial.copy(alerts = deriveAlertsFromWeatherData(partial))
}
