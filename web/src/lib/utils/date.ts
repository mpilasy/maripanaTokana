// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

let clock24: boolean | null = null;

/** True when the browser's region uses a 24-hour clock. Found once, then kept. As Android's uses24HourClock(). */
export function uses24HourClock(): boolean {
	if (clock24 === null) {
		const cycle = new Intl.DateTimeFormat(undefined, { hour: 'numeric' }).resolvedOptions().hourCycle;
		clock24 = cycle === 'h23' || cycle === 'h24';
	}
	return clock24;
}

/** Sets the clock for tests; null finds it again from the browser. */
export function setUses24HourClock(value: boolean | null): void {
	clock24 = value;
}

const amPmMarkers = new Map<string, [string, string]>();

/**
 * The AM or PM marker of [hours] in the app language (English when Intl does not have the language). No dots or
 * spaces ("p. m." becomes "pm"): the digit helper changes dots to decimal commas. As Android's clockFormat().
 */
function amPm(hours: number, localeTag: string): string {
	let markers = amPmMarkers.get(localeTag);
	if (!markers) {
		const tag = Intl.DateTimeFormat.supportedLocalesOf([localeTag]).length > 0 ? localeTag : 'en';
		const formatter = new Intl.DateTimeFormat(tag, { hour: 'numeric', hourCycle: 'h12', timeZone: 'UTC' });
		const marker = (h: number, fallback: string) =>
			cleanDayPeriod(formatter.formatToParts(Date.UTC(2026, 0, 1, h)).find((p) => p.type === 'dayPeriod')?.value ?? fallback);
		markers = [marker(9, 'AM'), marker(21, 'PM')];
		amPmMarkers.set(localeTag, markers);
	}
	return markers[hours < 12 ? 0 : 1];
}

function cleanDayPeriod(value: string): string {
	return value.replace(/[.\s]/g, '');
}

/** An Intl result with the AM/PM marker cleaned as in amPm(). */
function formatWithCleanDayPeriod(formatter: Intl.DateTimeFormat, date: Date): string {
	return formatter.formatToParts(date).map((p) => (p.type === 'dayPeriod' ? cleanDayPeriod(p.value) : p.value)).join('');
}

/** A time with minutes: "HH:mm" (24-hour) or "h:mm a" (12-hour, "9:05 PM"). As Android's clockPattern(). */
function clockText(hours: number, minutes: number, localeTag: string): string {
	const mm = String(minutes).padStart(2, '0');
	if (uses24HourClock()) return `${String(hours).padStart(2, '0')}:${mm}`;
	return `${hours % 12 || 12}:${mm} ${amPm(hours, localeTag)}`;
}

/**
 * The label of a full hour: "HH:mm" (24-hour) or "h a" (12-hour, "9 PM"). [wholeHour] is false for times that are
 * not on the full hour (a place or a device at a :30 or :45 offset): they keep the minutes. As Android's hourLabelPattern().
 */
function hourLabelText(hours: number, minutes: number, wholeHour: boolean, localeTag: string): string {
	if (uses24HourClock() || !wholeHour) return clockText(hours, minutes, localeTag);
	return `${hours % 12 || 12} ${amPm(hours, localeTag)}`;
}

/** A time with minutes in the device's timezone. */
export function formatTime(timestamp: number, localeTag: string): string {
	const d = new Date(timestamp);
	return clockText(d.getHours(), d.getMinutes(), localeTag);
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
		const uses24 = uses24HourClock();
		const key = `${localeTag}:${uses24}`;
		let formatter = dateFormatters.get(key);
		if (!formatter) {
			formatter = new Intl.DateTimeFormat(localeTag, {
				weekday: 'long', day: 'numeric', month: 'long', year: 'numeric',
				hour: 'numeric', minute: '2-digit', hourCycle: uses24 ? 'h23' : 'h12'
			});
			dateFormatters.set(key, formatter);
		}
		return formatWithCleanDayPeriod(formatter, d);
	}

	// Build from arrays for unsupported locales (mg, ne)
	const weekday = weekdayNames[d.getDay()];
	const month = monthNames[d.getMonth()];
	const day = d.getDate();
	const year = d.getFullYear();

	return `${weekday}, ${day} ${month} ${year} ${clockText(d.getHours(), d.getMinutes(), localeTag)}`;
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

/** An epoch (UTC millis) as an hour label in the device's timezone: the minutes stay when the device is at a :30 or :45 offset. */
export function formatHourInDeviceTime(epochMillis: number, localeTag: string): string {
	const d = new Date(epochMillis);
	return hourLabelText(d.getHours(), d.getMinutes(), d.getMinutes() === 0, localeTag);
}

/** An epoch (UTC millis) as an hour label at the location, from its UTC offset ("21:00" or "9 PM"). */
export function formatHourAtLocation(epochMillis: number, utcOffsetSec: number, localeTag: string): string {
	// UTC methods, because the offset is already in the time
	const d = new Date(epochMillis + utcOffsetSec * 1000);
	return hourLabelText(d.getUTCHours(), d.getUTCMinutes(), utcOffsetSec % 3600 === 0, localeTag);
}

/** An epoch (UTC millis) as a time with minutes at the location, from its UTC offset ("21:05" or "9:05 PM"). */
export function formatTimeAtLocation(epochMillis: number, utcOffsetSec: number, localeTag: string): string {
	// UTC methods, because the offset is already in the time
	const d = new Date(epochMillis + utcOffsetSec * 1000);
	return clockText(d.getUTCHours(), d.getUTCMinutes(), localeTag);
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
		const uses24 = uses24HourClock();
		const key = `${localeTag}:${uses24}`;
		let formatter = locationTimeFormatters.get(key);
		if (!formatter) {
			formatter = new Intl.DateTimeFormat(localeTag, {
				weekday: 'short', hour: uses24 ? '2-digit' : 'numeric', minute: '2-digit', hourCycle: uses24 ? 'h23' : 'h12', timeZone: 'UTC'
			});
			locationTimeFormatters.set(key, formatter);
		}
		return formatWithCleanDayPeriod(formatter, dLocation);
	}

	return clockText(dLocation.getUTCHours(), dLocation.getUTCMinutes(), localeTag);
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

/** Day, month, year and time (24-hour or 12-hour as the browser's region) in the app language's order. */
export function formatAlertTime(timestamp: number, localeTag: string): string {
	const uses24 = uses24HourClock();
	const key = `${localeTag}:${uses24}`;
	let formatter = alertTimeFormatters.get(key);
	if (!formatter) {
		formatter = new Intl.DateTimeFormat(localeTag, {
			year: 'numeric',
			month: '2-digit',
			day: '2-digit',
			hour: '2-digit',
			minute: '2-digit',
			hourCycle: uses24 ? 'h23' : 'h12'
		});
		alertTimeFormatters.set(key, formatter);
	}
	return formatWithCleanDayPeriod(formatter, new Date(timestamp));
}
