export interface FontPairing {
	name: string;
	displayFamily: string;
	bodyFamily: string;
	bodyFontFeatures?: string;
	googleFontsUrl?: string;
}

export const fontPairings: FontPairing[] = [
	// 0: Default — system fonts
	{
		name: 'Default',
		displayFamily: 'system-ui, sans-serif',
		bodyFamily: 'system-ui, sans-serif',
	},
	// 1
	{
		name: 'Orbitron + Outfit',
		displayFamily: "'Orbitron', sans-serif",
		bodyFamily: "'Outfit', sans-serif",
		googleFontsUrl: 'https://fonts.googleapis.com/css2?family=Orbitron:wght@400;700&family=Outfit:wght@400;700&display=swap',
	},
	// 2
	{
		name: 'Rajdhani + Inter',
		displayFamily: "'Rajdhani', sans-serif",
		bodyFamily: "'Inter', sans-serif",
		googleFontsUrl: 'https://fonts.googleapis.com/css2?family=Rajdhani:wght@400;700&family=Inter:wght@400;700&display=swap',
	},
	// 3
	{
		name: 'Oxanium + Nunito',
		displayFamily: "'Oxanium', sans-serif",
		bodyFamily: "'Nunito', sans-serif",
		googleFontsUrl: 'https://fonts.googleapis.com/css2?family=Oxanium:wght@400;700&family=Nunito:wght@400;700&display=swap',
	},
	// 4
	{
		name: 'Space Grotesk + DM Sans',
		displayFamily: "'Space Grotesk', sans-serif",
		bodyFamily: "'DM Sans', sans-serif",
		googleFontsUrl: 'https://fonts.googleapis.com/css2?family=Space+Grotesk:wght@400;700&family=DM+Sans:wght@400;700&display=swap',
	},
	// 5
	{
		name: 'Sora + Source Sans',
		displayFamily: "'Sora', sans-serif",
		bodyFamily: "'Source Sans 3', sans-serif",
		googleFontsUrl: 'https://fonts.googleapis.com/css2?family=Sora:wght@400;700&family=Source+Sans+3:wght@400;700&display=swap',
	},
	// 6
	{
		name: 'Manrope + Rubik',
		displayFamily: "'Manrope', sans-serif",
		bodyFamily: "'Rubik', sans-serif",
		googleFontsUrl: 'https://fonts.googleapis.com/css2?family=Manrope:wght@400;700&family=Rubik:wght@400;700&display=swap',
	},
	// 7
	{
		name: 'Josefin Sans + Lato',
		displayFamily: "'Josefin Sans', sans-serif",
		bodyFamily: "'Lato', sans-serif",
		googleFontsUrl: 'https://fonts.googleapis.com/css2?family=Josefin+Sans:wght@400;700&family=Lato:wght@400;700&display=swap',
	},
	// 8
	{
		name: 'Cormorant + Fira Sans',
		displayFamily: "'Cormorant Garamond', serif",
		bodyFamily: "'Fira Sans', sans-serif",
		googleFontsUrl: 'https://fonts.googleapis.com/css2?family=Cormorant+Garamond:wght@400;700&family=Fira+Sans:wght@400;700&display=swap',
	},
	// 9
	{
		name: 'Playfair + Work Sans',
		displayFamily: "'Playfair Display', serif",
		bodyFamily: "'Work Sans', sans-serif",
		googleFontsUrl: 'https://fonts.googleapis.com/css2?family=Playfair+Display:wght@400;700&family=Work+Sans:wght@400;700&display=swap',
	},
	// 10
	{
		name: 'Quicksand + Nunito Sans',
		displayFamily: "'Quicksand', sans-serif",
		bodyFamily: "'Nunito Sans', sans-serif",
		googleFontsUrl: 'https://fonts.googleapis.com/css2?family=Quicksand:wght@400;700&family=Nunito+Sans:wght@400;700&display=swap',
	},
	// 11
	{
		name: 'Comfortaa + Karla',
		displayFamily: "'Comfortaa', sans-serif",
		bodyFamily: "'Karla', sans-serif",
		googleFontsUrl: 'https://fonts.googleapis.com/css2?family=Comfortaa:wght@400;700&family=Karla:wght@400;700&display=swap',
	},
	// 12
	{
		name: 'Baloo 2 + Poppins',
		displayFamily: "'Baloo 2', sans-serif",
		bodyFamily: "'Poppins', sans-serif",
		googleFontsUrl: 'https://fonts.googleapis.com/css2?family=Baloo+2:wght@400;700&family=Poppins:wght@400;700&display=swap',
	},
	// 13
	{
		name: 'Exo 2 + Barlow',
		displayFamily: "'Exo 2', sans-serif",
		bodyFamily: "'Barlow', sans-serif",
		googleFontsUrl: 'https://fonts.googleapis.com/css2?family=Exo+2:wght@400;700&family=Barlow:wght@400;700&display=swap',
	},
	// 14
	{
		name: 'Michroma + Saira',
		displayFamily: "'Michroma', sans-serif",
		bodyFamily: "'Saira', sans-serif",
		googleFontsUrl: 'https://fonts.googleapis.com/css2?family=Michroma&family=Saira:wght@400;700&display=swap',
	},
	// 15
	{
		name: 'Jost + Atkinson',
		displayFamily: "'Jost', sans-serif",
		bodyFamily: "'Atkinson Hyperlegible', sans-serif",
		googleFontsUrl: 'https://fonts.googleapis.com/css2?family=Jost:wght@400;700&family=Atkinson+Hyperlegible:wght@400;700&display=swap',
	},
	// 16
	{
		name: 'Roboto + Fira Code',
		displayFamily: 'system-ui, sans-serif',
		bodyFamily: "'Fira Code', monospace",
		bodyFontFeatures: '"tnum"',
		googleFontsUrl: 'https://fonts.googleapis.com/css2?family=Fira+Code:wght@400;700&display=swap',
	},
	// 17
	{
		name: 'Montserrat + Open Sans',
		displayFamily: "'Montserrat', sans-serif",
		bodyFamily: "'Open Sans', sans-serif",
		bodyFontFeatures: '"tnum"',
		googleFontsUrl: 'https://fonts.googleapis.com/css2?family=Montserrat:wght@400;700&family=Open+Sans:wght@400;700&display=swap',
	},
	// 18
	{
		name: 'Space Grotesk + Space Mono',
		displayFamily: "'Space Grotesk', sans-serif",
		bodyFamily: "'Space Mono', monospace",
		bodyFontFeatures: '"tnum"',
		googleFontsUrl: 'https://fonts.googleapis.com/css2?family=Space+Grotesk:wght@400;700&family=Space+Mono:wght@400;700&display=swap',
	},
	// 19
	{
		name: 'Plus Jakarta Sans + Inter',
		displayFamily: "'Plus Jakarta Sans', sans-serif",
		bodyFamily: "'Inter', sans-serif",
		bodyFontFeatures: '"tnum"',
		googleFontsUrl: 'https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;700&family=Inter:wght@400;700&display=swap',
	},
	// 20
	{
		name: 'Archivo + Archivo Narrow',
		displayFamily: "'Archivo', sans-serif",
		bodyFamily: "'Archivo Narrow', sans-serif",
		bodyFontFeatures: '"tnum"',
		googleFontsUrl: 'https://fonts.googleapis.com/css2?family=Archivo:wght@400;700&family=Archivo+Narrow:wght@400;700&display=swap',
	},
	// 21
	{
		name: 'Roboto + Lora',
		displayFamily: 'system-ui, sans-serif',
		bodyFamily: "'Lora', serif",
		googleFontsUrl: 'https://fonts.googleapis.com/css2?family=Lora:wght@400;700&display=swap',
	},
];
