package orinasa.njarasoa.maripanatokana.ui.weather

import android.content.Context
import android.content.SharedPreferences
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import orinasa.njarasoa.maripanatokana.domain.repository.AlertsResult
import orinasa.njarasoa.maripanatokana.R
import orinasa.njarasoa.maripanatokana.data.remote.GeocodingResult
import orinasa.njarasoa.maripanatokana.data.repository.WeatherCache
import orinasa.njarasoa.maripanatokana.data.settings.AppSettingsRepository
import orinasa.njarasoa.maripanatokana.domain.model.AppSettings
import orinasa.njarasoa.maripanatokana.domain.model.Precipitation
import orinasa.njarasoa.maripanatokana.domain.model.Pressure
import orinasa.njarasoa.maripanatokana.domain.model.Temperature
import orinasa.njarasoa.maripanatokana.domain.model.WeatherData
import orinasa.njarasoa.maripanatokana.domain.model.WindSpeed
import orinasa.njarasoa.maripanatokana.domain.repository.LocationRepository
import orinasa.njarasoa.maripanatokana.domain.repository.WeatherRepository
import orinasa.njarasoa.maripanatokana.testutil.FakePrefs

@OptIn(ExperimentalCoroutinesApi::class)
class WeatherViewModelTest {

    @get:Rule val tmp = TemporaryFolder()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var locationRepository: LocationRepository
    private lateinit var weatherRepository: WeatherRepository
    private lateinit var settingsRepository: AppSettingsRepository
    private lateinit var weatherCache: WeatherCache
    private lateinit var context: Context
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var viewModel: WeatherViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        locationRepository = mockk()
        weatherRepository = mockk()
        settingsRepository = mockk()
        weatherCache = mockk(relaxed = true)
        every { weatherCache.load(any()) } returns null
        context = mockk()
        sharedPreferences = mockk()
        editor = mockk(relaxed = true)

        every { settingsRepository.settings } returns MutableStateFlow(AppSettings())
        every { settingsRepository.current } returns AppSettings()

        every { context.getSharedPreferences(any(), any()) } returns sharedPreferences
        every { sharedPreferences.getBoolean(any(), any()) } returns true
        every { sharedPreferences.getInt(any(), any()) } returns 0
        every { sharedPreferences.getFloat(any(), any()) } returns 0f
        every { sharedPreferences.getString(any(), any()) } returns ""
        every { sharedPreferences.getLong(any(), any()) } returns 0L
        every { sharedPreferences.contains(any()) } returns false
        every { sharedPreferences.edit() } returns editor

        // Default mocks
        val weatherData = mockk<WeatherData>(relaxed = true) {
            every { locationName } returns "Test City"
            every { timestamp } returns System.currentTimeMillis()
        }

        coEvery { weatherRepository.getWeather(any(), any()) } coAnswers {
            delay(2000) // Simulate network delay
            Result.success(weatherData)
        }

        coEvery { locationRepository.getLastLocation() } coAnswers {
            delay(100) // Fast cache
            Result.success(10.0 to 20.0)
        }

        coEvery { locationRepository.getFreshLocation() } coAnswers {
            delay(3000) // Slow GPS
            Result.success(10.1 to 20.1)
        }

        coEvery { weatherRepository.fetchAlerts(any(), any()) } returns Result.success(AlertsResult(emptyList(), emptyList()))

