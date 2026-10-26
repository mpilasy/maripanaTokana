<!-- SPDX-FileCopyrightText: Orinasa Njarasoa -->
<!-- SPDX-License-Identifier: GPL-3.0-or-later -->
<script lang="ts">
	import type { Snippet } from 'svelte';
	import DualUnitText from './DualUnitText.svelte';
	import { longPress } from '#lib/actions/longPress.js';

	interface Props {
		title?: string;
		value: string;
		secondaryValue?: string;
		subtitle?: string;
		subtitleSnippet?: Snippet;
		trailingSnippet?: Snippet;
		/** A small graphic in the top corner; the value may run under it. */
		cornerSnippet?: Snippet;
		/** A dial in the bottom corner, in the space the value leaves free. */
		dialSnippet?: Snippet;
		onToggleUnits?: () => void;
		unit?: string;
		secondaryUnit?: string;
		/** Fills the tile from the bottom up to this percentage, like a water level. */
		fillPercent?: number;
		onClick?: () => void;
	}

	let { title, value, secondaryValue, subtitle, subtitleSnippet, trailingSnippet, cornerSnippet, dialSnippet, onToggleUnits, unit, secondaryUnit, fillPercent, onClick }: Props = $props();

	// A title cut short by the card's width shows in full while held
	let titleEl = $state<HTMLElement | null>(null);
	let titleCut = $state(false);
	let showFullTitle = $state(false);
	$effect(() => {
		const el = titleEl;
		if (!el) return;
		const measure = () => { titleCut = el.scrollWidth > el.clientWidth; };
		measure();
		const observer = new ResizeObserver(measure);
		observer.observe(el);
		return () => observer.disconnect();
	});
	$effect(() => {
		if (!showFullTitle) return;
		const hide = () => { showFullTitle = false; };
		const timer = setTimeout(hide, 4000);
		// The press that opened it is over; the next one anywhere closes it
		const listen = setTimeout(() => window.addEventListener('pointerdown', hide, { once: true }), 0);
		return () => { clearTimeout(timer); clearTimeout(listen); window.removeEventListener('pointerdown', hide); };
	});
</script>

<!-- svelte-ignore a11y_no_static_element_interactions -->
<!-- svelte-ignore a11y_no_noninteractive_tabindex -->
<div class="detail-card" class:clickable={onClick} class:has-dial={dialSnippet} role={onClick ? 'button' : undefined} tabindex={onClick ? 0 : undefined}
	onclick={onClick} onkeydown={(e) => { if (onClick && (e.key === 'Enter' || e.key === ' ')) { e.preventDefault(); onClick(); } }}
	style:background={fillPercent != null ? `linear-gradient(to top, rgba(100, 181, 246, 0.2), rgba(100, 181, 246, 0.1) ${Math.max(0, fillPercent - 10)}%, var(--surface-tile) ${fillPercent}%)` : undefined}>
	<div class="card-body">
		{#if title}
			<span class="card-title" class:beside-corner={cornerSnippet} bind:this={titleEl} title={titleCut ? title : undefined}
				use:longPress={titleCut ? () => (showFullTitle = true) : null}>{title}</span>
			{#if showFullTitle}
				<span class="full-title" role="tooltip">{title}</span>
			{/if}
		{/if}
		{#if secondaryValue}
			<DualUnitText
				primary={value}
				secondary={secondaryValue}
				primarySize="20px"
				onClick={onToggleUnits}
				primaryUnit={unit}
				secondaryUnit={secondaryUnit}
			/>
		{:else}
			<span class="card-value">{value}</span>
		{/if}
		{#if subtitleSnippet}
			{@render subtitleSnippet()}
		{:else if subtitle}
			<span class="card-subtitle">{subtitle}</span>
		{/if}
	</div>
	{#if trailingSnippet}
		{@render trailingSnippet()}
	{/if}
	{#if cornerSnippet}
		<div class="corner">{@render cornerSnippet()}</div>
	{/if}
	{#if dialSnippet}
		<div class="dial">{@render dialSnippet()}</div>
	{/if}
</div>

<style>
	.detail-card {
		background: var(--surface-tile);
		border-radius: 16px;
		padding: 16px;
		display: flex;
		align-items: center;
		gap: 8px;
		flex: 1;
		min-width: 0;
		position: relative;
	}

	.corner {
		position: absolute;
		top: 10px;
		inset-inline-end: 10px;
		pointer-events: none;
	}

	/* Tall enough for the dial to sit below the value instead of over it */
	.detail-card.has-dial {
		min-height: 150px;
	}

	.dial {
		position: absolute;
		bottom: 14px;
		inset-inline-end: 14px;
		pointer-events: none;
	}

	.detail-card.clickable {
		cursor: pointer;
	}

	.card-body {
		flex: 1;
		min-width: 0;
		display: flex;
		flex-direction: column;
		align-self: flex-start;
		gap: 4px;
	}

	.card-title {
		font-size: 13px;
		color: rgba(255,255,255,0.6);
		white-space: nowrap;
		overflow: hidden;
		text-overflow: ellipsis;
		margin-bottom: 2px;
	}

	/* Clear of the corner graphic */
	.card-title.beside-corner {
		padding-inline-end: 34px;
	}

	.card-value {
		font-family: var(--font-display);
		font-size: 20px;
		font-weight: 700;
		color: white;
		font-feature-settings: var(--font-features);
	}

	.full-title {
		position: absolute;
		top: 8px;
		left: 8px;
		right: 8px;
		z-index: 2;
		padding: 6px 10px;
		border-radius: 8px;
		background: #1A1565;
		border: 1px solid var(--hairline);
		color: white;
		font-size: 13px;
	}

	.card-subtitle {
		font-size: 12px;
		color: rgba(255,255,255,0.6);
	}
</style>
