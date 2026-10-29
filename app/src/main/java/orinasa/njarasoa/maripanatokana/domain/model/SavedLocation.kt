// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

package orinasa.njarasoa.maripanatokana.domain.model

import kotlinx.serialization.Serializable
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Serializable
data class SavedLocation(
    val id: String,
    val name: String,
    val subtext: String? = null,
    val latitude: Double,
    val longitude: Double,
    /** The user's own name for this favorite, shown instead of [name] when set. */
    val label: String? = null,
) {
    val displayName: String get() = label ?: name
}

/** The places sorted by distance from ([lat], [lon]), nearest first. */
fun List<SavedLocation>.sortedByDistanceFrom(lat: Double, lon: Double): List<SavedLocation> =
    sortedBy { greatCircleKm(lat, lon, it.latitude, it.longitude) }

private fun greatCircleKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2) * sin(dLat / 2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2) * sin(dLon / 2)
    return 2 * 6371.0 * asin(sqrt(a))
}
