<!-- SPDX-FileCopyrightText: Orinasa Njarasoa -->
<!-- SPDX-License-Identifier: GPL-3.0-or-later -->
<script lang="ts">
	import type { Snippet } from 'svelte';

	/**
	 * Lets the user slide a finger along the chart in `children`: a line and a label (the hour
	 * or the day of `label`) follow the finger, and the row of cards (`scrollEl`) scrolls to the
	 * item under it. Only a horizontal slide starts it, so the page still scrolls up and down.
	 */
	interface Props {
		count: number;
		itemWidth: number;
		itemSpacing: number;
		scrollEl: HTMLElement | null;
		label: (i: number) => string;
		children: Snippet;
	}

	let { count, itemWidth, itemSpacing, scrollEl, label, children }: Props = $props();

	const SLOP = 8;
	let el = $state<HTMLElement | null>(null);
	let index = $state<number | null>(null);
	let width = $state(0);
	let start: { x: number; y: number; id: number } | null = null;

	let step = $derived(itemWidth + itemSpacing);
	let totalWidth = $derived(count * itemWidth + (count - 1) * itemSpacing);
	let xScale = $derived(width > 0 && totalWidth > 0 ? width / totalWidth : 1);
	let lineX = $derived(index == null ? 0 : (index * step + itemWidth / 2) * xScale);
	let pillWidth = $state(0);
	// Centred on the line, but inside the chart near the edges
	let pillLeft = $derived(Math.min(Math.max(0, lineX - pillWidth / 2), Math.max(0, width - pillWidth)));

	function select(clientX: number) {
		if (!el || count === 0) return;
		const x = clientX - el.getBoundingClientRect().left;
		const i = Math.min(count - 1, Math.max(0, Math.round((x / xScale - itemWidth / 2) / step)));
		if (i === index) return;
		index = i;
		navigator.vibrate?.(5);
		// The item in the middle of the row
		if (scrollEl) scrollEl.scrollLeft = i * step + itemWidth / 2 - scrollEl.clientWidth / 2;
	}

	function onPointerDown(e: PointerEvent) {
		if (e.pointerType === 'mouse' && e.button !== 0) return;
		start = { x: e.clientX, y: e.clientY, id: e.pointerId };
	}

	function onPointerMove(e: PointerEvent) {
		if (!start || e.pointerId !== start.id) return;
		if (index == null) {
			const dx = Math.abs(e.clientX - start.x);
			if (dx < SLOP || dx < Math.abs(e.clientY - start.y)) return;
			el?.setPointerCapture(e.pointerId);
		}
		select(e.clientX);
	}

	function end() {
		start = null;
		index = null;
	}
</script>

<div
	class="scrub"
	bind:this={el}
	bind:clientWidth={width}
	onpointerdown={onPointerDown}
	onpointermove={onPointerMove}
	onpointerup={end}
	onpointercancel={end}
	ontouchmove={(e) => { if (index != null) e.stopPropagation(); }}
	role="presentation"
>
	{@render children()}
	{#if index != null}
		<div class="line" style:left="{lineX}px"></div>
		<div class="pill" style:left="{pillLeft}px" bind:offsetWidth={pillWidth}>{label(index)}</div>
	{/if}
</div>

<style>
	.scrub {
		position: relative;
		/* The browser keeps the up-and-down page scroll; sideways moves come to the chart */
		touch-action: pan-y;
		user-select: none;
		-webkit-user-select: none;
	}

	.line {
		position: absolute;
		top: 0;
		bottom: 0;
		width: 1px;
		background: rgba(255, 255, 255, 0.6);
		pointer-events: none;
	}

	.pill {
		position: absolute;
		top: 0;
		padding: 2px 6px;
		border-radius: 6px;
		background: rgba(0, 0, 0, 0.75);
		color: white;
		font-family: var(--font-display);
		font-size: 10px;
		font-weight: 700;
		white-space: nowrap;
		pointer-events: none;
	}
</style>
