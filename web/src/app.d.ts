// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

// See https://svelte.dev/docs/kit/types#app.d.ts
declare global {
	/** The short hash of the built commit (vite.config.ts) */
	const __GIT_HASH__: string;
	/** The UTC commit time of the built commit, yyyy-MM-dd HH:mm (vite.config.ts) */
	const __COMMIT_TIME__: string;

	namespace App {
	}

	namespace svelteHTML {
		interface HTMLAttributes<T> {
			onlongpress?: (event: CustomEvent) => void;
		}
	}
}

export {};
