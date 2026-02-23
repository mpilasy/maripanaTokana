<script lang="ts">
	import { _ } from 'svelte-i18n';
	import { weatherState, isRefreshing, doFetchWeather } from '$lib/stores/weather';
	import { metricPrimary, fontIndex, localeIndex, toggleUnits, cycleFont, cycleLanguage } from '$lib/stores/preferences';
	import { SUPPORTED_LOCALES, localizeDigits } from '$lib/i18n/index';
	import { fontPairings } from '$lib/fonts';
	import HeroCard from './HeroCard.svelte';
	import HourlyForecast from './HourlyForecast.svelte';
	import DailyForecast from './DailyForecast.svelte';
	import CurrentConditions from './CurrentConditions.svelte';
	import CollapsibleSection from './CollapsibleSection.svelte';
	import Footer from './Footer.svelte';
	import { captureAndShare } from '$lib/share';
	import { onMount } from 'svelte';

	// Browser locale detection for secondary language on error screen
	function findBrowserLocaleTag(): string | null {
		if (typeof navigator === 'undefined') return null;
		const lang = navigator.language.split('-')[0].toLowerCase();
		return SUPPORTED_LOCALES.find(l => l.tag === lang)?.tag ?? null;
	}

	const browserLocaleTag = findBrowserLocaleTag();
	let browserStrings = $state<Record<string, string> | null>(null);

	if (browserLocaleTag) {
		// eslint-disable-next-line @typescript-eslint/no-explicit-any
		const loaders: Record<string, () => Promise<{ default: any }>> = {
			en: () => import('$lib/i18n/locales/en.json'),
			mg: () => import('$lib/i18n/locales/mg.json'),
			ar: () => import('$lib/i18n/locales/ar.json'),
			es: () => import('$lib/i18n/locales/es.json'),
			fr: () => import('$lib/i18n/locales/fr.json'),
			hi: () => import('$lib/i18n/locales/hi.json'),
			ne: () => import('$lib/i18n/locales/ne.json'),
			zh: () => import('$lib/i18n/locales/zh.json'),
		};
		loaders[browserLocaleTag]?.().then(mod => {
			const { web_only, android_only, ...shared } = mod.default;
			browserStrings = { ...shared, ...web_only };
		});
	}

	let showSecondary = $derived(
		browserLocaleTag != null &&
		browserLocaleTag !== SUPPORTED_LOCALES[$localeIndex]?.tag &&
		browserStrings != null
	);

	let pullStartY = $state(0);
	let pullDelta = $state(0);
	let isPulling = $state(false);
	let scrollContainer = $state<HTMLElement | null>(null);
	let headerEl = $state<HTMLElement | null>(null);

	function loc(s: string): string {
		return localizeDigits(s, SUPPORTED_LOCALES[$localeIndex]);
	}

	function formatDate(timestamp: number): string {
		const locale = SUPPORTED_LOCALES[$localeIndex];
		const d = new Date(timestamp);
		// Use Intl for day/month names in correct locale, but localize digits ourselves
		const formatted = new Intl.DateTimeFormat(locale.tag, {
			weekday: 'long', day: 'numeric', month: 'long', year: 'numeric'
		}).format(d);
		return formatted;
	}

	function formatTime(timestamp: number): string {
		const d = new Date(timestamp);
		const hh = String(d.getHours()).padStart(2, '0');
		const mm = String(d.getMinutes()).padStart(2, '0');
		return `${hh}:${mm}`;
	}

	function handleTouchStart(e: TouchEvent) {
		if (scrollContainer && scrollContainer!.scrollTop <= 0) {
			pullStartY = e.touches[0].clientY;
			isPulling = true;
		}
	}

	function handleTouchMove(e: TouchEvent) {
		if (!isPulling) return;
		pullDelta = Math.max(0, e.touches[0].clientY - pullStartY);
	}

	function handleTouchEnd() {
		if (pullDelta > 80) {
			doFetchWeather();
		}
		pullDelta = 0;
		isPulling = false;
	}

	function handleShare(el: HTMLElement) {
		if (!headerEl) return;
		captureAndShare(headerEl, el);
	}

	onMount(() => {
		doFetchWeather();
	});
