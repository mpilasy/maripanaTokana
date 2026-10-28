<!-- SPDX-FileCopyrightText: Orinasa Njarasoa -->
<!-- SPDX-License-Identifier: GPL-3.0-or-later -->
<script lang="ts">
	import { _ } from 'svelte-i18n';

	interface Props {
		fontName: string;
		currentFlag: string;
		onCycleFont: () => void;
		onCycleLanguage: () => void;
		onOpenAbout: () => void;
		onOpenHelp: () => void;
	}

	let { fontName, currentFlag, onCycleFont, onCycleLanguage, onOpenAbout, onOpenHelp }: Props = $props();
</script>

<footer class="footer" dir="ltr">
	<button type="button" class="pill" onclick={onCycleFont} aria-label={$_('cd_change_font')}>
		<span class="icon">Aa</span>
		<span class="name">{fontName.split(' + ')[0]}</span>
	</button>
	<!-- Help, and About (which credits the data sources) -->
	<div class="footer-credits">
		<span class="links">
			<button type="button" class="about-link" onclick={onOpenHelp}>{$_('help_title')}</button>
			<span class="dot">·</span>
			<button type="button" class="about-link" onclick={onOpenAbout}>{$_('about_title')}</button>
		</span>
		<span class="version">v1.6.20261028234248</span>
	</div>
	<button type="button" class="pill" onclick={onCycleLanguage} aria-label={$_('cd_change_language')}>
		<span class="flag">{currentFlag}</span>
	</button>
</footer>

<style>
	.footer {
		display: flex;
		align-items: center;
		gap: 8px;
		padding: 8px 0;
		padding-bottom: max(8px, env(safe-area-inset-bottom));
		flex-shrink: 0;
	}

	.pill {
		display: flex;
		align-items: center;
		gap: 6px;
		cursor: pointer;
		background: none;
		border: none;
		padding: 6px 8px;
		opacity: 0.7;
		border-radius: 999px;
		transition: background 0.2s, transform 0.1s;
		font-family: inherit;
		flex-shrink: 0;
	}

	.pill:hover {
		background: var(--surface-tile-highlight);
	}

	.pill:focus-visible {
		outline: 2px solid rgba(255, 255, 255, 0.5);
		outline-offset: 2px;
	}

	.pill:active {
		transform: scale(0.97);
	}

	.icon {
		font-size: 13px;
		color: rgba(255, 255, 255, 0.6);
	}

	.name {
		font-size: 11px;
		color: rgba(255, 255, 255, 0.6);
		white-space: nowrap;
	}

	.flag {
		font-size: 16px;
		line-height: 1;
	}

	.footer-credits {
		flex: 1;
		min-width: 0;
		text-align: center;
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: 2px;
	}

	.links {
		display: flex;
		align-items: center;
		gap: 4px;
	}

	.dot {
		font-size: 10px;
		color: rgba(255,255,255,0.3);
	}

	.about-link {
		background: none;
		border: none;
		padding: 2px 4px;
		font-family: inherit;
		cursor: pointer;
		font-size: 10px;
		line-height: 13px;
		color: rgba(255,255,255,0.5);
		text-decoration: underline;
	}

	.version {
		font-size: 9px;
		line-height: 12px;
		color: rgba(255,255,255,0.3);
	}
</style>
