import { Component, input, output } from '@angular/core';

@Component({
	selector: 'app-dual-unit-text',
	standalone: true,
	template: `
		<div class="dual-unit" [class.clickable]="!!onClick" [style.text-align]="align()" (click)="onClick.emit()">
			<div class="primary" [style.font-size]="primarySize()">{{ primary() }}</div>
			<div class="secondary" [style.font-size]="'calc(' + primarySize() + ' * 0.75)'">{{ secondary() }}</div>
		</div>
	`,
	styles: `
		.dual-unit { display: flex; flex-direction: column; }
		.dual-unit.clickable { cursor: pointer; }
		.primary { font-family: var(--font-display); font-weight: 700; color: white; font-feature-settings: var(--font-features); }
		.secondary { font-family: var(--font-display); color: rgba(255,255,255,0.55); font-feature-settings: var(--font-features); }
	`,
})
export class DualUnitTextComponent {
	primary = input.required<string>();
	secondary = input.required<string>();
	primarySize = input<string>('16px');
	align = input<'start' | 'end' | 'center'>('start');
	onClick = output<void>();
}
