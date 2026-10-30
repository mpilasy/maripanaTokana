<!-- SPDX-FileCopyrightText: Orinasa Njarasoa -->
<!-- SPDX-License-Identifier: GPL-3.0-or-later -->
<script lang="ts">
	import type { WeatherAlert } from '#lib/domain/weatherData.js';
	import { slide } from 'svelte/transition';
	import { _ } from 'svelte-i18n';
	import { localeIndex } from '#lib/stores/preferences.js';
	import { SUPPORTED_LOCALES } from '#lib/i18n/locales.js';
	import { formatAlertTime } from '#lib/utils/date.js';
	import { motionDuration } from '#lib/utils/motion.js';

	interface Props {
		alerts: WeatherAlert[];
		expanded?: boolean;
		onToggle?: () => void;
	}

	let { alerts, expanded = false, onToggle }: Props = $props();
	let expandedItems = $state<Set<number>>(new Set());

	function toggleItem(index: number) {
		const next = new Set(expandedItems);
		if (next.has(index)) next.delete(index);
		else next.add(index);
		expandedItems = next;
	}

	// Determine the most severe level present
	let levels = $derived(alerts.map(a => a.level));
	let topLevel = $derived(levels.includes('emergency') ? 'emergency'
		: levels.includes('warning') ? 'warning'
		: 'watch');

	let topAlert = $derived(alerts.find(a => a.level === topLevel) || alerts[0]);
	let localeTag = $derived(SUPPORTED_LOCALES[$localeIndex].tag);

	const LEVEL_ICON: Record<string, string> = { watch: '⚠️', warning: '❗', emergency: '🚨' };

	// Enter or Space opens a header or a row, as on a button. Keys on the source links inside it stay with the links.
	function onKey(e: KeyboardEvent, toggle: () => void) {
		if (e.target !== e.currentTarget || (e.key !== 'Enter' && e.key !== ' ')) return;
		e.preventDefault();
		toggle();
	}

	const LEVEL_RANK: Record<string, number> = { watch: 0, warning: 1, emergency: 2 };
	let distinctSources = $derived([...new Set(alerts.map(a => a.source))]);
	// One representative alert per source (the most severe) to back that source's badge link.
	let sourceRepresentatives = $derived(
		distinctSources.map(source => {
			const forSource = alerts.filter(a => a.source === source);
			return forSource.reduce((best, a) => LEVEL_RANK[a.level] > LEVEL_RANK[best.level] ? a : best, forSource[0]);
		})
	);
</script>

