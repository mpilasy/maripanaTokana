import { Component, input, output, inject, computed } from '@angular/core';
import type { WeatherData } from '$lib/domain/weatherData';
import { I18nService } from '../services/i18n.service';
import { DetailCardComponent } from './detail-card.component';

@Component({
	selector: 'app-current-conditions',
	standalone: true,
	imports: [DetailCardComponent],
	template: `
		<div class="conditions-grid">
			<!-- High / Low merged card -->
			<div class="merged-card" (click)="onToggleUnits.emit()">
				<span class="highlow-arrow">↓</span>
				<span class="merged-values">
					<span class="merged-primary">{{ minDual()[0] }}</span>
					<span class="merged-secondary">{{ minDual()[1] }}</span>
				</span>
				<span class="merged-label">{{ i18n.t('detail_high_low') }}</span>
				<span class="merged-values merged-values-end">
					<span class="merged-primary">{{ maxDual()[0] }}</span>
					<span class="merged-secondary">{{ maxDual()[1] }}</span>
				</span>
				<span class="highlow-arrow">↑</span>
			</div>

			<!-- Wind merged card -->
			<div class="merged-card" (click)="onToggleUnits.emit()">
				<div class="wind-side">
					<span class="merged-values">
						<span class="merged-primary">{{ windDual()[0] }}</span>
						<span class="merged-secondary">{{ windDual()[1] }}</span>
					</span>
					<span class="wind-subtitle">{{ windSubtitle() }}</span>
				</div>
				<span class="merged-label">{{ i18n.t('detail_wind') }}</span>
				<div class="wind-side wind-side-end">
					@if (gustDual()) {
						<span class="merged-values merged-values-end">
							<span class="merged-primary">{{ gustDual()![0] }}</span>
							<span class="merged-secondary">{{ gustDual()![1] }}</span>
						</span>
						<span class="wind-subtitle">{{ i18n.t('detail_wind_gust') }}</span>
					}
				</div>
			</div>

			<!-- Sunrise / Sunset merged card -->
			<div class="merged-card sun-card">
				<div class="sun-side">
					<span class="sun-time">{{ loc()(formatTime(data().sunrise)) }}</span>
					<span class="sun-label">{{ i18n.t('detail_sunrise') }}</span>
				</div>
				<span class="sun-icon">☀️</span>
				<div class="sun-side sun-side-end">
					<span class="sun-time">{{ loc()(formatTime(data().sunset)) }}</span>
					<span class="sun-label">{{ i18n.t('detail_sunset') }}</span>
				</div>
			</div>

			<!-- Temperature + Precipitation merged card -->
			<div class="merged-card temp-precip-card" (click)="onToggleUnits.emit()">
				<div class="tp-side">
					<span class="card-title">{{ i18n.t('detail_temperature') }}</span>
					<span class="tp-values">
						<span class="tp-primary">{{ tempDual()[0] }}</span>
						<span class="tp-secondary">{{ tempDual()[1] }}</span>
					</span>
					<span class="feels-label">{{ i18n.t('feels_like') }}</span>
					<span class="tp-values">
						<span class="feels-primary">{{ feelsLikeDual()[0] }}</span>
						<span class="feels-secondary">{{ feelsLikeDual()[1] }}</span>
					</span>
				</div>
				<div class="tp-side tp-side-end">
					<span class="card-title">{{ i18n.t('detail_precipitation') }}</span>
					@if (snowDual()) {
						<span class="tp-values tp-values-end">
							<span class="tp-primary">{{ '\u2744\uFE0F ' + snowDual()![0] }}</span>
							<span class="tp-secondary">{{ snowDual()![1] }}</span>
						</span>
					} @else if (rainDual()) {
						<span class="tp-values tp-values-end">
							<span class="tp-primary">{{ '\uD83C\uDF27\uFE0F ' + rainDual()![0] }}</span>
							<span class="tp-secondary">{{ rainDual()![1] }}</span>
						</span>
					} @else {
						<span class="tp-primary">{{ i18n.t('no_precip') }}</span>
					}
					<span class="feels-label">{{ i18n.t('detail_cloud_cover') }}</span>
					<span class="feels-primary">{{ loc()(data().cloudCover + '%') }}</span>
				</div>
			</div>

			<app-detail-card [title]="i18n.t('detail_pressure')" [value]="pressDual()[0]" [secondaryValue]="pressDual()[1]" (onToggleUnits)="onToggleUnits.emit()" />
			<div class="humidity-card">
				<span class="card-title">{{ i18n.t('detail_humidity') }}</span>
				<span class="card-value">{{ loc()(data().humidity + '%') }}</span>
				<span class="dew-label">{{ i18n.t('detail_dewpoint') }}</span>
				<span class="dew-values" (click)="onToggleUnits.emit()">
					<span class="dew-primary">{{ dewDual()[0] }}</span>
					<span class="dew-secondary">{{ dewDual()[1] }}</span>
				</span>
			</div>

			<app-detail-card [title]="i18n.t('detail_uv_index')" [value]="loc()(data().uvIndex.toFixed(1))" [subtitle]="uvLabel()" />
			<app-detail-card [title]="i18n.t('detail_visibility')" [value]="visDual()[0]" [secondaryValue]="visDual()[1]" (onToggleUnits)="onToggleUnits.emit()" />
		</div>
	`,
	styles: `
		.conditions-grid { display: grid; grid-template-columns: 1fr 1fr; grid-auto-rows: auto; gap: 16px; }
		.merged-card { grid-column: 1 / -1; background: rgba(42,31,165,0.6); border-radius: 16px; padding: 16px; display: flex; flex-direction: row; align-items: center; justify-content: space-between; gap: 8px; cursor: pointer; }
		.merged-label { font-size: 12px; color: rgba(255,255,255,0.5); text-align: center; }
		.merged-values { display: flex; align-items: baseline; gap: 4px; }
		.merged-values-end { justify-content: flex-end; }
		.merged-primary { font-family: var(--font-display); font-size: 18px; font-weight: 700; color: white; font-feature-settings: var(--font-features); }
		.merged-secondary { font-family: var(--font-display); font-size: 12px; color: rgba(255,255,255,0.55); font-feature-settings: var(--font-features); }
		.highlow-arrow { font-size: 24px; color: rgba(255,255,255,0.7); }
		.wind-side { display: flex; flex-direction: column; gap: 2px; min-width: 0; }
		.wind-side-end { align-items: flex-end; }
		.wind-subtitle { font-size: 12px; color: rgba(255,255,255,0.6); }
		.sun-card { cursor: default; }
		.sun-side { display: flex; flex-direction: column; gap: 2px; }
		.sun-side-end { align-items: flex-end; }
		.sun-time { font-family: var(--font-display); font-size: 18px; font-weight: 700; color: white; font-feature-settings: var(--font-features); }
		.sun-label { font-size: 12px; color: rgba(255,255,255,0.6); }
		.sun-icon { font-size: 24px; }
		.temp-precip-card { align-items: flex-start; }
		.tp-side { display: flex; flex-direction: column; gap: 4px; }
		.tp-side-end { align-items: flex-end; }
		.tp-values { display: flex; align-items: baseline; gap: 4px; }
		.tp-values-end { justify-content: flex-end; }
		.tp-primary { font-family: var(--font-display); font-size: 18px; font-weight: 700; color: white; font-feature-settings: var(--font-features); }
		.tp-secondary { font-family: var(--font-display); font-size: 12px; color: rgba(255,255,255,0.55); font-feature-settings: var(--font-features); }
		.feels-label { font-size: 12px; color: rgba(255,255,255,0.5); }
		.feels-values { cursor: pointer; }
		.feels-primary { font-family: var(--font-display); font-size: 13px; font-weight: 700; color: white; }
		.feels-secondary { font-family: var(--font-display); font-size: 12px; color: rgba(255,255,255,0.55); margin-left: 4px; }
		.humidity-card { background: rgba(42,31,165,0.6); border-radius: 16px; padding: 16px; display: flex; flex-direction: column; gap: 8px; }
		.card-title { font-size: 14px; color: rgba(255,255,255,0.7); }
		.card-value { font-family: var(--font-display); font-size: 20px; font-weight: 700; color: white; font-feature-settings: var(--font-features); }
		.dew-label { font-size: 12px; color: rgba(255,255,255,0.5); }
		.dew-values { cursor: pointer; }
		.dew-primary { font-family: var(--font-display); font-size: 13px; font-weight: 700; color: white; }
		.dew-secondary { font-family: var(--font-display); font-size: 12px; color: rgba(255,255,255,0.55); margin-left: 4px; }
	`,
})
export class CurrentConditionsComponent {
	i18n = inject(I18nService);

