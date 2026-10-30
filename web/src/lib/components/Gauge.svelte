<!-- SPDX-FileCopyrightText: Orinasa Njarasoa -->
<!-- SPDX-License-Identifier: GPL-3.0-or-later -->
<script lang="ts">
	import type { Snippet } from 'svelte';

	/**
	 * A 270° arc, open at the bottom, with a dot at `fraction` (0-1). Filled up to the dot in `color`, or,
	 * with `segments`, drawn as equal coloured segments (one per level) so the whole scale always shows.
	 * Without `size` it fills its container's width (square); `children` show in the middle.
	 */
	let { fraction, color, size, segments, children }: { fraction: number; color: string; size?: number; segments?: string[]; children?: Snippet } = $props();

	let width = $state(0);
	// The ring and the dot stop growing on a large dial (as on Android: at most 1.6 px per unit of 48)
	let k = $derived(Math.min(1, (1.6 * 48) / Math.max(1, size ?? width)));
	let r = $derived(24 - 4 * k);
	function point(deg: number): string {
		const a = (deg * Math.PI) / 180;
		return `${(24 + r * Math.cos(a)).toFixed(2)} ${(24 + r * Math.sin(a)).toFixed(2)}`;
	}
	// Angles clockwise from 3 o'clock: the arc runs from 135° (bottom left) to 405° (bottom right)
	function arc(from: number, to: number): string {
		return `M ${point(from)} A ${r} ${r} 0 ${to - from > 180 ? 1 : 0} 1 ${point(to)}`;
	}
	let f = $derived(Number.isFinite(fraction) ? Math.min(1, Math.max(0, fraction)) : 0);
	let end = $derived(point(135 + 270 * f).split(' '));
	// Twin: dialEndTop in app/src/main/java/orinasa/njarasoa/maripanatokana/ui/weather/WeatherContent.kt
	// The top of the DialEnds labels: 2 px under the knob (radius 4.5 + half its 2.5 stroke) at an end of the arc
	let dialEndTop = $derived(((24 + r * Math.SQRT1_2 + 5.75 * k) / 48) * (size ?? width) + 2);
</script>

<div class="gauge" class:fill={size == null} style:width={size != null ? `${size}px` : undefined} style:height={size != null ? `${size}px` : undefined} bind:clientWidth={width}
	style:--dial-end-top="{dialEndTop}px">
	<svg width="100%" height="100%" viewBox="0 0 48 48" aria-hidden="true">
		{#if segments}
			{@const span = 270 / segments.length}
			{#each segments as c, i}
				<!-- A small gap keeps the levels apart; the reading's own level is drawn brightest -->
				<path d={arc(135 + i * span + 2, 135 + (i + 1) * span - 2)} fill="none" stroke={c} stroke-width={4 * k} stroke-linecap="butt"
					stroke-opacity={i === Math.min(segments.length - 1, Math.floor(f * segments.length)) ? 1 : 0.4} />
			{/each}
		{:else}
			<path d={arc(135, 405)} fill="none" stroke="rgba(255,255,255,0.18)" stroke-width={4 * k} stroke-linecap="round" />
			{#if f > 0}
				<path d={arc(135, 135 + 270 * f)} fill="none" stroke={color} stroke-width={4 * k} stroke-linecap="round" />
			{/if}
		{/if}
		<!-- Where the value sits on the dial -->
		<circle cx={end[0]} cy={end[1]} r={4.5 * k} fill="white" stroke={color} stroke-width={2.5 * k} />
	</svg>
	{#if children}{@render children()}{/if}
</div>

<style>
	.gauge {
		position: relative;
		flex-shrink: 0;
	}

	.gauge.fill {
		width: 100%;
		aspect-ratio: 1;
	}

	.gauge svg {
		display: block;
	}
</style>
