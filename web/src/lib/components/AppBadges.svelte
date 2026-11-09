<!-- SPDX-FileCopyrightText: Orinasa Njarasoa -->
<!-- SPDX-License-Identifier: GPL-3.0-or-later -->
<script lang="ts">
	import { _ } from 'svelte-i18n';
	import InstallBadge from './InstallBadge.svelte';

	/**
	 * The QR codes of the app (source code, web app, F-Droid and Obtainium), and ways to get it. `android`: the
	 * GitHub releases badge for the Android app. `installable`: the "Add to Home screen" badge for the web app.
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
	const SOURCE_URL = 'https://github.com/mpilasy/maripanaTokana';
	const WEB_URL = 'https://maripana.njarasoa.org';
</script>

<div class="get-app">
	{#if android}
		<p class="text">{$_('android_app_text')}</p>
	{/if}
	<!-- One even grid: a QR code in each cell, for a phone to scan from a computer. A click opens the address.
	     Android's AboutScreen.kt has the same four codes. -->
	<div class="codes">
		<a class="cell" href={SOURCE_URL} target="_blank" rel="noopener noreferrer">
			<img class="qr" src="/qr/source_code.png" alt="" width="112" height="112" />
			<span class="label">{$_('about_source_code')}</span>
			<span class="address" dir="ltr">github.com/mpilasy/maripanaTokana</span>
		</a>
		<a class="cell" href={WEB_URL} target="_blank" rel="noopener noreferrer">
			<img class="qr" src="/qr/web_app.png" alt="" width="112" height="112" />
			<span class="label">{$_('about_web_version')}</span>
			<span class="address" dir="ltr">maripana.njarasoa.org</span>
		</a>
		<a class="cell" href={FDROID_URL} target="_blank" rel="noopener noreferrer">
			<img class="qr" src="/qr/fdroid.png" alt="" width="112" height="112" />
			<img class="badge" src="/badges/get-it-on-fdroid.png" alt={$_('android_app_fdroid')} width="646" height="250" />
		</a>
		<a class="cell" href={OBTAINIUM_URL} target="_blank" rel="noopener noreferrer">
			<img class="qr" src="/qr/obtainium.png" alt="" width="112" height="112" />
			<img class="badge" src="/badges/get-it-on-obtainium.png" alt={$_('android_app_obtainium')} width="646" height="250" />
		</a>
	</div>
	{#if android || installable}
		<div class="badges">
			{#if android}
				<a href={GITHUB_URL} target="_blank" rel="noopener noreferrer">
					<img src="/badges/get-it-on-github.png" alt={$_('android_app_github')} width="646" height="250" />
				</a>
			{/if}
			{#if installable}
				<InstallBadge />
			{/if}
		</div>
	{/if}
</div>

<style>
	.get-app {
		margin-top: 12px;
		width: 100%;
	}

	.text {
		margin: 0 0 12px;
		font-size: 0.8125rem;
		color: rgba(255, 255, 255, 0.6);
	}

	/* Two cells across on a phone, four on a wider screen; every cell is the same width */
	.codes {
		display: grid;
		grid-template-columns: repeat(2, 140px);
		justify-content: center;
		align-items: start;
		gap: 20px 24px;
	}

	@media (min-width: 640px) {
		.codes {
			grid-template-columns: repeat(4, 140px);
		}
	}

	.cell {
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: 8px;
		color: inherit;
		text-decoration: none;
		text-align: center;
		border-radius: 8px;
	}

	.qr {
		display: block;
		width: 112px;
		height: 112px;
		/* No smoothing: the small image keeps sharp squares when it grows */
		image-rendering: pixelated;
	}

	.label {
		font-size: 13px;
		line-height: 1.2;
	}

	.address {
		margin-top: -6px;
		font-size: 11px;
		opacity: 0.55;
		overflow-wrap: anywhere;
	}

	.badge {
		display: block;
		width: 128px;
		height: auto;
	}

	.badges {
		display: flex;
		flex-wrap: wrap;
		justify-content: center;
		gap: 8px;
		margin-top: 20px;
	}

	.badges > :global(*) {
		flex: 0 1 150px;
		border-radius: 8px;
	}

	.badges img {
		display: block;
		width: 100%;
		height: auto;
	}

	.cell:focus-visible,
	.badges a:focus-visible {
		outline: 2px solid rgba(255, 255, 255, 0.5);
		outline-offset: 2px;
	}
</style>
