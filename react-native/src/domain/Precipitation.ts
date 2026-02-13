/**
 * Value object encapsulating precipitation (rain/snow). Stores the canonical value in mm;
 * inches is always derived so both units are available once a value is set.
 */
export class Precipitation {
  readonly mm: number;

  private constructor(mm: number) {
    this.mm = mm;
  }

  get inches(): number {
    return this.mm * 0.03937;
  }

  displayMetric(): string {
    return `${this.mm.toFixed(1)} mm`;
  }

  displayImperial(): string {
    return `${this.inches.toFixed(2)} in`;
  }

  /** Returns [primary, secondary] based on preferred unit system. */
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
