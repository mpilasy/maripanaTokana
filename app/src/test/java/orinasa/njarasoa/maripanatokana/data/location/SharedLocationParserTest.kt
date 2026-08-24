package orinasa.njarasoa.maripanatokana.data.location

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class SharedLocationParserTest {

    @Test
    fun testParseRawDecimalCoordinates() {
        val parsed = SharedLocationParser.parseLocationText("-18.8792, 47.5079")
        assertNotNull(parsed)
        val p = parsed!!
        assertEquals(-18.8792, p.latitude, 0.0001)
        assertEquals(47.5079, p.longitude, 0.0001)
    }

    @Test
    fun testParseGeoUri() {
        val parsed = SharedLocationParser.parseLocationText("geo:48.8566,2.3522?z=17")
        assertNotNull(parsed)
        val p = parsed!!
        assertEquals(48.8566, p.latitude, 0.0001)
        assertEquals(2.3522, p.longitude, 0.0001)
    }

    @Test
    fun testParseGoogleMapsUrl() {
        val urlText = "Dropped pin\nhttps://www.google.com/maps/place/Paris/@48.856614,2.352222,17z/data=!4m2!3m1!1s0x0:0x0"
        val parsed = SharedLocationParser.parseLocationText(urlText)
        assertNotNull(parsed)
        val p = parsed!!
        assertEquals(48.856614, p.latitude, 0.0001)
        assertEquals(2.352222, p.longitude, 0.0001)
    }

    @Test
    fun testParseDmsCoordinates() {
        val parsed = SharedLocationParser.parseLocationText("""48°51'23.8"N 2°21'07.2"E""")
        assertNotNull(parsed)
        val p = parsed!!
        assertEquals(48.8566, p.latitude, 0.001)
        assertEquals(2.3520, p.longitude, 0.001)
    }

    @Test
    fun testParseInvalidTextReturnsNull() {
        val parsed = SharedLocationParser.parseLocationText("Just some random text with no coordinates")
        assertNull(parsed)
    }
}
