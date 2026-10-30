<!-- SPDX-FileCopyrightText: Orinasa Njarasoa -->
<!-- SPDX-License-Identifier: GPL-3.0-or-later -->
<script lang="ts">
	import { _ } from 'svelte-i18n';
	import { longPress } from '#lib/actions/longPress.js';
	import PickerMenu from './PickerMenu.svelte';
	import { SUPPORTED_LOCALES } from '#lib/i18n/locales.js';
	import { fontPairings } from '#lib/fonts.js';
	import { fontIndex, localeIndex, selectFont, selectLanguage } from '#lib/stores/preferences.js';

	interface Props {
		fontName: string;
		currentFlag: string;
		onCycleFont: () => void;
		onCycleLanguage: () => void;
		onOpenAbout: () => void;
		onOpenHelp: () => void;
	}

	let { fontName, currentFlag, onCycleFont, onCycleLanguage, onOpenAbout, onOpenHelp }: Props = $props();
	// Holding the flag or the font lists every language or font to pick one
	let pickerOpen = $state(false);
	let fontPickerOpen = $state(false);
	const languageItems = SUPPORTED_LOCALES.map((l) => ({ lead: l.flag, label: l.name, lang: l.tag }));
	// Each font name in its own display font
	const fontItems = fontPairings.map((p) => ({ label: p.name, font: p.displayFamily }));
</script>

<!-- The font list draws each name in its font, so it loads them all while it is open -->
<svelte:head>
	{#if fontPickerOpen}
		{#each fontPairings as p (p.name)}
			{#if p.fontCssUrl}<link href={p.fontCssUrl} rel="stylesheet" />{/if}
		{/each}
	{/if}
</svelte:head>

<!-- A small arrow after the font name or the flag: a tap opens the same list as holding it -->
<!-- Twin: the KeyboardArrowDown icons in app/src/main/java/orinasa/njarasoa/maripanatokana/ui/weather/components/FooterPickers.kt -->
{#snippet showAll(toggle: () => void, expanded: boolean)}
	<button type="button" class="show-all touch-target" aria-label={$_('cd_show_all')} aria-expanded={expanded}
		onclick={(e) => { e.stopPropagation(); toggle(); }}>
		<svg viewBox="0 0 24 24" width="12" height="12" fill="currentColor" aria-hidden="true"><path d="M7.41 8.59L12 13.17l4.59-4.58L18 10l-6 6-6-6 1.41-1.41z"/></svg>
	</button>
{/snippet}

<footer class="footer" dir="ltr">
	<span class="picker-box">
		<button type="button" class="pill touch-target" onclick={onCycleFont} use:longPress={() => (fontPickerOpen = true)} aria-label={$_('cd_change_font')}>
			<span class="icon">Aa</span>
			<span class="name">{fontName.split(' + ')[0]}</span>
		</button>
		{@render showAll(() => (fontPickerOpen = !fontPickerOpen), fontPickerOpen)}
		<PickerMenu bind:open={fontPickerOpen} items={fontItems} current={$fontIndex} onSelect={selectFont} above alignStart />
	</span>
	<!-- Help, and About (which credits the data sources) -->
	<div class="footer-credits">
		<span class="links">
			<button type="button" class="about-link touch-target" onclick={onOpenHelp}>{$_('help_title')}</button>
			<span class="dot">·</span>
			<button type="button" class="about-link touch-target" onclick={onOpenAbout}>{$_('about_title')}</button>
		</span>
		<span class="version">v1.6.20261030023347</span>
	</div>
	<span class="picker-box">
		<button type="button" class="pill touch-target" onclick={onCycleLanguage} use:longPress={() => (pickerOpen = true)} aria-label={$_('cd_change_language')}>
			<span class="flag">{currentFlag}</span>
		</button>
		{@render showAll(() => (pickerOpen = !pickerOpen), pickerOpen)}
		<PickerMenu bind:open={pickerOpen} items={languageItems} current={$localeIndex} onSelect={selectLanguage} above />
	</span>
</footer>

<style>
	.footer {
		display: flex;
		align-items: center;
		gap: 8px;
		padding: 8px 0;
		padding-bottom: max(8px, env(safe-area-inset-bottom));
		flex-shrink: 0;
	}

	.pill {
		display: flex;
		align-items: center;
		gap: 6px;
		cursor: pointer;
		background: none;
		border: none;
		padding: 6px 8px;
		border-radius: 999px;
		transition: background 0.2s, transform 0.1s;
		font-family: inherit;
		flex-shrink: 0;
	}

	.pill:hover {
		background: var(--surface-tile-highlight);
	}

	.pill:focus-visible {
		outline: 2px solid rgba(255, 255, 255, 0.5);
		outline-offset: 2px;
	}

	.pill:active {
		transform: scale(0.97);
	}

	.icon {
		font-size: 0.8125rem;
		color: rgba(255, 255, 255, 0.7);
	}

	.name {
		font-size: 0.6875rem;
		color: rgba(255, 255, 255, 0.7);
		white-space: nowrap;
	}

	.picker-box {
		position: relative;
		flex-shrink: 0;
		display: flex;
		align-items: center;
	}

	.show-all {
		display: flex;
		align-items: center;
		padding: 0;
		border: none;
		background: none;
		color: rgba(255, 255, 255, 0.7);
		cursor: pointer;
	}

	.show-all:focus-visible {
		outline: 2px solid rgba(255, 255, 255, 0.5);
		outline-offset: 2px;
	}

	.flag {
		font-size: 1rem;
		line-height: 1;
	}

	.footer-credits {
		flex: 1;
		min-width: 0;
		text-align: center;
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: 2px;
	}

	.links {
		display: flex;
		align-items: center;
		gap: 4px;
	}

	.dot {
		font-size: 0.625rem;
		color: rgba(255,255,255,0.3);
	}

	.about-link {
		background: none;
		border: none;
		padding: 2px 4px;
		font-family: inherit;
		cursor: pointer;
		font-size: 0.625rem;
		line-height: 0.8125rem;
		color: rgba(255,255,255,0.7);
		text-decoration: underline;
	}

	.version {
		font-size: 0.5625rem;
		line-height: 0.75rem;
		color: rgba(255,255,255,0.6);
	}
</style>
