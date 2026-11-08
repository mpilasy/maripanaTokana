// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

// The state of the header and the footer of the main page.
// Twin: IdleChrome in app/src/main/java/orinasa/njarasoa/maripanatokana/ui/weather/IdleChrome.kt

/** The time without a scroll after which the header folds and the footer goes out of view. */
export const CHROME_IDLE_MS = 3000;

/**
 * Pure state of the header and the footer: a scroll makes the header big and shows the footer at once;
 * {@link CHROME_IDLE_MS} without a scroll folds the header and hides the footer.
 * An open dialog stops the fold (the time starts again when it closes).
 * The accessibility lock (a screen reader is on) keeps the header big and the footer in view.
 * Web only: the footer hold (the mouse near the bottom edge, or the keyboard focus in the footer) shows the
 * footer; it goes out of view {@link CHROME_IDLE_MS} after the hold ends.
 */
export class IdleChrome {
	private constructor(
		readonly headerBig: boolean,
		/** The last scroll, or when a dialog closed or the lock went off */
		readonly lastActiveMs: number,
		readonly inDialog = false,
		readonly a11yLocked = false,
		/** Web only: the footer shows because of the footer hold */
		readonly footerShown = false,
		/** Web only: the footer hold is on now */
		readonly footerHeld = false,
		/** Web only: when the footer hold went on or off */
		readonly footerActiveMs = lastActiveMs,
	) {}

	/** The screen opens big, with the footer in view. */
	static start(nowMs: number): IdleChrome {
		return new IdleChrome(true, nowMs);
	}

	get footerVisible(): boolean {
		return this.headerBig || this.footerShown;
	}

	scrollStarted(nowMs: number): IdleChrome {
		return this.copy({ headerBig: true, lastActiveMs: nowMs });
	}

	dialogOpen(open: boolean, nowMs: number): IdleChrome {
		return open === this.inDialog ? this : this.copy({ inDialog: open, lastActiveMs: open ? this.lastActiveMs : nowMs });
	}

	a11yLock(on: boolean, nowMs: number): IdleChrome {
		return on === this.a11yLocked ? this : this.copy({ a11yLocked: on, headerBig: this.headerBig || on, lastActiveMs: nowMs });
	}

	/** Web only: the footer hold went on or off. */
	footerHold(held: boolean, nowMs: number): IdleChrome {
		return held === this.footerHeld
			? this
			: this.copy({ footerHeld: held, footerShown: this.footerShown || held, footerActiveMs: nowMs });
	}

	/** The state at `nowMs`: folded and hidden when nothing stops it and the time is over. */
	tick(nowMs: number): IdleChrome {
		const fold = this.msUntilHeaderFold(nowMs) === 0;
		const hide = this.msUntilFooterHide(nowMs) === 0;
		return fold || hide
			? this.copy({ headerBig: fold ? false : this.headerBig, footerShown: hide ? false : this.footerShown })
			: this;
	}

	/** The time until the next fold or hide, or null when none will occur (already folded, dialog open or locked). */
	msUntilFold(nowMs: number): number | null {
		const times = [this.msUntilHeaderFold(nowMs), this.msUntilFooterHide(nowMs)].filter((t) => t != null);
		return times.length ? Math.min(...times) : null;
	}

	private msUntilHeaderFold(nowMs: number): number | null {
		if (!this.headerBig || this.inDialog || this.a11yLocked) return null;
		return Math.max(0, this.lastActiveMs + CHROME_IDLE_MS - nowMs);
	}

	private msUntilFooterHide(nowMs: number): number | null {
		if (!this.footerShown || this.footerHeld || this.inDialog || this.a11yLocked) return null;
		return Math.max(0, Math.max(this.lastActiveMs, this.footerActiveMs) + CHROME_IDLE_MS - nowMs);
	}

	private copy(c: Partial<Pick<IdleChrome, 'headerBig' | 'lastActiveMs' | 'inDialog' | 'a11yLocked' | 'footerShown' | 'footerHeld' | 'footerActiveMs'>>): IdleChrome {
		return new IdleChrome(
			c.headerBig ?? this.headerBig,
			c.lastActiveMs ?? this.lastActiveMs,
			c.inDialog ?? this.inDialog,
			c.a11yLocked ?? this.a11yLocked,
			c.footerShown ?? this.footerShown,
			c.footerHeld ?? this.footerHeld,
			c.footerActiveMs ?? this.footerActiveMs,
		);
	}
}
