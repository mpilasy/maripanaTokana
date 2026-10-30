<!-- SPDX-FileCopyrightText: Orinasa Njarasoa -->
<!-- SPDX-License-Identifier: GPL-3.0-or-later -->
<script lang="ts">
	import DualUnitText from './DualUnitText.svelte';
	import type { Temperature } from '#lib/domain/temperature.js';

	/** A forecast card's heat index or wind chill in both units; "--" for an item where it does not apply. */
	let { value, metricPrimary, loc, onToggleUnits }: { value: Temperature | null; metricPrimary: boolean; loc: (s: string) => string; onToggleUnits: () => void } = $props();
	let dual = $derived(value?.displayDual(metricPrimary));
</script>

{#if dual}
	<DualUnitText primary={loc(dual[0])} secondary={loc(dual[1])} primarySize="0.875rem" align="center" onClick={onToggleUnits} />
{:else}
	<span class="none">--</span>
{/if}

<style>
	.none {
		color: rgba(255, 255, 255, 0.5);
	}
</style>
