import { describe, expect, it } from 'vitest';
import { dailyAirQuality } from './dailyAirQuality';

const H = 3_600_000;
const hours = (from: number, count: number, us: (i: number) => number) =>
	Array.from({ length: count }, (_, i) => ({ time: (from + i) * H, usValue: us(i), europeanValue: 10 + i }));

describe('dailyAirQuality', () => {
	it("takes each day's highest value for both standards", () => {
		const result = dailyAirQuality([0, 24 * H], hours(0, 48, (i) => (i === 30 ? 90 : 40)), 'US');
		expect(result).toEqual([[40, 33], [90, 57]]);
	});

	it('puts the European value first for European locations', () => {
		expect(dailyAirQuality([0], hours(0, 24, () => 40), 'EUROPEAN')).toEqual([[33, 40]]);
	});

	it('is null for a day with under 6 hours of data', () => {
		expect(dailyAirQuality([0, 24 * H], hours(0, 28, () => 40), 'US')[1]).toBeNull();
	});
});
