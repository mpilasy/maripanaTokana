<!-- SPDX-FileCopyrightText: Orinasa Njarasoa -->
<!-- SPDX-License-Identifier: GPL-3.0-or-later -->
<script lang="ts">
	import { moonLitPath } from '#lib/utils/moonPath.js';

	/** The moon's lit part for `phase` (0 = new, 0.5 = full); `southern` mirrors it, as it is seen south of the equator. */
	let { phase, southern, size = 44 }: { phase: number; southern: boolean; size?: number } = $props();
</script>

<svg width={size} height={size} viewBox="0 0 48 48" aria-hidden="true">
	<circle cx="24" cy="24" r="24" fill="#3A3570" />
	<path d={moonLitPath(phase, southern, 24, 24, 24)} fill="#F2EBD3" />
</svg>
