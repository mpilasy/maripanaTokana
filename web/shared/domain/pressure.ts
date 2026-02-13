export class Pressure {
	private constructor(readonly hPa: number) {}

	get inHg(): number {
		return this.hPa * 0.02953;
	}

	displayHPa(): string {
		return `${Math.round(this.hPa)} hPa`;
	}

	displayInHg(): string {
		return `${this.inHg.toFixed(2)} inHg`;
	}

	displayDual(metricPrimary: boolean): [string, string] {
		return metricPrimary
			? [this.displayHPa(), this.displayInHg()]
			: [this.displayInHg(), this.displayHPa()];
	}

	static fromHPa(hPa: number): Pressure {
		return new Pressure(hPa);
	}

	static fromInHg(inHg: number): Pressure {
		return new Pressure(inHg / 0.02953);
	}
}
