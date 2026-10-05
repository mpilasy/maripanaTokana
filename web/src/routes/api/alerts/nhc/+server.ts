import type { RequestHandler } from './$types';
import { proxyResponse } from '$lib/server/proxy';

const NHC_URL = 'https://www.nhc.noaa.gov/CurrentStorms.json';

export const GET: RequestHandler = () =>
	proxyResponse(NHC_URL, async () => {
		const res = await fetch(NHC_URL, { signal: AbortSignal.timeout(8_000) });
		if (!res.ok) throw new Error(`NHC status ${res.status}`);
		return res.text();
	}, 'application/json', JSON.stringify({ activeStorms: [] }));
