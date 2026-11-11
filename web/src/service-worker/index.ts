// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

import { self as sw } from '$app/service-worker';
import { version } from '$app/env';
import { immutable, assets } from '$app/manifest';
import { asset } from '$app/paths';
import type { AssetPath } from '$app/types';

const CACHE_APP = `app-${version}`;
const CACHE_API = 'api-cache';
const CACHE_API_MAX_ENTRIES = 50;

// Delete the oldest entries (keys() is in insertion order) above the limit
async function trimApiCache(cache: Cache) {
	const keys = await cache.keys();
	await Promise.all(keys.slice(0, Math.max(0, keys.length - CACHE_API_MAX_ENTRIES)).map((k) => cache.delete(k)));
}

// Manifest paths are relative to the base path; asset() makes them absolute pathnames.
const APP_ASSETS = new Set<string>([
	...immutable.map((file) => asset(file.path as AssetPath)),
	...assets.map((file) => asset(file.path))
]);

// Install: activate at once; no precache, NetworkFirst fills the cache
sw.addEventListener('install', () => {
	sw.skipWaiting();
});

// Activate: delete old caches and take control of all clients
sw.addEventListener('activate', (event) => {
	event.waitUntil(
		caches.keys().then((keys) =>
			Promise.all(
				keys
					.filter((key) => key !== CACHE_APP && key !== CACHE_API)
					.map((key) => caches.delete(key))
			)
		).then(() => sw.clients.claim())
	);
});

// Fetch: strategy per request type
sw.addEventListener('fetch', (event) => {
	const url = new URL(event.request.url);

	if (event.request.method !== 'GET') return;

	// API calls: NetworkFirst
	if (url.hostname === 'api.open-meteo.com' || url.hostname === 'air-quality-api.open-meteo.com' || url.hostname === 'nominatim.openstreetmap.org') {
		event.respondWith(
			fetch(event.request)
				.then((response) => {
					if (response.ok) {
						// Stamp the cache time so the client can tell replayed data from fresh data.
						const headers = new Headers(response.headers);
						headers.set('X-SW-Cached-At', String(Date.now()));
						const stamped = response.clone().arrayBuffer().then(
							(body) => new Response(body, { status: response.status, statusText: response.statusText, headers })
						);
						stamped.then((copy) => caches.open(CACHE_API).then((cache) => cache.put(event.request, copy).then(() => trimApiCache(cache)))).catch(() => {});
					}
					return response;
				})
				.catch(() => caches.match(event.request).then((r) => r || new Response('Offline', { status: 503 })))
		);
		return;
	}

	// Page navigations: NetworkFirst; the page is server-rendered so cache it under its URL and '/'
	if (url.origin === sw.location.origin && event.request.mode === 'navigate') {
		event.respondWith(
			fetch(event.request)
				.then((response) => {
					if (response.ok) {
						const clone = response.clone();
						const clone2 = response.clone();
						caches.open(CACHE_APP).then((cache) => {
							cache.put(event.request, clone);
							cache.put('/', clone2);
						});
					}
					return response;
				})
				.catch(() => caches.match(event.request).then((r) => r || caches.match('/')).then((r) => r || new Response('Offline', { status: 503 })))
		);
		return;
	}

	// App shell: NetworkFirst (use network, fall back to cache for offline)
	if (url.origin === sw.location.origin) {
		event.respondWith(
			fetch(event.request)
				.then((response) => {
					if (response.ok && APP_ASSETS.has(url.pathname)) {
						const clone = response.clone();
						caches.open(CACHE_APP).then((cache) => cache.put(event.request, clone));
					}
					return response;
				})
				.catch(() => caches.match(event.request).then((r) => r || new Response('Offline', { status: 503 })))
		);
	}
});
