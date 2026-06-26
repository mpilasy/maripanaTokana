import { redirect } from '@sveltejs/kit';
import type { Handle } from '@sveltejs/kit';

export const handle: Handle = async ({ event, resolve }) => {
	const path = event.url.pathname;
	if (path === '/svelte' || path === '/svelte/') {
		throw redirect(301, '/');
	}
	return resolve(event);
};
