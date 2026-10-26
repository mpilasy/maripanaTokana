// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later
import { afterEach, describe, expect, it, vi } from 'vitest';
import { isAndroid } from './installHint.js';

describe('isAndroid', () => {
	afterEach(() => vi.unstubAllGlobals());

	it('is true for an Android browser', () => {
		vi.stubGlobal('navigator', { userAgent: 'Mozilla/5.0 (Linux; Android 16; Pixel 10) AppleWebKit/537.36 Chrome/141.0 Mobile Safari/537.36' });
		expect(isAndroid()).toBe(true);
	});

	it('is false for iPhone and desktop browsers', () => {
		vi.stubGlobal('navigator', { userAgent: 'Mozilla/5.0 (iPhone; CPU iPhone OS 19_0 like Mac OS X) AppleWebKit/605.1.15 Mobile/15E148 Safari/604.1' });
		expect(isAndroid()).toBe(false);
		vi.stubGlobal('navigator', { userAgent: 'Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 Chrome/141.0 Safari/537.36' });
		expect(isAndroid()).toBe(false);
	});
});
