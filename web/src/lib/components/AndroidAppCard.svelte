<!-- SPDX-FileCopyrightText: Orinasa Njarasoa -->
<!-- SPDX-License-Identifier: GPL-3.0-or-later -->
<script lang="ts">
	import { _ } from 'svelte-i18n';
	import SectionCard from './SectionCard.svelte';

	/** Download links for the Android app, as in the README. Shown only on Android. */
	const FDROID_URL = 'https://f-droid.org/packages/orinasa.njarasoa.maripanatokana/';
	const OBTAINIUM_URL = 'https://apps.obtainium.imranr.dev/redirect?r=obtainium://add/https://github.com/mpilasy/maripanaTokana';
</script>

<SectionCard title={$_('android_app_title')}>
	<p class="text">{$_('android_app_text')}</p>
	<div class="badges">
		<a href={FDROID_URL} target="_blank" rel="noopener noreferrer">
			<img src="/badges/get-it-on-fdroid.png" alt={$_('android_app_fdroid')} width="646" height="250" />
		</a>
		<a href={OBTAINIUM_URL} target="_blank" rel="noopener noreferrer">
			<img src="/badges/get-it-on-obtainium.png" alt={$_('android_app_obtainium')} width="646" height="250" />
		</a>
	</div>
</SectionCard>

<style>
	.text {
		margin: 0 0 8px;
		font-size: 0.85rem;
		opacity: 0.8;
	}

	.badges {
		display: flex;
		flex-wrap: wrap;
		gap: 8px;
	}

	.badges a {
		flex: 1 1 140px;
		max-width: 200px;
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
