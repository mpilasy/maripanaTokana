import { error } from '@sveltejs/kit';
import type { RequestHandler } from './$types';

const USER_AGENT = 'maripanaTokana (contact@orinasa.mg)';

export const GET: RequestHandler = async () => {
	const res = await fetch('https://api.weather.bom.gov.au/v1/warnings', {
		headers: { 'User-Agent': USER_AGENT }
	});
	if (!res.ok) throw error(res.status, 'BOM fetch failed');
	const data = await res.json();
	return Response.json(data);
};
