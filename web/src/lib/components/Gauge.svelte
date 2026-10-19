<script lang="ts">
	/**
	 * A 270° arc, open at the bottom, with a dot at `fraction` (0-1). Filled up to the dot in `color`, or,
	 * with `segments`, drawn as equal coloured segments (one per level) so the whole scale always shows.
	 */
	let { fraction, color, size = 48, segments }: { fraction: number; color: string; size?: number; segments?: string[] } = $props();

	const r = 20;
	function point(deg: number): string {
		const a = (deg * Math.PI) / 180;
		return `${(24 + r * Math.cos(a)).toFixed(2)} ${(24 + r * Math.sin(a)).toFixed(2)}`;
	}
	// Angles clockwise from 3 o'clock: the arc runs from 135° (bottom left) to 405° (bottom right)
	function arc(from: number, to: number): string {
		return `M ${point(from)} A ${r} ${r} 0 ${to - from > 180 ? 1 : 0} 1 ${point(to)}`;
	}
	let f = $derived(Number.isFinite(fraction) ? Math.min(1, Math.max(0, fraction)) : 0);
	let end = $derived(point(135 + 270 * f).split(' '));
</script>

<svg width={size} height={size} viewBox="0 0 48 48" aria-hidden="true">
	{#if segments}
		{@const span = 270 / segments.length}
		{#each segments as c, i}
			<!-- A small gap keeps the levels apart; the reading's own level is drawn brightest -->
			<path d={arc(135 + i * span + 2, 135 + (i + 1) * span - 2)} fill="none" stroke={c} stroke-width="4" stroke-linecap="butt"
				stroke-opacity={i === Math.min(segments.length - 1, Math.floor(f * segments.length)) ? 1 : 0.4} />
		{/each}
	{:else}
		<path d={arc(135, 405)} fill="none" stroke="rgba(255,255,255,0.18)" stroke-width="4" stroke-linecap="round" />
		{#if f > 0}
			<path d={arc(135, 135 + 270 * f)} fill="none" stroke={color} stroke-width="4" stroke-linecap="round" />
		{/if}
	{/if}
	<!-- Where the value sits on the dial -->
	<circle cx={end[0]} cy={end[1]} r="4.5" fill="white" stroke={color} stroke-width="2.5" />
</svg>
