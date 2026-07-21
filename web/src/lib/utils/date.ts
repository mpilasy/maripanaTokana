export function formatTime(timestamp: number): string {
	const d = new Date(timestamp);
	const hh = String(d.getHours()).padStart(2, '0');
	const mm = String(d.getMinutes()).padStart(2, '0');
	return `${hh}:${mm}`;
}

// Cache for date formatters to avoid expensive instantiations
const dateFormatters = new Map<string, Intl.DateTimeFormat>();

export function formatDate(timestamp: number, localeTag: string): string {
	const d = new Date(timestamp);
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

/**
 * Format an epoch (UTC millis) to HH:mm in the device's local timezone.
 */
export function formatHourInDeviceTime(epochMillis: number): string {
	const d = new Date(epochMillis);
	return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
}

/**
 * Format an epoch (UTC millis) to HH:mm in the location's timezone using UTC offset.
 */
export function formatHourAtLocation(epochMillis: number, utcOffsetSec: number): string {
	// Add offset to get location's local "millisecond instant"
	const locationLocalMs = epochMillis + utcOffsetSec * 1000;
	const d = new Date(locationLocalMs);
	// Use UTC methods because we manually applied the offset to the epoch
	return `${String(d.getUTCHours()).padStart(2, '0')}:${String(d.getUTCMinutes()).padStart(2, '0')}`;
}

// Cache for location time formatters to avoid expensive instantiations
const locationTimeFormatters = new Map<string, Intl.DateTimeFormat>();

/**
 * Get the current time at a location given its UTC offset.
 */
export function formatLocationCurrentTime(utcOffsetSeconds: number, localeTag: string): string {
	const now = Date.now();
	const locationMs = now + utcOffsetSeconds * 1000;
	const dLocation = new Date(locationMs);
	const dDevice = new Date(now);
	
	// Compare dates using fast numeric checks instead of slow toLocaleDateString
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

/**
 * Check if the location's timezone differs from the device's timezone.
 */
export function isRemoteTimezone(utcOffsetSeconds: number): boolean {
	const deviceOffsetSeconds = new Date().getTimezoneOffset() * -60;
	return deviceOffsetSeconds !== utcOffsetSeconds;
}

const dayNameFormatters = new Map<string, Intl.DateTimeFormat>();
const dayMonthFormatters = new Map<string, Intl.DateTimeFormat>();
const alertTimeFormatters = new Map<string, Intl.DateTimeFormat>();

/**
 * Day names must use the forecast location's timezone, not the browser's — `timestamp` is an
 * absolute instant representing local midnight AT THE LOCATION, so formatting it in the
 * browser's default timezone can shift the displayed calendar day when the two zones differ
 * (e.g. a dev-mode location override far from the browser's real timezone). We shift the
 * timestamp by the location's offset and format with timeZone: 'UTC' so the result is anchored
 * to the location's wall-clock date regardless of where the browser is.
 */
export function formatDayName(timestamp: number, localeTag: string, utcOffsetSeconds: number, short = false): string {
	const key = short ? `${localeTag}:short` : localeTag;
	let formatter = dayNameFormatters.get(key);
	if (!formatter) {
		formatter = new Intl.DateTimeFormat(localeTag, { weekday: short ? 'short' : 'long', timeZone: 'UTC' });
		dayNameFormatters.set(key, formatter);
	}
	return formatter.format(new Date(timestamp + utcOffsetSeconds * 1000));
}

export function formatDayMonth(timestamp: number, localeTag: string, utcOffsetSeconds: number): string {
	let formatter = dayMonthFormatters.get(localeTag);
	if (!formatter) {
		formatter = new Intl.DateTimeFormat(localeTag, { day: 'numeric', month: 'short', timeZone: 'UTC' });
		dayMonthFormatters.set(localeTag, formatter);
	}
	return formatter.format(new Date(timestamp + utcOffsetSeconds * 1000));
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
