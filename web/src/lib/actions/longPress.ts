// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

/**
 * Svelte action: calls `callback` after the element is held for 500 ms (touch, mouse or pen), or on
 * the context-menu gesture. The click that ends a long press is swallowed so it doesn't also fire.
 * A null callback turns the long press off.
 */
export function longPress(node: HTMLElement, callback: (() => void) | null) {
	let cb = callback;
	let timer: ReturnType<typeof setTimeout> | undefined;
	let fired = false;
	const fire = () => {
		fired = true;
		navigator.vibrate?.(10);
		cb?.();
	};
	const start = () => {
		fired = false;
		if (!cb) return;
		clearTimeout(timer);
		timer = setTimeout(fire, 500);
	};
	const cancel = () => clearTimeout(timer);
	const swallowClick = (e: MouseEvent) => {
		if (fired) {
			e.preventDefault();
			e.stopImmediatePropagation();
			fired = false;
		}
	};
	const contextMenu = (e: MouseEvent) => {
		if (!cb) return;
		e.preventDefault();
		cancel();
		if (!fired) fire();
	};
	node.addEventListener('pointerdown', start);
	node.addEventListener('pointerup', cancel);
	node.addEventListener('pointerleave', cancel);
	node.addEventListener('pointercancel', cancel);
	node.addEventListener('click', swallowClick, true);
	node.addEventListener('contextmenu', contextMenu);
	return {
		update(next: (() => void) | null) {
			cb = next;
		},
		destroy() {
			cancel();
			node.removeEventListener('pointerdown', start);
			node.removeEventListener('pointerup', cancel);
			node.removeEventListener('pointerleave', cancel);
			node.removeEventListener('pointercancel', cancel);
			node.removeEventListener('click', swallowClick, true);
			node.removeEventListener('contextmenu', contextMenu);
		},
	};
}
