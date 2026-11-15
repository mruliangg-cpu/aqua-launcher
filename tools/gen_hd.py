"""HD-плащи Aqua (512×256, в 8 раз детальнее обычных 64×32) и чёткие значки у ника (16×16).
Запускать ПОСЛЕ gen_cosmetics.py — перезаписывает плащи и значки."""
import json, math, os
import numpy as np
from PIL import Image, ImageDraw, ImageFilter

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
MOD = f'{ROOT}/aqua-client/src/main/resources/assets/aquaclient'
UI = f'{ROOT}/launcher/src/ui/assets'
S = 8                     # масштаб: 1 пиксель обычного плаща = 8 пикселей
PW, PH = 10 * S, 16 * S   # лицевая панель 80×128
rng = np.random.default_rng(7)


def noise(w, h, cell, seed=0, oct=4):
    """Фрактальный шум 0..1."""
    r = np.random.default_rng(seed); out = np.zeros((h, w)); amp, tot = 1.0, 0
    for o in range(oct):
        c = max(2, cell // (2 ** o))
        g = r.random((h // c + 3, w // c + 3))
        im = Image.fromarray((g * 255).astype(np.uint8)).resize(((w // c + 3) * c, (h // c + 3) * c), Image.BICUBIC)
        out += np.asarray(im, float)[:h, :w] / 255 * amp; tot += amp; amp *= .5
    return out / tot


def grid():
    y, x = np.mgrid[0:PH, 0:PW]
    return x / PW, y / PH


def lerp(a, b, t):
    t = np.clip(t, 0, 1)[..., None]
    return np.array(a, float) * (1 - t) + np.array(b, float) * t


def stars(img, n, seed, cols=((255, 255, 255),), big=0.15):
    r = np.random.default_rng(seed)
    for _ in range(n):
        x, y = r.integers(2, PW - 2), r.integers(2, PH - 2); c = cols[r.integers(len(cols))]
        img[y, x] = c
        if r.random() < big:
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)): img[y + dy, x + dx] = np.array(c) * .6
    return img


# ---------------------------------------------------------------- дизайны (u,v → RGB на лицевой панели)
def d_ender(_):
    u, v = grid(); n = noise(PW, PH, 24, 1)
    img = lerp((20, 6, 34), (70, 20, 110), n * .9 + v * .3)
    cx, cy = .5, .45; r = np.hypot((u - cx) * PW, (v - cy) * PH)
    ring = np.exp(-((r - 22) ** 2) / 10); img = img + ring[..., None] * np.array([150, 60, 200])
    img = stars(img, 140, 2, ((230, 160, 255), (255, 255, 255), (180, 90, 255)))
    return img

def d_portal(frame):
    u, v = grid(); img = np.zeros((PH, PW, 3)) + (8, 14, 24)
    for layer in range(5):
        r = np.random.default_rng(100 + layer); k = 1 + layer * .6
        col = [(40, 220, 200), (60, 160, 255), (160, 90, 255), (90, 255, 210), (255, 255, 255)][layer]
        for _ in range(90 - layer * 12):
            x = (r.random() * PW + frame * k * 3) % PW; y = (r.random() * PH + frame * k * 2) % PH
            s = 1 + (layer < 2)
            img[int(y):int(y) + s, int(x):int(x) + s] = col
    glow = noise(PW, PH, 32, 50 + frame, 3)
    return img + glow[..., None] * np.array([10, 30, 40])

def d_eye(_):
    u, v = grid(); img = lerp((6, 30, 30), (14, 60, 56), noise(PW, PH, 20, 3))
    r = np.hypot((u - .5) * PW, (v - .45) * PH)
    img = np.where((r < 26)[..., None], lerp((60, 200, 160), (20, 110, 90), r / 26), img)
    img = np.where((r < 16)[..., None], lerp((150, 255, 210), (40, 170, 130), r / 16), img)
    pupil = (np.abs((u - .5) * PW) < 4) & (np.abs((v - .45) * PH) < 12)
    img[pupil] = (8, 20, 18)
    img[(r > 25) & (r < 27)] = (10, 40, 36)
    return stars(img, 50, 4, ((120, 255, 210),))

def d_pearl(_):
    u, v = grid(); r = np.hypot((u - .5) * PW, (v - .5) * PH) / 60
    img = lerp((140, 250, 225), (10, 70, 64), r)
    sheen = np.exp(-(((u - .35) * PW) ** 2 + ((v - .3) * PH) ** 2) / 120); img = img + sheen[..., None] * 120
    swirl = np.sin(np.arctan2((v - .5) * PH, (u - .5) * PW) * 5 + r * 18) * .5 + .5
    return img * (0.85 + swirl[..., None] * .2)

def d_aqua(_):
    u, v = grid(); img = lerp((60, 220, 250), (6, 40, 90), v ** .9)
    c = np.sin(u * 40 + np.sin(v * 25) * 2) * np.sin(v * 34 + np.sin(u * 20) * 2)
    img = img + (np.clip(c, .6, 1) - .6)[..., None] * 250
    img = stars(img, 30, 5, ((220, 250, 255),), .9)
    return img

def d_dragon(_):
    u, v = grid(); x, y = u * PW, v * PH
    row = np.floor(y / 7); xs = x + (row % 2) * 4
    sx, sy = (xs % 8) - 4, (y % 7) - 3.5
    scale = np.clip(1 - (sx ** 2 / 16 + sy ** 2 / 12), 0, 1)
    img = lerp((14, 8, 22), (90, 40, 140), scale * .9 + v * .2)
    img[(scale < .08)] = (8, 4, 12)
    eye = np.hypot((u - .5) * PW, (v - .28) * PH * 1.6) < 9
    img[eye] = (220, 120, 255); img[eye & (np.abs((u - .5) * PW) < 1.5)] = (20, 0, 30)
    return img

def d_turtle(_):
    u, v = grid(); x, y = u * PW, v * PH
    img = lerp((40, 110, 50), (20, 70, 30), v)
    cs = []
    for cy in (22, 52, 82, 112):
        for cx in (16, 40, 64): cs.append((cx + (8 if cy % 60 == 52 else 0), cy))
    d = np.stack([np.hypot(x - cx, y - cy) for cx, cy in cs]); ds = np.sort(d, 0)
    edge = (ds[1] - ds[0]) < 2.2
    inner = np.clip(1 - ds[0] / 20, 0, 1)
    img = img + inner[..., None] * np.array([60, 90, 40])
    img[edge] = (200, 180, 90)
    return img

def d_galaxy(_):
    u, v = grid(); n1 = noise(PW, PH, 40, 11); n2 = noise(PW, PH, 18, 12)
    img = lerp((6, 4, 24), (40, 10, 80), n1)
    img = img + (np.clip(n2 - .55, 0, 1) * 3)[..., None] * np.array([200, 60, 180])
    img = img + (np.clip(n1 - .6, 0, 1) * 3)[..., None] * np.array([40, 120, 255])
    return stars(img, 220, 13, ((255, 255, 255), (200, 220, 255), (255, 200, 240)), .25)

def d_fire(_):
    u, v = grid(); n = noise(PW, PH, 14, 21)
    t = np.clip((1 - v) * 1.25 - n * .55, 0, 1)
    img = lerp((255, 240, 150), (255, 140, 30), np.clip(t * 2, 0, 1))
    img = np.where((t > .5)[..., None], lerp((255, 140, 30), (120, 10, 10), (t - .5) * 2), img)
    return img

def d_ice(_):
    u, v = grid(); img = lerp((220, 248, 255), (90, 170, 230), v * .8 + noise(PW, PH, 20, 31) * .3)
    im = Image.fromarray(np.uint8(np.clip(img, 0, 255))); d = ImageDraw.Draw(im); r = np.random.default_rng(32)
    for _ in range(14):
        x, y = r.integers(0, PW), r.integers(0, PH)
        for _ in range(5):
            nx, ny = x + r.integers(-14, 15), y + r.integers(-14, 15); d.line([(x, y), (nx, ny)], fill=(255, 255, 255), width=1); x, y = nx, ny
    for cx, cy in [(40, 40), (22, 95), (60, 100)]:
        for a in range(6):
            d.line([(cx, cy), (cx + math.cos(a * math.pi / 3) * 9, cy + math.sin(a * math.pi / 3) * 9)], fill=(255, 255, 255), width=2)
    return np.asarray(im, float)

def d_rainbow(_):
    u, v = grid(); h = (v * 1.0 + u * .15) % 1
    rgb = np.stack([np.clip(np.abs(h * 6 - 3) - 1, 0, 1), np.clip(2 - np.abs(h * 6 - 2), 0, 1), np.clip(2 - np.abs(h * 6 - 4), 0, 1)], -1)
    img = rgb * 230 + 25
    return stars(img, 60, 41, ((255, 255, 255),), .8)

def d_wave(_):
    u, v = grid(); img = lerp((230, 245, 250), (180, 220, 240), v)
    for k, (base, col) in enumerate([(.35, (40, 140, 200)), (.55, (20, 100, 170)), (.75, (10, 60, 120)), (.92, (6, 35, 80))]):
        crest = base + np.sin(u * 9 + k * 1.7) * .05 + np.sin(u * 23 + k) * .015
        m = v > crest
        img = np.where(m[..., None], np.array(col, float), img)
        foam = (v > crest) & (v < crest + .012)
        img[foam] = (250, 252, 255)
    return img

def d_sakura(_):
    u, v = grid(); img = lerp((255, 222, 236), (250, 160, 196), v)
    im = Image.fromarray(np.uint8(img)); d = ImageDraw.Draw(im); r = np.random.default_rng(51)
    d.line([(0, 30), (30, 42), (55, 36), (80, 48)], fill=(90, 50, 50), width=4); d.line([(30, 42), (44, 70)], fill=(90, 50, 50), width=3)
    for _ in range(26):
        x, y = r.integers(0, PW), r.integers(0, PH); s = r.integers(3, 6)
        for a in range(5):
            px, py = x + math.cos(a * 1.256) * s, y + math.sin(a * 1.256) * s
            d.ellipse([px - 2.5, py - 2.5, px + 2.5, py + 2.5], fill=(255, 245, 250))
        d.ellipse([x - 1.5, y - 1.5, x + 1.5, y + 1.5], fill=(240, 110, 160))
    return np.asarray(im, float)

def d_gold(_):
    u, v = grid(); sheen = np.sin((u * PW - v * PH) / 14) * .5 + .5
    img = lerp((150, 100, 20), (255, 225, 110), sheen * .8 + noise(PW, PH, 16, 61) * .3)
    x, y = u * PW, v * PH; dia = (np.abs(x - 40) + np.abs(y - 64)) % 18 < 1.3
    img[dia] = img[dia] * .7
    return img

def d_coral(_):
    u, v = grid(); img = lerp((40, 170, 210), (16, 80, 140), v)
    im = Image.fromarray(np.uint8(img)); d = ImageDraw.Draw(im); r = np.random.default_rng(71)
    for _ in range(9):
        x = r.integers(0, PW); col = [(255, 111, 97), (255, 170, 80), (244, 120, 200), (95, 242, 200)][r.integers(4)]
        stack = [(x, PH, -math.pi / 2, 22)]
        while stack:
            px, py, a, ln = stack.pop()
            nx, ny = px + math.cos(a) * ln, py + math.sin(a) * ln
            d.line([(px, py), (nx, ny)], fill=col, width=max(1, int(ln / 6)))
            if ln > 6: stack += [(nx, ny, a - .5, ln * .7), (nx, ny, a + .45, ln * .7)]
    for _ in range(6):
        x, y = r.integers(5, PW - 10), r.integers(10, 70)
        d.ellipse([x, y, x + 8, y + 5], fill=(255, 140, 40)); d.polygon([(x, y + 2.5), (x - 4, y), (x - 4, y + 5)], fill=(255, 140, 40)); d.line([(x + 3, y), (x + 3, y + 5)], fill=(255, 255, 255))
    return np.asarray(im, float)

def d_deepsea(_):
    u, v = grid(); img = lerp((10, 50, 90), (2, 6, 18), v)
    img = stars(img, 90, 81, ((120, 255, 230), (80, 200, 255)), .5)
    r = np.hypot((u - .6) * PW, (v - .55) * PH); img = img + np.exp(-r ** 2 / 40)[..., None] * np.array([200, 255, 220])
    return img

def d_sunset(_):
    u, v = grid(); img = lerp((255, 200, 90), (230, 70, 110), v / .55)
    r = np.hypot((u - .5) * PW, (v - .5) * PH); img = np.where((r < 18)[..., None] & (v < .55)[..., None], np.array([255, 240, 170.]), img)
    sea = v > .55; img[sea] = lerp((80, 40, 120), (20, 15, 60), (v[sea] - .55) / .45)
    refl = sea & (np.abs(u - .5) * PW < 14 - (v - .55) * 20) & (np.sin(v * PH * 1.3) > .2); img[refl] = (255, 200, 140)
    return img

def d_lava(_):
    u, v = grid(); x, y = u * PW, v * PH; r = np.random.default_rng(91)
    pts = r.random((26, 2)) * (PW, PH)
    d = np.sort(np.stack([np.hypot(x - px, y - py) for px, py in pts]), 0)
    crack = np.clip(1 - (d[1] - d[0]) / 3.5, 0, 1)
    img = lerp((40, 30, 30), (70, 55, 50), noise(PW, PH, 10, 92))
    return img * (1 - crack[..., None]) + lerp((255, 80, 10), (255, 230, 120), crack ** 3) * crack[..., None]

def d_emerald(_):
    u, v = grid(); x, y = u * PW - 40, v * PH - 64
    facet = (np.floor((x + y) / 12) + np.floor((x - y) / 12)) % 3
    img = lerp((10, 120, 60), (90, 240, 150), facet / 2 * .7 + noise(PW, PH, 30, 101) * .3)
    edge = (np.abs((x + y) % 12) < .9) | (np.abs((x - y) % 12) < .9); img[edge] = (190, 255, 210)
    return img

def d_amethyst(_):
    u, v = grid(); img = lerp((40, 16, 70), (90, 50, 140), v)
    im = Image.fromarray(np.uint8(img)); d = ImageDraw.Draw(im); r = np.random.default_rng(111)
    for _ in range(16):
        x = r.integers(0, PW); h = r.integers(30, 80); w = r.integers(5, 12); base = PH - r.integers(0, 20)
        c = [(190, 130, 255), (150, 90, 230), (230, 190, 255)][r.integers(3)]
        d.polygon([(x - w, base), (x - w, base - h), (x, base - h - w * 1.5), (x + w, base - h), (x + w, base)], fill=c)
        d.line([(x, base), (x, base - h - w * 1.5)], fill=(255, 235, 255), width=1)
    return np.asarray(im, float)


DESIGNS = {'ender': (d_ender, (200, 120, 255)), 'eye': (d_eye, (95, 242, 200)), 'pearl': (d_pearl, (62, 230, 200)), 'aqua': (d_aqua, (159, 246, 255)),
           'dragon': (d_dragon, (200, 120, 255)), 'turtle': (d_turtle, (240, 220, 120)), 'galaxy': (d_galaxy, (200, 160, 255)), 'fire': (d_fire, (255, 220, 120)),
           'ice': (d_ice, (255, 255, 255)), 'rainbow': (d_rainbow, (255, 255, 255)), 'wave': (d_wave, (255, 255, 255)), 'sakura': (d_sakura, (255, 255, 255)),
           'gold': (d_gold, (255, 245, 190)), 'coral': (d_coral, (255, 111, 97)), 'deepsea': (d_deepsea, (62, 230, 255)), 'sunset': (d_sunset, (255, 210, 63)),
           'lava': (d_lava, (255, 160, 40)), 'emerald': (d_emerald, (190, 255, 210)), 'amethyst': (d_amethyst, (230, 190, 255))}


def cape(name, face, trim):
    face = np.clip(face, 0, 255)
    tex = np.zeros((32 * S, 64 * S, 4), np.uint8)
    base = face.mean((0, 1)) * .45
    tex[:17 * S + S, :23 * S, :3] = base; tex[:18 * S, :23 * S, 3] = 255
    tex[S:17 * S, S:11 * S, :3] = face                               # лицевая сторона (видна сзади)
    tex[S:17 * S, 12 * S:22 * S, :3] = np.clip(face[:, ::-1] * .5 + 20, 0, 255)  # подкладка
    tex[0:S, S:11 * S, :3] = face[:S]; tex[0:S, 11 * S:21 * S, :3] = face[-S:]  # верх/низ
    tex[S:17 * S, 0:S, :3] = face[:, :S]; tex[S:17 * S, 11 * S:12 * S, :3] = face[:, -S:]
    # окантовка: 3 px цвета + светлая внутренняя линия
    t = np.array(trim, np.uint8); x0, y0, x1, y1 = S, S, 11 * S, 17 * S
    for k in range(3):
        tex[y0 + k, x0:x1, :3] = t; tex[y1 - 1 - k, x0:x1, :3] = (t * .75).astype(np.uint8)
        tex[y0:y1, x0 + k, :3] = t; tex[y0:y1, x1 - 1 - k, :3] = (t * .85).astype(np.uint8)
    tex[y0 + 3, x0 + 3:x1 - 3, :3] = np.minimum(255, t.astype(int) + 60)
    im = Image.fromarray(tex, 'RGBA')
    for d in (f'{MOD}/textures/cape', f'{UI}/capes'): im.save(f'{d}/{name}.png')


for name, (fn, trim) in DESIGNS.items(): cape(name, fn(0), trim)
for f in range(8): cape(f'portal_{f}', d_portal(f), (62, 230, 200))
cape('portal', d_portal(0), (62, 230, 200))
print('HD capes:', len(DESIGNS) + 1)

# ---------------------------------------------------------------- значки 16×16 (Scale2x + свет/тень + контур)
def scale2x(a):
    h, w = a.shape[:2]; o = np.zeros((h * 2, w * 2, 4), np.uint8)
    g = lambda y, x: a[min(h - 1, max(0, y)), min(w - 1, max(0, x))]
    eq = lambda p, q: np.array_equal(p, q)
    for y in range(h):
        for x in range(w):
            P = g(y, x); A, B, C, D = g(y - 1, x), g(y, x + 1), g(y, x - 1), g(y + 1, x)
            e0 = C if eq(C, A) and not eq(C, D) and not eq(A, B) else P
            e1 = A if eq(A, B) and not eq(A, C) and not eq(B, D) else P
            e2 = C if eq(D, C) and not eq(D, B) and not eq(C, A) else P
            e3 = B if eq(B, D) and not eq(B, A) and not eq(D, C) else P
            o[y * 2, x * 2], o[y * 2, x * 2 + 1], o[y * 2 + 1, x * 2], o[y * 2 + 1, x * 2 + 1] = e0, e1, e2, e3
    return o

data = json.load(open(f'{UI}/cosmetics.json'))
providers = []
for i, icon in enumerate(data['icons']):
    a = np.asarray(Image.open(f'{MOD}/textures/font/{icon}.png').convert('RGBA'))
    if a.shape[0] != 8: a = np.asarray(Image.open(f'{MOD}/textures/font/{icon}.png').convert('RGBA').resize((8, 8), Image.NEAREST))
    b = scale2x(a).astype(int)
    solid = b[..., 3] > 0
    up = np.roll(solid, 1, 0); up[0] = False; dn = np.roll(solid, -1, 0); dn[-1] = False
    b[solid & ~up, :3] = np.minimum(255, b[solid & ~up, :3] + 55)         # блик сверху
    b[solid & ~dn, :3] = (b[solid & ~dn, :3] * .65).astype(int)           # тень снизу
    im = Image.fromarray(np.uint8(b), 'RGBA')
    im.save(f'{MOD}/textures/font/{icon}.png')
    im.resize((32, 32), Image.NEAREST).save(f'{UI}/icons/{icon}.png')
    providers.append({'type': 'bitmap', 'file': f'aquaclient:font/{icon}.png', 'ascent': 7, 'height': 8, 'chars': [chr(0xE000 + i)]})
json.dump({'providers': providers}, open(f'{MOD}/font/icons.json', 'w'), indent=1)
print('HD icons:', len(providers))
