<!-- SPDX-FileCopyrightText: Orinasa Njarasoa -->
<!-- SPDX-License-Identifier: GPL-3.0-or-later -->
<script lang="ts">
	/** A thermometer with a plus for the heat index, with a minus for the wind chill (Material Symbols, Apache License 2.0). */
	/**
	 * As tall as its row, which the value and its other unit set; `size` (px) where the row has no
	 * fixed height for the icon to follow.
	 */
	let { kind, color, size }: { kind: 'heat' | 'cold'; color: string; size?: number } = $props();
</script>

<svg class="felt-icon" class:sized={size} style:width={size ? `${size}px` : undefined} style:height={size ? `${size}px` : undefined} viewBox="0 -960 960 960" fill={color} aria-hidden="true">
	{#if kind === 'heat'}
		<path d="M680-640h-80q-17 0-28.5-11.5T560-680q0-17 11.5-28.5T600-720h80v-80q0-17 11.5-28.5T720-840q17 0 28.5 11.5T760-800v80h80q17 0 28.5 11.5T880-680q0 17-11.5 28.5T840-640h-80v80q0 17-11.5 28.5T720-520q-17 0-28.5-11.5T680-560v-80ZM320-120q-83 0-141.5-58.5T120-320q0-48 21-89.5t59-70.5v-240q0-50 35-85t85-35q50 0 85 35t35 85v240q38 29 59 70.5t21 89.5q0 83-58.5 141.5T320-120Zm-40-440h80v-160q0-17-11.5-28.5T320-760q-17 0-28.5 11.5T280-720v160Z" />
	{:else}
		<path d="M600-640q-17 0-28.5-11.5T560-680q0-17 11.5-28.5T600-720h240q17 0 28.5 11.5T880-680q0 17-11.5 28.5T840-640H600ZM320-120q-83 0-141.5-58.5T120-320q0-48 21-89.5t59-70.5v-240q0-50 35-85t85-35q50 0 85 35t35 85v240q38 29 59 70.5t21 89.5q0 83-58.5 141.5T320-120Zm-40-440h80v-160q0-17-11.5-28.5T320-760q-17 0-28.5 11.5T280-720v160Z" />
	{/if}
</svg>

<style>
	/* No height of its own: it takes the height of the row, which the two numbers set */
	.felt-icon {
		flex-shrink: 0;
		align-self: stretch;
		height: 0;
		min-height: 100%;
		width: auto;
		aspect-ratio: 1;
		margin-inline-end: 4px;
	}

	.felt-icon.sized {
		align-self: center;
		min-height: 0;
	}
</style>
