// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

import type { RequestHandler } from './$types';

interface ThrottleWindow {
	startTime: number;
	count: number;
	suppressed: number;
}

let throttle: ThrottleWindow = { startTime: Date.now(), count: 0, suppressed: 0 };
const MAX_LOGS_PER_MINUTE = 20;
const THROTTLE_WINDOW_MS = 60000;

// Receives the CSP report-only violation reports (see csp in vite.config.ts) and logs one line for each,
// so that the policy can be checked before it is enforced.
export const POST: RequestHandler = async ({ request }) => {
	const contentLength = request.headers.get('content-length');
	if (contentLength && parseInt(contentLength) > 8192) {
		return new Response(null, { status: 413 });
	}

	try {
		const text = (await request.text()).slice(0, 4096);
		const report = JSON.parse(text)['csp-report'] ?? {};

		// At most MAX_LOGS_PER_MINUTE warnings a minute
		const now = Date.now();
		if (now - throttle.startTime >= THROTTLE_WINDOW_MS) {
			// A new minute: log how many reports were left out, and start again
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
		// A malformed report is ignored
	}
	return new Response(null, { status: 204 });
};
