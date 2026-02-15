export class Precipitation {
	private constructor(readonly mm: number) {}

	get inches(): number {
		return this.mm * 0.03937;
	}

	displayMetric(): string {
		return `${this.mm.toFixed(1)} mm`;
	}

	displayImperial(): string {
		return `${this.inches.toFixed(2)} in`;
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
