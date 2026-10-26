// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

/** What the browser tells us about this device, as label key and value pairs for the About page. */
export interface DeviceRow {
	labelKey: string;
	value: string;
}

// navigator.userAgentData and deviceMemory exist only in Chromium browsers
interface UaData {
	brands?: { brand: string; version: string }[];
	platform?: string;
	mobile?: boolean;
	getHighEntropyValues?: (hints: string[]) => Promise<Record<string, string>>;
}

/** A short browser name and major version from the user agent, for browsers without userAgentData (Firefox, Safari). */
export function browserName(userAgent: string): string {
	const rules: [RegExp, string][] = [
		[/Edg(?:A|iOS)?\/(\d+)/, 'Edge'],
		[/SamsungBrowser\/(\d+)/, 'Samsung Internet'],
		[/OPR\/(\d+)/, 'Opera'],
		[/(?:Firefox|FxiOS)\/(\d+)/, 'Firefox'],
		[/CriOS\/(\d+)/, 'Chrome'],
		[/Chrome\/(\d+)/, 'Chrome'],
		[/Version\/(\d+)[\d.]* (?:Mobile\/\S+ )?Safari\//, 'Safari'],
	];
	for (const [re, name] of rules) {
		const m = userAgent.match(re);
		if (m) return `${name} ${m[1]}`;
	}
	return userAgent;
}

/** The device pixel ratio, with at most 2 decimals (3.5294117647058822 → 3.53, 2 → 2). */
export function formatDensity(ratio: number): string {
	return `× ${Number(ratio.toFixed(2))}`;
}

export async function deviceInfo(appLanguage: string): Promise<DeviceRow[]> {
	const nav = navigator as Navigator & { userAgentData?: UaData; deviceMemory?: number };
	const ua = nav.userAgentData;
	const rows: DeviceRow[] = [];
	let model = '';
	let platformVersion = '';
	if (ua?.getHighEntropyValues) {
		try {
			const hi = await ua.getHighEntropyValues(['model', 'platformVersion', 'fullVersionList']);
			model = hi.model ?? '';
			platformVersion = hi.platformVersion ?? '';
		} catch {
			// Not allowed or not supported: fall back to the user agent below
		}
	}
	if (model) rows.push({ labelKey: 'device_model', value: model });
	const brands = ua?.brands?.filter((b) => !/not.?a.?brand/i.test(b.brand)).map((b) => `${b.brand} ${b.version}`).join(', ');
	rows.push({ labelKey: 'device_browser', value: brands || browserName(nav.userAgent) });
	const platform = ua?.platform || nav.platform;
	if (platform) rows.push({ labelKey: 'device_os', value: platformVersion ? `${platform} ${platformVersion}` : platform });
	rows.push({
		labelKey: 'device_screen',
		value: `${screen.width} × ${screen.height} · ${innerWidth} × ${innerHeight}`,
	});
	rows.push({ labelKey: 'device_density', value: formatDensity(devicePixelRatio) });
	// The browser's languages, then the one chosen in the app
	rows.push({ labelKey: 'device_language', value: `${(nav.languages ?? [nav.language]).join(', ')} · app: ${appLanguage}` });
	const tz = Intl.DateTimeFormat().resolvedOptions().timeZone;
	const offset = -new Date().getTimezoneOffset();
	const sign = offset >= 0 ? '+' : '−';
	rows.push({ labelKey: 'device_time_zone', value: `${tz} (UTC${sign}${Math.floor(Math.abs(offset) / 60)}${Math.abs(offset) % 60 ? ':' + String(Math.abs(offset) % 60).padStart(2, '0') : ''})` });
	if (nav.hardwareConcurrency) rows.push({ labelKey: 'device_processor', value: `${nav.hardwareConcurrency}` });
	if (nav.deviceMemory) rows.push({ labelKey: 'device_memory', value: `≈ ${nav.deviceMemory} GB` });
	rows.push({ labelKey: 'device_touch', value: `${nav.maxTouchPoints ?? 0}` });
	const standalone = matchMedia('(display-mode: standalone)').matches;
	rows.push({ labelKey: 'device_installer', value: standalone ? 'PWA' : 'Web' });
	return rows;
}
