// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

import { describe, expect, it } from 'vitest';
import { runsOf, splitDual } from './dualLabel.js';

describe('splitDual', () => {
	it('splits a label with both units', () => {
		expect(splitDual('27°C · 80°F')).toEqual(['27°C', '80°F']);
		expect(splitDual('UV 8.4')).toEqual(['UV 8.4', null]);
	});
});

describe('runsOf', () => {
	it('breaks the line where a value is missing', () => {
		const pts = [0, 1, 2, 5, 6, 9].map((i) => ({ i }));
		expect(runsOf(pts).map((r) => r.map((p) => p.i))).toEqual([[0, 1, 2], [5, 6], [9]]);
	});
});
