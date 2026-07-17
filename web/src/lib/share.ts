const BG_COLOR = '#0E0B3D';
const PADDING = 32;
const GAP = 16;
const COPYRIGHT_FONT_SIZE = 10;

const textEncoder = new TextEncoder();

const CRC_TABLE = (() => {
	const table = new Uint32Array(256);
	for (let n = 0; n < 256; n++) {
		let c = n;
		for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1;
		table[n] = c >>> 0;
	}
	return table;
})();

function crc32(bytes: Uint8Array): number {
	let crc = 0xffffffff;
	for (let i = 0; i < bytes.length; i++) {
		crc = CRC_TABLE[(crc ^ bytes[i]) & 0xff] ^ (crc >>> 8);
	}
	return (crc ^ 0xffffffff) >>> 0;
}

function formatExifDate(date: Date): string {
	const pad = (n: number) => String(n).padStart(2, '0');
	return `${date.getFullYear()}:${pad(date.getMonth() + 1)}:${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
}

/**
 * Minimal little-endian TIFF/EXIF blob carrying just DateTime, DateTimeOriginal
 * and DateTimeDigitized — no GPS/location data. This is the same payload format
 * PNG's "eXIf" chunk expects (no "Exif\0\0"/APP1 wrapper needed, unlike JPEG).
 */
function buildExifTiff(date: Date): Uint8Array {
	const dateBytes = textEncoder.encode(formatExifDate(date) + '\0'); // 20 bytes incl. null terminator

	const ifd0Offset = 8;
	const ifd0Size = 2 + 12 * 2 + 4; // entry count + 2 entries + next-IFD offset
	const exifIfdOffset = ifd0Offset + ifd0Size;
	const exifIfdSize = 2 + 12 * 2 + 4;
	const dataOffset = exifIfdOffset + exifIfdSize;

	const dateTimeOffset = dataOffset;
	const dateTimeOriginalOffset = dateTimeOffset + dateBytes.length;
	const dateTimeDigitizedOffset = dateTimeOriginalOffset + dateBytes.length;
	const totalSize = dateTimeDigitizedOffset + dateBytes.length;

	const buf = new ArrayBuffer(totalSize);
	const view = new DataView(buf);
	const bytes = new Uint8Array(buf);
	const le = true;

	// TIFF header: 'II' (little-endian), magic 42, offset to IFD0
	bytes[0] = 0x49;
	bytes[1] = 0x49;
	view.setUint16(2, 42, le);
	view.setUint32(4, ifd0Offset, le);

	// IFD0: DateTime + pointer to Exif SubIFD
	let off = ifd0Offset;
	view.setUint16(off, 2, le); off += 2;

	view.setUint16(off, 0x0132, le); off += 2; // DateTime
	view.setUint16(off, 2, le); off += 2; // type ASCII
	view.setUint32(off, dateBytes.length, le); off += 4;
	view.setUint32(off, dateTimeOffset, le); off += 4;

	view.setUint16(off, 0x8769, le); off += 2; // ExifIFDPointer
	view.setUint16(off, 4, le); off += 2; // type LONG
	view.setUint32(off, 1, le); off += 4;
	view.setUint32(off, exifIfdOffset, le); off += 4;

	view.setUint32(off, 0, le); off += 4; // next IFD offset

	// Exif SubIFD: DateTimeOriginal + DateTimeDigitized
	off = exifIfdOffset;
	view.setUint16(off, 2, le); off += 2;

	view.setUint16(off, 0x9003, le); off += 2; // DateTimeOriginal
	view.setUint16(off, 2, le); off += 2;
	view.setUint32(off, dateBytes.length, le); off += 4;
	view.setUint32(off, dateTimeOriginalOffset, le); off += 4;

	view.setUint16(off, 0x9004, le); off += 2; // DateTimeDigitized
	view.setUint16(off, 2, le); off += 2;
	view.setUint32(off, dateBytes.length, le); off += 4;
	view.setUint32(off, dateTimeDigitizedOffset, le); off += 4;

	view.setUint32(off, 0, le); off += 4; // next IFD offset

	bytes.set(dateBytes, dateTimeOffset);
	bytes.set(dateBytes, dateTimeOriginalOffset);
	bytes.set(dateBytes, dateTimeDigitizedOffset);

	return bytes;
}

/**
 * Insert an "eXIf" chunk (PNG's native EXIF carrier, standardized 2017) right
 * after IHDR, per spec requiring it precede the first IDAT chunk.
 */
function addExifDateTime(pngBytes: Uint8Array, date: Date): Uint8Array {
	const exifData = buildExifTiff(date);
	const typeAndData = new Uint8Array(4 + exifData.length);
	typeAndData.set(textEncoder.encode('eXIf'), 0);
	typeAndData.set(exifData, 4);
	const crc = crc32(typeAndData);

	const chunk = new Uint8Array(4 + typeAndData.length + 4);
	const chunkView = new DataView(chunk.buffer);
	chunkView.setUint32(0, exifData.length, false);
	chunk.set(typeAndData, 4);
	chunkView.setUint32(4 + typeAndData.length, crc, false);

	// PNG signature (8 bytes) + IHDR chunk (4 len + 4 type + 13 data + 4 crc = 25) = 33.
	const insertAt = 33;
	const result = new Uint8Array(pngBytes.length + chunk.length);
	result.set(pngBytes.subarray(0, insertAt), 0);
	result.set(chunk, insertAt);
	result.set(pngBytes.subarray(insertAt), insertAt + chunk.length);
	return result;
}

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
		'© Orinasa Njarasoa • maripanaTokana',
		width / 2,
		height - copyrightHeight + PADDING / 4
	);

	// Export as PNG blob, then stamp the share time into EXIF (no GPS/location)
	const rawBlob = await new Promise<Blob>((resolve) =>
		canvas.toBlob((b) => resolve(b!), 'image/png')
	);
	const pngBytes = new Uint8Array(await rawBlob.arrayBuffer());
	const withExif = addExifDateTime(pngBytes, new Date());
	const blob = new Blob([withExif.buffer as ArrayBuffer], { type: 'image/png' });
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
