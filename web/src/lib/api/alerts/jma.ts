import type { WeatherAlert, AlertLevel } from '$lib/domain/weatherData';

export function isInJapan(lat: number, lon: number): boolean {
	return lat >= 24.0 && lat <= 46.0 && lon >= 122.0 && lon <= 154.0;
}

function nearestPrefectureCode(lat: number, lon: number): string {
	const prefectures: [string, number, number][] = [
		['016000',43.06,141.35],['020000',40.82,140.74],['030000',39.70,141.15],
		['040000',38.27,140.87],['050000',39.72,140.10],['060000',38.24,140.36],
		['070000',37.75,140.47],['080000',36.34,140.45],['090000',36.57,139.88],
		['100000',36.39,139.06],['110000',35.86,139.65],['120000',35.61,140.12],
		['130000',35.69,139.69],['140000',35.45,139.64],['150000',37.90,139.02],
		['160000',36.70,137.21],['170000',36.59,136.63],['180000',36.07,136.22],
		['190000',35.66,138.57],['200000',36.65,138.18],['210000',35.39,136.72],
		['220000',34.98,138.38],['230000',35.18,136.91],['240000',34.73,136.51],
		['250000',35.00,135.87],['260000',35.02,135.76],['270000',34.69,135.50],
		['280000',34.69,135.19],['290000',34.69,135.83],['300000',34.23,135.17],
		['310000',35.50,134.24],['320000',35.47,133.05],['330000',34.66,133.93],
		['340000',34.40,132.45],['350000',34.19,131.47],['360000',34.07,134.56],
		['370000',34.34,134.04],['380000',33.84,132.77],['390000',33.56,133.53],
		['400000',33.61,130.42],['410000',33.25,130.30],['420000',32.74,129.87],
		['430000',32.79,130.74],['440000',33.24,131.61],['450000',31.91,131.42],
		['460000',31.56,130.56],['471000',26.21,127.68]
	];
	let best = prefectures[0];
	let bestDist = Infinity;
	for (const p of prefectures) {
		const d = (lat - p[1]) ** 2 + (lon - p[2]) ** 2;
		if (d < bestDist) { bestDist = d; best = p; }
	}
	return best[0];
}

const PREFECTURE_NAMES: Record<string, string> = {
	'016000': 'Hokkaido', '020000': 'Aomori', '030000': 'Iwate', '040000': 'Miyagi',
	'050000': 'Akita', '060000': 'Yamagata', '070000': 'Fukushima', '080000': 'Ibaraki',
	'090000': 'Tochigi', '100000': 'Gunma', '110000': 'Saitama', '120000': 'Chiba',
	'130000': 'Tokyo', '140000': 'Kanagawa', '150000': 'Niigata', '160000': 'Toyama',
	'170000': 'Ishikawa', '180000': 'Fukui', '190000': 'Yamanashi', '200000': 'Nagano',
	'210000': 'Gifu', '220000': 'Shizuoka', '230000': 'Aichi', '240000': 'Mie',
	'250000': 'Shiga', '260000': 'Kyoto', '270000': 'Osaka', '280000': 'Hyogo',
	'290000': 'Nara', '300000': 'Wakayama', '310000': 'Tottori', '320000': 'Shimane',
	'330000': 'Okayama', '340000': 'Hiroshima', '350000': 'Yamaguchi', '360000': 'Tokushima',
	'370000': 'Kagawa', '380000': 'Ehime', '390000': 'Kochi', '400000': 'Fukuoka',
	'410000': 'Saga', '420000': 'Nagasaki', '430000': 'Kumamoto', '440000': 'Oita',
	'450000': 'Miyazaki', '460000': 'Kagoshima', '471000': 'Okinawa',
};

function prefectureName(areaCode: string): string | null {
	const prefix = areaCode.slice(0, 2);
	const entry = Object.entries(PREFECTURE_NAMES).find(([k]) => k.slice(0, 2) === prefix);
	return entry ? entry[1] : null;
}

function jmaWarningName(code: string): string {
	const names: Record<string, string> = {
		'01': 'Special Warning',
		'02': 'Heavy Rain Warning',
		'03': 'Flood Warning',
		'04': 'Storm Warning',
		'05': 'Snowstorm Warning',
		'06': 'Heavy Snow Warning',
		'07': 'Wave Warning',
		'08': 'Storm Surge Warning',
		'10': 'Heavy Rain Advisory',
		'12': 'Strong Wind Advisory',
		'13': 'Wave Advisory',
		'14': 'Storm Surge Advisory',
		'16': 'Flood Advisory',
		'17': 'Frost Advisory',
		'18': 'Thunder Advisory',
		'19': 'Dry Advisory',
		'20': 'Dense Fog Advisory',
		'21': 'Low Temperature Advisory',
		'22': 'Heavy Snow Advisory',
	};
	return names[code] ?? `Weather Warning (${code})`;
}

export async function fetchJmaAlerts(lat: number, lon: number): Promise<WeatherAlert[]> {
	if (!isInJapan(lat, lon)) return [];
	try {
		const areaCode = nearestPrefectureCode(lat, lon);
		const res = await fetch(`https://www.jma.go.jp/bosai/warning/data/warning/${areaCode}.json`);
		if (!res.ok) return [];
		// eslint-disable-next-line @typescript-eslint/no-explicit-any
		const data: any = await res.json();
		const warningMap = new Map<string, { level: AlertLevel; areas: Set<string> }>();
		// eslint-disable-next-line @typescript-eslint/no-explicit-any
		for (const areaType of (data.areaTypes ?? [])) {
			// eslint-disable-next-line @typescript-eslint/no-explicit-any
			for (const area of (areaType.areas ?? [])) {
				const areaName = prefectureName(area.code ?? '');
				// eslint-disable-next-line @typescript-eslint/no-explicit-any
				for (const w of (area.warnings ?? [])) {
					if (w.status !== '発表' && w.status !== '継続') continue;
					const level: AlertLevel = w.code === '01' ? 'emergency'
						: (parseInt(w.code) <= 8) ? 'warning' : 'watch';
					const warnName = jmaWarningName(w.code);
					const existing = warningMap.get(warnName);
					if (!existing) {
						const areas = new Set<string>();
						if (areaName) areas.add(areaName);
						warningMap.set(warnName, { level, areas });
					} else {
						if (areaName) existing.areas.add(areaName);
						const levels: AlertLevel[] = ['watch', 'warning', 'emergency'];
						if (levels.indexOf(level) > levels.indexOf(existing.level)) existing.level = level;
					}
				}
			}
		}
		return Array.from(warningMap.entries()).map(([title, { level, areas }]) => ({
			level, title, description: Array.from(areas).join(', '), source: 'jma' as const,
		}));
	} catch { return []; }
}
