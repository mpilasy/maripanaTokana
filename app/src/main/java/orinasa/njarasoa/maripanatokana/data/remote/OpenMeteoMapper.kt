package orinasa.njarasoa.maripanatokana.data.remote

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

    val sunriseEpoch = daily.sunrise.firstOrNull()?.let {
        dateFormat.parse(it)?.time?.div(1000) ?: 0L
    } ?: 0L
    val sunsetEpoch = daily.sunset.firstOrNull()?.let {
        dateFormat.parse(it)?.time?.div(1000) ?: 0L
    } ?: 0L

    return WeatherData(
        temperature = Temperature.fromCelsius(c.temperature),
        feelsLike = Temperature.fromCelsius(c.apparentTemperature),
        tempMin = Temperature.fromCelsius(daily.temperatureMin.firstOrNull() ?: c.temperature),
        tempMax = Temperature.fromCelsius(daily.temperatureMax.firstOrNull() ?: c.temperature),
        description = wmoDescription(c.weatherCode),
        iconCode = wmoIconCode(c.weatherCode, isDay),
        locationName = locationName,
        pressure = Pressure.fromHPa(c.pressureMsl),
        humidity = c.relativeHumidity,
        windSpeed = WindSpeed.fromMetersPerSecond(c.windSpeed),
        windDeg = c.windDirection,
        windGust = if (c.windGusts > 0) WindSpeed.fromMetersPerSecond(c.windGusts) else null,
        rain = if (c.rain > 0) Precipitation.fromMm(c.rain) else null,
        snow = if (c.snowfall > 0) Precipitation.fromMm(c.snowfall) else null,
        visibility = c.visibility.toInt(),
        sunrise = sunriseEpoch,
        sunset = sunsetEpoch,
    )
}
