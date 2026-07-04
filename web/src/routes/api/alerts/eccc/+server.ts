import { error } from '@sveltejs/kit';
import type { RequestHandler } from './$types';
import { USER_AGENT } from '$lib/api/alerts/shared';

export const GET: RequestHandler = async ({ url }) => {
	const bbox = url.searchParams.get('bbox');
	if (!bbox || !/^-?[\d.]+,-?[\d.]+,-?[\d.]+,-?[\d.]+$/.test(bbox)) {
		throw error(400, 'Missing or invalid bbox');
	}
	try {
		const res = await fetch(
			`https://api.weather.gc.ca/collections/alerts/items?bbox=${bbox}&f=json`,
			{ headers: { 'User-Agent': USER_AGENT } }
		);
		if (!res.ok) return Response.json({ features: [] });
		const data = await res.json();
		return Response.json(data);
	} catch {
		return Response.json({ features: [] });
	}
};
