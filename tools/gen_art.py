#!/usr/bin/env python3
"""LegoLauncher uchun original piksel-art 'overworld' manzaralari (1280x720 PNG).
Ishlatish: python3 tools/gen_art.py  -> lego-res/drawable-nodpi/lego_world_*.png"""
import math, random, os
from PIL import Image, ImageDraw, ImageEnhance

W, H, S = 256, 144, 5
OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'lego-res', 'drawable-nodpi')

def hexc(h): h = h.lstrip('#'); return tuple(int(h[i:i+2], 16) for i in (0, 2, 4))
def mix(a, b, t): return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))
def jitter(c, r, amt=10): return tuple(max(0, min(255, v + r.randint(-amt, amt))) for v in c)

def sky(d, top, bot, steps=14):
    for y in range(H):
        band = int(y / H * steps) / steps          # bosqichli gradient = piksel ko'rinish
        d.line([(0, y), (W, y)], fill=mix(hexc(top), hexc(bot), band))

def cloud(d, x, y, r, col='#FFFFFF'):
    c = hexc(col)
    w = r.randint(18, 34)
    for row in range(3):
        inset = 0 if row == 1 else r.randint(3, 6)
        d.rectangle([x + inset, y + row * 3, x + w - inset, y + row * 3 + 2], fill=c)

def heights(seed, base, amp, freq):
    r = random.Random(seed)
    p = [r.random() * 6.28 for _ in range(3)]
    return [int(base + amp * (math.sin(x * freq + p[0]) * .55 + math.sin(x * freq * 2.3 + p[1]) * .3 + math.sin(x * freq * 4.1 + p[2]) * .15)) for x in range(W)]

def hills(d, hs, col):
    for x, h in enumerate(hs): d.line([(x, h), (x, H)], fill=col)

ORES = ['#2B2B2B', '#D8AF93', '#E5C34A', '#4AEDD9']  # ko'mir, temir, oltin, olmos

def terrain(d, r, hs, top, dirt, stone, topdark=None, depth=4):
    for x, h in enumerate(hs):
        d.point((x, h), fill=jitter(hexc(top), r, 8))
        if topdark: d.point((x, h + 1), fill=jitter(hexc(topdark), r, 8))
        for y in range(h + 1, H):
            t = y - h
            if t <= depth + r.randint(0, 2):
                c = hexc(dirt)
            else:
                deep = min(1.0, (y - h - depth) / 55.0)
                c = mix(hexc(stone), (52, 54, 60), deep * .75)
            d.point((x, y), fill=jitter(c, r, 9))
    for _ in range(95):                       # rudalar
        x = r.randint(0, W - 4); y = r.randint(min(hs) + 14, H - 3)
        if y <= hs[x] + depth + 6: continue
        col = hexc(r.choice(ORES[:2] if y < 118 else ORES))
        for dx, dy in r.sample([(0, 0), (1, 0), (0, 1), (1, 1), (2, 0)], r.randint(2, 4)):
            if x + dx < W and y + dy < H: d.point((x + dx, y + dy), fill=col)
    for y in range(H - 5, H):                 # bedrock chizig'i
        for x in range(W): d.point((x, y), fill=jitter((30, 30, 34), r, 6))

