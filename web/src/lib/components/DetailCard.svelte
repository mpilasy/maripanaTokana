<script lang="ts">
	import type { Snippet } from 'svelte';
	import DualUnitText from './DualUnitText.svelte';

	interface Props {
		title?: string;
		value: string;
		secondaryValue?: string;
		subtitle?: string;
		subtitleSnippet?: Snippet;
		trailingSnippet?: Snippet;
		/** A small graphic in the top corner; the value may run under it. */
		cornerSnippet?: Snippet;
		onToggleUnits?: () => void;
		unit?: string;
		secondaryUnit?: string;
		/** Fills the tile from the bottom up to this percentage, like a water level. */
		fillPercent?: number;
		onClick?: () => void;
	}

	let { title, value, secondaryValue, subtitle, subtitleSnippet, trailingSnippet, cornerSnippet, onToggleUnits, unit, secondaryUnit, fillPercent, onClick }: Props = $props();
</script>

<!-- svelte-ignore a11y_no_static_element_interactions -->
<!-- svelte-ignore a11y_no_noninteractive_tabindex -->
<div class="detail-card" class:clickable={onClick} role={onClick ? 'button' : undefined} tabindex={onClick ? 0 : undefined}
	onclick={onClick} onkeydown={(e) => { if (onClick && (e.key === 'Enter' || e.key === ' ')) { e.preventDefault(); onClick(); } }}
	style:background={fillPercent != null ? `linear-gradient(to top, rgba(100, 181, 246, 0.16) ${fillPercent}%, var(--surface-tile) ${fillPercent}%)` : undefined}>
	<div class="card-body">
		{#if title}
			<span class="card-title" class:beside-corner={cornerSnippet}>{title}</span>
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

	.card-subtitle {
		font-size: 12px;
		color: rgba(255,255,255,0.6);
	}
</style>
