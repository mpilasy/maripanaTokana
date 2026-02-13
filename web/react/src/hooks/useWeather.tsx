import { createContext, useContext, useState, useCallback, useEffect, useRef, type ReactNode } from 'react';
import type { WeatherData } from '$lib/domain/weatherData';
import { fetchWeather } from '$lib/api/openMeteo';
import { mapToWeatherData } from '$lib/api/openMeteoMapper';
import { getCachedLocation, cacheLocation, movedSignificantly, getPosition, reverseGeocode } from '$lib/stores/location';

export type WeatherState =
	| { kind: 'loading' }
	| { kind: 'success'; data: WeatherData }
	| { kind: 'error'; message: string };

interface WeatherContextType {
	weatherState: WeatherState;
	isRefreshing: boolean;
	doFetchWeather: () => void;
}

const WeatherContext = createContext<WeatherContextType>(null!);

const STALE_MS = 30 * 60 * 1000;

async function fetchAtLocation(lat: number, lon: number): Promise<WeatherData> {
	const [response, locationName] = await Promise.all([
		fetchWeather(lat, lon),
		reverseGeocode(lat, lon),
	]);
	return mapToWeatherData(response, locationName);
}

export function WeatherProvider({ children }: { children: ReactNode }) {
	const [state, setState] = useState<WeatherState>({ kind: 'loading' });
	const [isRefreshing, setIsRefreshing] = useState(false);
	const stateRef = useRef(state);
	stateRef.current = state;

	const doFetchWeather = useCallback(async () => {
		const current = stateRef.current;
		if (current.kind !== 'success') {
			setState({ kind: 'loading' });
		} else {
			setIsRefreshing(true);
		}

		try {
			const cached = getCachedLocation();
			let data: WeatherData | null = null;

			if (cached) {
				data = await fetchAtLocation(cached.lat, cached.lon);
				setState({ kind: 'success', data });
			}

			const fresh = await getPosition();
			cacheLocation(fresh.lat, fresh.lon);

			if (!cached || movedSignificantly(cached.lat, cached.lon, fresh.lat, fresh.lon)) {
				data = await fetchAtLocation(fresh.lat, fresh.lon);
				setState({ kind: 'success', data });
			}
		} catch (err) {
			const current = stateRef.current;
			if (current.kind !== 'success') {
				setState({
					kind: 'error',
					message: err instanceof GeolocationPositionError
						? 'error_get_location'
						: 'error_fetch_weather',
				});
			}
		} finally {
			setIsRefreshing(false);
		}
	}, []);

	const refreshIfStale = useCallback(() => {
		const current = stateRef.current;
		if (current.kind === 'success' && Date.now() - current.data.timestamp > STALE_MS) {
			doFetchWeather();
		}
	}, [doFetchWeather]);

	// Auto-refresh on visibility change
	useEffect(() => {
		const handler = () => {
			if (document.visibilityState === 'visible') {
				refreshIfStale();
			}
		};
		document.addEventListener('visibilitychange', handler);
		return () => document.removeEventListener('visibilitychange', handler);
	}, [refreshIfStale]);

	return (
		<WeatherContext.Provider value={{ weatherState: state, isRefreshing, doFetchWeather }}>
			{children}
		</WeatherContext.Provider>
	);
}

export function useWeather() {
	return useContext(WeatherContext);
}
