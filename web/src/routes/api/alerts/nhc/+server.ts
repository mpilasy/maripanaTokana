import type { RequestHandler } from './$types';

export const GET: RequestHandler = async () => {
	try {
		const res = await fetch('https://www.nhc.noaa.gov/CurrentStorms.json', { signal: AbortSignal.timeout(8_000) });
		if (!res.ok) return Response.json({ activeStorms: [] }, { status: 502 });
		const data = await res.json();
		return Response.json(data);
	} catch {
		return Response.json({ activeStorms: [] }, { status: 502 });
	}
};
