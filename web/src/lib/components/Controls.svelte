<script lang="ts">
	interface Props {
		fontName: string;
		currentFlag: string;
		onCycleFont: () => void;
		onCycleLanguage: () => void;
	}

	let { fontName, currentFlag, onCycleFont, onCycleLanguage }: Props = $props();
</script>

<div class="controls" dir="ltr">
	<!-- svelte-ignore a11y_click_events_have_key_events -->
	<!-- svelte-ignore a11y_no_static_element_interactions -->
	<div class="control-item" onclick={onCycleFont}>
		<span class="icon">Aa</span>
		<span class="name">{fontName.replace(' + ', '\n')}</span>
	</div>

	<div class="spacer"></div>

	<!-- svelte-ignore a11y_click_events_have_key_events -->
	<!-- svelte-ignore a11y_no_static_element_interactions -->
	<div class="control-item" onclick={onCycleLanguage}>
		<span class="flag">{currentFlag}</span>
	</div>
</div>

<style>
	.controls {
		display: flex;
		align-items: center;
		padding: 8px 0;
		flex-shrink: 0;
	}

	.control-item {
		display: flex;
		align-items: center;
		gap: 6px;
		cursor: pointer;
		background: rgba(255, 255, 255, 0.1);
		padding: 4px 10px;
		border-radius: 20px;
		transition: background 0.2s;
	}

	.control-item:hover {
		background: rgba(255, 255, 255, 0.15);
	}

	.control-item:active {
		transform: scale(0.97);
	}

	.icon {
		font-size: 14px;
		color: rgba(255, 255, 255, 0.8);
	}

	.name {
		font-size: 10px;
		line-height: 1.1;
		color: rgba(255, 255, 255, 0.8);
		white-space: pre-line;
		font-weight: 500;
	}

	.flag {
		font-size: 18px;
	}

	.spacer {
		flex: 1;
	}
</style>
