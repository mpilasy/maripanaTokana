<!-- SPDX-FileCopyrightText: Orinasa Njarasoa -->
<!-- SPDX-License-Identifier: GPL-3.0-or-later -->
<script lang="ts">
	/** A short bar in a level's colour, for cards that sit above a colour legend; the label is for screen readers and hover. */
	let { color, label }: { color: string; label: string } = $props();
</script>

<span class="level-bar" style:background={color} role="img" aria-label={label} title={label}></span>

<style>
	.level-bar {
		display: block;
		width: 28px;
		height: 5px;
		border-radius: 3px;
		margin: 4px auto 2px;
	}
</style>
