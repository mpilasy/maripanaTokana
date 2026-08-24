import type { WeatherData } from './weatherData';

export type ActivityTier = 'EXCELLENT' | 'GOOD' | 'FAIR' | 'POOR';

export interface ActivityIndices {
	running: ActivityTier;
	laundry: ActivityTier;
	uvSafety: ActivityTier;
}

export function computeActivityIndices(data: WeatherData): ActivityIndices {
	const tempC = data.temperature.celsius;
	const humidity = data.humidity;
	const windMs = data.windSpeed.metersPerSecond;
	const precipMm = (data.rain?.mm ?? 0) + (data.snow?.mm ?? 0);
	const uv = data.uvIndex;

	let runningTier: ActivityTier = 'FAIR';
	if (precipMm > 0 || tempC < 0 || tempC > 32 || windMs > 12) {
		runningTier = 'POOR';
	} else if (tempC >= 8 && tempC <= 22 && humidity < 70 && windMs < 7) {
		runningTier = 'EXCELLENT';
	} else if (tempC >= 2 && tempC <= 28 && humidity < 85) {
		runningTier = 'GOOD';
	}

	let laundryTier: ActivityTier = 'FAIR';
	if (precipMm > 0 || humidity > 80) {
		laundryTier = 'POOR';
	} else if (tempC > 18 && humidity < 50 && windMs >= 2) {
		laundryTier = 'EXCELLENT';
	} else if (tempC > 12 && humidity < 65) {
		laundryTier = 'GOOD';
	}

	let uvTier: ActivityTier = 'POOR';
	if (uv < 3) {
		uvTier = 'EXCELLENT';
	} else if (uv < 6) {
		uvTier = 'GOOD';
	} else if (uv < 8) {
		uvTier = 'FAIR';
	}

	const result: ActivityIndices = {
		running: runningTier,
		laundry: laundryTier,
		uvSafety: uvTier
	};
	return result;
}
