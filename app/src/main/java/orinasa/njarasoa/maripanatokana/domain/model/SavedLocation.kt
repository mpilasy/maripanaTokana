// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

package orinasa.njarasoa.maripanatokana.domain.model

import kotlinx.serialization.Serializable

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
