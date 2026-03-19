export function formatTime(timestamp: number): string {
	const d = new Date(timestamp);
	const hh = String(d.getHours()).padStart(2, '0');
	const mm = String(d.getMinutes()).padStart(2, '0');
	return `${hh}:${mm}`;
}

export function formatDate(timestamp: number, localeTag: string): string {
	const d = new Date(timestamp);
	const formatter = new Intl.DateTimeFormat(localeTag, {
		weekday: 'long', day: 'numeric', month: 'long', year: 'numeric'
	});
	return formatter.format(d);
}

/**
 * Convert a timestamp that was parsed as device-local (from an API time string
 * in the location's timezone) back to the correct UTC instant, then return
 * what that instant looks like in the device's local timezone.
 *
 * @param locationLocalMillis  epoch millis obtained by parsing the bare ISO string
 * @param locationUtcOffsetSec the location's UTC offset returned by the API
 * @returns HH:MM in the device's local time
 */
export function formatHourInDeviceTime(locationLocalMillis: number, locationUtcOffsetSec: number): string {
	// The stored millis treat the API's local-time string as if it were device-local.
	// Device UTC offset in ms (getTimezoneOffset returns minutes, sign inverted).
	const deviceOffsetMs = new Date(locationLocalMillis).getTimezoneOffset() * -60_000;
	const locationOffsetMs = locationUtcOffsetSec * 1000;
	// targetEpoch = locationLocalMillis + (deviceOffset - locationOffset)
	const targetEpoch = locationLocalMillis + (deviceOffsetMs - locationOffsetMs);
	const d = new Date(targetEpoch);
	return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
}

/**
 * Get the current time at a location given its UTC offset.
 * @returns HH:MM in the location's local time
 */
export function formatLocationCurrentTime(utcOffsetSeconds: number, localeTag: string): string {
	const now = Date.now();
	const deviceOffsetMs = new Date().getTimezoneOffset() * -60_000;
	const utcMs = now - deviceOffsetMs;
	const locationMs = utcMs + utcOffsetSeconds * 1000;
	const d = new Date(locationMs);
	
	const deviceDateStr = new Date(now).toISOString().split('T')[0];
	const locationDateStr = new Date(locationMs).toISOString().split('T')[0];
	
	if (deviceDateStr !== locationDateStr) {
		const formatter = new Intl.DateTimeFormat(localeTag, {
			weekday: 'short', hour: '2-digit', minute: '2-digit', hour12: false
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
