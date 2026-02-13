package orinasa.njarasoa.maripanatokana.data.repository

import orinasa.njarasoa.maripanatokana.data.location.LocationProvider
import orinasa.njarasoa.maripanatokana.domain.repository.LocationRepository
import javax.inject.Inject

class LocationRepositoryImpl @Inject constructor(
    private val locationProvider: LocationProvider,
) : LocationRepository {

    override suspend fun getLastLocation(): Result<Pair<Double, Double>> {
        return locationProvider.getLastLocation()
    }

    override suspend fun getFreshLocation(): Result<Pair<Double, Double>> {
        return locationProvider.getFreshLocation()
    }
}
