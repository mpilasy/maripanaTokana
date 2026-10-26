// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

/** True when every letter in the text is Latin (digits, spaces and punctuation are ignored). Mirrors Android's Script.kt. */
export function isLatinScript(text: string): boolean {
	return [...text].every((ch) => !/\p{L}/u.test(ch) || /\p{Script=Latin}/u.test(ch));
}
