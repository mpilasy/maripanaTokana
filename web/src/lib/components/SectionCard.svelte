<script lang="ts">
	import type { Snippet } from 'svelte';
	import { _ } from 'svelte-i18n';

	/** A card of weather data, always open, with a title and a share button. */
	/** `bare`: no surface of its own, for sections whose tiles are already cards. */
	let { title, children, onShare, bare = false }: { title: string; children: Snippet; onShare?: (el: HTMLElement) => void; bare?: boolean } = $props();
	let contentEl = $state<HTMLElement | null>(null);
</script>

<section class="section-card" class:bare>
	<div class="card-header">
		<h2 class="card-title">{title}</h2>
		{#if onShare}
			<button type="button" class="share-btn" aria-label={$_('cd_share')} onclick={() => contentEl && onShare?.(contentEl)}>
				<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
					<path d="M4 12v8a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-8"/>
					<polyline points="16 6 12 2 8 6"/>
					<line x1="12" y1="2" x2="12" y2="15"/>
				</svg>
			</button>
		{/if}
	</div>
	<div class="card-body" bind:this={contentEl}>
		{@render children()}
	</div>
</section>

<style>
	.section-card {
		background: var(--surface-section);
		border: 1px solid var(--hairline);
		border-radius: 20px;
		padding: 6px 12px 12px;
	}

	.section-card.bare {
		background: none;
		border-color: transparent;
		padding-inline: 0;
	}

	.card-header {
		display: flex;
		align-items: center;
		min-height: 44px;
		padding-inline-start: 4px;
	}

	.card-title {
		flex: 1;
		margin: 0;
		font-family: var(--font-body);
		font-size: 17px;
		font-weight: 700;
		color: white;
	}

	.share-btn {
		width: 36px;
		height: 36px;
		border: none;
		border-radius: 50%;
		background: none;
		color: rgba(255, 255, 255, 0.6);
		display: flex;
		align-items: center;
		justify-content: center;
		cursor: pointer;
	}

	.share-btn:hover {
		background: var(--surface-tile);
	}
</style>
