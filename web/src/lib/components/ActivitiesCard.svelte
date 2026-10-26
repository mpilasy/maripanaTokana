<!-- SPDX-FileCopyrightText: Orinasa Njarasoa -->
<!-- SPDX-License-Identifier: GPL-3.0-or-later -->
<script lang="ts">
	import { _ } from 'svelte-i18n';
	import type { ActivityIndices, ActivityTier } from '#lib/domain/activityIndices.js';

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
			<svg class="icon" viewBox="0 0 24 24" width="22" height="22" fill="currentColor" aria-hidden="true"><path d="M13.49 5.48c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2zm-3.6 13.9l1-4.4 2.1 2v6h2v-7.5l-2.1-2 .6-3c1.3 1.5 3.3 2.5 5.5 2.5v-2c-1.9 0-3.5-1-4.3-2.4l-1-1.6c-.4-.6-1-1-1.7-1-.3 0-.5.1-.8.1l-5.2 2.2v4.7h2v-3.4l1.8-.7-1.6 8.1-4.9-1-.4 2 7 1.4z"/></svg>
			<span class="title">{$_('activity_running')}</span>
		</div>
		<span class="badge tier-{indices.running.toLowerCase()}">{tierLabel(indices.running)}</span>
	</div>
	<div class="activity-row">
		<div class="activity-left">
			<svg class="icon" viewBox="0 0 24 24" width="22" height="22" fill="currentColor" aria-hidden="true"><path d="M9.17 16.83c1.56 1.56 4.1 1.56 5.66 0 1.56-1.56 1.56-4.1 0-5.66l-5.66 5.66zM18 2.01L6 2c-1.11 0-2 .89-2 2v16c0 1.11.89 2 2 2h12c1.11 0 2-.89 2-2V4c0-1.11-.89-1.99-2-1.99zM10 4c.55 0 1 .45 1 1s-.45 1-1 1-1-.45-1-1 .45-1 1-1zM7 4c.55 0 1 .45 1 1s-.45 1-1 1-1-.45-1-1 .45-1 1-1zm5 16c-3.31 0-6-2.69-6-6s2.69-6 6-6 6 2.69 6 6-2.69 6-6 6z"/></svg>
			<span class="title">{$_('activity_laundry')}</span>
		</div>
		<span class="badge tier-{indices.laundry.toLowerCase()}">{tierLabel(indices.laundry)}</span>
	</div>
	<div class="activity-row">
		<div class="activity-left">
			<svg class="icon" viewBox="0 0 24 24" width="22" height="22" fill="currentColor" aria-hidden="true"><path d="M6.76 4.84l-1.8-1.79-1.41 1.41 1.79 1.79 1.42-1.41zM4 10.5H1v2h3v-2zm9-9.95h-2V3.5h2V.55zm7.45 3.91l-1.41-1.41-1.79 1.79 1.41 1.41 1.79-1.79zm-3.21 13.7l1.79 1.8 1.41-1.41-1.8-1.79-1.4 1.4zM20 10.5v2h3v-2h-3zm-8-5c-3.31 0-6 2.69-6 6s2.69 6 6 6 6-2.69 6-6-2.69-6-6-6zm-1 16.95h2V19.5h-2v2.95zm-7.45-3.91l1.41 1.41 1.79-1.8-1.41-1.41-1.79 1.8z"/></svg>
			<span class="title">{$_('activity_uv_safety')}</span>
		</div>
		<span class="badge tier-{indices.uvSafety.toLowerCase()}">{tierLabel(indices.uvSafety)}</span>
	</div>
</div>

<style>
	.activities-card {
		background: var(--surface-tile);
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
		flex: none;
		color: rgba(255, 255, 255, 0.7);
	}

	.title {
		font-family: var(--font-body);
		font-size: 15px;
		font-weight: 500;
		color: #ffffff;
	}

	.badge {
		/* A tint of the level's colour, with the text in a lighter shade of it */
		background: color-mix(in srgb, var(--tier) 22%, transparent);
		color: color-mix(in srgb, var(--tier) 70%, white);
		font-family: var(--font-display);
		font-size: 13px;
		font-weight: 700;
		padding: 4px 10px;
		border-radius: 12px;
	}

	.tier-excellent { --tier: #299501; }
	.tier-good { --tier: #0088CC; }
	.tier-fair { --tier: #F7E401; }
	.tier-poor { --tier: #F85900; }
</style>
