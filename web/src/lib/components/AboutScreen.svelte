<script lang="ts">
	import { _ } from 'svelte-i18n';
	import { version } from '../../../package.json';
	import { creditSections } from '$lib/credits';
	import SettingsPage from './SettingsPage.svelte';
	import { deviceInfo, type DeviceRow } from '$lib/deviceInfo';
	import { onMount } from 'svelte';
	import { SUPPORTED_LOCALES } from '$lib/i18n/index';
	import { localeIndex } from '$lib/stores/preferences';

	interface Props { onBack: () => void; onOpenHelp: () => void; }
	let { onBack, onOpenHelp }: Props = $props();

	let deviceRows = $state<DeviceRow[]>([]);
	onMount(() => {
		deviceInfo(version, SUPPORTED_LOCALES[$localeIndex].tag).then((rows) => (deviceRows = rows));
	});
</script>

<SettingsPage title={$_('about_title')} {onBack}>
	<div class="section group identity">
		<span class="app-name">maripànaTokana</span>
		<span class="version">v{version}</span>
		<span class="copyright">© 2026 Orinasa Njarasoa</span>
		<span class="app-license">{$_('about_app_license')}</span>
		<!-- How to use the app -->
		<button type="button" class="source-link help-link" onclick={onOpenHelp}>{$_('help_title')} ›</button>
		<a class="source-link" href="https://github.com/mpilasy/maripanaTokana" target="_blank" rel="noopener">{$_('about_source_code')} ↗</a>
		<a class="source-link web-link" href="https://maripana.njarasoa.org" target="_blank" rel="noopener">
			{$_('about_web_version')} ↗
			<span class="web-url">maripana.njarasoa.org</span>
		</a>
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
		font-size: 24px;
		font-weight: 700;
	}

	.version {
		font-size: 13px;
		color: rgba(255, 255, 255, 0.55);
	}

	.copyright {
		margin-top: 8px;
		font-size: 14px;
		color: rgba(255, 255, 255, 0.85);
	}

	.app-license {
		font-size: 13px;
		color: rgba(255, 255, 255, 0.6);
	}

	.source-link {
		margin-top: 8px;
		padding: 6px 12px;
		font-size: 14px;
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

	.web-link {
		margin-top: 0;
		display: flex;
		flex-direction: column;
		align-items: center;
		border-radius: 16px;
	}

	.web-url {
		font-size: 12px;
		font-weight: 400;
		color: rgba(255, 255, 255, 0.55);
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
		font-size: 13px;
		color: rgba(255, 255, 255, 0.6);
	}

	.device-value {
		flex: 1;
		min-width: 0;
		font-size: 13px;
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
		font-size: 15px;
		font-weight: 500;
	}

	.credit-note {
		font-size: 12px;
		line-height: 16px;
		color: rgba(255, 255, 255, 0.55);
	}

	.credit-license {
		width: 110px;
		flex-shrink: 0;
		text-align: end;
		font-size: 12px;
		color: rgba(255, 255, 255, 0.65);
	}
</style>
