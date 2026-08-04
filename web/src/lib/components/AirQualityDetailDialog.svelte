<script lang="ts">
	import { _, json } from 'svelte-i18n';
	import type { AirQualityIndex, AqiTier, PollenTier } from '$lib/domain/airQuality';
	import { POLLEN_TIERS, pollenTierFor } from '$lib/domain/airQuality';
	import AqiTierBadge from './AqiTierBadge.svelte';
	import PollenTierBadge from './PollenTierBadge.svelte';

	interface Props {
		airQuality: AirQualityIndex;
		aqiTierLabel: string;
		onClose: () => void;
		loc: (s: string) => string;
	}

	let { airQuality, aqiTierLabel, onClose, loc }: Props = $props();

	let aqiDual = $derived(airQuality.displayDual());
	let aqiUnitDual = $derived<[string, string]>(
		airQuality.primaryStandard === 'EUROPEAN'
			? [$_('air_quality_eu_aqi'), $_('air_quality_us_aqi')]
			: [$_('air_quality_us_aqi'), $_('air_quality_eu_aqi')]
	);

	const AQI_TIERS = ['good', 'moderate', 'unhealthy', 'very_unhealthy', 'hazardous'];

	function tierLabel(tier: AqiTier): string {
		const labels: string[] = $_('aqi_tier_labels') as unknown as string[];
		if (!Array.isArray(labels)) return '';
		return labels[AQI_TIERS.indexOf(tier)] ?? '';
	}

	let pollutants = $derived(
		(
			[
				['air_quality_pm2_5', airQuality.pollutants.pm25, airQuality.pollutants.pm25Tier],
				['air_quality_pm10', airQuality.pollutants.pm10, airQuality.pollutants.pm10Tier],
				['air_quality_carbon_monoxide', airQuality.pollutants.carbonMonoxide, airQuality.pollutants.carbonMonoxideTier],
				['air_quality_nitrogen_dioxide', airQuality.pollutants.nitrogenDioxide, airQuality.pollutants.nitrogenDioxideTier],
				['air_quality_sulphur_dioxide', airQuality.pollutants.sulphurDioxide, airQuality.pollutants.sulphurDioxideTier],
				['air_quality_ozone', airQuality.pollutants.ozone, airQuality.pollutants.ozoneTier],
				['air_quality_ammonia', airQuality.pollutants.ammonia, null],
				['air_quality_dust', airQuality.pollutants.dust, null],
			] as [string, number | null, AqiTier | null][]
		).filter(([, value]) => value != null) as [string, number, AqiTier | null][]
	);

	// Pollen tiers reuse uv_labels (0=Low..3=Very High) rather than a dedicated array — see
	// pollenTierFor's doc comment in $lib/domain/airQuality.
	function pollenTierLabel(tier: PollenTier): string {
		const labels = $json('uv_labels') as string[];
		if (!Array.isArray(labels)) return '';
		return labels[POLLEN_TIERS.indexOf(tier)] ?? '';
	}

	let pollenRows = $derived(
		(
			[
				['pollen_alder', airQuality.pollen.alder],
				['pollen_birch', airQuality.pollen.birch],
				['pollen_grass', airQuality.pollen.grass],
				['pollen_mugwort', airQuality.pollen.mugwort],
				['pollen_olive', airQuality.pollen.olive],
				['pollen_ragweed', airQuality.pollen.ragweed],
			] as [string, number | null][]
		)
			.filter(([, value]) => value != null)
			.map(([key, value]) => [key, value as number, pollenTierFor(value as number)] as [string, number, PollenTier])
	);
</script>

