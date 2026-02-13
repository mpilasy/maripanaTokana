import { Component, inject, computed, signal, viewChild, ElementRef, OnInit, OnDestroy } from '@angular/core';
import { SUPPORTED_LOCALES, localizeDigits } from '$lib/i18n/locales';
import { fontPairings } from '$lib/fonts';
import { captureAndShare } from '$lib/share';
import { WeatherService } from '../services/weather.service';
import { PreferencesService } from '../services/preferences.service';
import { I18nService } from '../services/i18n.service';
import { TranslatePipe } from '../pipes/translate.pipe';
import { HeroCardComponent } from './hero-card.component';
import { HourlyForecastComponent } from './hourly-forecast.component';
import { DailyForecastComponent } from './daily-forecast.component';
import { CurrentConditionsComponent } from './current-conditions.component';
import { CollapsibleSectionComponent } from './collapsible-section.component';
import { FooterComponent } from './footer.component';

@Component({
	selector: 'app-weather-screen',
	standalone: true,
	imports: [
		TranslatePipe, HeroCardComponent, HourlyForecastComponent,
		DailyForecastComponent, CurrentConditionsComponent,
		CollapsibleSectionComponent, FooterComponent,
	],
	template: `
		<div class="weather-screen">
			<div class="bg-marble"></div>

			@if (weather.weatherState().kind === 'loading') {
				<div class="center"><div class="spinner"></div></div>
			}

			@if (weather.weatherState().kind === 'success') {
				@if (pullDelta() > 0) {
					<div class="pull-indicator" [style.transform]="'translateY(' + pullDeltaClamped() + 'px)'">
						<div class="pull-spinner" [class.active]="pullDelta() > 80"></div>
					</div>
				}
				@if (weather.isRefreshing()) {
					<div class="refresh-bar"><div class="refresh-spinner"></div></div>
				}

				<div class="content-wrapper">
					<div class="header">
						<div #headerEl>
							<h1 class="location-name">{{ successData()!.locationName }}</h1>
							<p class="date">{{ formatDate(successData()!.timestamp) }}</p>
						</div>
						<p class="updated" (click)="weather.doFetchWeather()">{{ 'updated_time' | translate:{ time: loc(formatTime(successData()!.timestamp)) } }}</p>
					</div>

					<div class="scroll-area" role="region" #scrollEl
						(touchstart)="handleTouchStart($event)"
						(touchmove)="handleTouchMove($event)"
						(touchend)="handleTouchEnd()">

						<app-hero-card [data]="successData()!" [metricPrimary]="prefs.metricPrimary()" [loc]="locFn" (onToggleUnits)="prefs.toggleUnits()" (onShare)="handleShare($event)" />

						@if (successData()!.hourlyForecast.length > 0) {
							<app-collapsible-section [title]="i18n.t('section_hourly_forecast')" [expanded]="true" (onShare)="handleShare($event)">
								<app-hourly-forecast [forecasts]="successData()!.hourlyForecast" [metricPrimary]="prefs.metricPrimary()" [dailySunrise]="successData()!.dailySunrise" [dailySunset]="successData()!.dailySunset" [loc]="locFn" (onToggleUnits)="prefs.toggleUnits()" />
							</app-collapsible-section>
						}

						@if (successData()!.dailyForecast.length > 0) {
							<app-collapsible-section [title]="i18n.t('section_this_week')" (onShare)="handleShare($event)">
								<app-daily-forecast [forecasts]="successData()!.dailyForecast" [metricPrimary]="prefs.metricPrimary()" [localeTag]="currentLocaleTag()" [loc]="locFn" (onToggleUnits)="prefs.toggleUnits()" />
							</app-collapsible-section>
						}

						<app-collapsible-section [title]="i18n.t('section_current_conditions')" (onShare)="handleShare($event)">
							<app-current-conditions [data]="successData()!" [metricPrimary]="prefs.metricPrimary()" [loc]="locFn" (onToggleUnits)="prefs.toggleUnits()" />
						</app-collapsible-section>

						<div class="scroll-bottom-pad"></div>
					</div>

					<app-footer [fontName]="fontPairings[prefs.fontIndex()].name" [currentFlag]="currentLocale().flag" (onCycleFont)="prefs.cycleFont()" (onCycleLanguage)="prefs.cycleLanguage()" />
				</div>
			}

			@if (weather.weatherState().kind === 'error') {
				<div class="center error-state">
					<h2>{{ 'error_title' | translate }}</h2>
					<p>{{ errorMessage() | translate }}</p>
					@if (showSecondary() && browserStrings()) {
						<div class="secondary-block">
							<h3>{{ browserStrings()!['error_title'] }}</h3>
							<p>{{ browserStrings()![errorMessage()] }}</p>
						</div>
					}
					<button (click)="weather.doFetchWeather()">
						{{ 'error_retry' | translate }}
						@if (showSecondary() && browserStrings()) {
							<span class="btn-secondary">{{ browserStrings()!['error_retry'] }}</span>
						}
					</button>
				</div>
			}
		</div>
	`,
	styles: `
		.weather-screen { width: 100%; height: 100%; position: relative; }
		.bg-marble { position: absolute; inset: 0; background: url('/bg-blue-marble.webp') center/cover no-repeat; opacity: 0.12; pointer-events: none; }
		.center { display: flex; flex-direction: column; align-items: center; justify-content: center; height: 100%; gap: 16px; }
		.spinner { width: 40px; height: 40px; border: 3px solid rgba(255,255,255,0.2); border-top-color: white; border-radius: 50%; animation: spin 0.8s linear infinite; }
		@keyframes spin { to { transform: rotate(360deg); } }
		.content-wrapper { display: flex; flex-direction: column; height: 100%; position: relative; z-index: 1; padding: 0 24px; padding-top: env(safe-area-inset-top); }
		.header { padding-top: 24px; flex-shrink: 0; }
		.location-name { font-family: var(--font-display); font-size: 32px; font-weight: 700; color: white; }
		.date { font-size: 16px; color: rgba(255,255,255,0.7); }
		.updated { font-size: 12px; color: rgba(255,255,255,0.4); cursor: pointer; }
		.scroll-area { flex: 1; min-width: 0; overflow-y: auto; overflow-x: hidden; padding: 24px 0; -webkit-overflow-scrolling: touch; }
		.scroll-bottom-pad { height: 24px; }
		.pull-indicator { position: absolute; top: 0; left: 50%; transform: translateX(-50%); z-index: 10; }
		.pull-spinner { width: 24px; height: 24px; border: 2px solid rgba(255,255,255,0.3); border-top-color: white; border-radius: 50%; transition: opacity 0.2s; }
		.pull-spinner.active { animation: spin 0.8s linear infinite; }
		.refresh-bar { position: absolute; top: env(safe-area-inset-top); left: 50%; transform: translateX(-50%); z-index: 10; padding: 8px; }
		.refresh-spinner { width: 20px; height: 20px; border: 2px solid rgba(255,255,255,0.3); border-top-color: white; border-radius: 50%; animation: spin 0.8s linear infinite; }
		.secondary-block { opacity: 0.45; text-align: center; padding: 0 16px; }
		.secondary-block h3 { font-family: var(--font-display); font-size: 16px; font-weight: 600; margin-bottom: 8px; }
		.secondary-block p { font-size: 13px; line-height: 1.4; }
		.btn-secondary { display: block; font-size: 11px; opacity: 0.4; margin-top: 4px; font-weight: 400; color: inherit; }
		.error-state h2 { font-family: var(--font-display); font-size: 24px; font-weight: 700; }
		.error-state p { color: rgba(255,255,255,0.7); }
		.error-state button { margin-top: 24px; padding: 16px 48px; background: white; color: #0E0B3D; border: none; border-radius: 14px; font-size: 18px; font-weight: 700; cursor: pointer; transition: background 0.2s, transform 0.1s; box-shadow: 0 4px 20px rgba(255,255,255,0.25); }
		.error-state button:hover { background: rgba(255,255,255,0.9); }
		.error-state button:active { transform: scale(0.97); }
	`,
})
export class WeatherScreenComponent implements OnInit, OnDestroy {
	weather = inject(WeatherService);
	prefs = inject(PreferencesService);
	i18n = inject(I18nService);
	fontPairings = fontPairings;

