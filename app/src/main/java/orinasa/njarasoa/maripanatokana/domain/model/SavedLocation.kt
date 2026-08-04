package orinasa.njarasoa.maripanatokana.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class SavedLocation(
    val id: String,
    val name: String,
    val subtext: String? = null,
    val latitude: Double,
    val longitude: Double,
)
