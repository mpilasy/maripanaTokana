// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

import { describe, expect, it } from 'vitest';
import { SUPPORTED_LOCALES } from '#lib/i18n/locales.js';
import { browserLocaleIndex } from './preferences.js';

const tag = (i: number) => SUPPORTED_LOCALES[i].tag;

// Same cases as DefaultLanguageTest.kt
describe('browserLocaleIndex', () => {
	it('takes the first browser language that the app has', () => {
		expect(tag(browserLocaleIndex(['fr-FR']))).toBe('fr');
		expect(tag(browserLocaleIndex(['mg', 'fr']))).toBe('mg');
		expect(tag(browserLocaleIndex(['de-DE', 'zh-CN']))).toBe('zh');
	});

	it('falls back to English', () => {
		expect(tag(browserLocaleIndex(['de', 'it']))).toBe('en');
		expect(tag(browserLocaleIndex([]))).toBe('en');
	});
});
