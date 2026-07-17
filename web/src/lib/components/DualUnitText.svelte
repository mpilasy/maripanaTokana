<script lang="ts">
	import { _ } from 'svelte-i18n';
	interface Props {
		primary: string;
		secondary: string;
		primarySize?: string;
		align?: 'start' | 'end' | 'center';
		onClick?: () => void;
		primaryUnit?: string;
		secondaryUnit?: string;
	}

	let { primary, secondary, primarySize = '16px', align = 'start', onClick, primaryUnit, secondaryUnit }: Props = $props();
</script>

{#snippet content()}
	<div class="primary" style:font-size={primarySize}
		>{primary}{#if primaryUnit}<span class="unit" style:font-size="calc({primarySize} * 0.55)"> {primaryUnit}</span>{/if}</div
	>
	<div class="secondary" style:font-size="calc({primarySize} * 0.75)"
		>{secondary}{#if secondaryUnit}<span class="unit" style:font-size="calc({primarySize} * 0.55)"> {secondaryUnit}</span>{/if}</div
	>
{/snippet}

{#if onClick}
	<button
		type="button"
		class="dual-unit clickable"
		style:text-align={align}
		onclick={onClick}
		aria-label={$_('android_only.cd_toggle_units')}
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
		background: none;
		border: none;
		padding: 0;
		font-family: inherit;
		color: inherit;
	}

	.dual-unit.clickable {
		cursor: pointer;
	}

	.dual-unit.clickable:focus-visible {
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

	.unit {
		font-weight: 400;
		opacity: 0.75;
	}
</style>
