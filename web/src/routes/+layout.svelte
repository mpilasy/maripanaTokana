<script lang="ts">
	import { onMount } from 'svelte';
	import { locale } from 'svelte-i18n';
	import { initI18n, SUPPORTED_LOCALES } from '$lib/i18n/index';
	import { fontPairings } from '$shared/fonts';
	import { fontIndex, localeIndex } from '$lib/stores/preferences';
	import { refreshIfStale } from '$lib/stores/weather';

	let { children } = $props();

	let fontUrl = $state('');
	let displayFamily = $state('system-ui, sans-serif');
	let bodyFamily = $state('system-ui, sans-serif');
	let fontFeatures = $state('normal');

	// Initialize i18n on module load
	const savedIndex = typeof localStorage !== 'undefined'
		? JSON.parse(localStorage.getItem('locale_index') ?? '0')
		: 0;
	initI18n(SUPPORTED_LOCALES[savedIndex]?.tag);

	// React to locale index changes
	$effect(() => {
		const loc = SUPPORTED_LOCALES[$localeIndex];
		if (loc) {
			locale.set(loc.tag);
			if (typeof document !== 'undefined') {
				document.documentElement.dir = loc.tag === 'ar' ? 'rtl' : 'ltr';
				document.documentElement.lang = loc.tag;
			}
		}
	});

	// React to font index changes
	$effect(() => {
		const pairing = fontPairings[$fontIndex];
		if (pairing) {
			fontUrl = pairing.googleFontsUrl ?? '';
			displayFamily = pairing.displayFamily;
			bodyFamily = pairing.bodyFamily;
			fontFeatures = pairing.bodyFontFeatures ?? 'normal';
		}
	});

	onMount(() => {
		// Auto-refresh on tab focus
		const handler = () => {
			if (document.visibilityState === 'visible') {
				refreshIfStale();
			}
		};
		document.addEventListener('visibilitychange', handler);
		return () => document.removeEventListener('visibilitychange', handler);
	});
</script>

<svelte:head>
	{#if fontUrl}
		<link href={fontUrl} rel="stylesheet" />
	{/if}
</svelte:head>

<div
	class="app-shell"
	style:--font-display={displayFamily}
	style:--font-body={bodyFamily}
	style:--font-features={fontFeatures}
>
	{@render children()}
</div>

<style>
	:global(*, *::before, *::after) {
		box-sizing: border-box;
		margin: 0;
		padding: 0;
	}

	:global(body) {
		background: #0E0B3D;
		color: white;
		overflow: hidden;
		-webkit-font-smoothing: antialiased;
		-moz-osx-font-smoothing: grayscale;
	}

	:global(html, body) {
		height: 100%;
		width: 100%;
	}

	.app-shell {
		width: 100%;
		height: 100dvh;
		background: linear-gradient(to bottom, #0E0B3D, #1A1565);
		position: relative;
		overflow: hidden;
		font-family: var(--font-body);
	}
</style>
