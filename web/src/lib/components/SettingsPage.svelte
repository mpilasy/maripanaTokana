<!-- SPDX-FileCopyrightText: Orinasa Njarasoa -->
<!-- SPDX-License-Identifier: GPL-3.0-or-later -->
<script module lang="ts">
	// Open pages, bottom → top; Escape only closes the top-most one.
	const openPages: symbol[] = [];
</script>

<script lang="ts">
	import { onMount, type Snippet } from 'svelte';
	import { _ } from 'svelte-i18n';

	/** Shared shell for Settings-style pages: gradient + Blue Marble background, back header, scrolling body. */
	interface Props {
		title: string;
		onBack: () => void;
		children: Snippet;
		/** The language flag at the bottom, as in the main screen's footer: a tap shows the next language. */
		languageFlag?: string;
		onCycleLanguage?: () => void;
	}

	let { title, onBack, children, languageFlag, onCycleLanguage }: Props = $props();

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

<div class="page">
	<header>
		<button class="back-btn" onclick={onBack} aria-label={$_('settings_back')}>
			<svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor">
				<path d="M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20v-2z"/>
			</svg>
		</button>
		<h2>{title}</h2>
		{#if languageFlag && onCycleLanguage}
			<button type="button" class="page-flag" onclick={onCycleLanguage} aria-label={$_('cd_change_language')}>{languageFlag}</button>
		{/if}
	</header>
	{@render children()}
</div>

<style>
	.page {
		position: absolute;
		inset: 0;
		background: linear-gradient(to bottom, #0E0B3D, #1A1565);
		isolation: isolate;
		color: white;
		overflow-y: auto;
		z-index: 100;
		padding: 0 16px;
		padding-top: env(safe-area-inset-top);
		padding-bottom: max(24px, env(safe-area-inset-bottom));
		box-sizing: border-box;
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
		font-size: 20px;
		font-weight: 700;
		margin: 0;
	}

	/* Shared building blocks for pages rendered inside this shell */
	.page-flag {
		margin-inline-start: auto;
		padding: 6px 10px;
		border: none;
		border-radius: 999px;
		background: none;
		font-size: 22px;
		line-height: 1;
		cursor: pointer;
	}

	.page-flag:hover {
		background: var(--surface-tile);
	}

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
		font-size: 11px;
		font-weight: 600;
		letter-spacing: 1px;
		text-transform: uppercase;
		color: rgba(255, 255, 255, 0.5);
		margin: 0 0 8px 8px;
	}
</style>
