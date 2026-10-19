<script lang="ts">
	/** The day as an arc from sunrise to sunset with the sun at the current time (epoch seconds). */
	let { sunrise, sunset, now }: { sunrise: number; sunset: number; now: number } = $props();

	// Half-ellipse from (8,44) to (112,44) peaking at y=8
	const W = 120, H = 52, CX = 60, RX = 52, RY = 36, BASE = 44;
	let t = $derived(sunset > sunrise ? (now - sunrise) / (sunset - sunrise) : -1);
	let up = $derived(t >= 0 && t <= 1);
	let sun = $derived.by(() => {
		const a = Math.PI * (1 - Math.min(1, Math.max(0, t)));
		return { x: CX + RX * Math.cos(a), y: BASE - RY * Math.sin(a) };
	});
	// The part of the arc already travelled today
	let travelled = $derived(`M ${CX - RX} ${BASE} A ${RX} ${RY} 0 0 1 ${sun.x.toFixed(2)} ${sun.y.toFixed(2)}`);
</script>

<svg width="100%" viewBox="0 0 {W} {H}" aria-hidden="true" class="sun-arc">
	<line x1="2" y1={BASE} x2={W - 2} y2={BASE} stroke="rgba(255,255,255,0.15)" stroke-width="1" />
	<path d="M {CX - RX} {BASE} A {RX} {RY} 0 0 1 {CX + RX} {BASE}" fill="none" stroke="rgba(255,255,255,0.25)" stroke-width="1.5" stroke-dasharray="3 3" />
	{#if up}
		<path d={travelled} fill="none" stroke="#FFB74D" stroke-width="2" stroke-linecap="round" />
		<circle cx={sun.x} cy={sun.y} r="5" fill="#FFD54F" />
	{/if}
</svg>

<style>
	.sun-arc {
		display: block;
		max-height: 64px;
	}
</style>
