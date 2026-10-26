package orinasa.njarasoa.maripanatokana.ui.weather

import android.content.Context
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import orinasa.njarasoa.maripanatokana.data.remote.GeocodingResult
import orinasa.njarasoa.maripanatokana.data.repository.WeatherCache
import orinasa.njarasoa.maripanatokana.data.settings.AppSettingsRepository
import orinasa.njarasoa.maripanatokana.domain.model.Precipitation
import orinasa.njarasoa.maripanatokana.domain.model.Pressure
import orinasa.njarasoa.maripanatokana.domain.model.Temperature
import orinasa.njarasoa.maripanatokana.domain.model.WeatherData
import orinasa.njarasoa.maripanatokana.domain.model.WindSpeed
import orinasa.njarasoa.maripanatokana.domain.repository.AlertsResult
import orinasa.njarasoa.maripanatokana.domain.repository.LocationRepository
import orinasa.njarasoa.maripanatokana.domain.repository.WeatherRepository
import orinasa.njarasoa.maripanatokana.testutil.FakePrefs

@OptIn(ExperimentalCoroutinesApi::class)
class WeatherViewModelTest {
    @get:Rule val tmp = TemporaryFolder()

    private val prefsByName = mutableMapOf<String, FakePrefs>()
    private val context = mockk<Context>(relaxed = true).also { ctx ->
        every { ctx.getSharedPreferences(any(), any()) } answers { prefsByName.getOrPut(firstArg()) { FakePrefs() } }
    }
    private val weatherRepository = mockk<WeatherRepository>()
    private val locationRepository = mockk<LocationRepository>()
    private lateinit var weatherCache: WeatherCache

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        weatherCache = WeatherCache(tmp.newFolder("cache"))
        coEvery { weatherRepository.getWeather(any(), any()) } answers {
            Result.success(data("${firstArg<Double>()},${secondArg<Double>()}"))
        }
        coEvery { weatherRepository.fetchAlerts(any(), any()) } returns Result.success(AlertsResult(emptyList(), emptyList()))
        coEvery { weatherRepository.romanPlaceName(any(), any()) } returns null
        coEvery { locationRepository.getLastLocation() } returns Result.success(1.0 to 2.0)
        coEvery { locationRepository.getFreshLocation() } returns Result.success(1.0 to 2.0)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = WeatherViewModel(weatherRepository, locationRepository, AppSettingsRepository(context), weatherCache, context)

    private fun data(name: String, timestamp: Long = System.currentTimeMillis()) = WeatherData(
        temperature = Temperature.fromCelsius(21.5),
        feelsLike = Temperature.fromCelsius(20.0),
        tempMin = Temperature.fromCelsius(15.0),
        tempMax = Temperature.fromCelsius(25.0),
        weatherCode = 3,
        iconCode = "03d",
        locationName = name,
        pressure = Pressure.fromHPa(1013.0),
        humidity = 50,
        dewPoint = Temperature.fromCelsius(10.0),
        windSpeed = WindSpeed.fromMetersPerSecond(4.0),
        windDeg = null,
        windGust = null,
        rain = Precipitation.fromMm(0.0),
        snow = null,
        cloudCover = 10,
        uvIndex = null,
        visibility = null,
        sunrise = 1L,
        sunset = 2L,
        hourlyForecast = emptyList(),
        timestamp = timestamp,
    )

    /** Runs queued coroutines until [condition] holds; work on Dispatchers.IO runs on real threads. */
    private fun TestScope.awaitCondition(condition: () -> Boolean) {
        repeat(300) {
            advanceUntilIdle()
            if (condition()) return
            Thread.sleep(10)
        }
        fail("condition not reached")
    }

    private fun WeatherViewModel.shownName() = (uiState.value as? WeatherUiState.Success)?.data?.locationName

    @Test
    fun placePickedWhileSharedTextResolvesIsKept() = runTest {
        val search = CompletableDeferred<Result<List<GeocodingResult>>>()
        coEvery { weatherRepository.searchLocation("Paris") } coAnswers { search.await() }
        val vm = viewModel()

        vm.handleSharedText("Paris")
        awaitCondition { runCatching { coVerify { weatherRepository.searchLocation("Paris") } }.isSuccess }

        // The user goes back to the GPS place before the search answers
        vm.switchToLocation(null)
        awaitCondition { vm.shownName() == "1.0,2.0" }

        search.complete(Result.success(listOf(GeocodingResult(id = 1, name = "Paris", latitude = 48.85, longitude = 2.35))))
        advanceUntilIdle()

        assertEquals("1.0,2.0", vm.shownName())
        assertFalse(prefsByName.getValue("private_prefs").contains("advanced_override_lat"))
    }

    @Test
    fun sharedTextWithNoMatchShowsTheGpsPlace() = runTest {
        coEvery { weatherRepository.searchLocation("Nowhere") } returns Result.success(emptyList())
        val vm = viewModel()

        vm.handleSharedText("Nowhere")

        awaitCondition { vm.shownName() == "1.0,2.0" }
    }

    @Test
    fun previewSkipsGpsRefreshWhileGpsCacheIsRecent() = runTest {
        weatherCache.save("gps", data("Here"))
        val vm = viewModel()

        vm.setLocationOverride(10.0, 20.0, "Preview")
        awaitCondition { vm.shownName() == "Preview" }
        Thread.sleep(50)
        advanceUntilIdle()

        coVerify(exactly = 0) { locationRepository.getFreshLocation() }
    }

    @Test
    fun previewRefreshesRunOneGpsRefreshAtATimeAndCacheIt() = runTest {
        val fix = CompletableDeferred<Result<Pair<Double, Double>>>()
        coEvery { locationRepository.getFreshLocation() } coAnswers { fix.await() }
        val vm = viewModel()

        vm.setLocationOverride(10.0, 20.0, "Preview")
        awaitCondition { runCatching { coVerify { locationRepository.getFreshLocation() } }.isSuccess }
        // Let each refresh run: a refresh cancels the one before it while it is still queued
        vm.refresh()
        advanceUntilIdle()
        vm.refresh()
        advanceUntilIdle()
        awaitCondition { runCatching { coVerify(exactly = 3) { weatherRepository.getWeather(10.0, 20.0) } }.isSuccess }

        coVerify(exactly = 1) { locationRepository.getFreshLocation() }

        fix.complete(Result.success(3.0 to 4.0))
        awaitCondition { weatherCache.load("gps") != null }
        assertEquals("3.0,4.0", weatherCache.load("gps")?.locationName)
        assertNotNull(vm.shownName())
    }
}