	data = input.required<WeatherData>();
	metricPrimary = input.required<boolean>();
	loc = input.required<(s: string) => string>();
	onToggleUnits = output<void>();

	tempDual = computed(() => { const [a, b] = this.data().temperature.displayDual(this.metricPrimary()); return [this.loc()(a), this.loc()(b)]; });
	feelsLikeDual = computed(() => { const [a, b] = this.data().feelsLike.displayDual(this.metricPrimary()); return [this.loc()(a), this.loc()(b)]; });
	snowDual = computed(() => { const s = this.data().snow; if (!s) return null; const [a, b] = s.displayDual(this.metricPrimary()); return [this.loc()(a), this.loc()(b)]; });
	rainDual = computed(() => { const r = this.data().rain; if (!r) return null; const [a, b] = r.displayDual(this.metricPrimary()); return [this.loc()(a), this.loc()(b)]; });
	minDual = computed(() => { const [a, b] = this.data().tempMin.displayDual(this.metricPrimary()); return [this.loc()(a), this.loc()(b)]; });
	maxDual = computed(() => { const [a, b] = this.data().tempMax.displayDual(this.metricPrimary()); return [this.loc()(a), this.loc()(b)]; });
	windDual = computed(() => { const [a, b] = this.data().windSpeed.displayDual(this.metricPrimary()); return [this.loc()(a), this.loc()(b)]; });
	gustDual = computed(() => { const g = this.data().windGust; if (!g) return null; const [a, b] = g.displayDual(this.metricPrimary()); return [this.loc()(a), this.loc()(b)]; });
	pressDual = computed(() => { const [a, b] = this.data().pressure.displayDual(this.metricPrimary()); return [this.loc()(a), this.loc()(b)]; });
	dewDual = computed(() => { const [a, b] = this.data().dewPoint.displayDual(this.metricPrimary()); return [this.loc()(a), this.loc()(b)]; });

