<script lang="ts">
	import { _ } from 'svelte-i18n';
	import { longPress } from '$lib/actions/longPress';

	interface Props {
		/** City, region, country; absent when the name is only coordinates. */
		place?: string;
		/** Decimal coordinates, the form that is shared and copied. */
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

	const mapLink = $derived(`https://www.google.com/maps/search/?api=1&query=${lat.toFixed(6)},${lon.toFixed(6)}`);

	// A tap shares the value with a map link; without a share sheet (desktop), the map opens
	async function share(value: string) {
		if (navigator.share) {
			try {
				await navigator.share({ text: `${value}\n${mapLink}` });
			} catch {
				// Dismissed
			}
		} else {
			window.open(mapLink, '_blank');
		}
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

	const boxes: { shown: string; value: string; dir: 'auto' | 'ltr' }[] = $derived([
		...(place ? [{ shown: place, value: place, dir: 'auto' as const }] : []),
		{ shown: coordinatesShown, value: coordinates, dir: 'ltr' as const },
		{ shown: `Plus Code  ${plusCode}`, value: plusCode, dir: 'ltr' as const },
	]);
</script>

<span class="details">
	<button type="button" class="details-btn" aria-label={$_('cd_location_details')} aria-expanded={open}
		onclick={(e) => { e.stopPropagation(); open = !open; }}>
		<svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor" aria-hidden="true">
			<path d="M20 2H4c-1.1 0-2 .9-2 2v16c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zM8 20H4v-4h4v4zm0-6H4v-4h4v4zm0-6H4V4h4v4zm6 12h-4v-4h4v4zm0-6h-4v-4h4v4zm0-6h-4V4h4v4zm6 12h-4v-4h4v4zm0-6h-4v-4h4v4zm0-6h-4V4h4v4z"/>
		</svg>
	</button>
	{#if open}
		<!-- svelte-ignore a11y_click_events_have_key_events -->
		<!-- svelte-ignore a11y_no_static_element_interactions -->
		<span class="panel" onclick={(e) => e.stopPropagation()}>
			{#each boxes as box (box.value)}
				<button type="button" class="box" dir={copied === box.value ? 'auto' : box.dir}
					onclick={() => share(box.value)} use:longPress={() => copy(box.value)}>
					{copied === box.value ? $_('copied') : box.shown}
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

	.panel {
		position: absolute;
		top: calc(100% + 6px);
		inset-inline-start: 0;
		z-index: 5;
		display: flex;
		flex-direction: column;
		gap: 6px;
		width: max-content;
		max-width: min(320px, calc(100vw - 32px));
		padding: 8px;
		border-radius: 12px;
		background: #1A1565;
		border: 1px solid var(--hairline);
		box-shadow: 0 4px 16px rgba(0, 0, 0, 0.4);
	}

	.box {
		padding: 8px 10px;
		border: none;
		border-radius: 8px;
		background: rgba(255, 255, 255, 0.12);
		color: white;
		font-family: var(--font-body);
		font-size: 14px;
		text-align: start;
		cursor: pointer;
	}

	.box:hover {
		background: rgba(255, 255, 255, 0.2);
	}

	/* Long-press targets: no text selection or callout on touch */
	.box, .details-btn {
		-webkit-user-select: none;
		user-select: none;
		-webkit-touch-callout: none;
	}
</style>
