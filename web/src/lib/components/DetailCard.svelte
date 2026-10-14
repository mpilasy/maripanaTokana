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
		onToggleUnits?: () => void;
		unit?: string;
		secondaryUnit?: string;
	}

	let { title, value, secondaryValue, subtitle, subtitleSnippet, trailingSnippet, onToggleUnits, unit, secondaryUnit }: Props = $props();
</script>

<div class="detail-card">
	<div class="card-body">
		{#if title}
			<span class="card-title">{title}</span>
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
