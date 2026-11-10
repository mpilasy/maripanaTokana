<!-- SPDX-FileCopyrightText: Orinasa Njarasoa -->
<!-- SPDX-License-Identifier: GPL-3.0-or-later -->
<script lang="ts">
	import { _ } from 'svelte-i18n';
	import InstallBadge from './InstallBadge.svelte';
	import QrDialog from './QrDialog.svelte';
	import { isIos } from '#lib/installHint.js';

	/**
	 * The links of the app (source code, web app, F-Droid and Obtainium), and ways to get it. `android`: the
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

	type Code = { label: string; address: string; url: string; qr: string };
	const codes = $derived({
		source: { label: $_('about_source_code'), address: 'github.com/mpilasy/maripanaTokana', url: SOURCE_URL, qr: '/qr/source_code.png' },
		web: { label: $_('about_web_version'), address: 'maripana.njarasoa.org', url: WEB_URL, qr: '/qr/web_app.png' },
		fdroid: { label: 'F-Droid', address: 'f-droid.org', url: FDROID_URL, qr: '/qr/fdroid.png' },
		obtainium: { label: 'Obtainium', address: 'apps.obtainium.imranr.dev', url: OBTAINIUM_URL, qr: '/qr/obtainium.png' },
	});
	let open = $state<Code | null>(null);
	// F-Droid and Obtainium install Android apps only: an iPhone or iPad cannot use them
	const ios = isIos();

	function show(e: MouseEvent, code: Code) {
		e.preventDefault();
		open = code;
	}
</script>

<div class="get-app">
	{#if android}
		<p class="text">{$_('android_app_text')}</p>
	{/if}
	<!-- A click shows the QR code of the link, for a phone to scan from a computer; the dialog opens the address.
	     Android's AboutScreen.kt has the same four links. -->
	<div class="links">
		{#each [codes.source, codes.web] as code (code.qr)}
			<a class="link" href={code.url} target="_blank" rel="noopener noreferrer" onclick={(e) => show(e, code)}>
				<span class="label">{code.label} ›</span>
				<span class="address" dir="ltr">{code.address}</span>
			</a>
		{/each}
	</div>
	{#if !ios || installable}
	<div class="badges">
		{#if !ios}
		<a href={FDROID_URL} target="_blank" rel="noopener noreferrer" onclick={(e) => show(e, codes.fdroid)}>
			<img src="/badges/get-it-on-fdroid.png" alt={$_('android_app_fdroid')} width="646" height="250" />
		</a>
		<a href={OBTAINIUM_URL} target="_blank" rel="noopener noreferrer" onclick={(e) => show(e, codes.obtainium)}>
			<img src="/badges/get-it-on-obtainium.png" alt={$_('android_app_obtainium')} width="646" height="250" />
		</a>
		{/if}
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

{#if open}
	<QrDialog label={open.label} address={open.address} url={open.url} qr={open.qr} onclose={() => (open = null)} />
{/if}

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

	.links {
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: 4px;
	}

	.link {
		display: flex;
		flex-direction: column;
		align-items: center;
		padding: 4px 12px;
		color: inherit;
		text-decoration: none;
		text-align: center;
		border-radius: 8px;
	}

	.label {
		font-size: 14px;
		font-weight: 600;
	}

	.address {
		font-size: 11px;
		opacity: 0.55;
		overflow-wrap: anywhere;
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

	.link:focus-visible,
	.badges a:focus-visible {
		outline: 2px solid rgba(255, 255, 255, 0.5);
		outline-offset: 2px;
	}
</style>
