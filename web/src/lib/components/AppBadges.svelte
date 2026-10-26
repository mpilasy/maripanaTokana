<!-- SPDX-FileCopyrightText: Orinasa Njarasoa -->
<!-- SPDX-License-Identifier: GPL-3.0-or-later -->
<script lang="ts">
	import { _ } from 'svelte-i18n';
	import InstallBadge from './InstallBadge.svelte';

	/**
	 * Ways to get the app. `android`: download links for the Android app (F-Droid and Obtainium as in
	 * the README, and the GitHub releases). `installable`: the "Add to Home screen" badge for the web app.
	 */
	let { android, installable }: { android: boolean; installable: boolean } = $props();
	const FDROID_URL = 'https://f-droid.org/packages/orinasa.njarasoa.maripanatokana/';
	// Obtainium adds the app with an APK filter, so it always installs the F-Droid APK (no Google Play
	// services) and does not ask which of the two release APKs to use. It always installs the latest release.
	const OBTAINIUM_CONFIG = {
		id: 'orinasa.njarasoa.maripanatokana',
		url: 'https://github.com/mpilasy/maripanaTokana',
		author: 'mpilasy',
		name: 'maripànaTokana',
		additionalSettings: JSON.stringify({ apkFilterRegEx: '^maripanaTokana-v.*\\.apk$' }),
	};
	const OBTAINIUM_URL = 'https://apps.obtainium.imranr.dev/redirect?r='
		+ encodeURIComponent('obtainium://app/' + encodeURIComponent(JSON.stringify(OBTAINIUM_CONFIG)));
	// GitHub redirects /releases/latest to the newest release tag, so the link needs no change for each release
	const GITHUB_URL = 'https://github.com/mpilasy/maripanaTokana/releases/latest';
</script>

<div class="get-app">
	{#if android}
		<p class="text">{$_('android_app_text')}</p>
	{/if}
	<div class="badges">
		{#if android}
		<a href={FDROID_URL} target="_blank" rel="noopener noreferrer">
			<img src="/badges/get-it-on-fdroid.png" alt={$_('android_app_fdroid')} width="646" height="250" />
		</a>
		<a href={OBTAINIUM_URL} target="_blank" rel="noopener noreferrer">
			<img src="/badges/get-it-on-obtainium.png" alt={$_('android_app_obtainium')} width="646" height="250" />
		</a>
		<a href={GITHUB_URL} target="_blank" rel="noopener noreferrer">
			<img src="/badges/get-it-on-github.png" alt={$_('android_app_github')} width="646" height="250" />
		</a>
		{/if}
		{#if installable}
			<InstallBadge />
		{/if}
	</div>
</div>

<style>
	.get-app {
		margin-top: 12px;
		width: 100%;
	}

	.text {
		margin: 0 0 8px;
		font-size: 13px;
		color: rgba(255, 255, 255, 0.6);
	}

	.badges {
		display: flex;
		flex-wrap: wrap;
		justify-content: center;
		gap: 8px;
	}

	.badges > :global(*) {
		flex: 0 1 150px;
		border-radius: 8px;
	}

	.badges a:focus-visible {
		outline: 2px solid rgba(255, 255, 255, 0.5);
		outline-offset: 2px;
	}

	.badges img {
		display: block;
		width: 100%;
		height: auto;
	}
</style>
