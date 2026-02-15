export class WindSpeed {
	private constructor(readonly metersPerSecond: number) {}

	get mph(): number {
		return this.metersPerSecond * 2.23694;
	}

	displayMetric(): string {
		return `${this.metersPerSecond.toFixed(1)} m/s`;
	}

	displayImperial(): string {
		return `${this.mph.toFixed(1)} mph`;
	}

	displayDual(metricPrimary: boolean): [string, string] {
		return metricPrimary
			? [this.displayMetric(), this.displayImperial()]
			: [this.displayImperial(), this.displayMetric()];
	}

	static fromMetersPerSecond(ms: number): WindSpeed {
		return new WindSpeed(ms);
	}

	static fromMph(mph: number): WindSpeed {
		return new WindSpeed(mph / 2.23694);
	}
}
