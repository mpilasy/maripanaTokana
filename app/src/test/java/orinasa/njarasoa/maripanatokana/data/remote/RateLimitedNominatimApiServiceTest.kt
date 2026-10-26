// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

package orinasa.njarasoa.maripanatokana.data.remote

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RateLimitedNominatimApiServiceTest {
    private val api = mockk<NominatimApiService>()
    private val place = NominatimPlace(placeId = 1, name = "Toamasina")

    @Test
    fun `same place asked three times at once is one request`() = runTest {
        coEvery { api.reverse(any(), any(), any(), any()) } returns place
        val service = RateLimitedNominatimApiService(api) { testScheduler.currentTime }

        val results = List(3) { async { service.reverse(-18.15, 49.40) } }.awaitAll()

        assertTrue(results.all { it == place })
        coVerify(exactly = 1) { api.reverse(any(), any(), any(), any()) }
    }

    @Test
    fun `other language is a new request`() = runTest {
        coEvery { api.reverse(any(), any(), any(), any()) } returns place
        val service = RateLimitedNominatimApiService(api) { testScheduler.currentTime }

        service.reverse(-18.15, 49.40, language = "en")
        service.reverse(-18.15, 49.40, language = "fr")

        coVerify(exactly = 2) { api.reverse(any(), any(), any(), any()) }
    }

    @Test
    fun `requests are at least one second apart`() = runTest {
        val sentAt = mutableListOf<Long>()
        coEvery { api.reverse(any(), any(), any(), any()) } answers { sentAt += testScheduler.currentTime; place }
        coEvery { api.search(any(), any(), any(), any(), any()) } answers { sentAt += testScheduler.currentTime; emptyList() }
        val service = RateLimitedNominatimApiService(api) { testScheduler.currentTime }

        listOf(
            async { service.reverse(1.0, 2.0) },
            async { service.reverse(3.0, 4.0) },
            async { service.search("Toamasina") },
        ).awaitAll()

        assertEquals(listOf(0L, 1_000L, 2_000L), sentAt)
    }

    @Test
    fun `failed request is not kept`() = runTest {
        coEvery { api.reverse(any(), any(), any(), any()) } throws java.io.IOException("offline") andThen place
        val service = RateLimitedNominatimApiService(api) { testScheduler.currentTime }

        runCatching { service.reverse(1.0, 2.0) }
        assertEquals(place, service.reverse(1.0, 2.0))
    }
}
