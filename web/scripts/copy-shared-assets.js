#!/usr/bin/env node
/**
 * Copies shared static assets into the Svelte app's static directory.
 * Run before building.
 *
 * Usage: node scripts/copy-shared-assets.js
 */

import { cpSync, mkdirSync } from 'fs';
import { join, dirname } from 'path';
import { fileURLToPath } from 'url';

const __dirname = dirname(fileURLToPath(import.meta.url));
const WEB_ROOT = join(__dirname, '..');
const SHARED_ASSETS = join(WEB_ROOT, 'shared', 'assets');

const targets = [
	join(WEB_ROOT, 'svelte', 'static'),
];

for (const target of targets) {
	mkdirSync(join(target, 'icons'), { recursive: true });
	cpSync(SHARED_ASSETS, target, { recursive: true });
}

console.log('Shared assets copied to Svelte app.');
