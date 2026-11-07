<!-- SPDX-FileCopyrightText: Orinasa Njarasoa -->
<!-- SPDX-License-Identifier: GPL-3.0-or-later -->
<script lang="ts">
	import { uvTier } from '#lib/domain/uv.js';

	interface Props {
		uvIndex: number;
		label: string;
		compact?: boolean;
	}

	let { uvIndex, label, compact = false }: Props = $props();
</script>

<span class="uv-badge uv-{uvTier(uvIndex)}" class:compact>{label}</span>

<style>
	/* EPA UV Index color scale (epa.gov/sunsafety/uv-index-scale-0): Green/Yellow/Orange/Red/Violet.
	   Thresholds match the Low/Moderate/High/Very High/Extreme bands used for uv_labels elsewhere. */
	/* One line always; in a narrow forecast card the label is cut short rather than wrapped */
	.uv-badge {
		/* A tint of the level's colour, with the text in a lighter shade of it */
		background: color-mix(in srgb, var(--tier) 22%, transparent);
		color: color-mix(in srgb, var(--tier) 50%, white);
		display: inline-block;
		max-width: 100%;
		box-sizing: border-box;
		white-space: nowrap;
		overflow: hidden;
		text-overflow: ellipsis;
		padding: 2px 8px;
		border-radius: 999px;
		font-size: 0.75rem;
		font-weight: 600;
		line-height: 1.4;
	}

	/* Small, as the air quality badge in the hero */
	.uv-badge.compact {
		font-size: 0.625rem;
		padding: 1px 6px;
	}

	.uv-low { --tier: #299501; }
	.uv-moderate { --tier: #F7E401; }
	.uv-high { --tier: #F85900; }
	.uv-veryHigh { --tier: #D8001D; }
	.uv-extreme { --tier: #6B49C8; }
</style>
