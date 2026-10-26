// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

package orinasa.njarasoa.maripanatokana.data.location

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScriptTest {
    @Test
    fun `accepts Latin names, with accents and punctuation`() {
        assertTrue(isLatinScript("Saint-Étienne, Île-de-France"))
        assertTrue(isLatinScript("Antananarivo 101"))
    }

    @Test
    fun `rejects other scripts`() {
        assertFalse(isLatinScript("القاهرة"))
        assertFalse(isLatinScript("東京"))
        assertFalse(isLatinScript("काठमाडौं"))
    }
}
