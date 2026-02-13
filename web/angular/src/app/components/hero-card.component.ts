import { Component, input, output, inject, computed, viewChild, ElementRef } from '@angular/core';
import type { WeatherData } from '$lib/domain/weatherData';
import { wmoEmoji, wmoDescriptionKey } from '$lib/api/wmoWeatherCode';
import { I18nService } from '../services/i18n.service';
import { DualUnitTextComponent } from './dual-unit-text.component';

@Component({
	selector: 'app-hero-card',
	standalone: true,
	imports: [DualUnitTextComponent],
	template: `
		<div class="hero-card" #cardEl>
			@if (true) {
				<button class="share-btn" (click)="handleShare($event)" aria-label="Share">
					<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
						<path d="M4 12v8a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-8"/><polyline points="16 6 12 2 8 6"/><line x1="12" y1="2" x2="12" y2="15"/>
					</svg>
				</button>
			}
			<div class="hero-top">
				<div class="hero-weather">
					<span class="hero-emoji">{{ emoji() }}</span>
					<span class="hero-description">{{ description() }}</span>
				</div>
				<app-dual-unit-text [primary]="tempDual()[0]" [secondary]="tempDual()[1]" primarySize="48px" align="end" (onClick)="onToggleUnits.emit()" />
			</div>
			<div class="hero-bottom">
				<div class="hero-feels-like">
					<span class="label">{{ i18n.t('feels_like') }}</span>
					<app-dual-unit-text [primary]="feelsLikeDual()[0]" [secondary]="feelsLikeDual()[1]" (onClick)="onToggleUnits.emit()" />
				</div>
				<div class="hero-precip">
					@if (snowDual()) {
						<app-dual-unit-text [primary]="'\\u2744\\uFE0F ' + snowDual()![0]" [secondary]="snowDual()![1]" align="end" (onClick)="onToggleUnits.emit()" />
					} @else if (rainDual()) {
						<app-dual-unit-text [primary]="'\\uD83C\\uDF27\\uFE0F ' + rainDual()![0]" [secondary]="rainDual()![1]" align="end" (onClick)="onToggleUnits.emit()" />
					} @else {
						<span class="no-precip">{{ i18n.t('no_precip') }}</span>
					}
				</div>
			</div>
			<div class="hero-extra">
				<div class="hero-highlow">
					<app-dual-unit-text [primary]="'\\u2191 ' + maxDual()[0]" [secondary]="maxDual()[1]" (onClick)="onToggleUnits.emit()" />
					<app-dual-unit-text [primary]="'\\u2193 ' + minDual()[0]" [secondary]="minDual()[1]" (onClick)="onToggleUnits.emit()" />
				</div>
				<div class="hero-wind">
					<app-dual-unit-text [primary]="windDual()[0]" [secondary]="windDual()[1]" align="end" (onClick)="onToggleUnits.emit()" />
					<span class="wind-direction">{{ windCardinal() }}</span>
				</div>
			</div>
			<div class="copyright">&copy; Orinasa Njarasoa</div>
		</div>
	`,
	styles: `
		.hero-card { background: rgba(42,31,165,0.8); border-radius: 20px; padding: 24px; margin-bottom: 24px; position: relative; }
		.share-btn { position: absolute; top: 12px; right: 12px; background: rgba(255,255,255,0.1); border: none; border-radius: 50%; width: 32px; height: 32px; display: flex; align-items: center; justify-content: center; color: rgba(255,255,255,0.5); cursor: pointer; transition: background 0.2s, color 0.2s; }
		.share-btn:hover { background: rgba(255,255,255,0.2); color: rgba(255,255,255,0.8); }
		.hero-top { display: flex; align-items: center; justify-content: space-between; gap: 16px; }
		.hero-weather { display: flex; flex-direction: column; align-items: center; flex: 1; }
		.hero-emoji { font-size: 48px; line-height: 1; }
		.hero-description { font-size: 16px; color: rgba(255,255,255,0.9); text-align: center; margin-top: 4px; }
		.hero-bottom { display: flex; justify-content: space-between; align-items: flex-start; margin-top: 16px; }
		.hero-feels-like { flex: 1; }
		.label { font-size: 14px; color: rgba(255,255,255,0.7); display: block; margin-bottom: 2px; }
		.hero-precip { text-align: end; }
		.no-precip { font-size: 14px; color: rgba(255,255,255,0.5); }
		.hero-extra { display: flex; justify-content: space-between; align-items: flex-start; margin-top: 12px; }
		.hero-highlow { display: flex; flex-direction: column; gap: 2px; }
		.hero-wind { text-align: end; }
		.wind-direction { font-size: 12px; color: rgba(255,255,255,0.6); }
		.copyright { text-align: center; font-size: 9px; color: rgba(255,255,255,0.2); margin-top: 7px; }
	`,
})
export class HeroCardComponent {
	i18n = inject(I18nService);

	data = input.required<WeatherData>();
	metricPrimary = input.required<boolean>();
	loc = input.required<(s: string) => string>();
	onToggleUnits = output<void>();
	onShare = output<HTMLElement>();

	private cardEl = viewChild<ElementRef<HTMLElement>>('cardEl');

	isNight = computed(() => {
		const d = this.data();
		return d.timestamp < d.sunrise * 1000 || d.timestamp > d.sunset * 1000;
	});
	emoji = computed(() => wmoEmoji(this.data().weatherCode, this.isNight()));
	description = computed(() => this.i18n.t(wmoDescriptionKey(this.data().weatherCode)));
	tempDual = computed(() => {
		const [a, b] = this.data().temperature.displayDualMixed(this.metricPrimary());
		return [this.loc()(a), this.loc()(b)];
	});
	feelsLikeDual = computed(() => {
		const [a, b] = this.data().feelsLike.displayDual(this.metricPrimary());
		return [this.loc()(a), this.loc()(b)];
	});
	snowDual = computed(() => {
		const snow = this.data().snow;
		if (!snow) return null;
		const [a, b] = snow.displayDual(this.metricPrimary());
		return [this.loc()(a), this.loc()(b)];
	});
	rainDual = computed(() => {
		const rain = this.data().rain;
		if (!rain) return null;
		const [a, b] = rain.displayDual(this.metricPrimary());
		return [this.loc()(a), this.loc()(b)];
	});
	maxDual = computed(() => {
		const [a, b] = this.data().tempMax.displayDual(this.metricPrimary());
		return [this.loc()(a), this.loc()(b)];
	});
	minDual = computed(() => {
		const [a, b] = this.data().tempMin.displayDual(this.metricPrimary());
		return [this.loc()(a), this.loc()(b)];
	});
	windDual = computed(() => {
		const [a, b] = this.data().windSpeed.displayDual(this.metricPrimary());
		return [this.loc()(a), this.loc()(b)];
	});
	windCardinal = computed(() => {
		const dirs = this.i18n.tArray('cardinal_directions');
		if (dirs.length === 0) return '';
		const deg = this.data().windDeg;
		const idx = ((deg % 360 + 360) % 360 * 16 / 360) % 16;
		return this.loc()(dirs[Math.round(idx)] ?? '');
	});

	handleShare(e: MouseEvent) {
		e.stopPropagation();
		if (this.cardEl()) this.onShare.emit(this.cardEl()!.nativeElement);
	}
}
