package orinasa.njarasoa.maripanatokana.widget

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.OffsetDateTime
import java.util.Locale

class WidgetFormatTest {
    @Test
    fun dayNameUsesMillisAndLocationTimezone() {
        // Local midnight Tue 2026-10-06 at UTC+03:00 is Mon 21:00 UTC; device-timezone formatting would give Monday.
        val millis = OffsetDateTime.parse("2026-10-06T00:00:00+03:00").toInstant().toEpochMilli()
        assertEquals("Tue", widgetDayName(millis, 3 * 3600, Locale.ENGLISH))
        assertEquals("Mon", widgetDayName(millis, 0, Locale.ENGLISH))
    }

    @Test
    fun ltrWrapsInIsolate() {
        assertEquals("⁦16°C⁩", ltr("16°C"))
    }
}
