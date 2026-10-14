/**
 * Services, assets and libraries acknowledged on the About page.
 * Keep in sync with app/src/main/java/.../ui/settings/AboutScreen.kt.
 * `nameKey` / `noteKey` are i18n keys used instead of the literal `name` / `note`.
 */
export interface Credit {
	name?: string;
	nameKey?: string;
	license: string;
	url?: string;
	note?: string;
	noteKey?: string;
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

export const creditSections: CreditSection[] = [
	{
		titleKey: 'about_section_weather',
		items: [
			{ name: 'Open-Meteo', license: 'CC BY 4.0', url: 'https://open-meteo.com/en/licence' },
			// No published data licence; credit its upstream sources.
			{ name: 'Pirate Weather', license: 'NOAA, ECMWF, FMI, CMC', url: 'https://pirateweather.net', noteKey: 'settings_requires_api_key' },
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
		],
	},
	{
		titleKey: 'about_section_alerts',
		items: [
			{ nameKey: 'settings_alert_nws', license: 'Public domain', url: 'https://www.weather.gov/disclaimer' },
			{ nameKey: 'settings_alert_gdacs', license: 'CC BY 4.0', url: 'https://www.gdacs.org/About/termofuse.aspx', note: 'Global Disaster Alert and Coordination System, GDACS' },
			{ nameKey: 'settings_alert_meteoalarm', license: 'CC BY 4.0', url: 'https://api.meteoalarm.org', note: 'Data provided by EUMETNET members' },
			{ nameKey: 'settings_alert_jma', license: 'Public Data License 1.0', url: 'https://www.jma.go.jp/jma/en/copyright.html', note: 'Source: Japan Meteorological Agency website' },
			{ nameKey: 'settings_alert_eccc', license: 'ECCC Data Servers End-use Licence', url: 'https://eccc-msc.github.io/open-data/licence/readme_en/', note: 'Data Source: Environment and Climate Change Canada' },
			{ nameKey: 'settings_alert_bom', license: '© Commonwealth of Australia', url: 'https://www.bom.gov.au/copyright', note: 'Bureau of Meteorology' },
			{ nameKey: 'settings_alert_nhc', license: 'Public domain', url: 'https://www.nhc.noaa.gov' },
		],
	},
	{
		titleKey: 'about_section_imagery',
		items: [
			{ name: 'NASA Blue Marble', license: 'Public domain', url: 'https://visibleearth.nasa.gov/collection/1484/blue-marble' },
		],
	},
	{
		titleKey: 'about_section_fonts',
		items: [
			{ name: 'Google Fonts', license: 'SIL OFL 1.1', url: 'https://fonts.google.com', note: FONT_FAMILIES },
		],
	},
	{
		titleKey: 'about_section_libraries',
		items: [
			{ name: 'Svelte, SvelteKit', license: 'MIT', url: 'https://svelte.dev' },
			{ name: 'svelte-i18n', license: 'MIT', url: 'https://github.com/kaisermann/svelte-i18n' },
			{ name: 'html2canvas', license: 'MIT', url: 'https://html2canvas.hertzen.com' },
			{ name: 'Vite', license: 'MIT', url: 'https://vite.dev' },
		],
	},
];
