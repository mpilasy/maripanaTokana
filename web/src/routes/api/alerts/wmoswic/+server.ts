import { error } from '@sveltejs/kit';
import https from 'node:https';
import tls from 'node:tls';
import type { RequestHandler } from './$types';

// WMO SWIC serves a cert for cyclone.wmo.int at severe.worldweather.wmo.int —
// hostname mismatch causes Node fetch to reject. Use node:https and validate the
// chain normally, but check the identity against the name the cert is actually issued for.
function fetchWmoJson(country: string): Promise<unknown> {
	return new Promise((resolve, reject) => {
		const req = https.get(
			`https://severe.worldweather.wmo.int/json/${country}.json`,
			{
				rejectUnauthorized: true,
				checkServerIdentity: (_host, cert) => tls.checkServerIdentity('cyclone.wmo.int', cert)
			},
			(res) => {
				if (!res.statusCode || res.statusCode < 200 || res.statusCode >= 300) {
					res.resume();
					reject(new Error(`WMO SWIC status ${res.statusCode}`));
					return;
				}
				res.setEncoding('utf8');
				let raw = '';
				res.on('data', (chunk: string) => { raw += chunk; });
				res.on('end', () => {
					try { resolve(JSON.parse(raw)); }
					catch { resolve({ Warning: [] }); }
				});
			}
		);
		req.setTimeout(8000, () => req.destroy(new Error('WMO SWIC timeout')));
		req.on('error', reject);
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
