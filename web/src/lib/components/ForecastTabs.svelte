<!-- SPDX-FileCopyrightText: Orinasa Njarasoa -->
<!-- SPDX-License-Identifier: GPL-3.0-or-later -->
<script lang="ts">
	import { dragScroll } from '#lib/actions/dragScroll.js';

	/** A row of chips that picks what a forecast card shows. */
	let { tabs, selected, onSelect }: {
		tabs: { id: string; label: string }[];
		selected: string;
		onSelect: (id: string) => void;
	} = $props();
</script>

<div class="tabs" role="tablist" use:dragScroll>
	{#each tabs as tab (tab.id)}
		<button type="button" role="tab" class="tab" class:selected={tab.id === selected}
			aria-selected={tab.id === selected} onclick={() => onSelect(tab.id)}>
			{tab.label}
		</button>
	{/each}
</div>

<style>
	.tabs {
		display: flex;
		gap: 6px;
		overflow-x: auto;
		padding-bottom: 4px;
	}

	/* scrollbar-width: Chrome ignores ::-webkit-scrollbar when an ancestor sets scrollbar-color */
	.tabs {
		scrollbar-width: none;
	}

	.tabs::-webkit-scrollbar {
		display: none;
	}

	.tab {
		flex-shrink: 0;
		padding: 7px 14px;
		border: none;
		border-radius: 20px;
		background: var(--surface-tile);
		color: rgba(255, 255, 255, 0.7);
		font-family: inherit;
		font-size: 0.8125rem;
		font-weight: 500;
		cursor: pointer;
		white-space: nowrap;
	}

	.tab.selected {
		background: #9B8FFF;
		color: #1A0050;
		font-weight: 700;
	}
</style>
