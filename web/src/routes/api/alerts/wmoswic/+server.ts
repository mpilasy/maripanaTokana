import { error } from '@sveltejs/kit';
import https from 'node:https';
import type { RequestHandler } from './$types';

// WMO SWIC serves a cert for cyclone.wmo.int at severe.worldweather.wmo.int —
// hostname mismatch causes Node fetch to reject. Use node:https with the check
// disabled for this one host.
function fetchWmoJson(country: string): Promise<unknown> {
	return new Promise((resolve, reject) => {
		https.get(
			`https://severe.worldweather.wmo.int/json/${country}.json`,
			{ rejectUnauthorized: false },
			(res) => {
				let raw = '';
				res.on('data', (chunk: string) => { raw += chunk; });
				res.on('end', () => {
					try { resolve(JSON.parse(raw)); }
					catch { resolve({ Warning: [] }); }
				});
			}
		).on('error', reject);
	});
}

export const GET: RequestHandler = async ({ url }) => {
	const country = url.searchParams.get('country');
	if (!country || !/^[A-Z]{2}$/.test(country)) {
		throw error(400, 'Missing or invalid country code');
	}
	try {
		const data = await fetchWmoJson(country);
		return Response.json(data);
	} catch {
		return Response.json({ Warning: [] });
	}
};
