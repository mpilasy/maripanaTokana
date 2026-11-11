// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

package orinasa.njarasoa.maripanatokana.ui.common

import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * A latitude as whole degrees and a direction ("17° S"), isolated left to right, so the degrees and the letter keep
 * their order in an Arabic sentence.
 */
internal fun latitudeText(lat: Double, directions: Array<String>, localizeDigits: (String) -> String): String {
    val r = abs(lat).roundToInt()
    return "\u2066" + localizeDigits(if (r == 0) "0°" else "$r° ${directions[if (lat > 0) 0 else 8]}") + "\u2069"
}
