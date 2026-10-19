package orinasa.njarasoa.maripanatokana.domain.model

private const val DAY = 24 * 3_600_000L

/**
 * Each day's worst air quality as (primary, secondary) index, from the hourly forecast. Null for a day
 * the forecast covers for less than 6 hours (air-quality forecasts only reach about 5 days ahead);
 * today counts with whatever hours are left of it. [dates] are each day's local midnight, epoch millis.
 */
fun dailyAirQuality(dates: List<Long>, hourly: List<HourlyAirQuality>, standard: AqiStandard): List<Pair<Int, Int>?> =
    dates.mapIndexed { i, start ->
        val day = hourly.filter { it.time >= start && it.time < start + DAY }
        if (day.size < (if (i == 0) 1 else 6)) return@mapIndexed null
        val us = day.maxOf { it.usValue }
        val eu = day.maxOf { it.europeanValue }
        if (standard == AqiStandard.EUROPEAN) eu to us else us to eu
    }
