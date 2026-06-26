import { error } from '@sveltejs/kit';
import type { RequestHandler } from './$types';

export const GET: RequestHandler = async () => {
	const res = await fetch('https://www.nhc.noaa.gov/CurrentStorms.json');
	if (!res.ok) throw error(res.status, 'NHC fetch failed');
	const data = await res.json();
	return Response.json(data);
};
