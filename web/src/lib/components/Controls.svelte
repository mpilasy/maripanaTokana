<script lang="ts">
	import { _ } from 'svelte-i18n';
	interface Props {
		fontName: string;
		currentFlag: string;
		onCycleFont: () => void;
		onCycleLanguage: () => void;
	}

	let { fontName, currentFlag, onCycleFont, onCycleLanguage }: Props = $props();
</script>

<div class="controls" dir="ltr">
	<button type="button" class="control-item" onclick={onCycleFont} aria-label={$_('android_only.cd_change_font')}>
		<span class="icon">Aa</span>
		<span class="name">{fontName.replace(' + ', '\n')}</span>
	</button>

	<div class="spacer"></div>

	<button type="button" class="control-item" onclick={onCycleLanguage} aria-label={$_('android_only.cd_change_language')}>
		<span class="flag">{currentFlag}</span>
	</button>
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
		border: none;
		padding: 4px 10px;
		border-radius: 20px;
		transition: background 0.2s, transform 0.1s;
		font-family: inherit;
	}

	.control-item:hover {
		background: rgba(255, 255, 255, 0.15);
	}

	.control-item:focus-visible {
		outline: 2px solid rgba(255, 255, 255, 0.5);
		outline-offset: 2px;
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
