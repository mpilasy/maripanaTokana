// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

/** True when the user asks the system for less motion. */
export function prefersReducedMotion(): boolean {
	return typeof matchMedia !== 'undefined' && matchMedia('(prefers-reduced-motion: reduce)').matches;
}

/** The duration `ms` for a Svelte transition, or 0 when the user asks for less motion. */
export function motionDuration(ms: number): number {
	return prefersReducedMotion() ? 0 : ms;
}
