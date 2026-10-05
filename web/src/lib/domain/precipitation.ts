export class Precipitation {
	private constructor(readonly mm: number) {}

	get inches(): number {
		return this.mm * 0.03937;
	}

	displayMetric(): string {
		return Number.isFinite(this.mm) ? `${this.mm.toFixed(1)} mm` : '-- mm';
	}

	displayImperial(): string {
		return Number.isFinite(this.inches) ? `${this.inches.toFixed(2)} in` : '-- in';
	}

	displayDual(metricPrimary: boolean): [string, string] {
		return metricPrimary
			? [this.displayMetric(), this.displayImperial()]
			: [this.displayImperial(), this.displayMetric()];
	}

	static fromMm(mm: number): Precipitation {
		return new Precipitation(mm);
	}

	static fromInches(inches: number): Precipitation {
		return new Precipitation(inches / 0.03937);
	}
}
