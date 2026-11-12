// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

package orinasa.njarasoa.maripanatokana.domain.model

enum class ActivityTier {
    EXCELLENT,
    GOOD,
    FAIR,
    POOR,
    // Sun protection at night: there is no sun, so no tier applies
    NOT_NEEDED,
}

data class ActivityIndices(
    val running: ActivityTier,
    val laundry: ActivityTier,
    val uvSafety: ActivityTier,
    /** Dry air (low dew point or humidity) dries the skin: POOR means use moisturizer */
    val skinComfort: ActivityTier,
    /** Rain or snow soon, or frost, spoils a car wash */
    val carWash: ActivityTier,
    /** Weather only (no traffic): ice, snow, fog, heavy rain, strong gusts */
    val driving: ActivityTier,
) {
    companion object {
        // Twin: web/src/lib/domain/activityIndices.ts
        fun fromWeatherData(data: WeatherData, isDay: Boolean): ActivityIndices {
            val tempC = data.temperature.celsius
            val humidity = data.humidity ?: 70 // neutral when unavailable
            val windMs = data.windSpeed.metersPerSecond
            val precipMm = (data.rain?.mm ?: 0.0) + (data.snow?.mm ?: 0.0)
            val uv = data.uvIndex ?: 6.0 // neutral (FAIR) when unavailable
            return ActivityIndices(
                running = runningTier(tempC, humidity, windMs, precipMm),
                laundry = laundryTier(tempC, humidity, windMs, precipMm),
                uvSafety = uvTier(uv, isDay),
                skinComfort = skinTier(data.dewPoint.celsius, data.humidity),
                carWash = carWashTier(tempC, precipMm, data.dailyForecast),
                driving = drivingTier(data.weatherCode, data.visibility, data.windGust?.metersPerSecond, tempC, precipMm),
            )
        }

        private fun skinTier(dewC: Double, humidity: Int?): ActivityTier = when {
            dewC < -10.0 || (humidity != null && humidity < 25) -> ActivityTier.POOR
            dewC < -2.0 || (humidity != null && humidity < 35) -> ActivityTier.FAIR
            dewC < 5.0 -> ActivityTier.GOOD
            else -> ActivityTier.EXCELLENT
        }

        // A day is wet when rain or snow is likely (50 % or more) or 1 mm or more is forecast
        private fun isWet(day: DailyForecast) = day.precipProbability >= 50 || day.precipitation.mm >= 1.0

        /** [days]: the daily forecast from today; today, tomorrow and the day after count. */
        private fun carWashTier(tempC: Double, precipMm: Double, days: List<DailyForecast>): ActivityTier {
            val next = days.take(3)
            return when {
                precipMm > 0.0 || tempC < 0.0 || next.take(2).any(::isWet) -> ActivityTier.POOR
                next.getOrNull(2)?.let(::isWet) == true || (next.getOrNull(1)?.precipProbability ?: 0) >= 30 -> ActivityTier.FAIR
                next.any { it.precipProbability >= 20 } || tempC < 5.0 -> ActivityTier.GOOD
                else -> ActivityTier.EXCELLENT
            }
        }

        // WMO weather codes
        private val ICE_OR_HEAVY_SNOW = setOf(56, 57, 66, 67, 75, 86, 96, 99) // freezing drizzle or rain, heavy snow, hail
        private val SNOW_FOG_HEAVY_RAIN = setOf(45, 48, 65, 71, 73, 77, 82, 85, 95)
        private val LIGHT_RAIN = setOf(51, 53, 55, 61, 63, 80, 81)

        private fun drivingTier(code: Int, visibilityM: Int?, gustMs: Double?, tempC: Double, precipMm: Double): ActivityTier {
            val vis = visibilityM ?: Int.MAX_VALUE
            val gust = gustMs ?: 0.0
            return when {
                code in ICE_OR_HEAVY_SNOW || vis < 200 || gust > 25.0 -> ActivityTier.POOR
                code in SNOW_FOG_HEAVY_RAIN || vis < 1000 || gust > 17.0 || (tempC <= 1.0 && precipMm > 0.0) -> ActivityTier.FAIR
                code in LIGHT_RAIN || vis < 5000 || gust > 12.0 -> ActivityTier.GOOD
                else -> ActivityTier.EXCELLENT
            }
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

        private fun uvTier(uv: Double, isDay: Boolean): ActivityTier = when {
            !isDay -> ActivityTier.NOT_NEEDED
            uv < 3.0 -> ActivityTier.EXCELLENT
            uv < 6.0 -> ActivityTier.GOOD
            uv < 8.0 -> ActivityTier.FAIR
            else -> ActivityTier.POOR
        }
    }
}
