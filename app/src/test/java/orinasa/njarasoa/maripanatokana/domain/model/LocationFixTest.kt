// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

package orinasa.njarasoa.maripanatokana.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationFixTest {
    private val now = 1_800_000_000_000L
    private val minute = 60_000L

    @Test
    fun fixOfTenMinutesIsNotOld() = assertFalse(LocationFix(1.0, 2.0, now - 10 * minute).isOld(now))

    @Test
    fun fixOfThirtyMinutesIsNotOld() = assertFalse(LocationFix(1.0, 2.0, now - 30 * minute).isOld(now))

    @Test
    fun fixOfMoreThanThirtyMinutesIsOld() = assertTrue(LocationFix(1.0, 2.0, now - 31 * minute).isOld(now))

    @Test
    fun recentFallbackFixIsOld() = assertTrue(LocationFix(1.0, 2.0, now - minute, fallback = true).isOld(now))
}
