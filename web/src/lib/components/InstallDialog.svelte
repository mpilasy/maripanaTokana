<!-- SPDX-FileCopyrightText: Orinasa Njarasoa -->
<!-- SPDX-License-Identifier: GPL-3.0-or-later -->
<script lang="ts">
	import { _ } from 'svelte-i18n';

	/**
	 * The two steps to add the app to the Home Screen, for browsers that give no install prompt:
	 * Safari on iPhone and iPad (`ios`), else the browser menu (Firefox and others).
	 */
	let { onclose, ios = true }: { onclose: () => void; ios?: boolean } = $props();
</script>

<!-- svelte-ignore a11y_click_events_have_key_events -->
<!-- svelte-ignore a11y_no_noninteractive_element_interactions -->
<div class="scrim" role="button" tabindex="0" onclick={onclose} onkeydown={(e) => { if (e.target === e.currentTarget && (e.key === 'Enter' || e.key === ' ')) { e.preventDefault(); onclose(); } }}>
	<div class="dialog" role="dialog" aria-modal="true" aria-labelledby="install-dialog-title" tabindex="-1" onclick={(e) => e.stopPropagation()}
		onkeydown={(e) => { if (e.key === 'Escape') onclose(); }}>
		<h2 id="install-dialog-title" class="title">{$_('help_q_install')}</h2>

		<ol class="steps">
			<li>
				{#if ios}
					<!-- The iOS share icon: a box with an arrow up -->
					<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 3v12M8 7l4-4 4 4M7 10H5v11h14V10h-2" /></svg>
					<span>{$_('install_step_share')}</span>
				{:else}
					<!-- The browser menu icon: three dots -->
					<svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="5" r="1.5" /><circle cx="12" cy="12" r="1.5" /><circle cx="12" cy="19" r="1.5" /></svg>
					<span>{$_('install_step_menu')}</span>
				{/if}
			</li>
			<li>
				<!-- Add to Home Screen: a box with a plus -->
				<svg viewBox="0 0 24 24" aria-hidden="true"><rect x="4" y="4" width="16" height="16" rx="4" /><path d="M12 8v8M8 12h8" /></svg>
				<span>{$_(ios ? 'install_step_add' : 'install_step_install')}</span>
			</li>
		</ol>

		<div class="actions">
			<button class="action primary" onclick={onclose}>{$_('cd_close')}</button>
		</div>
	</div>
</div>

<style>
	.scrim {
		position: fixed;
		inset: 0;
		background: rgba(0, 0, 0, 0.55);
		display: flex;
		align-items: center;
		justify-content: center;
		z-index: 1000;
		padding: 24px;
	}

	.dialog {
		background: #1A1565;
		border: 1px solid var(--hairline);
		border-radius: 24px;
		padding: 16px;
		width: 100%;
		max-width: 400px;
		display: flex;
		flex-direction: column;
		color: white;
	}

	.title {
		font-family: var(--font-display);
		font-size: 20px;
		font-weight: 700;
		margin: 0 0 12px 4px;
	}

	.steps {
		list-style: none;
		margin: 0;
		padding: 0;
		display: flex;
		flex-direction: column;
		gap: 8px;
	}

	.steps li {
		display: flex;
		align-items: center;
		gap: 12px;
		padding: 12px 14px;
		border-radius: 14px;
		background: var(--surface-tile);
		font-family: var(--font-body);
		font-size: 15px;
		line-height: 20px;
	}

	.steps svg {
		width: 24px;
		height: 24px;
		flex-shrink: 0;
		fill: none;
		stroke: #64B5F6;
		stroke-width: 1.8;
		stroke-linecap: round;
		stroke-linejoin: round;
	}

	.actions {
		display: flex;
		justify-content: flex-end;
		margin-top: 8px;
	}

	.action {
		padding: 10px 12px;
		border: none;
		border-radius: 20px;
		background: none;
		color: white;
		font-family: inherit;
		font-size: 14px;
		font-weight: 700;
		cursor: pointer;
	}

	.action:hover {
		background: var(--surface-tile);
	}
</style>
