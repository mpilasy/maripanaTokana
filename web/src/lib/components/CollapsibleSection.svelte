<script lang="ts">
	import { slide } from 'svelte/transition';
	import { untrack } from 'svelte';
	import type { Snippet } from 'svelte';

	interface Props {
		title: string;
		expanded?: boolean;
		children: Snippet;
		onShare?: (el: HTMLElement) => void;
	}

	let { title, expanded = false, children, onShare }: Props = $props();
	let isExpanded = $state(untrack(() => expanded));
	let contentEl = $state<HTMLElement | null>(null);

	$effect(() => {
		isExpanded = expanded;
	});

	function handleShare(e: MouseEvent) {
		e.stopPropagation();
		if (contentEl && onShare) onShare(contentEl);
	}

    // Fallback to random ID suffix if unique ID isn't easily doable here
    let contentId = $derived(`content-${title.replace(/\s+/g, '-').toLowerCase()}-${Math.random().toString(36).substring(2, 9)}`);
</script>

<div class="collapsible-section">
	<div class="section-header">
		<button
			class="expand-btn title-btn"
			aria-expanded={isExpanded}
			aria-controls={contentId}
			onclick={() => isExpanded = !isExpanded}
		>
			<span class="section-title">{title}</span>
		</button>

		{#if isExpanded && onShare}
			<button class="share-btn" onclick={handleShare} aria-label="Share">
				<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
					<path d="M4 12v8a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-8"/>
					<polyline points="16 6 12 2 8 6"/>
					<line x1="12" y1="2" x2="12" y2="15"/>
				</svg>
			</button>
		{/if}

		<button
			class="expand-btn spacer-btn"
			aria-expanded={isExpanded}
			aria-controls={contentId}
			onclick={() => isExpanded = !isExpanded}
			tabindex="-1"
			aria-hidden="true"
		>
			<span class="spacer"></span>
			<span class="chevron" class:expanded={isExpanded}>&#9660;</span>
		</button>
	</div>

	{#if isExpanded}
		<div id={contentId} class="section-content" bind:this={contentEl} transition:slide={{ duration: 300 }}>
			{@render children()}
		</div>
	{/if}
</div>

<style>
	.collapsible-section {
		margin-bottom: 24px;
	}

	.section-header {
		display: flex;
		align-items: center;
		padding: 8px 0;
		gap: 8px;
	}

	.expand-btn {
		border: none;
		background: transparent;
		font: inherit;
		color: inherit;
		text-align: left;
		box-sizing: border-box;
		padding: 0;
		cursor: pointer;
		display: flex;
		align-items: center;
	}

	.expand-btn:focus-visible {
		outline: 2px solid rgba(255, 255, 255, 0.5);
		outline-offset: 4px;
		border-radius: 4px;
	}

	.title-btn {
		flex-shrink: 0;
	}

	.spacer-btn {
		flex-grow: 1;
		justify-content: flex-end;
		min-height: 28px;
	}

	.section-title {
		font-size: 20px;
		font-weight: 700;
		color: white;
	}

	.spacer {
		flex: 1;
	}

	.share-btn {
		background: rgba(255,255,255,0.1);
		border: none;
		border-radius: 50%;
		width: 28px;
		height: 28px;
		display: flex;
		align-items: center;
		justify-content: center;
		color: rgba(255,255,255,0.4);
		cursor: pointer;
		transition: background 0.2s, color 0.2s;
		flex-shrink: 0;
	}

	.share-btn:hover {
		background: rgba(255,255,255,0.2);
		color: rgba(255,255,255,0.7);
	}

	.share-btn:focus-visible {
		outline: 2px solid rgba(255, 255, 255, 0.5);
		outline-offset: 2px;
	}

	.chevron {
		color: rgba(255,255,255,0.7);
		font-size: 12px;
		transition: transform 0.3s ease;
		transform: rotate(-90deg);
	}

	.chevron.expanded {
		transform: rotate(0deg);
	}

	.section-content {
		padding-top: 8px;
		min-width: 0;
	}
</style>
