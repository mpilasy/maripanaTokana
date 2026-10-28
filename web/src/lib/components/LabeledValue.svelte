<!-- SPDX-FileCopyrightText: Orinasa Njarasoa -->
<!-- SPDX-License-Identifier: GPL-3.0-or-later -->
<script lang="ts">
	/** A small label with its value under it, at the start or the end of a card's bottom row (like sunrise and sunset). */
	let { label, value, end = false }: { label: string; value: string; end?: boolean } = $props();
</script>

<span class="labeled" class:end><span class="label">{label}</span>{value}</span>

<style>
	.labeled {
		display: flex;
		flex-direction: column;
		font-family: var(--font-display);
		font-size: 15px;
		font-weight: 700;
		color: white;
		font-feature-settings: var(--font-features);
		text-align: start;
		/* A value stays on one line; text beside it wraps instead */
		white-space: nowrap;
		flex-shrink: 0;
	}

	.labeled.end {
		text-align: end;
		align-items: flex-end;
	}

	.label {
		font-family: var(--font-body, inherit);
		font-size: 11px;
		font-weight: 400;
		color: rgba(255, 255, 255, 0.5);
	}
</style>
