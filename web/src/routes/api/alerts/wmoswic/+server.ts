import { error } from '@sveltejs/kit';
import type { RequestHandler } from './$types';

export const GET: RequestHandler = async ({ url }) => {
	const country = url.searchParams.get('country');
	if (!country || !/^[A-Z]{2}$/.test(country)) {
		throw error(400, 'Missing or invalid country code');
	}
	try {
		const res = await fetch(`https://severe.worldweather.wmo.int/json/${country}.json`);
		if (!res.ok) return Response.json({ Warning: [] });
		const data = await res.json();
		return Response.json(data);
	} catch {
		// WMO SWIC has an SSL certificate mismatch (cert for cyclone.wmo.int served at
		// severe.worldweather.wmo.int). Return empty rather than propagating a 500.
		return Response.json({ Warning: [] });
	}
};
