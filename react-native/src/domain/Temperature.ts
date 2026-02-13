/**
 * Value object encapsulating a temperature. Stores the canonical value in Celsius;
 * Fahrenheit is always derived so both units are available once a value is set.
 */
export class Temperature {
  readonly celsius: number;

  private constructor(celsius: number) {
    this.celsius = celsius;
  }

  get fahrenheit(): number {
    return this.celsius * 9.0 / 5.0 + 32.0;
  }

  displayCelsius(decimals: number = 0): string {
    return decimals > 0
      ? `${this.celsius.toFixed(decimals)}\u00B0C`
      : `${Math.round(this.celsius)}\u00B0C`;
  }

  displayFahrenheit(decimals: number = 0): string {
    return decimals > 0
      ? `${this.fahrenheit.toFixed(decimals)}\u00B0F`
      : `${Math.round(this.fahrenheit)}\u00B0F`;
  }

  /** Returns [primary, secondary] based on preferred unit system. */
  displayDual(metricPrimary: boolean, decimals: number = 0): [string, string] {
    return metricPrimary
      ? [this.displayCelsius(decimals), this.displayFahrenheit(decimals)]
      : [this.displayFahrenheit(decimals), this.displayCelsius(decimals)];
  }

  /** Celsius with cDecimals decimals, Fahrenheit always integer. */
  displayDualMixed(metricPrimary: boolean, cDecimals: number = 1): [string, string] {
    return metricPrimary
      ? [this.displayCelsius(cDecimals), this.displayFahrenheit(0)]
      : [this.displayFahrenheit(0), this.displayCelsius(cDecimals)];
  }

  static fromCelsius(c: number): Temperature {
    return new Temperature(c);
  }

  static fromFahrenheit(f: number): Temperature {
    return new Temperature((f - 32.0) * 5.0 / 9.0);
  }
}
