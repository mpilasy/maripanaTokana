<!-- SPDX-FileCopyrightText: Orinasa Njarasoa -->
<!-- SPDX-License-Identifier: GPL-3.0-or-later -->
<script lang="ts">
	import { _ } from 'svelte-i18n';

	/** Download links for the Android app (F-Droid and Obtainium as in the README, and the GitHub releases). The About page shows them only on Android. */
	const FDROID_URL = 'https://f-droid.org/packages/orinasa.njarasoa.maripanatokana/';
	const OBTAINIUM_URL = 'https://apps.obtainium.imranr.dev/redirect?r=obtainium://add/https://github.com/mpilasy/maripanaTokana';
	// GitHub redirects /releases/latest to the newest release tag, so the link needs no change for each release
	const GITHUB_URL = 'https://github.com/mpilasy/maripanaTokana/releases/latest';
</script>

<div class="android-app">
	<p class="text">{$_('android_app_text')}</p>
	<div class="badges">
		<a href={FDROID_URL} target="_blank" rel="noopener noreferrer">
			<img src="/badges/get-it-on-fdroid.png" alt={$_('android_app_fdroid')} width="646" height="250" />
		</a>
		<a href={OBTAINIUM_URL} target="_blank" rel="noopener noreferrer">
			<img src="/badges/get-it-on-obtainium.png" alt={$_('android_app_obtainium')} width="646" height="250" />
		</a>
		<a href={GITHUB_URL} target="_blank" rel="noopener noreferrer">
			<img src="/badges/get-it-on-github.png" alt={$_('android_app_github')} width="646" height="250" />
		</a>
	</div>
</div>

<style>
	.android-app {
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

	.badges a {
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
