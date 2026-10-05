import { cached } from './ttlCache';

export const PROXY_TTL_MS = 5 * 60 * 1000;

const OK_HEADERS = { 'Cache-Control': 'public, max-age=300' };
const FAIL_HEADERS = { 'Cache-Control': 'no-store' };

/** Cache `loader` by upstream URL; success -> 200 + cache header, any failure -> 502 + empty payload. */
export async function proxyResponse(
	key: string,
	loader: () => Promise<string>,
	contentType: string,
	emptyBody: string
): Promise<Response> {
	try {
		const body = await cached(key, PROXY_TTL_MS, loader);
		return new Response(body, { headers: { 'Content-Type': contentType, ...OK_HEADERS } });
	} catch {
		return new Response(emptyBody, { status: 502, headers: { 'Content-Type': contentType, ...FAIL_HEADERS } });
	}
}
