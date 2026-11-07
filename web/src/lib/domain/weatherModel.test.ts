// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

import { describe, expect, it } from 'vitest';
import { weatherModelFor } from './weatherModel';

describe('weatherModelFor', () => {
	it('finds a known model', () => {
		expect(weatherModelFor('icon_seamless')?.name).toBe('ICON');
	});
	it('gives Automatic (null) for an empty or unknown key', () => {
		expect(weatherModelFor('')).toBeNull();
		expect(weatherModelFor(null)).toBeNull();
		expect(weatherModelFor('removed_model')).toBeNull();
	});
});
