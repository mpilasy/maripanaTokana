package orinasa.njarasoa.maripanatokana.data.source

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import orinasa.njarasoa.maripanatokana.domain.model.FetchError
import retrofit2.HttpException
import java.io.IOException
import java.io.InterruptedIOException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.UnknownHostException

/** Classifies a weather-fetch exception. 401/403 only mean "bad key" for Pirate Weather. */
fun Throwable.toFetchError(isPirateWeather: Boolean = false): FetchError = when {
    this is UnknownHostException || this is ConnectException || this is NoRouteToHostException -> FetchError.Offline
    this is InterruptedIOException -> FetchError.Timeout // includes SocketTimeoutException
    this is HttpException -> when {
        code() == 429 -> FetchError.RateLimited
        isPirateWeather && (code() == 401 || code() == 403) -> FetchError.InvalidApiKey
        code() >= 500 -> FetchError.Server
        else -> FetchError.Generic
    }
    else -> FetchError.Generic
}

private fun Throwable.isTransient() = this is IOException || (this is HttpException && code() >= 500)

/** Runs [block], retrying once after [delayMs] on IOException or HTTP 5xx (never on 4xx). */
suspend fun <T> retryOnce(delayMs: Long = 1_000L, block: suspend () -> T): T {
    try {
        return block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        if (!e.isTransient()) throw e
    }
    delay(delayMs)
    return block()
}
