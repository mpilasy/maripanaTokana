<!-- SPDX-FileCopyrightText: Orinasa Njarasoa -->
<!-- SPDX-License-Identifier: GPL-3.0-or-later -->
<script lang="ts">
	import SkyArc from './SkyArc.svelte';

	/** The day as an arc from sunrise to sunset with the sun at the current time (epoch seconds). */
	let { sunrise, sunset, now }: { sunrise: number; sunset: number; now: number } = $props();

	let t = $derived(sunset > sunrise ? (now - sunrise) / (sunset - sunrise) : -1);
</script>

<SkyArc {t} progressColor="#FFB74D">
	{#snippet body(x, y)}<circle cx={x} cy={y} r="5" fill="#FFD54F" />{/snippet}
</SkyArc>
