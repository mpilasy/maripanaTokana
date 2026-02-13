import { Injectable, signal } from '@angular/core';
import type { WeatherData } from '$lib/domain/weatherData';
import { fetchWeather } from '$lib/api/openMeteo';
import { mapToWeatherData } from '$lib/api/openMeteoMapper';
import { getCachedLocation, cacheLocation, movedSignificantly, getPosition, reverseGeocode } from '$lib/stores/location';

export type WeatherState =
	| { kind: 'loading' }
	| { kind: 'success'; data: WeatherData }
	| { kind: 'error'; message: string };

const STALE_MS = 30 * 60 * 1000;

async function fetchAtLocation(lat: number, lon: number): Promise<WeatherData> {
	const [response, locationName] = await Promise.all([
		fetchWeather(lat, lon),
		reverseGeocode(lat, lon),
	]);
	return mapToWeatherData(response, locationName);
}

@Injectable({ providedIn: 'root' })
export class WeatherService {
	readonly weatherState = signal<WeatherState>({ kind: 'loading' });
	readonly isRefreshing = signal(false);

	async doFetchWeather() {
		const current = this.weatherState();
		if (current.kind !== 'success') {
			this.weatherState.set({ kind: 'loading' });
		} else {
			this.isRefreshing.set(true);
		}

		try {
			const cached = getCachedLocation();
			if (cached) {
				const data = await fetchAtLocation(cached.lat, cached.lon);
				this.weatherState.set({ kind: 'success', data });
			}

			const fresh = await getPosition();
			cacheLocation(fresh.lat, fresh.lon);

			if (!cached || movedSignificantly(cached.lat, cached.lon, fresh.lat, fresh.lon)) {
				const data = await fetchAtLocation(fresh.lat, fresh.lon);
				this.weatherState.set({ kind: 'success', data });
			}
		} catch (err) {
			if (this.weatherState().kind !== 'success') {
				this.weatherState.set({
					kind: 'error',
					message: err instanceof GeolocationPositionError
						? 'error_get_location'
						: 'error_fetch_weather',
				});
			}
		} finally {
			this.isRefreshing.set(false);
		}
	}

	refreshIfStale() {
		const current = this.weatherState();
		if (current.kind === 'success' && Date.now() - current.data.timestamp > STALE_MS) {
			this.doFetchWeather();
		}
	}
}
