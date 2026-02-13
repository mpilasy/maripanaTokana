<script lang="ts">
	import { isLoading } from 'svelte-i18n';
	import WeatherScreen from '$lib/components/WeatherScreen.svelte';
</script>

{#if $isLoading}
	<div class="loading-shell">
		<div class="spinner"></div>
	</div>
{:else}
	<WeatherScreen />
{/if}

<style>
	.loading-shell {
		display: flex;
		align-items: center;
		justify-content: center;
		height: 100dvh;
	}

	.spinner {
		width: 40px;
		height: 40px;
		border: 3px solid rgba(255, 255, 255, 0.2);
		border-top-color: white;
		border-radius: 50%;
		animation: spin 0.8s linear infinite;
	}

	@keyframes spin {
		to { transform: rotate(360deg); }
	}
</style>
