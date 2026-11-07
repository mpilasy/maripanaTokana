// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

import { describe, expect, it } from 'vitest';
import { showWindArrow, WindSpeed } from './windSpeed';

describe('showWindArrow', () => {
	it('shows the arrow from 5 km/h', () => {
		expect(showWindArrow(WindSpeed.fromMetersPerSecond(5 / 3.6))).toBe(true);
		expect(showWindArrow(WindSpeed.fromMetersPerSecond(4.9 / 3.6))).toBe(false);
		expect(showWindArrow(WindSpeed.fromMetersPerSecond(0))).toBe(false);
		expect(showWindArrow(WindSpeed.fromMetersPerSecond(NaN))).toBe(false);
	});
});
