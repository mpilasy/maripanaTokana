export class Pressure {
	private constructor(readonly hPa: number) {}

	get inHg(): number {
		return this.hPa * 0.02953;
	}

	displayHPa(): string {
		return Number.isFinite(this.hPa) ? `${Math.round(this.hPa)} hPa` : '-- hPa';
	}

	displayInHg(): string {
		return Number.isFinite(this.inHg) ? `${this.inHg.toFixed(2)} inHg` : '-- inHg';
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
