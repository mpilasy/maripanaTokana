import { Component, input, output, inject } from '@angular/core';
import { I18nService } from '../services/i18n.service';

@Component({
	selector: 'app-footer',
	standalone: true,
	template: `
		<footer class="footer" dir="ltr">
			<div class="footer-font" (click)="onCycleFont.emit()">
				<span class="font-icon">Aa</span>
				<span class="font-name">{{ fontName().replace(' + ', '\\n') }}</span>
			</div>
			<div class="footer-credits">
				<span class="credit-text">
					{{ i18n.t('credits_weather_data') }}
					<a href="https://open-meteo.com" target="_blank" rel="noopener">Open-Meteo</a>
				</span>
				<span class="version">v1.0.0</span>
			</div>
			<div class="footer-lang" (click)="onCycleLanguage.emit()">
				<span class="flag">{{ currentFlag() }}</span>
			</div>
		</footer>
	`,
	styles: `
		.footer { display: flex; align-items: center; padding: 4px 0; padding-bottom: max(4px, env(safe-area-inset-bottom)); flex-shrink: 0; }
		.footer-font { display: flex; align-items: center; gap: 4px; cursor: pointer; }
		.font-icon { font-size: 14px; color: rgba(255,255,255,0.4); }
		.font-name { font-size: 9px; line-height: 11px; color: rgba(255,255,255,0.4); white-space: pre-line; }
		.footer-credits { flex: 1; text-align: center; display: flex; flex-direction: column; align-items: center; }
		.credit-text { font-size: 9px; line-height: 11px; color: rgba(255,255,255,0.3); }
		.credit-text a { color: rgba(255,255,255,0.5); text-decoration: underline; }
		.version { font-size: 9px; line-height: 11px; color: rgba(255,255,255,0.25); }
		.footer-lang { cursor: pointer; }
		.flag { font-size: 16px; }
	`,
})
export class FooterComponent {
	i18n = inject(I18nService);
	fontName = input.required<string>();
	currentFlag = input.required<string>();
	onCycleFont = output<void>();
	onCycleLanguage = output<void>();
}
