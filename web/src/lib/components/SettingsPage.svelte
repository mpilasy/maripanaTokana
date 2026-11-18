<!-- SPDX-FileCopyrightText: Orinasa Njarasoa -->
<!-- SPDX-License-Identifier: AGPL-3.0-or-later -->
<script module lang="ts">
	// Open pages, bottom → top; Escape only closes the top-most one.
	const openPages: symbol[] = [];
</script>

<script lang="ts">
	import { onMount, type Snippet } from 'svelte';
	import { _ } from 'svelte-i18n';
	import LanguageFlagButton from './LanguageFlagButton.svelte';

	/** Shared shell for Settings-style pages: gradient + Blue Marble background, back header, scrolling body. */
	interface Props {
		title: string;
		/** Smaller text after the title, for example the place name. */
		subtitle?: string;
		onBack: () => void;
		children: Snippet;
		/**
		 * The language flag in a round button over the bottom right corner, as in the main screen's footer (also in
		 * right-to-left languages); it takes no height from the page.
		 */
		languageFlag?: string;
		onCycleLanguage?: () => void;
		/** The title as a large line (as the place name on the main screen) with the subtitle under it, instead of side by side. */
		stacked?: boolean;
		/** With `stacked`: a control after the subtitle, on the same line. */
		subtitleEnd?: Snippet;
		/** The page does not scroll: its children stack under the header, and a child with its own scroll fills the rest. */
		pinned?: boolean;
	}

	let { title, subtitle, onBack, children, languageFlag, onCycleLanguage, stacked = false, subtitleEnd, pinned = false }: Props = $props();

	const id = Symbol();
	onMount(() => {
		openPages.push(id);
		return () => {
			const i = openPages.indexOf(id);
			if (i !== -1) openPages.splice(i, 1);
		};
	});

	function handleKey(e: KeyboardEvent) {
		if (e.key === 'Escape' && openPages[openPages.length - 1] === id) onBack();
	}
</script>

<svelte:window onkeydown={handleKey} />

<div class="page" class:with-flag={languageFlag && onCycleLanguage} class:pinned>
	<header>
		<button class="back-btn" onclick={onBack} aria-label={$_('settings_back')}>
			<svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor">
				<path d="M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20v-2z"/>
			</svg>
		</button>
		{#if stacked}
			<div class="titles">
				<h2>{title}</h2>
				{#if subtitle || subtitleEnd}
					<span class="subtitle-row">
						{#if subtitle}<span class="subtitle">{subtitle}</span>{/if}
						{@render subtitleEnd?.()}
					</span>
				{/if}
			</div>
		{:else}
			<h2>{title}</h2>
			{#if subtitle}
				<span class="subtitle">{subtitle}</span>
			{/if}
		{/if}
	</header>
	{@render children()}
	{#if languageFlag && onCycleLanguage}
		<div class="flag-dock" dir="ltr">
			<LanguageFlagButton flag={languageFlag} {onCycleLanguage} />
		</div>
	{/if}
</div>

<style>
	.page {
		position: absolute;
		inset: 0;
		background: linear-gradient(to bottom, #0E0B3D, #1A1565);
		isolation: isolate;
		color: white;
		overflow-y: auto;
		/* No scrollbar, as in the Android app */
		scrollbar-width: none;
		z-index: 100;
		padding: 0 16px;
		padding-top: env(safe-area-inset-top);
		padding-bottom: max(24px, env(safe-area-inset-bottom));
		box-sizing: border-box;
	}

	/* Safari before 18.2 ignores scrollbar-width */
	.page::-webkit-scrollbar {
		display: none;
	}

	.page::before {
		content: '';
		position: absolute;
		inset: 0;
		background: url('/bg-blue-marble.webp') center/cover no-repeat;
		opacity: 0.12;
		pointer-events: none;
		z-index: -1;
	}

	/* Stays at the top while the page scrolls */
	header {
		position: sticky;
		top: 0;
		z-index: 1;
		display: flex;
		align-items: center;
		gap: 12px;
		margin: 0 -16px 16px;
		padding: 8px 16px;
		background: rgba(14, 11, 61, 0.92);
		backdrop-filter: blur(8px);
		flex-shrink: 0;
	}

	.back-btn {
		background: none;
		border: none;
		color: white;
		cursor: pointer;
		padding: 8px;
		display: flex;
		align-items: center;
		justify-content: center;
		border-radius: 50%;
		transition: background 0.2s;
	}

	.back-btn:hover {
		background: rgba(255, 255, 255, 0.1);
	}

	:global([dir='rtl']) .back-btn svg {
		transform: scaleX(-1);
	}

	header h2 {
		font-size: 1.25rem;
		font-weight: 700;
		margin: 0;
	}

	.subtitle {
		min-width: 0;
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
		font-size: 0.875rem;
		color: rgba(255, 255, 255, 0.7);
	}

	/* Stacked: the title as large as the place name on the main screen, the subtitle under it */
	.titles {
		display: flex;
		flex-direction: column;
		min-width: 0;
	}

	.subtitle-row {
		display: flex;
		align-items: center;
		gap: 4px;
		min-width: 0;
	}

	.titles h2 {
		font-family: var(--font-display);
		font-size: 1.875rem;
		line-height: 2.25rem;
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
	}

	.titles .subtitle {
		font-family: var(--font-body);
		color: rgba(255, 255, 255, 0.6);
	}

	/* The flag at the bottom right of the screen, whatever the text direction. The page keeps room under its content,
	   so the end of the content can scroll out from under it */
	.page.with-flag {
		padding-bottom: calc(64px + env(safe-area-inset-bottom));
	}

	/* After .with-flag: the child that scrolls keeps the room for the flag */
	.page.pinned {
		display: flex;
		flex-direction: column;
		overflow: hidden;
		padding-bottom: 0;
	}

	.flag-dock {
		position: fixed;
		z-index: 2;
		right: 16px;
		bottom: max(16px, env(safe-area-inset-bottom));
		padding: 4px;
		border: 1px solid var(--hairline);
		border-radius: 999px;
		background: rgba(14, 11, 61, 0.7);
	}

	/* Shared building blocks for pages rendered inside this shell */
	.page :global(.section) {
		margin-bottom: 24px;
	}

	.page :global(.group) {
		background: var(--surface-section);
		border: 1px solid var(--hairline);
		border-radius: 20px;
		padding: 8px 16px;
	}

	.page :global(.section-title) {
		font-size: 0.6875rem;
		font-weight: 600;
		letter-spacing: 1px;
		text-transform: uppercase;
		color: rgba(255, 255, 255, 0.5);
		margin: 0 0 8px 8px;
	}
</style>
