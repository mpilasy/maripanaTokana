/// <reference types="@sveltejs/kit" />
/// <reference no-default-lib="true"/>
/// <reference lib="esnext" />
/// <reference lib="webworker" />

const sw = self as unknown as ServiceWorkerGlobalScope;

// Install: activate immediately
sw.addEventListener('install', () => {
	sw.skipWaiting();
});

// Activate: delete ALL caches, claim clients
sw.addEventListener('activate', (event) => {
	event.waitUntil(
		caches.keys().then((keys) =>
			Promise.all(keys.map((key) => caches.delete(key)))
		).then(() => sw.clients.claim())
	);
});

// Fetch: pass everything straight through to network (no caching)
sw.addEventListener('fetch', () => {
	// Do nothing — let the browser handle all requests normally
});
