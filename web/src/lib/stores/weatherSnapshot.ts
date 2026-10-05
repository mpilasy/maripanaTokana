import type { WeatherData } from '$lib/domain/weatherData';
import { Temperature } from '$lib/domain/temperature';
import { WindSpeed } from '$lib/domain/windSpeed';
import { Pressure } from '$lib/domain/pressure';
import { Precipitation } from '$lib/domain/precipitation';
import { AirQualityIndex } from '$lib/domain/airQuality';

const STORAGE_KEY = 'weather_snapshot';

// Domain value objects are class instances with private constructors, so JSON drops their
// prototype. Tag them on write and restore the prototype on read.
const CLASSES: Record<string, { prototype: object }> = {
	Temperature, WindSpeed, Pressure, Precipitation, AirQualityIndex
};

function replacer(this: Record<string, unknown>, key: string, value: unknown) {
	const original = this[key];
	if (original && typeof original === 'object') {
		const name = (original as object).constructor?.name;
		if (name && CLASSES[name] && Object.getPrototypeOf(original) === CLASSES[name].prototype) {
			return { __class: name, ...(original as object) };
		}
	}
	return value;
}

function reviver(_key: string, value: unknown) {
	if (value && typeof value === 'object' && typeof (value as { __class?: unknown }).__class === 'string') {
		const { __class, ...props } = value as { __class: string } & Record<string, unknown>;
		const cls = CLASSES[__class];
		if (cls) return Object.assign(Object.create(cls.prototype), props);
	}
	return value;
}

export function saveSnapshot(key: string, data: WeatherData): void {
	try {
		localStorage.setItem(STORAGE_KEY, JSON.stringify({ key, data }, replacer));
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
		return parsed.data as WeatherData;
	} catch {
		return null;
	}
}
