// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

package orinasa.njarasoa.maripanatokana.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class UvDialTest {
    @Test
    fun `places a reading inside its level segment`() {
        assertEquals(0.1f, uvDialPosition(1.5), 0.001f)
        assertEquals(0.5f, uvDialPosition(7.0), 0.001f)
        assertEquals(0.7f, uvDialPosition(9.5), 0.001f)
    }

    @Test
    fun `agrees with the level the reading is given`() {
        listOf(0.0, 2.9, 3.0, 5.9, 6.0, 7.9, 8.0, 10.9, 11.0, 20.0).forEach { uv ->
            assertEquals("$uv", uvLevel(uv), (uvDialPosition(uv) * 5).toInt())
        }
    }
}
