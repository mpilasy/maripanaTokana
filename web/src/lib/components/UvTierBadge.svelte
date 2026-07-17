<script lang="ts">
	interface Props {
		uvIndex: number;
		label: string;
	}

	let { uvIndex, label }: Props = $props();

	function tierClass(uv: number): string {
		if (uv < 3) return 'uv-low';
		if (uv < 6) return 'uv-moderate';
		if (uv < 8) return 'uv-high';
		if (uv < 11) return 'uv-very-high';
		return 'uv-extreme';
	}
</script>

<span class="uv-badge {tierClass(uvIndex)}">{label}</span>

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
	.uv-very-high { background: #D8001D; color: #fff; }
	.uv-extreme { background: #6B49C8; color: #fff; }
</style>
