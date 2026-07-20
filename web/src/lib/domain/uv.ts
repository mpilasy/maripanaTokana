export type UvTier = 'low' | 'moderate' | 'high' | 'veryHigh' | 'extreme';

// EPA UV Index color scale (epa.gov/sunsafety/uv-index-scale-0): Green/Yellow/Orange/Red/Violet.
// Thresholds match the Low/Moderate/High/Very High/Extreme bands used for uv_labels elsewhere.
export const UV_TIER_COLORS: Record<UvTier, string> = {
	low: '#299501',
	moderate: '#F7E401',
	high: '#F85900',
	veryHigh: '#D8001D',
	extreme: '#6B49C8',
};

export function uvTier(uvIndex: number): UvTier {
	if (uvIndex < 3) return 'low';
	if (uvIndex < 6) return 'moderate';
	if (uvIndex < 8) return 'high';
	if (uvIndex < 11) return 'veryHigh';
	return 'extreme';
}

export function uvColorFor(uvIndex: number): string {
	return UV_TIER_COLORS[uvTier(uvIndex)];
}
