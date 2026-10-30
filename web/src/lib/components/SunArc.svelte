<!-- SPDX-FileCopyrightText: Orinasa Njarasoa -->
<!-- SPDX-License-Identifier: GPL-3.0-or-later -->
<script lang="ts">
	import SkyArc from './SkyArc.svelte';

	/** The day as an arc from sunrise to sunset with the sun at the current time (epoch seconds). */
	let { sunrise, sunset, now }: { sunrise: number; sunset: number; now: number } = $props();

	let t = $derived(sunset > sunrise ? (now - sunrise) / (sunset - sunrise) : -1);
</script>

<!-- Before sunrise or after sunset: a faint sun under the horizon where it will rise -->
<SkyArc {t} progressColor="#FFB74D" belowColor="#FFD54F">
	{#snippet body(x, y)}<circle cx={x} cy={y} r="5" fill="#FFD54F" />{/snippet}
</SkyArc>
