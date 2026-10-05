import { error } from '@sveltejs/kit';
import type { RequestHandler } from './$types';
import { proxyResponse } from '$lib/server/proxy';
import { USER_AGENT } from '$lib/api/alerts/shared';

const COUNTRY_SLUGS: Record<string, string> = {
	at: 'austria', ba: 'bosnia-herzegovina', be: 'belgium', bg: 'bulgaria',
	hr: 'croatia', cy: 'cyprus', cz: 'czechia', dk: 'denmark', ee: 'estonia',
	fi: 'finland', fr: 'france', de: 'germany', gr: 'greece', hu: 'hungary',
	ie: 'ireland', it: 'italy', lv: 'latvia', lt: 'lithuania', lu: 'luxembourg',
	mt: 'malta', md: 'moldova', me: 'montenegro', nl: 'netherlands',
	mk: 'republic-of-north-macedonia', no: 'norway', pl: 'poland', pt: 'portugal',
	ro: 'romania', rs: 'serbia', sk: 'slovakia', si: 'slovenia', es: 'spain',
	se: 'sweden', ch: 'switzerland', ua: 'ukraine', gb: 'united-kingdom',
};

export const GET: RequestHandler = async ({ url }) => {
	const country = url.searchParams.get('country');
	if (!country || !/^[a-z]{2}$/.test(country)) {
		throw error(400, 'Missing or invalid country code');
	}
	const slug = COUNTRY_SLUGS[country];
	if (!slug) throw error(404, 'No MeteoAlarm feed for this country');
	const upstream = `https://feeds.meteoalarm.org/feeds/meteoalarm-legacy-atom-${slug}`;
	return proxyResponse(upstream, async () => {
		const res = await fetch(upstream, { headers: { 'User-Agent': USER_AGENT }, signal: AbortSignal.timeout(8_000) });
		if (!res.ok) throw new Error(`MeteoAlarm status ${res.status}`);
		return res.text();
	}, 'application/xml', '');
};
