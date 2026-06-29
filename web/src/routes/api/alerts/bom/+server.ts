import https from 'node:https';
import type { RequestHandler } from './$types';

const USER_AGENT = 'maripanaTokana (contact@orinasa.mg)';

// BOM API has HTTP/2 issues (INTERNAL_ERROR). Use node:https to force HTTP/1.1.
function fetchBomJson(): Promise<unknown> {
	return new Promise((resolve, reject) => {
		const req = https.get(
			'https://api.weather.bom.gov.au/v1/warnings',
			{},
			(res) => {
				let raw = '';
				res.on('data', (chunk: string) => { raw += chunk; });
				res.on('end', () => {
					try { resolve(JSON.parse(raw)); }
					catch { resolve({ data: [] }); }
				});
			}
		);
		req.setTimeout(8000, () => req.destroy(new Error('BOM API timeout')));
		req.on('error', reject);
	});
}

export const GET: RequestHandler = async () => {
	try {
		const data = await fetchBomJson();
		return Response.json(data);
	} catch {
		return Response.json({ data: [] });
	}
};
