package orinasa.njarasoa.maripanatokana.data.source

import orinasa.njarasoa.maripanatokana.data.settings.AppSettingsRepository
import orinasa.njarasoa.maripanatokana.domain.model.GeocodingSource
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeocodingSourceSelector @Inject constructor(
    private val settingsRepository: AppSettingsRepository,
    private val system: SystemGeocoderSource,
    private val nominatim: NominatimGeocodingSource,
) {
    fun current(): GeocodingDataSource = when (settingsRepository.current.geocodingSource) {
        GeocodingSource.SYSTEM_GEOCODER -> system
        GeocodingSource.NOMINATIM -> nominatim
    }
}
