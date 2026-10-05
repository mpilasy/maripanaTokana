const MAX_ENTRIES = 200;

const store = new Map<string, { expires: number; value: unknown }>();
const inflight = new Map<string, Promise<unknown>>();

/** In-memory TTL cache with in-flight coalescing. Failures are not cached. */
export function cached<T>(key: string, ttlMs: number, loader: () => Promise<T>): Promise<T> {
	const hit = store.get(key);
	if (hit && hit.expires > Date.now()) return Promise.resolve(hit.value as T);
	if (hit) store.delete(key);

	const pending = inflight.get(key);
	if (pending) return pending as Promise<T>;

	const p = loader().then(
		(value) => {
			inflight.delete(key);
			store.set(key, { expires: Date.now() + ttlMs, value });
			while (store.size > MAX_ENTRIES) {
				store.delete(store.keys().next().value as string);
			}
			return value;
		},
		(err) => {
			inflight.delete(key);
			throw err;
		}
	);
	inflight.set(key, p);
	return p;
}

/** Test helper. */
export function clearCache() {
	store.clear();
	inflight.clear();
}
