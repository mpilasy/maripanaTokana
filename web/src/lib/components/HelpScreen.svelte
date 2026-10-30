<!-- SPDX-FileCopyrightText: Orinasa Njarasoa -->
<!-- SPDX-License-Identifier: GPL-3.0-or-later -->
<script lang="ts">
	import { _, locale, waitLocale } from 'svelte-i18n';
	import { onMount } from 'svelte';
	import SettingsPage from './SettingsPage.svelte';
	import { canAddToHomeScreen, instructionsLocale } from '#lib/installHint.js';
	import { SUPPORTED_LOCALES } from '#lib/i18n/index.js';

	interface Props { onBack: () => void; languageFlag: string; onCycleLanguage: () => void; }
	let { onBack, languageFlag, onCycleLanguage }: Props = $props();

	// How to use the app: short questions and answers; mirrors Android's HelpScreen
	const sections: { title: string; items: string[] }[] = [
		{ title: 'help_section_places', items: ['search', 'favorite_add', 'favorite_move', 'favorite_edit', 'place_details'] },
		{ title: 'help_section_display', items: ['language', 'font', 'units'] },
		{ title: 'help_section_weather', items: ['refresh', 'cut_title', 'share'] },
		// Web only: iPhone and iPad browsers do not offer to install the app
		...(canAddToHomeScreen() ? [{ title: 'help_section_install', items: ['install'] }] : []),
	];

	// The install steps name browser menu items: with the app in Malagasy, they use the phone language
	let installTag = $derived(instructionsLocale($locale ?? 'en', typeof navigator === 'undefined' ? [] : navigator.languages ?? [], SUPPORTED_LOCALES.map((l) => l.tag)));
	let installReady = $state(false);
	onMount(() => {
		waitLocale(installTag).then(() => (installReady = true));
	});
	const sectionTag = (title: string) => (title === 'help_section_install' && installReady ? installTag : undefined);
</script>

<SettingsPage title={$_('help_title')} {onBack} {languageFlag} {onCycleLanguage}>
	{#each sections as section (section.title)}
		{@const tag = sectionTag(section.title)}
		<div class="section" lang={tag} dir={tag === 'ar' ? 'rtl' : tag ? 'ltr' : undefined}>
			<div class="section-title">{$_(section.title, { locale: tag })}</div>
			<div class="group">
				{#each section.items as item (item)}
					<div class="qa">
						<span class="question">{$_(`help_q_${item}`, { locale: tag })}</span>
						<span class="answer">{$_(`help_a_${item}`, { locale: tag })}</span>
					</div>
				{/each}
			</div>
		</div>
	{/each}
</SettingsPage>

<style>
	.qa {
		display: flex;
		flex-direction: column;
		gap: 4px;
		padding: 12px 0;
	}

	.qa + .qa {
		border-top: 1px solid var(--hairline);
	}

	.question {
		font-family: var(--font-body);
		font-size: 0.9375rem;
		font-weight: 600;
	}

	.answer {
		font-family: var(--font-body);
		font-size: 0.875rem;
		line-height: 1.25rem;
		color: rgba(255, 255, 255, 0.75);
	}
</style>
