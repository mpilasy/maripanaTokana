// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

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
            val humidity = data.humidity ?: 70 // neutral when unavailable
            val windMs = data.windSpeed.metersPerSecond
            val precipMm = (data.rain?.mm ?: 0.0) + (data.snow?.mm ?: 0.0)
            val uv = data.uvIndex ?: 6.0 // neutral (FAIR) when unavailable
            return ActivityIndices(
                running = runningTier(tempC, humidity, windMs, precipMm),
                laundry = laundryTier(tempC, humidity, windMs, precipMm),
                uvSafety = uvTier(uv),
            )
        }

        private fun runningTier(tempC: Double, humidity: Int, windMs: Double, precipMm: Double): ActivityTier = when {
            precipMm > 0.0 || tempC < 0.0 || tempC > 32.0 || windMs > 12.0 -> ActivityTier.POOR
            tempC in 8.0..22.0 && humidity < 70 && windMs < 7.0 -> ActivityTier.EXCELLENT
            tempC in 2.0..28.0 && humidity < 85 -> ActivityTier.GOOD
            else -> ActivityTier.FAIR
        }

        private fun laundryTier(tempC: Double, humidity: Int, windMs: Double, precipMm: Double): ActivityTier = when {
            precipMm > 0.0 || humidity > 80 -> ActivityTier.POOR
            tempC > 18.0 && humidity < 50 && windMs >= 2.0 -> ActivityTier.EXCELLENT
            tempC > 12.0 && humidity < 65 -> ActivityTier.GOOD
            else -> ActivityTier.FAIR
        }

        private fun uvTier(uv: Double): ActivityTier = when {
            uv < 3.0 -> ActivityTier.EXCELLENT
            uv < 6.0 -> ActivityTier.GOOD
            uv < 8.0 -> ActivityTier.FAIR
            else -> ActivityTier.POOR
        }
    }
}
