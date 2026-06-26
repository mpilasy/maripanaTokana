import { error } from '@sveltejs/kit';
import type { RequestHandler } from './$types';

const USER_AGENT = 'maripanaTokana (contact@orinasa.mg)';

export const GET: RequestHandler = async ({ url }) => {
	const country = url.searchParams.get('country');
	if (!country || !/^[a-z]{2}$/.test(country)) {
		throw error(400, 'Missing or invalid country code');
	}
	const res = await fetch(
		`https://feeds.meteoalarm.org/feeds/meteoalarm-legacy-atom-${country}`,
		{ headers: { 'User-Agent': USER_AGENT } }
	);
	if (!res.ok) throw error(res.status, 'MeteoAlarm fetch failed');
	const text = await res.text();
	return new Response(text, {
		headers: { 'Content-Type': 'application/xml' }
	});
};
