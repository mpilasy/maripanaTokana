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
			case 'NOT_NEEDED':
				return $_('activity_tier_not_needed');
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
	<div class="activity-row">
		<div class="activity-left">
			<svg class="icon" viewBox="0 0 24 24" width="22" height="22" fill="currentColor" aria-hidden="true"><path d="M12,2c-5.33,4.55-8,8.48-8,11.8c0,4.98,3.8,8.2,8,8.2s8-3.22,8-8.2C20,10.48,17.33,6.55,12,2z M7.83,14 c0.37,0,0.67,0.26,0.74,0.62c0.41,2.22,2.28,2.98,3.64,2.87c0.43-0.02,0.79,0.32,0.79,0.75c0,0.4-0.32,0.73-0.72,0.75 c-2.13,0.13-4.62-1.09-5.19-4.12C7.01,14.42,7.37,14,7.83,14z"/></svg>
			<span class="title">{$_('activity_skin_comfort')}</span>
		</div>
		<span class="badge tier-{indices.skinComfort.toLowerCase()}">{tierLabel(indices.skinComfort)}</span>
	</div>
	<div class="activity-row">
		<div class="activity-left">
			<svg class="icon" viewBox="0 0 24 24" width="22" height="22" fill="currentColor" aria-hidden="true"><path d="M17 5c.83 0 1.5-.67 1.5-1.5 0-1-1.5-2.7-1.5-2.7s-1.5 1.7-1.5 2.7c0 .83.67 1.5 1.5 1.5zm-5 0c.83 0 1.5-.67 1.5-1.5 0-1-1.5-2.7-1.5-2.7s-1.5 1.7-1.5 2.7c0 .83.67 1.5 1.5 1.5zM7 5c.83 0 1.5-.67 1.5-1.5C8.5 2.5 7 .8 7 .8S5.5 2.5 5.5 3.5C5.5 4.33 6.17 5 7 5zm11.92 3.01C18.72 7.42 18.16 7 17.5 7h-11c-.66 0-1.21.42-1.42 1.01L3 14v8c0 .55.45 1 1 1h1c.55 0 1-.45 1-1v-1h12v1c0 .55.45 1 1 1h1c.55 0 1-.45 1-1v-8l-2.08-5.99zM6.5 18c-.83 0-1.5-.67-1.5-1.5S5.67 15 6.5 15s1.5.67 1.5 1.5S7.33 18 6.5 18zm11 0c-.83 0-1.5-.67-1.5-1.5s.67-1.5 1.5-1.5 1.5.67 1.5 1.5-.67 1.5-1.5 1.5zM5 13l1.5-4.5h11L19 13H5z"/></svg>
			<span class="title">{$_('activity_car_wash')}</span>
		</div>
		<span class="badge tier-{indices.carWash.toLowerCase()}">{tierLabel(indices.carWash)}</span>
	</div>
	<div class="activity-row">
		<div class="activity-left">
			<svg class="icon" viewBox="0 0 24 24" width="22" height="22" fill="currentColor" aria-hidden="true"><path d="M18.92 6.01C18.72 5.42 18.16 5 17.5 5h-11c-.66 0-1.21.42-1.42 1.01L3 12v8c0 .55.45 1 1 1h1c.55 0 1-.45 1-1v-1h12v1c0 .55.45 1 1 1h1c.55 0 1-.45 1-1v-8l-2.08-5.99zM6.5 16c-.83 0-1.5-.67-1.5-1.5S5.67 13 6.5 13s1.5.67 1.5 1.5S7.33 16 6.5 16zm11 0c-.83 0-1.5-.67-1.5-1.5s.67-1.5 1.5-1.5 1.5.67 1.5 1.5-.67 1.5-1.5 1.5zM5 11l1.5-4.5h11L19 11H5z"/></svg>
			<span class="title">{$_('activity_driving')}</span>
		</div>
		<span class="badge tier-{indices.driving.toLowerCase()}">{tierLabel(indices.driving)}</span>
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
		font-size: 0.9375rem;
		font-weight: 500;
		color: #ffffff;
	}

	.badge {
		/* A tint of the level's colour, with the text in a lighter shade of it */
		background: color-mix(in srgb, var(--tier) 22%, transparent);
		color: color-mix(in srgb, var(--tier) 50%, white);
		font-family: var(--font-display);
		font-size: 0.8125rem;
		font-weight: 700;
		padding: 4px 10px;
		border-radius: 12px;
	}

	.tier-excellent { --tier: #299501; }
	.tier-good { --tier: #0088CC; }
	.tier-fair { --tier: #F7E401; }
	.tier-poor { --tier: #F85900; }
	/* Neutral grey: sun protection at night */
	.tier-not_needed { --tier: #B0BEC5; }
</style>
