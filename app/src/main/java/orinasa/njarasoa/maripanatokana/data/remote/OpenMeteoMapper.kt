package orinasa.njarasoa.maripanatokana.data.remote

import orinasa.njarasoa.maripanatokana.domain.model.DailyForecast
import orinasa.njarasoa.maripanatokana.domain.model.HourlyForecast
import orinasa.njarasoa.maripanatokana.domain.model.Precipitation
import orinasa.njarasoa.maripanatokana.domain.model.Pressure
import orinasa.njarasoa.maripanatokana.domain.model.Temperature
import orinasa.njarasoa.maripanatokana.domain.model.WeatherData
import orinasa.njarasoa.maripanatokana.domain.model.WindSpeed
import java.text.SimpleDateFormat
import java.util.Locale

fun OpenMeteoResponse.toDomain(locationName: String): WeatherData {
    val c = current
    val isDay = c.isDay == 1
    val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US)
    val dayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    val sunriseEpoch = daily.sunrise.firstOrNull()?.let {
        dateFormat.parse(it)?.time?.div(1000) ?: 0L
    } ?: 0L
    val sunsetEpoch = daily.sunset.firstOrNull()?.let {
        dateFormat.parse(it)?.time?.div(1000) ?: 0L
    } ?: 0L

    val nowMillis = System.currentTimeMillis()

    val hourlyForecast = hourly.time.indices
        .map { i ->
            val epoch = dateFormat.parse(hourly.time[i])?.time ?: 0L
            HourlyForecast(
                time = epoch,
                temperature = Temperature.fromCelsius(hourly.temperature2m[i]),
                weatherCode = hourly.weatherCode[i],
                precipProbability = hourly.precipitationProbability[i],
            )
        }
        .filter { it.time >= nowMillis }
        .take(24)

    val dailyForecast = daily.time.indices.map { i ->
        val epoch = dayFormat.parse(daily.time[i])?.time ?: 0L
        DailyForecast(
            date = epoch,
            tempMax = Temperature.fromCelsius(daily.temperatureMax[i]),
            tempMin = Temperature.fromCelsius(daily.temperatureMin[i]),
            weatherCode = daily.weatherCode[i],
            precipProbability = daily.precipitationProbabilityMax[i],
        )
    }

    return WeatherData(
        temperature = Temperature.fromCelsius(c.temperature),
        feelsLike = Temperature.fromCelsius(c.apparentTemperature),
        tempMin = Temperature.fromCelsius(daily.temperatureMin.firstOrNull() ?: c.temperature),
        tempMax = Temperature.fromCelsius(daily.temperatureMax.firstOrNull() ?: c.temperature),
        weatherCode = c.weatherCode,
        iconCode = wmoIconCode(c.weatherCode, isDay),
        locationName = locationName,
        pressure = Pressure.fromHPa(c.pressureMsl),
        humidity = c.relativeHumidity,
        windSpeed = WindSpeed.fromMetersPerSecond(c.windSpeed),
        windDeg = c.windDirection,
        windGust = if (c.windGusts > 0) WindSpeed.fromMetersPerSecond(c.windGusts) else null,
        rain = if (c.rain > 0) Precipitation.fromMm(c.rain) else null,
        snow = if (c.snowfall > 0) Precipitation.fromMm(c.snowfall) else null,
        uvIndex = c.uvIndex,
        visibility = c.visibility.toInt(),
        sunrise = sunriseEpoch,
        sunset = sunsetEpoch,
        hourlyForecast = hourlyForecast,
        dailyForecast = dailyForecast,
    )
}
