#!/usr/bin/env node
const fs = require('fs');
const path = require('path');

// Flatten nested object keys using dot notation
function flattenKeys(obj, prefix = '') {
	const keys = new Set();

	for (const [key, value] of Object.entries(obj)) {
		const fullKey = prefix ? `${prefix}.${key}` : key;

		if (value !== null && typeof value === 'object' && !Array.isArray(value)) {
			// Recurse into nested objects
			flattenKeys(value, fullKey).forEach(k => keys.add(k));
		} else {
			// Leaf value (string, array, number, etc.)
			keys.add(fullKey);
		}
	}

	return keys;
}

function main() {
	const localesDir = path.join(__dirname, '..', 'shared', 'i18n', 'locales');

	// Read all locale files
	const localeFiles = fs.readdirSync(localesDir)
		.filter(f => f.endsWith('.json'))
		.sort();

	if (localeFiles.length === 0) {
		console.error('Error: No locale files found.');
		process.exit(1);
	}

	const locales = {};
	for (const file of localeFiles) {
		const filePath = path.join(localesDir, file);
		const locale = file.replace('.json', '');
		try {
			locales[locale] = JSON.parse(fs.readFileSync(filePath, 'utf-8'));
		} catch (err) {
			console.error(`Error reading ${file}: ${err.message}`);
			process.exit(1);
		}
	}

	// Flatten all keys for each locale
	const flattenedLocales = {};
	for (const [locale, data] of Object.entries(locales)) {
		flattenedLocales[locale] = flattenKeys(data);
	}

	// Compute union of all keys
	const allKeys = new Set();
	for (const keys of Object.values(flattenedLocales)) {
		keys.forEach(k => allKeys.add(k));
	}

	// Check for missing keys and report
	let hasMissing = false;
	for (const locale of Object.keys(flattenedLocales).sort()) {
		const missing = [...allKeys].filter(k => !flattenedLocales[locale].has(k));
		if (missing.length > 0) {
			console.log(`${locale}: missing ${missing.length} key(s)`);
			missing.forEach(k => console.log(`  - ${k}`));
			hasMissing = true;
		}
	}

	if (hasMissing) {
		process.exit(1);
	}

	const numKeys = allKeys.size;
	const numLocales = Object.keys(flattenedLocales).length;
	console.log(`i18n parity OK: ${numKeys} keys x ${numLocales} locales`);
	process.exit(0);
}

main();