{#if alerts.length > 0 && topAlert}
	<div class="alert-banner level-{topLevel}">
		<!-- The header holds the source links, so it is a role="button" and not a <button> -->
		<div class="banner-header" role="button" tabindex="0" aria-expanded={expanded} onclick={() => onToggle?.()} onkeydown={(e) => onKey(e, () => onToggle?.())}>
			<div class="icon-text">
				<span class="alert-icon" aria-hidden="true">{LEVEL_ICON[topLevel]}</span>
				<span class="level-label level-{topLevel}">{$_(`alert_level_${topLevel}`)}</span>
				<span class="alert-title">
					{#if alerts.length > 1}
						{$_('alert_count_title', { values: { count: alerts.length } })}
					{:else}
						{topAlert.title}
					{/if}
				</span>
				{#if distinctSources.length > 1}
					<span class="source-badges">
						{#each sourceRepresentatives as rep (rep.source)}
							{#if rep.link}
								<a href={rep.link} target="_blank" rel="noopener noreferrer" class="source-badge clickable touch-target" onclick={(e) => e.stopPropagation()}>
									{rep.source.toUpperCase()}
								</a>
							{:else}
								<span class="source-badge">{rep.source.toUpperCase()}</span>
							{/if}
						{/each}
					</span>
				{:else if topAlert.link}
					<a href={topAlert.link} target="_blank" rel="noopener noreferrer" class="source-badge clickable touch-target" onclick={(e) => e.stopPropagation()}>
						{topAlert.source.toUpperCase()}
					</a>
				{:else}
					<span class="source-badge">{topAlert.source.toUpperCase()}</span>
				{/if}
			</div>
			<span class="spacer"></span>
			<span class="chevron" class:expanded={expanded} aria-hidden="true">&#9660;</span>
		</div>

		{#if expanded}
			<div class="alert-details" transition:slide={{ duration: motionDuration(300) }}>
				{#each alerts as alert, index}
					{@const collapsible = alerts.length > 1}
					{@const itemExpanded = !collapsible || expandedItems.has(index)}
					<div class="alert-item">
						<!-- The row is a button only when it folds; svelte-check cannot see the dynamic role -->
						<!-- svelte-ignore a11y_no_static_element_interactions, a11y_no_noninteractive_tabindex -->
						<div class="item-title-row" class:clickable-row={collapsible}
							role={collapsible ? 'button' : undefined} tabindex={collapsible ? 0 : undefined} aria-expanded={collapsible ? itemExpanded : undefined}
							onclick={() => collapsible && toggleItem(index)} onkeydown={(e) => collapsible && onKey(e, () => toggleItem(index))}>
							<div class="item-title-group">
								{#if alerts.length > 1}
									<span class="alert-number level-{alert.level}" aria-hidden="true">{index + 1}</span>
									<span class="level-label level-{alert.level}">{$_(`alert_level_${alert.level}`)}</span>
								{/if}
								<div class="item-title">{alert.title}</div>
							</div>
							<div class="item-title-end">
								{#if alert.link}
									<a href={alert.link} target="_blank" rel="noopener noreferrer" class="source-badge clickable touch-target" onclick={(e) => e.stopPropagation()}>
										{alert.source.toUpperCase()}
									</a>
								{:else}
									<span class="source-badge">{alert.source.toUpperCase()}</span>
								{/if}
								{#if collapsible}
									<span class="chevron item-chevron" class:expanded={itemExpanded} aria-hidden="true">&#9660;</span>
								{/if}
							</div>
						</div>

						{#if alert.time}
							<div class="alert-time">{formatAlertTime(alert.time, localeTag)}</div>
						{/if}

						{#if itemExpanded}
							<div transition:slide={{ duration: motionDuration(200) }}>
								{#if alert.headline}
									<div class="alert-headline">{alert.headline}</div>
								{/if}
								<div class="item-desc">{alert.description}</div>
							</div>
						{/if}
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

	/* The level colours, as on Android (WeatherAlertBanner.kt). Dark text on them is at least 5:1. */
	.level-watch { --level: #FFA500; }
	.level-warning { --level: #FF4444; }
	.level-emergency { --level: #E040FB; }

	.alert-banner {
		border-left-color: var(--level);
		background: color-mix(in srgb, var(--level) 15%, transparent);
	}

	.level-label {
		flex-shrink: 0;
		background: var(--level);
		color: #1A1A1A;
		padding: 2px 6px;
		border-radius: 4px;
		font-size: 0.6875rem;
		font-weight: 800;
		text-transform: uppercase;
		letter-spacing: 0.5px;
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

	.banner-header:focus-visible, .item-title-row:focus-visible {
		outline: 2px solid rgba(255, 255, 255, 0.7);
		border-radius: 12px;
	}

	.banner-header:focus-visible {
		outline-offset: -2px;
	}

	.item-title-row:focus-visible {
		outline-offset: 4px;
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
		font-size: 1.125rem;
	}

	.alert-title {
		font-size: 1rem;
		font-weight: 700;
		color: white;
	}

	.source-badges {
		display: inline-flex;
		gap: 4px;
	}

	/* White on a dark tint, or dark on white for a link: both at least 4.5:1 on every banner colour */
	.source-badge {
		background: rgba(0, 0, 0, 0.3);
		padding: 2px 6px;
		border-radius: 4px;
		font-size: 0.625rem;
		font-weight: 800;
		color: white;
		margin-left: 4px;
		letter-spacing: 0.5px;
		text-decoration: none;
	}

	.source-badges .source-badge {
		margin-left: 0;
	}

	.source-badge.clickable {
		background: rgba(255, 255, 255, 0.85);
		color: #1A1A1A;
		transition: background 0.2s;
	}

	.source-badge.clickable:hover {
		background: white;
	}

	.chevron {
		color: rgba(255, 255, 255, 0.7);
		font-size: 0.75rem;
		transition: transform 0.3s ease;
		transform: rotate(-90deg);
	}

	.chevron.expanded {
		transform: rotate(0deg);
	}

	.item-chevron {
		font-size: 0.625rem;
		color: rgba(255, 255, 255, 0.5);
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

	.item-title-row.clickable-row {
		cursor: pointer;
		user-select: none;
	}

	.item-title-group {
		display: flex;
		align-items: center;
		gap: 8px;
	}

	.item-title-end {
		display: flex;
		align-items: center;
		gap: 6px;
	}

	.alert-number {
		display: inline-flex;
		align-items: center;
		justify-content: center;
		width: 18px;
		height: 18px;
		border-radius: 50%;
		font-size: 0.625rem;
		font-weight: 800;
		color: #1A1A1A;
		background: var(--level);
	}

	.alert-item:first-child {
		border-top: none;
		padding-top: 0;
	}

	.item-title {
		font-size: 0.875rem;
		font-weight: 700;
		color: white;
	}

	.alert-time {
		font-size: 0.6875rem;
		color: rgba(255, 255, 255, 0.5);
		margin-bottom: 4px;
	}

	.alert-headline {
		font-size: 0.875rem;
		font-weight: 700;
		color: white;
		margin-bottom: 6px;
	}

	.item-desc {
		font-size: 0.8125rem;
		color: rgba(255, 255, 255, 0.8);
		line-height: 1.4;
	}
</style>
