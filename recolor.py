#!/usr/bin/env python3
"""Pojav ranglarini Lego temasiga o'tkazadi (qizil #D01012 / sariq #F5CD2A).
Faqat res/*.xml ichidagi #RRGGBB / #AARRGGBB qiymatlarni almashtiradi - nomlarga bog'liq emas, build buzilmaydi."""
import re, sys, colorsys, pathlib

HEX = re.compile(r'#([0-9a-fA-F]{8}|[0-9a-fA-F]{6})(?![0-9a-fA-F])')

def conv(m):
    h = m.group(1)
    a = h[:2] if len(h) == 8 else ''
    r, g, b = (int(h[i:i+2], 16) / 255 for i in range(len(h)-6, len(h), 2))
    hh, s, v = colorsys.rgb_to_hsv(r, g, b)
    if s < 0.15:                       # oq/qora/kulrang - tegilmaydi (matn o'qiladi)
        return m.group(0)
    deg = hh * 360
    if v < 0.5:                        # to'q fonlar -> iliq to'q qizg'ish
        nh, ns, nv = 0.0, min(s, 0.45), v
    elif 90 <= deg <= 200:             # yashil/ko'k-yashil (Play, success) -> sariq
        nh, ns, nv = 47/360, 0.83, max(v, 0.9)
    else:                              # boshqa aksent ranglar -> Lego qizil
        nh, ns, nv = 357/360, 0.9, max(v, 0.8)
    R, G, B = (round(x * 255) for x in colorsys.hsv_to_rgb(nh, ns, nv))
    return f'#{a}{R:02X}{G:02X}{B:02X}'

def main(res):
    n = 0
    for f in pathlib.Path(res).rglob('*.xml'):
        try:
            t = f.read_text(encoding='utf-8')
        except Exception:
            continue
        t2 = HEX.sub(conv, t)
        if t2 != t:
            f.write_text(t2, encoding='utf-8'); n += 1
    print(f'recolor: {n} ta fayl o\'zgartirildi')

if __name__ == '__main__':
    main(sys.argv[1])
