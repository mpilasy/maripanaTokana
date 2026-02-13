import { sveltekit } from '@sveltejs/kit/vite';
import { defineConfig } from 'vite';

export default defineConfig({
	plugins: [sveltekit()],
	resolve: {
		dedupe: ['html2canvas'],
	},
	build: {
		rollupOptions: {
			output: {
				manualChunks: () => 'app',
			},
		},
	},
});
