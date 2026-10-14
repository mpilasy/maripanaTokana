<script lang="ts">
	import { _ } from 'svelte-i18n';
	import { version } from '../../../package.json';
	import { creditSections } from '$lib/credits';
	import SettingsPage from './SettingsPage.svelte';

	interface Props { onBack: () => void; }
	let { onBack }: Props = $props();
</script>

<SettingsPage title={$_('about_title')} {onBack}>
	<div class="section group identity">
		<span class="app-name">maripànaTokana</span>
		<span class="version">v{version}</span>
		<span class="copyright">© 2026 Orinasa Njarasoa</span>
		<span class="app-license">{$_('about_app_license')}</span>
		<a class="source-link" href="https://github.com/mpilasy/maripanaTokana" target="_blank" rel="noopener">{$_('about_source_code')} ↗</a>
	</div>

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

	.source-link:hover {
		background: var(--surface-tile);
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
