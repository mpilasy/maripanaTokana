declare module 'html2canvas' {
	interface Options {
		backgroundColor?: string;
		scale?: number;
		useCORS?: boolean;
		logging?: boolean;
	}
	export default function html2canvas(element: HTMLElement, options?: Options): Promise<HTMLCanvasElement>;
}
