// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

import { describe, expect, it } from 'vitest';
import { shareItemsPerLine } from './shareLayout.js';

describe('shareItemsPerLine', () => {
	it('fills lines of at most max items, as even as possible', () => {
		expect(shareItemsPerLine(24, 6)).toBe(6);
		expect(shareItemsPerLine(10, 5)).toBe(5);
		expect(shareItemsPerLine(7, 5)).toBe(4);
		expect(shareItemsPerLine(16, 5)).toBe(4);
	});

	it('keeps a short list on one line', () => {
		expect(shareItemsPerLine(3, 6)).toBe(3);
		expect(shareItemsPerLine(0, 6)).toBe(1);
	});
});
