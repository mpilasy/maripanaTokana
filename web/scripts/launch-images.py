#!/usr/bin/env python3
# SPDX-FileCopyrightText: Orinasa Njarasoa
# SPDX-License-Identifier: GPL-3.0-or-later
"""Makes the iOS launch images of the web app: the globe of the app icon on the app background.

iOS shows a launch image while a web app that is open from the Home Screen loads. iOS uses an
image only when its size is the same as the screen, so there is one image for each iPhone and
iPad screen. The script also writes src/lib/launchImages.json, which +layout.svelte reads to
write the <link rel="apple-touch-startup-image"> tags.

Run it from the web directory after a change to the icon or to the list of screens:
    python3 scripts/launch-images.py
"""
import json
from pathlib import Path

from PIL import Image, ImageDraw

WEB = Path(__file__).resolve().parent.parent
ICON = WEB / 'static/icons/icon-512.png'
OUT = WEB / 'static/launch'
LIST = WEB / 'src/lib/launchImages.json'

# The globe in icon-512.png: a circle with a black area around it
GLOBE_CENTER = (256, 256)
GLOBE_RADIUS = 230

# The colors of the .app-shell gradient in +layout.svelte, top to bottom
TOP = (0x0E, 0x0B, 0x3D)
BOTTOM = (0x1A, 0x15, 0x65)

# Screen sizes in CSS pixels (portrait) and the pixel ratio
IPHONES = [
	(440, 956, 3),  # 16 Pro Max, 17 Pro Max
	(420, 912, 3),  # Air
	(430, 932, 3),  # 14 Pro Max, 15 Plus, 15 Pro Max, 16 Plus
	(428, 926, 3),  # 12 Pro Max, 13 Pro Max, 14 Plus
	(414, 896, 3),  # XS Max, 11 Pro Max
	(414, 896, 2),  # XR, 11
	(414, 736, 3),  # 6 Plus to 8 Plus
	(402, 874, 3),  # 16 Pro, 17, 17 Pro
	(393, 852, 3),  # 14 Pro, 15, 15 Pro, 16
	(390, 844, 3),  # 12, 13, 14, 16e
	(375, 812, 3),  # X, XS, 11 Pro, 12 mini, 13 mini
	(375, 667, 2),  # 6 to 8, SE 2nd and 3rd generation
	(320, 568, 2),  # 5, SE 1st generation
]
IPADS = [
	(1032, 1376, 2),  # Pro 13-inch (M4)
	(1024, 1366, 2),  # Pro 12.9-inch, Air 13-inch
	(834, 1210, 2),  # Pro 11-inch (M4)
	(834, 1194, 2),  # Pro 11-inch
	(820, 1180, 2),  # Air 10.9-inch and 11-inch, iPad 10th generation
	(834, 1112, 2),  # Air 10.5-inch
	(810, 1080, 2),  # iPad 10.2-inch
	(768, 1024, 2),  # iPad 9.7-inch, mini 7.9-inch
	(744, 1133, 2),  # mini 8.3-inch
]

# Globe diameter in CSS pixels
PHONE_GLOBE = 128
TABLET_GLOBE = 160


def globe() -> Image.Image:
	"""The globe of the icon, with a transparent area in place of the black area."""
	icon = Image.open(ICON).convert('RGBA')
	scale = 4  # Draw the mask larger, then reduce it, for a smooth edge
	mask = Image.new('L', (icon.width * scale, icon.height * scale), 0)
	cx, cy = GLOBE_CENTER
	r = GLOBE_RADIUS
	ImageDraw.Draw(mask).ellipse([(cx - r) * scale, (cy - r) * scale, (cx + r) * scale, (cy + r) * scale], fill=255)
	icon.putalpha(mask.resize(icon.size, Image.LANCZOS))
	return icon.crop((cx - r, cy - r, cx + r, cy + r))


def launch_image(width: int, height: int, diameter: int, source: Image.Image) -> Image.Image:
	"""The vertical gradient of the app background, with the globe in the center."""
	column = Image.new('RGB', (1, height))
	for y in range(height):
		t = y / max(height - 1, 1)
		column.putpixel((0, y), tuple(round(a + (b - a) * t) for a, b in zip(TOP, BOTTOM)))
	image = column.resize((width, height), Image.NEAREST)
	sized = source.resize((diameter, diameter), Image.LANCZOS)
	image.paste(sized, ((width - diameter) // 2, (height - diameter) // 2), sized)
	return image


def main() -> None:
	source = globe()
	OUT.mkdir(exist_ok=True)
	for old in OUT.glob('*.png'):
		old.unlink()
	entries = []
	screens = [(s, PHONE_GLOBE, ['portrait', 'landscape']) for s in IPHONES] + [(s, TABLET_GLOBE, ['portrait', 'landscape']) for s in IPADS]
	for (w, h, ratio), globe_css, orientations in screens:
		for orientation in orientations:
			pw, ph = (w * ratio, h * ratio) if orientation == 'portrait' else (h * ratio, w * ratio)
			name = f'{pw}x{ph}.png'
			launch_image(pw, ph, globe_css * ratio, source).save(OUT / name, optimize=True)
			entries.append({'width': w, 'height': h, 'ratio': ratio, 'orientation': orientation, 'href': f'/launch/{name}'})
	LIST.write_text(json.dumps(entries, indent='\t') + '\n')
	print(f'{len(entries)} launch images in {OUT}')


if __name__ == '__main__':
	main()
