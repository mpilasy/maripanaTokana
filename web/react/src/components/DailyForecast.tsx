import { useTranslation } from 'react-i18next';
import type { DailyForecast as DailyForecastType } from '$lib/domain/weatherData';
import { wmoEmoji, wmoDescriptionKey } from '$lib/api/wmoWeatherCode';
import DualUnitText from './DualUnitText';

interface DailyForecastProps {
	forecasts: DailyForecastType[];
	metricPrimary: boolean;
	localeTag: string;
	loc: (s: string) => string;
	onToggleUnits: () => void;
}

function formatDayName(millis: number, localeTag: string): string {
	return new Intl.DateTimeFormat(localeTag, { weekday: 'long' }).format(new Date(millis));
}

function formatDayMonth(millis: number, localeTag: string): string {
	return new Intl.DateTimeFormat(localeTag, { day: 'numeric', month: 'short' }).format(new Date(millis));
}

export default function DailyForecast({ forecasts, metricPrimary, localeTag, loc, onToggleUnits }: DailyForecastProps) {
	const { t } = useTranslation();

	return (
		<div className="daily-list">
			{forecasts.map((item, i) => {
				const [maxP, maxS] = item.tempMax.displayDual(metricPrimary);
				const [minP, minS] = item.tempMin.displayDual(metricPrimary);
				return (
					<div className="daily-row" key={i}>
						<div className="daily-day-info">
							<span className="daily-day-name">{formatDayName(item.date, localeTag)}</span>
							<span className="daily-day-date">{loc(formatDayMonth(item.date, localeTag))}</span>
						</div>
						<span className="daily-weather">
							{wmoEmoji(item.weatherCode)} {t(wmoDescriptionKey(item.weatherCode))}
						</span>
						<span className="daily-precip">
							{item.precipProbability > 0 ? loc(`${item.precipProbability}%`) : ''}
						</span>
						<DualUnitText
							primary={loc(`\u2191${maxP} \u2193${minP}`)}
							secondary={loc(`\u2191${maxS} \u2193${minS}`)}
							primarySize="13px"
							onClick={onToggleUnits}
						/>
					</div>
				);
			})}
		</div>
	);
}
