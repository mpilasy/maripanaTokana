// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

package orinasa.njarasoa.maripanatokana.data.location

import orinasa.njarasoa.maripanatokana.domain.model.LocationFix

/**
 * Abstraction for location retrieval supporting multiple implementations.
 * - PlayServicesLocationProvider: Uses Google Play Services for enhanced accuracy
 * - NativeLocationProvider: Uses Android's built-in LocationManager (F-Droid compatible)
 */
interface LocationProvider {
    /**
     * Get the last known location from the device.
     * @return Success with the fix and its time, or failure if no cached location exists
     */
    suspend fun getLastLocation(): Result<LocationFix>

    /**
     * Request a fresh location update from the device.
     * @return Success with the fix and its time (marked as fallback when a cached fix replaced a failed
     * request), or failure if location cannot be acquired
     */
    suspend fun getFreshLocation(): Result<LocationFix>
}
