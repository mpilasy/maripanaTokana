import type { WeatherData } from '$lib/domain/weatherData';
import { Temperature } from '$lib/domain/temperature';
import { WindSpeed } from '$lib/domain/windSpeed';
import { Pressure } from '$lib/domain/pressure';
import { Precipitation } from '$lib/domain/precipitation';
import { AirQualityIndex } from '$lib/domain/airQuality';

const STORAGE_KEY = 'weather_snapshot_v2';
// v1 tagged instances by constructor.name, which is minified in production builds.
const OLD_STORAGE_KEY = 'weather_snapshot';

// Domain value objects are class instances with private constructors, so JSON drops their
// prototype. Tag them on write and restore the prototype on read. Tags are string literals
// matched with instanceof, so they survive minification (unlike constructor.name).
const CLASSES: [string, { prototype: object }][] = [
	['Temperature', Temperature],
	['WindSpeed', WindSpeed],
	['Pressure', Pressure],
	['Precipitation', Precipitation],
	['AirQualityIndex', AirQualityIndex],
];

function replacer(this: Record<string, unknown>, key: string, value: unknown) {
	const original = this[key];
	if (original && typeof original === 'object') {
		for (const [tag, cls] of CLASSES) {
			if (original instanceof (cls as unknown as new (...a: never[]) => object)) {
				return { __class: tag, ...(original as object) };
			}
		}
	}
	return value;
}

function reviver(_key: string, value: unknown) {
	if (value && typeof value === 'object' && typeof (value as { __class?: unknown }).__class === 'string') {
		const { __class, ...props } = value as { __class: string } & Record<string, unknown>;
		const entry = CLASSES.find(([tag]) => tag === __class);
		if (entry) return Object.assign(Object.create(entry[1].prototype), props);
	}
	return value;
}

export function saveSnapshot(key: string, data: WeatherData): void {
	try {
		localStorage.setItem(STORAGE_KEY, JSON.stringify({ key, data }, replacer));
		localStorage.removeItem(OLD_STORAGE_KEY);
	} catch {
		// storage unavailable or full
	}
}

export function loadSnapshot(key: string): WeatherData | null {
	try {
		const raw = localStorage.getItem(STORAGE_KEY);
		if (!raw) return null;
		const parsed = JSON.parse(raw, reviver);
		if (parsed?.key !== key || typeof parsed.data?.timestamp !== 'number') return null;
		if (typeof parsed.data.temperature?.displayDual !== 'function') return null;
		return parsed.data as WeatherData;
	} catch {
		return null;
	}
}
