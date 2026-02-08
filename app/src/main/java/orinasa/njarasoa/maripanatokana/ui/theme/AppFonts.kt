package orinasa.njarasoa.maripanatokana.ui.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import orinasa.njarasoa.maripanatokana.R

private val Orbitron = FontFamily(
    Font(R.font.orbitron_regular, FontWeight.Normal),
    Font(R.font.orbitron_bold, FontWeight.Bold),
)

private val Outfit = FontFamily(
    Font(R.font.outfit_regular, FontWeight.Normal),
    Font(R.font.outfit_bold, FontWeight.Bold),
)

private val Rajdhani = FontFamily(
    Font(R.font.rajdhani_regular, FontWeight.Normal),
    Font(R.font.rajdhani_bold, FontWeight.Bold),
)

private val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_bold, FontWeight.Bold),
)

private val Oxanium = FontFamily(
    Font(R.font.oxanium_regular, FontWeight.Normal),
    Font(R.font.oxanium_bold, FontWeight.Bold),
)

private val Nunito = FontFamily(
    Font(R.font.nunito_regular, FontWeight.Normal),
    Font(R.font.nunito_bold, FontWeight.Bold),
)

data class FontPairing(
    val name: String,
    val display: FontFamily,
    val body: FontFamily,
)

val fontPairings = listOf(
    FontPairing(
        name = "Default",
        display = FontFamily.Default,
        body = FontFamily.Default,
    ),
    FontPairing(
        name = "Orbitron + Outfit",
        display = Orbitron,
        body = Outfit,
    ),
    FontPairing(
        name = "Rajdhani + Inter",
        display = Rajdhani,
        body = Inter,
    ),
    FontPairing(
        name = "Oxanium + Nunito",
        display = Oxanium,
        body = Nunito,
    ),
)

val LocalDisplayFont = compositionLocalOf<FontFamily> { FontFamily.Default }
val LocalBodyFont = compositionLocalOf<FontFamily> { FontFamily.Default }
