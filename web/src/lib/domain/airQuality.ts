export type AqiStandard = 'US' | 'EUROPEAN';
export type AqiTier = 'good' | 'moderate' | 'unhealthy' | 'very_unhealthy' | 'hazardous';

// Countries covered by Open-Meteo's CAMS-Europe air quality domain. Mirrors the country set
// used for MeteoAlarm coverage — see METEOALARM_COUNTRIES in $lib/api/alerts/meteoAlarm.ts.
const EUROPEAN_COUNTRY_CODES = new Set([
	'at', 'ba', 'be', 'bg', 'hr', 'cy', 'cz', 'dk', 'ee', 'fi', 'fr', 'de', 'gr', 'hu', 'ie', 'it',
	'lv', 'lt', 'lu', 'mt', 'md', 'me', 'nl', 'mk', 'no', 'pl', 'pt', 'ro', 'rs', 'sk', 'si',
	'es', 'se', 'ch', 'ua', 'gb',
]);

export class AirQualityIndex {
	private constructor(readonly usValue: number, readonly europeanValue: number, readonly primaryStandard: AqiStandard) {}

	get usTier(): AqiTier {
		if (this.usValue <= 50) return 'good';
		if (this.usValue <= 100) return 'moderate';
		if (this.usValue <= 200) return 'unhealthy';
		if (this.usValue <= 300) return 'very_unhealthy';
		return 'hazardous';
	}

	get europeanTier(): AqiTier {
		if (this.europeanValue < 20) return 'good';
		if (this.europeanValue < 40) return 'moderate';
		if (this.europeanValue < 60) return 'unhealthy';
		if (this.europeanValue < 100) return 'very_unhealthy';
		return 'hazardous';
	}

	/**
	 * Dual-unit display. Unlike temperature/pressure, primary/secondary here is NOT tied to the
	 * metric/imperial toggle — it's fixed by location (European countries show EU AQI as primary,
	 * everywhere else shows US AQI as primary) and doesn't flip when units are toggled.
	 */
	displayDual(): [string, string] {
		return this.primaryStandard === 'EUROPEAN'
			? [this.europeanValue.toString(), this.usValue.toString()]
			: [this.usValue.toString(), this.europeanValue.toString()];
	}

	/** Unit labels matching the order returned by displayDual(). */
	unitDual(): [string, string] {
		return this.primaryStandard === 'EUROPEAN' ? ['EU AQI', 'US AQI'] : ['US AQI', 'EU AQI'];
	}

	get primaryTier(): AqiTier {
		return this.primaryStandard === 'EUROPEAN' ? this.europeanTier : this.usTier;
	}

	static from(usAqi: number | null, europeanAqi: number | null, countryCode: string | null): AirQualityIndex | null {
		if (usAqi == null || europeanAqi == null) return null;
		const standard: AqiStandard = countryCode != null && EUROPEAN_COUNTRY_CODES.has(countryCode) ? 'EUROPEAN' : 'US';
		return new AirQualityIndex(usAqi, europeanAqi, standard);
	}
}
