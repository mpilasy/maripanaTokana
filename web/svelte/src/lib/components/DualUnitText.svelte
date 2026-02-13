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

<!-- svelte-ignore a11y_click_events_have_key_events -->
<!-- svelte-ignore a11y_no_static_element_interactions -->
<div
	class="dual-unit"
	style:text-align={align}
	onclick={onClick}
	class:clickable={!!onClick}
>
	<div class="primary" style:font-size={primarySize}>{primary}</div>
	<div class="secondary" style:font-size="calc({primarySize} * 0.75)">{secondary}</div>
</div>

<style>
	.dual-unit {
		display: flex;
		flex-direction: column;
	}

	.dual-unit.clickable {
		cursor: pointer;
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
