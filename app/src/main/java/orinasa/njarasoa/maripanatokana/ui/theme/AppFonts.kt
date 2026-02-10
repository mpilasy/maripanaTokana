package orinasa.njarasoa.maripanatokana.ui.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import orinasa.njarasoa.maripanatokana.R

// Existing fonts
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

// New fonts
private val SpaceGrotesk = FontFamily(
    Font(R.font.space_grotesk_regular, FontWeight.Normal),
    Font(R.font.space_grotesk_bold, FontWeight.Bold),
)
private val DmSans = FontFamily(
    Font(R.font.dm_sans_regular, FontWeight.Normal),
    Font(R.font.dm_sans_bold, FontWeight.Bold),
)
private val Sora = FontFamily(
    Font(R.font.sora_regular, FontWeight.Normal),
    Font(R.font.sora_bold, FontWeight.Bold),
)
private val SourceSans3 = FontFamily(
    Font(R.font.source_sans_3_regular, FontWeight.Normal),
    Font(R.font.source_sans_3_bold, FontWeight.Bold),
)
private val Manrope = FontFamily(
    Font(R.font.manrope_regular, FontWeight.Normal),
    Font(R.font.manrope_bold, FontWeight.Bold),
)
private val Rubik = FontFamily(
    Font(R.font.rubik_regular, FontWeight.Normal),
    Font(R.font.rubik_bold, FontWeight.Bold),
)
private val JosefinSans = FontFamily(
    Font(R.font.josefin_sans_regular, FontWeight.Normal),
    Font(R.font.josefin_sans_bold, FontWeight.Bold),
)
private val Lato = FontFamily(
    Font(R.font.lato_regular, FontWeight.Normal),
    Font(R.font.lato_bold, FontWeight.Bold),
)
private val CormorantGaramond = FontFamily(
    Font(R.font.cormorant_garamond_regular, FontWeight.Normal),
    Font(R.font.cormorant_garamond_bold, FontWeight.Bold),
)
private val FiraSans = FontFamily(
    Font(R.font.fira_sans_regular, FontWeight.Normal),
    Font(R.font.fira_sans_bold, FontWeight.Bold),
)
private val PlayfairDisplay = FontFamily(
    Font(R.font.playfair_display_regular, FontWeight.Normal),
    Font(R.font.playfair_display_bold, FontWeight.Bold),
)
private val WorkSans = FontFamily(
    Font(R.font.work_sans_regular, FontWeight.Normal),
    Font(R.font.work_sans_bold, FontWeight.Bold),
)
private val Quicksand = FontFamily(
    Font(R.font.quicksand_regular, FontWeight.Normal),
    Font(R.font.quicksand_bold, FontWeight.Bold),
)
private val NunitoSans = FontFamily(
    Font(R.font.nunito_sans_regular, FontWeight.Normal),
    Font(R.font.nunito_sans_bold, FontWeight.Bold),
)
private val Comfortaa = FontFamily(
    Font(R.font.comfortaa_regular, FontWeight.Normal),
    Font(R.font.comfortaa_bold, FontWeight.Bold),
)
private val Karla = FontFamily(
    Font(R.font.karla_regular, FontWeight.Normal),
    Font(R.font.karla_bold, FontWeight.Bold),
)
private val Baloo2 = FontFamily(
    Font(R.font.baloo_2_regular, FontWeight.Normal),
    Font(R.font.baloo_2_bold, FontWeight.Bold),
)
private val Poppins = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_bold, FontWeight.Bold),
)
private val Exo2 = FontFamily(
    Font(R.font.exo_2_regular, FontWeight.Normal),
    Font(R.font.exo_2_bold, FontWeight.Bold),
)
private val Barlow = FontFamily(
    Font(R.font.barlow_regular, FontWeight.Normal),
    Font(R.font.barlow_bold, FontWeight.Bold),
)
private val Michroma = FontFamily(
    Font(R.font.michroma_regular, FontWeight.Normal),
)
private val Saira = FontFamily(
    Font(R.font.saira_regular, FontWeight.Normal),
    Font(R.font.saira_bold, FontWeight.Bold),
)
private val Jost = FontFamily(
    Font(R.font.jost_regular, FontWeight.Normal),
    Font(R.font.jost_bold, FontWeight.Bold),
)
private val AtkinsonHyperlegible = FontFamily(
    Font(R.font.atkinson_hyperlegible_regular, FontWeight.Normal),
    Font(R.font.atkinson_hyperlegible_bold, FontWeight.Bold),
)

