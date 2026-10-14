<script lang="ts">
	import {
		weatherSource, weatherApiKey,
		alertsEnabled, alertsNwsEnabled, alertsGdacsEnabled,
		alertsMeteoAlarmEnabled, alertsJmaEnabled, alertsEcccEnabled,
		alertsBomEnabled, alertsNhcEnabled,
	} from '$lib/stores/preferences';
	import { testPirateWeatherKey } from '$lib/api/pirateWeather';
	import { _ } from 'svelte-i18n';
	import SettingsPage from './SettingsPage.svelte';
	import { HttpError } from '$lib/api/http';
	import type { WeatherSource } from '$lib/domain/weatherData';
	import { advancedModeActive, enableAdvancedMode, disableAdvancedMode } from '$lib/stores/advancedMode';

	interface Props { onBack: () => void; onOpenAbout: () => void; }
	let { onBack, onOpenAbout }: Props = $props();

	let pendingApiKey = $state($weatherApiKey);
	let testState: 'idle' | 'loading' | 'success' | { error: string } = $state('idle');

	// Reset pending key when source changes externally
	$effect(() => {
		const _src = $weatherSource;
		void _src;
		pendingApiKey = $weatherApiKey;
		testState = 'idle';
	});

	function selectSource(src: WeatherSource) {
		weatherSource.set(src);
		pendingApiKey = '';
		testState = 'idle';
	}

	async function testKey() {
		testState = 'loading';
		try {
			await testPirateWeatherKey(pendingApiKey.trim());
			weatherApiKey.set(pendingApiKey.trim());
			testState = 'success';
		} catch (e: unknown) {
			const status = e instanceof HttpError ? e.status : null;
			testState = {
				error: status === null ? $_('settings_network_error')
					: status === 401 ? $_('settings_key_invalid')
					: status === 403 ? $_('settings_key_forbidden')
					: status === 429 ? $_('settings_key_rate_limited')
					: $_('settings_api_error', { values: { code: status } })
			};
		}
	}
</script>

