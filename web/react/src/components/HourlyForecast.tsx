import type { HourlyForecast as HourlyForecastType } from '$lib/domain/weatherData';
import { wmoEmoji } from '$lib/api/wmoWeatherCode';
import DualUnitText from './DualUnitText';

interface HourlyForecastProps {
	forecasts: HourlyForecastType[];
	metricPrimary: boolean;
	dailySunrise: number[];
	dailySunset: number[];
	loc: (s: string) => string;
	onToggleUnits: () => void;
}

function formatHour(millis: number): string {
	const d = new Date(millis);
	return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
}

function isNightForHour(time: number, dailySunrise: number[], dailySunset: number[]): boolean {
	let dayIdx = 0;
	for (let i = dailySunrise.length - 1; i >= 0; i--) {
		if (dailySunrise[i] <= time) { dayIdx = i; break; }
	}
	const sr = dailySunrise[dayIdx] ?? 0;
	const ss = dailySunset[dayIdx] ?? 0;
	return time < sr || time > ss;
}

export default function HourlyForecast({ forecasts, metricPrimary, dailySunrise, dailySunset, loc, onToggleUnits }: HourlyForecastProps) {
	return (
		<div className="hourly-row">
			{forecasts.map((item, i) => {
				const [tempP, tempS] = item.temperature.displayDual(metricPrimary);
				return (
					<div className="hourly-card" key={i}>
						<span className="hourly-hour">{loc(formatHour(item.time))}</span>
						<span className="hourly-emoji">{wmoEmoji(item.weatherCode, isNightForHour(item.time, dailySunrise, dailySunset))}</span>
						<DualUnitText
							primary={loc(tempP)}
							secondary={loc(tempS)}
							primarySize="14px"
							align="center"
							onClick={onToggleUnits}
						/>
						<span className="hourly-precip-prob">
							{item.precipProbability > 0 ? loc(`${item.precipProbability}%`) : ''}
						</span>
					</div>
				);
			})}
		</div>
	);
}
