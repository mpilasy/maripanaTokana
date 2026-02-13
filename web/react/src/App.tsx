import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { fontPairings } from '$lib/fonts';
import { usePreferences } from './hooks/usePreferences';
import { WeatherProvider } from './hooks/useWeather';
import WeatherScreen from './components/WeatherScreen';
import './App.css';
import './components/components.css';

function AppInner() {
	const { i18n } = useTranslation();
	const { fontIndex } = usePreferences();
	const [fontUrl, setFontUrl] = useState('');
	const [displayFamily, setDisplayFamily] = useState('system-ui, sans-serif');
	const [bodyFamily, setBodyFamily] = useState('system-ui, sans-serif');
	const [fontFeatures, setFontFeatures] = useState('normal');

	useEffect(() => {
		const pairing = fontPairings[fontIndex];
		if (pairing) {
			setFontUrl(pairing.googleFontsUrl ?? '');
			setDisplayFamily(pairing.displayFamily);
			setBodyFamily(pairing.bodyFamily);
			setFontFeatures(pairing.bodyFontFeatures ?? 'normal');
		}
	}, [fontIndex]);

	const isReady = i18n.isInitialized;

	if (!isReady) {
		return (
			<div className="loading-shell">
				<div className="spinner" />
			</div>
		);
	}

	return (
		<>
			{fontUrl && <link href={fontUrl} rel="stylesheet" />}
			<div
				className="app-shell"
				style={{
					'--font-display': displayFamily,
					'--font-body': bodyFamily,
					'--font-features': fontFeatures,
				} as React.CSSProperties}
			>
				<WeatherProvider>
					<WeatherScreen />
				</WeatherProvider>
			</div>
		</>
	);
}

export default AppInner;