<!-- svelte-ignore a11y_click_events_have_key_events -->
<!-- svelte-ignore a11y_no_noninteractive_element_interactions -->
<div class="scrim" role="button" tabindex="0" onclick={onClose} onkeydown={(e) => { if (e.target === e.currentTarget && (e.key === 'Enter' || e.key === ' ')) { e.preventDefault(); onClose(); } }}>
	<div class="dialog" role="dialog" aria-modal="true" aria-labelledby="aqi-dialog-title" tabindex="-1" onclick={(e) => e.stopPropagation()}>
		<h2 id="aqi-dialog-title" class="title">{$_('detail_air_quality')}</h2>

		<div class="aqi-summary">
			<div class="aqi-value">
				<span class="aqi-unit">{aqiUnitDual[0]}</span>
				<span class="aqi-number">{loc(aqiDual[0])}</span>
				<AqiTierBadge tier={airQuality.primaryTier} label={aqiTierLabel} />
			</div>
			<div class="aqi-value">
				<span class="aqi-unit">{aqiUnitDual[1]}</span>
				<span class="aqi-number">{loc(aqiDual[1])}</span>
			</div>
		</div>

		{#if pollutants.length > 0}
			<div class="divider"></div>
			<div class="pollutants">
				{#each pollutants as [key, value, tier]}
					<div class="pollutant-row">
						<span class="pollutant-label">{$_(key)}</span>
						<span class="pollutant-value-group">
							{#if tier}
								<AqiTierBadge {tier} label={tierLabel(tier)} compact />
							{/if}
							<span class="pollutant-value">{loc(value.toFixed(1))} µg/m³</span>
						</span>
					</div>
				{/each}
			</div>
		{/if}

		{#if pollenRows.length > 0}
			<div class="divider"></div>
			<h3 class="section-title">{$_('detail_pollen')}</h3>
			<div class="pollutants">
				{#each pollenRows as [key, value, tier]}
					<div class="pollutant-row">
						<span class="pollutant-label">{$_(key)}</span>
						<span class="pollutant-value-group">
							<PollenTierBadge {tier} label={pollenTierLabel(tier)} compact />
							<span class="pollutant-value">{loc(value.toFixed(1))} grains/m³</span>
						</span>
					</div>
				{/each}
			</div>
		{/if}
	</div>
</div>

<style>
	.scrim {
		position: fixed;
		inset: 0;
		background: rgba(0, 0, 0, 0.5);
		display: flex;
		align-items: center;
		justify-content: center;
		z-index: 1000;
		padding: 24px;
	}

	.dialog {
		background: #0E0B3D;
		border-radius: 16px;
		padding: 24px;
		width: 100%;
		max-width: 400px;
		max-height: 80vh;
		overflow-y: auto;
		color: white;
	}

	.title {
		font-size: 20px;
		font-weight: 600;
		margin: 0 0 16px 0;
	}

	.aqi-summary {
		display: flex;
		gap: 16px;
	}

	.aqi-value {
		display: flex;
		flex-direction: column;
		gap: 2px;
		flex: 1;
	}

	.aqi-unit {
		font-size: 12px;
		color: rgba(255, 255, 255, 0.7);
	}

	.aqi-number {
		font-family: var(--font-display);
		font-size: 28px;
		font-weight: 700;
		font-feature-settings: var(--font-features);
	}

	.divider {
		height: 1px;
		background: rgba(255, 255, 255, 0.15);
		margin: 16px 0 8px 0;
	}

	.section-title {
		font-size: 14px;
		font-weight: 600;
		margin: 0 0 4px 0;
		color: rgba(255, 255, 255, 0.9);
	}

	.pollutants {
		display: flex;
		flex-direction: column;
	}

	.pollutant-row {
		display: flex;
		justify-content: space-between;
		align-items: center;
		padding: 6px 0;
	}

	.pollutant-label {
		font-size: 14px;
		color: rgba(255, 255, 255, 0.8);
	}

	.pollutant-value-group {
		display: flex;
		align-items: center;
		gap: 6px;
	}

	.pollutant-value {
		font-family: var(--font-display);
		font-size: 14px;
		font-feature-settings: var(--font-features);
	}

</style>
