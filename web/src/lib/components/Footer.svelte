<script lang="ts">
	import { _ } from 'svelte-i18n';

	interface Props {
		// No props needed after moving controls
	}

	let { }: Props = $props();
</script>

<footer class="footer" dir="ltr">
	<div class="footer-credits">
		<span class="credit-text">
			{$_('credits_weather_data')}
			<a href="https://open-meteo.com" target="_blank" rel="noopener">Open-Meteo</a>
		</span>
		<span class="version">v1.0.3</span>
	</div>
</footer>

<style>
	.footer {
		display: flex;
		align-items: center;
		padding: 12px 0;
		padding-bottom: max(12px, env(safe-area-inset-bottom));
		flex-shrink: 0;
	}

	.footer-credits {
		flex: 1;
		text-align: center;
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: 2px;
	}

	.credit-text {
		font-size: 9px;
		line-height: 11px;
		color: rgba(255,255,255,0.3);
	}

	.credit-text a {
		color: rgba(255,255,255,0.5);
		text-decoration: underline;
	}

	.version {
		font-size: 9px;
		line-height: 11px;
		color: rgba(255,255,255,0.25);
	}
</style>
