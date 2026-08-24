<script lang="ts">
	import { _ } from 'svelte-i18n';
	import type { ActivityIndices, ActivityTier } from '$lib/domain/activityIndices';

	interface Props {
		indices: ActivityIndices;
	}

	let { indices }: Props = $props();

	function tierLabel(tier: ActivityTier): string {
		switch (tier) {
			case 'EXCELLENT':
				return $_('activity_tier_excellent');
			case 'GOOD':
				return $_('activity_tier_good');
			case 'FAIR':
				return $_('activity_tier_fair');
			case 'POOR':
				return $_('activity_tier_poor');
		}
	}
</script>

<div class="activities-card">
	<div class="activity-row">
		<div class="activity-left">
			<span class="icon">🏃</span>
			<span class="title">{$_('activity_running')}</span>
		</div>
		<span class="badge tier-{indices.running.toLowerCase()}">{tierLabel(indices.running)}</span>
	</div>
	<div class="activity-row">
		<div class="activity-left">
			<span class="icon">🧺</span>
			<span class="title">{$_('activity_laundry')}</span>
		</div>
		<span class="badge tier-{indices.laundry.toLowerCase()}">{tierLabel(indices.laundry)}</span>
	</div>
	<div class="activity-row">
		<div class="activity-left">
			<span class="icon">☀️</span>
			<span class="title">{$_('activity_uv_safety')}</span>
		</div>
		<span class="badge tier-{indices.uvSafety.toLowerCase()}">{tierLabel(indices.uvSafety)}</span>
	</div>
</div>

<style>
	.activities-card {
		background: rgba(42, 31, 165, 0.6);
		backdrop-filter: blur(12px);
		-webkit-backdrop-filter: blur(12px);
		border-radius: 16px;
		padding: 16px;
		display: flex;
		flex-direction: column;
		gap: 12px;
	}

	.activity-row {
		display: flex;
		align-items: center;
		justify-content: space-between;
	}

	.activity-left {
		display: flex;
		align-items: center;
		gap: 8px;
	}

	.icon {
		font-size: 18px;
	}

	.title {
		font-family: var(--font-body);
		font-size: 15px;
		font-weight: 500;
		color: #ffffff;
	}

	.badge {
		font-family: var(--font-display);
		font-size: 13px;
		font-weight: 700;
		padding: 4px 10px;
		border-radius: 12px;
	}

	.tier-excellent { background: #299501; color: #fff; }
	.tier-good { background: #0088CC; color: #fff; }
	.tier-fair { background: #F7E401; color: #1a1a1a; }
	.tier-poor { background: #F85900; color: #fff; }
</style>
