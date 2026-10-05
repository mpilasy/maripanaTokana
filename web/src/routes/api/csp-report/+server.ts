import type { RequestHandler } from './$types';

interface ThrottleWindow {
	startTime: number;
	count: number;
	suppressed: number;
}

let throttle: ThrottleWindow = { startTime: Date.now(), count: 0, suppressed: 0 };
const MAX_LOGS_PER_MINUTE = 20;
const THROTTLE_WINDOW_MS = 60000;

// Receives Content-Security-Policy-Report-Only violation reports (see kit.csp in
// svelte.config.js). Logs a one-line summary so the policy can be verified before enforcing.
export const POST: RequestHandler = async ({ request }) => {
	// Check content-length header and reject if > 8 KB
	const contentLength = request.headers.get('content-length');
	if (contentLength && parseInt(contentLength) > 8192) {
		return new Response(null, { status: 413 });
	}

	try {
		const text = (await request.text()).slice(0, 4096);
		const report = JSON.parse(text)['csp-report'] ?? {};

		// Throttle console.warn to max 20 per minute
		const now = Date.now();
		if (now - throttle.startTime >= THROTTLE_WINDOW_MS) {
			// Window expired, log suppressed count if any and reset
			if (throttle.suppressed > 0) {
				console.warn(`CSP report: ${throttle.suppressed} reports suppressed`);
			}
			throttle = { startTime: now, count: 0, suppressed: 0 };
		}

		if (throttle.count < MAX_LOGS_PER_MINUTE) {
			throttle.count++;
			console.warn(`CSP report: ${report['violated-directive'] ?? '?'} blocked ${report['blocked-uri'] ?? '?'}`);
		} else {
			throttle.suppressed++;
		}
	} catch {
		// Ignore malformed reports
	}
	return new Response(null, { status: 204 });
};
