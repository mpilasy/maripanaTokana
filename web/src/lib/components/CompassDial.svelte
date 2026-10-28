<!-- SPDX-FileCopyrightText: Orinasa Njarasoa -->
<!-- SPDX-License-Identifier: GPL-3.0-or-later -->
<script lang="ts">
	import type { Snippet } from 'svelte';

	/**
	 * A compass ring for a card's middle: a tick at north and an arrowhead on the ring at where the
	 * wind comes from (degrees, 0 = north), pointing in. It fills its container's width (square);
	 * `children` show in the middle. Same as CompassDial on Android.
	 */
	let { degrees, children }: { degrees: number; children?: Snippet } = $props();

	let width = $state(0);
	// The ring stops growing on a large dial, like the gauges
	let k = $derived(Math.min(1, (1.6 * 48) / Math.max(1, width)));
	let r = $derived(24 - 4 * k);
</script>

<div class="compass" bind:clientWidth={width}>
	<svg width="100%" height="100%" viewBox="0 0 48 48" aria-hidden="true">
		<circle cx="24" cy="24" r={r} fill="none" stroke="rgba(255,255,255,0.18)" stroke-width={2.5 * k} />
		<line x1="24" y1={24 - r - 3 * k} x2="24" y2={24 - r + 4 * k} stroke="rgba(255,255,255,0.6)" stroke-width={2 * k} stroke-linecap="round" />
		<g transform="rotate({degrees} 24 24)">
			<path d="M 24 {24 - r + 7 * k} L {24 + 5 * k} {24 - r - 3 * k} L {24 - 5 * k} {24 - r - 3 * k} Z" fill="#64B5F6" />
		</g>
	</svg>
	{#if children}{@render children()}{/if}
</div>

<style>
	.compass {
		position: relative;
		width: 100%;
		aspect-ratio: 1;
	}

	.compass svg {
		display: block;
	}
</style>