        viewModel = WeatherViewModel(weatherRepository, locationRepository, settingsRepository, weatherCache, context)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testRefreshPerformance() = runTest(testDispatcher) {
        // Baseline:
        // doFetch calls:
        // 1. getLastLocation (100ms) -> success
        // 2. getWeather (2000ms) -> success (Total 2100ms)
        // 3. getFreshLocation (3000ms) -> success (Total 5100ms)
        // 4. movedSignificantly? Assume yes or usedCached logic.
        // If sequential: 100 + 2000 + 3000 = 5100ms roughly.
        // If parallel: max(100+2000, 3000) = 3000ms roughly. Or slightly more due to logic.

        // We need to ensure movedSignificantly returns true or we force a fetch?
        // In current implementation:
        // if (!usedCached || movedSignificantly(...))
        // usedCached becomes true in step 1.
        // movedSignificantly checks prefs.
        // Prefs return 0f (from setup).
        // getLastLocation returns 10.0, 20.0.
        // saveLocation saves 10.0, 20.0.
        // getFreshLocation returns 10.1, 20.1.
        // movedSignificantly(10.1, 20.1) vs (10.0, 20.0).
        // delta ~ 0.1 deg > 0.045 deg. So movedSignificantly = true.
        // So it will fetch weather again.
        // 3000ms (Fresh Loc) + 2000ms (Weather) = 5000ms.

        // Wait, if parallel:
        // Task 1: 100ms (Loc) + 2000ms (Weather) = 2100ms.
        // Task 2: 3000ms (Loc).
        // Task 2 finishes at 3000ms.
        // Then Task 2 fetches weather (2000ms).
        // Total time for Task 2 chain: 3000 + 2000 = 5000ms.

        // So Parallel total time = 5000ms.
        // Sequential total time:
        // 1. LastLoc (100) -> Weather (2000) = 2100ms.
        // 2. FreshLoc (3000) -> Weather (2000) = 5000ms (relative to start of FreshLoc).
        // Total sequential: 2100 + 5000 = 7100ms.

        // Improvement: 7100ms -> 5000ms.

        val start = currentTime
        viewModel.refresh()
        advanceUntilIdle()
        val end = currentTime
        val duration = end - start

        println("Duration: $duration ms")

        // Assert that parallel execution is faster than sequential sum
        // 100 + 2000 + 3000 + 2000 = 7100
        // We expect < 6000 for parallel

        // Actually, if parallel:
        // T1: 0..2100
        // T2: 0..3000 (Loc) -> 3000..5000 (Weather)
        // Max end time is 5000.

        // If sequential:
        // T1: 0..2100
        // T2: 2100..5100 (Loc) -> 5100..7100 (Weather)
        // Max end time is 7100.

        // So checking if duration < 6000 asserts parallel execution of T2 Loc vs T1.

        assertTrue("Duration should be optimized (parallel). Got $duration ms, expected < 6000 ms", duration < 6000)
    }

    @Test
    fun cachedLocationWeatherFailureShowsFetchError() = runTest(testDispatcher) {
        // Fresh fix is within 5 km of the cached one, and weather always fails.
        every { sharedPreferences.getFloat("last_render_lat", any()) } returns 10.0f
        every { sharedPreferences.getFloat("last_render_lon", any()) } returns 20.0f
        coEvery { weatherRepository.getWeather(any(), any()) } returns Result.failure(Exception("boom"))
        coEvery { locationRepository.getFreshLocation() } coAnswers {
            delay(3000)
            Result.success(10.0 to 20.0)
        }

        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(WeatherUiState.Error(R.string.error_fetch_weather), viewModel.uiState.value)
    }

