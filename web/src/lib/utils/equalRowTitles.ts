// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

/**
 * A Svelte action for a grid of tiles: the titles of the tiles in one row get the height of the
 * tallest title in that row, so the dials under them line up when one title wraps to two lines.
 * Twin: RowTitles in app/.../ui/weather/WeatherContent.kt.
 */
export function equalRowTitles(node: HTMLElement) {
	const align = () => {
		const titles = [...node.querySelectorAll<HTMLElement>('.card-title')];
		for (const t of titles) t.style.minHeight = '';
		// The tiles in one row have the same top
		const rows = new Map<number, HTMLElement[]>();
		for (const t of titles) {
			const top = Math.round(t.getBoundingClientRect().top);
			rows.set(top, [...(rows.get(top) ?? []), t]);
		}
		for (const row of rows.values()) {
			const tallest = Math.max(...row.map((t) => t.getBoundingClientRect().height));
			for (const t of row) t.style.minHeight = `${tallest}px`;
		}
	};
	// A new width, font or language can change how the titles wrap
	const observer = new ResizeObserver(align);
	observer.observe(node);
	const mutations = new MutationObserver(align);
	mutations.observe(node, { childList: true, subtree: true, characterData: true });
	align();
	return {
		destroy() {
			observer.disconnect();
			mutations.disconnect();
		},
	};
}
