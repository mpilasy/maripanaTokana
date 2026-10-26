// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

package orinasa.njarasoa.maripanatokana.data.remote

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Locale

/**
 * Keeps all Nominatim calls inside its usage policy: one request at a time, at most one per second.
 * One refresh can ask for the same place up to three times (local name, Latin name, country for
 * alerts), so reverse results are kept by coordinates and language and asked for only once.
 */
class RateLimitedNominatimApiService(
    private val api: NominatimApiService,
    private val now: () -> Long = System::currentTimeMillis,
) : NominatimApiService {
    private val mutex = Mutex()
    private var lastRequestAt = Long.MIN_VALUE / 2

    private val reverseCache = object : LinkedHashMap<String, NominatimPlace>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, NominatimPlace>?) = size > MAX_ENTRIES
    }

    override suspend fun reverse(lat: Double, lon: Double, format: String, language: String): NominatimPlace {
        val key = String.format(Locale.US, "%.4f,%.4f,%s,%s", lat, lon, format, language)
        // Callers that wait for the lock find the result the first caller got
        return mutex.withLock {
            reverseCache[key] ?: throttled { api.reverse(lat, lon, format, language) }.also { reverseCache[key] = it }
        }
    }

    override suspend fun search(
        query: String,
        format: String,
        language: String,
        limit: Int,
        addressDetails: Int,
    ): List<NominatimPlace> = mutex.withLock { throttled { api.search(query, format, language, limit, addressDetails) } }

    /** Call with [mutex] held. */
    private suspend fun <T> throttled(request: suspend () -> T): T {
        val wait = lastRequestAt + MIN_INTERVAL_MS - now()
        if (wait > 0) delay(wait)
        lastRequestAt = now()
        return request()
    }

    private companion object {
        const val MIN_INTERVAL_MS = 1_000L
        const val MAX_ENTRIES = 32
    }
}
