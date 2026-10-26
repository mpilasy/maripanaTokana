package orinasa.njarasoa.maripanatokana.data.repository

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ExpiringValueTest {
    private var clock = 0L
    private var loads = 0
    private val value = ExpiringValue<String, Int>(ttlMs = 1_000) { clock }

    private suspend fun get(key: String) = value.get(key) { ++loads }

    @Test
    fun `value is kept until it expires`() = runTest {
        assertEquals(1, get("a"))
        clock = 999
        assertEquals(1, get("a"))
        clock = 1_000
        assertEquals(2, get("a"))
    }

    @Test
    fun `other key loads again`() = runTest {
        get("a")
        assertEquals(2, get("b"))
    }

    @Test
    fun `callers at the same time share one load`() = runTest {
        val results = List(3) { async { value.get("a") { delay(100); ++loads } } }.awaitAll()
        assertEquals(listOf(1, 1, 1), results)
    }

    @Test
    fun `failed load is not kept`() = runTest {
        runCatching { value.get("a") { error("offline") } }
        assertEquals(1, get("a"))
    }
}
