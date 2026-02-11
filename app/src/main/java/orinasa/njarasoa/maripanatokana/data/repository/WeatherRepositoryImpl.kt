package orinasa.njarasoa.maripanatokana.data.repository

import android.location.Geocoder
import orinasa.njarasoa.maripanatokana.data.remote.OpenMeteoApiService
import orinasa.njarasoa.maripanatokana.data.remote.toDomain
import orinasa.njarasoa.maripanatokana.domain.model.WeatherData
import orinasa.njarasoa.maripanatokana.domain.repository.WeatherRepository
import java.util.Locale
import javax.inject.Inject

class WeatherRepositoryImpl @Inject constructor(
    private val apiService: OpenMeteoApiService,
    private val geocoder: Geocoder,
) : WeatherRepository {

    override suspend fun getWeather(lat: Double, lon: Double): Result<WeatherData> {
        return try {
            val response = apiService.getForecast(latitude = lat, longitude = lon)
            val locationName = try {
                @Suppress("DEPRECATION")
                val addr = geocoder.getFromLocation(lat, lon, 1)?.firstOrNull()
                addr?.locality
                    ?: addr?.subAdminArea
                    ?: addr?.adminArea
                    ?: "%.2f, %.2f".format(Locale.US, lat, lon)
            } catch (_: Exception) {
                "%.2f, %.2f".format(Locale.US, lat, lon)
            }
            Result.success(response.toDomain(locationName))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
