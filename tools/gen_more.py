# Выполняется внутри gen_cosmetics.py (общие ITEMS, SPRITES, WS, Image, ...). +100 вещей в каждый слот.
import colorsys, copy
import numpy as np

HUES = [(0, 'Red', 'красный'), (22, 'Orange', 'оранжевый'), (42, 'Gold', 'золотой'), (58, 'Yellow', 'жёлтый'), (90, 'Lime', 'лаймовый'),
        (128, 'Green', 'зелёный'), (160, 'Mint', 'мятный'), (183, 'Cyan', 'бирюзовый'), (203, 'Sky', 'небесный'), (225, 'Blue', 'синий'),
        (258, 'Violet', 'фиолетовый'), (285, 'Purple', 'пурпурный'), (315, 'Pink', 'розовый'), (340, 'Rose', 'малиновый'), (360, 'Red', 'красный')]
SHIFTS = [0.1, 0.27, 0.45, 0.62, 0.8]

def shift_argb(c, d, tint=False):
    a, r, g_, b = (c >> 24) & 255, (c >> 16) & 255, (c >> 8) & 255, c & 255
    h, s, v = colorsys.rgb_to_hsv(r / 255, g_ / 255, b / 255)
    if tint and v >= 0.12:
        r, g_, b = colorsys.hsv_to_rgb(d % 1, max(s, 0.55), max(v, 0.35))
        return (a << 24) | (int(r * 255) << 16) | (int(g_ * 255) << 8) | int(b * 255)
    if s < 0.2 or v < 0.12: return c
    r, g_, b = colorsys.hsv_to_rgb((h + d) % 1, s, v)
    return (a << 24) | (int(r * 255) << 16) | (int(g_ * 255) << 8) | int(b * 255)

def hue_name(colors):
    acc = [0, 0, 0]
    for c in colors:
        r, g_, b = ((c >> 16) & 255) / 255, ((c >> 8) & 255) / 255, (c & 255) / 255
        h, s, v = colorsys.rgb_to_hsv(r, g_, b)
        w = s * v
        acc[0] += math.cos(h * 2 * math.pi) * w; acc[1] += math.sin(h * 2 * math.pi) * w; acc[2] += w
    if acc[2] < 1e-3: return ('Silver', 'серебряный')
    deg = (math.degrees(math.atan2(acc[1], acc[0])) + 360) % 360
    best = min(HUES, key=lambda x: abs(x[0] - deg))
    return best[1], best[2]

def shift_image(im, d):
    a = np.asarray(im.convert('RGBA')).astype(np.float32) / 255
    rgb, al = a[..., :3], a[..., 3:]
    mx, mn = rgb.max(-1), rgb.min(-1); s = np.where(mx > 0, (mx - mn) / np.maximum(mx, 1e-6), 0)
    hsv = np.asarray(im.convert('RGB').convert('HSV')).astype(np.float32)
    hsv[..., 0] = (hsv[..., 0] + d * 255) % 255
    out = np.asarray(Image.fromarray(hsv.astype(np.uint8), 'HSV').convert('RGB')).astype(np.float32) / 255
    keep = ((s < 0.2) | (mx < 0.12))[..., None]
    res = np.where(keep, rgb, out)
    return Image.fromarray((np.concatenate([res, al], -1) * 255).astype(np.uint8), 'RGBA')

def img_colors(im):
    a = np.asarray(im.convert('RGBA').resize((24, 24)))
    return [(255 << 24) | (int(p[0]) << 16) | (int(p[1]) << 8) | int(p[2]) for row in a for p in row if p[3] > 0]

def unique_name(slot, en, ru):
    names = {x['en'] for x in ITEMS[slot]}
    if en not in names: return en, ru
    k = 2
    while f'{en} {k}' in names: k += 1
    return f'{en} {k}', f'{ru} {k}'

def variants(slot):
    base = [it for it in ITEMS[slot][1:] if 'base' not in it]
    for it in base:
        for k, d in enumerate(SHIFTS):
            v = copy.deepcopy(it); v['id'] = f"{it['id']}_v{k + 1}"; v['base'] = it['id']; v['hue'] = d
            allc = [bx[6] for grp in it.get('g', []) for bx in grp['b']]
            tint = 'sp' not in str(it.get('g')) and not any(shift_argb(c, 0.5) != c for c in allc)
            cols = []
            for grp in v.get('g', []):
                for bx in grp['b']:
                    bx[6] = shift_argb(bx[6], d, tint); cols.append(bx[6])
                if 'sp' in grp and it['id'] in SPRITES:
                    im = shift_image(SPRITES[it['id']], d)
                    for dd in (f'{MOD_ASSETS}/textures/wings', f'{UI_ASSETS}/wings'): im.save(f"{dd}/{v['id']}.png")
                    grp['sp'] = dict(grp['sp'], s=v['id']); cols += img_colors(im)
            cen, cru = hue_name(cols)
            v['en'], v['ru'] = unique_name(slot, f"{it['en']} · {cen}", f"{it['ru']} · {cru}")
            ITEMS[slot].append(v)

for slot in ('hat', 'face', 'wings', 'back', 'pet'): variants(slot)

# Плащи: варианты по оттенку (текстуры сделает gen_hd.py)
_cb = [x for x in ITEMS['cape'][1:] if 'base' not in x and x['id'] not in ('rainbow', 'portal')]
_NM = ['Ruby', 'Sun', 'Jungle', 'Abyss', 'Orchid', 'Ember', 'Moss', 'Lagoon', 'Dusk', 'Blossom']
_NR = ['рубин', 'солнце', 'джунгли', 'бездна', 'орхидея', 'уголь', 'мох', 'лагуна', 'сумерки', 'цветение']
for k, d in enumerate(SHIFTS + [0.18, 0.36, 0.54, 0.71, 0.9]):
    for it in _cb:
        if len(ITEMS['cape']) >= 121: break
        en, ru = unique_name('cape', f"{it['en']} · {_NM[k]}", f"{it['ru']} · {_NR[k]}")
        ITEMS['cape'].append({'id': f"{it['id']}_v{k + 1}", 'en': en, 'ru': ru, 'e': it['e'], 'base': it['id'], 'hue': d})

# Ауры: частица × узор движения
PATTERNS = [('ring', 'Ring', 'кольцо'), ('spiral', 'Spiral', 'спираль'), ('halo', 'Halo', 'нимб'), ('trail', 'Trail', 'след'), ('cloud', 'Cloud', 'облако')]
for it in [x for x in ITEMS['aura'][1:] if '_' not in x['id']]:
    for pid, pen, pru in PATTERNS:
        ITEMS['aura'].append({'id': f"{it['id']}_{pid}", 'en': f"{it['en']} · {pen}", 'ru': f"{it['ru']} · {pru}", 'e': it['e']})

# Значки: варианты по оттенку из 8×8 исходников
for it in [x for x in ITEMS['icon'][1:] if 'base' not in x]:
    src = Image.open(f"{MOD_ASSETS}/textures/font/{it['id']}.png").convert('RGBA')
    for k, d in enumerate(SHIFTS):
        vid = f"{it['id']}_v{k + 1}"
        im = shift_image(src, d)
        im.save(f'{MOD_ASSETS}/textures/font/{vid}.png'); im.resize((32, 32), Image.NEAREST).save(f'{UI_ASSETS}/icons/{vid}.png')
        cen, cru = hue_name(img_colors(im))
        en, ru = unique_name('icon', f"{it['en']} · {cen}", f"{it['ru']} · {cru}")
        ITEMS['icon'].append({'id': vid, 'en': en, 'ru': ru, 'e': it['e'], 'base': it['id']})