    @Test
    fun freshLocationFailureAfterCachedWeatherKeepsSuccessWithoutRefreshFailed() = runTest(testDispatcher) {
        coEvery { locationRepository.getFreshLocation() } coAnswers {
            delay(3000)
            Result.failure(Exception("no gps"))
        }

        viewModel.refresh()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is WeatherUiState.Success)
        assertEquals(false, viewModel.refreshFailed.value)
    }

    @Test
    fun freshLocationFailureBeforeSlowCachedWeatherNeverEmitsError() = runTest(testDispatcher) {
        coEvery { locationRepository.getFreshLocation() } coAnswers {
            delay(50)
            Result.failure(Exception("no gps"))
        }
        // Default getWeather takes 2000 ms and succeeds.
        val states = mutableListOf<WeatherUiState>()
        val collector = backgroundScope.launch(testDispatcher) { viewModel.uiState.collect { states.add(it) } }

        viewModel.refresh()
        advanceUntilIdle()
        collector.cancel()

        assertTrue("states=$states", states.none { it is WeatherUiState.Error })
        assertTrue(viewModel.uiState.value is WeatherUiState.Success)
        assertEquals(false, viewModel.refreshFailed.value)
    }

    @Test
    fun freshFixNearTheCachedOneWaitsForItsWeather() = runTest(testDispatcher) {
        // The fresh fix answers while the weather for the cached fix (10, 20) is still loading
        coEvery { locationRepository.getFreshLocation() } coAnswers {
            delay(500)
            Result.success(10.001 to 20.001)
        }

        viewModel.refresh()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is WeatherUiState.Success)
        coVerify(exactly = 1) { weatherRepository.getWeather(10.0, 20.0) }
        coVerify(exactly = 0) { weatherRepository.getWeather(10.001, 20.001) }
    }

    @Test
    fun freshFixFarFromTheCachedOneLoadsItsWeather() = runTest(testDispatcher) {
        // The fresh fix is about 55 km from the cached fix (10, 20)
        coEvery { locationRepository.getFreshLocation() } coAnswers {
            delay(500)
            Result.success(10.5 to 20.0)
        }

        viewModel.refresh()
        advanceUntilIdle()

        coVerify(exactly = 1) { weatherRepository.getWeather(10.5, 20.0) }
    }

    @Test
    fun languageChangeRenamesTheGpsPlace() = runTest(testDispatcher) {
        // Current location, no favorite and no typed-in place
        every { sharedPreferences.getString("active_location_id", any()) } returns null
        every { sharedPreferences.getFloat("advanced_override_lat", any()) } returns Float.NaN
        every { sharedPreferences.getFloat("advanced_override_lon", any()) } returns Float.NaN
        coEvery { weatherRepository.romanPlaceName(any(), any()) } returns null
        coEvery { weatherRepository.getWeather(any(), any()) } coAnswers {
            Result.success(realData("سانت بول", firstArg(), secondArg()))
        }
        viewModel = WeatherViewModel(weatherRepository, locationRepository, settingsRepository, weatherCache, context)
        viewModel.refresh()
        advanceUntilIdle()
        assertEquals("سانت بول", (viewModel.uiState.value as WeatherUiState.Success).data.locationName)

        coEvery { weatherRepository.localPlaceName(any(), any()) } returns ("Saint Paul" to null)
        viewModel.cycleLanguage()
        advanceUntilIdle()

        assertEquals("Saint Paul", (viewModel.uiState.value as WeatherUiState.Success).data.locationName)
    }

    /**
     * Real prefs, settings and cache, for the tests of the shared-text and background GPS jobs.
     * The repositories answer at once; work on Dispatchers.IO runs on real threads.
     */
    private inner class JobsFixture {
        val prefsByName = mutableMapOf<String, FakePrefs>()
        val context = mockk<Context>(relaxed = true).also { ctx ->
            every { ctx.getSharedPreferences(any(), any()) } answers { prefsByName.getOrPut(firstArg()) { FakePrefs() } }
        }
        val weatherRepository = mockk<WeatherRepository>()
        val locationRepository = mockk<LocationRepository>()
        val weatherCache = WeatherCache(tmp.newFolder())

        init {
            coEvery { weatherRepository.getWeather(any(), any()) } answers {
                Result.success(realData("${firstArg<Double>()},${secondArg<Double>()}", firstArg(), secondArg()))
            }
            coEvery { weatherRepository.fetchAlerts(any(), any()) } returns Result.success(AlertsResult(emptyList(), emptyList()))
            coEvery { weatherRepository.romanPlaceName(any(), any()) } returns null
            coEvery { locationRepository.getLastLocation() } returns Result.success(1.0 to 2.0)
            coEvery { locationRepository.getFreshLocation() } returns Result.success(1.0 to 2.0)
        }

        fun viewModel() = WeatherViewModel(weatherRepository, locationRepository, AppSettingsRepository(context), weatherCache, context)
    }

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
    fun placePickedWhileSharedTextResolvesIsKept() = runTest(testDispatcher) {
        val f = JobsFixture()
        val search = CompletableDeferred<Result<List<GeocodingResult>>>()
        coEvery { f.weatherRepository.searchLocation("Paris") } coAnswers { search.await() }
        val vm = f.viewModel()

        vm.handleSharedText("Paris")
        awaitCondition { runCatching { coVerify { f.weatherRepository.searchLocation("Paris") } }.isSuccess }

        // The user goes back to the GPS place before the search answers
        vm.switchToLocation(null)
        awaitCondition { vm.shownName() == "1.0,2.0" }

        search.complete(Result.success(listOf(GeocodingResult(id = 1, name = "Paris", latitude = 48.85, longitude = 2.35))))
        advanceUntilIdle()

        assertEquals("1.0,2.0", vm.shownName())
        assertFalse(f.prefsByName.getValue("private_prefs").contains("advanced_override_lat"))
    }

    @Test
    fun sharedTextWithNoMatchShowsTheGpsPlace() = runTest(testDispatcher) {
        val f = JobsFixture()
        coEvery { f.weatherRepository.searchLocation("Nowhere") } returns Result.success(emptyList())
        val vm = f.viewModel()

        vm.handleSharedText("Nowhere")

        awaitCondition { vm.shownName() == "1.0,2.0" }
    }

    @Test
    fun sharedCoordinatesShowThePlaceName() = runTest(testDispatcher) {
        val f = JobsFixture()
        coEvery { f.weatherRepository.localPlaceName(any(), any()) } returns ("Antananarivo" to "Analamanga, Madagascar")
        val vm = f.viewModel()

        vm.handleSharedText("-18.91, 47.52")

        awaitCondition { vm.shownName() == "Antananarivo" }
        // The shared place starts a background GPS refresh on Dispatchers.IO. Let it end before the test does:
        // after tearDown resets Main, its return to Main throws, and the next test fails with that exception.
        awaitCondition { f.weatherCache.load("gps") != null }
        Thread.sleep(50)
        advanceUntilIdle()
    }

    @Test
    fun previewSkipsGpsRefreshWhileGpsCacheIsRecent() = runTest(testDispatcher) {
        val f = JobsFixture()
        f.weatherCache.save("gps", realData("Here", 1.0, 2.0))
        val vm = f.viewModel()

        vm.setLocationOverride(10.0, 20.0, "Preview")
        awaitCondition { vm.shownName() == "Preview" }
        Thread.sleep(50)
        advanceUntilIdle()

        coVerify(exactly = 0) { f.locationRepository.getFreshLocation() }
    }

    @Test
    fun previewRefreshesRunOneGpsRefreshAtATimeAndCacheIt() = runTest(testDispatcher) {
        val f = JobsFixture()
        val fix = CompletableDeferred<Result<Pair<Double, Double>>>()
        coEvery { f.locationRepository.getFreshLocation() } coAnswers { fix.await() }
        val vm = f.viewModel()

        vm.setLocationOverride(10.0, 20.0, "Preview")
        awaitCondition { runCatching { coVerify { f.locationRepository.getFreshLocation() } }.isSuccess }
        // Let each refresh run: a refresh cancels the one before it while it is still queued
        vm.refresh()
        advanceUntilIdle()
        vm.refresh()
        advanceUntilIdle()
        awaitCondition { runCatching { coVerify(exactly = 3) { f.weatherRepository.getWeather(10.0, 20.0) } }.isSuccess }

        coVerify(exactly = 1) { f.locationRepository.getFreshLocation() }

        fix.complete(Result.success(3.0 to 4.0))
        awaitCondition { f.weatherCache.load("gps") != null }
        assertEquals("3.0,4.0", f.weatherCache.load("gps")?.locationName)
        assertNotNull(vm.shownName())
    }

    private fun realData(name: String, lat: Double, lon: Double) = WeatherData(
        temperature = Temperature.fromCelsius(16.5),
        feelsLike = Temperature.fromCelsius(16.0),
        tempMin = Temperature.fromCelsius(4.0),
        tempMax = Temperature.fromCelsius(19.0),
        weatherCode = 0,
        iconCode = "01d",
        locationName = name,
        pressure = Pressure.fromHPa(1013.0),
        humidity = 50,
        dewPoint = Temperature.fromCelsius(5.0),
        windSpeed = WindSpeed.fromMetersPerSecond(2.0),
        windDeg = null,
        windGust = null,
        rain = Precipitation.fromMm(0.0),
        snow = null,
        cloudCover = 0,
        uvIndex = null,
        visibility = null,
        sunrise = 1L,
        sunset = 2L,
        latitude = lat,
        longitude = lon,
    )
}
