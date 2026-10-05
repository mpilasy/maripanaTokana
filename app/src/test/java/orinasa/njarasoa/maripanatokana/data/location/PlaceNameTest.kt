package orinasa.njarasoa.maripanatokana.data.location

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaceNameTest {
    @Test fun keepsHyphenatedNames() {
        assertEquals("Saint-Denis", shortPlaceName("Saint-Denis"))
        assertEquals("Aix-en-Provence", shortPlaceName("Aix-en-Provence"))
    }

    @Test fun splitsOnCommaSemicolonAndSpacedHyphen() {
        assertEquals("Paris", shortPlaceName("Paris, France"))
        assertEquals("Foo", shortPlaceName("Foo - Bar"))
        assertEquals("A", shortPlaceName("A;B"))
    }
}
