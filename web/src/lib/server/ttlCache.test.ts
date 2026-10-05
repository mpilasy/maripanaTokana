import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { cached, clearCache } from './ttlCache';

describe('ttlCache', () => {
	beforeEach(() => { clearCache(); vi.useFakeTimers(); });
	afterEach(() => vi.useRealTimers());

	it('hits within TTL, misses after', async () => {
		const loader = vi.fn(async () => 'v');
		await cached('k', 1000, loader);
		vi.advanceTimersByTime(999);
		await cached('k', 1000, loader);
		expect(loader).toHaveBeenCalledTimes(1);
		vi.advanceTimersByTime(2);
		await cached('k', 1000, loader);
		expect(loader).toHaveBeenCalledTimes(2);
	});

	it('coalesces concurrent callers', async () => {
		const loader = vi.fn(async () => 'v');
		const [a, b] = await Promise.all([cached('k', 1000, loader), cached('k', 1000, loader)]);
		expect(a).toBe('v');
		expect(b).toBe('v');
		expect(loader).toHaveBeenCalledTimes(1);
	});

	it('does not cache failures', async () => {
		const loader = vi.fn().mockRejectedValueOnce(new Error('x')).mockResolvedValueOnce('ok');
		await expect(cached('k', 1000, loader)).rejects.toThrow('x');
		await expect(cached('k', 1000, loader)).resolves.toBe('ok');
		expect(loader).toHaveBeenCalledTimes(2);
	});
});
