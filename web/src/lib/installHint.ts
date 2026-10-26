// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

/** iPhone and iPad browsers never offer to install a web app, so the app tells the user how. */
// True on iOS when the app is not open from the Home Screen. All iOS browsers can add it there.
export function canAddToHomeScreen(): boolean {
	if (typeof navigator === 'undefined') return false;
	const ua = navigator.userAgent;
	// iPadOS reports itself as a Mac with a touch screen
	const ios = /iPhone|iPad|iPod/.test(ua) || (/Macintosh/.test(ua) && navigator.maxTouchPoints > 1);
	const standalone = (navigator as Navigator & { standalone?: boolean }).standalone === true
		|| matchMedia('(display-mode: standalone)').matches;
	return ios && !standalone;
}
