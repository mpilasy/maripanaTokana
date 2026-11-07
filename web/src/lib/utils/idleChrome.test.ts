// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

import { describe, expect, it } from 'vitest';
import { CHROME_IDLE_MS, IdleChrome } from './idleChrome';

const T0 = 1_000_000;

describe('IdleChrome', () => {
	it('starts big with the footer in view', () => {
		const s = IdleChrome.start(T0);
		expect(s.headerBig).toBe(true);
		expect(s.footerVisible).toBe(true);
	});

	it('folds after 5 s without a scroll', () => {
		const s = IdleChrome.start(T0);
		expect(s.tick(T0 + CHROME_IDLE_MS - 1).headerBig).toBe(true);
		const folded = s.tick(T0 + CHROME_IDLE_MS);
		expect(folded.headerBig).toBe(false);
		expect(folded.footerVisible).toBe(false);
		expect(s.msUntilFold(T0)).toBe(CHROME_IDLE_MS);
		expect(folded.msUntilFold(T0 + CHROME_IDLE_MS)).toBeNull();
	});

	it('is big at once on a scroll, and the time starts again', () => {
		const folded = IdleChrome.start(T0).tick(T0 + CHROME_IDLE_MS);
		const scrolled = folded.scrollStarted(T0 + 7000);
		expect(scrolled.headerBig).toBe(true);
		expect(scrolled.footerVisible).toBe(true);
		expect(scrolled.tick(T0 + 7000 + CHROME_IDLE_MS - 1).headerBig).toBe(true);
		expect(scrolled.tick(T0 + 7000 + CHROME_IDLE_MS).headerBig).toBe(false);
	});

	it('does not fold while a dialog is open, and the time starts again when it closes', () => {
		const open = IdleChrome.start(T0).dialogOpen(true, T0 + 1000);
		expect(open.tick(T0 + 10_000).headerBig).toBe(true);
		expect(open.msUntilFold(T0 + 10_000)).toBeNull();
		const closed = open.dialogOpen(false, T0 + 10_000);
		expect(closed.tick(T0 + 10_000 + CHROME_IDLE_MS - 1).headerBig).toBe(true);
		expect(closed.tick(T0 + 10_000 + CHROME_IDLE_MS).headerBig).toBe(false);
	});

	it('keeps the header big and the footer in view with the accessibility lock', () => {
		const folded = IdleChrome.start(T0).tick(T0 + CHROME_IDLE_MS);
		const locked = folded.a11yLock(true, T0 + 6000);
		expect(locked.headerBig).toBe(true);
		expect(locked.footerVisible).toBe(true);
		expect(locked.tick(T0 + 60_000).headerBig).toBe(true);
		expect(IdleChrome.start(T0).a11yLock(true, T0).tick(T0 + 60_000).footerVisible).toBe(true);
		// Off again: the usual rule, from that time
		const unlocked = locked.a11yLock(false, T0 + 60_000);
		expect(unlocked.tick(T0 + 60_000 + CHROME_IDLE_MS - 1).headerBig).toBe(true);
		expect(unlocked.tick(T0 + 60_000 + CHROME_IDLE_MS).headerBig).toBe(false);
	});

	it('shows only the footer while the footer hold is on, and hides it 5 s after the hold ends', () => {
		const folded = IdleChrome.start(T0).tick(T0 + CHROME_IDLE_MS);
		const held = folded.footerHold(true, T0 + 6000);
		expect(held.footerVisible).toBe(true);
		expect(held.headerBig).toBe(false);
		expect(held.tick(T0 + 60_000).footerVisible).toBe(true);
		const released = held.footerHold(false, T0 + 60_000);
		expect(released.msUntilFold(T0 + 60_000)).toBe(CHROME_IDLE_MS);
		expect(released.tick(T0 + 60_000 + CHROME_IDLE_MS - 1).footerVisible).toBe(true);
		expect(released.tick(T0 + 60_000 + CHROME_IDLE_MS).footerVisible).toBe(false);
	});

	it('folds the header at its time while the footer hold keeps the footer', () => {
		const held = IdleChrome.start(T0).footerHold(true, T0 + 1000);
		const s = held.tick(T0 + CHROME_IDLE_MS);
		expect(s.headerBig).toBe(false);
		expect(s.footerVisible).toBe(true);
	});
});
