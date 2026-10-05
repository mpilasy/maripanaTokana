import type { RequestHandler } from './$types';

// Receives Content-Security-Policy-Report-Only violation reports (see kit.csp in
// svelte.config.js). Logs a one-line summary so the policy can be verified before enforcing.
export const POST: RequestHandler = async ({ request }) => {
	try {
		const text = (await request.text()).slice(0, 4096);
		const report = JSON.parse(text)['csp-report'] ?? {};
		console.warn(`CSP report: ${report['violated-directive'] ?? '?'} blocked ${report['blocked-uri'] ?? '?'}`);
	} catch {
		// Ignore malformed reports
	}
	return new Response(null, { status: 204 });
};
