<!-- SPDX-FileCopyrightText: Orinasa Njarasoa -->
<!-- SPDX-License-Identifier: GPL-3.0-or-later -->
<script lang="ts">
	import { _ } from 'svelte-i18n';
	import { version } from '../../../package.json';
	import SettingsPage from './SettingsPage.svelte';
	import { deviceInfo, type DeviceRow } from '#lib/deviceInfo.js';
	import { onMount } from 'svelte';
	import { SUPPORTED_LOCALES } from '#lib/i18n/index.js';
	import { localeIndex } from '#lib/stores/preferences.js';
	import { isAndroid, isStandalone } from '#lib/installHint.js';
	import AppBadges from './AppBadges.svelte';
	import LicensesScreen from './LicensesScreen.svelte';

	interface Props { onBack: () => void; onOpenHelp: () => void; languageFlag: string; onCycleLanguage: () => void; }
	let { onBack, onOpenHelp, languageFlag, onCycleLanguage }: Props = $props();

	let showLicenses = $state(false);

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
		<!-- The hash and the UTC commit time, for bug reports. Twin: the Android About -->
		<span class="build">{[__GIT_HASH__ && `#${__GIT_HASH__}`, __COMMIT_TIME__ && `${__COMMIT_TIME__} UTC`].filter(Boolean).join(' • ')}</span>
		<span class="copyright">© 2026 Orinasa Njarasoa</span>
		<a class="app-license" href="https://github.com/mpilasy/maripanaTokana/blob/main/LICENSE" target="_blank" rel="noopener">{$_('about_app_license')} ↗</a>
		<!-- How to use the app, and the credits for the data, images, fonts and libraries with their licences -->
		<span class="page-links">
			<button type="button" class="source-link help-link" onclick={onOpenHelp}>{$_('help_title')} ›</button>
			<button type="button" class="source-link help-link" onclick={() => (showLicenses = true)}>{$_('licenses_title')} ›</button>
		</span>
		<AppBadges android={onAndroid} {installable} />
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

</SettingsPage>

{#if showLicenses}
	<LicensesScreen onBack={() => (showLicenses = false)} />
{/if}

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

	.build {
		font-size: 0.6875rem;
		color: rgba(255, 255, 255, 0.4);
	}

	.copyright {
		margin-top: 6px;
		font-size: 0.875rem;
		color: rgba(255, 255, 255, 0.85);
	}

	.app-license {
		font-size: 0.8125rem;
		color: rgba(255, 255, 255, 0.6);
		text-align: center;
		text-decoration: none;
	}

	.page-links {
		display: flex;
		margin-top: 4px;
	}

	.source-link {
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
	}</style>
