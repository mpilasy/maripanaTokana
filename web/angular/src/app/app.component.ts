import { Component, inject, computed, effect } from '@angular/core';
import { fontPairings } from '$lib/fonts';
import { PreferencesService } from './services/preferences.service';
import { WeatherScreenComponent } from './components/weather-screen.component';

@Component({
	selector: 'app-root',
	standalone: true,
	imports: [WeatherScreenComponent],
	template: `
		@if (fontUrl()) {
			<link [href]="fontUrl()" rel="stylesheet" />
		}
		<div class="app-shell"
			[style.--font-display]="displayFamily()"
			[style.--font-body]="bodyFamily()"
			[style.--font-features]="fontFeatures()">
			<app-weather-screen />
		</div>
	`,
	styles: `
		.app-shell {
			width: 100%;
			height: 100dvh;
			background: linear-gradient(to bottom, #0E0B3D, #1A1565);
			position: relative;
			overflow: hidden;
			font-family: var(--font-body);
		}
	`,
})
export class AppComponent {
	private prefs = inject(PreferencesService);

	fontUrl = computed(() => fontPairings[this.prefs.fontIndex()].googleFontsUrl ?? '');
	displayFamily = computed(() => fontPairings[this.prefs.fontIndex()].displayFamily);
	bodyFamily = computed(() => fontPairings[this.prefs.fontIndex()].bodyFamily);
	fontFeatures = computed(() => fontPairings[this.prefs.fontIndex()].bodyFontFeatures ?? 'normal');
}
