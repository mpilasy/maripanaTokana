package orinasa.njarasoa.maripanatokana.domain.repository

interface LocationRepository {
    suspend fun getLocation(): Result<Pair<Double, Double>>
}
