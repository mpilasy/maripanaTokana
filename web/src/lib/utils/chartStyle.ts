// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

import { CHILL_COLOR, FREEZING_LINE } from './feltColors.js';

const PRIMARY = '#9B8FFF';
const SKY_BLUE = '#64B5F6';

/**
 * The colours, line widths (px) and dash patterns (SVG stroke-dasharray, px) of the chart view and of the charts
 * on the main page. A chart with a colour of its own (the heat index, the UV and air quality tiers) keeps it.
 * Android has the same values with the same names in ChartStyle.kt.
 */
export const ChartStyle = {
	temperature: 'white',
	dewPoint: CHILL_COLOR,
	precipitation: SKY_BLUE,
	chance: 'rgba(255, 255, 255, 0.6)',
	wind: PRIMARY,
	gust: 'rgba(155, 143, 255, 0.6)',
	pressure: PRIMARY,
	humidity: SKY_BLUE,
	freezing: FREEZING_LINE,
	// The normal high and low: the colours of the high and low lines at 50 % opacity
	normalHigh: 'rgba(255, 112, 67, 0.5)',
	normalLow: 'rgba(100, 181, 246, 0.5)',
	night: 'rgba(0, 0, 0, 0.18)',
	midnight: 'rgba(255, 255, 255, 0.25)',
	selection: 'rgba(255, 255, 255, 0.7)',
	selectionDot: 'white',
	// The soft column of one hour behind the lines in the chart view
	selectionColumn: 'rgba(255, 255, 255, 0.1)',

	lineWidth: 2,
	thinLineWidth: 1.5,
	chanceWidth: 1.2,
	freezingWidth: 1,
	normalWidth: 1,
	midnightWidth: 1,
	selectionWidth: 1,
	selectionDotRadius: 3.5,
	selectionColumnRadius: 8,

	freezingDash: '4 3',
	gustDash: '5 3',
	// Shorter dashes than the freezing line
	normalDash: '2 3',
} as const;
