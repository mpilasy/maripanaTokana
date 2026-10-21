<script lang="ts">
	import { _ } from 'svelte-i18n';
	import { weatherSource } from '$lib/stores/preferences';
	import type { WeatherSource } from '$lib/domain/weatherData';

	interface Props {
		fontName: string;
		currentFlag: string;
		onCycleFont: () => void;
		onCycleLanguage: () => void;
		onOpenAbout: () => void;
	}

	let { fontName, currentFlag, onCycleFont, onCycleLanguage, onOpenAbout }: Props = $props();

	const SOURCE_INFO: Record<WeatherSource, { label: string; url: string }> = {
		OPEN_METEO: { label: 'Open-Meteo', url: 'https://open-meteo.com' },
		PIRATE_WEATHER: { label: 'Pirate Weather', url: 'https://pirateweather.net' },
	};
</script>

<footer class="footer" dir="ltr">
	<button type="button" class="pill" onclick={onCycleFont} aria-label={$_('cd_change_font')}>
		<span class="icon">Aa</span>
		<span class="name">{fontName.split(' + ')[0]}</span>
	</button>
	<!-- Tapping the credits opens About; the source link handles its own clicks -->
	<!-- svelte-ignore a11y_click_events_have_key_events -->
	<!-- svelte-ignore a11y_no_static_element_interactions -->
	<div class="footer-credits" onclick={(e) => { if (!(e.target as HTMLElement).closest('a')) onOpenAbout(); }} title={$_('about_title')}>
		<span class="credit-text">
			{$_('credits_weather_data')}
			<a href={SOURCE_INFO[$weatherSource].url} target="_blank" rel="noopener">{SOURCE_INFO[$weatherSource].label}</a>
		</span>
		<span class="version">v1.5.16</span>
	</div>
	<button type="button" class="pill" onclick={onCycleLanguage} aria-label={$_('cd_change_language')}>
		<span class="flag">{currentFlag}</span>
	</button>
</footer>

<style>
	.footer {
		display: flex;
		align-items: center;
		gap: 8px;
		padding: 8px 0;
		padding-bottom: max(8px, env(safe-area-inset-bottom));
		flex-shrink: 0;
	}

	.pill {
		display: flex;
		align-items: center;
		gap: 6px;
		cursor: pointer;
		background: none;
		border: none;
		padding: 6px 8px;
		opacity: 0.7;
		border-radius: 999px;
		transition: background 0.2s, transform 0.1s;
		font-family: inherit;
		flex-shrink: 0;
	}

	.pill:hover {
		background: var(--surface-tile-highlight);
	}

	.pill:focus-visible {
		outline: 2px solid rgba(255, 255, 255, 0.5);
		outline-offset: 2px;
	}

	.pill:active {
		transform: scale(0.97);
	}

	.icon {
		font-size: 13px;
		color: rgba(255, 255, 255, 0.6);
	}

	.name {
		font-size: 11px;
		color: rgba(255, 255, 255, 0.6);
		white-space: nowrap;
	}

	.flag {
		font-size: 16px;
		line-height: 1;
	}

	.footer-credits {
		cursor: pointer;
		flex: 1;
		min-width: 0;
		text-align: center;
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: 2px;
	}

	.credit-text {
		font-size: 10px;
		line-height: 13px;
		color: rgba(255,255,255,0.3);
	}

	.credit-text a {
		color: rgba(255,255,255,0.5);
		text-decoration: underline;
	}

	.version {
		font-size: 9px;
		line-height: 12px;
		color: rgba(255,255,255,0.3);
	}
</style>
