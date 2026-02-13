import { Component, input, output, signal, viewChild, ElementRef, afterRender } from '@angular/core';

@Component({
	selector: 'app-collapsible-section',
	standalone: true,
	template: `
		<div class="collapsible-section">
			<div class="section-header" (click)="toggle()">
				<span class="section-title">{{ title() }}</span>
				@if (isExpanded()) {
					<button class="share-btn" (click)="handleShare($event)" aria-label="Share">
						<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
							<path d="M4 12v8a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-8"/><polyline points="16 6 12 2 8 6"/><line x1="12" y1="2" x2="12" y2="15"/>
						</svg>
					</button>
				}
				<span class="spacer"></span>
				<span class="chevron" [class.expanded]="isExpanded()">&#9660;</span>
			</div>
			<div class="content-wrapper" [style.height.px]="isExpanded() ? contentHeight() : 0">
				<div #inner>
					<div class="content" #contentEl>
						<ng-content />
					</div>
				</div>
			</div>
		</div>
	`,
	styles: `
		.collapsible-section { margin-bottom: 24px; }
		.section-header { display: flex; align-items: center; cursor: pointer; padding: 8px 0; user-select: none; gap: 8px; }
		.section-title { font-size: 20px; font-weight: 700; color: white; }
		.spacer { flex: 1; }
		.share-btn { background: rgba(255,255,255,0.1); border: none; border-radius: 50%; width: 28px; height: 28px; display: flex; align-items: center; justify-content: center; color: rgba(255,255,255,0.4); cursor: pointer; transition: background 0.2s, color 0.2s; flex-shrink: 0; }
		.share-btn:hover { background: rgba(255,255,255,0.2); color: rgba(255,255,255,0.7); }
		.chevron { color: rgba(255,255,255,0.7); font-size: 12px; transition: transform 0.3s ease; transform: rotate(-90deg); }
		.chevron.expanded { transform: rotate(0deg); }
		.content-wrapper { overflow: hidden; transition: height 0.3s ease; }
		.content { padding-top: 8px; min-width: 0; }
	`,
})
export class CollapsibleSectionComponent {
	title = input.required<string>();
	expanded = input(false);
	onShare = output<HTMLElement>();

	isExpanded = signal(false);
	contentHeight = signal(0);

	private innerEl = viewChild<ElementRef<HTMLElement>>('inner');
	private contentEl = viewChild<ElementRef<HTMLElement>>('contentEl');

	constructor() {
		afterRender(() => {
			if (this.isExpanded() && this.innerEl()) {
				this.contentHeight.set(this.innerEl()!.nativeElement.scrollHeight);
			}
		});
	}

	ngOnInit() {
		this.isExpanded.set(this.expanded());
	}

	toggle() {
		this.isExpanded.update(v => !v);
	}

	handleShare(e: MouseEvent) {
		e.stopPropagation();
		if (this.contentEl()) {
			this.onShare.emit(this.contentEl()!.nativeElement);
		}
	}
}
