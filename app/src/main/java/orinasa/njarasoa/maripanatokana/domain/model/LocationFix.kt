// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

package orinasa.njarasoa.maripanatokana.domain.model

/**
 * A device location. [timeMs] is the time of the fix (epoch ms). [fallback] is true when a fresh fix
 * was asked for but failed, and a cached fix was used.
 */
data class LocationFix(
    val latitude: Double,
    val longitude: Double,
    val timeMs: Long,
    val fallback: Boolean = false,
) {
    /** True when the fix is a fallback, or more than 30 minutes old at [nowMs]. */
    fun isOld(nowMs: Long = System.currentTimeMillis()): Boolean = fallback || nowMs - timeMs > OLD_AFTER_MS

    companion object {
        const val OLD_AFTER_MS = 30 * 60 * 1000L
    }
}
