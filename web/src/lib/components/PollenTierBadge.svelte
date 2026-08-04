<script lang="ts">
	import type { PollenTier } from '$lib/domain/airQuality';

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
		display: inline-block;
		padding: 2px 8px;
		border-radius: 999px;
		font-size: 12px;
		font-weight: 600;
		line-height: 1.4;
	}

	.pollen-badge.compact {
		font-size: 10px;
		padding: 1px 6px;
	}

	.tier-low { background: #299501; color: #fff; }
	.tier-moderate { background: #F7E401; color: #1a1a1a; }
	.tier-high { background: #F85900; color: #fff; }
	.tier-very_high { background: #D8001D; color: #fff; }
</style>
