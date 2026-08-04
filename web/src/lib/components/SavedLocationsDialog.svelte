<script lang="ts">
	import { _ } from 'svelte-i18n';
	import { searchLocations, type SearchResult } from '$lib/api/geocodingSearch';
	import {
		savedLocations, activeLocationId, showSavedLocationsDialog,
		switchToLocation, removeSavedLocation, addSavedLocation,
	} from '$lib/stores/savedLocations';

	let query = $state('');
	let results = $state<SearchResult[]>([]);
	let timeout: ReturnType<typeof setTimeout>;

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
		addSavedLocation(result);
		query = '';
		results = [];
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

		<button class="location-row" class:active={$activeLocationId === null} onclick={() => switchToLocation(null)}>
			<svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor">
				<path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5c-1.38 0-2.5-1.12-2.5-2.5s1.12-2.5 2.5-2.5 2.5 1.12 2.5 2.5-1.12 2.5-2.5 2.5z"/>
			</svg>
			<span>{$_('locations_current')}</span>
		</button>

		{#if $savedLocations.length > 0}
			<div class="divider"></div>
			{#each $savedLocations as loc (loc.id)}
				<div class="location-row saved">
					<button class="location-row-main" class:active={$activeLocationId === loc.id} onclick={() => switchToLocation(loc.id)}>
						<span class="loc-name">{loc.name}</span>
						{#if loc.subtext}
							<span class="loc-subtext">{loc.subtext}</span>
						{/if}
					</button>
					<button class="remove-button" onclick={() => removeSavedLocation(loc.id)} aria-label={$_('locations_remove')}>
						<svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor">
							<path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"/>
						</svg>
					</button>
				</div>
			{/each}
		{/if}

		<div class="divider"></div>

		<input
			type="text"
			bind:value={query}
			oninput={handleInput}
			placeholder={$_('locations_search_hint')}
			class="search-input"
		/>

		<div class="results">
			{#each results as result}
				<button class="result-item" onclick={() => selectResult(result)}>
					<div class="result-name">{result.name}</div>
					<div class="result-meta">{result.displayName}</div>
				</button>
			{/each}
		</div>
	</div>
</div>

<style>
	.scrim {
		position: fixed;
		inset: 0;
		background: rgba(0, 0, 0, 0.5);
		display: flex;
		align-items: center;
		justify-content: center;
		z-index: 1000;
		padding: 24px;
	}

	.dialog {
		background: #0E0B3D;
		border-radius: 16px;
		padding: 24px;
		width: 100%;
		max-width: 400px;
		max-height: 80vh;
		overflow-y: auto;
		color: white;
	}

	.title {
		font-size: 20px;
		font-weight: 600;
		margin: 0 0 12px 0;
	}

	.divider {
		height: 1px;
		background: rgba(255, 255, 255, 0.15);
		margin: 8px 0;
	}

	.location-row {
		display: flex;
		align-items: center;
		gap: 8px;
		width: 100%;
		background: transparent;
		border: none;
		color: rgba(255, 255, 255, 0.8);
		font-size: 16px;
		text-align: left;
		cursor: pointer;
		padding: 10px 4px;
		border-radius: 8px;
	}

	.location-row:hover {
		background: rgba(255, 255, 255, 0.05);
	}

	.location-row.active {
		color: white;
		font-weight: 700;
	}

	.location-row.saved {
		padding: 0;
		display: flex;
		align-items: center;
	}

	.location-row-main {
		flex: 1;
		display: flex;
		flex-direction: column;
		align-items: flex-start;
		background: transparent;
		border: none;
		color: rgba(255, 255, 255, 0.8);
		text-align: left;
		cursor: pointer;
		padding: 8px 4px;
		border-radius: 8px;
	}

	.location-row-main:hover {
		background: rgba(255, 255, 255, 0.05);
	}

	.location-row-main.active {
		color: white;
		font-weight: 700;
	}

	.loc-name {
		font-size: 16px;
	}

	.loc-subtext {
		font-size: 13px;
		color: rgba(255, 255, 255, 0.5);
	}

	.remove-button {
		background: transparent;
		border: none;
		color: rgba(255, 255, 255, 0.6);
		cursor: pointer;
		padding: 8px;
		border-radius: 50%;
		display: flex;
		align-items: center;
		justify-content: center;
	}

	.remove-button:hover {
		background: rgba(255, 255, 255, 0.1);
		color: white;
	}

	.search-input {
		width: 100%;
		padding: 12px 16px;
		border-radius: 8px;
		border: 1px solid rgba(255, 255, 255, 0.2);
		background: transparent;
		color: white;
		font-size: 16px;
		box-sizing: border-box;
		margin-bottom: 8px;
	}

	.search-input:focus {
		outline: none;
		border-color: rgba(255, 255, 255, 0.5);
	}

	.results {
		max-height: 160px;
		overflow-y: auto;
	}

	.result-item {
		width: 100%;
		text-align: left;
		background: transparent;
		border: none;
		padding: 12px 8px;
		color: white;
		cursor: pointer;
		border-bottom: 1px solid rgba(255, 255, 255, 0.1);
	}

	.result-item:hover {
		background: rgba(255, 255, 255, 0.05);
	}

	.result-name {
		font-size: 16px;
	}

	.result-meta {
		font-size: 14px;
		color: rgba(255, 255, 255, 0.5);
	}
</style>
