// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

// See https://svelte.dev/docs/kit/types#app.d.ts
declare global {
	namespace App {
	}

	namespace svelteHTML {
		interface HTMLAttributes<T> {
			onlongpress?: (event: CustomEvent) => void;
		}
	}
}

export {};
