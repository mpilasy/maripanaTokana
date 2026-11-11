// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

export type AqiStandard = 'US' | 'EUROPEAN';
export type AqiTier = 'good' | 'moderate' | 'unhealthy' | 'very_unhealthy' | 'hazardous';

// The order matches uv_labels[0..3] ("Low"/"Moderate"/"High"/"Very High"): pollen uses those labels, which have the same words.
export type PollenTier = 'low' | 'moderate' | 'high' | 'very_high';
export const POLLEN_TIERS: PollenTier[] = ['low', 'moderate', 'high', 'very_high'];

// Pollen has no official breakpoints (unlike AQI): these are common grains/m³ bands for all species, an approximation.
export function pollenTierFor(value: number): PollenTier {
	if (value < 10) return 'low';
	if (value < 50) return 'moderate';
	if (value < 200) return 'high';
	return 'very_high';
}

// Pollen in grains/m³ from Open-Meteo's CAMS-Europe model; all null outside CAMS-Europe.
export interface PollenReadings {
	alder: number | null;
	birch: number | null;
	grass: number | null;
	mugwort: number | null;
	olive: number | null;
	ragweed: number | null;
}

export const EMPTY_POLLEN: PollenReadings = {
	alder: null, birch: null, grass: null, mugwort: null, olive: null, ragweed: null,
};

export function hasPollenData(pollen: PollenReadings): boolean {
	return pollen.alder != null || pollen.birch != null || pollen.grass != null
		|| pollen.mugwort != null || pollen.olive != null || pollen.ragweed != null;
}

// AirNow AQI category colors (airnow.gov/aqi/aqi-basics), the same as in AqiTierBadge.svelte, for SVG fills.
export const AQI_TIER_COLORS: Record<AqiTier, string> = {
	good: '#00E400',
	moderate: '#FFFF00',
	unhealthy: '#FF0000',
	very_unhealthy: '#8F3F97',
	hazardous: '#7E0023',
};

// Countries in Open-Meteo's CAMS-Europe air quality domain; the same set as METEOALARM_COUNTRIES in
// $lib/api/alerts/meteoAlarm.ts.
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

// The tier of each pollutant from Open-Meteo's sub-indices of the primary standard. Null when there is no
// sub-index (the EU index has none for CO), and for ammonia and dust, which have no AQI breakpoints.
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

const AQI_DIAL_TIERS: AqiTier[] = ['good', 'moderate', 'unhealthy', 'very_unhealthy', 'hazardous'];

// Each level's range, in the order of AQI_DIAL_TIERS; the last is capped so it can be drawn
const DIAL_BOUNDS: Record<AqiStandard, number[]> = {
	US: [0, 50, 100, 200, 300, 500],
	EUROPEAN: [0, 20, 40, 60, 100, 150],
};

/**
 * Where a reading sits on a dial of five equal level segments, 0-1: the level picks the segment and
 * the reading's place within that level's range picks the spot inside it.
 */
export function aqiDialPosition(value: number, standard: AqiStandard): number {
	if (!Number.isFinite(value)) return 0;
	const b = DIAL_BOUNDS[standard];
	const level = AQI_DIAL_TIERS.indexOf(AirQualityIndex.tierFor(value, standard));
	// Kept just inside the segment, so a reading on a boundary still shows in its own level
	const within = Math.min(0.98, Math.max(0.02, (value - b[level]) / (b[level + 1] - b[level])));
	return (level + within) / 5;
}

export class AirQualityIndex {
	// Concentrations in µg/m³ and tiers from Open-Meteo's "current" air quality. Null when the domain does not
	// cover the pollutant at the place (ammonia is CAMS-Europe only) or the API leaves it out.
	readonly pollutants: AirQualityPollutants;
	readonly pollen: PollenReadings;

	private constructor(
		readonly usValue: number,
		readonly europeanValue: number,
		readonly primaryStandard: AqiStandard,
		pollutants: AirQualityPollutants,
		pollen: PollenReadings = EMPTY_POLLEN,
	) {
		this.pollutants = pollutants;
		this.pollen = pollen;
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
	 * Both indices, primary first. The place sets the primary (EU AQI in Europe, else US AQI), not the
	 * metric/imperial switch.
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
		pollen: PollenReadings = EMPTY_POLLEN,
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
		return new AirQualityIndex(usAqi, europeanAqi, standard, pollutants, pollen);
	}
}
