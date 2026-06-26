import { error } from '@sveltejs/kit';
import type { RequestHandler } from './$types';

export const GET: RequestHandler = async ({ url }) => {
	const country = url.searchParams.get('country');
	if (!country || !/^[A-Z]{2}$/.test(country)) {
		throw error(400, 'Missing or invalid country code');
	}
	const res = await fetch(`https://severe.worldweather.wmo.int/json/${country}.json`);
	if (!res.ok) throw error(res.status, 'WMO SWIC fetch failed');
	const data = await res.json();
	return Response.json(data);
};
