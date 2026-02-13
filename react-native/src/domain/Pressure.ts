/**
 * Value object encapsulating atmospheric pressure. Stores the canonical value in hPa;
 * inHg is always derived so both units are available once a value is set.
 */
export class Pressure {
  readonly hPa: number;

  private constructor(hPa: number) {
    this.hPa = hPa;
  }

  get inHg(): number {
    return this.hPa * 0.02953;
  }

  displayHPa(): string {
    return `${Math.round(this.hPa)} hPa`;
  }

  displayInHg(): string {
    return `${this.inHg.toFixed(2)} inHg`;
  }

  /** Returns [primary, secondary] based on preferred unit system. */
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