// New pairings fonts
private val FiraCode = FontFamily(
    Font(R.font.fira_code, FontWeight.Normal),
    Font(R.font.fira_code, FontWeight.Bold),
)
private val Montserrat = FontFamily(
    Font(R.font.montserrat, FontWeight.Normal),
    Font(R.font.montserrat, FontWeight.Bold),
)
private val OpenSans = FontFamily(
    Font(R.font.open_sans, FontWeight.Normal),
    Font(R.font.open_sans, FontWeight.Bold),
)
private val SpaceMono = FontFamily(
    Font(R.font.space_mono_regular, FontWeight.Normal),
    Font(R.font.space_mono_bold, FontWeight.Bold),
)
private val PlusJakartaSans = FontFamily(
    Font(R.font.plus_jakarta_sans, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans, FontWeight.Bold),
)
private val Archivo = FontFamily(
    Font(R.font.archivo, FontWeight.Normal),
    Font(R.font.archivo, FontWeight.Bold),
)
private val ArchivoNarrow = FontFamily(
    Font(R.font.archivo_narrow, FontWeight.Normal),
    Font(R.font.archivo_narrow, FontWeight.Bold),
)

data class FontPairing(
    val name: String,
    val display: FontFamily,
    val body: FontFamily,
    /** OpenType font feature settings for the body font (e.g. "tnum" for tabular numerals). */
    val bodyFontFeatures: String? = null,
)

val fontPairings = listOf(
    FontPairing("Default", FontFamily.Default, FontFamily.Default),
    // Existing
    FontPairing("Orbitron + Outfit", Orbitron, Outfit),
    FontPairing("Rajdhani + Inter", Rajdhani, Inter),
    FontPairing("Oxanium + Nunito", Oxanium, Nunito),
    // Sleek/Modern
    FontPairing("Space Grotesk + DM Sans", SpaceGrotesk, DmSans),
    FontPairing("Sora + Source Sans", Sora, SourceSans3),
    FontPairing("Manrope + Rubik", Manrope, Rubik),
    // Elegant/Refined
    FontPairing("Josefin Sans + Lato", JosefinSans, Lato),
    FontPairing("Cormorant + Fira Sans", CormorantGaramond, FiraSans),
    FontPairing("Playfair + Work Sans", PlayfairDisplay, WorkSans),
    // Warm/Friendly
    FontPairing("Quicksand + Nunito Sans", Quicksand, NunitoSans),
    FontPairing("Comfortaa + Karla", Comfortaa, Karla),
    FontPairing("Baloo 2 + Poppins", Baloo2, Poppins),
    // Weather-fitting/Atmospheric
    FontPairing("Exo 2 + Barlow", Exo2, Barlow),
    FontPairing("Michroma + Saira", Michroma, Saira),
    FontPairing("Jost + Atkinson", Jost, AtkinsonHyperlegible),
    // New pairings – data fonts configured with tabular numerals (tnum)
    FontPairing("Roboto + Fira Code", FontFamily.Default, FiraCode, bodyFontFeatures = "tnum"),
    FontPairing("Montserrat + Open Sans", Montserrat, OpenSans, bodyFontFeatures = "tnum"),
    FontPairing("Space Grotesk + Space Mono", SpaceGrotesk, SpaceMono, bodyFontFeatures = "tnum"),
    FontPairing("Plus Jakarta Sans + Inter", PlusJakartaSans, Inter, bodyFontFeatures = "tnum"),
    FontPairing("Archivo + Archivo Narrow", Archivo, ArchivoNarrow, bodyFontFeatures = "tnum"),
)

val LocalDisplayFont = compositionLocalOf<FontFamily> { FontFamily.Default }
val LocalBodyFont = compositionLocalOf<FontFamily> { FontFamily.Default }
val LocalBodyFontFeatures = compositionLocalOf<String?> { null }
