import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'path';

export default defineConfig({
	base: '/react/',
	plugins: [react()],
	resolve: {
		alias: {
			'$lib': path.resolve(__dirname, '../shared'),
		},
		dedupe: ['html2canvas'],
	},
	esbuild: {
		tsconfigRaw: {
			compilerOptions: {
				jsx: 'react-jsx',
			},
		},
	},
	build: {
		minify: 'terser',
		sourcemap: false,
		cssCodeSplit: false,
		emptyOutDir: true,
		rollupOptions: {
			output: {
				manualChunks: () => 'app',
			},
		},
	},
});
