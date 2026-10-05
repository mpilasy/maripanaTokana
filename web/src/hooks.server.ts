import { redirect } from '@sveltejs/kit';
import type { Handle, HandleServerError } from '@sveltejs/kit';

export const handle: Handle = async ({ event, resolve }) => {
	const path = event.url.pathname;
	if (path === '/svelte' || path === '/svelte/') {
		throw redirect(301, '/');
	}
	const response = await resolve(event);

	// Security headers
	response.headers.set('X-Content-Type-Options', 'nosniff');
	response.headers.set('Referrer-Policy', 'strict-origin-when-cross-origin');
	response.headers.set('Permissions-Policy', 'geolocation=(self), camera=(), microphone=()');
	response.headers.set('X-Frame-Options', 'DENY');

	return response;
};

export const handleError: HandleServerError = ({ event, error, status }) => {
	if (status !== 404) console.error(`Error at ${event.url.pathname}:`, error);
	return {
		message: 'Internal error'
	};
};