<SettingsPage title={$_('settings_title')} {onBack}>
	<div class="section group">
		<label class="toggle-row advanced-toggle">
			<span class="advanced-label">{$_('settings_advanced_mode')}</span>
			<input
				type="checkbox"
				checked={$advancedModeActive}
				onchange={(e) => {
					if ((e.currentTarget as HTMLInputElement).checked) {
						enableAdvancedMode();
					} else {
						disableAdvancedMode();
					}
				}}
			/>
		</label>
		{#if !$advancedModeActive}
			<p class="info-text">{$_('settings_advanced_off_desc')}</p>
		{/if}
	</div>

	{#if $advancedModeActive}
	<div class="section">
		<div class="section-title">{$_('settings_section_weather_source')}</div>
		<div class="group">

		{#each (['OPEN_METEO', 'PIRATE_WEATHER'] as WeatherSource[]) as src}
			<label class="radio-row">
				<input
					type="radio"
					name="source"
					value={src}
					checked={$weatherSource === src}
					onchange={() => selectSource(src)}
				/>
				<span class="radio-label">
					<span class="radio-main">{src === 'OPEN_METEO' ? $_('settings_open_meteo_default') : $_('settings_pirate_weather')}</span>
					{#if src === 'PIRATE_WEATHER'}
						<small class="radio-sub">{$_('settings_requires_api_key')}</small>
					{/if}
				</span>
			</label>
		{/each}

		{#if $weatherSource === 'PIRATE_WEATHER'}
			<input
				type="password"
				class="api-key-input"
				placeholder={$_('settings_api_key')}
				value={pendingApiKey}
				oninput={(e) => { pendingApiKey = (e.currentTarget as HTMLInputElement).value; testState = 'idle'; }}
			/>
			{#if pendingApiKey}
				<div class="test-row">
					<button
						class="test-btn"
						onclick={testKey}
						disabled={testState === 'loading'}
					>
						{testState === 'loading' ? '…' : $_('settings_test')}
					</button>
					{#if testState === 'success'}
						<span class="status ok">{$_('settings_saved')}</span>
					{:else if typeof testState === 'object'}
						<span class="status err">{testState.error}</span>
					{/if}
				</div>
			{/if}
		{/if}
		</div>
	</div>

	<div class="section">
		<div class="section-title">{$_('settings_section_alerts')}</div>
		<div class="group">
		<label class="toggle-row">
			<span>{$_('settings_show_alerts')}</span>
			<input type="checkbox" bind:checked={$alertsEnabled} />
		</label>
		{#if $alertsEnabled}
			<label class="check-row">
				<input type="checkbox" bind:checked={$alertsNwsEnabled} />
				<span>{$_('settings_alert_nws')}</span>
			</label>
			<label class="check-row">
				<input type="checkbox" bind:checked={$alertsGdacsEnabled} />
				<span>{$_('settings_alert_gdacs')}</span>
			</label>
			<label class="check-row">
				<input type="checkbox" bind:checked={$alertsMeteoAlarmEnabled} />
				<span>{$_('settings_alert_meteoalarm')}</span>
			</label>
			<label class="check-row">
				<input type="checkbox" bind:checked={$alertsJmaEnabled} />
				<span>{$_('settings_alert_jma')}</span>
			</label>
			<label class="check-row">
				<input type="checkbox" bind:checked={$alertsEcccEnabled} />
				<span>{$_('settings_alert_eccc')}</span>
			</label>
			<label class="check-row">
				<input type="checkbox" bind:checked={$alertsBomEnabled} />
				<span>{$_('settings_alert_bom')}</span>
			</label>
			<label class="check-row">
				<input type="checkbox" bind:checked={$alertsNhcEnabled} />
				<span>{$_('settings_alert_nhc')}</span>
			</label>
		{/if}
		</div>
	</div>

	<div class="section">
		<div class="section-title">{$_('settings_section_location')}</div>
		<div class="group">
			<p class="info-text">{$_('settings_web_geocoding_info')}</p>
		</div>
	</div>
	{/if}

	<div class="section group">
		<button class="about-row" onclick={onOpenAbout}>
			<span class="about-text">
				<span class="about-title">{$_('about_title')}</span>
				<span class="about-desc">{$_('about_row_desc')}</span>
			</span>
			<svg class="about-chevron" viewBox="0 0 24 24" width="22" height="22" fill="currentColor" aria-hidden="true">
				<path d="M8.59 16.59L10 18l6-6-6-6-1.41 1.41L13.17 12z"/>
			</svg>
		</button>
	</div>
</SettingsPage>

<style>








	:global(.group) .info-text {
		padding: 6px 0;
	}


	.radio-row {
		display: flex;
		align-items: center;
		gap: 12px;
		padding: 6px 0;
		cursor: pointer;
	}

	.radio-row input[type="radio"] {
		accent-color: white;
		width: 16px;
		height: 16px;
		flex-shrink: 0;
	}

	.radio-label {
		display: flex;
		flex-direction: column;
		gap: 2px;
	}

	.radio-main {
		font-size: 15px;
		color: white;
	}

	.radio-sub {
		font-size: 12px;
		color: rgba(255, 255, 255, 0.6);
	}

	.api-key-input {
		width: 100%;
		box-sizing: border-box;
		margin-top: 8px;
		background: rgba(255, 255, 255, 0.08);
		border: 1px solid rgba(255, 255, 255, 0.3);
		border-radius: 8px;
		padding: 10px 14px;
		color: white;
		font-size: 14px;
		outline: none;
		transition: border-color 0.2s;
	}

	.api-key-input::placeholder {
		color: rgba(255, 255, 255, 0.4);
	}

	.api-key-input:focus {
		border-color: rgba(255, 255, 255, 0.7);
	}

	.test-row {
		display: flex;
		align-items: center;
		gap: 12px;
		margin-top: 8px;
	}

	.test-btn {
		background: none;
		border: 1px solid rgba(255, 255, 255, 0.5);
		border-radius: 8px;
		color: white;
		padding: 6px 20px;
		font-size: 14px;
		cursor: pointer;
		transition: background 0.2s;
	}

	.test-btn:hover:not(:disabled) {
		background: rgba(255, 255, 255, 0.1);
	}

	.test-btn:disabled {
		opacity: 0.5;
		cursor: default;
	}

	.status {
		font-size: 13px;
	}

	.status.ok {
		color: #66BB6A;
	}

	.status.err {
		color: #EF5350;
	}

	.toggle-row {
		display: flex;
		align-items: center;
		justify-content: space-between;
		padding: 6px 0;
		cursor: pointer;
	}

	.toggle-row span {
		font-size: 15px;
		color: white;
	}

	/* Switch-style toggle (matches Android Switch) */
	.toggle-row input[type="checkbox"] {
		appearance: none;
		-webkit-appearance: none;
		position: relative;
		flex-shrink: 0;
		width: 48px;
		height: 28px;
		margin-inline-start: 12px;
		border-radius: 14px;
		background: rgba(255, 255, 255, 0.12);
		border: 2px solid rgba(255, 255, 255, 0.5);
		cursor: pointer;
		transition: background 0.2s, border-color 0.2s;
	}

	.toggle-row input[type="checkbox"]::before {
		content: '';
		position: absolute;
		top: 50%;
		inset-inline-start: 4px;
		width: 14px;
		height: 14px;
		border-radius: 50%;
		background: rgba(255, 255, 255, 0.6);
		transform: translateY(-50%);
		transition: inset-inline-start 0.2s, width 0.2s, height 0.2s;
	}

	.toggle-row input[type="checkbox"]:checked {
		background: rgba(255, 255, 255, 0.3);
		border-color: transparent;
	}

	.toggle-row input[type="checkbox"]:checked::before {
		inset-inline-start: 22px;
		width: 20px;
		height: 20px;
		background: white;
	}

	.toggle-row input[type="checkbox"]:focus-visible {
		outline: 2px solid rgba(255, 255, 255, 0.5);
		outline-offset: 2px;
	}

	.check-row {
		display: flex;
		align-items: center;
		gap: 12px;
		padding: 4px 0 4px 8px;
		cursor: pointer;
	}

	.check-row input[type="checkbox"] {
		accent-color: white;
		width: 16px;
		height: 16px;
		flex-shrink: 0;
	}

	.check-row span {
		font-size: 14px;
		color: rgba(255, 255, 255, 0.6);
	}

	.info-text {
		font-size: 13px;
		color: rgba(255, 255, 255, 0.5);
		line-height: 1.5;
		margin: 0;
	}

	.advanced-label {
		font-size: 15px;
		color: white;
		font-weight: 600;
	}

	.about-row {
		width: 100%;
		display: flex;
		align-items: center;
		gap: 8px;
		padding: 8px 0;
		background: none;
		border: none;
		color: white;
		font-family: inherit;
		text-align: start;
		cursor: pointer;
	}

	.about-text {
		flex: 1;
		display: flex;
		flex-direction: column;
		gap: 2px;
	}

	.about-title {
		font-size: 15px;
		font-weight: 600;
	}

	.about-desc {
		font-size: 12px;
		color: rgba(255, 255, 255, 0.55);
	}

	.about-chevron {
		color: rgba(255, 255, 255, 0.6);
		flex-shrink: 0;
	}

	:global([dir='rtl']) .about-chevron {
		transform: scaleX(-1);
	}
</style>