	private headerEl = viewChild<ElementRef<HTMLElement>>('headerEl');
	private scrollEl = viewChild<ElementRef<HTMLElement>>('scrollEl');

	pullStartY = signal(0);
	pullDelta = signal(0);
	isPulling = signal(false);
	pullDeltaClamped = computed(() => Math.min(this.pullDelta(), 100));

	currentLocale = computed(() => SUPPORTED_LOCALES[this.prefs.localeIndex()]);
	currentLocaleTag = computed(() => this.currentLocale().tag);

	locFn = (s: string): string => localizeDigits(s, SUPPORTED_LOCALES[this.prefs.localeIndex()]);

	loc(s: string): string {
		return localizeDigits(s, SUPPORTED_LOCALES[this.prefs.localeIndex()]);
	}

	successData = computed(() => {
		const s = this.weather.weatherState();
		return s.kind === 'success' ? s.data : null;
	});

	errorMessage = computed(() => {
		const s = this.weather.weatherState();
		return s.kind === 'error' ? s.message : '';
	});

	private browserLocaleTag = (() => {
		if (typeof navigator === 'undefined') return null;
		const lang = navigator.language.split('-')[0].toLowerCase();
		return SUPPORTED_LOCALES.find(l => l.tag === lang)?.tag ?? null;
	})();

	browserStrings = computed(() => this.browserLocaleTag ? this.i18n.getLocaleStrings(this.browserLocaleTag) : null);

	showSecondary = computed(() =>
		this.browserLocaleTag != null
		&& this.browserLocaleTag !== this.currentLocaleTag()
		&& this.browserStrings() != null
	);

	private visibilityHandler = () => {
		if (document.visibilityState === 'visible') this.weather.refreshIfStale();
	};

	ngOnInit() {
		this.weather.doFetchWeather();
		document.addEventListener('visibilitychange', this.visibilityHandler);
	}

	ngOnDestroy() {
		document.removeEventListener('visibilitychange', this.visibilityHandler);
	}

	formatDate(timestamp: number): string {
		return new Intl.DateTimeFormat(this.currentLocaleTag(), {
			weekday: 'long', day: 'numeric', month: 'long', year: 'numeric'
		}).format(new Date(timestamp));
	}

	formatTime(timestamp: number): string {
		const d = new Date(timestamp);
		return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
	}

	handleTouchStart(e: TouchEvent) {
		if (this.scrollEl() && this.scrollEl()!.nativeElement.scrollTop <= 0) {
			this.pullStartY.set(e.touches[0].clientY);
			this.isPulling.set(true);
		}
	}

	handleTouchMove(e: TouchEvent) {
		if (!this.isPulling()) return;
		this.pullDelta.set(Math.max(0, e.touches[0].clientY - this.pullStartY()));
	}

	handleTouchEnd() {
		if (this.pullDelta() > 80) this.weather.doFetchWeather();
		this.pullDelta.set(0);
		this.isPulling.set(false);
	}

	handleShare(el: HTMLElement) {
		if (!this.headerEl()) return;
		captureAndShare(this.headerEl()!.nativeElement, el);
	}
}
