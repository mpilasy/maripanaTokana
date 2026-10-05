import adapter from '@sveltejs/adapter-node';

/** @type {import('@sveltejs/kit').Config} */
const config = {
	kit: {
		adapter: adapter({
			out: 'build',
		}),
		paths: {
			base: ''
		},
		csp: {
			mode: 'auto',
			reportOnly: {
				// Validated default policy — report-only until verified in browser console
				'default-src': ["'self'"],
				'script-src': ["'self'"],
				'style-src': ["'self'", "'unsafe-inline'"],
				'font-src': ["'self'"],
				'img-src': ["'self'", 'data:', 'blob:'],
				'connect-src': [
					"'self'",
					'https://api.open-meteo.com',
					'https://air-quality-api.open-meteo.com',
					'https://geocoding-api.open-meteo.com',
					'https://api.pirateweather.net',
					'https://api.weather.gov',
					'https://nominatim.openstreetmap.org',
					'https://www.gdacs.org',
					'https://www.jma.go.jp'
				],
				'worker-src': ["'self'"],
				'manifest-src': ["'self'"],
				'frame-ancestors': ["'none'"],
				'base-uri': ["'self'"],
				'form-action': ["'self'"],
				// Required by SvelteKit for report-only mode; see src/routes/api/csp-report
				'report-uri': ['/api/csp-report']
			}
		}
	}
};

export default config;
