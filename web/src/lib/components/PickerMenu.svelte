<!-- SPDX-FileCopyrightText: Orinasa Njarasoa -->
<!-- SPDX-License-Identifier: GPL-3.0-or-later -->
<script lang="ts">
	/** One row: an optional flag before the label, the label's language, and the font to draw it in. */
	export interface PickerItem {
		lead?: string;
		label: string;
		lang?: string;
		font?: string;
	}

	/**
	 * A list opened by holding a footer button (the language flag or the font), as holding a favorites arrow
	 * lists the places. Put it in a positioned box around the button. `above` opens it upwards (for the footer),
	 * `alignStart` lines its start up with the button's start (else its end with the button's end).
	 * A long list scrolls, and starts at the current item.
	 */
	let {
		open = $bindable(),
		items,
		current,
		onSelect,
		above = false,
		alignStart = false,
	}: {
		open: boolean;
		items: PickerItem[];
		current: number;
		onSelect: (i: number) => void;
		above?: boolean;
		alignStart?: boolean;
	} = $props();

	$effect(() => {
		if (!open) return;
		const close = () => { open = false; };
		window.addEventListener('click', close);
		return () => window.removeEventListener('click', close);
	});

	// Show the current item with about two items above it
	function scrollToCurrent(node: HTMLElement) {
		const list = node.parentElement;
		if (list) list.scrollTop = Math.max(0, node.offsetTop - 2 * node.offsetHeight);
	}
</script>

{#if open}
	<div class="picker" class:above class:align-start={alignStart} role="menu">
		{#each items as item, i (item.label)}
			{@const isCurrent = i === current}
			{#if isCurrent}
				<button type="button" role="menuitem" class="picker-item current" lang={item.lang} style:font-family={item.font}
					use:scrollToCurrent onclick={(e) => { e.stopPropagation(); open = false; }}>
					{#if item.lead}<span class="lead" aria-hidden="true">{item.lead}</span>{/if}
					{item.label}
				</button>
			{:else}
				<button type="button" role="menuitem" class="picker-item" lang={item.lang} style:font-family={item.font}
					onclick={(e) => { e.stopPropagation(); open = false; onSelect(i); }}>
					{#if item.lead}<span class="lead" aria-hidden="true">{item.lead}</span>{/if}
					{item.label}
				</button>
			{/if}
		{/each}
	</div>
{/if}

<style>
	/* Same look as the favorites list */
	.picker {
		position: absolute;
		top: 100%;
		inset-inline-end: 0;
		z-index: 20;
		min-width: 180px;
		max-height: 60vh;
		overflow-y: auto;
		padding: 6px 0;
		border-radius: 16px;
		border: 1px solid var(--hairline);
		background: #1A1565;
		box-shadow: 0 8px 24px rgba(0, 0, 0, 0.4);
	}

	.picker.above {
		top: auto;
		bottom: 100%;
	}

	.picker.align-start {
		inset-inline-end: auto;
		inset-inline-start: 0;
	}

	.picker-item {
		display: flex;
		align-items: center;
		gap: 12px;
		width: 100%;
		padding: 12px 16px;
		border: none;
		background: transparent;
		color: rgba(255, 255, 255, 0.85);
		font-family: var(--font-body);
		font-size: 0.9375rem;
		text-align: start;
		white-space: nowrap;
		cursor: pointer;
	}

	.picker-item:hover {
		background: var(--surface-tile);
	}

	.picker-item.current {
		color: white;
		font-weight: 700;
	}

	.lead {
		font-size: 1.125rem;
		line-height: 1;
	}
</style>
