#!/usr/bin/env python3
# SPDX-FileCopyrightText: Orinasa Njarasoa
# SPDX-License-Identifier: GPL-3.0-or-later
"""Writes the licence texts of the code, icons and fonts that the web app ships, to web/static/licenses/:
third-party.txt (all of them), one file for each package or icon set (<name>.txt, '@scope/x' as 'scope-x.txt'),
fonts.txt (all font licences) and meteocons.txt. The Licenses page (src/lib/credits.ts) shows them in the app.

The packages are the ones that the Vite build puts in the bundle (find them with
`npx vite build --sourcemap` and the node_modules paths in the .map files), plus the packages that
html2canvas has inside its own bundle. Run again after a dependency change:
    python3 scripts/web_third_party_licenses.py
"""
import json
import pathlib
import urllib.request

ROOT = pathlib.Path(__file__).resolve().parent.parent
WEB = ROOT / 'web'
OUT_DIR = WEB / 'static' / 'licenses'
OUT = OUT_DIR / 'third-party.txt'

PACKAGES = [
    'svelte', '@sveltejs/kit', '@sveltejs/adapter-node', 'devalue', 'esm-env', 'clsx', 'cookie',
    'svelte-i18n', 'deepmerge', 'intl-messageformat', '@formatjs/icu-messageformat-parser',
    '@formatjs/icu-skeleton-parser', '@formatjs/fast-memoize', 'tslib',
    'html2canvas', 'css-line-break', 'text-segmentation', 'base64-arraybuffer', 'utrie',
]

# Code and icons that are not npm packages: file name, name, what the app uses, licence, licence text URL
OTHERS = [
    ('suncalc', 'SunCalc', 'the moon formulas in src/lib/domain/moon.ts', 'BSD-2-Clause',
     'https://raw.githubusercontent.com/mourner/suncalc/master/LICENSE'),
    ('feather', 'Feather icons', 'the refresh and share icons', 'MIT',
     'https://raw.githubusercontent.com/feathericons/feather/main/LICENSE'),
    ('apache-2.0', 'Material Design icons', 'the interface icons (search, settings, place, heart and others)', 'Apache-2.0',
     'https://raw.githubusercontent.com/google/material-design-icons/master/LICENSE'),
]


def license_text(pkg_dir: pathlib.Path) -> str:
    for f in sorted(pkg_dir.iterdir()):
        if f.is_file() and f.name.lower().startswith(('license', 'licence', 'copying')):
            return f.read_text(encoding='utf-8').strip()
    raise SystemExit(f'No licence file in {pkg_dir}')


def write(path: pathlib.Path, text: str) -> None:
    # With a byte order mark: the server sends text/plain with no charset, and the mark makes the browser read UTF-8
    path.write_text(text.strip() + '\n', encoding='utf-8-sig')


def main() -> None:
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    parts = [
        'Third-party software and icons in the maripànaTokana web app',
        '',
        'The web app is free software under the GNU General Public License, version 3 or later.',
        'It includes the software and icons below, each under its own licence.',
    ]
    for name in PACKAGES:
        pkg_dir = WEB / 'node_modules' / name
        meta = json.loads((pkg_dir / 'package.json').read_text(encoding='utf-8'))
        text = license_text(pkg_dir)
        parts += ['', f'==== {name} {meta["version"]} ({meta.get("license")}) ====', '', text]
        write(OUT_DIR / (name.lstrip('@').replace('/', '-') + '.txt'), text)
    for file, name, use, spdx, url in OTHERS:
        with urllib.request.urlopen(url, timeout=30) as r:
            text = r.read().decode('utf-8').strip()
        parts += ['', f'==== {name} ({spdx}): {use} ====', '', text]
        write(OUT_DIR / f'{file}.txt', text)
    write(OUT, '\n'.join(parts))
    fonts = sorted((WEB / 'static' / 'fonts').glob('*/LICENSE*'))
    write(OUT_DIR / 'fonts.txt', '\n\n'.join(f'==== {f.parent.name} ====\n\n{f.read_text(encoding="utf-8-sig").strip()}' for f in fonts))
    write(OUT_DIR / 'meteocons.txt', (WEB / 'static' / 'icons' / 'weather' / 'LICENSE.txt').read_text(encoding='utf-8-sig'))
    print(f'Wrote {OUT_DIR.relative_to(ROOT)}/: {len(list(OUT_DIR.glob("*.txt")))} files, {len(fonts)} font licences')


if __name__ == '__main__':
    main()
