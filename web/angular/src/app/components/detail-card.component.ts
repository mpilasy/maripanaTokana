import { Component, input, output } from '@angular/core';
import { DualUnitTextComponent } from './dual-unit-text.component';

@Component({
	selector: 'app-detail-card',
	standalone: true,
	imports: [DualUnitTextComponent],
	template: `
		<div class="detail-card">
			<span class="title">{{ title() }}</span>
			@if (secondaryValue()) {
				<app-dual-unit-text [primary]="value()" [secondary]="secondaryValue()!" primarySize="20px" (onClick)="onToggleUnits.emit()" />
			} @else {
				<span class="value">{{ value() }}</span>
			}
			@if (subtitle()) {
				<span class="subtitle">{{ subtitle() }}</span>
			}
		</div>
	`,
	host: { style: 'display: flex' },
	styles: `
		.detail-card { background: rgba(42,31,165,0.6); border-radius: 16px; padding: 16px; display: flex; flex-direction: column; gap: 8px; flex: 1; }
		.title { font-size: 14px; color: rgba(255,255,255,0.7); }
		.value { font-family: var(--font-display); font-size: 20px; font-weight: 700; color: white; font-feature-settings: var(--font-features); }
		.subtitle { font-size: 12px; color: rgba(255,255,255,0.6); }
	`,
})
export class DetailCardComponent {
	title = input.required<string>();
	value = input.required<string>();
	secondaryValue = input<string>();
	subtitle = input<string>();
	onToggleUnits = output<void>();
}
