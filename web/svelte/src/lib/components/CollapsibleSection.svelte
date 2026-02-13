<script lang="ts">
	import { slide } from 'svelte/transition';
	import type { Snippet } from 'svelte';

	interface Props {
		title: string;
		expanded?: boolean;
		children: Snippet;
		onShare?: (el: HTMLElement) => void;
	}

	let { title, expanded = false, children, onShare }: Props = $props();
	let isExpanded = $state(expanded);
	let contentEl = $state<HTMLElement | null>(null);

	function handleShare(e: MouseEvent) {
		e.stopPropagation();
		if (contentEl && onShare) onShare(contentEl);
	}
</script>

<div class="collapsible-section">
	<!-- svelte-ignore a11y_click_events_have_key_events -->
	<!-- svelte-ignore a11y_no_static_element_interactions -->
	<div class="section-header" onclick={() => isExpanded = !isExpanded}>
		<span class="section-title">{title}</span>
		{#if isExpanded && onShare}
			<button class="share-btn" onclick={handleShare} aria-label="Share">
				<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
					<path d="M4 12v8a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-8"/>
					<polyline points="16 6 12 2 8 6"/>
					<line x1="12" y1="2" x2="12" y2="15"/>
				</svg>
			</button>
		{/if}
		<span class="spacer"></span>
		<span class="chevron" class:expanded={isExpanded}>&#9660;</span>
	</div>

	{#if isExpanded}
		<div class="section-content" bind:this={contentEl} transition:slide={{ duration: 300 }}>
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
		cursor: pointer;
		padding: 8px 0;
		user-select: none;
		gap: 8px;
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
