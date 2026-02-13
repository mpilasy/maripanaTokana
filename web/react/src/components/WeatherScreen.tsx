import { useEffect, useRef, useState, useCallback, useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import { SUPPORTED_LOCALES, localizeDigits } from '$lib/i18n/locales';
import { fontPairings } from '$lib/fonts';
import { captureAndShare } from '$lib/share';
import { useWeather } from '../hooks/useWeather';
import { usePreferences } from '../hooks/usePreferences';
import { getLocaleStrings } from '../i18n';
import HeroCard from './HeroCard';
import HourlyForecast from './HourlyForecast';
import DailyForecast from './DailyForecast';
import CurrentConditions from './CurrentConditions';
import CollapsibleSection from './CollapsibleSection';
import Footer from './Footer';

function findBrowserLocaleTag(): string | null {
	if (typeof navigator === 'undefined') return null;
	const lang = navigator.language.split('-')[0].toLowerCase();
	return SUPPORTED_LOCALES.find(l => l.tag === lang)?.tag ?? null;
}

export default function WeatherScreen() {
	const { t } = useTranslation();
	const { weatherState, isRefreshing, doFetchWeather } = useWeather();
	const { metricPrimary, fontIndex, localeIndex, toggleUnits, cycleFont, cycleLanguage } = usePreferences();

	const browserLocaleTag = useMemo(findBrowserLocaleTag, []);
	const browserStrings = useMemo(() => browserLocaleTag ? getLocaleStrings(browserLocaleTag) : null, [browserLocaleTag]);

	const showSecondary = browserLocaleTag != null
		&& browserLocaleTag !== SUPPORTED_LOCALES[localeIndex]?.tag
		&& browserStrings != null;

	const [pullStartY, setPullStartY] = useState(0);
	const [pullDelta, setPullDelta] = useState(0);
	const [isPulling, setIsPulling] = useState(false);
	const scrollRef = useRef<HTMLDivElement>(null);
	const headerRef = useRef<HTMLDivElement>(null);

	const loc = useCallback((s: string): string => {
		return localizeDigits(s, SUPPORTED_LOCALES[localeIndex]);
	}, [localeIndex]);

	function formatDate(timestamp: number): string {
		const locale = SUPPORTED_LOCALES[localeIndex];
		const d = new Date(timestamp);
		return new Intl.DateTimeFormat(locale.tag, {
			weekday: 'long', day: 'numeric', month: 'long', year: 'numeric'
		}).format(d);
	}

	function formatTime(timestamp: number): string {
		const d = new Date(timestamp);
		const hh = String(d.getHours()).padStart(2, '0');
		const mm = String(d.getMinutes()).padStart(2, '0');
		return `${hh}:${mm}`;
	}

	function handleTouchStart(e: React.TouchEvent) {
		if (scrollRef.current && scrollRef.current.scrollTop <= 0) {
			setPullStartY(e.touches[0].clientY);
			setIsPulling(true);
		}
	}

	function handleTouchMove(e: React.TouchEvent) {
		if (!isPulling) return;
		setPullDelta(Math.max(0, e.touches[0].clientY - pullStartY));
	}

	function handleTouchEnd() {
		if (pullDelta > 80) {
			doFetchWeather();
		}
		setPullDelta(0);
		setIsPulling(false);
	}

	function handleShare(el: HTMLElement) {
		if (!headerRef.current) return;
		captureAndShare(headerRef.current, el);
	}

	useEffect(() => {
		doFetchWeather();
	}, [doFetchWeather]);

	return (
		<div className="weather-screen">
			<div className="bg-marble" />

			{weatherState.kind === 'loading' && (
				<div className="center">
					<div className="spinner" />
				</div>
			)}

			{weatherState.kind === 'success' && (() => {
				const data = weatherState.data;
				return (
					<>
						{pullDelta > 0 && (
							<div className="pull-indicator" style={{ transform: `translateY(${Math.min(pullDelta, 100)}px)` }}>
								<div className={`pull-spinner${pullDelta > 80 ? ' active' : ''}`} />
							</div>
						)}

						{isRefreshing && (
							<div className="refresh-bar">
								<div className="refresh-spinner" />
							</div>
						)}

						<div className="content-wrapper">
							<div className="header">
								<div ref={headerRef}>
									<h1 className="location-name">{data.locationName}</h1>
									<p className="date">{formatDate(data.timestamp)}</p>
								</div>
								<p className="updated" onClick={doFetchWeather}>{t('updated_time', { time: loc(formatTime(data.timestamp)) })}</p>
							</div>

							<div
								className="scroll-area"
								role="region"
								ref={scrollRef}
								onTouchStart={handleTouchStart}
								onTouchMove={handleTouchMove}
								onTouchEnd={handleTouchEnd}
							>
								<HeroCard data={data} metricPrimary={metricPrimary} loc={loc} onToggleUnits={toggleUnits} onShare={handleShare} />

								{data.hourlyForecast.length > 0 && (
									<CollapsibleSection title={t('section_hourly_forecast')} expanded={true} onShare={handleShare}>
										<HourlyForecast
											forecasts={data.hourlyForecast}
											metricPrimary={metricPrimary}
											dailySunrise={data.dailySunrise}
											dailySunset={data.dailySunset}
											loc={loc}
											onToggleUnits={toggleUnits}
										/>
									</CollapsibleSection>
								)}

								{data.dailyForecast.length > 0 && (
									<CollapsibleSection title={t('section_this_week')} onShare={handleShare}>
										<DailyForecast
											forecasts={data.dailyForecast}
											metricPrimary={metricPrimary}
											localeTag={SUPPORTED_LOCALES[localeIndex].tag}
											loc={loc}
											onToggleUnits={toggleUnits}
										/>
									</CollapsibleSection>
								)}

								<CollapsibleSection title={t('section_current_conditions')} onShare={handleShare}>
									<CurrentConditions data={data} metricPrimary={metricPrimary} loc={loc} onToggleUnits={toggleUnits} />
								</CollapsibleSection>

								<div className="scroll-bottom-pad" />
							</div>

							<Footer
								fontName={fontPairings[fontIndex].name}
								currentFlag={SUPPORTED_LOCALES[localeIndex].flag}
								onCycleFont={cycleFont}
								onCycleLanguage={cycleLanguage}
							/>
						</div>
					</>
				);
			})()}

			{weatherState.kind === 'error' && (
				<div className="center error-state">
					<h2>{t('error_title')}</h2>
					<p>{t(weatherState.message)}</p>
					{showSecondary && browserStrings && (
						<div className="secondary-block">
							<h3>{browserStrings.error_title}</h3>
							<p>{browserStrings[weatherState.message]}</p>
						</div>
					)}
					<button onClick={doFetchWeather}>
						{t('error_retry')}
						{showSecondary && browserStrings && (
							<span className="btn-secondary">{browserStrings.error_retry}</span>
						)}
					</button>
				</div>
			)}
		</div>
	);
}