</script>

<div class="weather-screen">
	<!-- Blue Marble background -->
	<div class="bg-marble"></div>

	{#if $weatherState.kind === 'loading'}
		<div class="center">
			<div class="spinner"></div>
		</div>

	{:else if $weatherState.kind === 'success'}
		{@const data = $weatherState.data}
		<!-- Pull indicator -->
		{#if pullDelta > 0}
			<div class="pull-indicator" style:transform="translateY({Math.min(pullDelta, 100)}px)">
				<div class="pull-spinner" class:active={pullDelta > 80}></div>
			</div>
		{/if}

		{#if $isRefreshing}
			<div class="refresh-bar">
				<div class="refresh-spinner"></div>
			</div>
		{/if}

		<div class="content-wrapper">
			<!-- Fixed header (location + date captured for share screenshots) -->
			<div class="header">
				<div bind:this={headerEl}>
					<h1 class="location-name">{data.locationName}</h1>
					<p class="date">{formatDate(data.timestamp)}</p>
				</div>
				<!-- svelte-ignore a11y_click_events_have_key_events -->
			<!-- svelte-ignore a11y_no_static_element_interactions -->
			<p class="updated" onclick={doFetchWeather}>{$_('updated_time', { values: { time: loc(formatTime(data.timestamp)) } })}</p>
			</div>

			<!-- svelte-ignore a11y_no_static_element_interactions -->
			<!-- Scrollable content -->
			<div
				class="scroll-area"
				role="region"
				bind:this={scrollContainer}
				ontouchstart={handleTouchStart}
				ontouchmove={handleTouchMove}
				ontouchend={handleTouchEnd}
			>
				<HeroCard {data} metricPrimary={$metricPrimary} {loc} onToggleUnits={toggleUnits} onShare={handleShare} />

				{#if data.hourlyForecast.length > 0}
					<CollapsibleSection title={$_('section_hourly_forecast')} expanded={true} onShare={handleShare}>
						<HourlyForecast
							forecasts={data.hourlyForecast}
							metricPrimary={$metricPrimary}
							dailySunrise={data.dailySunrise}
							dailySunset={data.dailySunset}
							{loc}
							onToggleUnits={toggleUnits}
						/>
					</CollapsibleSection>
				{/if}

				{#if data.dailyForecast.length > 0}
					<CollapsibleSection title={$_('section_this_week')} onShare={handleShare}>
						<DailyForecast
							forecasts={data.dailyForecast}
							metricPrimary={$metricPrimary}
							localeTag={SUPPORTED_LOCALES[$localeIndex].tag}
							{loc}
							onToggleUnits={toggleUnits}
						/>
					</CollapsibleSection>
				{/if}

				<CollapsibleSection title={$_('section_current_conditions')} onShare={handleShare}>
					<CurrentConditions {data} metricPrimary={$metricPrimary} {loc} onToggleUnits={toggleUnits} />
				</CollapsibleSection>

				<div class="scroll-bottom-pad"></div>
			</div>

			<!-- Fixed footer -->
			<Footer
				fontName={fontPairings[$fontIndex].name}
				currentFlag={SUPPORTED_LOCALES[$localeIndex].flag}
				onCycleFont={cycleFont}
				onCycleLanguage={cycleLanguage}
			/>
		</div>

	{:else if $weatherState.kind === 'error'}
		<div class="center error-state">
			<h2>{$_('error_title')}</h2>
			<p>{$_($weatherState.message)}</p>
			{#if showSecondary && browserStrings}
				<div class="secondary-block">
					<h3>{browserStrings.error_title}</h3>
					<p>{browserStrings[$weatherState.message]}</p>
				</div>
			{/if}
			<button onclick={doFetchWeather}>
				{$_('error_retry')}
				{#if showSecondary && browserStrings}
					<span class="btn-secondary">{browserStrings.error_retry}</span>
				{/if}
			</button>
		</div>
	{/if}
</div>

<style>
	.weather-screen {
		width: 100%;
		height: 100%;
		position: relative;
	}

	.bg-marble {
		position: absolute;
		inset: 0;
		background: url('/bg-blue-marble.webp') center/cover no-repeat;
		opacity: 0.12;
		pointer-events: none;
	}

	.center {
		display: flex;
		flex-direction: column;
		align-items: center;
		justify-content: center;
		height: 100%;
		gap: 16px;
	}

	.spinner {
		width: 40px;
		height: 40px;
		border: 3px solid rgba(255,255,255,0.2);
		border-top-color: white;
		border-radius: 50%;
		animation: spin 0.8s linear infinite;
	}

	@keyframes spin { to { transform: rotate(360deg); } }

	.content-wrapper {
		display: flex;
		flex-direction: column;
		height: 100%;
		position: relative;
		z-index: 1;
		padding: 0 24px;
		padding-top: env(safe-area-inset-top);
	}

	.header {
		padding-top: 24px;
		flex-shrink: 0;
	}

	.location-name {
		font-family: var(--font-display);
		font-size: 32px;
		font-weight: 700;
		color: white;
	}

	.date {
		font-size: 16px;
		color: rgba(255,255,255,0.7);
	}

	.updated {
		font-size: 12px;
		color: rgba(255,255,255,0.4);
		cursor: pointer;
	}

	.scroll-area {
		flex: 1;
		min-width: 0;
		overflow-y: auto;
		overflow-x: hidden;
		padding: 24px 0;
		-webkit-overflow-scrolling: touch;
	}

	.scroll-bottom-pad {
		height: 24px;
	}

	.pull-indicator {
		position: absolute;
		top: 0;
		left: 50%;
		transform: translateX(-50%);
		z-index: 10;
	}

	.pull-spinner {
		width: 24px;
		height: 24px;
		border: 2px solid rgba(255,255,255,0.3);
		border-top-color: white;
		border-radius: 50%;
		transition: opacity 0.2s;
	}

	.pull-spinner.active {
		animation: spin 0.8s linear infinite;
	}

	.refresh-bar {
		position: absolute;
		top: env(safe-area-inset-top);
		left: 50%;
		transform: translateX(-50%);
		z-index: 10;
		padding: 8px;
	}

	.refresh-spinner {
		width: 20px;
		height: 20px;
		border: 2px solid rgba(255,255,255,0.3);
		border-top-color: white;
		border-radius: 50%;
		animation: spin 0.8s linear infinite;
	}

	.secondary-block {
		opacity: 0.45;
		text-align: center;
		padding: 0 16px;
	}

	.secondary-block h3 {
		font-family: var(--font-display);
		font-size: 16px;
		font-weight: 600;
		margin-bottom: 8px;
	}

	.secondary-block p {
		font-size: 13px;
		line-height: 1.4;
	}

	.btn-secondary {
		display: block;
		font-size: 11px;
		opacity: 0.4;
		margin-top: 4px;
		font-weight: 400;
		color: inherit;
	}

	.error-state h2 {
		font-family: var(--font-display);
		font-size: 24px;
		font-weight: 700;
	}

	.error-state p {
		color: rgba(255,255,255,0.7);
	}

	.error-state button {
		margin-top: 24px;
		padding: 16px 48px;
		background: white;
		color: #0E0B3D;
		border: none;
		border-radius: 14px;
		font-size: 18px;
		font-weight: 700;
		cursor: pointer;
		transition: background 0.2s, transform 0.1s;
		box-shadow: 0 4px 20px rgba(255,255,255,0.25);
	}

	.error-state button:hover {
		background: rgba(255,255,255,0.9);
	}

	.error-state button:active {
		transform: scale(0.97);
	}
</style>
