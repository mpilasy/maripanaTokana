package orinasa.njarasoa.maripanatokana.data.source

import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import orinasa.njarasoa.maripanatokana.domain.model.FetchError
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class FetchErrorsTest {

    private fun http(code: Int) =
        HttpException(Response.error<Any>(code, "".toResponseBody("text/plain".toMediaType())))

    @Test
    fun classifiesNetworkExceptions() {
        assertEquals(FetchError.Offline, UnknownHostException().toFetchError())
        assertEquals(FetchError.Offline, ConnectException().toFetchError())
        assertEquals(FetchError.Timeout, SocketTimeoutException().toFetchError())
        assertEquals(FetchError.Generic, IOException().toFetchError())
        assertEquals(FetchError.Generic, IllegalStateException().toFetchError())
    }

    @Test
    fun classifiesHttpStatuses() {
        assertEquals(FetchError.RateLimited, http(429).toFetchError())
        assertEquals(FetchError.Server, http(503).toFetchError())
        assertEquals(FetchError.Generic, http(404).toFetchError())
        assertEquals(FetchError.Generic, http(401).toFetchError())
        assertEquals(FetchError.InvalidApiKey, http(401).toFetchError(isPirateWeather = true))
        assertEquals(FetchError.InvalidApiKey, http(403).toFetchError(isPirateWeather = true))
    }

    @Test
    fun retryOnce_retriesOnceOnIOException() = runBlocking {
        var calls = 0
        val result = retryOnce(delayMs = 1) { if (++calls == 1) throw IOException("drop") else "ok" }
        assertEquals("ok", result)
        assertEquals(2, calls)
    }

    @Test
    fun retryOnce_retriesOn5xxButGivesUpAfterSecondFailure() {
        var calls = 0
        assertThrows(HttpException::class.java) {
            runBlocking { retryOnce<String>(delayMs = 1) { calls++; throw http(503) } }
        }
        assertEquals(2, calls)
    }

    @Test
    fun retryOnce_doesNotRetryOn4xx() {
        var calls = 0
        assertThrows(HttpException::class.java) {
            runBlocking { retryOnce<String>(delayMs = 1) { calls++; throw http(429) } }
        }
        assertEquals(1, calls)
    }
}
