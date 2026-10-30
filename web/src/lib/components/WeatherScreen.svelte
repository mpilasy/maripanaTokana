<!-- SPDX-FileCopyrightText: Orinasa Njarasoa -->
<!-- SPDX-License-Identifier: GPL-3.0-or-later -->
<script lang="ts">
	import { _, json } from 'svelte-i18n';
	import { weatherState, isRefreshing, doFetchWeather, updateLocationName, refreshFailed, refreshError } from '#lib/stores/weather.js';
	import {
		initAdvancedMode,
		advancedModeActive
	} from '#lib/stores/advancedMode.js';
	import SavedLocationsDialog from './SavedLocationsDialog.svelte';
	import LocationDetails from './LocationDetails.svelte';
	import RenameLocationDialog from './RenameLocationDialog.svelte';
	import {
		showSavedLocationsDialog, openSavedLocationsDialog, activeLocationId, savedLocations, switchToLocation,
		locationOverride, initLocationOverride, setLocationOverride, favoriteCurrentLocation, unfavoriteCurrentLocation, displayName
	} from '#lib/stores/savedLocations.js';
	import { parseLocationText } from '#lib/domain/sharedLocationParser.js';
	import { encodePlusCode, findPlusCode } from '#lib/domain/plusCode.js';
	import { longPress } from '#lib/actions/longPress.js';
	import { searchLocations } from '#lib/api/geocodingSearch.js';
	import { getCachedLocation, lookupLocation } from '#lib/stores/location.js';
	import { calculateDistance } from '#lib/api/alerts/shared.js';
	import SettingsScreen from './SettingsScreen.svelte';
	import AboutScreen from './AboutScreen.svelte';
	import HelpScreen from './HelpScreen.svelte';
	import { metricPrimary, fontIndex, localeIndex, weatherSource, weatherApiKey, toggleUnits, cycleFont, cycleLanguage } from '#lib/stores/preferences.js';
	import { SUPPORTED_LOCALES, localizeDigits } from '#lib/i18n/index.js';
	import { fontPairings } from '#lib/fonts.js';
	import { formatShortDate, formatTime, formatLocationCurrentTime, isRemoteTimezone, isToday } from '#lib/utils/date.js';
	import HeroCard from './HeroCard.svelte';
	import WeatherAlertBanner from './WeatherAlertBanner.svelte';
	import HourlyForecast from './HourlyForecast.svelte';
	import DailyForecast from './DailyForecast.svelte';
	import CurrentConditions from './CurrentConditions.svelte';
	import NowcastCard from './NowcastCard.svelte';
	import { nowcastHasRain } from '#lib/domain/precipOutlook.js';
	import AirQualityDetailDialog from './AirQualityDetailDialog.svelte';
	import SectionCard from './SectionCard.svelte';
	import ActivitiesCard from './ActivitiesCard.svelte';
	import { computeActivityIndices } from '#lib/domain/activityIndices.js';
	import Footer from './Footer.svelte';
	import { captureAndShare } from '#lib/share.js';
	import { isDaytime } from '#lib/domain/weatherData.js';
	import { motionDuration } from '#lib/utils/motion.js';
	import { slide } from 'svelte/transition';
	import { onMount } from 'svelte';

	let showSettings = $state(false);
	let showAbout = $state(false);
	let showHelp = $state(false);

	const weekdayNames = $derived($json('weekday_names') as string[] | undefined);
	const monthNames = $derived($json('month_names') as string[] | undefined);

	// Browser locale detection for secondary language on error screen
	function findBrowserLocaleTag(): string | null {
		if (typeof navigator === 'undefined') return null;
		const lang = navigator.language.split('-')[0].toLowerCase();
		return SUPPORTED_LOCALES.find(l => l.tag === lang)?.tag ?? null;
	}

	const browserLocaleTag = findBrowserLocaleTag();
	let browserStrings = $state<Record<string, string> | null>(null);

	onMount(() => {
		initAdvancedMode();
		initLocationOverride();

		if (typeof window !== 'undefined') {
			const params = new URLSearchParams(window.location.search);
			const sharedText = params.get('text') || params.get('url') || params.get('q') || params.get('loc') || params.get('title');
			if (sharedText) {
				const parsed = parseLocationText(sharedText);
				if (parsed?.name) {
					setLocationOverride(parsed.latitude, parsed.longitude, parsed.name);
				} else if (parsed) {
					// Bare coordinates have no name: use the place name found for them
					const { latitude, longitude } = parsed;
					lookupLocation(latitude, longitude, SUPPORTED_LOCALES[$localeIndex].tag).then((r) => {
						setLocationOverride(latitude, longitude, r?.name ?? `${latitude.toFixed(4)}, ${longitude.toFixed(4)}`, r?.subtext);
					});
				} else if (findPlusCode(sharedText)) {
					searchLocations(sharedText).then(([r]) => {
						if (r) setLocationOverride(r.latitude, r.longitude, r.name, [r.admin1, r.country].filter(Boolean).join(', ') || undefined);
					});
				}
			}
		}
	});

	if (browserLocaleTag) {
		// eslint-disable-next-line @typescript-eslint/no-explicit-any
		const loaders: Record<string, () => Promise<{ default: any }>> = {
			en: () => import('#lib/i18n/locales/en.json'),
			mg: () => import('#lib/i18n/locales/mg.json'),
			ar: () => import('#lib/i18n/locales/ar.json'),
			es: () => import('#lib/i18n/locales/es.json'),
			fr: () => import('#lib/i18n/locales/fr.json'),
			hi: () => import('#lib/i18n/locales/hi.json'),
			ne: () => import('#lib/i18n/locales/ne.json'),
			zh: () => import('#lib/i18n/locales/zh.json'),
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

	$effect(() => {
		if ($weatherState.kind !== 'loading') return;
		acquiringIndex = 0;
		elapsedSeconds = 0;
		const id = setInterval(() => {
			elapsedSeconds++;
			if (elapsedSeconds % 2 === 0) {
				acquiringIndex = (acquiringIndex + 1) % acquiringStrings.length;
			}
		}, 1000);
		return () => clearInterval(id);
	});

	let pullStartY = $state(0);
	let pullDelta = $state(0);
	let isPulling = $state(false);
	let scrollContainer = $state<HTMLElement | null>(null);
	let headerEl = $state<HTMLElement | null>(null);
	let headerActionsWidth = $state(0);
	// Location whose name is currently shown in Latin letters (null: local script)
	let romanShownFor = $state<string | null>(null);
	// The list of places, opened from the place name or the arrow before it
	let pickerOpen = $state(false);
	// The favorite in the rename dialog: the one on screen (hold its name) or one in the list of places (its pencil)
	let renamingId = $state<string | null>(null);
	const savedById = (id: string | null) => $savedLocations.find((l) => l.id === id);
	// The favorite on screen (not a preview): holding its name renames it
	let activeSavedLocation = $derived($locationOverride ? undefined : savedById($activeLocationId));
	let renamingLocation = $derived(savedById(renamingId));
	$effect(() => {
		if (!pickerOpen) return;
		const close = () => { pickerOpen = false; };
		window.addEventListener('click', close);
		return () => window.removeEventListener('click', close);
	});

	let hasLocationList = $derived($savedLocations.length > 0);
	/**
	 * The list of places with their distance in km from the place on screen at (`lat`, `lon`): the current
	 * location first, then the favorites nearest first. The distance is null when the device location is not known.
	 */
	function placeList(lat: number, lon: number): { id: string | null; km: number | null }[] {
		const here = getCachedLocation();
		const favorites = $savedLocations
			.map((l) => ({ id: l.id as string | null, km: calculateDistance(lat, lon, l.latitude, l.longitude) as number | null }))
			.sort((a, b) => a.km! - b.km!);
		return [{ id: null, km: here ? calculateDistance(lat, lon, here.lat, here.lon) : null }, ...favorites];
	}
	/** " (12 km)" in the main unit; empty when not known. */
	function distanceSuffix(km: number | null): string {
		if (km == null) return '';
		// The main unit, then the other
		const kmText = `${Math.round(km)} km`;
		const miText = `${Math.round(km / 1.609344)} mi`;
		return loc($metricPrimary ? ` (${kmText} · ${miText})` : ` (${miText} · ${kmText})`);
	}

	function loc(s: string): string {
		return localizeDigits(s, SUPPORTED_LOCALES[$localeIndex]);
	}

	let showAirQualityDetail = $state(false);

	let alertsOpen = $state(false);

	// Switching to a new location goes back to the top.
	let lastLocationKey: string | null = null;

	$effect(() => {
		const state = $weatherState;
		if (state.kind !== 'success') return;
		const key = `${state.data.locationName}|${state.data.locationSubtext ?? ''}`;
		if (lastLocationKey !== null && key !== lastLocationKey) {
			alertsOpen = false;
			scrollContainer?.scrollTo({ top: 0 });
		}
		lastLocationKey = key;
	});


	const AQI_TIERS = ['good', 'moderate', 'unhealthy', 'very_unhealthy', 'hazardous'];

	function getAqiTierLabel(tier: string): string {
		const labels: string[] = $json('aqi_tier_labels') as string[];
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
		if (isPulling) {
			pullDelta = Math.max(0, e.touches[0].clientY - pullStartY);
		}
	}

	function handleTouchEnd() {
		if (pullDelta > 80) {
			doFetchWeather();
		}
		pullDelta = 0;
		isPulling = false;
	}

	// Past about 48 px of scroll the header shrinks to one row; it comes back in full at the top.
	// It expands again only near the top, so a scroll position near 48 px does not make it flicker.
	let headerCollapsed = $state(false);
	function handleScroll() {
		const top = scrollContainer?.scrollTop ?? 0;
		if (!headerCollapsed && top > 48) headerCollapsed = true;
		else if (headerCollapsed && top < 8) headerCollapsed = false;
	}

	function handleShare(el: HTMLElement) {
		if (!headerEl) return;
		return captureAndShare(headerEl, el);
	}

	// The selected forecast tabs, kept here so the share images show the same tabs
	let hourlyMode = $state('Temperature');
	let dailyMode = $state('Temperature');

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

	// Refetch when the weather source or API key changes (skips the initial value).
	let prevSource = $weatherSource;
	let prevApiKey = $weatherApiKey;
	$effect(() => {
		const src = $weatherSource;
		const key = $weatherApiKey;
		if (src === prevSource && key === prevApiKey) return;
		prevSource = src;
		prevApiKey = key;
		doFetchWeather();
	});

	/** True when the place name is only coordinates, as when the name lookup failed. */
	function isCoordinatesName(name: string): boolean {
		return /^-?\d+(\.\d+)?,\s*-?\d+(\.\d+)?$/.test(name.trim());
	}

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
			<!-- A way out when the location or the weather takes long: the same button as on the error screen -->
			{#if elapsedSeconds >= 5}
				<button class="choose-place-btn" onclick={openSavedLocationsDialog}>
					{$_('choose_place')}
					{#if showSecondary && browserStrings}
						<span class="btn-secondary">{browserStrings.choose_place}</span>
					{/if}
				</button>
			{/if}
		</div>

	{:else if $weatherState.kind === 'success'}
		{@const data = $weatherState.data}
		{@const romanKey = `${data.latitude},${data.longitude},${data.locationName}`}
		{@const showRoman = !!data.romanName && romanShownFor === romanKey}
		{@const name = showRoman ? data.romanName! : data.locationName}
		{@const subtext = showRoman ? data.romanSubtext : data.locationSubtext}
		<!-- The name on the first header line: without the region when the name holds it after a comma -->
		{@const shownName = !subtext && name.includes(',') && !isCoordinatesName(name) ? name.split(',')[0].trim() : name}
		<!-- The second header line always shows: the region and country, or else the coordinates
		     (the Plus Code when the name itself is already the coordinates) -->
		{@const region = subtext || (name.includes(',') && !isCoordinatesName(name) ? name.split(',').slice(1).join(',').trim() : '')}
		{@const plusCode = encodePlusCode(data.latitude, data.longitude)}
		{@const decimal = `${data.latitude.toFixed(4)}, ${data.longitude.toFixed(4)}`}
		{@const refreshFailedText = $_('refresh_failed', { values: { time: loc(formatTime(data.timestamp)) } }) + ($refreshError ? ' · ' + $_($refreshError) : '')}
		{@const updatedText = $_('updated_time', { values: { time: loc(isToday(data.timestamp)
			? formatTime(data.timestamp)
			: `${formatShortDate(data.timestamp, SUPPORTED_LOCALES[$localeIndex].tag, weekdayNames, monthNames)}, ${formatTime(data.timestamp)}`) } })}
		{@const remoteTime = isRemoteTimezone(data.utcOffsetSeconds) ? loc(formatLocationCurrentTime(data.utcOffsetSeconds, SUPPORTED_LOCALES[$localeIndex].tag)) : null}
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
			<!-- Fixed header: full-width location block (captured for share screenshots) with the actions
			     overlaid on the name line (not captured); only the name shrinks to make room.
			     Collapsed (scrolled down): only the name line, with the local time and a refresh button -->
			<div class="header" class:collapsed={headerCollapsed}>
				<!-- svelte-ignore a11y_click_events_have_key_events -->
				<!-- svelte-ignore a11y_no_static_element_interactions -->
				<div class="header-main" bind:this={headerEl}
					use:longPress={activeSavedLocation ? () => (renamingId = activeSavedLocation!.id) : null}>
					<!-- A tap on the name opens the list of places -->
					<div class="name-row" style:padding-inline-end="{headerActionsWidth}px"
						onclick={(e) => { if (hasLocationList) { e.stopPropagation(); pickerOpen = !pickerOpen; } }}>
						<h1 class="location-name">{shownName}</h1>
						{#if data.romanName}
							<!-- Discreet switch between the local script and Latin letters; resets per location -->
							<button type="button" class="script-toggle touch-target" class:active={showRoman} aria-label={$_('cd_roman_name')}
								onclick={(e) => { e.stopPropagation(); romanShownFor = showRoman ? null : romanKey; }}>
								<svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor" aria-hidden="true">
									<path d="M12.87 15.07l-2.54-2.51.03-.03c1.74-1.94 2.98-4.17 3.71-6.53H17V4h-7V2H8v2H1v1.99h11.17C11.5 7.92 10.44 9.75 9 11.35 8.07 10.32 7.3 9.19 6.69 8h-2c.73 1.63 1.73 3.17 2.98 4.56l-5.09 5.02L4 19l5-5 3.11 3.11.76-2.04zM18.5 10h-2L12 22h2l1.12-3h4.75L21 22h2l-4.5-12zm-2.62 7l1.62-4.33L19.12 17h-3.24z"/>
								</svg>
							</button>
						{/if}
						<!-- Twin: HeaderNameLine in app/src/main/java/orinasa/njarasoa/maripanatokana/ui/weather/WeatherContent.kt -->
						<!-- The name has priority: the time shows only when all of it fits after the name -->
						{#if headerCollapsed && remoteTime}
							<span class="location-time-fit"><span class="location-time"><span aria-hidden="true">&#x1F553;</span> {remoteTime}</span></span>
						{/if}
					</div>
					{#if !headerCollapsed}
					<div transition:slide={{ duration: motionDuration(200) }}>
					<div class="subtext-row">
						<LocationDetails
							place={isCoordinatesName(name) ? (region || undefined) : [name.includes(',') && !subtext ? name.split(',')[0].trim() : name, region].filter(Boolean).join(', ')}
							coordinates={decimal}
							coordinatesShown={`${formatDMS(data.latitude, 'N', 'S')}  ${formatDMS(data.longitude, 'E', 'W')}`}
							{plusCode} lat={data.latitude} lon={data.longitude} />
						<p class="location-subtext" dir={region ? undefined : 'ltr'}>{region || (isCoordinatesName(name) ? plusCode : decimal)}</p>
						{#if remoteTime}
							<span class="location-time"><span aria-hidden="true">&#x1F553;</span> {remoteTime}</span>
						{/if}
					</div>
					<!-- The visible time stays in the button's name; "Refresh" follows it for screen readers -->
					<button type="button" class="updated touch-target" onclick={(e) => { e.stopPropagation(); doFetchWeather(); }}>
						<!-- The fetch time is the phone's; marked as such when the place keeps another time -->
						{#if isRemoteTimezone(data.utcOffsetSeconds)}
							<svg class="updated-phone" viewBox="0 0 24 24" width="13" height="13" fill="currentColor" aria-hidden="true"><path d="M17 1.01L7 1c-1.1 0-2 .9-2 2v18c0 1.1.9 2 2 2h10c1.1 0 2-.9 2-2V3c0-1.1-.9-1.99-2-1.99zM17 19H7V5h10v14z"/></svg>
						{/if}
						<span class="updated-text">{updatedText}</span>
						<span class="sr-only">{$_('cd_refresh')}</span>
						<svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
							<path d="M23 4v6h-6"/>
							<path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10"/>
						</svg>
					</button>
					{#if $refreshFailed}
						<button type="button" class="refresh-failed touch-target" onclick={(e) => { e.stopPropagation(); doFetchWeather(); }}>
							{refreshFailedText}
						</button>
					{/if}
					</div>
					{/if}
				</div>

				<!-- The arrow before the name, in the margin outside the share image -->
				{#if hasLocationList}
					<div class="location-list-box">
						<button type="button" class="location-list-arrow touch-target" aria-label={$_('cd_location_list')} title={$_('cd_location_list')}
							aria-expanded={pickerOpen} onclick={(e) => { e.stopPropagation(); pickerOpen = !pickerOpen; }}>
							<svg viewBox="0 0 24 24" width="24" height="24" fill="currentColor" aria-hidden="true"><path d="M7 10l5 5 5-5z"/></svg>
						</button>
						{#if pickerOpen}
							<div class="location-picker" role="menu" dir={SUPPORTED_LOCALES[$localeIndex].tag === 'ar' ? 'rtl' : 'ltr'}>
								{#each placeList(data.latitude, data.longitude) as { id, km } (id ?? 'current')}
									{@const isCurrent = id === $activeLocationId && !$locationOverride}
									<div class="picker-row">
									<button type="button" role="menuitem" class="picker-item" class:current={isCurrent}
										onclick={(e) => { e.stopPropagation(); pickerOpen = false; if (!isCurrent) switchToLocation(id); }}>
										<svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor" aria-hidden="true">
											{#if id === null}
												<path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5c-1.38 0-2.5-1.12-2.5-2.5s1.12-2.5 2.5-2.5 2.5 1.12 2.5 2.5-1.12 2.5-2.5 2.5z"/>
											{:else}
												<path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/>
											{/if}
										</svg>
										<!-- No distance for the place on screen -->
										{(id === null ? $_('current_location') : (savedById(id) ? displayName(savedById(id)!) : id)) + (isCurrent ? '' : distanceSuffix(km))}
									</button>
									<!-- A favorite's pencil opens its rename dialog, as holding the name does -->
									{#if id !== null && savedById(id)}
										<button type="button" role="menuitem" class="picker-rename" aria-label={$_('rename_title')} title={$_('rename_title')}
											onclick={(e) => { e.stopPropagation(); pickerOpen = false; renamingId = id; }}>
											<svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor" aria-hidden="true"><path d="M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04c.39-.39.39-1.02 0-1.41l-2.34-2.34c-.39-.39-1.02-.39-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z"/></svg>
										</button>
									{/if}
									</div>
								{/each}
							</div>
						{/if}
					</div>
				{/if}

				<div class="header-actions" bind:clientWidth={headerActionsWidth}>
					<!-- Collapsed: the refresh button, named with the data age as the "Updated" line it replaces -->
					{#if headerCollapsed}
						<!-- After a failed refresh: in the colour of the "refresh failed" text, and named with it -->
						<button class="header-action touch-target" class:failed={$refreshFailed} onclick={doFetchWeather} title={$_('cd_refresh')}>
							<svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
								<path d="M23 4v6h-6"/>
								<path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10"/>
							</svg>
							<span class="sr-only">{updatedText}{#if $refreshFailed}{' ' + refreshFailedText}{/if} {$_('cd_refresh')}</span>
						</button>
					{/if}
					<button class="header-action touch-target" onclick={openSavedLocationsDialog} aria-label={$_('cd_manage_locations')} title={$_('cd_manage_locations')}>
						<svg viewBox="0 0 24 24" width="22" height="22" fill="currentColor"><path d="M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5 16 5.91 13.09 3 9.5 3S3 5.91 3 9.5 5.91 16 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z"/></svg>
					</button>
					<!-- Folded: no favourite and current-location buttons, so the name has room -->
					{#if !headerCollapsed && ($activeLocationId !== null || $locationOverride !== null)}
						<button
							class="header-action touch-target"
							onclick={() => { if ($activeLocationId !== null) unfavoriteCurrentLocation(); else favoriteCurrentLocation(); }}
							aria-label={$_($activeLocationId !== null ? 'cd_remove_favorite' : 'cd_add_favorite')}
							title={$_($activeLocationId !== null ? 'cd_remove_favorite' : 'cd_add_favorite')}
						>
							<svg viewBox="0 0 24 24" width="22" height="22" fill="currentColor">
								{#if $activeLocationId !== null}
									<path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/>
								{:else}
									<path d="M16.5 3c-1.74 0-3.41.81-4.5 2.09C10.91 3.81 9.24 3 7.5 3 4.42 3 2 5.42 2 8.5c0 3.78 3.4 6.86 8.55 11.54L12 21.35l1.45-1.32C18.6 15.36 22 12.28 22 8.5 22 5.42 19.58 3 16.5 3zm-4.4 15.55l-.1.1-.1-.1C7.14 14.24 4 11.39 4 8.5 4 6.5 5.5 5 7.5 5c1.54 0 3.04.99 3.57 2.36h1.87C13.46 5.99 14.96 5 16.5 5c2 0 3.5 1.5 3.5 3.5 0 2.89-3.14 5.74-7.9 10.05z"/>
								{/if}
							</svg>
						</button>
						<button class="header-action touch-target" onclick={() => switchToLocation(null)} aria-label={$_('cd_go_to_current_location')} title={$_('cd_go_to_current_location')}>
							<svg viewBox="0 0 24 24" width="22" height="22" fill="currentColor"><path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5c-1.38 0-2.5-1.12-2.5-2.5s1.12-2.5 2.5-2.5 2.5 1.12 2.5 2.5-1.12 2.5-2.5 2.5z"/></svg>
						</button>
					{/if}
					<button class="header-action touch-target" onclick={() => showSettings = true} aria-label={$_('settings_title')} title={$_('settings_title')}>
						<svg viewBox="0 0 24 24" width="22" height="22" fill="currentColor"><path d="M19.14,12.94c0.04-0.3,0.06-0.61,0.06-0.94c0-0.32-0.02-0.64-0.07-0.94l2.03-1.58c0.18-0.14,0.23-0.41,0.12-0.61 l-1.92-3.32c-0.12-0.22-0.37-0.29-0.59-0.22l-2.39,0.96c-0.5-0.38-1.03-0.7-1.62-0.94L14.4,2.81c-0.04-0.24-0.24-0.41-0.48-0.41 h-3.84c-0.24,0-0.43,0.17-0.47,0.41L9.25,5.35C8.66,5.59,8.12,5.92,7.63,6.29L5.24,5.33c-0.22-0.08-0.47,0-0.59,0.22L2.74,8.87 C2.62,9.08,2.66,9.34,2.86,9.48l2.03,1.58C4.84,11.36,4.8,11.69,4.8,12s0.02,0.64,0.07,0.94l-2.03,1.58 c-0.18,0.14-0.23,0.41-0.12,0.61l1.92,3.32c0.12,0.22,0.37,0.29,0.59,0.22l2.39-0.96c0.5,0.38,1.03,0.7,1.62,0.94l0.36,2.54 c0.05,0.24,0.24,0.41,0.48,0.41h3.84c0.24,0,0.44-0.17,0.47-0.41l0.36-2.54c0.59-0.24,1.13-0.56,1.62-0.94l2.39,0.96 c0.22,0.08,0.47,0,0.59-0.22l1.92-3.32c0.12-0.22,0.07-0.47-0.12-0.61L19.14,12.94z M12,15.6c-1.98,0-3.6-1.62-3.6-3.6 s1.62-3.6,3.6-3.6s3.6,1.62,3.6,3.6S13.98,15.6,12,15.6z"/></svg>
					</button>
				</div>
			</div>

			<!-- svelte-ignore a11y_no_static_element_interactions -->
			<!-- Scrollable content -->
			<div
				class="scroll-area"
				role="region"
				bind:this={scrollContainer}
				onscroll={handleScroll}
				ontouchstart={handleTouchStart}
				ontouchmove={handleTouchMove}
				ontouchend={handleTouchEnd}
			>
				<div class="glance">
					<WeatherAlertBanner alerts={data.alerts} expanded={alertsOpen} onToggle={() => (alertsOpen = !alertsOpen)} />
					{#if $advancedModeActive && (data.failedAlertSources?.length ?? 0) > 0}
						<div class="alert-sources-unavailable">{$_('alert_sources_unavailable', { values: { sources: data.failedAlertSources?.join(', ') } })}</div>
					{/if}
					<HeroCard {data} placeName={shownName} metricPrimary={$metricPrimary} {loc} onToggleUnits={toggleUnits} onShare={handleShare} />
				</div>

				{#if data.hourlyForecast.length > 0}
					<div>
						<SectionCard title={$_('section_hourly_forecast')} onShare={handleShare}>
							{#snippet shareCopy()}
								{#if $weatherSource === 'OPEN_METEO' && data.minutelyForecast && nowcastHasRain(data.minutelyForecast)}
									<h3 class="sub-title">{$_('section_nowcast')}</h3>
									<NowcastCard items={data.minutelyForecast} localizeDigits={loc} utcOffsetSeconds={data.utcOffsetSeconds} />
									<div class="sub-gap"></div>
								{/if}
								<HourlyForecast
									forecasts={data.hourlyForecast}
									metricPrimary={$metricPrimary}
									dailySunrise={data.dailySunrise}
									dailySunset={data.dailySunset}
									{loc}
									onToggleUnits={toggleUnits}
									utcOffsetSeconds={data.utcOffsetSeconds}
									shareCopy
									bind:displayMode={hourlyMode}
									airQuality={data.hourlyAirQuality && data.hourlyAirQuality.length > 0 && data.airQuality
										? { forecasts: data.hourlyAirQuality, primaryStandard: data.airQuality.primaryStandard }
										: null}
								/>
							{/snippet}
							{#if $weatherSource === 'OPEN_METEO' && data.minutelyForecast && nowcastHasRain(data.minutelyForecast)}
								<h3 class="sub-title">{$_('section_nowcast')}</h3>
								<NowcastCard items={data.minutelyForecast} localizeDigits={loc} utcOffsetSeconds={data.utcOffsetSeconds} />
								<div class="sub-gap"></div>
							{/if}
							<HourlyForecast
								forecasts={data.hourlyForecast}
								metricPrimary={$metricPrimary}
								dailySunrise={data.dailySunrise}
								dailySunset={data.dailySunset}
								{loc}
								onToggleUnits={toggleUnits}
								utcOffsetSeconds={data.utcOffsetSeconds}
								bind:displayMode={hourlyMode}
								airQuality={data.hourlyAirQuality && data.hourlyAirQuality.length > 0 && data.airQuality
									? { forecasts: data.hourlyAirQuality, primaryStandard: data.airQuality.primaryStandard }
									: null}
							/>
						</SectionCard>
					</div>
				{/if}

				{#if data.dailyForecast.length > 0}
					<div>
						<SectionCard title={$_('section_this_week')} onShare={handleShare}>
							{#snippet shareCopy()}
								<DailyForecast
									forecasts={data.dailyForecast}
									metricPrimary={$metricPrimary}
									localeTag={SUPPORTED_LOCALES[$localeIndex].tag}
									{loc}
									onToggleUnits={toggleUnits}
									utcOffsetSeconds={data.utcOffsetSeconds}
									shareCopy
									bind:displayMode={dailyMode}
									airQuality={data.hourlyAirQuality && data.hourlyAirQuality.length > 0 && data.airQuality
										? { forecasts: data.hourlyAirQuality, primaryStandard: data.airQuality.primaryStandard }
										: null}
								/>
							{/snippet}
							<DailyForecast
								forecasts={data.dailyForecast}
								metricPrimary={$metricPrimary}
								localeTag={SUPPORTED_LOCALES[$localeIndex].tag}
								{loc}
								onToggleUnits={toggleUnits}
								utcOffsetSeconds={data.utcOffsetSeconds}
								bind:displayMode={dailyMode}
								airQuality={data.hourlyAirQuality && data.hourlyAirQuality.length > 0 && data.airQuality
									? { forecasts: data.hourlyAirQuality, primaryStandard: data.airQuality.primaryStandard }
									: null}
							/>
						</SectionCard>
					</div>
				{/if}

				<div>
					<SectionCard bare title={$_('section_current_conditions')} onShare={handleShare}>
						<CurrentConditions part="now" {data} metricPrimary={$metricPrimary} {loc} onToggleUnits={toggleUnits} onOpenAirQuality={() => showAirQualityDetail = true} />
					</SectionCard>
				</div>

				<div>
					<SectionCard bare title={$_('section_sun_air')} onShare={handleShare}>
						<CurrentConditions part="sky" {data} metricPrimary={$metricPrimary} {loc} onToggleUnits={toggleUnits} onOpenAirQuality={() => showAirQualityDetail = true} />
					</SectionCard>
				</div>

				<div>
					<SectionCard bare title={$_('section_activities')} onShare={handleShare}>
						<ActivitiesCard indices={computeActivityIndices(data, isDaytime(Date.now() / 1000, data.sunrise, data.sunset))} />
					</SectionCard>
				</div>
			</div>



			<!-- Fixed footer -->
			<Footer
				fontName={fontPairings[$fontIndex].name}
				currentFlag={SUPPORTED_LOCALES[$localeIndex].flag}
				onCycleFont={cycleFont}
				onCycleLanguage={cycleLanguage}
				onOpenAbout={() => showAbout = true}
				onOpenHelp={() => showHelp = true}
			/>

			<!-- Outside .scroll-area: its mask-image would clip the dialog's backdrop -->
			{#if showAirQualityDetail && data.airQuality}
				<AirQualityDetailDialog
					airQuality={data.airQuality}
					aqiTierLabel={getAqiTierLabel(data.airQuality.primaryTier)}
					onClose={() => showAirQualityDetail = false}
					{loc}
				/>
			{/if}
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
			<button class="settings-btn" onclick={openSavedLocationsDialog}>
				{$_('choose_place')}
				{#if showSecondary && browserStrings}
					<span class="btn-secondary">{browserStrings.choose_place}</span>
				{/if}
			</button>
			<button class="settings-btn" onclick={() => showSettings = true}>
				{$_('open_settings')}
				{#if showSecondary && browserStrings}
					<span class="btn-secondary">{browserStrings.open_settings}</span>
				{/if}
			</button>
		</div>
	{/if}
</div>

{#if renamingLocation}
	<RenameLocationDialog location={renamingLocation} onclose={() => (renamingId = null)} />
{/if}

{#if $showSavedLocationsDialog}
	<SavedLocationsDialog />
{/if}

{#if showSettings}
	<SettingsScreen onBack={() => showSettings = false} onOpenAbout={() => showAbout = true} />
{/if}

{#if showAbout}
	<AboutScreen onBack={() => showAbout = false} onOpenHelp={() => showHelp = true}
		languageFlag={SUPPORTED_LOCALES[$localeIndex].flag} onCycleLanguage={cycleLanguage} />
{/if}

{#if showHelp}
	<HelpScreen onBack={() => showHelp = false} languageFlag={SUPPORTED_LOCALES[$localeIndex].flag} onCycleLanguage={cycleLanguage} />
{/if}

<style>
	.alert-sources-unavailable {
		font-size: 0.75rem;
		color: rgba(255, 255, 255, 0.5);
		padding: 0 4px 8px;
	}

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

	.acquiring-text {
		color: white;
		font-size: 0.9375rem;
		margin: 0;
		text-align: center;
	}

	.acquiring-timer {
		color: rgba(255, 255, 255, 0.4);
		font-size: 0.75rem;
		margin: 0;
	}

	/* Same look as the error screen's "Choose a place" button */
	.choose-place-btn {
		margin-top: 8px;
		padding: 12px 36px;
		background: transparent;
		color: white;
		border: 1px solid rgba(255, 255, 255, 0.5);
		border-radius: 14px;
		font-size: 1rem;
		font-weight: 700;
		cursor: pointer;
		transition: background 0.2s, transform 0.1s;
	}

	.choose-place-btn:hover {
		background: rgba(255, 255, 255, 0.1);
	}

	.choose-place-btn:focus-visible {
		outline: 2px solid rgba(255, 255, 255, 0.5);
		outline-offset: 2px;
	}

	.choose-place-btn:active {
		transform: scale(0.97);
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
		position: relative;
		padding-top: 16px;
		flex-shrink: 0;
	}

	.header-main {
		min-width: 0;
		cursor: pointer;
	}

	.name-row {
		display: flex;
		align-items: center;
		min-width: 0;
	}

	/* Collapsed: one row as tall as the action buttons, so they stay in line with the name */
	.header.collapsed .name-row {
		min-height: 40px;
	}

	.name-row .location-time {
		margin-inline-start: 8px;
		line-height: 1rem;
	}

	/* As tall as the time, which goes to a hidden second line when it does not fit: it is not cut.
	   It gives up its width before the name does */
	.location-time-fit {
		flex: 0 1000 auto;
		min-width: 0;
		height: calc(1rem + 4px);
		display: flex;
		flex-wrap: wrap;
		align-content: flex-start;
		overflow: hidden;
	}

	/* No width; holds the first line open when the time goes to the second */
	.location-time-fit::before {
		content: '';
		height: 100%;
	}

	.script-toggle {
		flex: none;
		display: flex;
		margin-inline-start: 6px;
		padding: 4px;
		border: none;
		border-radius: 50%;
		background: transparent;
		color: rgba(255, 255, 255, 0.45);
		cursor: pointer;
	}

	.script-toggle.active {
		color: rgba(255, 255, 255, 0.85);
	}

	.location-name {
		min-width: 0;
		font-family: var(--font-display);
		font-size: 1.875rem;
		line-height: 2.25rem;
		font-weight: 700;
		color: white;
		margin: 0;
		white-space: nowrap;
		overflow: hidden;
		text-overflow: ellipsis;
		transition: font-size 0.2s, line-height 0.2s;
	}

	.header.collapsed .location-name {
		font-size: 1.25rem;
		line-height: 1.5rem;
		/* Never shrinks to nothing */
		min-width: 4em;
	}

	.subtext-row {
		display: flex;
		align-items: center;
		gap: 8px;
		min-width: 0;
	}

	.location-subtext {
		font-family: var(--font-body);
		font-size: 0.875rem;
		color: rgba(255, 255, 255, 0.6);
		white-space: nowrap;
		overflow: hidden;
		text-overflow: ellipsis;
		min-width: 0;
	}

	.location-time {
		font-size: 0.75rem;
		color: rgba(255, 255, 255, 0.85);
		font-family: var(--font-display);
		font-weight: 600;
		font-feature-settings: var(--font-features);
		background: var(--surface-tile);
		border-radius: 999px;
		padding: 2px 8px;
		white-space: nowrap;
		flex-shrink: 0;
	}






	.updated-phone {
		flex: none;
	}

	.updated {
		display: flex;
		align-items: center;
		gap: 4px;
		max-width: 100%;
		margin-top: 6px;
		padding: 0;
		background: none;
		border: none;
		font-family: var(--font-body);
		font-size: 0.8125rem;
		color: rgba(255, 255, 255, 0.55);
		cursor: pointer;
	}

	.updated svg {
		flex-shrink: 0;
		opacity: 0.8;
	}

	.updated:hover {
		color: rgba(255, 255, 255, 0.8);
	}

	.updated-text {
		white-space: nowrap;
		overflow: hidden;
		text-overflow: ellipsis;
	}

	.refresh-failed {
		display: block;
		margin-top: 2px;
		padding: 0;
		background: none;
		border: none;
		font-family: var(--font-body);
		font-size: 0.8125rem;
		color: #ff6b6b;
		cursor: pointer;
		text-align: start;
	}

	.header-actions {
		position: absolute;
		top: 16px;
		inset-inline-end: 0;
		display: flex;
		align-items: center;
	}

	.header-action {
		width: 40px;
		height: 40px;
		border-radius: 50%;
		border: none;
		background: none;
		color: rgba(255, 255, 255, 0.85);
		display: flex;
		align-items: center;
		justify-content: center;
		cursor: pointer;
		transition: background 0.2s, color 0.2s;
	}

	.header-action:hover {
		background: var(--surface-tile);
		color: white;
	}

	/* The colour of the "refresh failed" text */
	.header-action.failed {
		color: #ff6b6b;
	}

	.header-action:focus-visible, .location-list-arrow:focus-visible {
		outline: 2px solid rgba(255, 255, 255, 0.5);
		outline-offset: 2px;
	}

	/* In the content's start margin, beside the name line */
	.location-list-box {
		position: absolute;
		top: 16px;
		inset-inline-start: -24px;
	}

	.location-list-arrow {
		width: 24px;
		height: 36px;
		transition: height 0.2s;
		padding: 0;
		border: none;
		border-radius: 8px;
		background: none;
		color: rgba(255, 255, 255, 0.7);
		display: flex;
		align-items: center;
		justify-content: center;
		cursor: pointer;
	}

	.header.collapsed .location-list-arrow {
		height: 40px;
	}

	.location-picker {
		position: absolute;
		top: 100%;
		inset-inline-start: 0;
		z-index: 20;
		min-width: 200px;
		max-height: 60vh;
		overflow-y: auto;
		padding: 6px 0;
		border-radius: 16px;
		border: 1px solid var(--hairline);
		background: #1A1565;
		box-shadow: 0 8px 24px rgba(0, 0, 0, 0.4);
	}

	.picker-row {
		display: flex;
		align-items: center;
	}

	.picker-item {
		display: flex;
		align-items: center;
		gap: 12px;
		flex: 1;
		min-width: 0;
		padding: 12px 16px;
		border: none;
		background: transparent;
		color: rgba(255, 255, 255, 0.85);
		font-family: var(--font-body);
		font-size: 0.9375rem;
		text-align: start;
		white-space: nowrap;
		cursor: pointer;
	}

	.picker-item svg {
		flex: none;
		color: rgba(255, 255, 255, 0.6);
	}

	.picker-item:hover {
		background: var(--surface-tile);
	}

	.picker-item.current {
		color: white;
		font-weight: 700;
	}

	.picker-rename {
		flex: none;
		width: 44px;
		height: 44px;
		margin-inline-end: 4px;
		border: none;
		border-radius: 50%;
		background: transparent;
		color: rgba(255, 255, 255, 0.6);
		display: flex;
		align-items: center;
		justify-content: center;
		cursor: pointer;
	}

	.picker-rename:hover {
		background: var(--surface-tile);
		color: white;
	}

	.scroll-area {
		flex: 1;
		min-width: 0;
		overflow-y: auto;
		overflow-x: hidden;
		padding: 16px 0;
		-webkit-overflow-scrolling: touch;
		/* A dark scrollbar on desktop, to match the dark screen */
		color-scheme: dark;
		scrollbar-color: rgba(255, 255, 255, 0.25) transparent;
		/* Soft fade where content scrolls under the fixed header */
		-webkit-mask-image: linear-gradient(to bottom, transparent 0, black 16px);
		mask-image: linear-gradient(to bottom, transparent 0, black 16px);
	}

	/* Cards stacked in one free-scrolling column */
	.scroll-area {
		display: flex;
		flex-direction: column;
		gap: 12px;
	}

	.sub-title {
		margin: 0 4px 8px;
		font-family: var(--font-body);
		font-size: 0.875rem;
		font-weight: 600;
		color: rgba(255, 255, 255, 0.7);
	}

	.sub-gap {
		height: 16px;
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
		font-size: 1rem;
		font-weight: 600;
		margin-bottom: 8px;
	}

	.secondary-block p {
		font-size: 0.8125rem;
		line-height: 1.4;
	}

	.btn-secondary {
		display: block;
		font-size: 0.6875rem;
		opacity: 0.4;
		margin-top: 4px;
		font-weight: 400;
		color: inherit;
	}

	.error-state h2 {
		font-family: var(--font-display);
		font-size: 1.5rem;
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
		font-size: 1.125rem;
		font-weight: 700;
		cursor: pointer;
		transition: background 0.2s, transform 0.1s;
		box-shadow: 0 4px 20px rgba(255,255,255,0.25);
	}

	.error-state button.settings-btn {
		margin-top: 8px;
		padding: 12px 36px;
		font-size: 1rem;
		background: transparent;
		color: white;
		border: 1px solid rgba(255, 255, 255, 0.5);
		box-shadow: none;
	}

	.error-state button.settings-btn:hover {
		background: rgba(255, 255, 255, 0.1);
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
