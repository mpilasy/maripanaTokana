<script lang="ts">
	import DualUnitText from './DualUnitText.svelte';

	interface Props {
		title: string;
		value: string;
		secondaryValue?: string;
		subtitle?: string;
		onToggleUnits?: () => void;
	}

	let { title, value, secondaryValue, subtitle, onToggleUnits }: Props = $props();
</script>

<div class="detail-card">
	<span class="card-title">{title}</span>
	{#if secondaryValue}
		<DualUnitText
			primary={value}
			secondary={secondaryValue}
			primarySize="20px"
			onClick={onToggleUnits}
		/>
	{:else}
		<span class="card-value">{value}</span>
	{/if}
	{#if subtitle}
		<span class="card-subtitle">{subtitle}</span>
	{/if}
</div>

<style>
	.detail-card {
		background: rgba(42, 31, 165, 0.6);
		border-radius: 16px;
		padding: 16px;
		display: flex;
		flex-direction: column;
		gap: 8px;
		flex: 1;
	}

	.card-title {
		font-size: 14px;
		color: rgba(255,255,255,0.7);
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
