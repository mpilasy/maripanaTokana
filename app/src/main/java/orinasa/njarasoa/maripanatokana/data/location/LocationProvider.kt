package orinasa.njarasoa.maripanatokana.data.location

/**
 * Abstraction for location retrieval supporting multiple implementations.
 * - PlayServicesLocationProvider: Uses Google Play Services for enhanced accuracy
 * - NativeLocationProvider: Uses Android's built-in LocationManager (F-Droid compatible)
 */
interface LocationProvider {
    /**
     * Get the last known location from the device.
     * @return Success with Pair(latitude, longitude) or failure if no cached location exists
     */
    suspend fun getLastLocation(): Result<Pair<Double, Double>>

    /**
     * Request a fresh location update from the device.
     * @return Success with Pair(latitude, longitude) or failure if location cannot be acquired
     */
    suspend fun getFreshLocation(): Result<Pair<Double, Double>>
}
