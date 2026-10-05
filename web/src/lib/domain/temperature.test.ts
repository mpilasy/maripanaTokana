import { describe, expect, it } from 'vitest';
import { Temperature } from './temperature';

describe('Temperature', () => {
	it('renders a placeholder for NaN and infinite values', () => {
		for (const v of [NaN, Infinity, -Infinity]) {
			const t = Temperature.fromCelsius(v);
			expect(t.displayCelsius()).toBe('--°C');
			expect(t.displayCelsius(1)).toBe('--°C');
			expect(t.displayFahrenheit()).toBe('--°F');
			expect(t.displayFahrenheit(1)).toBe('--°F');
		}
	});

	it('formats finite values unchanged', () => {
		expect(Temperature.fromCelsius(20).displayCelsius()).toBe('20°C');
		expect(Temperature.fromCelsius(20).displayFahrenheit()).toBe('68°F');
	});
});
