<script lang="ts">
	interface Entry {
		color: string;
		label: string;
	}

	interface Props {
		entries: Entry[];
	}

	let { entries }: Props = $props();
</script>

<!--
	Compact color-key legend for charts whose data points are colored by a discrete tier (UV,
	AQI, ...) rather than a single fixed line color — without this, a viewer has no way to know
	what a given dot's color means. Wraps across lines instead of truncating, so it stays
	readable regardless of label length or locale.
-->
<div class="tier-legend">
	{#each entries as entry}
		<span class="legend-item">
			<span class="legend-dot" style="background: {entry.color};"></span>
			{entry.label}
		</span>
	{/each}
</div>

<style>
	.tier-legend {
		display: flex;
		flex-wrap: wrap;
		gap: 4px 10px;
		margin-bottom: 6px;
	}

	.legend-item {
		display: flex;
		align-items: center;
		gap: 4px;
		font-size: 9px;
		color: rgba(255, 255, 255, 0.65);
		white-space: nowrap;
	}

	.legend-dot {
		width: 7px;
		height: 7px;
		border-radius: 50%;
		flex-shrink: 0;
	}
</style>
