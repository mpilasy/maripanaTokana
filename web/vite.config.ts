import { sveltekit } from '@sveltejs/kit/vite';
import { defineConfig } from 'vitest/config';

export default defineConfig({
	plugins: [sveltekit()],
	resolve: {
		dedupe: ['html2canvas'],
	},
	test: {
		environment: 'node',
		include: ['src/**/*.test.ts'],
	},
	build: {
		rollupOptions: {
			output: {
				manualChunks: () => 'app',
			},
		},
	},
});
