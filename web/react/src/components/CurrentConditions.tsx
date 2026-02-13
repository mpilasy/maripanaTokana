import { useTranslation } from 'react-i18next';
import type { WeatherData } from '$lib/domain/weatherData';
import DetailCard from './DetailCard';

interface CurrentConditionsProps {
	data: WeatherData;
	metricPrimary: boolean;
	loc: (s: string) => string;
	onToggleUnits: () => void;
}

function formatTime(epochSec: number): string {
	const d = new Date(epochSec * 1000);
	return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
}

function getCardinalDirection(deg: number, t: (key: string) => unknown): string {
	const dirs = t('cardinal_directions') as unknown as string[];
	if (!Array.isArray(dirs)) return '';
	const idx = ((deg % 360 + 360) % 360 * 16 / 360) % 16;
	return dirs[Math.round(idx)] ?? '';
}

function getUvLabel(uv: number, t: (key: string) => unknown): string {
	const labels = t('uv_labels') as unknown as string[];
	if (!Array.isArray(labels)) return '';
	if (uv < 3) return labels[0];
	if (uv < 6) return labels[1];
	if (uv < 8) return labels[2];
	if (uv < 11) return labels[3];
	return labels[4];
}

function visibilityDisplay(meters: number, metric: boolean): [string, string] {
	const km = (meters / 1000).toFixed(1);
	const mi = (meters / 1609.34).toFixed(2);
	return metric
		? [`${km} km`, `${mi} mi`]
		: [`${mi} mi`, `${km} km`];
}

export default function CurrentConditions({ data, metricPrimary, loc, onToggleUnits }: CurrentConditionsProps) {
	const { t } = useTranslation();

	const tempDual = data.temperature.displayDual(metricPrimary);
	const feelsLikeDual = data.feelsLike.displayDual(metricPrimary);
	const snowDual = data.snow?.displayDual(metricPrimary);
	const rainDual = data.rain?.displayDual(metricPrimary);
	const minDual = data.tempMin.displayDual(metricPrimary);
	const maxDual = data.tempMax.displayDual(metricPrimary);
	const windDual = data.windSpeed.displayDual(metricPrimary);
	const gustDual = data.windGust?.displayDual(metricPrimary);
	const pressDual = data.pressure.displayDual(metricPrimary);
	const dewDual = data.dewPoint.displayDual(metricPrimary);
	const visDual = visibilityDisplay(data.visibility, metricPrimary);

	return (
		<div className="conditions-grid">
			{/* Temperature + Precipitation merged card */}
			<div className="merged-card temp-precip-card" onClick={onToggleUnits}>
				<div className="tp-side">
					<span className="detail-card-title">{t('detail_temperature')}</span>
					<span className="tp-values">
						<span className="tp-primary">{loc(tempDual[0])}</span>
						<span className="tp-secondary">{loc(tempDual[1])}</span>
					</span>
					<span className="feels-label">{t('feels_like')}</span>
					<span className="tp-values">
						<span className="feels-primary">{loc(feelsLikeDual[0])}</span>
						<span className="feels-secondary">{loc(feelsLikeDual[1])}</span>
					</span>
				</div>
				<div className="tp-side tp-side-end">
					<span className="detail-card-title">{t('detail_precipitation')}</span>
					{snowDual ? (
						<span className="tp-values tp-values-end">
							<span className="tp-primary">{"\u2744\uFE0F " + loc(snowDual[0])}</span>
							<span className="tp-secondary">{loc(snowDual[1])}</span>
						</span>
					) : rainDual ? (
						<span className="tp-values tp-values-end">
							<span className="tp-primary">{"\uD83C\uDF27\uFE0F " + loc(rainDual[0])}</span>
							<span className="tp-secondary">{loc(rainDual[1])}</span>
						</span>
					) : (
						<span className="tp-primary">{t('no_precip')}</span>
					)}
					<span className="feels-label">{t('detail_cloud_cover')}</span>
					<span className="feels-primary">{loc(`${data.cloudCover}%`)}</span>
				</div>
			</div>

			{/* High / Low merged card */}
			<div className="merged-card highlow-card" onClick={onToggleUnits}>
				<span className="highlow-arrow">↓</span>
				<span className="merged-values">
					<span className="merged-primary">{loc(minDual[0])}</span>
					<span className="merged-secondary">{loc(minDual[1])}</span>
				</span>
				<span className="merged-label">{t('detail_high_low')}</span>
				<span className="merged-values merged-values-end">
					<span className="merged-primary">{loc(maxDual[0])}</span>
					<span className="merged-secondary">{loc(maxDual[1])}</span>
				</span>
				<span className="highlow-arrow">↑</span>
			</div>

			{/* Wind merged card */}
			<div className="merged-card wind-merged-card" onClick={onToggleUnits}>
				<div className="wind-side">
					<span className="merged-values">
						<span className="merged-primary">{loc(windDual[0])}</span>
						<span className="merged-secondary">{loc(windDual[1])}</span>
					</span>
					<span className="wind-subtitle">{loc(`${getCardinalDirection(data.windDeg, t)} (${data.windDeg}°)`)}</span>
				</div>
				<span className="merged-label">{t('detail_wind')}</span>
				<div className="wind-side wind-side-end">
					{gustDual && (
						<>
							<span className="merged-values merged-values-end">
								<span className="merged-primary">{loc(gustDual[0])}</span>
								<span className="merged-secondary">{loc(gustDual[1])}</span>
							</span>
							<span className="wind-subtitle">{t('detail_wind_gust')}</span>
						</>
					)}
				</div>
			</div>

			{/* Sunrise / Sunset merged card */}
			<div className="merged-card sun-card">
				<div className="sun-side">
					<span className="sun-time">{loc(formatTime(data.sunrise))}</span>
					<span className="sun-label">{t('detail_sunrise')}</span>
				</div>
				<span className="sun-icon">☀️</span>
				<div className="sun-side sun-side-end">
					<span className="sun-time">{loc(formatTime(data.sunset))}</span>
					<span className="sun-label">{t('detail_sunset')}</span>
				</div>
			</div>

			<DetailCard
				title={t('detail_pressure')}
				value={loc(pressDual[0])}
				secondaryValue={loc(pressDual[1])}
				onToggleUnits={onToggleUnits}
			/>
			{/* Humidity + Dew point combined card */}
			<div className="detail-card humidity-card">
				<span className="detail-card-title">{t('detail_humidity')}</span>
				<span className="detail-card-value">{loc(`${data.humidity}%`)}</span>
				<span className="dew-label">{t('detail_dewpoint')}</span>
				<span className="dew-values" onClick={onToggleUnits}>
					<span className="dew-primary">{loc(dewDual[0])}</span>
					<span className="dew-secondary">{loc(dewDual[1])}</span>
				</span>
			</div>

			<DetailCard
				title={t('detail_uv_index')}
				value={loc(data.uvIndex.toFixed(1))}
				subtitle={getUvLabel(data.uvIndex, t)}
			/>
			<DetailCard
				title={t('detail_visibility')}
				value={loc(visDual[0])}
				secondaryValue={loc(visDual[1])}
				onToggleUnits={onToggleUnits}
			/>
		</div>
	);
}
