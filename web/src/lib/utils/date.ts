// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

export function formatTime(timestamp: number): string {
	const d = new Date(timestamp);
	const hh = String(d.getHours()).padStart(2, '0');
	const mm = String(d.getMinutes()).padStart(2, '0');
	return `${hh}:${mm}`;
}

/** True if the timestamp falls on the device's current calendar day. */
export function isToday(timestamp: number): boolean {
	const d = new Date(timestamp);
	const now = new Date();
	return d.getFullYear() === now.getFullYear() && d.getMonth() === now.getMonth() && d.getDate() === now.getDate();
}

// Formatters are slow to make, so they are kept
const dateFormatters = new Map<string, Intl.DateTimeFormat>();

export function formatDate(timestamp: number, localeTag: string, weekdayNames?: string[], monthNames?: string[]): string {
	const d = new Date(timestamp);

	const intlSupported = Intl.DateTimeFormat.supportedLocalesOf([localeTag]).length > 0;

	if (intlSupported || !weekdayNames || !monthNames) {
		let formatter = dateFormatters.get(localeTag);
		if (!formatter) {
			formatter = new Intl.DateTimeFormat(localeTag, {
				weekday: 'long', day: 'numeric', month: 'long', year: 'numeric',
				hour: 'numeric', minute: '2-digit'
			});
			dateFormatters.set(localeTag, formatter);
		}
		return formatter.format(d);
	}

	// Build from arrays for unsupported locales (mg, ne)
	const weekday = weekdayNames[d.getDay()];
	const month = monthNames[d.getMonth()];
	const day = d.getDate();
	const year = d.getFullYear();
	const hours = String(d.getHours()).padStart(2, '0');
	const minutes = String(d.getMinutes()).padStart(2, '0');

	return `${weekday}, ${day} ${month} ${year} ${hours}:${minutes}`;
}

const shortDateFormatters = new Map<string, Intl.DateTimeFormat>();

/** Malagasy short weekday names from Sunday, as in CLDR (Android's "EEE"). */
const MG_SHORT_WEEKDAYS = ['Alah', 'Alats', 'Tal', 'Alar', 'Alak', 'Zom', 'Asab'];

/** Short date like "Mon 12 Oct" (matches Android's "EEE d MMM"), in the device's timezone. */
export function formatShortDate(timestamp: number, localeTag: string, weekdayNames?: string[], monthNames?: string[]): string {
	const d = new Date(timestamp);
	const intlSupported = Intl.DateTimeFormat.supportedLocalesOf([localeTag]).length > 0;

	if (intlSupported || !weekdayNames || !monthNames) {
		let formatter = shortDateFormatters.get(localeTag);
		if (!formatter) {
			formatter = new Intl.DateTimeFormat(localeTag, { weekday: 'short', day: 'numeric', month: 'short' });
			shortDateFormatters.set(localeTag, formatter);
		}
		return formatter.format(d);
	}

	// Build from arrays for unsupported locales (mg, ne); Devanagari names stay in full
	const month = monthNames[d.getMonth()];
	const short = (name: string) => (localeTag === 'ne' ? name : name.substring(0, 3));
	// Three letters would make Alahady and Alatsinainy both "Ala": use the short names that Android shows
	const weekday = localeTag === 'mg' ? MG_SHORT_WEEKDAYS[d.getDay()] : short(weekdayNames[d.getDay()]);
	return `${weekday} ${d.getDate()} ${short(month)}`;
}

/** HH:mm of an epoch (UTC millis) in the device's timezone. */
export function formatHourInDeviceTime(epochMillis: number): string {
	const d = new Date(epochMillis);
	return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
}

/** HH:mm of an epoch (UTC millis) at the location, from its UTC offset. */
export function formatHourAtLocation(epochMillis: number, utcOffsetSec: number): string {
	const locationLocalMs = epochMillis + utcOffsetSec * 1000;
	const d = new Date(locationLocalMs);
	// UTC methods, because the offset is already in the time
	return `${String(d.getUTCHours()).padStart(2, '0')}:${String(d.getUTCMinutes()).padStart(2, '0')}`;
}

// Formatters are slow to make, so they are kept
const locationTimeFormatters = new Map<string, Intl.DateTimeFormat>();

