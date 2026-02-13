/**
 * Post-build: inline CSS into index.html.
 *
 * JS modules must stay as files because ES module imports between
 * data: URI modules don't resolve via import maps.
 */
import { readFileSync, writeFileSync, unlinkSync, readdirSync, statSync, rmdirSync } from 'fs';
import { join, resolve } from 'path';

const BUILD = resolve('build');
const HTML_PATH = join(BUILD, 'index.html');

let html = readFileSync(HTML_PATH, 'utf-8');

// Inline CSS: <link href="...css" rel="stylesheet"> → <style>...</style>
html = html.replace(
	/<link\s+href="([^"]+\.css)"\s+rel="stylesheet"\s*>/gi,
	(orig, href) => {
		try {
			const css = readFileSync(join(BUILD, href), 'utf-8');
			unlinkSync(join(BUILD, href));
			return `<style>${css}</style>`;
		} catch { return orig; }
	}
);

writeFileSync(HTML_PATH, html);

// Clean up empty directories
function removeEmptyDirs(dir) {
	try {
		for (const entry of readdirSync(dir)) {
			const full = join(dir, entry);
			if (statSync(full).isDirectory()) removeEmptyDirs(full);
		}
		if (readdirSync(dir).length === 0) rmdirSync(dir);
	} catch {}
}
removeEmptyDirs(join(BUILD, '_app', 'immutable', 'assets'));

console.log('CSS inlined into index.html');
