// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

package orinasa.njarasoa.maripanatokana.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class TempColorsTest {
    @Test
    fun `gives the stop colour on a stop`() {
        assertEquals(Color(0xFF4FC3F7), temperatureColor(0.0))
        assertEquals(Color(0xFF81C784), temperatureColor(15.0))
        assertEquals(Color(0xFFFFD54F), temperatureColor(25.0))
    }

    @Test
    fun `interpolates between two stops`() {
        // Half way from 0xDCE775 (20 °C) to 0xFFD54F (25 °C)
        assertEquals(Color(0xFFEEDE62), temperatureColor(22.5))
        // A fifth of the way from 0x4FC3F7 (0 °C) to 0x4DD0E1 (10 °C)
        assertEquals(Color(0xFF4FC6F3), temperatureColor(2.0))
    }

    @Test
    fun `clamps outside the scale`() {
        assertEquals(Color(0xFFB39DDB), temperatureColor(-35.0))
        assertEquals(Color(0xFFE57373), temperatureColor(48.0))
    }
}
