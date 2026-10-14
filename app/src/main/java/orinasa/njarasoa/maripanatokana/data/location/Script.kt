package orinasa.njarasoa.maripanatokana.data.location

/** True when every letter in [text] is Latin (digits, spaces and punctuation are ignored). */
fun isLatinScript(text: String): Boolean = text.codePoints().allMatch { cp ->
    !Character.isLetter(cp) || Character.UnicodeScript.of(cp) == Character.UnicodeScript.LATIN
}
