// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

// Postal abbreviations people type after a city name ("Sudbury, ON", "Austin, TX")
const REGION_ABBREVIATIONS: Record<string, string> = {
	AL: 'Alabama', AK: 'Alaska', AZ: 'Arizona', AR: 'Arkansas', CA: 'California',
	CO: 'Colorado', CT: 'Connecticut', DE: 'Delaware', DC: 'Washington, D.C.', FL: 'Florida',
	GA: 'Georgia', HI: 'Hawaii', ID: 'Idaho', IL: 'Illinois', IN: 'Indiana', IA: 'Iowa',
	KS: 'Kansas', KY: 'Kentucky', LA: 'Louisiana', ME: 'Maine', MD: 'Maryland',
	MA: 'Massachusetts', MI: 'Michigan', MN: 'Minnesota', MS: 'Mississippi', MO: 'Missouri',
	MT: 'Montana', NE: 'Nebraska', NV: 'Nevada', NH: 'New Hampshire', NJ: 'New Jersey',
	NM: 'New Mexico', NY: 'New York', NC: 'North Carolina', ND: 'North Dakota', OH: 'Ohio',
	OK: 'Oklahoma', OR: 'Oregon', PA: 'Pennsylvania', RI: 'Rhode Island', SC: 'South Carolina',
	SD: 'South Dakota', TN: 'Tennessee', TX: 'Texas', UT: 'Utah', VT: 'Vermont',
	VA: 'Virginia', WA: 'Washington', WV: 'West Virginia', WI: 'Wisconsin', WY: 'Wyoming',
	PR: 'Puerto Rico',
	AB: 'Alberta', BC: 'British Columbia', MB: 'Manitoba', NB: 'New Brunswick',
	NL: 'Newfoundland and Labrador', NS: 'Nova Scotia', NT: 'Northwest Territories', NU: 'Nunavut',
	ON: 'Ontario', PE: 'Prince Edward Island', QC: 'Quebec', SK: 'Saskatchewan', YT: 'Yukon',
};

/**
 * Whether the text after the comma names one of these regions: a postal abbreviation of the state or
 * province, or the start of a word in it ("on" is Ontario, not the end of "Washington").
 * Mirrors Android's matchesRegionQualifier.
 */
export function matchesRegionQualifier(qualifier: string, ...regions: (string | undefined)[]): boolean {
	const q = qualifier.trim().toLowerCase();
	if (!q) return true;
	const expanded = REGION_ABBREVIATIONS[q.toUpperCase()]?.toLowerCase();
	return regions.filter((r): r is string => !!r).map((r) => r.toLowerCase()).some((region) =>
		region === expanded || region.startsWith(q) || region.includes(` ${q}`) || region.includes(`-${q}`));
}
