<script lang="ts">
	import { _ } from 'svelte-i18n';
	import { page } from '$app/stores';
</script>

<div class="error-container">
	<div class="error-content">
		<h1 class="error-status">{$page.status}</h1>
		<h2 class="error-title">{$_('error_title')}</h2>
		<button class="retry-button" onclick={() => location.reload()}>
			{$_('error_retry')}
		</button>
	</div>
</div>

<style>
	.error-container {
		display: flex;
		align-items: center;
		justify-content: center;
		height: 100dvh;
		padding: 1rem;
	}

	.error-content {
		text-align: center;
		max-width: 400px;
	}

	.error-status {
		font-size: 3rem;
		margin: 0 0 0.5rem 0;
		opacity: 0.7;
	}

	.error-title {
		font-size: 1.25rem;
		margin: 0 0 2rem 0;
	}

	.retry-button {
		padding: 0.75rem 1.5rem;
		font-size: 1rem;
		border: none;
		border-radius: 0.5rem;
		cursor: pointer;
		background-color: var(--color-primary, #0E0B3D);
		color: var(--color-text-primary, #ffffff);
	}

	.retry-button:active {
		opacity: 0.8;
	}
</style>
