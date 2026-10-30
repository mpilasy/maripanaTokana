<!-- SPDX-FileCopyrightText: Orinasa Njarasoa -->
<!-- SPDX-License-Identifier: GPL-3.0-or-later -->
<script lang="ts">
	import type { PollenTier } from '#lib/domain/airQuality.js';

	interface Props {
		tier: PollenTier;
		label: string;
		compact?: boolean;
	}

	let { tier, label, compact = false }: Props = $props();
</script>

<span class="pollen-badge tier-{tier}" class:compact>{label}</span>

<style>
	/* Reuses UvTierBadge's Low/Moderate/High/Very High colors (green/yellow/orange/red) — same
	   four-tier semantics, so the same color ramp applies. */
	.pollen-badge {
		/* A tint of the level's colour, with the text in a lighter shade of it, as on the activity badges */
		background: color-mix(in srgb, var(--tier) 22%, transparent);
		color: color-mix(in srgb, var(--tier) 50%, white);
		display: inline-block;
		padding: 2px 8px;
		border-radius: 999px;
		font-size: 0.75rem;
		font-weight: 600;
		line-height: 1.4;
	}

	.pollen-badge.compact {
		font-size: 0.625rem;
		padding: 1px 6px;
	}

	.tier-low { --tier: #299501; }
	.tier-moderate { --tier: #F7E401; }
	.tier-high { --tier: #F85900; }
	.tier-very_high { --tier: #D8001D; }
</style>
