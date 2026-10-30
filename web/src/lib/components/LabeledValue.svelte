<!-- SPDX-FileCopyrightText: Orinasa Njarasoa -->
<!-- SPDX-License-Identifier: GPL-3.0-or-later -->
<script lang="ts">
	import FeltIcon from './FeltIcon.svelte';
	/**
	 * A small label with its value under it (and the other unit under that, smaller), at the start or
	 * the end of a card's bottom row (like sunrise and sunset), or in its middle.
	 */
	let { label, value, secondary, end = false, centered = false, icon, ltr = false }: {
		label: string; value: string; secondary?: string; end?: boolean; centered?: boolean;
		/** A number with its unit: keep the unit after the number in Arabic ("١٦°C", not "C°١٦") */
		ltr?: boolean;
		/** An icon beside the value, as tall as the value and the other unit: a thermometer with a plus for the heat index, with a minus for the wind chill. */
		icon?: { kind: 'heat' | 'cold'; color: string } | null;
	} = $props();
</script>

<span class="labeled" class:end class:centered
	><span class="label">{label}</span
	><!-- The icon beside the value and the other unit, as tall as both --><span class="values-row"
		>{#if icon}<FeltIcon kind={icon.kind} color={icon.color} />{/if}<span class="values"><bdi dir={ltr ? 'ltr' : undefined}>{value}</bdi>{#if secondary}<span class="secondary" dir={ltr ? 'ltr' : undefined}>{secondary}</span>{/if}</span></span
	></span
>

<style>
	.labeled {
		display: flex;
		flex-direction: column;
		font-family: var(--font-display);
		font-size: 0.9375rem;
		font-weight: 700;
		color: white;
		font-feature-settings: var(--font-features);
		text-align: start;
		/* A value stays on one line; text beside it wraps instead */
		white-space: nowrap;
		flex-shrink: 0;
	}

	.labeled.end {
		text-align: end;
		align-items: flex-end;
	}

	.labeled.centered {
		text-align: center;
		align-items: center;
	}

	.label {
		font-family: var(--font-body, inherit);
		font-size: 0.6875rem;
		font-weight: 400;
		color: rgba(255, 255, 255, 0.5);
	}

	.values-row {
		display: flex;
	}

	.values {
		display: flex;
		flex-direction: column;
		align-items: inherit;
	}

	.labeled.end .values {
		align-items: flex-end;
	}

	.labeled.centered .values {
		align-items: center;
	}

	/* The other unit, under the value and smaller */
	.secondary {
		font-size: 0.75rem;
		font-weight: 400;
		color: rgba(255, 255, 255, 0.6);
	}
</style>
