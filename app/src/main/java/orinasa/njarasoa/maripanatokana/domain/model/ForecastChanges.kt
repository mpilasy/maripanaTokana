// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

package orinasa.njarasoa.maripanatokana.domain.model

import kotlinx.serialization.Serializable
import kotlin.math.abs

/** One day of a saved forecast: the ISO date at the place, high and low (°C), rain sum (mm), wind max (km/h). */
@Serializable
data class SnapshotDay(val date: String, val high: Double, val low: Double, val rainSum: Double, val windMax: Double)

/** The daily forecast as it was at [savedAt] (epoch millis). */
@Serializable
data class ForecastSnapshot(val savedAt: Long, val days: List<SnapshotDay>)

/**
 * A large change of one day's forecast since the snapshot. [date] is the ISO date at the place. [value] is the
 * high's change (°C) for the high, the new rain sum (mm) for rain, the new wind max (km/h) for wind, and 0 for dry.
 */
@Serializable
data class ForecastChange(val date: String, val kind: Kind, val value: Double) {
    enum class Kind { HIGH_WARMER, HIGH_COOLER, RAIN_NOW, DRY_NOW, RAIN_MORE, RAIN_LESS, WIND_STRONGER, WIND_WEAKER }
}

/** True for a change up (warmer, wetter, windier), shown with the warm accent. */
fun isUpChange(change: ForecastChange): Boolean = change.kind == ForecastChange.Kind.HIGH_WARMER ||
    change.kind == ForecastChange.Kind.RAIN_NOW || change.kind == ForecastChange.Kind.RAIN_MORE ||
    change.kind == ForecastChange.Kind.WIND_STRONGER

private const val HIGH_CHANGE_C = 3.0
private const val RAIN_DAY_MM = 1.0
private const val RAIN_CHANGE_MM = 5.0
private const val RAIN_CHANGE_FRACTION = 0.5
private const val WIND_CHANGE_KMH = 15.0
private const val MAX_CHANGES = 4
/** Days after the first day of the current forecast (today) that are compared. */
private const val DAYS_AHEAD = 6

private const val HOUR_MS = 60 * 60 * 1000L

/**
 * The large changes between an [old] and the [current] forecast, for the days from tomorrow up to 6 days ahead
 * that are in both. The first day of [current] is today. In date order; for one date: high, rain, wind. At most 4.
 */
fun forecastChanges(old: ForecastSnapshot, current: ForecastSnapshot): List<ForecastChange> {
    val oldByDate = old.days.associateBy { it.date }
    val changes = mutableListOf<ForecastChange>()
    for (now in current.days.drop(1).take(DAYS_AHEAD)) {
        val before = oldByDate[now.date] ?: continue
        val highDelta = now.high - before.high
        if (abs(highDelta) >= HIGH_CHANGE_C) {
            changes += ForecastChange(now.date, if (highDelta > 0) ForecastChange.Kind.HIGH_WARMER else ForecastChange.Kind.HIGH_COOLER, highDelta)
        }
        val rainDelta = now.rainSum - before.rainSum
        when {
            before.rainSum < RAIN_DAY_MM && now.rainSum >= RAIN_DAY_MM ->
                changes += ForecastChange(now.date, ForecastChange.Kind.RAIN_NOW, now.rainSum)
            before.rainSum >= RAIN_DAY_MM && now.rainSum < RAIN_DAY_MM ->
                changes += ForecastChange(now.date, ForecastChange.Kind.DRY_NOW, 0.0)
            before.rainSum >= RAIN_DAY_MM && now.rainSum >= RAIN_DAY_MM &&
                abs(rainDelta) >= RAIN_CHANGE_MM && abs(rainDelta) >= RAIN_CHANGE_FRACTION * before.rainSum ->
                changes += ForecastChange(now.date, if (rainDelta > 0) ForecastChange.Kind.RAIN_MORE else ForecastChange.Kind.RAIN_LESS, now.rainSum)
        }
        val windDelta = now.windMax - before.windMax
        if (abs(windDelta) >= WIND_CHANGE_KMH) {
            changes += ForecastChange(now.date, if (windDelta > 0) ForecastChange.Kind.WIND_STRONGER else ForecastChange.Kind.WIND_WEAKER, now.windMax)
        }
    }
    return changes.take(MAX_CHANGES)
}

/** The snapshot whose age at [now] is closest to 24 hours, if that age is 18 to 36 hours; else null. */
fun snapshotToCompare(snapshots: List<ForecastSnapshot>, now: Long): ForecastSnapshot? =
    snapshots.filter { now - it.savedAt in 18 * HOUR_MS..36 * HOUR_MS }
        .minByOrNull { abs(now - it.savedAt - 24 * HOUR_MS) }

/** The snapshot of [data]'s daily forecast, saved at [savedAt]. */
fun toSnapshot(data: WeatherData, savedAt: Long): ForecastSnapshot = ForecastSnapshot(
    savedAt = savedAt,
    days = data.dailyForecast.map {
        SnapshotDay(
            date = localDate(it.date, data.utcOffsetSeconds),
            high = it.tempMax.celsius,
            low = it.tempMin.celsius,
            rainSum = it.precipitation.mm,
            windMax = it.windSpeed.metersPerSecond * 3.6,
        )
    },
)
