// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

/** SVG path through the points as a monotone cubic spline (no overshoot between points). */
export function monotonePath(pts: { x: number; y: number }[]): string {
	const n = pts.length;
	if (n < 2) return '';
	if (n === 2) return `M ${pts[0].x.toFixed(1)} ${pts[0].y.toFixed(1)} L ${pts[1].x.toFixed(1)} ${pts[1].y.toFixed(1)}`;

	const dx = new Float64Array(n - 1);
	const ms = new Float64Array(n - 1);
	for (let i = 0; i < n - 1; i++) {
		dx[i] = Math.max(0.0001, pts[i + 1].x - pts[i].x);
		ms[i] = (pts[i + 1].y - pts[i].y) / dx[i];
	}

	const ds = new Float64Array(n);
	ds[0] = ms[0];
	ds[n - 1] = ms[n - 2];
	for (let i = 1; i < n - 1; i++) {
		ds[i] = ms[i - 1] * ms[i] <= 0 ? 0 : (ms[i - 1] + ms[i]) / 2;
	}
	for (let i = 0; i < n - 1; i++) {
		if (ms[i] === 0) {
			ds[i] = 0;
			ds[i + 1] = 0;
		} else {
			const alpha = ds[i] / ms[i];
			const beta = ds[i + 1] / ms[i];
			const dist = alpha * alpha + beta * beta;
			if (dist > 9) {
				const tau = 3 / Math.sqrt(dist);
				ds[i] = tau * alpha * ms[i];
				ds[i + 1] = tau * beta * ms[i];
			}
		}
	}

	let d = `M ${pts[0].x.toFixed(1)} ${pts[0].y.toFixed(1)}`;
	for (let i = 0; i < n - 1; i++) {
		const h = dx[i];
		const p1 = pts[i];
		const p2 = pts[i + 1];
		d += ` C ${(p1.x + h / 3).toFixed(1)} ${(p1.y + (ds[i] * h) / 3).toFixed(1)}, ${(p2.x - h / 3).toFixed(1)} ${(p2.y - (ds[i + 1] * h) / 3).toFixed(1)}, ${p2.x.toFixed(1)} ${p2.y.toFixed(1)}`;
	}
	return d;
}
