// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

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
        // Coordinates alone are not a place name
        assertNull(p.name)
    }

    @Test
    fun testCoordinatesWithAPlaceNameKeepTheName() {
        val parsed = SharedLocationParser.parseLocationText("Antananarivo\ngeo:-18.8792,47.5079")
        assertEquals("Antananarivo", parsed?.name)
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

    @Test
    fun testParseLinksWithAnEncodedComma() {
        listOf(
            "geo:0,0?q=-18.97%2C49.14",
            "https://www.google.com/maps/search/?api=1&query=-18.97%2C49.14",
            "https://maps.google.com/?q=-18.97%2c49.14",
        ).forEach { link ->
            val parsed = SharedLocationParser.parseLocationText(link)
            assertEquals(link, -18.97, parsed!!.latitude, 0.0001)
            assertEquals(link, 49.14, parsed.longitude, 0.0001)
        }
    }

    @Test
    fun testTextWithAPercentSignStillParses() {
        val parsed = SharedLocationParser.parseLocationText("100% here\n-18.8792, 47.5079")
        assertEquals(-18.8792, parsed!!.latitude, 0.0001)
    }

    @Test
    fun testParsePlaceLinkWithOnlyThePin() {
        // The link that a maps.app.goo.gl short link from Google Photos resolves to
        val parsed = SharedLocationParser.parseLocationText("https://www.google.com/maps/place/5553%2BMCG+Gare+Train,+Ambila+Lemaitso,+Madagascar/data=!4m6!3m5!1s0x21f38ffc892ea313:0x7acf3fb7e2ee5ef2!7e2!8m2!3d-18.8407999!4d49.1535083!18m1!1e1?entry=gps")
        assertEquals(-18.8407999, parsed!!.latitude, 0.0001)
        assertEquals(49.1535083, parsed.longitude, 0.0001)
    }

    @Test
    fun testPlaceLinkUsesThePinNotTheMapCentre() {
        val parsed = SharedLocationParser.parseLocationText("https://www.google.com/maps/place/Ambila/@-18.80,49.10,12z/data=!3m1!4b1!4m6!3m5!1s0x0:0x0!8m2!3d-18.8407999!4d49.1535083")
        assertEquals(-18.8407999, parsed!!.latitude, 0.0001)
        assertEquals(49.1535083, parsed.longitude, 0.0001)
    }
}
