<!-- SPDX-FileCopyrightText: Orinasa Njarasoa -->
<!-- SPDX-License-Identifier: GPL-3.0-or-later -->
<script lang="ts">
	import { _ } from 'svelte-i18n';
	import { longPress } from '#lib/actions/longPress.js';

	interface Props {
		/** City, region, country; absent when the name is only coordinates. */
		place?: string;
		/** Decimal coordinates, the form that is copied. */
		coordinates: string;
		/** Coordinates as shown, in degrees, minutes and seconds. */
		coordinatesShown: string;
		plusCode: string;
		lat: number;
		lon: number;
	}

	let { place, coordinates, coordinatesShown, plusCode, lat, lon }: Props = $props();

	let open = $state(false);
	let copied = $state<string | null>(null);

	$effect(() => {
		if (!open) return;
		const close = () => { open = false; };
		window.addEventListener('click', close);
		return () => window.removeEventListener('click', close);
	});

	// A tap opens the point on a map, as the coordinates did before
	function openInMap() {
		window.open(`https://www.openstreetmap.org/?mlat=${lat}&mlon=${lon}#map=15/${lat}/${lon}`, '_blank');
	}

	async function copy(value: string) {
		try {
			await navigator.clipboard.writeText(value);
		} catch {
			return;
		}
		copied = value;
		setTimeout(() => { if (copied === value) copied = null; }, 1500);
	}

	const PIN = 'M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5c-1.38 0-2.5-1.12-2.5-2.5s1.12-2.5 2.5-2.5 2.5 1.12 2.5 2.5-1.12 2.5-2.5 2.5z';
	const TARGET = 'M12 8c-2.21 0-4 1.79-4 4s1.79 4 4 4 4-1.79 4-4-1.79-4-4-4zm8.94 3A8.994 8.994 0 0 0 13 3.06V1h-2v2.06A8.994 8.994 0 0 0 3.06 11H1v2h2.06A8.994 8.994 0 0 0 11 20.94V23h2v-2.06A8.994 8.994 0 0 0 20.94 13H23v-2h-2.06zM12 19c-3.87 0-7-3.13-7-7s3.13-7 7-7 7 3.13 7 7-3.13 7-7 7z';
	const GRID = 'M20 2H4c-1.1 0-2 .9-2 2v16c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zM8 20H4v-4h4v4zm0-6H4v-4h4v4zm0-6H4V4h4v4zm6 12h-4v-4h4v4zm0-6h-4v-4h4v4zm0-6h-4V4h4v4zm6 12h-4v-4h4v4zm0-6h-4v-4h4v4zm0-6h-4V4h4v4z';

	const rows: { icon: string; shown: string; value: string; dir: 'auto' | 'ltr' }[] = $derived([
		...(place ? [{ icon: PIN, shown: place, value: place, dir: 'auto' as const }] : []),
		{ icon: TARGET, shown: coordinatesShown, value: coordinates, dir: 'ltr' as const },
		{ icon: GRID, shown: `Plus Code  ${plusCode}`, value: plusCode, dir: 'ltr' as const },
	]);
</script>

<span class="details">
	<button type="button" class="details-btn" aria-label={$_('cd_location_details')} aria-expanded={open}
		onclick={(e) => { e.stopPropagation(); open = !open; }}>
		<svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor" aria-hidden="true">
			<path d={GRID}/>
		</svg>
	</button>
	{#if open}
		<!-- svelte-ignore a11y_click_events_have_key_events -->
		<!-- svelte-ignore a11y_no_static_element_interactions -->
		<span class="panel" onclick={(e) => e.stopPropagation()}>
			{#each rows as row (row.value)}
				<button type="button" class="row" onclick={() => { open = false; openInMap(); }} use:longPress={() => copy(row.value)}>
					<svg viewBox="0 0 24 24" width="14" height="14" fill="currentColor" aria-hidden="true"><path d={row.icon}/></svg>
					<span dir={copied === row.value ? 'auto' : row.dir}>{copied === row.value ? $_('copied') : row.shown}</span>
				</button>
			{/each}
		</span>
	{/if}
</span>

<style>
	.details {
		position: relative;
		display: flex;
		flex-shrink: 0;
	}

	.details-btn {
		display: flex;
		padding: 2px 4px 2px 0;
		border: none;
		background: transparent;
		color: rgba(255, 255, 255, 0.5);
		cursor: pointer;
	}

	/* A light card, so it stands out from the dark screen */
	.panel {
		position: absolute;
		top: calc(100% + 6px);
		inset-inline-start: 0;
		z-index: 5;
		display: flex;
		flex-direction: column;
		width: max-content;
		max-width: min(320px, calc(100vw - 32px));
		padding: 4px 0;
		border-radius: 12px;
		background: #F3F1F7;
		box-shadow: 0 6px 20px rgba(0, 0, 0, 0.45);
		overflow: hidden;
	}

	.row {
		display: flex;
		align-items: center;
		gap: 8px;
		min-height: 36px;
		padding: 6px 12px;
		border: none;
		background: transparent;
		color: #1C1B1F;
		font-family: var(--font-body);
		font-size: 13px;
		text-align: start;
		cursor: pointer;
	}

	.row + .row {
		border-top: 1px solid rgba(28, 27, 31, 0.1);
	}

	.row svg {
		flex-shrink: 0;
		color: rgba(28, 27, 31, 0.55);
	}

	.row:hover {
		background: rgba(28, 27, 31, 0.06);
	}

	/* Long-press targets: no text selection or callout on touch */
	.row, .details-btn {
		-webkit-user-select: none;
		user-select: none;
		-webkit-touch-callout: none;
	}
</style>
