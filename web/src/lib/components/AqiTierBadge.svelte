<!-- SPDX-FileCopyrightText: Orinasa Njarasoa -->
<!-- SPDX-License-Identifier: GPL-3.0-or-later -->
<script lang="ts">
	import type { AqiTier } from '#lib/domain/airQuality.js';

	interface Props {
		/** The level; or give `color` for a level of another scale (e.g. UV). */
		tier?: AqiTier;
		color?: string;
		label: string;
		compact?: boolean;
		onClick?: () => void;
	}

	let { tier, label, color, compact = false, onClick }: Props = $props();
</script>

{#if onClick}
	<button type="button" class="aqi-badge tier-{tier}" class:compact style:--tier={color} onclick={onClick}>{label}</button>
{:else}
	<span class="aqi-badge tier-{tier}" class:compact style:--tier={color}>{label}</span>
{/if}

<style>
	/* AirNow AQI category colors (airnow.gov/aqi/aqi-basics). Our AqiTier type has 5 buckets
	   rather than AirNow's 6 — "unhealthy" covers AirNow's Orange "Unhealthy for Sensitive
	   Groups" (101-150) and Red "Unhealthy" (151-200) range, mapped here to Red. */
	/* One line always; in a narrow forecast card the label is cut short rather than wrapped */
	.aqi-badge {
		/* A tint of the level's colour, with the text in a lighter shade of it */
		background: color-mix(in srgb, var(--tier) 22%, transparent);
		color: color-mix(in srgb, var(--tier) 70%, white);
		display: inline-block;
		max-width: 100%;
		box-sizing: border-box;
		white-space: nowrap;
		overflow: hidden;
		text-overflow: ellipsis;
		padding: 2px 8px;
		border-radius: 999px;
		font-size: 12px;
		font-weight: 600;
		line-height: 1.4;
		border: none;
		font-family: inherit;
	}

	button.aqi-badge {
		cursor: pointer;
	}

	.aqi-badge.compact {
		font-size: 10px;
		padding: 1px 6px;
	}

	.tier-good { --tier: #00E400; }
	.tier-moderate { --tier: #FFFF00; }
	.tier-unhealthy { --tier: #FF0000; }
	.tier-very_unhealthy { --tier: #8F3F97; }
	.tier-hazardous { --tier: #7E0023; }
</style>