def oak(d, r, x, ground, leaf='#3F8F2E', leaf2='#2F7422', trunk='#6B4A26', size=1):
    th = int(r.randint(5, 8) * size)
    d.rectangle([x, ground - th, x + 1, ground], fill=hexc(trunk))
    lw = int(7 * size) | 1
    top = ground - th - lw + 2
    for yy in range(lw):
        for xx in range(lw):
            if (xx in (0, lw - 1) and yy in (0, lw - 1)): continue
            c = hexc(leaf) if r.random() > .3 else hexc(leaf2)
            d.point((x - lw // 2 + 1 + xx, top + yy), fill=c)

def spruce(d, r, x, ground, snow=False, col='#2E5E3E'):
    h = r.randint(10, 15)
    d.rectangle([x, ground - 2, x + 1, ground], fill=hexc('#4B3320'))
    for i in range(4):
        w = 2 + i * 2
        y = ground - 3 - (3 - i) * 3 - 2 + 6
        d.rectangle([x + 1 - w // 2 - 1, y - 3 + i * 0, x + w // 2 + 1, y], fill=hexc(col))
        if snow: d.rectangle([x + 1 - w // 2 - 1, y - 3, x + w // 2 + 1, y - 3], fill=hexc('#F4FAFF'))

def cactus(d, r, x, ground):
    h = r.randint(6, 10); c = hexc('#3E7D2F'); c2 = hexc('#2F6624')
    d.rectangle([x, ground - h, x + 1, ground], fill=c)
    d.rectangle([x - 2, ground - h + 3, x - 1, ground - h + 4], fill=c2)
    d.rectangle([x - 2, ground - h + 1, x - 2, ground - h + 4], fill=c2)
    d.rectangle([x + 3, ground - h + 2, x + 4, ground - h + 3], fill=c2)
    d.rectangle([x + 4, ground - h, x + 4, ground - h + 3], fill=c2)

def flowers(d, r, hs, n, cols):
    for _ in range(n):
        x = r.randint(0, W - 1); d.point((x, hs[x] - 1), fill=hexc(r.choice(cols)))

def snowfall(d, r, n):
    for _ in range(n): d.point((r.randint(0, W - 1), r.randint(0, H - 30)), fill=(255, 255, 255))

def save(img, name):
    big = img.resize((W * S, H * S), Image.NEAREST)
    big.save(os.path.join(OUT, name), optimize=True)
    return big

def plains():
    r = random.Random(1); img = Image.new('RGB', (W, H)); d = ImageDraw.Draw(img)
    sky(d, '#6FA0FF', '#CFE6FF'); d.rectangle([196, 18, 207, 29], fill=hexc('#FFF6B8')); d.rectangle([194, 20, 209, 27], fill=hexc('#FFF6B8'))
    for _ in range(6): cloud(d, r.randint(0, W - 30), r.randint(8, 50), r)
    hills(d, heights(11, 78, 12, .03), hexc('#7FB88A')); hills(d, heights(12, 88, 10, .045), hexc('#5FA25A'))
    hs = heights(13, 98, 7, .035); terrain(d, r, hs, '#5DB03A', '#8A5A2B', '#7C7C7C', '#4E9830')
    for x in range(8, W - 8, r.randint(34, 52)): oak(d, r, x, hs[x], size=1.7)
    flowers(d, r, hs, 30, ['#E0382E', '#F5D02A', '#FFFFFF', '#6A8CFF']); return img

def forest():
    r = random.Random(2); img = Image.new('RGB', (W, H)); d = ImageDraw.Draw(img)
    sky(d, '#5C8CD8', '#BCD8F0'); [cloud(d, r.randint(0, W - 30), r.randint(8, 40), r) for _ in range(4)]
    hills(d, heights(21, 80, 10, .03), hexc('#4C8A5A')); hs0 = heights(22, 92, 8, .04); hills(d, hs0, hexc('#3D7A44'))
    for x in range(2, W, 14): oak(d, r, x + r.randint(-2, 2), hs0[min(x, W - 1)] + 3, '#2F7A34', '#256628', size=1.2)
    hs = heights(23, 100, 6, .04); terrain(d, r, hs, '#4F9E34', '#7E5228', '#767676', '#418A2C')
    for x in range(5, W - 5, 26): oak(d, r, x + r.randint(-3, 3), hs[x], '#2C7A2C', '#216421', size=2.0)
    flowers(d, r, hs, 20, ['#E0382E', '#F5D02A']); return img

def desert():
    r = random.Random(3); img = Image.new('RGB', (W, H)); d = ImageDraw.Draw(img)
    sky(d, '#7FB0F5', '#F6E7B8'); d.rectangle([40, 20, 51, 31], fill=hexc('#FFF1A8'))
    for _ in range(3): cloud(d, r.randint(0, W - 30), r.randint(10, 40), r)
    # uzoqdagi piramida
    cx, base = 170, 96
    for i in range(18): d.rectangle([cx - 18 + i, base - i, cx + 18 - i, base - i], fill=jitter(hexc('#D8BE72'), r, 6))
    hills(d, heights(31, 92, 6, .03), hexc('#E7D390')); hs = heights(32, 100, 6, .035)
    terrain(d, r, hs, '#EAD992', '#E0C97A', '#C9AE62', None, depth=7)
    for x in range(10, W - 10, r.randint(40, 60)): cactus(d, r, x, hs[x])
    return img

def snow():
    r = random.Random(4); img = Image.new('RGB', (W, H)); d = ImageDraw.Draw(img)
    sky(d, '#8DA7C9', '#E4EEF8'); [cloud(d, r.randint(0, W - 30), r.randint(8, 44), r, '#F4F8FC') for _ in range(5)]
    hills(d, heights(41, 80, 12, .03), hexc('#B8CCE0')); hs0 = heights(42, 92, 8, .04); hills(d, hs0, hexc('#DCE8F4'))
    for x in range(4, W, 16): spruce(d, r, x + r.randint(-2, 2), hs0[min(x, W - 1)] + 3, True, '#355E4A')
    hs = heights(43, 100, 6, .04); terrain(d, r, hs, '#F6FBFF', '#EAF2FA', '#8A8F96', '#E1ECF6', depth=3)
    for x in range(6, W - 6, 20): spruce(d, r, x + r.randint(-3, 3), hs[x], True, '#2D5440')
    snowfall(d, r, 90); return img

def mountains():
    r = random.Random(5); img = Image.new('RGB', (W, H)); d = ImageDraw.Draw(img)
    sky(d, '#5F93EE', '#C6DEFA'); [cloud(d, r.randint(0, W - 30), r.randint(6, 36), r) for _ in range(4)]
    hills(d, heights(55, 92, 5, .04), hexc('#4C8A5A'))
    for cx, hgt in [(40, 58), (110, 74), (190, 64), (245, 50)]:
        for i in range(hgt):
            w = int(i * 1.15); y = 96 - hgt + i
            d.line([(cx - w, y), (cx + w, y)], fill=jitter(hexc('#8A8F98') if i > 14 else hexc('#F2F7FC'), r, 7))
    hs = heights(51, 98, 5, .04); terrain(d, r, hs, '#5DAA3A', '#855628', '#7A7A7A', '#4E962F')
    for x in range(6, W - 6, 18): spruce(d, r, x + r.randint(-3, 3), hs[x], False, '#2F6B3A')
    return img

if __name__ == '__main__':
    os.makedirs(OUT, exist_ok=True)
    scenes = {'plains': plains, 'forest': forest, 'desert': desert, 'snow': snow, 'mountains': mountains}
    first = None
    for n, f in scenes.items():
        big = save(f(), f'lego_world_{n}.png'); first = first or big
    # ilova foni: qoraytirilgan, to'yinganligi kamaytirilgan 'plains'
    bg = ImageEnhance.Brightness(ImageEnhance.Color(first).enhance(.7)).enhance(.38)
    bg.save(os.path.join(OUT, 'lego_world_bg.png'), optimize=True)
    print('tayyor')
