package orinasa.njarasoa.maripanatokana.domain.model

enum class ActivityTier {
    EXCELLENT,
    GOOD,
    FAIR,
    POOR
}

data class ActivityIndices(
    val running: ActivityTier,
    val laundry: ActivityTier,
    val uvSafety: ActivityTier,
) {
    companion object {
        fun fromWeatherData(data: WeatherData): ActivityIndices {
            val tempC = data.temperature.celsius
            val humidity = data.humidity
            val windMs = data.windSpeed.metersPerSecond
            val precipMm = (data.rain?.mm ?: 0.0) + (data.snow?.mm ?: 0.0)
            val uv = data.uvIndex

            val runningTier = if (precipMm > 0.0 || tempC < 0.0 || tempC > 32.0 || windMs > 12.0) {
                ActivityTier.POOR
            } else if (tempC in 8.0..22.0 && humidity < 70 && windMs < 7.0) {
                ActivityTier.EXCELLENT
            } else if (tempC in 2.0..28.0 && humidity < 85) {
                ActivityTier.GOOD
            } else {
                ActivityTier.FAIR
            }

            val laundryTier = if (precipMm > 0.0 || humidity > 80) {
                ActivityTier.POOR
            } else if (tempC > 18.0 && humidity < 50 && windMs >= 2.0) {
                ActivityTier.EXCELLENT
            } else if (tempC > 12.0 && humidity < 65) {
                ActivityTier.GOOD
            } else {
                ActivityTier.FAIR
            }

            val uvTier = if (uv < 3.0) {
                ActivityTier.EXCELLENT
            } else if (uv < 6.0) {
                ActivityTier.GOOD
            } else if (uv < 8.0) {
                ActivityTier.FAIR
            } else {
                ActivityTier.POOR
            }

            return ActivityIndices(
                running = runningTier,
                laundry = laundryTier,
                uvSafety = uvTier,
            )
        }
    }
}
