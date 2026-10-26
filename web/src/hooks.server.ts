// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

import { redirect } from '@sveltejs/kit';
import type { Handle, HandleServerError } from '@sveltejs/kit/hooks';

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

// SvelteKit 3 sends all errors here. Log and mask only unexpected errors.
// Errors from error(...) and from SvelteKit (such as 404) keep their status and message.
export const handleError: HandleServerError = ({ event, error, kind }) => {
	if (kind !== 'unknown') return;
	console.error(`Error at ${event.url.pathname}:`, error);
	return {
		message: 'Internal error'
	};
};
