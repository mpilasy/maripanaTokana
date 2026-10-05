import { describe, expect, it } from 'vitest';
import { mapToWeatherData } from './openMeteoMapper';
import { computeActivityIndices } from '$lib/domain/activityIndices';

const response: any = {
	utc_offset_seconds: 0,
	current: {
		time: '2026-10-05T12:00', temperature_2m: 20, weather_code: 1,
		relative_humidity_2m: null, uv_index: null, cloud_cover: null,
		wind_direction_10m: null, visibility: null,
	},
	hourly: { time: [] },
	daily: { time: [], temperature_2m_min: [], temperature_2m_max: [], sunrise: [], sunset: [] },
};

describe('open-meteo mapper nulls', () => {
	it('maps missing secondary metrics to null and activity indices do not throw', () => {
		const d = mapToWeatherData(response, 'X');
		expect(d.humidity).toBeNull();
		expect(d.uvIndex).toBeNull();
		expect(d.cloudCover).toBeNull();
		expect(d.windDeg).toBeNull();
		expect(d.visibility).toBeNull();
		expect(() => computeActivityIndices(d)).not.toThrow();
		expect(computeActivityIndices(d).uvSafety).toBe('FAIR');
	});
});
