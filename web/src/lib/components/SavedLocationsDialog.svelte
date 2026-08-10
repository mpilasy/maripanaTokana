<script lang="ts">
	import { _ } from 'svelte-i18n';
	import { searchLocations, type SearchResult } from '$lib/api/geocodingSearch';
	import { showSavedLocationsDialog, setLocationOverride } from '$lib/stores/savedLocations';

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
		max-height: 200px;
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
