<script lang="ts">
	import { untrack } from 'svelte';
	import { _ } from 'svelte-i18n';
	import { displayName, renameSavedLocation, type SavedLocation } from '$lib/stores/savedLocations';

	/** Names a favorite. Saving a blank name, or Reset, goes back to the place name. */
	let { location, onclose }: { location: SavedLocation; onclose: () => void } = $props();

	// Starts from the current name; the field is the user's from then on
	let text = $state(untrack(() => displayName(location)));
	let inputEl = $state<HTMLInputElement | null>(null);

	$effect(() => {
		inputEl?.focus();
		inputEl?.select();
	});

	function save(label: string | null) {
		renameSavedLocation(location.id, label);
		onclose();
	}
</script>

<!-- svelte-ignore a11y_click_events_have_key_events -->
<!-- svelte-ignore a11y_no_noninteractive_element_interactions -->
<div class="scrim" role="button" tabindex="0" onclick={onclose} onkeydown={(e) => { if (e.target === e.currentTarget && (e.key === 'Enter' || e.key === ' ')) { e.preventDefault(); onclose(); } }}>
	<div class="dialog" role="dialog" aria-modal="true" aria-labelledby="rename-dialog-title" tabindex="-1" onclick={(e) => e.stopPropagation()}
		onkeydown={(e) => { if (e.key === 'Escape') onclose(); }}>
		<h2 id="rename-dialog-title" class="title">{$_('rename_title')}</h2>

		<input
			type="text"
			bind:this={inputEl}
			bind:value={text}
			placeholder={location.name}
			autocapitalize="words"
			class="name-input"
			onkeydown={(e) => { if (e.key === 'Enter') save(text); }}
		/>

		<div class="actions">
			{#if location.label}
				<button class="action reset" onclick={() => save(null)}>{$_('rename_reset')}</button>
			{/if}
			<button class="action" onclick={onclose}>{$_('rename_cancel')}</button>
			<button class="action primary" onclick={() => save(text)}>{$_('rename_save')}</button>
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

	.name-input {
		padding: 14px;
		border: none;
		border-radius: 14px;
		background: var(--surface-tile);
		color: white;
		font-size: 16px;
		font-family: inherit;
	}

	.name-input:focus {
		outline: none;
	}

	.name-input::placeholder {
		color: rgba(255, 255, 255, 0.5);
	}

	.actions {
		display: flex;
		justify-content: flex-end;
		gap: 4px;
		margin-top: 8px;
	}

	.action {
		padding: 10px 12px;
		border: none;
		border-radius: 20px;
		background: none;
		color: rgba(255, 255, 255, 0.7);
		font-family: inherit;
		font-size: 14px;
		font-weight: 500;
		cursor: pointer;
	}

	.action:hover {
		background: var(--surface-tile);
	}

	.action.reset {
		margin-inline-end: auto;
	}

	.action.primary {
		color: white;
		font-weight: 700;
	}
</style>
