// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

package orinasa.njarasoa.maripanatokana.data.remote

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GeocodingAreaTest {
    private fun result(code: String?) = GeocodingResult(1, "x", 0.0, 0.0, featureCode = code)

    @Test
    fun `admin and country codes are areas`() {
        assertTrue(result("ADM1").isArea)
        assertTrue(result("PCLI").isArea)
    }

    @Test
    fun `populated places and unknown codes are not areas`() {
        assertFalse(result("PPLA").isArea)
        assertFalse(result(null).isArea)
    }
}
