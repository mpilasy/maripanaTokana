<script lang="ts">
	interface Props {
		primary: string;
		secondary: string;
		primarySize?: string;
		align?: 'start' | 'end' | 'center';
		onClick?: () => void;
	}

	let { primary, secondary, primarySize = '16px', align = 'start', onClick }: Props = $props();
</script>

{#snippet content()}
	<div class="primary" style:font-size={primarySize}>{primary}</div>
	<div class="secondary" style:font-size="calc({primarySize} * 0.75)">{secondary}</div>
{/snippet}

{#if onClick}
	<button
		type="button"
		class="dual-unit clickable"
		style:text-align={align}
		onclick={onClick}
	>
		{@render content()}
	</button>
{:else}
	<div
		class="dual-unit"
		style:text-align={align}
	>
		{@render content()}
	</div>
{/if}

<style>
	.dual-unit {
		display: flex;
		flex-direction: column;
	}

	button.dual-unit {
		background: none;
		border: none;
		padding: 0;
		font: inherit;
		color: inherit;
		cursor: pointer;
		text-align: inherit;
	}

	button.dual-unit:focus-visible {
		outline: 2px solid rgba(255, 255, 255, 0.5);
		outline-offset: 4px;
		border-radius: 4px;
	}

	.primary {
		font-family: var(--font-display);
		font-weight: 700;
		color: white;
		font-feature-settings: var(--font-features);
	}

	.secondary {
		font-family: var(--font-display);
		color: rgba(255,255,255,0.55);
		font-feature-settings: var(--font-features);
	}
</style>
