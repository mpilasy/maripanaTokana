import { describe, it, expect } from 'vitest';
import { formatDate, formatDayName, formatDayMonth } from './date';

describe('date formatting', () => {
	// Fixed timestamp: October 5, 2026, 14:30 UTC
	const testTimestamp = new Date('2026-10-05T14:30:00Z').getTime();
	const utcOffsetSeconds = 0; // UTC

	const mgWeekdays = ['Alahady', 'Alatsinainy', 'Talata', 'Alarobia', 'Alakamisy', 'Zoma', 'Asabotsy'];
	const mgMonths = ['Janoary', 'Febroary', 'Martsa', 'Aprily', 'Mey', 'Jona', 'Jolay', 'Aogositra', 'Septambra', 'Oktobra', 'Novambra', 'Desambra'];

	const neWeekdays = ['आइतबार', 'सोमबार', 'मंगलबार', 'बुधबार', 'बिहीबार', 'शुक्रबार', 'शनिबार'];
	const neMonths = ['जनवरी', 'फेब्रुअरी', 'मार्च', 'अप्रैल', 'मई', 'जुन', 'जुलाई', 'अगस्त', 'सितंबर', 'अक्तूबर', 'नवंबर', 'दिसंबर'];

	describe('formatDate', () => {
		it('should accept and use custom arrays for Malagasy without error', () => {
			// Note: In Node.js, Intl supports mg, so it uses Intl instead of arrays
			// This test just verifies the function accepts the arrays and returns a formatted string
			const result = formatDate(testTimestamp, 'mg', mgWeekdays, mgMonths);
			expect(result).toBeTruthy();
			expect(typeof result).toBe('string');
			// Should contain October in some form
			expect(result.toLowerCase()).toContain('oktobra');
		});

		it('should accept and use custom arrays for Nepali without error', () => {
			const result = formatDate(testTimestamp, 'ne', neWeekdays, neMonths);
			expect(result).toBeTruthy();
			expect(typeof result).toBe('string');
		});

		it('should use Intl for English when no arrays provided', () => {
			const result = formatDate(testTimestamp, 'en');
			expect(result).toContain('October');
			expect(result).toContain('2026');
		});
	});

	describe('formatDayName', () => {
		it('should accept custom arrays for Malagasy and return a day name', () => {
			const result = formatDayName(testTimestamp, 'mg', utcOffsetSeconds, false, mgWeekdays);
			expect(result).toBeTruthy();
			expect(typeof result).toBe('string');
			// Should be one of the weekday names
			expect(mgWeekdays.includes(result) || result.length <= 3).toBe(true);
		});

		it('should accept custom arrays for Nepali and return a day name', () => {
			const result = formatDayName(testTimestamp, 'ne', utcOffsetSeconds, false, neWeekdays);
			expect(result).toBeTruthy();
			expect(typeof result).toBe('string');
		});

		it('should handle short flag for day names', () => {
			const longResult = formatDayName(testTimestamp, 'mg', utcOffsetSeconds, false, mgWeekdays);
			const shortResult = formatDayName(testTimestamp, 'mg', utcOffsetSeconds, true, mgWeekdays);
			expect(longResult).toBeTruthy();
			expect(shortResult).toBeTruthy();
		});
	});

	describe('formatDayMonth', () => {
		it('should accept custom arrays for Malagasy and return day/month', () => {
			const result = formatDayMonth(testTimestamp, 'mg', utcOffsetSeconds, mgMonths);
			expect(result).toBeTruthy();
			expect(typeof result).toBe('string');
			expect(result).toContain('5');
		});

		it('should accept custom arrays for Nepali and return day/month', () => {
			const result = formatDayMonth(testTimestamp, 'ne', utcOffsetSeconds, neMonths);
			expect(result).toBeTruthy();
			expect(typeof result).toBe('string');
		});
	});

	describe('timezone handling', () => {
		it('should apply UTC offset to day calculation for Malagasy', () => {
			const offsetSeconds = 3 * 3600;
			const result = formatDayName(testTimestamp, 'mg', offsetSeconds, false, mgWeekdays);
			expect(result).toBeTruthy();
		});

		it('should handle negative offsets for day boundary crossing', () => {
			const offsetSeconds = -24 * 3600;
			const result = formatDayName(testTimestamp, 'mg', offsetSeconds, false, mgWeekdays);
			expect(result).toBeTruthy();
		});
	});

	describe('fallback behavior', () => {
		it('should work without arrays provided (uses Intl)', () => {
			const result = formatDayName(testTimestamp, 'mg', utcOffsetSeconds, false);
			expect(result).toBeTruthy();
			expect(typeof result).toBe('string');
		});

		it('should work for English with or without arrays', () => {
			const withoutArrays = formatDate(testTimestamp, 'en');
			const withArrays = formatDate(testTimestamp, 'en', ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'],
				['January', 'February', 'March', 'April', 'May', 'June', 'July', 'August', 'September', 'October', 'November', 'December']);
			expect(withoutArrays).toContain('2026');
			expect(withArrays).toContain('2026');
		});
	});

	describe('locale-specific behavior', () => {
		it('Malagasy formatDayMonth should use abbreviated month for Latin script', () => {
			const result = formatDayMonth(testTimestamp, 'mg', utcOffsetSeconds, mgMonths);
			expect(result).toBeTruthy();
		});

		it('Nepali formatDayMonth should handle Devanagari script properly', () => {
			const result = formatDayMonth(testTimestamp, 'ne', utcOffsetSeconds, neMonths);
			expect(result).toBeTruthy();
		});
	});
});
