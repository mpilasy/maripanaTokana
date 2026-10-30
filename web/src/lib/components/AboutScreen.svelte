<!-- SPDX-FileCopyrightText: Orinasa Njarasoa -->
<!-- SPDX-License-Identifier: GPL-3.0-or-later -->
<script lang="ts">
	import { _ } from 'svelte-i18n';
	import { version } from '../../../package.json';
	import { creditSections } from '#lib/credits.js';
	import SettingsPage from './SettingsPage.svelte';
	import { deviceInfo, type DeviceRow } from '#lib/deviceInfo.js';
	import { onMount } from 'svelte';
	import { SUPPORTED_LOCALES } from '#lib/i18n/index.js';
	import { localeIndex } from '#lib/stores/preferences.js';
	import { isAndroid, isStandalone } from '#lib/installHint.js';
	import AppBadges from './AppBadges.svelte';

	interface Props { onBack: () => void; onOpenHelp: () => void; languageFlag: string; onCycleLanguage: () => void; }
	let { onBack, onOpenHelp, languageFlag, onCycleLanguage }: Props = $props();

	let deviceRows = $state<DeviceRow[]>([]);
	let onAndroid = $state(false);
	let installable = $state(false);
	onMount(() => {
		onAndroid = isAndroid();
		installable = !isStandalone();
		deviceInfo(SUPPORTED_LOCALES[$localeIndex].tag).then((rows) => (deviceRows = rows));
	});
</script>

<SettingsPage title={$_('about_title')} {onBack} {languageFlag} {onCycleLanguage}>
	<div class="section group identity">
		<span class="app-name">maripànaTokana</span>
		<span class="version">v{version}</span>
		<span class="copyright">© 2026 Orinasa Njarasoa</span>
		<a class="app-license" href="https://github.com/mpilasy/maripanaTokana/blob/main/LICENSE" target="_blank" rel="noopener">{$_('about_app_license')} ↗</a>
		<!-- How to use the app -->
		<button type="button" class="source-link help-link" onclick={onOpenHelp}>{$_('help_title')} ›</button>
		<a class="source-link" href="https://github.com/mpilasy/maripanaTokana" target="_blank" rel="noopener">{$_('about_source_code')} ↗</a>
		{#if onAndroid || installable}
			<AppBadges android={onAndroid} {installable} />
		{/if}
	</div>

	{#if deviceRows.length > 0}
		<div class="section">
			<div class="section-title">{$_('about_section_device')}</div>
			<div class="group">
				{#each deviceRows as row (row.labelKey)}
					<div class="device-row">
						<span class="device-label">{$_(row.labelKey)}</span>
						<span class="device-value" dir="ltr">{row.value}</span>
					</div>
				{/each}
			</div>
		</div>
	{/if}

	{#each creditSections as section (section.titleKey)}
		<div class="section">
			<div class="section-title">{$_(section.titleKey)}</div>
			<div class="group">
				{#each section.items as item}
					{@const name = item.nameKey ? $_(item.nameKey) : item.name}
					{@const note = item.noteKey ? $_(item.noteKey) : item.note}
					<a class="credit-row" href={item.url} target="_blank" rel="noopener">
						<span class="credit-text">
							<span class="credit-name">{name}</span>
							{#if note}
								<span class="credit-note">{note}</span>
							{/if}
						</span>
						<span class="credit-license">{item.license} ↗</span>
					</a>
				{/each}
			</div>
		</div>
	{/each}
</SettingsPage>

<style>
	.identity {
		display: flex;
		flex-direction: column;
		align-items: center;
		text-align: center;
		gap: 2px;
		padding-top: 16px !important;
		padding-bottom: 16px !important;
	}

	.app-name {
		font-family: var(--font-display);
		font-size: 1.5rem;
		font-weight: 700;
	}

	.version {
		font-size: 0.8125rem;
		color: rgba(255, 255, 255, 0.55);
	}

	.copyright {
		margin-top: 8px;
		font-size: 0.875rem;
		color: rgba(255, 255, 255, 0.85);
	}

	.app-license {
		font-size: 0.8125rem;
		color: rgba(255, 255, 255, 0.6);
		text-align: center;
		text-decoration: none;
	}

	.source-link {
		margin-top: 8px;
		padding: 6px 12px;
		font-size: 0.875rem;
		font-weight: 600;
		color: white;
		text-decoration: none;
		border-radius: 999px;
	}

	.help-link {
		background: none;
		border: none;
		font-family: inherit;
		cursor: pointer;
	}

	.source-link:hover {
		background: var(--surface-tile);
	}

	.device-row {
		display: flex;
		align-items: baseline;
		gap: 12px;
		padding: 8px 0;
	}

	.device-row + .device-row {
		border-top: 1px solid var(--hairline);
	}

	.device-label {
		flex: none;
		width: 40%;
		font-size: 0.8125rem;
		color: rgba(255, 255, 255, 0.6);
	}

	.device-value {
		flex: 1;
		min-width: 0;
		font-size: 0.8125rem;
		color: white;
		text-align: end;
		overflow-wrap: anywhere;
		user-select: text;
	}

	.credit-row {
		display: flex;
		align-items: center;
		gap: 12px;
		padding: 10px 0;
		color: white;
		text-decoration: none;
	}

	.credit-row + .credit-row {
		border-top: 1px solid var(--hairline);
	}

	.credit-row:hover .credit-name {
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
</style>
