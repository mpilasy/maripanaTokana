#!/usr/bin/env python3
# SPDX-FileCopyrightText: Orinasa Njarasoa
# SPDX-License-Identifier: GPL-3.0-or-later
"""Takes the 6 phone store screenshots on the Pixel emulator, for one or more app languages.
Set up the emulator first (docs/SCREENSHOTS_README.md, steps 1-5). The app must be open on the main page.
Usage: python3 scripts/capture_store_screenshots.py OUT_DIR lang [lang ...]   (lang: ar en es fr hi mg ne zh)
Writes OUT_DIR/<lang>/1.png to 6.png, uncompressed:
1 main card, 2 hourly forecast, 3 chart view, 4 current conditions, 5 sky and air, 6 sea and activities.
The serial is $ANDROID_SERIAL, else emulator-5556."""
import json, os, re, subprocess, sys, time, pathlib, xml.etree.ElementTree as ET

SERIAL = os.environ.get('ANDROID_SERIAL', 'emulator-5556')
REPO = pathlib.Path(__file__).resolve().parent.parent
TARGET = 540  # a card title's top while the header is big; the big header ends lower in some languages (about 494 in Arabic)
FINAL = 370   # a card title's top under the folded header, in the screenshot


def adb(*a, out=False):
    r = subprocess.run(['adb', '-s', SERIAL, *a], capture_output=True)
    return r.stdout if out else None


def dump():
    for _ in range(5):
        adb('shell', 'uiautomator', 'dump', '/sdcard/u.xml')
        x = adb('shell', 'cat', '/sdcard/u.xml', out=True).decode('utf-8', 'replace')
        if x.startswith('<?xml'):
            return ET.fromstring(x)
        time.sleep(1)
    raise SystemExit('no dump')


def bounds(n):
    a = list(map(int, re.findall(r'\d+', n.get('bounds'))))
    return a  # x1 y1 x2 y2


def find(root, text):
    hits = [n for n in root.iter('node') if n.get('text') == text]
    return [bounds(n) for n in hits]


def strings(lang):
    return json.load(open(REPO / f'shared/i18n/locales/{lang}.json', encoding='utf-8'))


def swipe(y1, y2, ms=1500, x=540):
    adb('shell', 'input', 'swipe', str(x), str(y1), str(x), str(y2), str(ms))


def to_top():
    for _ in range(6):
        swipe(700, 2100, 120)
    time.sleep(0.5)


def to_bottom():
    for _ in range(8):
        swipe(2000, 500, 120)


def settle():
    time.sleep(4.2)  # the header folds 3 s after the last scroll


def bring(title):
    """Scrolls until the card title's top is at TARGET (a title below the screen is first found by scrolling)."""
    went_top = False
    for _ in range(24):
        b = [h for h in find(dump(), title) if h[1] > 150]
        if not b:
            if not went_top:
                to_top(); went_top = True
            else:
                swipe(1900, 1000, 1500); time.sleep(0.6)
            continue
        top = b[0][1]
        d = top - TARGET
        if abs(d) <= 12:
            break
        step = max(-1500, min(1500, d + (19 if d > 0 else -19)))
        start = 1950 if step > 0 else 500
        swipe(start, start - step, 1500)
        time.sleep(0.8)
    else:
        raise SystemExit(f'could not place {title}')
    for _ in range(6):
        settle()
        b = [h for h in find(dump(), title) if h[1] > 150]
        d = b[0][1] - FINAL
        if abs(d) <= 10:
            return True
        start = 1950 if d > 0 else 500
        swipe(start, start - (d + (19 if d > 0 else -19)), 1500)
    raise SystemExit(f'could not finish {title}')


def place(title):
    """bring(), and once more from the top when it fails (it sometimes does after the chart view closes)."""
    try:
        bring(title)
    except SystemExit:
        time.sleep(2)
        to_top()
        bring(title)


def shot(path):
    path.write_bytes(adb('exec-out', 'screencap', '-p', out=True))


def flag_node(root):
    for n in root.iter('node'):
        b = bounds(n)
        if n.get('clickable') == 'true' and b[0] > 800 and b[1] > 2150:
            return b
    return None


def set_lang(lang):
    want = strings(lang)['section_hourly_forecast']
    for _ in range(10):
        to_top()
        r = dump()
        if find(r, want):
            return
        swipe(1200, 1150, 200)  # a small scroll shows the footer
        time.sleep(0.5)
        b = flag_node(dump())
        if not b:
            continue
        adb('shell', 'input', 'tap', str((b[0] + b[2]) // 2), str((b[1] + b[3]) // 2))
        time.sleep(2.5)
    raise SystemExit(f'could not set {lang}')


def take(out, lang):
    s = strings(lang)
    d = pathlib.Path(out) / lang
    d.mkdir(parents=True, exist_ok=True)
    set_lang(lang)
    to_top(); settle(); shot(d / '1.png')
    place(s['section_hourly_forecast']); shot(d / '2.png')
    # the chart view: the chart button on the hourly card
    r = dump()
    btn = [bounds(n) for n in r.iter('node') if n.get('content-desc') == s['chart_view_open']]
    if btn:
        b = btn[0]
        adb('shell', 'input', 'tap', str((b[0] + b[2]) // 2), str((b[1] + b[3]) // 2))
        time.sleep(3)
        shot(d / '3.png')
        adb('shell', 'input', 'keyevent', 'KEYCODE_BACK')
        time.sleep(1.5)
    else:
        print('no chart button for', lang)
    for i, key in ((4, 'section_current_conditions'), (5, 'section_sun_air')):
        place(s[key]); shot(d / f'{i}.png')
    to_bottom(); settle(); shot(d / '6.png')
    print('done', lang)


if __name__ == '__main__':
    for lang in sys.argv[2:]:
        take(sys.argv[1], lang)
