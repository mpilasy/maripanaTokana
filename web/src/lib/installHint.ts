// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

import { writable } from 'svelte/store';

/** The install prompt that Chromium browsers (Chrome, Edge, Samsung Internet) give to an installable web app. */
export interface BeforeInstallPromptEvent extends Event {
	prompt(): Promise<void>;
	userChoice: Promise<{ outcome: 'accepted' | 'dismissed' }>;
}

/** The browser's install prompt, when the browser gave one. Null in Safari and Firefox, and after the install. */
export const installPrompt = writable<BeforeInstallPromptEvent | null>(null);

/** Keeps the install prompt for the "Add to Home screen" badge. The browser sends it once, soon after the page loads. */
export function listenForInstallPrompt(): void {
	if (typeof window === 'undefined') return;
	window.addEventListener('beforeinstallprompt', (e) => {
		e.preventDefault();
		installPrompt.set(e as BeforeInstallPromptEvent);
	});
	window.addEventListener('appinstalled', () => installPrompt.set(null));
}

/** True on iPhone and iPad. iPadOS reports itself as a Mac with a touch screen. */
export function isIos(): boolean {
	if (typeof navigator === 'undefined') return false;
	const ua = navigator.userAgent;
	return /iPhone|iPad|iPod/.test(ua) || (/Macintosh/.test(ua) && navigator.maxTouchPoints > 1);
}

/** True when the app runs from the Home Screen (installed), not in a browser tab. */
export function isStandalone(): boolean {
	if (typeof navigator === 'undefined') return false;
	return (navigator as Navigator & { standalone?: boolean }).standalone === true
		|| matchMedia('(display-mode: standalone)').matches;
}

/** iPhone and iPad browsers never offer to install a web app, so the app tells the user how. */
// True on iOS when the app is not open from the Home Screen. All iOS browsers can add it there.
export function canAddToHomeScreen(): boolean {
	return isIos() && !isStandalone();
}

/** True on Android, where the native app (with home screen widgets) can be installed. */
export function isAndroid(): boolean {
	if (typeof navigator === 'undefined') return false;
	return /Android/i.test(navigator.userAgent);
}
