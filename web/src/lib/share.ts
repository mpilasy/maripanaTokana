const BG_COLOR = '#0E0B3D';
const PADDING = 32;
const GAP = 16;
const COPYRIGHT_FONT_SIZE = 10;

/**
 * Capture a DOM element (and optionally a header element), composite them
 * onto a branded canvas, and share as PNG via Web Share API (or download).
 */
export async function captureAndShare(
	headerEl: HTMLElement,
	contentEl: HTMLElement
): Promise<void> {
	const { default: html2canvas } = await import('html2canvas');

	const opts = { backgroundColor: BG_COLOR, scale: 2, useCORS: true, logging: false };

	// Capture both elements
	const [headerCapture, contentCapture] = await Promise.all([
		html2canvas(headerEl, opts),
		html2canvas(contentEl, opts),
	]);

	// Composite onto branded canvas
	const width = Math.max(headerCapture.width, contentCapture.width) + PADDING * 2;
	const copyrightHeight = COPYRIGHT_FONT_SIZE + PADDING;
	const height = PADDING + headerCapture.height + GAP + contentCapture.height + copyrightHeight;

	const canvas = document.createElement('canvas');
	canvas.width = width;
	canvas.height = height;
	const ctx = canvas.getContext('2d')!;

	// Background
	ctx.fillStyle = BG_COLOR;
	ctx.fillRect(0, 0, width, height);

	// Header (location + date — captured from DOM with actual fonts/styling)
	ctx.drawImage(headerCapture, PADDING, PADDING);

	// Content (the shared section)
	ctx.drawImage(contentCapture, PADDING, PADDING + headerCapture.height + GAP);

	// Copyright watermark
	ctx.fillStyle = 'rgba(255,255,255,0.25)';
	ctx.font = `${COPYRIGHT_FONT_SIZE * 2}px system-ui, sans-serif`;
	ctx.textAlign = 'center';
	ctx.textBaseline = 'top';
	ctx.fillText(
		'\u00A9 Orinasa Njarasoa \u2022 maripanaTokana',
		width / 2,
		height - copyrightHeight + PADDING / 4
	);

	// Export as PNG blob
	const blob = await new Promise<Blob>((resolve) =>
		canvas.toBlob((b) => resolve(b!), 'image/png')
	);
	const file = new File([blob], 'maripanatokana-weather.png', { type: 'image/png' });

	// Share or download
	if (navigator.share && navigator.canShare?.({ files: [file] })) {
		await navigator.share({ files: [file] });
	} else {
		const url = URL.createObjectURL(blob);
		const a = document.createElement('a');
		a.href = url;
		a.download = file.name;
		a.click();
		URL.revokeObjectURL(url);
	}
}
