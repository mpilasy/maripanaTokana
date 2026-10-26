// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later
import { afterEach, describe, expect, it, vi } from 'vitest';
import { instructionsLocale, isAndroid } from './installHint.js';

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

describe('instructionsLocale', () => {
	const supported = ['mg', 'ar', 'en', 'es', 'fr', 'hi', 'ne', 'zh'];

	it('keeps the app language when it is not Malagasy', () => {
		expect(instructionsLocale('fr', ['en-US'], supported)).toBe('fr');
		expect(instructionsLocale('en', ['mg-MG'], supported)).toBe('en');
	});

	it('uses the first supported phone language when the app is in Malagasy', () => {
		expect(instructionsLocale('mg', ['fr-FR', 'en-US'], supported)).toBe('fr');
		expect(instructionsLocale('mg', ['de-DE', 'zh-CN'], supported)).toBe('zh');
	});

	it('uses English when the phone is in Malagasy or in a language the app does not have', () => {
		expect(instructionsLocale('mg', ['mg-MG'], supported)).toBe('en');
		expect(instructionsLocale('mg', ['de-DE'], supported)).toBe('en');
		expect(instructionsLocale('mg', [], supported)).toBe('en');
	});
});
