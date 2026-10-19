import { describe, expect, it } from 'vitest';
import { precipOutlook } from './precipOutlook';
import type { HourlyForecast } from './weatherData';
import { Precipitation } from './precipitation';

const H = 3_600_000;
function hours(wet: boolean[], code = 61): HourlyForecast[] {
	return wet.map((w, i) => ({
		time: i * H,
		weatherCode: w ? code : 1,
		precipProbability: w ? 80 : 10,
		precipitation: Precipitation.fromMm(w ? 1 : 0),
	}) as HourlyForecast);
}

describe('precipOutlook', () => {
	it('says when rain starts', () => {
		expect(precipOutlook(hours([false, false, true]))).toEqual({ kind: 'starts', snow: false, time: 2 * H });
	});

	it('says when the current rain stops', () => {
		expect(precipOutlook(hours([true, true, false]))).toEqual({ kind: 'stops', snow: false, time: 2 * H });
	});

	it('covers a dry or a wet day', () => {
		expect(precipOutlook(hours([false, false]))).toEqual({ kind: 'dry', snow: false });
		expect(precipOutlook(hours([true, true], 73))).toEqual({ kind: 'all_day', snow: true });
	});

	it('ignores a trace of rain the model thinks unlikely', () => {
		const trace = hours([false, false]).map((h) => ({ ...h, precipitation: Precipitation.fromMm(0.1), precipProbability: 2 }));
		expect(precipOutlook(trace)).toEqual({ kind: 'dry', snow: false });
	});

	it('is null without hourly data', () => {
		expect(precipOutlook([])).toBeNull();
	});
});
