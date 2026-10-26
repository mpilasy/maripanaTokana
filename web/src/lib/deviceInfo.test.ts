// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later
import { describe, expect, it } from 'vitest';
import { browserName, formatDensity } from './deviceInfo.js';

describe('browserName', () => {
	it('names the common browsers with their major version', () => {
		expect(browserName('Mozilla/5.0 (Android 16; Mobile; rv:158.0) Gecko/158.0 Firefox/158.0')).toBe('Firefox 158');
		expect(browserName('Mozilla/5.0 (iPhone; CPU iPhone OS 19_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/19.0 Mobile/15E148 Safari/604.1')).toBe('Safari 19');
		expect(browserName('Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/19.1 Safari/605.1.15')).toBe('Safari 19');
		expect(browserName('Mozilla/5.0 (iPhone; CPU iPhone OS 19_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) CriOS/141.0.0.0 Mobile/15E148 Safari/604.1')).toBe('Chrome 141');
		expect(browserName('Mozilla/5.0 (iPhone; CPU iPhone OS 19_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) FxiOS/158.0 Mobile/15E148 Safari/605.1.15')).toBe('Firefox 158');
		expect(browserName('Mozilla/5.0 (Linux; Android 16; SM-S931B) AppleWebKit/537.36 (KHTML, like Gecko) SamsungBrowser/29.0 Chrome/141.0.0.0 Mobile Safari/537.36')).toBe('Samsung Internet 29');
		expect(browserName('Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36 Edg/141.0.0.0')).toBe('Edge 141');
	});

	it('keeps an unknown user agent as it is', () => {
		expect(browserName('SomeBot/1.0')).toBe('SomeBot/1.0');
	});
});

describe('formatDensity', () => {
	it('rounds to at most 2 decimals', () => {
		expect(formatDensity(3.5294117647058822)).toBe('× 3.53');
		expect(formatDensity(2.625)).toBe('× 2.63');
		expect(formatDensity(2)).toBe('× 2');
	});
});
