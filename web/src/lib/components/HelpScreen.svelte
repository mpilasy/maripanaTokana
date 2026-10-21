<script lang="ts">
	import { _ } from 'svelte-i18n';
	import SettingsPage from './SettingsPage.svelte';

	interface Props { onBack: () => void; languageFlag: string; onCycleLanguage: () => void; }
	let { onBack, languageFlag, onCycleLanguage }: Props = $props();

	// How to use the app: short questions and answers; mirrors Android's HelpScreen
	const sections: { title: string; items: string[] }[] = [
		{ title: 'help_section_places', items: ['search', 'favorite_add', 'favorite_move', 'favorite_edit', 'place_details'] },
		{ title: 'help_section_display', items: ['language', 'font', 'units'] },
		{ title: 'help_section_weather', items: ['refresh', 'cut_title', 'share'] },
	];
</script>

<SettingsPage title={$_('help_title')} {onBack} {languageFlag} {onCycleLanguage}>
	{#each sections as section (section.title)}
		<div class="section">
			<div class="section-title">{$_(section.title)}</div>
			<div class="group">
				{#each section.items as item (item)}
					<div class="qa">
						<span class="question">{$_(`help_q_${item}`)}</span>
						<span class="answer">{$_(`help_a_${item}`)}</span>
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
		font-size: 15px;
		font-weight: 600;
	}

	.answer {
		font-family: var(--font-body);
		font-size: 14px;
		line-height: 20px;
		color: rgba(255, 255, 255, 0.75);
	}
</style>
