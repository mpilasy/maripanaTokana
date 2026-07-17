<script lang="ts">
	import type { AqiTier } from '$lib/domain/airQuality';

	interface Props {
		tier: AqiTier;
		label: string;
		compact?: boolean;
	}

	let { tier, label, compact = false }: Props = $props();
</script>

<span class="aqi-badge tier-{tier}" class:compact>{label}</span>

<style>
	/* AirNow AQI category colors (airnow.gov/aqi/aqi-basics). Our AqiTier type has 5 buckets
	   rather than AirNow's 6 — "unhealthy" covers AirNow's Orange "Unhealthy for Sensitive
	   Groups" (101-150) and Red "Unhealthy" (151-200) range, mapped here to Red. */
	.aqi-badge {
		display: inline-block;
		padding: 2px 8px;
		border-radius: 999px;
		font-size: 12px;
		font-weight: 600;
		line-height: 1.4;
	}

	.aqi-badge.compact {
		font-size: 10px;
		padding: 1px 6px;
	}

	.tier-good { background: #00E400; color: #1a1a1a; }
	.tier-moderate { background: #FFFF00; color: #1a1a1a; }
	.tier-unhealthy { background: #FF0000; color: #fff; }
	.tier-very_unhealthy { background: #8F3F97; color: #fff; }
	.tier-hazardous { background: #7E0023; color: #fff; }
</style>
