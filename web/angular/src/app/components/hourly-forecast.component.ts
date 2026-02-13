import { Component, input, output } from '@angular/core';
import type { HourlyForecast } from '$lib/domain/weatherData';
import { wmoEmoji } from '$lib/api/wmoWeatherCode';
import { DualUnitTextComponent } from './dual-unit-text.component';

@Component({
	selector: 'app-hourly-forecast',
	standalone: true,
	imports: [DualUnitTextComponent],
	template: `
		<div class="hourly-row">
			@for (item of forecasts(); track $index) {
				<div class="hourly-card">
					<span class="hour">{{ loc()(formatHour(item.time)) }}</span>
					<span class="emoji">{{ getEmoji(item) }}</span>
					<app-dual-unit-text [primary]="loc()(getTemp(item)[0])" [secondary]="loc()(getTemp(item)[1])" primarySize="14px" align="center" (onClick)="onToggleUnits.emit()" />
					<span class="precip-prob">{{ item.precipProbability > 0 ? loc()(item.precipProbability + '%') : '' }}</span>
				</div>
			}
		</div>
	`,
	styles: `
		.hourly-row { display: flex; flex-wrap: nowrap; gap: 12px; overflow-x: auto; scroll-snap-type: x mandatory; padding: 8px 0; max-width: 100%; -webkit-overflow-scrolling: touch; }
		.hourly-row::-webkit-scrollbar { display: none; }
		.hourly-card { display: flex; flex-direction: column; align-items: center; gap: 4px; padding: 12px; background: rgba(42,31,165,0.6); border-radius: 16px; scroll-snap-align: start; flex-shrink: 0; min-width: 80px; }
		.hour { font-size: 12px; color: rgba(255,255,255,0.7); font-feature-settings: var(--font-features); }
		.emoji { font-size: 20px; }
		.precip-prob { font-size: 11px; color: #64B5F6; min-height: 14px; font-feature-settings: var(--font-features); }
	`,
})
export class HourlyForecastComponent {
	forecasts = input.required<HourlyForecast[]>();
	metricPrimary = input.required<boolean>();
	dailySunrise = input.required<number[]>();
	dailySunset = input.required<number[]>();
	loc = input.required<(s: string) => string>();
	onToggleUnits = output<void>();

	formatHour(millis: number): string {
		const d = new Date(millis);
		return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
	}

	getEmoji(item: HourlyForecast): string {
		return wmoEmoji(item.weatherCode, this.isNightForHour(item.time));
	}

	getTemp(item: HourlyForecast): [string, string] {
		return item.temperature.displayDual(this.metricPrimary());
	}

	private isNightForHour(time: number): boolean {
		const sr = this.dailySunrise();
		const ss = this.dailySunset();
		let dayIdx = 0;
		for (let i = sr.length - 1; i >= 0; i--) {
			if (sr[i] <= time) { dayIdx = i; break; }
		}
		return time < (sr[dayIdx] ?? 0) || time > (ss[dayIdx] ?? 0);
	}
}
