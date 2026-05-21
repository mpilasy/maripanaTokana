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
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

private fun makeDateFormat(pattern: String, utcOffsetSeconds: Int): SimpleDateFormat {
    val sign = if (utcOffsetSeconds >= 0) "+" else "-"
    val abs = Math.abs(utcOffsetSeconds)
    return SimpleDateFormat(pattern, Locale.US).apply {
        timeZone = TimeZone.getTimeZone(String.format(Locale.US, "GMT%s%02d:%02d", sign, abs / 3600, (abs % 3600) / 60))
    }
}

fun deriveAlerts(c: OpenMeteoCurrent, h: OpenMeteoHourly, d: OpenMeteoDaily, utcOffsetSeconds: Int): List<WeatherAlert> {
    val alerts = mutableListOf<WeatherAlert>()
    val nowMillis = System.currentTimeMillis()
    val dateFormat = makeDateFormat("yyyy-MM-dd'T'HH:mm", utcOffsetSeconds)

    // Pre-parse timestamps once using the location's timezone
    val parsedTimes = h.time.map { dateFormat.parse(it)?.time ?: 0L }

    // Scan next 24 hours of hourly forecast starting from "now" at the location
    val startIndex = parsedTimes.indexOfFirst { it >= nowMillis }.coerceAtLeast(0)
    
    val forecastIndices = parsedTimes.indices.filter { it in startIndex until (startIndex + 24) && it < h.time.size }
    val forecastWindow = forecastIndices.map { idx ->
        Triple(h.weatherCode[idx], h.temperature2m[idx], h.windSpeed10m.getOrElse(idx) { 0.0 })
    }

    val codes = listOf(c.weatherCode) + forecastWindow.map { it.first }
    val maxWind = (listOf(c.windSpeed, c.windGusts) + forecastWindow.map { it.third }).maxOrNull() ?: 0.0
    val maxTemp = (listOf(c.temperature) + d.temperatureMax.take(2)).maxOrNull() ?: 0.0
    val minTemp = (listOf(c.temperature) + d.temperatureMin.take(2)).minOrNull() ?: 0.0

    fun hasCode(targetCodes: List<Int>) = codes.any { it in targetCodes }

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
    val dateFormat = makeDateFormat("yyyy-MM-dd'T'HH:mm", utcOffsetSeconds)
    val dayFormat = makeDateFormat("yyyy-MM-dd", utcOffsetSeconds)

    // Pre-parse hourly timestamps once
    val parsedHourlyTimes = hourly.time.map { dateFormat.parse(it)?.time ?: 0L }

    val sunriseEpoch = daily.sunrise.firstOrNull()?.let {
        dateFormat.parse(it)?.time?.div(1000) ?: 0L
    } ?: 0L
    val sunsetEpoch = daily.sunset.firstOrNull()?.let {
        dateFormat.parse(it)?.time?.div(1000) ?: 0L
    } ?: 0L

    val dailySunriseMillis = daily.sunrise.map { dateFormat.parse(it)?.time ?: 0L }
    val dailySunsetMillis = daily.sunset.map { dateFormat.parse(it)?.time ?: 0L }

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

    val parsedDailyTimes = daily.time.map { dayFormat.parse(it)?.time ?: 0L }

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
        )
    }.distinctBy { it.date }

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
