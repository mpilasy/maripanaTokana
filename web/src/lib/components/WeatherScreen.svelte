<script lang="ts">
	import { _ } from 'svelte-i18n';
	import { weatherState, isRefreshing, doFetchWeather, updateLocationName } from '$lib/stores/weather';
	import {
		onLocationClicked,
		showGpsCoordinates,
		showLocationOverrideDialog,
		expertModeActive,
		locationOverride,
		initExpertMode,
		openLocationOverride,
		resetLocationToCurrent
	} from '$lib/stores/devMode';
	import LocationOverrideDialog from './LocationOverrideDialog.svelte';
	import SavedLocationsDialog from './SavedLocationsDialog.svelte';
	import { showSavedLocationsDialog, openSavedLocationsDialog, activeLocationId, switchToLocation } from '$lib/stores/savedLocations';
	import SettingsScreen from './SettingsScreen.svelte';
	import { metricPrimary, fontIndex, localeIndex, toggleUnits, cycleFont, cycleLanguage } from '$lib/stores/preferences';
	import { SUPPORTED_LOCALES, localizeDigits } from '$lib/i18n/index';
	import { fontPairings } from '$lib/fonts';
	import { formatDate, formatLocationCurrentTime, isRemoteTimezone } from '$lib/utils/date';
	import HeroCard from './HeroCard.svelte';
	import WeatherAlertBanner from './WeatherAlertBanner.svelte';
	import HourlyForecast from './HourlyForecast.svelte';
	import DailyForecast from './DailyForecast.svelte';
	import AirQualityChart from './AirQualityChart.svelte';
	import DailyUvForecast from './DailyUvForecast.svelte';
	import CurrentConditions from './CurrentConditions.svelte';
	import DetailCard from './DetailCard.svelte';
	import AirQualityDetailDialog from './AirQualityDetailDialog.svelte';
	import AqiTierBadge from './AqiTierBadge.svelte';
	import UvTierBadge from './UvTierBadge.svelte';
	import CollapsibleSection from './CollapsibleSection.svelte';
	import Controls from './Controls.svelte';
	import Footer from './Footer.svelte';
	import { captureAndShare } from '$lib/share';
	import { onMount } from 'svelte';

	let showSettings = $state(false);

	// Browser locale detection for secondary language on error screen
	function findBrowserLocaleTag(): string | null {
		if (typeof navigator === 'undefined') return null;
		const lang = navigator.language.split('-')[0].toLowerCase();
		return SUPPORTED_LOCALES.find(l => l.tag === lang)?.tag ?? null;
	}

	const browserLocaleTag = findBrowserLocaleTag();
	let browserStrings = $state<Record<string, string> | null>(null);

	onMount(() => {
		initExpertMode();
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

	const acquiringStrings = [
		'Aiza isika?',       // mg
		'أين نحن؟',          // ar
		'Where are we?',     // en
		'¿Dónde estamos?',   // es
		'Où sommes-nous ?',  // fr
		'हम कहाँ हैं?',       // hi
		'हामी कहाँ छौं?',     // ne
		'我们在哪里？',          // zh
	];
	let acquiringIndex = $state(0);
	let elapsedSeconds = $state(0);
	let acquiringInterval: ReturnType<typeof setInterval> | null = null;

	$effect(() => {
		if ($weatherState.kind === 'loading') {
			acquiringIndex = 0;
			elapsedSeconds = 0;
			acquiringInterval = setInterval(() => {
				elapsedSeconds++;
				if (elapsedSeconds % 2 === 0) {
					acquiringIndex = (acquiringIndex + 1) % acquiringStrings.length;
				}
			}, 1000);
		} else {
			if (acquiringInterval) {
				clearInterval(acquiringInterval);
				acquiringInterval = null;
			}
		}
	});

	let pullStartY = $state(0);
	let pullDelta = $state(0);
	let isPulling = $state(false);
	let scrollContainer = $state<HTMLElement | null>(null);
	let headerEl = $state<HTMLElement | null>(null);

	function loc(s: string): string {
		return localizeDigits(s, SUPPORTED_LOCALES[$localeIndex]);
	}

	let showAirQualityDetail = $state(false);

	// Accordion: only one card open at a time; switching to a new location collapses everything.
	let openSection = $state<string | null>('hourly_forecast');
	let lastLocationKey: string | null = null;

	function toggleSection(key: string) {
		openSection = openSection === key ? null : key;
	}

	$effect(() => {
		const state = $weatherState;
		if (state.kind !== 'success') return;
		const key = `${state.data.locationName}|${state.data.locationSubtext ?? ''}`;
		if (lastLocationKey !== null && key !== lastLocationKey) {
			openSection = null;
		}
		lastLocationKey = key;
	});

	function getUvLabel(uv: number): string {
		const labels: string[] = $_('uv_labels') as unknown as string[];
		if (!Array.isArray(labels)) return '';
		if (uv < 3) return labels[0];
		if (uv < 6) return labels[1];
		if (uv < 8) return labels[2];
		if (uv < 11) return labels[3];
		return labels[4];
	}

	const AQI_TIERS = ['good', 'moderate', 'unhealthy', 'very_unhealthy', 'hazardous'];

	function getAqiUnitDual(standard: 'US' | 'EUROPEAN'): [string, string] {
		const us = $_('air_quality_us_aqi');
		const eu = $_('air_quality_eu_aqi');
		return standard === 'EUROPEAN' ? [eu, us] : [us, eu];
	}

	function getAqiTierLabel(tier: string): string {
		const labels: string[] = $_('aqi_tier_labels') as unknown as string[];
		if (!Array.isArray(labels)) return '';
		return labels[AQI_TIERS.indexOf(tier)] ?? '';
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

	let prevLocaleIndex = $localeIndex;
	$effect(() => {
		const idx = $localeIndex;
		if (idx === prevLocaleIndex) return;
		prevLocaleIndex = idx;
		updateLocationName(SUPPORTED_LOCALES[idx].tag);
	});

	function formatDMS(value: number, positive: string, negative: string): string {
		const direction = value >= 0 ? positive : negative;
		const absolute = Math.abs(value);
		const degrees = Math.floor(absolute);
		const minutesTotal = (absolute - degrees) * 60;
		const minutes = Math.floor(minutesTotal);
		const seconds = Math.floor((minutesTotal - minutes) * 60);
		return `${degrees}°${minutes.toString().padStart(2, '0')}'${seconds.toString().padStart(2, '0')}"${direction}`;
	}
</script>

<div class="weather-screen">
	<!-- Blue Marble background -->
	<div class="bg-marble"></div>

	{#if $weatherState.kind === 'loading'}
		<div class="center">
			<div class="spinner"></div>
			<p class="acquiring-text">{acquiringStrings[acquiringIndex]}</p>
			<p class="acquiring-timer">{elapsedSeconds}s</p>
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
							<!-- svelte-ignore a11y_click_events_have_key_events -->
							<!-- svelte-ignore a11y_no_static_element_interactions -->
							<span
								class="manage-locations-btn"
								onclick={(e) => { e.stopPropagation(); openSavedLocationsDialog(); }}
								title="Manage locations"
							>
								<svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor">
									<path d="M7.41 8.59L12 13.17l4.59-4.58L18 10l-6 6-6-6z"/>
								</svg>
							</span>
							{#if $activeLocationId !== null}
								<!-- svelte-ignore a11y_click_events_have_key_events -->
								<!-- svelte-ignore a11y_no_static_element_interactions -->
								<span
									class="goto-current-btn"
									onclick={(e) => { e.stopPropagation(); switchToLocation(null); }}
									title={$_('android_only.cd_go_to_current_location')}
								>
									<svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor">
										<path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5c-1.38 0-2.5-1.12-2.5-2.5s1.12-2.5 2.5-2.5 2.5 1.12 2.5 2.5-1.12 2.5-2.5 2.5z"/>
									</svg>
								</span>
							{/if}
							{#if $expertModeActive}
								<!-- svelte-ignore a11y_click_events_have_key_events -->
								<!-- svelte-ignore a11y_no_static_element_interactions -->
								<span
									class="edit-icon"
									onclick={(e) => { e.stopPropagation(); openLocationOverride(); }}
									title="Change location"
								>
									<svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor">
										<path d="M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04c.39-.39.39-1.02 0-1.41l-2.34-2.34c-.39-.39-1.02-.39-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z"/>
									</svg>
								</span>
								{#if $locationOverride !== null}
									<!-- svelte-ignore a11y_click_events_have_key_events -->
									<!-- svelte-ignore a11y_no_static_element_interactions -->
									<span
										class="goto-current-btn"
										onclick={(e) => { e.stopPropagation(); resetLocationToCurrent(); }}
										title="Go to current location"
									>
										<svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor">
											<path d="M12 2L4.5 20.29l.71.71L12 18l6.79 3 .71-.71z"/>
										</svg>
									</span>
								{/if}
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
					<div class="date-row">
						<p class="date">
							{$_('updated_time', { values: { time: loc(formatDate(data.timestamp, SUPPORTED_LOCALES[$localeIndex].tag)) } })}
						</p>
						{#if isRemoteTimezone(data.utcOffsetSeconds)}
							<p class="location-time">Local: {loc(formatLocationCurrentTime(data.utcOffsetSeconds, SUPPORTED_LOCALES[$localeIndex].tag))}</p>
						{/if}
						<button
							class="gear-btn"
							onclick={(e) => { e.stopPropagation(); showSettings = true; }}
							aria-label="Settings"
						>
							<svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor">
								<path d="M19.14,12.94c0.04-0.3,0.06-0.61,0.06-0.94c0-0.32-0.02-0.64-0.07-0.94l2.03-1.58c0.18-0.14,0.23-0.41,0.12-0.61 l-1.92-3.32c-0.12-0.22-0.37-0.29-0.59-0.22l-2.39,0.96c-0.5-0.38-1.03-0.7-1.62-0.94L14.4,2.81c-0.04-0.24-0.24-0.41-0.48-0.41 h-3.84c-0.24,0-0.43,0.17-0.47,0.41L9.25,5.35C8.66,5.59,8.12,5.92,7.63,6.29L5.24,5.33c-0.22-0.08-0.47,0-0.59,0.22L2.74,8.87 C2.62,9.08,2.66,9.34,2.86,9.48l2.03,1.58C4.84,11.36,4.8,11.69,4.8,12s0.02,0.64,0.07,0.94l-2.03,1.58 c-0.18,0.14-0.23,0.41-0.12,0.61l1.92,3.32c0.12,0.22,0.37,0.29,0.59,0.22l2.39-0.96c0.5,0.38,1.03,0.7,1.62,0.94l0.36,2.54 c0.05,0.24,0.24,0.41,0.48,0.41h3.84c0.24,0,0.44-0.17,0.47-0.41l0.36-2.54c0.59-0.24,1.13-0.56,1.62-0.94l2.39,0.96 c0.22,0.08,0.47,0,0.59-0.22l1.92-3.32c0.12-0.22,0.07-0.47-0.12-0.61L19.14,12.94z M12,15.6c-1.98,0-3.6-1.62-3.6-3.6 s1.62-3.6,3.6-3.6s3.6,1.62,3.6,3.6S13.98,15.6,12,15.6z"/>
							</svg>
						</button>
					</div>
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
				<WeatherAlertBanner alerts={data.alerts} expanded={openSection === 'alerts'} onToggle={() => toggleSection('alerts')} />
				<HeroCard {data} metricPrimary={$metricPrimary} {loc} onToggleUnits={toggleUnits} onShare={handleShare} />

				<CollapsibleSection title={$_('section_current_conditions')} expanded={openSection === 'current_conditions'} onToggle={() => toggleSection('current_conditions')} onShare={handleShare}>
					<CurrentConditions {data} metricPrimary={$metricPrimary} {loc} onToggleUnits={toggleUnits} />
				</CollapsibleSection>

				{#if data.hourlyForecast.length > 0}
					<CollapsibleSection title={$_('section_hourly_forecast')} expanded={openSection === 'hourly_forecast'} onToggle={() => toggleSection('hourly_forecast')} onShare={handleShare}>
						<HourlyForecast
							forecasts={data.hourlyForecast}
							metricPrimary={$metricPrimary}
							dailySunrise={data.dailySunrise}
							dailySunset={data.dailySunset}
							{loc}
							onToggleUnits={toggleUnits}
							utcOffsetSeconds={data.utcOffsetSeconds}
						/>
					</CollapsibleSection>
				{/if}

				{#if data.dailyForecast.length > 0}
					<CollapsibleSection title={$_('section_this_week')} expanded={openSection === 'this_week'} onToggle={() => toggleSection('this_week')} onShare={handleShare}>
						<DailyForecast
							forecasts={data.dailyForecast}
							metricPrimary={$metricPrimary}
							localeTag={SUPPORTED_LOCALES[$localeIndex].tag}
							{loc}
							onToggleUnits={toggleUnits}
							utcOffsetSeconds={data.utcOffsetSeconds}
						/>
					</CollapsibleSection>
				{/if}

				{#if data.hourlyAirQuality && data.hourlyAirQuality.length > 0 && data.airQuality}
					{@const aqiDual = data.airQuality.displayDual()}
					{@const aqiUnitDual = getAqiUnitDual(data.airQuality.primaryStandard)}
					<CollapsibleSection title={$_('section_air_quality_forecast')} expanded={openSection === 'air_quality_forecast'} onToggle={() => toggleSection('air_quality_forecast')} onShare={handleShare}>
						<DetailCard
							value={loc(aqiDual[0])}
							secondaryValue={loc(aqiDual[1])}
							unit={aqiUnitDual[0]}
							secondaryUnit={aqiUnitDual[1]}
						>
							{#snippet subtitleSnippet()}
								<AqiTierBadge
									tier={data.airQuality!.primaryTier}
									label={getAqiTierLabel(data.airQuality!.primaryTier)}
									onClick={() => showAirQualityDetail = true}
								/>
							{/snippet}
						</DetailCard>
						<div class="section-spacer"></div>
						<AirQualityChart
							forecasts={data.hourlyAirQuality}
							primaryStandard={data.airQuality.primaryStandard}
						/>
					</CollapsibleSection>
					{#if showAirQualityDetail}
						<AirQualityDetailDialog
							airQuality={data.airQuality}
							aqiTierLabel={getAqiTierLabel(data.airQuality.primaryTier)}
							onClose={() => showAirQualityDetail = false}
							{loc}
						/>
					{/if}
				{/if}

				{#if data.dailyForecast.length > 0}
					{@const todayUvMax = data.dailyForecast[0].uvIndexMax}
					<CollapsibleSection title={$_('section_uv_forecast')} expanded={openSection === 'uv_forecast'} onToggle={() => toggleSection('uv_forecast')} onShare={handleShare}>
						<DetailCard
							value={loc(todayUvMax.toFixed(1))}
						>
							{#snippet subtitleSnippet()}
								<UvTierBadge uvIndex={todayUvMax} label={getUvLabel(todayUvMax)} />
							{/snippet}
						</DetailCard>
						<div class="section-spacer"></div>
						<DailyUvForecast
							forecasts={data.dailyForecast}
							localeTag={SUPPORTED_LOCALES[$localeIndex].tag}
							{loc}
							utcOffsetSeconds={data.utcOffsetSeconds}
						/>
					</CollapsibleSection>
				{/if}

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

{#if $showSavedLocationsDialog}
	<SavedLocationsDialog />
{/if}

{#if showSettings}
	<SettingsScreen onBack={() => showSettings = false} />
{/if}

<style>
	.weather-screen {
		width: 100%;
		height: 100%;
		position: relative;
	}

	.section-spacer {
		height: 16px;
	}

	.bg-marble {
		position: absolute;
		inset: 0;
		background: url('/bg-blue-marble.webp') center/cover no-repeat;
		opacity: 0.12;
		pointer-events: none;
	}

	.date-row {
		display: flex;
		align-items: center;
		justify-content: space-between;
	}

	.gear-btn {
		background: none;
		border: none;
		color: rgba(255, 255, 255, 0.7);
		cursor: pointer;
		padding: 4px 0 4px 8px;
		display: flex;
		align-items: center;
		justify-content: center;
		border-radius: 50%;
		flex-shrink: 0;
		transition: color 0.2s;
	}

	.gear-btn:hover {
		color: white;
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

	.acquiring-text {
		color: white;
		font-size: 15px;
		margin: 0;
		text-align: center;
	}

	.acquiring-timer {
		color: rgba(255, 255, 255, 0.4);
		font-size: 12px;
		margin: 0;
	}

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

	.goto-current-btn {
		color: rgba(255, 255, 255, 0.6);
		cursor: pointer;
		display: flex;
		align-items: center;
		justify-content: center;
		padding: 4px;
		border-radius: 50%;
		transition: background 0.2s, color 0.2s;
	}

	.goto-current-btn:hover {
		background: rgba(255, 255, 255, 0.1);
		color: white;
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

	.manage-locations-btn {
		color: rgba(255, 255, 255, 0.6);
		cursor: pointer;
		display: flex;
		align-items: center;
		justify-content: center;
		padding: 4px;
		border-radius: 50%;
		transition: background 0.2s, color 0.2s;
	}

	.manage-locations-btn:hover {
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

	.location-time {
		font-size: 13px;
		color: rgba(255,255,255,0.45);
		font-family: var(--font-display);
		font-weight: 600;
		font-feature-settings: var(--font-features);
		margin: 0;
	}

	.location-time::before {
		content: '\1F553  ';
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

	.error-state button:focus-visible {
		outline: 2px solid rgba(255, 255, 255, 0.5);
		outline-offset: 2px;
	}

	.error-state button:active {
		transform: scale(0.97);
	}
</style>
