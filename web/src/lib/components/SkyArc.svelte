<!-- SPDX-FileCopyrightText: Orinasa Njarasoa -->
<!-- SPDX-License-Identifier: GPL-3.0-or-later -->
<script lang="ts">
	import type { Snippet } from 'svelte';

	/**
	 * A dashed arc from rising (left) to setting (right). While `t` (the part of the way done) is in
	 * 0-1, the part done is drawn in `progressColor` and `body` draws the sun or moon at its place.
	 * Else, with `belowColor`, a faint dot just under the horizon at the rising end shows it is below.
	 */
	let { t, progressColor, body, belowColor }: { t: number; progressColor: string; body: Snippet<[number, number]>; belowColor?: string } = $props();

	// Half-ellipse from (8,68) to (112,68) peaking at y=8
	const W = 120, H = 76, CX = 60, RX = 52, RY = 60, BASE = 68;
	let up = $derived(t >= 0 && t <= 1);
	let pos = $derived.by(() => {
		const a = Math.PI * (1 - Math.min(1, Math.max(0, t)));
		return { x: CX + RX * Math.cos(a), y: BASE - RY * Math.sin(a) };
	});
	// The part of the arc already travelled
	let travelled = $derived(`M ${CX - RX} ${BASE} A ${RX} ${RY} 0 0 1 ${pos.x.toFixed(2)} ${pos.y.toFixed(2)}`);
</script>

<svg width="100%" viewBox="0 0 {W} {H}" aria-hidden="true" class="sky-arc">
	<line x1="2" y1={BASE} x2={W - 2} y2={BASE} stroke="rgba(255,255,255,0.15)" stroke-width="1" />
	<path d="M {CX - RX} {BASE} A {RX} {RY} 0 0 1 {CX + RX} {BASE}" fill="none" stroke="rgba(255,255,255,0.25)" stroke-width="1.5" stroke-dasharray="3 3" />
	{#if up}
		<path d={travelled} fill="none" stroke={progressColor} stroke-width="2" stroke-linecap="round" />
		{@render body(pos.x, pos.y)}
	{:else if belowColor}
		<circle cx={CX - RX} cy={BASE + 4} r="3.5" fill={belowColor} fill-opacity="0.35" />
	{/if}
</svg>

<style>
	.sky-arc {
		display: block;
		max-height: 144px;
	}
</style>
