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

<footer class="footer" dir="ltr">
	<!-- svelte-ignore a11y_click_events_have_key_events -->
	<!-- svelte-ignore a11y_no_static_element_interactions -->
	<div class="footer-font" onclick={onCycleFont}>
		<span class="font-icon">Aa</span>
		<span class="font-name">{fontName.replace(' + ', '\n')}</span>
	</div>

	<div class="footer-credits">
		<span class="credit-text">
			{$_('credits_weather_data')}
			<a href="https://open-meteo.com" target="_blank" rel="noopener">Open-Meteo</a>
		</span>
		<span class="version">v1.0.0</span>
	</div>

	<!-- svelte-ignore a11y_click_events_have_key_events -->
	<!-- svelte-ignore a11y_no_static_element_interactions -->
	<div class="footer-lang" onclick={onCycleLanguage}>
		<span class="flag">{currentFlag}</span>
	</div>
</footer>

<style>
	.footer {
		display: flex;
		align-items: center;
		padding: 4px 0;
		padding-bottom: max(4px, env(safe-area-inset-bottom));
		flex-shrink: 0;
	}

	.footer-font {
		display: flex;
		align-items: center;
		gap: 4px;
		cursor: pointer;
	}

	.font-icon {
		font-size: 14px;
		color: rgba(255,255,255,0.4);
	}

	.font-name {
		font-size: 9px;
		line-height: 11px;
		color: rgba(255,255,255,0.4);
		white-space: pre-line;
	}

	.footer-credits {
		flex: 1;
		text-align: center;
		display: flex;
		flex-direction: column;
		align-items: center;
	}

	.credit-text {
		font-size: 9px;
		line-height: 11px;
		color: rgba(255,255,255,0.3);
	}

	.credit-text a {
		color: rgba(255,255,255,0.5);
		text-decoration: underline;
	}

	.version {
		font-size: 9px;
		line-height: 11px;
		color: rgba(255,255,255,0.25);
	}

	.footer-lang {
		cursor: pointer;
	}

	.flag {
		font-size: 16px;
	}
</style>
