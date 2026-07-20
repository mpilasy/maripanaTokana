package orinasa.njarasoa.maripanatokana.domain.model

data class HourlyAirQuality(
    val time: Long, // epoch millis, absolute (UTC-parsed)
    val usValue: Int,
    val europeanValue: Int,
) {
    fun tier(standard: AqiStandard): AqiTier =
        AirQualityIndex.tierFor(if (standard == AqiStandard.EUROPEAN) europeanValue else usValue, standard)
}
