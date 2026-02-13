declare module 'html2canvas' {
	interface Options {
		backgroundColor?: string;
		scale?: number;
		useCORS?: boolean;
		logging?: boolean;
	}
	export default function html2canvas(element: HTMLElement, options?: Options): Promise<HTMLCanvasElement>;
}

declare module '$lib/i18n/locales/en.json' {
	const value: Record<string, any>;
	export default value;
}
declare module '$lib/i18n/locales/mg.json' {
	const value: Record<string, any>;
	export default value;
}
declare module '$lib/i18n/locales/ar.json' {
	const value: Record<string, any>;
	export default value;
}
declare module '$lib/i18n/locales/es.json' {
	const value: Record<string, any>;
	export default value;
}
declare module '$lib/i18n/locales/fr.json' {
	const value: Record<string, any>;
	export default value;
}
declare module '$lib/i18n/locales/hi.json' {
	const value: Record<string, any>;
	export default value;
}
declare module '$lib/i18n/locales/ne.json' {
	const value: Record<string, any>;
	export default value;
}
declare module '$lib/i18n/locales/zh.json' {
	const value: Record<string, any>;
	export default value;
}
