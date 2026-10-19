import { describe, expect, it } from 'vitest';
import { nightWeatherCodes } from './nightWeather';

const H = 3_600_000;
// Hourly data for 48 hours from t=0; sunset at 18h, sunrise at 6h
const times = Array.from({ length: 48 }, (_, i) => i * H);

describe('nightWeatherCodes', () => {
	it('takes the most severe code between sunset and the next sunrise', () => {
		const codes = times.map((t) => (t === 22 * H ? 61 : 2));
		expect(nightWeatherCodes(times, codes, [18 * H, 42 * H], [6 * H, 30 * H])[0]).toBe(61);
	});

	it('ignores the daytime hours', () => {
		const codes = times.map((t) => (t === 12 * H ? 95 : 1));
		expect(nightWeatherCodes(times, codes, [18 * H], [6 * H, 30 * H])).toEqual([1]);
	});

	it('is null for a night the hourly data does not cover', () => {
		const codes = times.map(() => 3);
		// The second night runs 42h-54h but the data stops at 47h
		expect(nightWeatherCodes(times, codes, [18 * H, 42 * H], [6 * H, 30 * H])).toEqual([3, null]);
	});
});
