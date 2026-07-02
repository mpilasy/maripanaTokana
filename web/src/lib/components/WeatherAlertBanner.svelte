<script module lang="ts">
	let alertIdCounter = 0;
</script>

<script lang="ts">
	import type { WeatherAlert } from '$lib/domain/weatherData';
	import { slide } from 'svelte/transition';
	import { localeIndex } from '$lib/stores/preferences';
	import { SUPPORTED_LOCALES } from '$lib/i18n/locales';
	import { formatAlertTime } from '$lib/utils/date';

	interface Props {
		alerts: WeatherAlert[];
	}

	let { alerts }: Props = $props();
	let isExpanded = $state(false);

	let contentId = `alert-details-${alertIdCounter++}`;

	// Determine the most severe level present
	let levels = $derived(alerts.map(a => a.level));
	let topLevel = $derived(levels.includes('emergency') ? 'emergency'
		: levels.includes('warning') ? 'warning'
		: 'watch');

	let topAlert = $derived(alerts.find(a => a.level === topLevel) || alerts[0]);
	let localeTag = $derived(SUPPORTED_LOCALES[$localeIndex].tag);
</script>

{#if alerts.length > 0 && topAlert}
	<div class="alert-banner" class:watch={topLevel === 'watch'} class:warning={topLevel === 'warning' || topLevel === 'emergency'}>
		<button
			type="button"
			class="banner-header"
			onclick={() => isExpanded = !isExpanded}
			aria-expanded={isExpanded}
			aria-controls={contentId}
		>
			<div class="icon-text">
				<span class="alert-icon">
					{#if topLevel === 'watch'}
						&#9888;
					{:else}
						&#10071;
					{/if}
				</span>
				<span class="alert-title">{topAlert.title}</span>
				{#if alerts.length > 1}
					<span class="alert-count">&#9888; {alerts.length}</span>
				{/if}
				<span class="source-badge">{topAlert.source.toUpperCase()}</span>
			</div>
			<span class="spacer"></span>
			<span class="chevron" class:expanded={isExpanded}>&#9660;</span>
		</button>

		{#if isExpanded}
			<div id={contentId} class="alert-details" transition:slide={{ duration: 300 }}>
				{#each alerts as alert, index}
					<div class="alert-item">
						<div class="item-title-row">
							<div class="item-title-group">
								{#if alerts.length > 1}
									<span class="alert-number" class:watch={alert.level === 'watch'} class:warning={alert.level !== 'watch'}>
										{index + 1}
									</span>
								{/if}
								<div class="item-title">{alert.title}</div>
							</div>
							{#if alert.link}
								<a href={alert.link} target="_blank" rel="noopener noreferrer" class="source-badge clickable" onclick={(e) => e.stopPropagation()}>
									{alert.source.toUpperCase()}
								</a>
							{:else}
								<span class="source-badge">{alert.source.toUpperCase()}</span>
							{/if}
						</div>
						
						{#if alert.time}
							<div class="alert-time">{formatAlertTime(alert.time, localeTag)}</div>
						{/if}

						{#if alert.headline}
							<div class="alert-headline">{alert.headline}</div>
						{/if}

						<div class="item-desc">{alert.description}</div>
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
		width: 100%;
		border: none;
		background: transparent;
		font: inherit;
		color: inherit;
		text-align: left;
		box-sizing: border-box;
	}

	.banner-header:focus-visible {
		outline: 2px solid rgba(255, 255, 255, 0.5);
		outline-offset: -2px;
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

	.alert-count {
		font-size: 14px;
		font-weight: 800;
		color: #FF4444;
		margin-left: 8px;
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
		text-decoration: none;
	}

	.source-badge.clickable {
		background: rgba(255, 255, 255, 0.4);
		transition: background 0.2s;
	}

	.source-badge.clickable:hover {
		background: rgba(255, 255, 255, 0.6);
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

	.item-title-group {
		display: flex;
		align-items: center;
		gap: 8px;
	}

	.alert-number {
		display: inline-flex;
		align-items: center;
		justify-content: center;
		width: 18px;
		height: 18px;
		border-radius: 50%;
		font-size: 10px;
		font-weight: 800;
		color: white;
	}

	.alert-number.watch {
		background: #FFA500;
	}

	.alert-number.warning {
		background: #FF4444;
	}

	.alert-item:first-child {
		border-top: none;
		padding-top: 0;
	}

	.item-title {
		font-size: 14px;
		font-weight: 700;
		color: white;
	}

	.alert-time {
		font-size: 11px;
		color: rgba(255, 255, 255, 0.5);
		margin-bottom: 4px;
	}

	.alert-headline {
		font-size: 14px;
		font-weight: 700;
		color: white;
		margin-bottom: 6px;
	}

	.item-desc {
		font-size: 13px;
		color: rgba(255, 255, 255, 0.8);
		line-height: 1.4;
	}
</style>
