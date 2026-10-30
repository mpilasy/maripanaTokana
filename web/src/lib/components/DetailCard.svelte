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
		/** A dial under the values, e.g. the wind compass. */
		dialSnippet?: Snippet;
		/** A dial with the reading in it, in the middle of the card; replaces the value. */
		centerDialSnippet?: Snippet;
		/** The card's bottom row, full width: e.g. a label with its value at the start, as on the sun tile. */
		footerSnippet?: Snippet;
		onToggleUnits?: () => void;
		unit?: string;
		secondaryUnit?: string;
		/** Fills the tile from the bottom up to this percentage, like a water level. */
		fillPercent?: number;
		onClick?: () => void;
	}

	let { title, value, secondaryValue, subtitle, subtitleSnippet, trailingSnippet, cornerSnippet, dialSnippet, centerDialSnippet, footerSnippet, onToggleUnits, unit, secondaryUnit, fillPercent, onClick }: Props = $props();

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
<div class="detail-card" class:clickable={onClick} role={onClick ? 'button' : undefined} tabindex={onClick ? 0 : undefined}
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
		<!-- The values: centred across and down the space under the title. A dial sits right under
		     the title instead, so the dials of two cards side by side line up whatever their bottom rows -->
		<div class="card-content" class:top={centerDialSnippet}>
		{#if centerDialSnippet}
			<!-- As wide as the card allows, up to 132 px, and square -->
			<!-- svelte-ignore a11y_click_events_have_key_events -->
			<div class="center-dial" onclick={onToggleUnits ? (e) => { e.stopPropagation(); onToggleUnits(); } : undefined}>
				{@render centerDialSnippet()}
			</div>
		{:else if secondaryValue}
			<DualUnitText
				primary={value}
				secondary={secondaryValue}
				primarySize="1.25rem"
				align="center"
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
		{#if dialSnippet}
			<div class="dial">{@render dialSnippet()}</div>
		{/if}
		{#if trailingSnippet}
			{@render trailingSnippet()}
		{/if}
		<!-- Under a dial, the bottom row sits in the middle of the space that is left -->
		{#if centerDialSnippet && footerSnippet}
			<div class="footer-middle">{@render footerSnippet()}</div>
		{/if}
		</div>
		{#if footerSnippet && !centerDialSnippet}
			<div class="footer">{@render footerSnippet()}</div>
		{/if}
	</div>
	{#if cornerSnippet}
		<div class="corner">{@render cornerSnippet()}</div>
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

	.dial {
		margin-top: 4px;
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
		align-self: stretch;
		gap: 4px;
	}

	.card-content {
		flex: 1;
		display: flex;
		flex-direction: column;
		align-items: center;
		justify-content: center;
		gap: 4px;
		text-align: center;
	}

	.card-title {
		font-size: 0.8125rem;
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
		font-size: 1.25rem;
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
		font-size: 0.8125rem;
	}

	.card-subtitle {
		font-size: 0.75rem;
		color: rgba(255,255,255,0.6);
		/* A subtitle may hold a label and its values on separate lines */
		white-space: pre-line;
	}

	.card-content.top {
		justify-content: flex-start;
	}

	.footer {
		margin-top: 2px;
	}

	.footer-middle {
		flex: 1;
		align-self: stretch;
		display: flex;
		flex-direction: column;
		justify-content: center;
		margin-top: 6px;
	}

	.center-dial {
		width: 100%;
		max-width: 132px;
	}
</style>
