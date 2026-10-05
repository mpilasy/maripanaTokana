export class Temperature {
	private constructor(readonly celsius: number) {}

	get fahrenheit(): number {
		return this.celsius * 9 / 5 + 32;
	}

	displayCelsius(decimals = 0): string {
		if (!Number.isFinite(this.celsius)) return '--°C';
		return decimals > 0
			? `${this.celsius.toFixed(decimals)}°C`
			: `${Math.round(this.celsius)}°C`;
	}

	displayFahrenheit(decimals = 0): string {
		if (!Number.isFinite(this.fahrenheit)) return '--°F';
		return decimals > 0
			? `${this.fahrenheit.toFixed(decimals)}°F`
			: `${Math.round(this.fahrenheit)}°F`;
	}

	displayDual(metricPrimary: boolean, decimals = 0): [string, string] {
		return metricPrimary
			? [this.displayCelsius(decimals), this.displayFahrenheit(decimals)]
			: [this.displayFahrenheit(decimals), this.displayCelsius(decimals)];
	}

	/** Celsius with cDecimals, Fahrenheit always integer. */
	displayDualMixed(metricPrimary: boolean, cDecimals = 1): [string, string] {
		return metricPrimary
			? [this.displayCelsius(cDecimals), this.displayFahrenheit(0)]
			: [this.displayFahrenheit(0), this.displayCelsius(cDecimals)];
	}

	static fromCelsius(c: number): Temperature {
		return new Temperature(c);
	}

	static fromFahrenheit(f: number): Temperature {
		return new Temperature((f - 32) * 5 / 9);
	}
}
