/** Format a timestamp to "EEEE, d MMMM yyyy" in the given locale tag. */
export function formatFullDate(timestamp: number, localeTag: string): string {
  const date = new Date(timestamp);
  try {
    return date.toLocaleDateString(localeTag, {
      weekday: 'long',
      day: 'numeric',
      month: 'long',
      year: 'numeric',
    });
  } catch {
    return date.toLocaleDateString('en', {
      weekday: 'long',
      day: 'numeric',
      month: 'long',
      year: 'numeric',
    });
  }
}

/** Format a timestamp to "HH:mm" or "h:mm AM/PM". */
export function formatTime(timestamp: number): string {
  const date = new Date(timestamp);
  const hours = date.getHours().toString().padStart(2, '0');
  const minutes = date.getMinutes().toString().padStart(2, '0');
  return `${hours}:${minutes}`;
}

/** Format epoch seconds to "HH:mm". */
export function formatTimeFromEpochSeconds(epochSeconds: number): string {
  return formatTime(epochSeconds * 1000);
}

/** Format a timestamp to day name (e.g., "Monday"). */
export function formatDayName(timestamp: number, localeTag: string): string {
  const date = new Date(timestamp);
  try {
    return date.toLocaleDateString(localeTag, {weekday: 'long'});
  } catch {
    return date.toLocaleDateString('en', {weekday: 'long'});
  }
}

/** Format a timestamp to "d MMM" (e.g., "5 Jan"). */
export function formatDayMonth(timestamp: number, localeTag: string): string {
  const date = new Date(timestamp);
  try {
    return date.toLocaleDateString(localeTag, {day: 'numeric', month: 'short'});
  } catch {
    return date.toLocaleDateString('en', {day: 'numeric', month: 'short'});
  }
}

/** Format epoch millis to "HH:mm". */
export function formatHourMinute(epochMillis: number): string {
  return formatTime(epochMillis);
}
