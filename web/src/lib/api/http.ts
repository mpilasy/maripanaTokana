export const FETCH_TIMEOUT_MS = 10_000;

export class HttpError extends Error {
	constructor(readonly status: number, message?: string) {
		super(message ?? `HTTP ${status}`);
		this.name = 'HttpError';
	}
}

/** Run `fn` once more after `delayMs` on a network failure or 5xx. Not for 4xx or timeouts. */
export async function retryOnce<T>(fn: () => Promise<T>, delayMs = 1000): Promise<T> {
	try {
		return await fn();
	} catch (err) {
		const transient = err instanceof TypeError || (err instanceof HttpError && err.status >= 500);
		if (!transient) throw err;
		await new Promise((resolve) => setTimeout(resolve, delayMs));
		return fn();
	}
}

/** Map a thrown error to an i18n message key. `authStatuses` are HTTP codes meaning a bad API key. */
export function classifyError(err: unknown, authStatuses: number[] = []): string {
	if (typeof GeolocationPositionError !== 'undefined' && err instanceof GeolocationPositionError) {
		return 'error_get_location';
	}
	const name = (err as { name?: string } | null)?.name;
	if (name === 'TimeoutError' || name === 'AbortError') return 'error_timeout';
	if (err instanceof HttpError) {
		if (err.status === 429) return 'error_rate_limited';
		if (authStatuses.includes(err.status)) return 'error_invalid_api_key';
		if (err.status >= 500) return 'error_server';
		return 'error_fetch_weather';
	}
	if (err instanceof TypeError || (typeof navigator !== 'undefined' && navigator.onLine === false)) {
		return 'error_offline';
	}
	return 'error_fetch_weather';
}
