<script lang="ts">
	import { _ } from 'svelte-i18n';
	import { weatherState, isRefreshing, doFetchWeather } from '$lib/stores/weather';
	import { 
		onLocationClicked, 
		showGpsCoordinates, 
		showLocationOverrideDialog, 
		devModeActive,
		initDevMode,
		disableDevMode,
		openLocationOverride
	} from '$lib/stores/devMode';
	import LocationOverrideDialog from './LocationOverrideDialog.svelte';
	import { metricPrimary, fontIndex, localeIndex, toggleUnits, cycleFont, cycleLanguage } from '$lib/stores/preferences';
	import { SUPPORTED_LOCALES, localizeDigits } from '$lib/i18n/index';
	import { fontPairings } from '$lib/fonts';
	import { formatDate } from '$lib/utils/date';
	import HeroCard from './HeroCard.svelte';
	import WeatherAlertBanner from './WeatherAlertBanner.svelte';
	import HourlyForecast from './HourlyForecast.svelte';
	import DailyForecast from './DailyForecast.svelte';
	import CurrentConditions from './CurrentConditions.svelte';
	import CollapsibleSection from './CollapsibleSection.svelte';
	import Controls from './Controls.svelte';
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

	onMount(() => {
		initDevMode();
	});

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

	function formatDMS(value: number, positive: string, negative: string): string {
		const direction = value >= 0 ? positive : negative;
		const absolute = Math.abs(value);
		const degrees = Math.floor(absolute);
		const minutesTotal = (absolute - degrees) * 60;
		const minutes = Math.floor(minutesTotal);
		const seconds = Math.floor((minutesTotal - minutes) * 60);
		return `${degrees}\u00B0${minutes.toString().padStart(2, '0')}'${seconds.toString().padStart(2, '0')}"${direction}`;
	}
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
			<Controls
				fontName={fontPairings[$fontIndex].name}
				currentFlag={SUPPORTED_LOCALES[$localeIndex].flag}
				onCycleFont={cycleFont}
				onCycleLanguage={cycleLanguage}
			/>

			<!-- Fixed header (location + date captured for share screenshots) -->
			<div class="header">
				<!-- svelte-ignore a11y_click_events_have_key_events -->
				<!-- svelte-ignore a11y_no_static_element_interactions -->
				<div 
					bind:this={headerEl} 
					onclick={onLocationClicked} 
					style="cursor: pointer;"
				>
					<div class="location-header">
						<div class="location-primary-row">
							<h1 class="location-name">
								{#if !data.locationSubtext && data.locationName.includes(',')}
									{data.locationName.split(',')[0].trim()}
								{:else}
									{data.locationName}
								{/if}
							</h1>
							{#if $devModeActive}
								<!-- svelte-ignore a11y_click_events_have_key_events -->
								<!-- svelte-ignore a11y_no_static_element_interactions -->
								<span 
									class="edit-icon" 
									onclick={(e) => { e.stopPropagation(); openLocationOverride(); }}
								>
									<svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor">
										<path d="M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04c.39-.39.39-1.02 0-1.41l-2.34-2.34c-.39-.39-1.02-.39-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z"/>
									</svg>
								</span>
							{/if}
							
							{#if $devModeActive}
								<!-- svelte-ignore a11y_click_events_have_key_events -->
								<!-- svelte-ignore a11y_no_static_element_interactions -->
								<span 
									class="dev-badge" 
									onclick={(e) => { e.stopPropagation(); disableDevMode(); }}
								>
									DEV <span class="close-x">✕</span>
								</span>
							{/if}
						</div>
						{#if data.locationSubtext || data.locationName.includes(',')}
							<p class="location-subtext">
								{data.locationSubtext || data.locationName.split(',').slice(1).join(',').trim()}
							</p>
						{/if}
						{#if $showGpsCoordinates}
							<!-- svelte-ignore a11y_no_static_element_interactions -->
							<div class="location-text-group coords clickable" onclick={() => {
								const url = `https://www.openstreetmap.org/?mlat=${data.latitude}&mlon=${data.longitude}#map=15/${data.latitude}/${data.longitude}`;
								window.open(url, '_blank');
							}}>
								<span>{formatDMS(data.latitude, 'N', 'S')}</span>
								<span>{formatDMS(data.longitude, 'E', 'W')}</span>
							</div>
						{/if}
					</div>
					<p class="date">{formatDate(data.timestamp, SUPPORTED_LOCALES[$localeIndex].tag)}</p>
				</div>
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
				<WeatherAlertBanner alerts={data.alerts} />
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
					<CollapsibleSection title={$_('section_this_week')} expanded={true} onShare={handleShare}>
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
			<Footer />
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

{#if $showLocationOverrideDialog}
	<LocationOverrideDialog />
{/if}

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
		padding-top: 8px;
		flex-shrink: 0;
	}

	.location-primary-row {
		display: flex;
		align-items: center;
		gap: 12px;
		max-width: 100%;
		overflow: hidden;
	}

	.location-name {
		font-family: var(--font-display);
		font-size: 32px;
		font-weight: 700;
		color: white;
		margin: 0;
		white-space: nowrap;
		overflow: hidden;
		text-overflow: ellipsis;
	}

	.location-text-group.coords {
		display: flex;
		flex-direction: column;
		font-family: var(--font-display);
		font-weight: 700;
		font-size: 22px;
		line-height: 1.1;
		color: white;
		cursor: pointer;
	}

	.location-subtext {
		font-family: var(--font-body);
		font-size: 13px;
		color: rgba(255, 255, 255, 0.5);
		margin-top: 0;
		font-weight: 400;
		white-space: nowrap;
		overflow: hidden;
		text-overflow: ellipsis;
	}

	.dev-badge {
		background: rgba(255, 255, 255, 0.15);
		padding: 2px 6px;
		border-radius: 4px;
		font-size: 10px;
		font-weight: 800;
		color: rgba(255, 255, 255, 0.9);
		letter-spacing: 0.5px;
		font-family: var(--font-body);
		flex-shrink: 0;
		cursor: pointer;
		display: flex;
		align-items: center;
		gap: 4px;
		transition: background 0.2s;
	}

	.dev-badge:hover {
		background: rgba(255, 255, 255, 0.25);
	}

	.close-x {
		font-size: 10px;
		opacity: 0.7;
	}

	.edit-icon {
		color: rgba(255, 255, 255, 0.6);
		cursor: pointer;
		display: flex;
		align-items: center;
		justify-content: center;
		padding: 4px;
		border-radius: 50%;
		transition: background 0.2s, color 0.2s;
	}

	.edit-icon:hover {
		background: rgba(255, 255, 255, 0.1);
		color: white;
	}

	.location-text-group.coords {
		display: flex;
		flex-direction: column;
		font-family: var(--font-display);
		font-weight: 700;
		font-size: 20px;
		line-height: 1.1;
		color: rgba(255, 255, 255, 0.8);
		margin-top: 8px;
		cursor: pointer;
	}

	.date {
		font-size: 16px;
		color: rgba(255,255,255,0.7);
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
