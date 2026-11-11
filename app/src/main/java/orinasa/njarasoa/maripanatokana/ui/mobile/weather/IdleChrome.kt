// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

package orinasa.njarasoa.maripanatokana.ui.weather

// The state of the header and the footer of the main page. The web has the same helper with the same rules (idleChrome).

/** The time without a scroll after which the header folds and the footer goes out of view. */
const val CHROME_IDLE_MS = 3000L

/**
 * Pure state of the header and the footer: a scroll makes the header big and shows the footer at once;
 * [CHROME_IDLE_MS] without a scroll folds the header and hides the footer.
 * An open dialog stops the fold (the time starts again when it closes).
 * The accessibility lock (a screen reader is on) keeps the header big and the footer in view.
 */
data class IdleChrome(
    val headerBig: Boolean,
    val lastActiveMs: Long, // the last scroll, or when a dialog closed or the lock went off
    val inDialog: Boolean = false,
    val a11yLocked: Boolean = false,
) {
    val footerVisible: Boolean get() = headerBig

    fun scrollStarted(nowMs: Long) = copy(headerBig = true, lastActiveMs = nowMs)

    fun dialogOpen(open: Boolean, nowMs: Long) =
        if (open == inDialog) this else copy(inDialog = open, lastActiveMs = if (open) lastActiveMs else nowMs)

    fun a11yLock(on: Boolean, nowMs: Long) =
        if (on == a11yLocked) this else copy(a11yLocked = on, headerBig = headerBig || on, lastActiveMs = nowMs)

    /** The state at [nowMs]: folded when nothing stops the fold and the time is over. */
    fun tick(nowMs: Long) = if (msUntilFold(nowMs) == 0L) copy(headerBig = false) else this

    /** The time until the fold, or null when no fold will occur (already folded, dialog open or locked). */
    fun msUntilFold(nowMs: Long): Long? =
        if (!headerBig || inDialog || a11yLocked) null
        else (lastActiveMs + CHROME_IDLE_MS - nowMs).coerceAtLeast(0L)

    companion object {
        /** The screen opens big, with the footer in view. */
        fun start(nowMs: Long) = IdleChrome(headerBig = true, lastActiveMs = nowMs)
    }
}
