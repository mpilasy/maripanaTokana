<!-- SPDX-FileCopyrightText: Orinasa Njarasoa -->
<!-- SPDX-License-Identifier: GPL-3.0-or-later -->
<script lang="ts">
	import { _ } from 'svelte-i18n';
	import { wmoDescriptionKey } from '$lib/api/wmoWeatherCode';

	/** A weather condition as a Meteocons icon (MIT, Bas Milius), with day and night variants. */
	let { code, night = false, size = 32 }: { code: number; night?: boolean; size?: number } = $props();

	function iconName(c: number, n: boolean): string {
		const dn = n ? 'night' : 'day';
		switch (c) {
			case 0: return `clear-${dn}`;
			case 1: case 2: return `partly-cloudy-${dn}`;
			case 3: return 'overcast';
			case 45: case 48: return `fog-${dn}`;
			case 51: case 53: case 55: return 'drizzle';
			case 56: case 57: case 66: case 67: return 'sleet';
			case 61: case 63: case 65: return 'rain';
			case 71: case 73: case 75: case 77: return 'snow';
			case 80: case 81: case 82: return `partly-cloudy-${dn}-rain`;
			case 85: case 86: return `partly-cloudy-${dn}-snow`;
			case 95: case 96: case 99: return `thunderstorms-${dn}-rain`;
			default: return 'not-available';
		}
	}
</script>

<!-- The artwork leaves wide margins in its 64-unit box; a negative margin pulls neighbours closer -->
<img class="weather-icon" src="/icons/weather/{iconName(code, night)}.svg" width={size} height={size} style:margin="{-size * 0.12}px"
	alt={$_(wmoDescriptionKey(code))} draggable="false" />

<style>
	.weather-icon {
		display: block;
	}
</style>
