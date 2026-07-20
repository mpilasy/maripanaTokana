<script lang="ts">
	import { uvTier } from '$lib/domain/uv';

	interface Props {
		uvIndex: number;
		label: string;
	}

	let { uvIndex, label }: Props = $props();
</script>

<span class="uv-badge uv-{uvTier(uvIndex)}">{label}</span>

<style>
	/* EPA UV Index color scale (epa.gov/sunsafety/uv-index-scale-0): Green/Yellow/Orange/Red/Violet.
	   Thresholds match the Low/Moderate/High/Very High/Extreme bands used for uv_labels elsewhere. */
	.uv-badge {
		display: inline-block;
		padding: 2px 8px;
		border-radius: 999px;
		font-size: 12px;
		font-weight: 600;
		line-height: 1.4;
	}

	.uv-low { background: #299501; color: #fff; }
	.uv-moderate { background: #F7E401; color: #1a1a1a; }
	.uv-high { background: #F85900; color: #fff; }
	.uv-veryHigh { background: #D8001D; color: #fff; }
	.uv-extreme { background: #6B49C8; color: #fff; }
</style>
