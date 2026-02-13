import { useTranslation } from 'react-i18next';

interface FooterProps {
	fontName: string;
	currentFlag: string;
	onCycleFont: () => void;
	onCycleLanguage: () => void;
}

export default function Footer({ fontName, currentFlag, onCycleFont, onCycleLanguage }: FooterProps) {
	const { t } = useTranslation();

	return (
		<footer className="footer" dir="ltr">
			<div className="footer-font" onClick={onCycleFont}>
				<span className="font-icon">Aa</span>
				<span className="font-name">{fontName.replace(' + ', '\n')}</span>
			</div>

			<div className="footer-credits">
				<span className="credit-text">
					{t('credits_weather_data')}{' '}
					<a href="https://open-meteo.com" target="_blank" rel="noopener">Open-Meteo</a>
				</span>
				<span className="version">v1.0.0</span>
			</div>

			<div className="footer-lang" onClick={onCycleLanguage}>
				<span className="flag">{currentFlag}</span>
			</div>
		</footer>
	);
}
