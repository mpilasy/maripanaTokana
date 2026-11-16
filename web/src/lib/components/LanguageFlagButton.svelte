<!-- SPDX-FileCopyrightText: Orinasa Njarasoa -->
<!-- SPDX-License-Identifier: GPL-3.0-or-later -->
<script lang="ts">
	import { _ } from 'svelte-i18n';
	import { longPress } from '#lib/actions/longPress.js';
	import PickerMenu from './PickerMenu.svelte';
	import { SUPPORTED_LOCALES } from '#lib/i18n/locales.js';
	import { localeIndex, selectLanguage } from '#lib/stores/preferences.js';

	/**
	 * The language flag, as in the main screen's footer: a tap shows the next language; holding it, or the arrow after
	 * it, lists them. The list opens upwards. Twin: LanguageFlagButton in
	 * app/src/main/java/orinasa/njarasoa/maripanatokana/ui/mobile/weather/components/FooterPickers.kt
	 */
	interface Props {
		flag: string;
		onCycleLanguage: () => void;
	}

	let { flag, onCycleLanguage }: Props = $props();
	let pickerOpen = $state(false);
	const languageItems = SUPPORTED_LOCALES.map((l) => ({ lead: l.flag, label: l.name, lang: l.tag }));
</script>

<span class="picker-box" dir="ltr">
	<button type="button" class="pill touch-target" onclick={onCycleLanguage} use:longPress={() => (pickerOpen = true)} aria-label={$_('cd_change_language')}>
		<span class="flag">{flag}</span>
	</button>
	<button type="button" class="show-all touch-target" aria-label={$_('cd_show_all')} aria-expanded={pickerOpen}
		onclick={(e) => { e.stopPropagation(); pickerOpen = !pickerOpen; }}>
		<svg viewBox="0 0 24 24" width="12" height="12" fill="currentColor" aria-hidden="true"><path d="M7.41 8.59L12 13.17l4.59-4.58L18 10l-6 6-6-6 1.41-1.41z"/></svg>
	</button>
	<PickerMenu bind:open={pickerOpen} items={languageItems} current={$localeIndex} onSelect={selectLanguage} above />
</span>

<style>
	.picker-box {
		position: relative;
		display: flex;
		align-items: center;
	}

	.pill {
		display: flex;
		align-items: center;
		cursor: pointer;
		background: none;
		border: none;
		padding: 6px 8px;
		border-radius: 999px;
		transition: background 0.2s, transform 0.1s;
	}

	.pill:hover {
		background: var(--surface-tile-highlight);
	}

	.pill:active {
		transform: scale(0.97);
	}

	.pill:focus-visible,
	.show-all:focus-visible {
		outline: 2px solid rgba(255, 255, 255, 0.5);
		outline-offset: 2px;
	}

	.show-all {
		display: flex;
		align-items: center;
		padding: 0 4px 0 0;
		border: none;
		background: none;
		color: rgba(255, 255, 255, 0.7);
		cursor: pointer;
	}

	.flag {
		font-size: 1.125rem;
		line-height: 1;
	}
</style>