	windSubtitle = computed(() => {
		const d = this.data();
		return this.loc()(`${this.getCardinalDirection(d.windDeg)} (${d.windDeg}°)`);
	});

	visDual = computed(() => {
		const m = this.data().visibility;
		const km = (m / 1000).toFixed(1);
		const mi = (m / 1609.34).toFixed(2);
		return this.metricPrimary()
			? [this.loc()(`${km} km`), this.loc()(`${mi} mi`)]
			: [this.loc()(`${mi} mi`), this.loc()(`${km} km`)];
	});

	uvLabel = computed(() => {
		const labels = this.i18n.tArray('uv_labels');
		if (labels.length === 0) return '';
		const uv = this.data().uvIndex;
		if (uv < 3) return labels[0];
		if (uv < 6) return labels[1];
		if (uv < 8) return labels[2];
		if (uv < 11) return labels[3];
		return labels[4];
	});

	formatTime(epochSec: number): string {
		const d = new Date(epochSec * 1000);
		return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
	}

	private getCardinalDirection(deg: number): string {
		const dirs = this.i18n.tArray('cardinal_directions');
		if (dirs.length === 0) return '';
		const idx = ((deg % 360 + 360) % 360 * 16 / 360) % 16;
		return dirs[Math.round(idx)] ?? '';
	}
}
