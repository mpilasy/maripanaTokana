import https from 'node:https';
import type { RequestHandler } from './$types';
import { proxyResponse } from '$lib/server/proxy';

const BOM_URL = 'https://api.weather.bom.gov.au/v1/warnings';

// BOM API has HTTP/2 issues (INTERNAL_ERROR). Use node:https to force HTTP/1.1.
function fetchBomJson(): Promise<string> {
	return new Promise((resolve, reject) => {
		const req = https.get(
			BOM_URL,
			{},
			(res) => {
				if (!res.statusCode || res.statusCode < 200 || res.statusCode >= 300) {
					res.resume();
					reject(new Error(`BOM API status ${res.statusCode}`));
					return;
				}
				res.setEncoding('utf8');
				let raw = '';
				res.on('data', (chunk: string) => { raw += chunk; });
				res.on('end', () => {
					try { JSON.parse(raw); resolve(raw); }
					catch { reject(new Error('BOM API invalid JSON')); }
				});
			}
		);
		req.setTimeout(8000, () => req.destroy(new Error('BOM API timeout')));
		req.on('error', reject);
	});
}

export const GET: RequestHandler = () =>
	proxyResponse(BOM_URL, fetchBomJson, 'application/json', JSON.stringify({ data: [] }));
