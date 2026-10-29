// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

/** Items a line in a share image: at most `max`, and the lines as even as possible. */
export function shareItemsPerLine(count: number, max: number): number {
	const lines = Math.ceil(count / max);
	return lines <= 1 ? Math.max(1, count) : Math.ceil(count / lines);
}