/** The current time at a location, from its UTC offset; with the weekday when its date differs from the device's. */
export function formatLocationCurrentTime(utcOffsetSeconds: number, localeTag: string): string {
	const now = Date.now();
	const locationMs = now + utcOffsetSeconds * 1000;
	const dLocation = new Date(locationMs);
	const dDevice = new Date(now);
	
	// Numeric checks, faster than toLocaleDateString
	if (
		dDevice.getFullYear() !== dLocation.getUTCFullYear() ||
		dDevice.getMonth() !== dLocation.getUTCMonth() ||
		dDevice.getDate() !== dLocation.getUTCDate()
	) {
		let formatter = locationTimeFormatters.get(localeTag);
		if (!formatter) {
			formatter = new Intl.DateTimeFormat(localeTag, {
				weekday: 'short', hour: '2-digit', minute: '2-digit', hour12: false, timeZone: 'UTC'
			});
			locationTimeFormatters.set(localeTag, formatter);
		}
		return formatter.format(dLocation);
	}
	
	const hh = String(dLocation.getUTCHours()).padStart(2, '0');
	const mm = String(dLocation.getUTCMinutes()).padStart(2, '0');
	return `${hh}:${mm}`;
}

/** True when the location's UTC offset differs from the device's. */
export function isRemoteTimezone(utcOffsetSeconds: number): boolean {
	const deviceOffsetSeconds = new Date().getTimezoneOffset() * -60;
	return deviceOffsetSeconds !== utcOffsetSeconds;
}

const dayNameFormatters = new Map<string, Intl.DateTimeFormat>();
const dayMonthFormatters = new Map<string, Intl.DateTimeFormat>();
const alertTimeFormatters = new Map<string, Intl.DateTimeFormat>();

/**
 * The day name at the location, not in the browser's timezone: `timestamp` is local midnight at the location.
 * The function adds the location's offset and formats in UTC, so the day does not shift with the browser's zone.
 */
export function formatDayName(timestamp: number, localeTag: string, utcOffsetSeconds: number, short = false, weekdayNames?: string[]): string {
	const adjustedDate = new Date(timestamp + utcOffsetSeconds * 1000);

	const intlSupported = Intl.DateTimeFormat.supportedLocalesOf([localeTag]).length > 0;

	if (intlSupported || !weekdayNames) {
		const key = short ? `${localeTag}:short` : localeTag;
		let formatter = dayNameFormatters.get(key);
		if (!formatter) {
			formatter = new Intl.DateTimeFormat(localeTag, { weekday: short ? 'short' : 'long', timeZone: 'UTC' });
			dayNameFormatters.set(key, formatter);
		}
		return formatter.format(adjustedDate);
	}

	// Build from array for unsupported locales (mg, ne)
	const dayIndex = adjustedDate.getUTCDay();
	if (short) {
		// Malagasy uses its own short names (three letters would give "Ala" twice); Devanagari (ne) uses the full name
		if (localeTag === 'mg') return MG_SHORT_WEEKDAYS[dayIndex];
		const name = weekdayNames[dayIndex];
		return localeTag === 'ne' ? name : name.substring(0, 3);
	}
	return weekdayNames[dayIndex];
}

export function formatDayMonth(timestamp: number, localeTag: string, utcOffsetSeconds: number, monthNames?: string[]): string {
	const adjustedDate = new Date(timestamp + utcOffsetSeconds * 1000);

	const intlSupported = Intl.DateTimeFormat.supportedLocalesOf([localeTag]).length > 0;

	if (intlSupported || !monthNames) {
		let formatter = dayMonthFormatters.get(localeTag);
		if (!formatter) {
			formatter = new Intl.DateTimeFormat(localeTag, { day: 'numeric', month: 'short', timeZone: 'UTC' });
			dayMonthFormatters.set(localeTag, formatter);
		}
		return formatter.format(adjustedDate);
	}

	// Build from array for unsupported locales (mg, ne)
	const day = adjustedDate.getUTCDate();
	const month = monthNames[adjustedDate.getUTCMonth()];

	// 3 letters for mg; Devanagari (ne) keeps the full name
	const shortMonth = localeTag === 'ne' ? month : month.substring(0, 3);
	return `${day} ${shortMonth}`;
}

export function formatAlertTime(timestamp: number, localeTag: string): string {
	let formatter = alertTimeFormatters.get(localeTag);
	if (!formatter) {
		formatter = new Intl.DateTimeFormat(localeTag, {
			year: 'numeric',
			month: '2-digit',
			day: '2-digit',
			hour: '2-digit',
			minute: '2-digit',
			hour12: false
		});
		alertTimeFormatters.set(localeTag, formatter);
	}
	return formatter.format(new Date(timestamp));
}
