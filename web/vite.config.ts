// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

import adapter from '@sveltejs/adapter-node';
import { sveltekit } from '@sveltejs/kit/vite';
import { defineConfig } from 'vitest/config';
import { execFileSync } from 'node:child_process';

// The short hash and the UTC commit time of HEAD, for the About page. The Docker build has no .git:
// .claude/deploy-web.sh gives them as the GIT_HASH and COMMIT_TIME build arguments.
function git(args: string[]): string {
	try {
		return execFileSync('git', args, { env: { ...process.env, TZ: 'UTC' }, stdio: ['ignore', 'pipe', 'ignore'] }).toString().trim();
	} catch {
		return '';
	}
}
const gitHash = process.env.GIT_HASH || git(['rev-parse', 'HEAD']).slice(0, 7);
const commitTime = process.env.COMMIT_TIME || git(['log', '-1', '--format=%cd', '--date=format-local:%Y-%m-%d %H:%M']);

export default defineConfig({
	define: {
		__GIT_HASH__: JSON.stringify(gitHash),
		__COMMIT_TIME__: JSON.stringify(commitTime),
	},
	plugins: [
		sveltekit({
			adapter: adapter({ out: 'build' }),
			paths: { base: '' },
			// One JS bundle. SvelteKit 3 ignores build.rollupOptions.output.manualChunks.
			output: { bundleStrategy: 'single' },
			csp: {
				mode: 'auto',
				// Enforced; violations are still reported to src/routes/api/csp-report
				directives: {
					'default-src': ['self'],
					'script-src': ['self'],
					'style-src': ['self', 'unsafe-inline'],
					'font-src': ['self'],
					'img-src': ['self', 'data:', 'blob:'],
					'connect-src': [
						'self',
						'https://api.open-meteo.com',
						'https://air-quality-api.open-meteo.com',
						'https://archive-api.open-meteo.com',
						'https://marine-api.open-meteo.com',
						'https://geocoding-api.open-meteo.com',
						'https://api.pirateweather.net',
						'https://api.weather.gov',
						'https://nominatim.openstreetmap.org',
						'https://www.gdacs.org',
						'https://www.jma.go.jp'
					],
					'worker-src': ['self'],
					'manifest-src': ['self'],
					'frame-ancestors': ['none'],
					'base-uri': ['self'],
					'form-action': ['self'],
					'report-uri': ['/api/csp-report']
				}
			}
		})
	],
	resolve: {
		dedupe: ['html2canvas'],
	},
	test: {
		environment: 'node',
		include: ['src/**/*.test.ts'],
	},
});
