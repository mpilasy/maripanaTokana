import { Component, input, output, inject } from '@angular/core';
import type { DailyForecast } from '$lib/domain/weatherData';
import { wmoEmoji, wmoDescriptionKey } from '$lib/api/wmoWeatherCode';
import { I18nService } from '../services/i18n.service';
import { DualUnitTextComponent } from './dual-unit-text.component';

@Component({
	selector: 'app-daily-forecast',
	standalone: true,
	imports: [DualUnitTextComponent],
	template: `
		<div class="daily-list">
			@for (item of forecasts(); track $index) {
				<div class="daily-row">
					<div class="day-info">
						<span class="day-name">{{ formatDayName(item.date) }}</span>
						<span class="day-date">{{ loc()(formatDayMonth(item.date)) }}</span>
					</div>
					<span class="daily-weather">{{ getEmoji(item) }} {{ i18n.t(getDescKey(item)) }}</span>
					<span class="daily-precip">{{ item.precipProbability > 0 ? loc()(item.precipProbability + '%') : '' }}</span>
					<app-dual-unit-text
						[primary]="loc()('↑' + getMaxMin(item)[0] + ' ↓' + getMaxMin(item)[2])"
						[secondary]="loc()('↑' + getMaxMin(item)[1] + ' ↓' + getMaxMin(item)[3])"
						primarySize="13px"
						(onClick)="onToggleUnits.emit()" />
				</div>
			}
		</div>
	`,
	styles: `
		.daily-list { display: flex; flex-direction: column; gap: 8px; }
		.daily-row { display: flex; align-items: center; gap: 8px; padding: 12px 16px; background: rgba(42,31,165,0.3); border-radius: 12px; }
		.day-info { display: flex; flex-direction: column; width: 100px; flex-shrink: 0; }
		.day-name { font-size: 14px; font-weight: 500; color: white; }
		.day-date { font-size: 10px; color: rgba(255,255,255,0.4); }
		.daily-weather { font-size: 12px; color: rgba(255,255,255,0.7); flex: 1; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
		.daily-precip { font-size: 11px; color: #64B5F6; min-width: 30px; text-align: end; font-feature-settings: var(--font-features); }
	`,
})
export class DailyForecastComponent {
	i18n = inject(I18nService);

	forecasts = input.required<DailyForecast[]>();
	metricPrimary = input.required<boolean>();
	localeTag = input.required<string>();
	loc = input.required<(s: string) => string>();
	onToggleUnits = output<void>();

	formatDayName(millis: number): string {
		return new Intl.DateTimeFormat(this.localeTag(), { weekday: 'long' }).format(new Date(millis));
	}

	formatDayMonth(millis: number): string {
		return new Intl.DateTimeFormat(this.localeTag(), { day: 'numeric', month: 'short' }).format(new Date(millis));
	}

	getEmoji(item: DailyForecast): string {
		return wmoEmoji(item.weatherCode);
	}

	getDescKey(item: DailyForecast): string {
		return wmoDescriptionKey(item.weatherCode);
	}

	getMaxMin(item: DailyForecast): [string, string, string, string] {
		const [maxP, maxS] = item.tempMax.displayDual(this.metricPrimary());
		const [minP, minS] = item.tempMin.displayDual(this.metricPrimary());
		return [maxP, maxS, minP, minS];
	}
}
