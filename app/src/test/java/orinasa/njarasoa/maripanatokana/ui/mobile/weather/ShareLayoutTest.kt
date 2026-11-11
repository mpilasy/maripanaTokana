// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

package orinasa.njarasoa.maripanatokana.ui.weather

import org.junit.Assert.assertEquals
import org.junit.Test

class ShareLayoutTest {

    @Test
    fun shareItemsPerLine_fillsEvenLinesOfAtMostMax() {
        assertEquals(6, shareItemsPerLine(24, 6))
        assertEquals(5, shareItemsPerLine(10, 5))
        assertEquals(4, shareItemsPerLine(7, 5))
        assertEquals(4, shareItemsPerLine(16, 5))
    }

    @Test
    fun shareItemsPerLine_keepsAShortListOnOneLine() {
        assertEquals(3, shareItemsPerLine(3, 6))
        assertEquals(1, shareItemsPerLine(0, 6))
    }
}
