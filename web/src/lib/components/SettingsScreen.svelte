<script lang="ts">
	import {
		weatherSource, weatherApiKey,
		alertsEnabled, alertsNwsEnabled, alertsGdacsEnabled,
		alertsMeteoAlarmEnabled, alertsJmaEnabled, alertsEcccEnabled,
		alertsWmoSwicEnabled, alertsBomEnabled, alertsNhcEnabled,
	} from '$lib/stores/preferences';
	import { testPirateWeatherKey } from '$lib/api/pirateWeather';
	import type { WeatherSource } from '$lib/domain/weatherData';
	import { expertModeActive, enableExpertMode, disableExpertMode } from '$lib/stores/devMode';

	interface Props { onBack: () => void; }
	let { onBack }: Props = $props();

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
			const code = e instanceof Error ? e.message : 'unknown';
			testState = {
				error: code === '401' ? 'Invalid API key'
					: code === '403' ? 'Forbidden'
					: code === '429' ? 'Rate limited'
					: `Error ${code}`
			};
		}
	}
</script>

<div class="settings">
	<header>
		<button class="back-btn" onclick={onBack} aria-label="Back">
			<svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor">
				<path d="M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20v-2z"/>
			</svg>
		</button>
		<h2>Settings</h2>
	</header>

	<div class="section">
		<label class="toggle-row expert-toggle">
			<span class="expert-label">Expert mode</span>
			<input
				type="checkbox"
				checked={$expertModeActive}
				onchange={(e) => {
					if ((e.currentTarget as HTMLInputElement).checked) {
						enableExpertMode();
					} else {
						disableExpertMode();
					}
				}}
			/>
		</label>
		{#if !$expertModeActive}
			<p class="info-text">Enable Expert mode to configure weather source, alerts, and location.</p>
		{/if}
	</div>

	{#if $expertModeActive}
	<div class="section">
		<div class="section-title">WEATHER SOURCE</div>

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
					<span class="radio-main">{src === 'OPEN_METEO' ? 'Open-Meteo (default)' : 'Pirate Weather'}</span>
					{#if src === 'PIRATE_WEATHER'}
						<small class="radio-sub">Requires API key</small>
					{/if}
				</span>
			</label>
		{/each}

		{#if $weatherSource === 'PIRATE_WEATHER'}
			<input
				type="password"
				class="api-key-input"
				placeholder="API Key"
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
						{testState === 'loading' ? '…' : 'Test'}
					</button>
					{#if testState === 'success'}
						<span class="status ok">✓ Saved</span>
					{:else if typeof testState === 'object'}
						<span class="status err">{testState.error}</span>
					{/if}
				</div>
			{/if}
		{/if}
	</div>

	<div class="section">
		<div class="section-title">ALERTS</div>
		<label class="toggle-row">
			<span>Show weather alerts</span>
			<input type="checkbox" bind:checked={$alertsEnabled} />
		</label>
		{#if $alertsEnabled}
			<label class="check-row">
				<input type="checkbox" bind:checked={$alertsNwsEnabled} />
				<span>NWS alerts (USA)</span>
			</label>
			<label class="check-row">
				<input type="checkbox" bind:checked={$alertsGdacsEnabled} />
				<span>GDACS alerts (global disasters)</span>
			</label>
			<label class="check-row">
				<input type="checkbox" bind:checked={$alertsMeteoAlarmEnabled} />
				<span>MeteoAlarm (Europe)</span>
			</label>
			<label class="check-row">
				<input type="checkbox" bind:checked={$alertsJmaEnabled} />
				<span>JMA (Japan)</span>
			</label>
			<label class="check-row">
				<input type="checkbox" bind:checked={$alertsEcccEnabled} />
				<span>ECCC (Canada)</span>
			</label>
			<label class="check-row">
				<input type="checkbox" bind:checked={$alertsBomEnabled} />
				<span>BOM (Australia)</span>
			</label>
			<label class="check-row">
				<input type="checkbox" bind:checked={$alertsNhcEnabled} />
				<span>NHC (Atlantic &amp; Pacific hurricanes)</span>
			</label>
			<label class="check-row">
				<input type="checkbox" bind:checked={$alertsWmoSwicEnabled} />
				<span>WMO SWIC (global)</span>
			</label>
		{/if}
	</div>

	<div class="section">
		<div class="section-title">LOCATION / GEOCODING</div>
		<p class="info-text">Web version uses Nominatim (OpenStreetMap) for reverse geocoding — no API key required.</p>
	</div>
	{/if}
</div>

<style>
	.settings {
		position: fixed;
		inset: 0;
		background: linear-gradient(to bottom, #0E0B3D, #1A1565);
		color: white;
		overflow-y: auto;
		z-index: 100;
		padding: 0 24px;
		padding-top: env(safe-area-inset-top);
		padding-bottom: max(24px, env(safe-area-inset-bottom));
		box-sizing: border-box;
	}

	header {
		display: flex;
		align-items: center;
		gap: 12px;
		padding: 8px 0 24px;
		flex-shrink: 0;
	}

	.back-btn {
		background: none;
		border: none;
		color: white;
		cursor: pointer;
		padding: 8px;
		display: flex;
		align-items: center;
		justify-content: center;
		border-radius: 50%;
		transition: background 0.2s;
	}

	.back-btn:hover {
		background: rgba(255, 255, 255, 0.1);
	}

	header h2 {
		font-size: 20px;
		font-weight: 700;
		margin: 0;
	}

	.section {
		margin-bottom: 32px;
	}

	.section-title {
		font-size: 11px;
		font-weight: 600;
		letter-spacing: 1px;
		color: rgba(255, 255, 255, 0.35);
		margin-bottom: 12px;
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

	.toggle-row input[type="checkbox"] {
		accent-color: white;
		width: 18px;
		height: 18px;
	}

	.check-row {
		display: flex;
		align-items: center;
		gap: 12px;
		padding: 4px 0 4px 16px;
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

	.expert-label {
		font-size: 15px;
		color: white;
		font-weight: 600;
	}
</style>
