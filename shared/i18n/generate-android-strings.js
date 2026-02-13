#!/usr/bin/env node
/**
 * Generates Android strings.xml files from canonical JSON locale files.
 *
 * Usage: node generate-android-strings.js
 *
 * Reads from:  shared/i18n/locales/*.json
 * Writes to:   app/src/main/res/values[-xx]/strings.xml
 */

import { readFileSync, writeFileSync, readdirSync, mkdirSync } from 'fs';
import { join, dirname } from 'path';
import { fileURLToPath } from 'url';

const __dirname = dirname(fileURLToPath(import.meta.url));
const LOCALES_DIR = join(__dirname, 'locales');
const ANDROID_RES = join(__dirname, '..', '..', 'app', 'src', 'main', 'res');

// Placeholder mappings per key: ordered list of {name} placeholders → %N$s
// Keys with a single placeholder get %1$s, keys with multiple get %1$s, %2$s, etc.
const PLACEHOLDER_ORDER = {
	updated_time: ['time'],
	hash_version: ['hash'],
	widget_today_in: ['location'],
	widget_now_in: ['location'],
	widget_min_max: ['min_val', 'min_unit', 'max_val', 'max_unit'],
};

// Keys that use printf-style format specifiers (%.1f etc) — not our {name} placeholders
const FORMATTED_FALSE_KEYS = new Set(['visibility_km', 'visibility_mi']);

/**
 * Escape a string value for Android XML.
 * - Apostrophes → backslash-escaped
 * - XML special chars &, <, > → entities
 * - Non-ASCII supplementary chars (emoji) → numeric XML entities
 */
function escapeXml(s) {
	let result = '';
	for (const ch of s) {
		const cp = ch.codePointAt(0);
		if (ch === '&') result += '&amp;';
		else if (ch === '<') result += '&lt;';
		else if (ch === '>') result += '&gt;';
		else if (ch === "'") result += "\\'";
		else if (cp > 0xffff) result += `&#x${cp.toString(16)};`;
		else result += ch;
	}
	return result;
}

/**
 * Convert {name} placeholders to Android %N$s format.
 */
function convertPlaceholders(key, value) {
	const order = PLACEHOLDER_ORDER[key];
	if (!order) return value;

	let result = value;
	for (let i = 0; i < order.length; i++) {
		result = result.replace(`{${order[i]}}`, `%${i + 1}$s`);
	}
	return result;
}

/**
 * Build a strings.xml from a locale JSON object.
 */
function buildStringsXml(data) {
	const lines = ['<resources>'];

	// Collect all keys: top-level (non-object, non-array for now) + android_only
	const androidOnly = data.android_only || {};
	const stringKeys = [];
	const arrayKeys = [];

	// Top-level shared keys
	for (const [key, val] of Object.entries(data)) {
		if (key === 'web_only' || key === 'android_only') continue;
		if (Array.isArray(val)) {
			arrayKeys.push([key, val]);
		} else if (typeof val === 'string') {
			stringKeys.push([key, val]);
		}
	}

	// Android-only keys
	for (const [key, val] of Object.entries(androidOnly)) {
		if (Array.isArray(val)) {
			arrayKeys.push([key, val]);
		} else if (typeof val === 'string') {
			stringKeys.push([key, val]);
		}
	}

	// Emit string entries
	for (const [key, val] of stringKeys) {
		let value = convertPlaceholders(key, val);
		value = escapeXml(value);

		if (FORMATTED_FALSE_KEYS.has(key)) {
			lines.push(`    <string name="${key}" formatted="false">${value}</string>`);
		} else {
			lines.push(`    <string name="${key}">${value}</string>`);
		}
	}

	// Emit string-array entries
	for (const [key, items] of arrayKeys) {
		lines.push(`    <string-array name="${key}">`);
		for (const item of items) {
			lines.push(`        <item>${escapeXml(item)}</item>`);
		}
		lines.push('    </string-array>');
	}

	lines.push('</resources>');
	return lines.join('\n') + '\n';
}

// Main
const files = readdirSync(LOCALES_DIR).filter(f => f.endsWith('.json'));

for (const file of files) {
	const tag = file.replace('.json', '');
	const data = JSON.parse(readFileSync(join(LOCALES_DIR, file), 'utf-8'));

	const xml = buildStringsXml(data);

	// Android uses "values" for default (en) and "values-xx" for others
	const dirName = tag === 'en' ? 'values' : `values-${tag}`;
	const outDir = join(ANDROID_RES, dirName);
	mkdirSync(outDir, { recursive: true });
	writeFileSync(join(outDir, 'strings.xml'), xml, 'utf-8');

	console.log(`Generated ${dirName}/strings.xml`);
}
