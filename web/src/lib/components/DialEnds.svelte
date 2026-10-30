<!-- SPDX-FileCopyrightText: Orinasa Njarasoa -->
<!-- SPDX-License-Identifier: GPL-3.0-or-later -->
<!-- Twin: DialEnds in app/src/main/java/orinasa/njarasoa/maripanatokana/ui/weather/WeatherContent.kt -->
<script lang="ts">
	/**
	 * The two end values of a dial's scale, just under the two ends of its arc (in a Gauge): the number in
	 * the primary unit, and under it, smaller, the number in the secondary unit when there is one.
	 * The unit itself is in the middle of the dial.
	 */
	let { min, max }: { min: [string, string?]; max: [string, string?] } = $props();
</script>

<!-- The arc ends at 135° and 405°: 20.5 % and 79.5 % across; the labels start 2 px under a knob at an end (the Gauge sets the top) -->
{#each [{ end: min, x: 20.5 }, { end: max, x: 79.5 }] as { end, x } (x)}
	<span class="dial-end" style:left="{x}%" dir="ltr">
		<span class="primary">{end[0]}</span>
		{#if end[1]}<span class="secondary">{end[1]}</span>{/if}
	</span>
{/each}

<style>
	.dial-end {
		position: absolute;
		top: var(--dial-end-top, 85%);
		transform: translateX(-50%);
		display: flex;
		flex-direction: column;
		align-items: center;
		font-feature-settings: var(--font-features);
		pointer-events: none;
	}

	.primary {
		font-size: 0.625rem;
		line-height: 0.6875rem;
		color: rgba(255, 255, 255, 0.6);
	}

	.secondary {
		font-size: 0.5625rem;
		line-height: 0.625rem;
		color: rgba(255, 255, 255, 0.5);
	}
</style>
