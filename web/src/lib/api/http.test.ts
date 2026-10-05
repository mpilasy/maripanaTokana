import { describe, expect, it, vi } from 'vitest';
import { HttpError, NetworkError, classifyError, netFetch, retryOnce } from './http';

describe('classifyError', () => {
	it('maps HTTP statuses', () => {
		expect(classifyError(new HttpError(429))).toBe('error_rate_limited');
		expect(classifyError(new HttpError(503))).toBe('error_server');
		expect(classifyError(new HttpError(404))).toBe('error_fetch_weather');
		expect(classifyError(new HttpError(401))).toBe('error_fetch_weather');
		expect(classifyError(new HttpError(401), [401, 403])).toBe('error_invalid_api_key');
	});

	it('maps network and timeout errors', () => {
		expect(classifyError(new NetworkError())).toBe('error_offline');
		expect(classifyError(new DOMException('t', 'TimeoutError'))).toBe('error_timeout');
		expect(classifyError(new DOMException('a', 'AbortError'))).toBe('error_timeout');
		expect(classifyError(new Error('x'))).toBe('error_fetch_weather');
	});

	it('does not treat parse/mapping TypeErrors as offline', () => {
		expect(classifyError(new TypeError("Cannot read properties of undefined (reading 'x')"))).toBe('error_fetch_weather');
	});
});

describe('netFetch', () => {
	it('wraps fetch TypeErrors in NetworkError but passes timeouts through', async () => {
		const orig = globalThis.fetch;
		try {
			globalThis.fetch = vi.fn().mockRejectedValue(new TypeError('Failed to fetch'));
			await expect(netFetch('http://x')).rejects.toBeInstanceOf(NetworkError);
			globalThis.fetch = vi.fn().mockRejectedValue(new DOMException('t', 'TimeoutError'));
			await expect(netFetch('http://x')).rejects.toMatchObject({ name: 'TimeoutError' });
		} finally {
			globalThis.fetch = orig;
		}
	});
});

describe('retryOnce', () => {
	it('retries once on 5xx then succeeds', async () => {
		const fn = vi.fn().mockRejectedValueOnce(new HttpError(500)).mockResolvedValueOnce('ok');
		expect(await retryOnce(fn, 0)).toBe('ok');
		expect(fn).toHaveBeenCalledTimes(2);
	});

	it('retries once on NetworkError and gives up after one retry', async () => {
		const fn = vi.fn().mockRejectedValue(new NetworkError());
		await expect(retryOnce(fn, 0)).rejects.toBeInstanceOf(NetworkError);
		expect(fn).toHaveBeenCalledTimes(2);
	});

	it('does not retry parse TypeErrors', async () => {
		const fn = vi.fn().mockRejectedValue(new TypeError('bad shape'));
		await expect(retryOnce(fn, 0)).rejects.toBeInstanceOf(TypeError);
		expect(fn).toHaveBeenCalledTimes(1);
	});

	it('does not retry 4xx or timeouts', async () => {
		const a = vi.fn().mockRejectedValue(new HttpError(429));
		await expect(retryOnce(a, 0)).rejects.toBeInstanceOf(HttpError);
		expect(a).toHaveBeenCalledTimes(1);
		const b = vi.fn().mockRejectedValue(new DOMException('t', 'TimeoutError'));
		await expect(retryOnce(b, 0)).rejects.toBeDefined();
		expect(b).toHaveBeenCalledTimes(1);
	});
});
