package orinasa.njarasoa.maripanatokana.ui.weather

import android.content.Context
import android.content.SharedPreferences
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import orinasa.njarasoa.maripanatokana.domain.model.WeatherData
import orinasa.njarasoa.maripanatokana.domain.repository.LocationRepository
import orinasa.njarasoa.maripanatokana.domain.repository.WeatherRepository

@OptIn(ExperimentalCoroutinesApi::class)
class WeatherViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var locationRepository: LocationRepository
    private lateinit var weatherRepository: WeatherRepository
    private lateinit var context: Context
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var viewModel: WeatherViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        locationRepository = mockk()
        weatherRepository = mockk()
        context = mockk()
        sharedPreferences = mockk()
        editor = mockk(relaxed = true)

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

        viewModel = WeatherViewModel(weatherRepository, locationRepository, context)
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
}
