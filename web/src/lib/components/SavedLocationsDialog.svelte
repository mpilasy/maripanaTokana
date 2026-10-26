<!-- SPDX-FileCopyrightText: Orinasa Njarasoa -->
<!-- SPDX-License-Identifier: GPL-3.0-or-later -->
<script lang="ts">
	import { _ } from 'svelte-i18n';
	import { searchLocations, type SearchResult } from '$lib/api/geocodingSearch';
	import { showSavedLocationsDialog, setLocationOverride, addSavedLocation } from '$lib/stores/savedLocations';
	import { weatherState } from '$lib/stores/weather';
	import { get } from 'svelte/store';

	let query = $state('');
	let results = $state<SearchResult[]>([]);
	let timeout: ReturnType<typeof setTimeout>;
	let inputEl = $state<HTMLInputElement | null>(null);

	$effect(() => { inputEl?.focus(); });

	function handleInput() {
		clearTimeout(timeout);
		timeout = setTimeout(async () => {
			if (query.trim().length < 2) {
				results = [];
				return;
			}
			results = await searchLocations(query);
		}, 500);
	}

	function selectResult(result: SearchResult) {
		// With weather on screen a pick is a preview; without (no location) it is saved, so it stays
		if (get(weatherState).kind !== 'success') {
			addSavedLocation(result);
			return;
		}
		const subtext = [result.admin1, result.country].filter(Boolean).join(', ');
		setLocationOverride(result.latitude, result.longitude, result.name, subtext);
	}

	function close() {
		showSavedLocationsDialog.set(false);
	}
</script>

<!-- svelte-ignore a11y_click_events_have_key_events -->
<!-- svelte-ignore a11y_no_noninteractive_element_interactions -->
<div class="scrim" role="button" tabindex="0" onclick={close} onkeydown={(e) => { if (e.target === e.currentTarget && (e.key === 'Enter' || e.key === ' ')) { e.preventDefault(); close(); } }}>
	<div class="dialog" role="dialog" aria-modal="true" aria-labelledby="locations-dialog-title" tabindex="-1" onclick={(e) => e.stopPropagation()}>
		<h2 id="locations-dialog-title" class="title">{$_('locations_title')}</h2>

		<div class="search-field">
			<svg class="search-icon" viewBox="0 0 24 24" width="22" height="22" fill="currentColor" aria-hidden="true">
				<path d="M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5 16 5.91 13.09 3 9.5 3S3 5.91 3 9.5 5.91 16 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z"/>
			</svg>
			<input
				type="text"
				bind:this={inputEl}
				bind:value={query}
				oninput={handleInput}
				placeholder={$_('locations_search_hint')}
				class="search-input"
			/>
			{#if query}
				<button class="clear-btn" onclick={() => { query = ''; results = []; inputEl?.focus(); }} aria-label="Clear text">
					<svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor"><path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"/></svg>
				</button>
			{/if}
		</div>

		{#if results.length > 0}
			<div class="results">
				{#each results as result}
					<button class="result-item" onclick={() => selectResult(result)}>
						<!-- Map for a region or country, pin for a town or city -->
						<svg class="result-icon" viewBox="0 0 24 24" width="20" height="20" fill="currentColor" aria-hidden="true">
							{#if result.isArea}
								<path d="M20.5 3l-.16.03L15 5.1 9 3 3.36 4.9c-.21.07-.36.25-.36.48V20.5c0 .28.22.5.5.5l.16-.03L9 18.9l6 2.1 5.64-1.9c.21-.07.36-.25.36-.48V3.5c0-.28-.22-.5-.5-.5zM15 19l-6-2.11V5l6 2.11V19z"/>
							{:else}
								<path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5c-1.38 0-2.5-1.12-2.5-2.5s1.12-2.5 2.5-2.5 2.5 1.12 2.5 2.5-1.12 2.5-2.5 2.5z"/>
							{/if}
						</svg>
						<span class="result-text">
							<span class="result-name">{result.name}</span>
							<span class="result-meta">{result.displayName}</span>
						</span>
					</button>
				{/each}
			</div>
		{/if}
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
		max-height: 80vh;
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

	.search-field {
		display: flex;
		align-items: center;
		gap: 8px;
		background: var(--surface-tile);
		border-radius: 14px;
		padding: 0 4px 0 14px;
		flex-shrink: 0;
	}

	.search-icon {
		color: rgba(255, 255, 255, 0.6);
		flex-shrink: 0;
	}

	.search-input {
		flex: 1;
		min-width: 0;
		padding: 14px 0;
		border: none;
		background: transparent;
		color: white;
		font-size: 16px;
		font-family: inherit;
	}

	.search-input:focus {
		outline: none;
	}

	.search-input::placeholder {
		color: rgba(255, 255, 255, 0.5);
	}

	.clear-btn {
		width: 40px;
		height: 40px;
		border: none;
		border-radius: 50%;
		background: none;
		color: rgba(255, 255, 255, 0.7);
		display: flex;
		align-items: center;
		justify-content: center;
		cursor: pointer;
	}

	.results {
		margin-top: 8px;
		overflow-y: auto;
		max-height: 320px;
	}

	.result-item {
		width: 100%;
		display: flex;
		align-items: center;
		gap: 12px;
		text-align: start;
		background: transparent;
		border: none;
		border-radius: 12px;
		padding: 10px 8px;
		color: white;
		cursor: pointer;
		font-family: inherit;
	}

	.result-item + .result-item {
		border-top: 1px solid var(--hairline);
		border-radius: 0;
	}

	.result-item:hover {
		background: var(--surface-tile);
	}

	.result-icon {
		color: rgba(255, 255, 255, 0.5);
		flex-shrink: 0;
	}

	.result-text {
		display: flex;
		flex-direction: column;
		min-width: 0;
	}

	.result-name {
		font-family: var(--font-body);
		font-size: 16px;
		font-weight: 500;
	}

	.result-meta {
		font-size: 13px;
		color: rgba(255, 255, 255, 0.5);
		white-space: nowrap;
		overflow: hidden;
		text-overflow: ellipsis;
	}
</style>
