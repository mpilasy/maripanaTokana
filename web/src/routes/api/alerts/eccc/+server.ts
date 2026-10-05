import { error } from '@sveltejs/kit';
import type { RequestHandler } from './$types';
import { proxyResponse } from '$lib/server/proxy';
import { USER_AGENT } from '$lib/api/alerts/shared';

export const GET: RequestHandler = async ({ url }) => {
	const bbox = url.searchParams.get('bbox');
	if (!bbox || !/^-?[\d.]+,-?[\d.]+,-?[\d.]+,-?[\d.]+$/.test(bbox)) {
		throw error(400, 'Missing or invalid bbox');
	}
	const upstream = `https://api.weather.gc.ca/collections/weather-alerts/items?bbox=${bbox}&f=json`;
	return proxyResponse(upstream, async () => {
		const res = await fetch(upstream, { headers: { 'User-Agent': USER_AGENT }, signal: AbortSignal.timeout(8_000) });
		if (!res.ok) throw new Error(`ECCC status ${res.status}`);
		return res.text();
	}, 'application/json', JSON.stringify({ features: [] }));
};
