// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

package orinasa.njarasoa.maripanatokana

import orinasa.njarasoa.maripanatokana.ui.common.supportedLocales
import org.junit.Assert.assertEquals
import org.junit.Test

class DefaultLanguageTest {
    private fun tag(index: Int) = supportedLocales[index].tag

    @Test
    fun firstSupportedDeviceLanguage() {
        assertEquals("fr", tag(DefaultLanguage.indexFor(listOf("fr"))))
        assertEquals("mg", tag(DefaultLanguage.indexFor(listOf("mg", "fr"))))
        // German is not in the app: the next device language
        assertEquals("zh", tag(DefaultLanguage.indexFor(listOf("de", "zh"))))
    }

    @Test
    fun englishWhenNoDeviceLanguageIsSupported() {
        assertEquals("en", tag(DefaultLanguage.indexFor(listOf("de", "it"))))
        assertEquals("en", tag(DefaultLanguage.indexFor(emptyList())))
    }
}
