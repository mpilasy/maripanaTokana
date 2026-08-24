package orinasa.njarasoa.maripanatokana.data.remote

import orinasa.njarasoa.maripanatokana.domain.model.DailyForecast
import orinasa.njarasoa.maripanatokana.domain.model.HourlyForecast
import orinasa.njarasoa.maripanatokana.domain.model.Precipitation
import orinasa.njarasoa.maripanatokana.domain.model.Pressure
import orinasa.njarasoa.maripanatokana.domain.model.Temperature
import orinasa.njarasoa.maripanatokana.domain.model.WeatherData
import orinasa.njarasoa.maripanatokana.domain.model.WindSpeed
import java.util.Calendar
import java.util.TimeZone

internal fun parseIsoDateTime(iso: String, utcOffsetSeconds: Int): Long {
    try {
        val year = iso.substring(0, 4).toInt()
        val month = iso.substring(5, 7).toInt() - 1
        val day = iso.substring(8, 10).toInt()
        val hour = iso.substring(11, 13).toInt()
        val minute = iso.substring(14, 16).toInt()
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(year, month, day, hour, minute, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis - (utcOffsetSeconds * 1000L)
    } catch (e: Exception) {
        return 0L
    }
}

private fun parseIsoDate(iso: String, utcOffsetSeconds: Int): Long {
    try {
        val year = iso.substring(0, 4).toInt()
        val month = iso.substring(5, 7).toInt() - 1
        val day = iso.substring(8, 10).toInt()
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(year, month, day, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis - (utcOffsetSeconds * 1000L)
    } catch (e: Exception) {
        return 0L
    }
}


fun OpenMeteoResponse.toDomain(locationName: String, locationSubtext: String? = null): WeatherData {
    val c = current
    val isDay = c.isDay == 1

    // Pre-parse hourly timestamps once
    val parsedHourlyTimes = hourly.time.map { parseIsoDateTime(it, utcOffsetSeconds) }

    val sunriseEpoch = daily.sunrise.firstOrNull()?.let {
        parseIsoDateTime(it, utcOffsetSeconds) / 1000
    } ?: 0L
    val sunsetEpoch = daily.sunset.firstOrNull()?.let {
        parseIsoDateTime(it, utcOffsetSeconds) / 1000
    } ?: 0L

    val dailySunriseMillis = daily.sunrise.map { parseIsoDateTime(it, utcOffsetSeconds) }
    val dailySunsetMillis = daily.sunset.map { parseIsoDateTime(it, utcOffsetSeconds) }

    val nowMillis = System.currentTimeMillis()

    // Find the first index using correctly parsed absolute UTC times
    val startIndex = parsedHourlyTimes.indexOfFirst { it >= nowMillis }.takeIf { it != -1 } ?: 0

    val endIndex = minOf(startIndex + 24, hourly.time.size)

    val hourlyForecast = (startIndex until endIndex).mapNotNull { i ->
        val epoch = parsedHourlyTimes[i]
        if (epoch < nowMillis) return@mapNotNull null // Extra safety

        HourlyForecast(
            time = epoch,
            temperature = Temperature.fromCelsius(hourly.temperature2m.getOrElse(i) { 0.0 }),
            weatherCode = hourly.weatherCode.getOrElse(i) { 0 },
            precipProbability = hourly.precipitationProbability.getOrElse(i) { 0 },
            windSpeed = WindSpeed.fromMetersPerSecond(hourly.windSpeed10m.getOrElse(i) { 0.0 }),
            windDirection = hourly.windDirection10m.getOrElse(i) { 0 },
            pressure = Pressure.fromHPa(hourly.pressureMsl.getOrElse(i) { 1013.0 }),
            precipitation = Precipitation.fromMm(hourly.precipitation.getOrElse(i) { 0.0 }),
        )
    }.distinctBy { it.time }

    val parsedDailyTimes = daily.time.map { parseIsoDate(it, utcOffsetSeconds) }

    val dailyForecast = daily.time.indices.map { i ->
        val epoch = parsedDailyTimes[i]
        DailyForecast(
            date = epoch,
            tempMax = Temperature.fromCelsius(daily.temperatureMax[i]),
            tempMin = Temperature.fromCelsius(daily.temperatureMin[i]),
            weatherCode = if (i == 0) c.weatherCode else daily.weatherCode[i],
            precipProbability = daily.precipitationProbabilityMax[i],
            windSpeed = WindSpeed.fromMetersPerSecond(daily.windSpeed10mMax.getOrElse(i) { 0.0 }),
            windDirection = daily.windDirection10mDominant.getOrElse(i) { 0 },
            precipitation = Precipitation.fromMm(daily.precipitationSum.getOrElse(i) { 0.0 }),
            uvIndexMax = daily.uvIndexMax.getOrElse(i) { 0.0 },
        )
    }.distinctBy { it.date }

    val minutelyForecastList = minutely15?.let { m15 ->
        val parsedM15Times = m15.time.map { parseIsoDateTime(it, utcOffsetSeconds) }
        val startM15Index = parsedM15Times.indexOfFirst { it >= nowMillis - 15 * 60 * 1000L }.takeIf { it != -1 } ?: 0
        val endM15Index = minOf(startM15Index + 12, m15.time.size)
        (startM15Index until endM15Index).map { i ->
            orinasa.njarasoa.maripanatokana.domain.model.MinutelyForecast(
                time = parsedM15Times[i],
                precipitation = Precipitation.fromMm(m15.precipitation.getOrElse(i) { 0.0 }),
            )
        }
    } ?: emptyList()

    return WeatherData(
        utcOffsetSeconds = utcOffsetSeconds,
        temperature = Temperature.fromCelsius(c.temperature),
        feelsLike = Temperature.fromCelsius(c.apparentTemperature),
        tempMin = Temperature.fromCelsius(daily.temperatureMin.firstOrNull() ?: c.temperature),
        tempMax = Temperature.fromCelsius(daily.temperatureMax.firstOrNull() ?: c.temperature),
        weatherCode = c.weatherCode,
        iconCode = wmoIconCode(c.weatherCode, isDay),
        locationName = locationName,
        locationSubtext = locationSubtext,
        pressure = Pressure.fromHPa(c.pressureMsl),
        humidity = c.relativeHumidity,
        dewPoint = Temperature.fromCelsius(c.dewPoint),
        windSpeed = WindSpeed.fromMetersPerSecond(c.windSpeed),
        windDeg = c.windDirection,
        windGust = if (c.windGusts > 0) WindSpeed.fromMetersPerSecond(c.windGusts) else null,
        rain = if (c.rain > 0) Precipitation.fromMm(c.rain) else null,
        snow = if (c.snowfall > 0) Precipitation.fromMm(c.snowfall) else null,
        cloudCover = c.cloudCover,
        uvIndex = c.uvIndex,
        visibility = c.visibility.toInt(),
        sunrise = sunriseEpoch,
        sunset = sunsetEpoch,
        dailySunrise = dailySunriseMillis,
        dailySunset = dailySunsetMillis,
        hourlyForecast = hourlyForecast,
        minutelyForecast = minutelyForecastList,
        dailyForecast = dailyForecast,
        alerts = emptyList(),
    )
}
