package orinasa.njarasoa.maripanatokana.data.location

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlusCodeTest {
    private fun assertNear(expected: Pair<Double, Double>, actual: Pair<Double, Double>) {
        assertEquals(expected.first, actual.first, 1e-5)
        assertEquals(expected.second, actual.second, 1e-5)
    }

    @Test
    fun `decodes full codes to the centre of their area`() {
        assertNear(47.3655625 to 8.5249375, PlusCode.decode("8FVC9G8F+6X"))
        assertNear(37.4220625 to -122.0840625, PlusCode.decode("849VCWC8+R9"))
    }

    @Test
    fun `decodes grid digits past the tenth`() {
        // Q = row 3, column 3 of the 4x5 grid inside 8FVC9G8F+6X
        assertNear(47.3655875 to 8.524984375, PlusCode.decode("8FVC9G8F+6XQ"))
    }

    @Test
    fun `encodes to a 10-digit code that decodes back to the same cell`() {
        assertEquals("8FVC9G8F+6X", PlusCode.encode(47.3655625, 8.5249375))
        assertEquals("849VCWC8+R9", PlusCode.encode(37.42207, -122.08405))
        val (lat, lon) = PlusCode.decode("6GCRPR6C+24")
        assertEquals("6GCRPR6C+24", PlusCode.encode(lat, lon))
    }

    @Test
    fun `recovers short codes nearest the reference`() {
        assertNear(PlusCode.decode("8FVC9G8F+6X"), PlusCode.recoverNearest("9G8F+6X", 47.4, 8.6))
        assertNear(PlusCode.decode("849VCWC8+R9"), PlusCode.recoverNearest("CWC8+R9", 37.4, -122.1))
    }

    @Test
    fun `recovery crosses a cell edge when the reference is near it`() {
        // Reference just south of the 47°N band edge still lands on the Zurich code
        assertNear(PlusCode.decode("8FVC9G8F+6X"), PlusCode.recoverNearest("9G8F+6X", 46.99, 8.6))
    }

    @Test
    fun `finds codes in text with their locality`() {
        val short = PlusCode.find("PR6C+24 Nairobi, Kenya")!!
        assertEquals("PR6C+24", short.code)
        assertFalse(short.isFull)
        assertEquals("Nairobi, Kenya", short.locality)

        val full = PlusCode.find("6gcrpr6c+24")!!
        assertEquals("6GCRPR6C+24", full.code)
        assertTrue(full.isFull)
    }

    @Test
    fun `accepts codes typed without the plus`() {
        assertEquals(PlusCode.Match("5HH9+3G", false, ""), PlusCode.find("5hh93g"))
        assertEquals(PlusCode.Match("PR6C+43", false, "Nairobi"), PlusCode.find("PR6C43 Nairobi"))
        assertEquals(PlusCode.Match("6GCRPR6C+24", true, ""), PlusCode.find("6GCRPR6C24"))
    }

    @Test
    fun `does not take postcodes or words for codes`() {
        assertNull(PlusCode.find("562345"))
        assertNull(PlusCode.find("Rhythm"))
        assertNull(PlusCode.find("CWC8R9X2Q4M"))
    }

    @Test
    fun `ignores text without a code`() {
        assertNull(PlusCode.find("Nairobi"))
        assertNull(PlusCode.find("1+1"))
        assertNull(PlusCode.find("C++ meetup"))
    }
}
