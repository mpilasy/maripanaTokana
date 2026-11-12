// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

/**
 * Services, assets and libraries acknowledged on the About page.
 * Keep in sync with app/src/main/java/.../ui/common/about/Credits.kt.
 * `nameKey` / `noteKey` are i18n keys used instead of the literal `name` / `note`.
 */
export interface Credit {
	name?: string;
	nameKey?: string;
	license: string;
	url?: string;
	note?: string;
	noteKey?: string;
	/** A licence text in static/licenses/ that the Licenses page shows in the app, instead of opening url */
	text?: string;
}

export interface CreditSection {
	titleKey: string;
	items: Credit[];
}

const FONT_FAMILIES =
	'Orbitron, Outfit, Rajdhani, Inter, Oxanium, Nunito, Space Grotesk, DM Sans, Sora, Source Sans 3, ' +
	'Manrope, Rubik, Josefin Sans, Lato, Cormorant Garamond, Fira Sans, Playfair Display, Work Sans, ' +
	'Quicksand, Nunito Sans, Comfortaa, Karla, Baloo 2, Poppins, Exo 2, Barlow, Michroma, Saira, Jost, ' +
	'Atkinson Hyperlegible, Fira Code, Montserrat, Open Sans, Space Mono, Plus Jakarta Sans, Archivo, ' +
	'Archivo Narrow, Lora';

/** The licence texts of the code, icons and fonts that the web app ships (scripts/web_third_party_licenses.py) */
const LICENSES = '/licenses/';

