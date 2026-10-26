<!-- SPDX-FileCopyrightText: Orinasa Njarasoa -->
<!-- SPDX-License-Identifier: GPL-3.0-or-later -->
<script lang="ts">
	/** A compass ring whose needle points to where the wind comes from (degrees, 0 = north). */
	let { degrees, size = 48 }: { degrees: number; size?: number } = $props();
</script>

<svg width={size} height={size} viewBox="0 0 48 48" aria-hidden="true">
	<circle cx="24" cy="24" r="20" fill="none" stroke="rgba(255,255,255,0.12)" stroke-width="2" />
	<!-- North tick -->
	<line x1="24" y1="2" x2="24" y2="8" stroke="rgba(255,255,255,0.5)" stroke-width="2" stroke-linecap="round" />
	<g transform="rotate({degrees} 24 24)">
		<path d="M24 8 L29 26 L24 22 L19 26 Z" fill="#64B5F6" />
		<path d="M24 22 L29 26 L24 40 L19 26 Z" fill="rgba(255,255,255,0.25)" />
	</g>
</svg>
