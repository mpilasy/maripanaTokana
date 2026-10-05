export interface FontPairing {
	name: string;
	displayFamily: string;
	bodyFamily: string;
	bodyFontFeatures?: string;
	fontCssUrl?: string;
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
		fontCssUrl: '/fonts/orbitron-outfit.css',
	},
	// 2
	{
		name: 'Rajdhani + Inter',
		displayFamily: "'Rajdhani', sans-serif",
		bodyFamily: "'Inter', sans-serif",
		fontCssUrl: '/fonts/rajdhani-inter.css',
	},
	// 3
	{
		name: 'Oxanium + Nunito',
		displayFamily: "'Oxanium', sans-serif",
		bodyFamily: "'Nunito', sans-serif",
		fontCssUrl: '/fonts/oxanium-nunito.css',
	},
	// 4
	{
		name: 'Space Grotesk + DM Sans',
		displayFamily: "'Space Grotesk', sans-serif",
		bodyFamily: "'DM Sans', sans-serif",
		fontCssUrl: '/fonts/space-grotesk-dm-sans.css',
	},
	// 5
	{
		name: 'Sora + Source Sans',
		displayFamily: "'Sora', sans-serif",
		bodyFamily: "'Source Sans 3', sans-serif",
		fontCssUrl: '/fonts/sora-source-sans.css',
	},
	// 6
	{
		name: 'Manrope + Rubik',
		displayFamily: "'Manrope', sans-serif",
		bodyFamily: "'Rubik', sans-serif",
		fontCssUrl: '/fonts/manrope-rubik.css',
	},
	// 7
	{
		name: 'Josefin Sans + Lato',
		displayFamily: "'Josefin Sans', sans-serif",
		bodyFamily: "'Lato', sans-serif",
		fontCssUrl: '/fonts/josefin-sans-lato.css',
	},
	// 8
	{
		name: 'Cormorant + Fira Sans',
		displayFamily: "'Cormorant Garamond', serif",
		bodyFamily: "'Fira Sans', sans-serif",
		fontCssUrl: '/fonts/cormorant-fira-sans.css',
	},
	// 9
	{
		name: 'Playfair + Work Sans',
		displayFamily: "'Playfair Display', serif",
		bodyFamily: "'Work Sans', sans-serif",
		fontCssUrl: '/fonts/playfair-work-sans.css',
	},
	// 10
	{
		name: 'Quicksand + Nunito Sans',
		displayFamily: "'Quicksand', sans-serif",
		bodyFamily: "'Nunito Sans', sans-serif",
		fontCssUrl: '/fonts/quicksand-nunito-sans.css',
	},
	// 11
	{
		name: 'Comfortaa + Karla',
		displayFamily: "'Comfortaa', sans-serif",
		bodyFamily: "'Karla', sans-serif",
		fontCssUrl: '/fonts/comfortaa-karla.css',
	},
	// 12
	{
		name: 'Baloo 2 + Poppins',
		displayFamily: "'Baloo 2', sans-serif",
		bodyFamily: "'Poppins', sans-serif",
		fontCssUrl: '/fonts/baloo-2-poppins.css',
	},
	// 13
	{
		name: 'Exo 2 + Barlow',
		displayFamily: "'Exo 2', sans-serif",
		bodyFamily: "'Barlow', sans-serif",
		fontCssUrl: '/fonts/exo-2-barlow.css',
	},
	// 14
	{
		name: 'Michroma + Saira',
		displayFamily: "'Michroma', sans-serif",
		bodyFamily: "'Saira', sans-serif",
		fontCssUrl: '/fonts/michroma-saira.css',
	},
	// 15
	{
		name: 'Jost + Atkinson',
		displayFamily: "'Jost', sans-serif",
		bodyFamily: "'Atkinson Hyperlegible', sans-serif",
		fontCssUrl: '/fonts/jost-atkinson.css',
	},
	// 16
	{
		name: 'Roboto + Fira Code',
		displayFamily: 'system-ui, sans-serif',
		bodyFamily: "'Fira Code', monospace",
		bodyFontFeatures: '"tnum"',
		fontCssUrl: '/fonts/roboto-fira-code.css',
	},
	// 17
	{
		name: 'Montserrat + Open Sans',
		displayFamily: "'Montserrat', sans-serif",
		bodyFamily: "'Open Sans', sans-serif",
		bodyFontFeatures: '"tnum"',
		fontCssUrl: '/fonts/montserrat-open-sans.css',
	},
	// 18
	{
		name: 'Space Grotesk + Space Mono',
		displayFamily: "'Space Grotesk', sans-serif",
		bodyFamily: "'Space Mono', monospace",
		bodyFontFeatures: '"tnum"',
		fontCssUrl: '/fonts/space-grotesk-space-mono.css',
	},
	// 19
	{
		name: 'Plus Jakarta Sans + Inter',
		displayFamily: "'Plus Jakarta Sans', sans-serif",
		bodyFamily: "'Inter', sans-serif",
		bodyFontFeatures: '"tnum"',
		fontCssUrl: '/fonts/plus-jakarta-sans-inter.css',
	},
	// 20
	{
		name: 'Archivo + Archivo Narrow',
		displayFamily: "'Archivo', sans-serif",
		bodyFamily: "'Archivo Narrow', sans-serif",
		bodyFontFeatures: '"tnum"',
		fontCssUrl: '/fonts/archivo-archivo-narrow.css',
	},
	// 21
	{
		name: 'Roboto + Lora',
		displayFamily: 'system-ui, sans-serif',
		bodyFamily: "'Lora', serif",
		fontCssUrl: '/fonts/roboto-lora.css',
	},
];
