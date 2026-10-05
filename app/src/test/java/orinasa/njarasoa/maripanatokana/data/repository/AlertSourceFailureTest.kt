package orinasa.njarasoa.maripanatokana.data.repository

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import orinasa.njarasoa.maripanatokana.data.remote.NhcApiService
import orinasa.njarasoa.maripanatokana.data.remote.NhcStormCollection
import orinasa.njarasoa.maripanatokana.data.remote.NominatimApiService
import orinasa.njarasoa.maripanatokana.data.remote.NwsApiService
import orinasa.njarasoa.maripanatokana.data.settings.AppSettingsRepository
import orinasa.njarasoa.maripanatokana.domain.model.AppSettings
import org.junit.Assert.assertEquals
import org.junit.Test

class AlertSourceFailureTest {
    private val nws = mockk<NwsApiService>()
    private val nhc = mockk<NhcApiService>()
    private val nominatim = mockk<NominatimApiService>()

    private fun repo(settings: AppSettings): WeatherRepositoryImpl {
        val settingsRepo = mockk<AppSettingsRepository>()
        every { settingsRepo.current } returns settings
        return WeatherRepositoryImpl(
            context = mockk(relaxed = true),
            nwsApiService = nws,
            gdacsApiService = mockk(),
            meteoAlarmApiService = mockk(),
            jmaApiService = mockk(),
            ecccApiService = mockk(),
            bomApiService = mockk(),
            nhcApiService = nhc,
            settingsRepository = settingsRepo,
            weatherSourceSelector = mockk(),
            geocodingSelector = mockk(),
            nominatimApiService = nominatim,
        )
    }

    @Test
    fun `enabled throwing source is reported, disabled and non-applicable are not`() = runTest {
        coEvery { nominatim.reverse(any(), any(), any(), any()) } throws java.io.IOException("offline")
        coEvery { nws.getActiveAlerts(any()) } throws java.io.IOException("boom")
        coEvery { nhc.getCurrentStorms() } returns NhcStormCollection(emptyList())
        // Miami: inside the US box. NHC disabled; ECCC/BOM/JMA not applicable; GDACS covered by regional.
        val result = repo(AppSettings(alertsNhcEnabled = false)).fetchAlerts(25.76, -80.19).getOrThrow()
        assertEquals(listOf("NWS"), result.failedSources)
    }

    @Test
    fun `disabled throwing source is not reported`() = runTest {
        coEvery { nominatim.reverse(any(), any(), any(), any()) } throws java.io.IOException("offline")
        coEvery { nhc.getCurrentStorms() } returns NhcStormCollection(emptyList())
        val result = repo(AppSettings(alertsNwsEnabled = false)).fetchAlerts(25.76, -80.19).getOrThrow()
        assertEquals(emptyList<String>(), result.failedSources)
    }

    @Test
    fun `NWS is not queried outside the US`() = runTest {
        coEvery { nominatim.reverse(any(), any(), any(), any()) } throws java.io.IOException("offline")
        coEvery { nws.getActiveAlerts(any()) } throws java.io.IOException("boom")
        // Antananarivo, with the global sources (GDACS, NHC) disabled so only NWS could fail.
        val settings = AppSettings(alertsGdacsEnabled = false, alertsNhcEnabled = false)
        val result = repo(settings).fetchAlerts(-18.88, 47.51).getOrThrow()
        assertEquals(emptyList<String>(), result.failedSources)
    }
}
