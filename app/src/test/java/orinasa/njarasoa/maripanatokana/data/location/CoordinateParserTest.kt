package orinasa.njarasoa.maripanatokana.data.location

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CoordinateParserTest {
    @Test fun parsesDecimals() {
        assertEquals(-18.91 to 47.52, parseCoordinates("-18.91, 47.52"))
    }

    @Test fun parsesIntegers() {
        assertEquals(-18.0 to 47.0, parseCoordinates("-18, 47"))
        assertEquals(10.5 to 20.0, parseCoordinates(" 10.5,20 "))
    }

    @Test fun rejectsOutOfRange() {
        assertNull(parseCoordinates("95.0, 10.0"))
        assertNull(parseCoordinates("10, 181"))
        assertNull(parseCoordinates("-90.1, 0"))
    }

    @Test fun acceptsBounds() {
        assertEquals(90.0 to -180.0, parseCoordinates("90, -180"))
    }

    @Test fun rejectsGarbage() {
        assertNull(parseCoordinates("Paris"))
        assertNull(parseCoordinates("1.2.3, 4"))
        assertNull(parseCoordinates("10"))
        assertNull(parseCoordinates("10, 20, 30"))
    }
}