export const creditSections: CreditSection[] = [
	{
		titleKey: 'about_section_weather',
		items: [
			{ name: 'Open-Meteo', license: 'CC BY 4.0', url: 'https://open-meteo.com/en/licence' },
			// No published data licence; credit its upstream sources.
			{ name: 'Pirate Weather', license: 'NOAA, ECMWF, FMI, CMC', url: 'https://pirateweather.net', noteKey: 'settings_requires_api_key' },
			{ name: 'Open-Meteo Marine', license: 'CC BY 4.0', url: 'https://open-meteo.com/en/docs/marine-weather-api', note: 'Waves, sea temperature and tides. Generated using E.U. Copernicus Marine Service Information' },
			{ name: 'Open-Meteo Historical Weather', license: 'CC BY 4.0', url: 'https://open-meteo.com/en/docs/historical-weather-api', note: '10-year normals. Contains modified Copernicus Climate Change Service information' },
		],
	},
	{
		titleKey: 'about_section_air_quality',
		items: [
			{ name: 'Open-Meteo Air Quality', license: 'CC BY 4.0', url: 'https://open-meteo.com/en/docs/air-quality-api', note: 'Generated using Copernicus Atmosphere Monitoring Service information' },
		],
	},
	{
		titleKey: 'about_section_geocoding',
		items: [
			{ name: 'Nominatim / OpenStreetMap', license: 'ODbL', url: 'https://www.openstreetmap.org/copyright', note: '© OpenStreetMap contributors' },
			{ name: 'Open-Meteo Geocoding', license: 'CC BY 4.0', url: 'https://open-meteo.com/en/docs/geocoding-api', note: 'Location data based on GeoNames (CC BY 4.0)' },
			{ name: 'Plus Codes (Open Location Code)', license: 'Apache-2.0', url: 'https://github.com/google/open-location-code', note: 'Open format; our own implementation', text: LICENSES + 'apache-2.0.txt' },
		],
	},
	{
		titleKey: 'about_section_alerts',
		items: [
			{ nameKey: 'settings_alert_nws', license: 'Public domain', url: 'https://www.weather.gov/disclaimer' },
			{ nameKey: 'settings_alert_gdacs', license: 'CC BY 4.0 (EC reuse policy)', url: 'https://www.gdacs.org/About/termofuse.aspx', note: 'Source: GDACS (European Commission JRC, UN OCHA)' },
			{ nameKey: 'settings_alert_meteoalarm', license: 'CC BY 4.0', url: 'https://api.meteoalarm.org', note: 'Data provided by EUMETNET members' },
			{ nameKey: 'settings_alert_jma', license: 'Public Data License (Version 1.0)', url: 'https://www.jma.go.jp/jma/en/copyright.html', note: 'Source: Japan Meteorological Agency website' },
			{ nameKey: 'settings_alert_eccc', license: 'ECCC Data Services End-use Licence', url: 'https://eccc-msc.github.io/open-data/licence/readme_en/', note: 'Data Source: Environment and Climate Change Canada' },
			{ nameKey: 'settings_alert_bom', license: '© Commonwealth of Australia', url: 'https://www.bom.gov.au/copyright', note: 'Bureau of Meteorology' },
			{ nameKey: 'settings_alert_nhc', license: 'Public domain', url: 'https://www.nhc.noaa.gov' },
		],
	},
	{
		titleKey: 'about_section_imagery',
		items: [
			{ name: 'NASA Blue Marble', license: 'Public domain', url: 'https://visibleearth.nasa.gov/collection/1484/blue-marble' },
			{ name: 'Meteocons', license: 'MIT', url: 'https://github.com/basmilius/weather-icons', note: 'Weather icons by Bas Milius', text: LICENSES + 'meteocons.txt' },
			{ name: 'Material Design icons', license: 'Apache-2.0', url: 'https://github.com/google/material-design-icons', note: 'Interface icons by Google', text: LICENSES + 'apache-2.0.txt' },
			// Web only: Android uses Material Design icons for refresh and share
			{ name: 'Feather icons', license: 'MIT', url: 'https://feathericons.com', note: 'Refresh and share icons by Cole Bemis', text: LICENSES + 'feather.txt' },
			{ name: 'F-Droid badge', license: 'CC BY-SA 3.0 or GPL-3.0+', url: 'https://gitlab.com/fdroid/artwork', note: 'Andrew Nayenko' },
			{ name: 'GitHub badge', license: 'CC BY-SA 3.0', url: 'https://github.com/Kunzisoft/Github-badge', note: 'Kunzisoft' },
			{ name: 'Obtainium badge', license: 'GPL-3.0', url: 'https://github.com/ImranR98/Obtainium', note: 'Imran Remtulla' },
			// Web only: the install badge
			{ name: 'PWA logo', license: 'CC0 1.0', url: 'https://github.com/webmaxru/progressive-web-apps-logo', note: 'Maxim Salnikov and contributors' },
		],
	},
	{
		titleKey: 'about_section_fonts',
		items: [
			{ name: 'Google Fonts', license: 'SIL OFL 1.1', url: 'https://fonts.google.com', note: FONT_FAMILIES, text: LICENSES + 'fonts.txt' },
		],
	},
	{
		titleKey: 'about_section_libraries',
		items: [
			{ name: 'SunCalc', license: 'BSD-2-Clause', url: 'https://github.com/mourner/suncalc', note: 'Moon formulas by Volodymyr Agafonkin, in our TypeScript code', text: LICENSES + 'suncalc.txt' },
			{ name: 'Svelte, SvelteKit', license: 'MIT', url: 'https://svelte.dev', text: LICENSES + 'svelte.txt' },
			{ name: 'svelte-i18n', license: 'MIT', url: 'https://github.com/kaisermann/svelte-i18n', text: LICENSES + 'svelte-i18n.txt' },
			{ name: 'html2canvas', license: 'MIT', url: 'https://html2canvas.hertzen.com', text: LICENSES + 'html2canvas.txt' },
			{ name: 'Vite', license: 'MIT', url: 'https://vite.dev' },
			// Web only: the licence texts of all code and icons in the web bundle
			{ name: 'Licence texts', license: 'MIT, BSD, Apache-2.0', text: LICENSES + 'third-party.txt', note: 'svelte-i18n, intl-messageformat (FormatJS), deepmerge, html2canvas, devalue and the other code in this web app' },
		],
	},
];
