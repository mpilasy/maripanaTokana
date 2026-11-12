<!-- SPDX-FileCopyrightText: Orinasa Njarasoa -->
<!-- SPDX-License-Identifier: GPL-3.0-or-later -->
<!-- Twin: LicensesScreen in app/src/main/java/orinasa/njarasoa/maripanatokana/ui/mobile/settings/LicensesScreen.kt -->
<script lang="ts">
	import { _ } from 'svelte-i18n';
	import { creditSections, type Credit } from '#lib/credits.js';
	import SettingsPage from './SettingsPage.svelte';
	import { weatherSource, weatherModel } from '#lib/stores/preferences.js';
	import { weatherModelFor } from '#lib/domain/weatherModel.js';

	/**
	 * The credits for the data, images, fonts and libraries, opened from About. A row with a bundled licence text
	 * opens it on a page of its own; the other rows open their licence page in a new tab.
	 */
	interface Props { onBack: () => void; }
	let { onBack }: Props = $props();

	// A chosen Open-Meteo model shows after the source name, for example "Open-Meteo · ECMWF IFS"
	let chosenModel = $derived($weatherSource === 'OPEN_METEO' ? weatherModelFor($weatherModel) : null);

	// The licence text on screen: its page title, and the text when it has loaded (null while it loads)
	let openText = $state<{ title: string; text: string | null; failed: boolean } | null>(null);

	function nameOf(item: Credit): string {
		const base = item.nameKey ? $_(item.nameKey) : (item.name ?? '');
		return item.name === 'Open-Meteo' && chosenModel ? `${base} · ${chosenModel.name}` : base;
	}

	async function showText(title: string, path: string) {
		const shown = { title, text: null as string | null, failed: false };
		openText = shown;
		try {
			const res = await fetch(path);
			if (!res.ok) throw new Error(String(res.status));
			const text = await res.text();
			if (openText?.title === title) openText = { ...shown, text };
		} catch {
			if (openText?.title === title) openText = { ...shown, failed: true };
		}
	}
</script>

<SettingsPage title={$_('licenses_title')} {onBack}>
	{#each creditSections as section (section.titleKey)}
		<div class="section">
			<div class="section-title">{$_(section.titleKey)}</div>
			<div class="group">
				{#each section.items as item}
					{@const name = nameOf(item)}
					{@const note = item.noteKey ? $_(item.noteKey) : item.note}
					{#snippet rowContent(mark: string)}
						<span class="credit-text">
							<span class="credit-name">{name}</span>
							{#if note}
								<span class="credit-note">{note}</span>
							{/if}
						</span>
						<span class="credit-license">{item.license}{mark}</span>
					{/snippet}
					<!-- › opens the licence text in the app, ↗ opens a web page -->
					{#if item.text}
						<button type="button" class="credit-row" onclick={() => showText(name, item.text!)}>{@render rowContent(' ›')}</button>
					{:else if item.url}
						<a class="credit-row" href={item.url} target="_blank" rel="noopener">{@render rowContent(' ↗')}</a>
					{:else}
						<div class="credit-row">{@render rowContent('')}</div>
					{/if}
				{/each}
			</div>
		</div>
	{/each}
</SettingsPage>

{#if openText}
	<SettingsPage title={openText.title} onBack={() => (openText = null)}>
		<div class="section group">
			{#if openText.text != null}
				<pre class="license-text" dir="ltr">{openText.text}</pre>
			{:else if openText.failed}
				<p class="license-text">{$_('settings_network_error')}</p>
			{:else}
				<p class="license-text">…</p>
			{/if}
		</div>
	</SettingsPage>
{/if}

<style>
	.credit-row {
		display: flex;
		align-items: center;
		gap: 12px;
		width: 100%;
		padding: 10px 0;
		color: white;
		text-decoration: none;
		text-align: start;
		background: none;
		border: none;
		font: inherit;
	}

	button.credit-row {
		cursor: pointer;
	}

	.credit-row + .credit-row {
		border-top: 1px solid var(--hairline);
	}

	a.credit-row:hover .credit-name,
	button.credit-row:hover .credit-name {
		text-decoration: underline;
	}

	.credit-text {
		flex: 1;
		min-width: 0;
		display: flex;
		flex-direction: column;
		gap: 2px;
	}

	.credit-name {
		font-size: 0.9375rem;
		font-weight: 500;
	}

	.credit-note {
		font-size: 0.75rem;
		line-height: 1rem;
		color: rgba(255, 255, 255, 0.55);
	}

	.credit-license {
		width: 110px;
		flex-shrink: 0;
		text-align: end;
		font-size: 0.75rem;
		color: rgba(255, 255, 255, 0.65);
	}

	.license-text {
		margin: 0;
		padding: 12px 0;
		font-family: inherit;
		font-size: 0.75rem;
		line-height: 1.05rem;
		color: rgba(255, 255, 255, 0.8);
		white-space: pre-wrap;
		overflow-wrap: anywhere;
		user-select: text;
	}
</style>
