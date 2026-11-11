// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

import { describe, expect, it } from 'vitest';
import { srgbToRgba } from './share.js';

describe('srgbToRgba', () => {
	it('turns a color(srgb …) value into rgba', () => {
		expect(srgbToRgba('color(srgb 0 0.894118 0 / 0.22)')).toBe('rgba(0, 228, 0, 0.22)');
		expect(srgbToRgba('color(srgb 1 0.5 0)')).toBe('rgba(255, 128, 0, 1)');
	});

	it('leaves other values unchanged', () => {
		expect(srgbToRgba('rgb(1, 2, 3)')).toBe('rgb(1, 2, 3)');
	});
});
