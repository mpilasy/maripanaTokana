// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

package orinasa.njarasoa.maripanatokana.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IsDaytimeTest {
    private val midnight = 1_793_232_000L // a day start, in seconds

    @Test
    fun ordinaryDay() {
        assertTrue(isDaytime(midnight + 12 * 3600, midnight + 6 * 3600, midnight + 18 * 3600))
        assertFalse(isDaytime(midnight + 2 * 3600, midnight + 6 * 3600, midnight + 18 * 3600))
    }

    @Test
    fun polarNightHasNoDay() {
        // Sunrise and sunset are both 00:00
        assertFalse(isDaytime(midnight, midnight, midnight))
        assertFalse(isDaytime(midnight + 12 * 3600, midnight, midnight))
    }

    @Test
    fun midnightSunIsAllDay() {
        assertTrue(isDaytime(midnight, midnight, midnight + 86_400))
        assertTrue(isDaytime(midnight + 23 * 3600, midnight, midnight + 86_400))
    }
}
