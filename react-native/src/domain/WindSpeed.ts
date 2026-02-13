/**
 * Value object encapsulating wind speed. Stores the canonical value in m/s;
 * mph is always derived so both units are available once a value is set.
 */
export class WindSpeed {
  readonly metersPerSecond: number;

  private constructor(ms: number) {
    this.metersPerSecond = ms;
  }

  get mph(): number {
    return this.metersPerSecond * 2.23694;
  }

  displayMetric(): string {
    return `${this.metersPerSecond.toFixed(1)} m/s`;
  }

  displayImperial(): string {
    return `${this.mph.toFixed(1)} mph`;
  }

  /** Returns [primary, secondary] based on preferred unit system. */
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
