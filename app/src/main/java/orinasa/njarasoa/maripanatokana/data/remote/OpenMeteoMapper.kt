package orinasa.njarasoa.maripanatokana.data.remote

import orinasa.njarasoa.maripanatokana.domain.model.AlertLevel
import orinasa.njarasoa.maripanatokana.domain.model.DailyForecast
import orinasa.njarasoa.maripanatokana.domain.model.HourlyForecast
import orinasa.njarasoa.maripanatokana.domain.model.Precipitation
import orinasa.njarasoa.maripanatokana.domain.model.Pressure
import orinasa.njarasoa.maripanatokana.domain.model.Temperature
import orinasa.njarasoa.maripanatokana.domain.model.WeatherAlert
import orinasa.njarasoa.maripanatokana.domain.model.WeatherData
import orinasa.njarasoa.maripanatokana.domain.model.WindSpeed
import java.util.Calendar
import java.util.TimeZone

private fun parseIsoDateTime(iso: String, utcOffsetSeconds: Int): Long {
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

fun deriveAlerts(c: OpenMeteoCurrent, h: OpenMeteoHourly, d: OpenMeteoDaily, utcOffsetSeconds: Int): List<WeatherAlert> {
    val alerts = mutableListOf<WeatherAlert>()
    val nowMillis = System.currentTimeMillis()

    // Pre-parse timestamps once using the location's timezone
    val parsedTimes = h.time.map { parseIsoDateTime(it, utcOffsetSeconds) }

    // Scan next 24 hours of hourly forecast starting from "now" at the location
    val startIndex = parsedTimes.indexOfFirst { it >= nowMillis }.coerceAtLeast(0)

    // Bolt: Optimize loop to avoid multiple array creations
    var maxWind = maxOf(c.windSpeed, c.windGusts)
    val windowCodes = mutableSetOf(c.weatherCode)

    val maxI = minOf(24, h.time.size - startIndex)
    for (i in 0 until maxI) {
        val idx = startIndex + i
        windowCodes.add(h.weatherCode[idx])
        val wind = h.windSpeed10m.getOrElse(idx) { 0.0 }
        if (wind > maxWind) {
            maxWind = wind
        }
    }

    val maxTemp = maxOf(
        c.temperature,
        d.temperatureMax.getOrElse(0) { c.temperature },
        d.temperatureMax.getOrElse(1) { c.temperature }
    )
    val minTemp = minOf(
        c.temperature,
        d.temperatureMin.getOrElse(0) { c.temperature },
        d.temperatureMin.getOrElse(1) { c.temperature }
    )

    fun hasCode(targetCodes: List<Int>) = targetCodes.any { windowCodes.contains(it) }

    // Thunderstorm: 95, 96, 99
    if (hasCode(listOf(95, 96, 99))) {
        alerts.add(WeatherAlert(AlertLevel.WARNING, "alert_title_thunderstorm", "alert_desc_thunderstorm", "derived"))
    }

    // Heavy Rain: 65, 82
    if (hasCode(listOf(65, 82))) {
        alerts.add(WeatherAlert(AlertLevel.WARNING, "alert_title_heavy_rain", "alert_desc_heavy_rain", "derived"))
    }

    // Heavy Snow: 75, 86
    if (hasCode(listOf(75, 86))) {
        alerts.add(WeatherAlert(AlertLevel.WARNING, "alert_title_heavy_snow", "alert_desc_heavy_snow", "derived"))
    }

    // High Wind
    if (maxWind > 15.0) {
        alerts.add(WeatherAlert(AlertLevel.WARNING, "alert_title_high_wind", "alert_desc_high_wind", "derived"))
    }

    // Extreme Heat
    if (maxTemp > 35.0) {
        alerts.add(WeatherAlert(AlertLevel.WARNING, "alert_title_extreme_heat", "alert_desc_extreme_heat", "derived"))
    }

    // Extreme Cold
    if (minTemp < -15.0) {
        alerts.add(WeatherAlert(AlertLevel.WARNING, "alert_title_extreme_cold", "alert_desc_extreme_cold", "derived"))
    }

    // High UV
    if (c.uvIndex > 8.0) {
        alerts.add(WeatherAlert(AlertLevel.WATCH, "alert_title_high_uv", "alert_desc_high_uv", "derived"))
    }

    return alerts
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

    val hourlyForecast = ArrayList<HourlyForecast>(endIndex - startIndex)
    val seenHourlyTimes = HashSet<Long>()
    for (i in startIndex until endIndex) {
        val epoch = parsedHourlyTimes[i]
        if (epoch >= nowMillis && seenHourlyTimes.add(epoch)) {
            hourlyForecast.add(
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
            )
        }
    }

    val parsedDailyTimes = daily.time.map { parseIsoDate(it, utcOffsetSeconds) }

    val dailyForecast = ArrayList<DailyForecast>(daily.time.size)
    val seenDailyDates = HashSet<Long>()
    for (i in daily.time.indices) {
        val epoch = parsedDailyTimes[i]
        if (seenDailyDates.add(epoch)) {
            dailyForecast.add(
                DailyForecast(
                    date = epoch,
                    tempMax = Temperature.fromCelsius(daily.temperatureMax[i]),
                    tempMin = Temperature.fromCelsius(daily.temperatureMin[i]),
                    weatherCode = if (i == 0) c.weatherCode else daily.weatherCode[i],
                    precipProbability = daily.precipitationProbabilityMax[i],
                    windSpeed = WindSpeed.fromMetersPerSecond(daily.windSpeed10mMax.getOrElse(i) { 0.0 }),
                    windDirection = daily.windDirection10mDominant.getOrElse(i) { 0 },
                    precipitation = Precipitation.fromMm(daily.precipitationSum.getOrElse(i) { 0.0 }),
                )
            )
        }
    }

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
        dailyForecast = dailyForecast,
        alerts = deriveAlerts(c, hourly, daily, utcOffsetSeconds),
    )
}
