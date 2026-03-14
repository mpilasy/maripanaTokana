<script lang="ts">
	import { _ } from 'svelte-i18n';
	import type { WeatherAlert } from '$lib/domain/weatherData';
	import { slide } from 'svelte/transition';

	interface Props {
		alerts: WeatherAlert[];
	}

	let { alerts }: Props = $props();
	let isExpanded = $state(false);

	// Determine the most severe level present
	let levels = $derived(alerts.map(a => a.level));
	let topLevel = $derived(levels.includes('emergency') ? 'emergency'
		: levels.includes('warning') ? 'warning'
		: 'watch');

	let topAlert = $derived(alerts.find(a => a.level === topLevel) || alerts[0]);
</script>

{#if alerts.length > 0 && topAlert}
	<div class="alert-banner" class:watch={topLevel === 'watch'} class:warning={topLevel === 'warning' || topLevel === 'emergency'}>
		<!-- svelte-ignore a11y_click_events_have_key_events -->
		<!-- svelte-ignore a11y_no_static_element_interactions -->
		<div class="banner-header" onclick={() => isExpanded = !isExpanded}>
			<div class="icon-text">
				<span class="alert-icon">
					{#if topLevel === 'watch'}
						&#9888;
					{:else}
						&#10071;
					{/if}
				</span>
				<span class="alert-title">{$_(topAlert.title) || topAlert.title}</span>
				{#if topAlert.source !== 'derived'}
					<span class="source-badge">{topAlert.source.toUpperCase()}</span>
				{/if}
			</div>
			<span class="spacer"></span>
			<span class="chevron" class:expanded={isExpanded}>&#9660;</span>
		</div>

		{#if isExpanded}
			<div class="alert-details" transition:slide={{ duration: 300 }}>
				{#each alerts as alert}
					<div class="alert-item">
						<div class="item-title-row">
							<div class="item-title">{$_(alert.title) || alert.title}</div>
							{#if alert.source !== 'derived'}
								<span class="source-badge">{alert.source.toUpperCase()}</span>
							{/if}
						</div>
						<div class="item-desc">{$_(alert.description) || alert.description}</div>
					</div>
				{/each}
			</div>
		{/if}
	</div>
{/if}

<style>
	.alert-banner {
		margin-bottom: 24px;
		border-radius: 16px;
		overflow: hidden;
		background: rgba(255, 255, 255, 0.1);
		border-left: 4px solid transparent;
		transition: background 0.2s;
	}

	.alert-banner.watch {
		border-left-color: #FFA500;
		background: rgba(255, 165, 0, 0.15);
	}

	.alert-banner.warning {
		border-left-color: #FF4444;
		background: rgba(255, 68, 68, 0.15);
	}

	.banner-header {
		display: flex;
		align-items: center;
		justify-content: space-between;
		padding: 12px 16px;
		gap: 12px;
		cursor: pointer;
		user-select: none;
	}

	.icon-text {
		display: flex;
		align-items: center;
		gap: 8px;
	}

	.spacer {
		flex: 1;
	}

	.alert-icon {
		font-size: 18px;
	}

	.alert-title {
		font-size: 16px;
		font-weight: 700;
		color: white;
	}

	.source-badge {
		background: rgba(255, 255, 255, 0.2);
		padding: 2px 6px;
		border-radius: 4px;
		font-size: 10px;
		font-weight: 800;
		color: white;
		margin-left: 4px;
		letter-spacing: 0.5px;
	}

	.chevron {
		color: rgba(255, 255, 255, 0.7);
		font-size: 12px;
		transition: transform 0.3s ease;
		transform: rotate(-90deg);
	}

	.chevron.expanded {
		transform: rotate(0deg);
	}

	.alert-details {
		padding: 0 16px 16px 16px;
		display: flex;
		flex-direction: column;
		gap: 12px;
	}

	.alert-item {
		padding-top: 12px;
		border-top: 1px solid rgba(255, 255, 255, 0.1);
	}

	.item-title-row {
		display: flex;
		align-items: center;
		justify-content: space-between;
		margin-bottom: 4px;
	}

	.alert-item:first-child {
		border-top: none;
		padding-top: 0;
	}

	.item-title {
		font-size: 14px;
		font-weight: 700;
		color: white;
		margin-bottom: 4px;
	}

	.item-desc {
		font-size: 13px;
		color: rgba(255, 255, 255, 0.8);
		line-height: 1.4;
	}
</style>
