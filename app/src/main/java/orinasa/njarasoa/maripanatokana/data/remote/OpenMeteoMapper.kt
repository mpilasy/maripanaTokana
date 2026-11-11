// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

package orinasa.njarasoa.maripanatokana.data.remote

import orinasa.njarasoa.maripanatokana.domain.model.ChartHour
import orinasa.njarasoa.maripanatokana.domain.model.DailyForecast
import orinasa.njarasoa.maripanatokana.domain.model.HourlyForecast
import orinasa.njarasoa.maripanatokana.domain.model.Precipitation
import orinasa.njarasoa.maripanatokana.domain.model.Pressure
import orinasa.njarasoa.maripanatokana.domain.model.Temperature
import orinasa.njarasoa.maripanatokana.domain.model.WeatherData
import orinasa.njarasoa.maripanatokana.domain.model.WindSpeed
import orinasa.njarasoa.maripanatokana.ui.chart.chartDaylight
import orinasa.njarasoa.maripanatokana.ui.chart.chartHours
import kotlin.math.roundToInt
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

/** Days in the daily list. The request asks for one more, so that the chart view always has 240 hours. */
private const val DAILY_DAYS = 10

fun OpenMeteoResponse.toDomain(locationName: String, locationSubtext: String? = null): WeatherData {
    val c = current

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

    // The first hour at or after now, in absolute UTC times
    val startIndex = parsedHourlyTimes.indexOfFirst { it >= nowMillis }.takeIf { it != -1 } ?: 0

    val endIndex = minOf(startIndex + 24, hourly.time.size)

    val hourlyForecast = (startIndex until endIndex).mapNotNull { i ->
        val epoch = parsedHourlyTimes[i]
        if (epoch < nowMillis) return@mapNotNull null

        HourlyForecast(
            time = epoch,
            temperature = Temperature.fromCelsius(hourly.temperature2m.getOrElse(i) { 0.0 }),
            weatherCode = hourly.weatherCode.getOrElse(i) { 0 },
            precipProbability = hourly.precipitationProbability.getOrNull(i) ?: 0,
            windSpeed = WindSpeed.fromMetersPerSecond(hourly.windSpeed10m.getOrNull(i) ?: Double.NaN),
            windDirection = hourly.windDirection10m.getOrNull(i) ?: 0,
            pressure = Pressure.fromHPa(hourly.pressureMsl.getOrNull(i) ?: Double.NaN),
            precipitation = Precipitation.fromMm(hourly.precipitation.getOrNull(i) ?: Double.NaN),
            uvIndex = hourly.uvIndex.getOrNull(i),
            humidity = hourly.relativeHumidity.getOrNull(i),
            dewPoint = hourly.dewPoint.getOrNull(i)?.let { Temperature.fromCelsius(it) },
            windGust = hourly.windGusts10m.getOrNull(i)?.let { WindSpeed.fromMetersPerSecond(it) },
        )
    }.distinctBy { it.time }

    val chartRange = chartHours(parsedHourlyTimes, nowMillis)
    val chartNight = chartDaylight(parsedHourlyTimes.slice(chartRange), dailySunriseMillis, dailySunsetMillis)
    val chartHourly = chartRange.mapIndexed { k, i ->
        ChartHour(
            time = parsedHourlyTimes[i],
            temperature = hourly.temperature2m.getOrNull(i)?.let { Temperature.fromCelsius(it) },
            dewPoint = hourly.dewPoint.getOrNull(i)?.let { Temperature.fromCelsius(it) },
            weatherCode = hourly.weatherCode.getOrElse(i) { 0 },
            isNight = chartNight[k],
            precipitation = hourly.precipitation.getOrNull(i)?.let { Precipitation.fromMm(it) },
            precipProbability = hourly.precipitationProbability.getOrNull(i),
            windSpeed = hourly.windSpeed10m.getOrNull(i)?.let { WindSpeed.fromMetersPerSecond(it) },
            windDirection = hourly.windDirection10m.getOrNull(i),
            windGust = hourly.windGusts10m.getOrNull(i)?.let { WindSpeed.fromMetersPerSecond(it) },
            pressure = hourly.pressureMsl.getOrNull(i)?.let { Pressure.fromHPa(it) },
            humidity = hourly.relativeHumidity.getOrNull(i),
            uvIndex = hourly.uvIndex.getOrNull(i),
        )
    }.distinctBy { it.time }

    val parsedDailyTimes = daily.time.map { parseIsoDate(it, utcOffsetSeconds) }
    val nightCodes = nightWeatherCodes(parsedHourlyTimes, hourly.weatherCode, dailySunsetMillis, dailySunriseMillis)

    val dailyForecast = daily.time.indices.take(DAILY_DAYS).map { i ->
        val epoch = parsedDailyTimes[i]
        DailyForecast(
            date = epoch,
            tempMax = Temperature.fromCelsius(daily.temperatureMax[i]),
            tempMin = Temperature.fromCelsius(daily.temperatureMin[i]),
            weatherCode = if (i == 0) c.weatherCode else daily.weatherCode[i],
            precipProbability = daily.precipitationProbabilityMax.getOrNull(i) ?: 0,
            windSpeed = WindSpeed.fromMetersPerSecond(daily.windSpeed10mMax.getOrNull(i) ?: Double.NaN),
            windDirection = daily.windDirection10mDominant.getOrNull(i) ?: 0,
            precipitation = Precipitation.fromMm(daily.precipitationSum.getOrNull(i) ?: Double.NaN),
            uvIndexMax = daily.uvIndexMax.getOrNull(i) ?: 0.0,
            nightWeatherCode = nightCodes.getOrNull(i),
            pressure = daily.pressureMslMean.getOrNull(i)?.let { Pressure.fromHPa(it) },
            humidity = daily.relativeHumidityMean.getOrNull(i)?.roundToInt(),
            dewPoint = daily.dewPointMean.getOrNull(i)?.let { Temperature.fromCelsius(it) },
        )
    }.distinctBy { it.date }

    val minutelyForecastList = minutely15?.let { m15 ->
        val parsedM15Times = m15.time.map { parseIsoDateTime(it, utcOffsetSeconds) }
        val startM15Index = parsedM15Times.indexOfFirst { it >= nowMillis - 15 * 60 * 1000L }.takeIf { it != -1 } ?: 0
        val endM15Index = minOf(startM15Index + 12, m15.time.size)
        (startM15Index until endM15Index).map { i ->
            orinasa.njarasoa.maripanatokana.domain.model.MinutelyForecast(
                time = parsedM15Times[i],
                precipitation = Precipitation.fromMm(m15.precipitation.getOrNull(i) ?: 0.0),
            )
        }
    } ?: emptyList()

    return WeatherData(
        utcOffsetSeconds = utcOffsetSeconds,
        temperature = Temperature.fromCelsius(c.temperature),
        feelsLike = Temperature.fromCelsius(c.apparentTemperature ?: Double.NaN),
        tempMin = Temperature.fromCelsius(daily.temperatureMin.firstOrNull() ?: c.temperature),
        tempMax = Temperature.fromCelsius(daily.temperatureMax.firstOrNull() ?: c.temperature),
        weatherCode = c.weatherCode,
        locationName = locationName,
        locationSubtext = locationSubtext,
        pressure = Pressure.fromHPa(c.pressureMsl ?: Double.NaN),
        humidity = c.relativeHumidity,
        dewPoint = Temperature.fromCelsius(c.dewPoint ?: Double.NaN),
        windSpeed = WindSpeed.fromMetersPerSecond(c.windSpeed ?: Double.NaN),
        windDeg = c.windDirection,
        windGust = c.windGusts?.takeIf { it > 0 }?.let { WindSpeed.fromMetersPerSecond(it) },
        rain = c.rain?.takeIf { it > 0 }?.let { Precipitation.fromMm(it) },
        snow = c.snowfall?.takeIf { it > 0 }?.let { Precipitation.fromMm(it) },
        cloudCover = c.cloudCover,
        uvIndex = c.uvIndex,
        visibility = c.visibility?.toInt(),
        sunrise = sunriseEpoch,
        sunset = sunsetEpoch,
        dailySunrise = dailySunriseMillis,
        dailySunset = dailySunsetMillis,
        hourlyForecast = hourlyForecast,
        chartHourly = chartHourly,
        minutelyForecast = minutelyForecastList,
        dailyForecast = dailyForecast,
        alerts = emptyList(),
    )
}
