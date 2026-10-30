<!-- SPDX-FileCopyrightText: Orinasa Njarasoa -->
<!-- SPDX-License-Identifier: GPL-3.0-or-later -->
<script lang="ts" module>
	/** The dew point line, next to the #64B5F6 humidity line. */
	export const DEW_COLOR = '#FFB74D';
</script>

<script lang="ts">
	/** A forecast card's humidity, with the dew point in both units under it. */
	import { _ } from 'svelte-i18n';
	import type { Temperature } from '#lib/domain/temperature.js';
	import DualUnitText from './DualUnitText.svelte';

	interface Props {
		humidity?: number | null;
		dewPoint?: Temperature | null;
		metricPrimary: boolean;
		loc: (s: string) => string;
		onToggleUnits: () => void;
	}

	let { humidity = null, dewPoint = null, metricPrimary, loc, onToggleUnits }: Props = $props();
</script>

<span class="humidity" class:no-data={humidity == null}>{humidity != null ? loc(`${humidity}%`) : '--'}</span>
{#if dewPoint && Number.isFinite(dewPoint.celsius)}
	{@const [dewP, dewS] = dewPoint.displayDual(metricPrimary)}
	<span class="dew-label">{$_('detail_dewpoint')}</span>
	<DualUnitText primary={loc(dewP)} secondary={loc(dewS)} primarySize="0.75rem" align="center" onClick={onToggleUnits} />
{/if}

<style>
	.humidity {
		font-family: var(--font-display);
		font-size: 0.9375rem;
		font-weight: 700;
		color: white;
		font-feature-settings: var(--font-features);
	}

	.humidity.no-data {
		color: rgba(255, 255, 255, 0.5);
	}

	.dew-label {
		font-size: 0.5625rem;
		line-height: 1.1;
		color: rgba(255, 255, 255, 0.5);
		text-align: center;
	}
</style>
