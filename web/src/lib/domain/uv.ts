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

// Each level's range, in the order of the tiers; the last is capped so it can be drawn
const UV_DIAL_BOUNDS = [0, 3, 6, 8, 11, 14];
const UV_DIAL_TIERS: UvTier[] = ['low', 'moderate', 'high', 'veryHigh', 'extreme'];

/**
 * Where a UV reading sits on a dial of five equal level segments, 0-1: the level picks the segment and
 * the reading's place within that level's range picks the spot inside it.
 */
export function uvDialPosition(uv: number): number {
	if (!Number.isFinite(uv)) return 0;
	const level = UV_DIAL_TIERS.indexOf(uvTier(uv));
	const b = UV_DIAL_BOUNDS;
	// Kept just inside the segment, so a reading on a boundary still shows in its own level
	const within = Math.min(0.98, Math.max(0.02, (uv - b[level]) / (b[level + 1] - b[level])));
	return (level + within) / 5;
}
