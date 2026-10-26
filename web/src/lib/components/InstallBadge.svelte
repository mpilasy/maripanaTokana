<!-- SPDX-FileCopyrightText: Orinasa Njarasoa -->
<!-- SPDX-License-Identifier: GPL-3.0-or-later -->
<script lang="ts">
	import { _ } from 'svelte-i18n';
	import { get } from 'svelte/store';
	import { installPrompt, isIos } from '#lib/installHint.js';
	import InstallDialog from './InstallDialog.svelte';

	/**
	 * "Add to Home screen" badge, in the style of the F-Droid badge. Chromium browsers open their own
	 * install dialog; Safari and Firefox give no install prompt, so the badge shows the steps instead.
	 */
	let showSteps = $state(false);

	// The text has about 58% of the badge width; long translations get a smaller font
	let bigSize = $derived(Math.min(8, 58 / ($_('install_badge_big').length * 0.56)));

	async function install() {
		const prompt = get(installPrompt);
		if (!prompt) {
			showSteps = true;
			return;
		}
		await prompt.prompt();
		// The browser gives a prompt only once
		installPrompt.set(null);
	}
</script>

<button type="button" class="badge" onclick={install} aria-label={`${$_('install_badge_small')} ${$_('install_badge_big')}`}>
	<span class="box">
		<img class="logo" src="/badges/pwa-logo.svg" alt="" width="1952" height="735" />
		<span class="text">
			<span class="small">{$_('install_badge_small')}</span>
			<span class="big" style:font-size="{bigSize}cqw">{$_('install_badge_big')}</span>
		</span>
	</span>
</button>
{#if showSteps}
	<InstallDialog ios={isIos()} onclose={() => (showSteps = false)} />
{/if}

<style>
	/* Same geometry as the 646 × 250 PNG badges: the box starts 41 px in, with a 4 px grey border */
	.badge {
		container-type: inline-size;
		display: block;
		padding: 0;
		border: none;
		background: none;
		cursor: pointer;
		font-family: 'Noto Sans', 'Roboto', system-ui, sans-serif;
	}

	.badge:focus-visible {
		outline: 2px solid rgba(255, 255, 255, 0.5);
		outline-offset: 2px;
	}

	.box {
		display: flex;
		align-items: center;
		gap: 3.4cqw;
		box-sizing: border-box;
		/* 564 × 168 px of 646, with 41 px around it, as in the PNG badges */
		width: 87.3cqw;
		height: 26cqw;
		margin: 6.35cqw;
		padding-inline-start: 5.3cqw;
		border: 0.62cqw solid #a6a6a6;
		border-radius: 3.7cqw;
		background: #000;
		color: #fff;
		text-align: start;
	}

	.logo {
		width: 23.2cqw;
		height: auto;
		flex: none;
	}

	.text {
		display: flex;
		flex-direction: column;
		line-height: 1;
		min-width: 0;
	}

	.small {
		font-size: 4.6cqw;
		font-weight: 500;
		letter-spacing: 0.15cqw;
		text-transform: uppercase;
	}

	.big {
		margin-top: 0.9cqw;
		font-weight: 500;
		white-space: nowrap;
	}
</style>
