/**
 * 16+ font pairings matching the Android Kotlin app.
 *
 * In React Native, custom fonts are loaded via the project's native configuration.
 * Each pairing specifies display (headings) and body (content) font family names.
 * The `bodyFontFeatures` field maps to fontVariant in RN styles (limited support).
 *
 * Note: Font files must be linked in the native project. Without the actual .ttf files
 * linked, the app will fall back to system fonts. The pairing names and structure are
 * preserved for parity with the Android app.
 */

export interface FontPairing {
  name: string;
  display: string; // Font family name for display/headings
  body: string;    // Font family name for body text
  bodyFontFeatures?: string; // e.g. 'tabular-nums' for tabular numerals
}

export const fontPairings: FontPairing[] = [
  {name: 'Default', display: 'System', body: 'System'},
  {name: 'Orbitron + Outfit', display: 'Orbitron', body: 'Outfit'},
  {name: 'Rajdhani + Inter', display: 'Rajdhani', body: 'Inter'},
  {name: 'Oxanium + Nunito', display: 'Oxanium', body: 'Nunito'},
  {name: 'Space Grotesk + DM Sans', display: 'SpaceGrotesk', body: 'DMSans'},
  {name: 'Sora + Source Sans', display: 'Sora', body: 'SourceSans3'},
  {name: 'Manrope + Rubik', display: 'Manrope', body: 'Rubik'},
  {name: 'Josefin Sans + Lato', display: 'JosefinSans', body: 'Lato'},
  {name: 'Cormorant + Fira Sans', display: 'CormorantGaramond', body: 'FiraSans'},
  {name: 'Playfair + Work Sans', display: 'PlayfairDisplay', body: 'WorkSans'},
  {name: 'Quicksand + Nunito Sans', display: 'Quicksand', body: 'NunitoSans'},
  {name: 'Comfortaa + Karla', display: 'Comfortaa', body: 'Karla'},
  {name: 'Baloo 2 + Poppins', display: 'Baloo2', body: 'Poppins'},
  {name: 'Exo 2 + Barlow', display: 'Exo2', body: 'Barlow'},
  {name: 'Michroma + Saira', display: 'Michroma', body: 'Saira'},
  {name: 'Jost + Atkinson', display: 'Jost', body: 'AtkinsonHyperlegible'},
  {name: 'Roboto + Fira Code', display: 'System', body: 'FiraCode', bodyFontFeatures: 'tabular-nums'},
  {name: 'Montserrat + Open Sans', display: 'Montserrat', body: 'OpenSans', bodyFontFeatures: 'tabular-nums'},
  {name: 'Space Grotesk + Space Mono', display: 'SpaceGrotesk', body: 'SpaceMono', bodyFontFeatures: 'tabular-nums'},
  {name: 'Plus Jakarta Sans + Inter', display: 'PlusJakartaSans', body: 'Inter', bodyFontFeatures: 'tabular-nums'},
  {name: 'Archivo + Archivo Narrow', display: 'Archivo', body: 'ArchivoNarrow', bodyFontFeatures: 'tabular-nums'},
  {name: 'Roboto + Lora', display: 'System', body: 'Lora'},
];
