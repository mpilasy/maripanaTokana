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
