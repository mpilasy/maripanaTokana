<!-- SPDX-FileCopyrightText: Orinasa Njarasoa -->
<!-- SPDX-License-Identifier: GPL-3.0-or-later -->
<script lang="ts">
	import type { Snippet } from 'svelte';

	/**
	 * A reading in the middle of a dial: the value, its unit, and the same reading in the other unit.
	 * `badge` sits in the open bottom of the ring.
	 */
	let { value, unit, secondary, badge }: { value: string; unit?: string; secondary?: string; badge?: Snippet } = $props();
</script>

<div class="dial-value">
	<span class="value">{value}</span>
	{#if unit}<span class="unit">{unit}</span>{/if}
	{#if secondary}<span class="secondary">{secondary}</span>{/if}
</div>
{#if badge}
	<div class="badge">{@render badge()}</div>
{/if}

<style>
	.dial-value {
		position: absolute;
		inset: 0;
		display: flex;
		flex-direction: column;
		align-items: center;
		justify-content: center;
		font-feature-settings: var(--font-features);
	}

	.value {
		font-family: var(--font-display);
		font-size: 22px;
		line-height: 24px;
		font-weight: 700;
		color: white;
	}

	.unit {
		font-size: 10px;
		line-height: 11px;
		color: rgba(255, 255, 255, 0.6);
	}

	.secondary {
		font-family: var(--font-display);
		font-size: 12px;
		line-height: 14px;
		margin-top: 2px;
		color: rgba(255, 255, 255, 0.7);
	}

	.badge {
		position: absolute;
		bottom: 4px;
		left: 0;
		right: 0;
		display: flex;
		justify-content: center;
	}
</style>
