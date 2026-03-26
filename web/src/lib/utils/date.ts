const formatterCache = new Map<string, Intl.DateTimeFormat>();

export function getFormatter(localeTag: string, options: Intl.DateTimeFormatOptions): Intl.DateTimeFormat {
	const key = `${localeTag}-${JSON.stringify(options)}`;
	let formatter = formatterCache.get(key);
	if (!formatter) {
		formatter = new Intl.DateTimeFormat(localeTag, options);
		formatterCache.set(key, formatter);
	}
	return formatter;
}

export function formatTime(timestamp: number): string {
	const d = new Date(timestamp);
	const hh = String(d.getHours()).padStart(2, '0');
	const mm = String(d.getMinutes()).padStart(2, '0');
	return `${hh}:${mm}`;
}

export function formatDate(timestamp: number, localeTag: string): string {
	const d = new Date(timestamp);
	const formatter = getFormatter(localeTag, {
		weekday: 'long', day: 'numeric', month: 'long', year: 'numeric'
	});
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

/**
 * Get the current time at a location given its UTC offset.
 */
export function formatLocationCurrentTime(utcOffsetSeconds: number, localeTag: string): string {
	const now = Date.now();
	const locationMs = now + utcOffsetSeconds * 1000;
	const d = new Date(locationMs);
	
	// Compare local dates using locale-aware strings
	const deviceDateStr = new Date(now).toLocaleDateString('en-US');
	const locationDateStr = new Date(locationMs).toLocaleDateString('en-US', { timeZone: 'UTC' });
	
	if (deviceDateStr !== locationDateStr) {
		const formatter = getFormatter(localeTag, {
			weekday: 'short', hour: '2-digit', minute: '2-digit', hour12: false, timeZone: 'UTC'
		});
		return formatter.format(d);
	}
	
	const hh = String(d.getUTCHours()).padStart(2, '0');
	const mm = String(d.getUTCMinutes()).padStart(2, '0');
	return `${hh}:${mm}`;
}

/**
 * Check if the location's timezone differs from the device's timezone.
 */
export function isRemoteTimezone(utcOffsetSeconds: number): boolean {
	const deviceOffsetSeconds = new Date().getTimezoneOffset() * -60;
	return deviceOffsetSeconds !== utcOffsetSeconds;
}
