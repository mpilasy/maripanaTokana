<!-- SPDX-FileCopyrightText: Orinasa Njarasoa -->
<!-- SPDX-License-Identifier: GPL-3.0-or-later -->
<script lang="ts">
	import { _ } from 'svelte-i18n';

	/** The QR code of a link of the About page, large, for a phone to scan. A click on the address opens it. */
	let { label, address, url, qr, onclose }: { label: string; address: string; url: string; qr: string; onclose: () => void } = $props();
</script>

<!-- svelte-ignore a11y_click_events_have_key_events -->
<!-- svelte-ignore a11y_no_noninteractive_element_interactions -->
<div class="scrim" role="button" tabindex="0" onclick={onclose} onkeydown={(e) => { if (e.target === e.currentTarget && (e.key === 'Enter' || e.key === ' ')) { e.preventDefault(); onclose(); } }}>
	<div class="dialog" role="dialog" aria-modal="true" aria-labelledby="qr-dialog-title" tabindex="-1" onclick={(e) => e.stopPropagation()}
		onkeydown={(e) => { if (e.key === 'Escape') onclose(); }}>
		<h2 id="qr-dialog-title" class="title">{label}</h2>
		<img class="qr" src={qr} alt="" width="240" height="240" />
		<a class="address" dir="ltr" href={url} target="_blank" rel="noopener noreferrer">{address}</a>
		<div class="actions">
			<button class="action" onclick={onclose}>{$_('cd_close')}</button>
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
		max-width: 320px;
		display: flex;
		flex-direction: column;
		align-items: center;
		color: white;
	}

	.title {
		font-family: var(--font-display);
		font-size: 1.25rem;
		font-weight: 700;
		margin: 0 0 12px;
	}

	.qr {
		display: block;
		width: 240px;
		height: 240px;
		/* No smoothing: the small image keeps sharp squares when it grows */
		image-rendering: pixelated;
	}

	.address {
		padding: 12px;
		font-size: 0.875rem;
		color: #64B5F6;
		overflow-wrap: anywhere;
		text-align: center;
	}

	.actions {
		align-self: stretch;
		display: flex;
		justify-content: flex-end;
	}

	.action {
		padding: 10px 12px;
		border: none;
		border-radius: 20px;
		background: none;
		color: white;
		font-family: inherit;
		font-size: 0.875rem;
		font-weight: 700;
		cursor: pointer;
	}

	.action:hover {
		background: var(--surface-tile);
	}
</style>
