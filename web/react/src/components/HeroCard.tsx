import { useRef } from 'react';
import { useTranslation } from 'react-i18next';
import type { WeatherData } from '$lib/domain/weatherData';
import { wmoEmoji, wmoDescriptionKey } from '$lib/api/wmoWeatherCode';
import DualUnitText from './DualUnitText';

interface HeroCardProps {
	data: WeatherData;
	metricPrimary: boolean;
	loc: (s: string) => string;
	onToggleUnits: () => void;
	onShare?: (el: HTMLElement) => void;
}

export default function HeroCard({ data, metricPrimary, loc, onToggleUnits, onShare }: HeroCardProps) {
	const { t } = useTranslation();
	const cardRef = useRef<HTMLDivElement>(null);

	const isNight = data.timestamp < data.sunrise * 1000 || data.timestamp > data.sunset * 1000;
	const emoji = wmoEmoji(data.weatherCode, isNight);
	const description = t(wmoDescriptionKey(data.weatherCode));
	const tempDual = data.temperature.displayDualMixed(metricPrimary);
	const feelsLikeDual = data.feelsLike.displayDual(metricPrimary);

	function handleShare(e: React.MouseEvent) {
		e.stopPropagation();
		if (cardRef.current && onShare) onShare(cardRef.current);
	}

	const snowDual = data.snow?.displayDual(metricPrimary);
	const rainDual = data.rain?.displayDual(metricPrimary);
	const maxDual = data.tempMax.displayDual(metricPrimary);
	const minDual = data.tempMin.displayDual(metricPrimary);
	const windDual = data.windSpeed.displayDual(metricPrimary);

	function getCardinalDirection(deg: number): string {
		const dirs = t('cardinal_directions') as unknown as string[];
		if (!Array.isArray(dirs)) return '';
		const idx = ((deg % 360 + 360) % 360 * 16 / 360) % 16;
		return dirs[Math.round(idx)] ?? '';
	}

	return (
		<div className="hero-card" ref={cardRef}>
			{onShare && (
				<button className="hero-share-btn" onClick={handleShare} aria-label="Share">
					<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
						<path d="M4 12v8a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-8"/>
						<polyline points="16 6 12 2 8 6"/>
						<line x1="12" y1="2" x2="12" y2="15"/>
					</svg>
				</button>
			)}

			<div className="hero-top">
				<div className="hero-weather">
					<span className="hero-emoji">{emoji}</span>
					<span className="hero-description">{description}</span>
				</div>
				<DualUnitText
					primary={loc(tempDual[0])}
					secondary={loc(tempDual[1])}
					primarySize="48px"
					align="end"
					onClick={onToggleUnits}
				/>
			</div>

			<div className="hero-bottom">
				<div className="hero-feels-like">
					<span className="hero-label">{t('feels_like')}</span>
					<DualUnitText
						primary={loc(feelsLikeDual[0])}
						secondary={loc(feelsLikeDual[1])}
						onClick={onToggleUnits}
					/>
				</div>
				<div className="hero-precip">
					{snowDual ? (
						<DualUnitText
							primary={"\u2744\uFE0F " + loc(snowDual[0])}
							secondary={loc(snowDual[1])}
							align="end"
							onClick={onToggleUnits}
						/>
					) : rainDual ? (
						<DualUnitText
							primary={"\uD83C\uDF27\uFE0F " + loc(rainDual[0])}
							secondary={loc(rainDual[1])}
							align="end"
							onClick={onToggleUnits}
						/>
					) : (
						<span className="no-precip">{t('no_precip')}</span>
					)}
				</div>
			</div>

			<div className="hero-extra">
				<div className="hero-highlow">
					<DualUnitText
						primary={"\u2191 " + loc(maxDual[0])}
						secondary={loc(maxDual[1])}
						onClick={onToggleUnits}
					/>
					<DualUnitText
						primary={"\u2193 " + loc(minDual[0])}
						secondary={loc(minDual[1])}
						onClick={onToggleUnits}
					/>
				</div>
				<div className="hero-wind">
					<DualUnitText
						primary={loc(windDual[0])}
						secondary={loc(windDual[1])}
						align="end"
						onClick={onToggleUnits}
					/>
					<span className="wind-direction">{loc(getCardinalDirection(data.windDeg))}</span>
				</div>
			</div>

			<div className="copyright">&copy; Orinasa Njarasoa</div>
		</div>
	);
}
