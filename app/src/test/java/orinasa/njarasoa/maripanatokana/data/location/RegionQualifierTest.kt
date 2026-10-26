// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

package orinasa.njarasoa.maripanatokana.data.location

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RegionQualifierTest {
    @Test
    fun `postal abbreviation names its province or state`() {
        assertTrue(matchesRegionQualifier("ON", "Ontario", null, "Canada"))
        assertTrue(matchesRegionQualifier("ma", "Massachusetts", null, "United States"))
    }

    @Test
    fun `text inside a word does not match`() {
        assertFalse(matchesRegionQualifier("on", "Washington", null, "United States"))
        assertFalse(matchesRegionQualifier("land", "England", null, "United Kingdom"))
    }

    @Test
    fun `start of any word matches`() {
        assertTrue(matchesRegionQualifier("kingdom", "England", null, "United Kingdom"))
        assertTrue(matchesRegionQualifier("fra", "Île-de-France", null, "France"))
    }

    @Test
    fun `blank qualifier matches everything`() {
        assertTrue(matchesRegionQualifier("", null, null, null))
    }
}
