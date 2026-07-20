export type AqiStandard = 'US' | 'EUROPEAN';
export type AqiTier = 'good' | 'moderate' | 'unhealthy' | 'very_unhealthy' | 'hazardous';

// AirNow AQI category colors (airnow.gov/aqi/aqi-basics) — matches AqiTierBadge.svelte's CSS
// classes, as actual color values for contexts (e.g. canvas/SVG fills) that need a value rather
// than a class name.
export const AQI_TIER_COLORS: Record<AqiTier, string> = {
	good: '#00E400',
	moderate: '#FFFF00',
	unhealthy: '#FF0000',
	very_unhealthy: '#8F3F97',
	hazardous: '#7E0023',
};

// Countries covered by Open-Meteo's CAMS-Europe air quality domain. Mirrors the country set
// used for MeteoAlarm coverage — see METEOALARM_COUNTRIES in $lib/api/alerts/meteoAlarm.ts.
const EUROPEAN_COUNTRY_CODES = new Set([
	'at', 'ba', 'be', 'bg', 'hr', 'cy', 'cz', 'dk', 'ee', 'fi', 'fr', 'de', 'gr', 'hu', 'ie', 'it',
	'lv', 'lt', 'lu', 'mt', 'md', 'me', 'nl', 'mk', 'no', 'pl', 'pt', 'ro', 'rs', 'sk', 'si',
	'es', 'se', 'ch', 'ua', 'gb',
]);

export interface AirQualityConcentrations {
	pm25: number | null;
	pm10: number | null;
	carbonMonoxide: number | null;
	nitrogenDioxide: number | null;
	sulphurDioxide: number | null;
	ozone: number | null;
	ammonia: number | null;
	dust: number | null;
}

// Per-pollutant AQI tier, resolved from Open-Meteo's precomputed sub-indices for whichever
// standard is primary at this location. Null when Open-Meteo doesn't publish a sub-index for
// this pollutant+standard pair (the EU index has no CO sub-index) or ammonia/dust, which have
// no official AQI breakpoints at all.
export interface AirQualityPollutants extends AirQualityConcentrations {
	pm25Tier: AqiTier | null;
	pm10Tier: AqiTier | null;
	carbonMonoxideTier: AqiTier | null;
	nitrogenDioxideTier: AqiTier | null;
	sulphurDioxideTier: AqiTier | null;
	ozoneTier: AqiTier | null;
}

export interface AirQualitySubIndices {
	usAqiPm25: number | null;
	usAqiPm10: number | null;
	usAqiCarbonMonoxide: number | null;
	usAqiNitrogenDioxide: number | null;
	usAqiSulphurDioxide: number | null;
	usAqiOzone: number | null;
	europeanAqiPm25: number | null;
	europeanAqiPm10: number | null;
	europeanAqiNitrogenDioxide: number | null;
	europeanAqiSulphurDioxide: number | null;
	europeanAqiOzone: number | null;
}

const EMPTY_CONCENTRATIONS: AirQualityConcentrations = {
	pm25: null, pm10: null, carbonMonoxide: null, nitrogenDioxide: null,
	sulphurDioxide: null, ozone: null, ammonia: null, dust: null,
};

const EMPTY_SUB_INDICES: AirQualitySubIndices = {
	usAqiPm25: null, usAqiPm10: null, usAqiCarbonMonoxide: null, usAqiNitrogenDioxide: null,
	usAqiSulphurDioxide: null, usAqiOzone: null, europeanAqiPm25: null, europeanAqiPm10: null,
	europeanAqiNitrogenDioxide: null, europeanAqiSulphurDioxide: null, europeanAqiOzone: null,
};

export class AirQualityIndex {
	// Pollutant concentrations in µg/m³ plus per-pollutant tier, straight from the Open-Meteo
	// air-quality "current" call. Null when a given pollutant isn't covered by the domain for
	// this location (e.g. ammonia is CAMS-Europe only) or the API omitted it.
	readonly pollutants: AirQualityPollutants;

	private constructor(
		readonly usValue: number,
		readonly europeanValue: number,
		readonly primaryStandard: AqiStandard,
		pollutants: AirQualityPollutants,
	) {
		this.pollutants = pollutants;
	}

	static tierFor(value: number, standard: AqiStandard): AqiTier {
		if (standard === 'US') {
			if (value <= 50) return 'good';
			if (value <= 100) return 'moderate';
			if (value <= 200) return 'unhealthy';
			if (value <= 300) return 'very_unhealthy';
			return 'hazardous';
		}
		if (value < 20) return 'good';
		if (value < 40) return 'moderate';
		if (value < 60) return 'unhealthy';
		if (value < 100) return 'very_unhealthy';
		return 'hazardous';
	}

	get usTier(): AqiTier {
		return AirQualityIndex.tierFor(this.usValue, 'US');
	}

	get europeanTier(): AqiTier {
		return AirQualityIndex.tierFor(this.europeanValue, 'EUROPEAN');
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

	get primaryTier(): AqiTier {
		return this.primaryStandard === 'EUROPEAN' ? this.europeanTier : this.usTier;
	}

	static from(
		usAqi: number | null,
		europeanAqi: number | null,
		countryCode: string | null,
		concentrations: AirQualityConcentrations = EMPTY_CONCENTRATIONS,
		subIndices: AirQualitySubIndices = EMPTY_SUB_INDICES,
	): AirQualityIndex | null {
		if (usAqi == null || europeanAqi == null) return null;
		const standard: AqiStandard = countryCode != null && EUROPEAN_COUNTRY_CODES.has(countryCode) ? 'EUROPEAN' : 'US';
		const tier = (us: number | null, eu: number | null): AqiTier | null => {
			const v = standard === 'EUROPEAN' ? eu : us;
			return v == null ? null : AirQualityIndex.tierFor(v, standard);
		};
		const pollutants: AirQualityPollutants = {
			...concentrations,
			pm25Tier: tier(subIndices.usAqiPm25, subIndices.europeanAqiPm25),
			pm10Tier: tier(subIndices.usAqiPm10, subIndices.europeanAqiPm10),
			carbonMonoxideTier: tier(subIndices.usAqiCarbonMonoxide, null),
			nitrogenDioxideTier: tier(subIndices.usAqiNitrogenDioxide, subIndices.europeanAqiNitrogenDioxide),
			sulphurDioxideTier: tier(subIndices.usAqiSulphurDioxide, subIndices.europeanAqiSulphurDioxide),
			ozoneTier: tier(subIndices.usAqiOzone, subIndices.europeanAqiOzone),
		};
		return new AirQualityIndex(usAqi, europeanAqi, standard, pollutants);
	}
}
