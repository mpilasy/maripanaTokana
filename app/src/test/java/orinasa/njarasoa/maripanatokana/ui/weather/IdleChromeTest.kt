// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

package orinasa.njarasoa.maripanatokana.ui.weather

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IdleChromeTest {
    private val t0 = 1_000_000L

    @Test
    fun startIsBigWithTheFooterInView() {
        val s = IdleChrome.start(t0)
        assertTrue(s.headerBig)
        assertTrue(s.footerVisible)
    }

    @Test
    fun fiveSecondsStillFolds() {
        val s = IdleChrome.start(t0)
        assertTrue(s.tick(t0 + CHROME_IDLE_MS - 1).headerBig)
        val folded = s.tick(t0 + CHROME_IDLE_MS)
        assertFalse(folded.headerBig)
        assertFalse(folded.footerVisible)
        assertEquals(CHROME_IDLE_MS, s.msUntilFold(t0))
        assertNull(folded.msUntilFold(t0 + CHROME_IDLE_MS))
    }

    @Test
    fun scrollIsBigAtOnceAndStartsTheTimeAgain() {
        val folded = IdleChrome.start(t0).tick(t0 + CHROME_IDLE_MS)
        val scrolled = folded.scrollStarted(t0 + 7000)
        assertTrue(scrolled.headerBig)
        assertTrue(scrolled.footerVisible)
        assertTrue(scrolled.tick(t0 + 7000 + CHROME_IDLE_MS - 1).headerBig)
        assertFalse(scrolled.tick(t0 + 7000 + CHROME_IDLE_MS).headerBig)
    }

    @Test
    fun openDialogStopsTheFoldAndClosingStartsTheTimeAgain() {
        val open = IdleChrome.start(t0).dialogOpen(true, t0 + 1000)
        assertTrue(open.tick(t0 + 10_000).headerBig)
        assertNull(open.msUntilFold(t0 + 10_000))
        val closed = open.dialogOpen(false, t0 + 10_000)
        assertTrue(closed.tick(t0 + 10_000 + CHROME_IDLE_MS - 1).headerBig)
        assertFalse(closed.tick(t0 + 10_000 + CHROME_IDLE_MS).headerBig)
    }

    @Test
    fun a11yLockKeepsBigAndInView() {
        val folded = IdleChrome.start(t0).tick(t0 + CHROME_IDLE_MS)
        val locked = folded.a11yLock(true, t0 + 6000)
        assertTrue(locked.headerBig)
        assertTrue(locked.footerVisible)
        assertTrue(locked.tick(t0 + 60_000).headerBig)
        assertTrue(IdleChrome.start(t0).a11yLock(true, t0).tick(t0 + 60_000).footerVisible)
        // Off again: the usual rule, from that time
        val unlocked = locked.a11yLock(false, t0 + 60_000)
        assertTrue(unlocked.tick(t0 + 60_000 + CHROME_IDLE_MS - 1).headerBig)
        assertFalse(unlocked.tick(t0 + 60_000 + CHROME_IDLE_MS).headerBig)
    }
}
