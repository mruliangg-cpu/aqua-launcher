"""Крылья Aqua как детальный пиксель-арт 32×40 → объёмные кубики (жадное слияние пикселей)."""
import math
from PIL import Image, ImageDraw

SW, SH, SP, ROOT_Y = 32, 40, 0.55, 14
K = 3  # детализация: рисуем в 3 раза подробнее (96×120)


class SD:
    """ImageDraw с масштабом K: координаты задаются в сетке 32×40, рисуется в HD."""
    def __init__(self, im): self.d = ImageDraw.Draw(im)
    def _p(self, pts): return [(x * K + K / 2 - .5, y * K + K / 2 - .5) for x, y in pts]
    def polygon(self, pts, fill=None, outline=None):
        self.d.polygon(self._p(pts), fill=fill)
        if outline: self.d.line(self._p(pts) + self._p(pts[:1]), fill=outline, width=max(1, K // 2))
    def line(self, pts, fill=None, width=1): self.d.line(self._p(pts), fill=fill, width=max(1, round(width * K * .55)), joint='curve')
    def ellipse(self, box, fill=None): self.d.ellipse([box[0] * K, box[1] * K, box[2] * K + K - 1, box[3] * K + K - 1], fill=fill)
    def point(self, pts, fill=None):
        for x, y in pts: self.d.rectangle([x * K, y * K, x * K + K - 1, y * K + K - 1], fill=fill)


def argb2rgba(c): return ((c >> 16) & 255, (c >> 8) & 255, c & 255, (c >> 24) & 255)
def rgba2argb(t): return (t[3] << 24) | (t[0] << 16) | (t[1] << 8) | t[2]
def mul(c, f): return (max(0, min(255, int(c[0] * f))), max(0, min(255, int(c[1] * f))), max(0, min(255, int(c[2] * f))), c[3])
def mix(c1, c2, t): return tuple(int(c1[i] + (c2[i] - c1[i]) * t) for i in range(4))
def opaque(c): return (c[0], c[1], c[2], 255)


def ell(cx, cy, rx, ry, ang, n=18):
    ca, sa = math.cos(ang), math.sin(ang)
    out = []
    for k in range(n):
        u, v = math.cos(k / n * 2 * math.pi) * rx, math.sin(k / n * 2 * math.pi) * ry
        out.append((cx + u * ca - v * sa, cy + u * sa + v * ca))
    return out


def quad_pts(p0, c, p1, n=8):
    return [((1 - t) ** 2 * p0[0] + 2 * (1 - t) * t * c[0] + t * t * p1[0], (1 - t) ** 2 * p0[1] + 2 * (1 - t) * t * c[1] + t * t * p1[1]) for t in [k / n for k in range(n + 1)]]


def finish(im, outline, light=True):
    """Контур, блик по верхнему краю, мягкая светотень сверху вниз."""
    px = im.load(); W, H = im.size
    solid = lambda x, y: 0 <= x < W and 0 <= y < H and px[x, y][3] > 0
    out = im.copy(); o = out.load()
    for y in range(H):
        for x in range(W):
            if not solid(x, y): continue
            c = px[x, y]
            if light: c = mul(c, 1.12 - 0.3 * y / H)
            edge = not (solid(x - 1, y) and solid(x + 1, y) and solid(x, y - 1) and solid(x, y + 1))
            if edge and outline: c = outline
            elif not solid(x, y - 1) or not solid(x, y - K): c = mix(c, (255, 255, 255, c[3]), .28)
            o[x, y] = c
    return out


def sprite_feather(bone, mem, acc):
    im = Image.new('RGBA', (SW * K, SH * K)); d = SD(im)
    m, b_, a_ = argb2rgba(mem), argb2rgba(bone), argb2rgba(acc)
    arm = lambda t: (1 + 22 * t, 13 - 11 * math.sin(t * math.pi * .62))
    layers = [  # (длина от, длина до, ширина, яркость, угол от, угол до, сколько)
        (20, 27, 2.4, .78, 90, 68, 12),   # маховые — длинные, веером вниз
        (11, 15, 2.5, .9, 92, 72, 11),    # второстепенные
        (5, 7, 2.6, 1.04, 96, 80, 10),    # кроющие у кости
    ]
    for li, (l0, l1, wd, f, a0, a1, n) in enumerate(layers):
        for k in range(n):
            t = k / (n - 1)
            if li == 0 and t < .18: continue
            bx, by = arm(t)
            ln = l0 + (l1 - l0) * t
            ang = math.radians(a0 + (a1 - a0) * t)
            cx, cy = bx + math.cos(ang) * ln / 2, by + math.sin(ang) * ln / 2
            col = mul(m, f * (1 if k % 2 else .92))
            d.polygon(ell(cx, cy, ln / 2, wd, ang, 22), fill=col, outline=mul(m, f * .6))
            d.line([(bx, by), (bx + math.cos(ang) * ln * .9, by + math.sin(ang) * ln * .9)], fill=mul(a_, .9 * f), width=.6)
    pts = [arm(k / 20) for k in range(21)]
    d.line(pts, fill=b_, width=3)
    d.line([(x, y - 1) for x, y in pts], fill=mul(b_, 1.25), width=1)
    return finish(im, None)


def sprite_membrane(bone, mem, acc):
    im = Image.new('RGBA', (SW * K, SH * K)); d = SD(im)
    m, b_, a_ = argb2rgba(mem), argb2rgba(bone), argb2rgba(acc)
    root, wrist, tip = (0, 14), (13, 5), (31, 1)
    ends = [tip, (30, 17), (25, 27), (16, 33), (5, 30), (0, 24)]
    poly = [root, wrist]
    for e0, e1 in zip(ends, ends[1:]):
        mid = ((e0[0] + e1[0]) / 2, (e0[1] + e1[1]) / 2)
        ctrl = (mid[0] + (wrist[0] - mid[0]) * .32, mid[1] + (wrist[1] - mid[1]) * .32)
        poly += quad_pts(e0, ctrl, e1)
    d.polygon(poly, fill=m)
    for e in ends[1:5]: d.line([wrist, e], fill=mul(m, .72), width=3)
    im = finish(im, mul(m, .45))
    d = SD(im)
    for e in ends[1:5]: d.line([wrist, e], fill=mul(b_, .95), width=1)
    d.line([root, wrist, tip], fill=b_, width=3)
    d.line([(root[0], root[1] - 1), (wrist[0], wrist[1] - 1), (tip[0], tip[1] - 1)], fill=mul(b_, 1.3), width=1)
    d.polygon([(wrist[0] - 1, wrist[1] - 1), (wrist[0] + 1, wrist[1] - 1), (wrist[0] - 1, wrist[1] - 5)], fill=a_)
    d.polygon([(tip[0] - 2, tip[1] + 1), (tip[0], tip[1] + 1), (tip[0], tip[1] - 3)], fill=a_)
    for e in ends[1:5]: d.point([e], fill=a_)
    return im


UP = [(0, 15), (3, 7), (9, 2), (18, 0), (26, 1), (31, 4), (31, 9), (27, 14), (19, 17), (9, 18), (1, 18)]
def lo_shape(k=1.0): return [(1, 18), (9, 19), (16, 22), (20 * k, 27), (19 * k, 33), (13, 37), (6, 35), (2, 30), (0, 23)]


def sprite_butterfly(edge, mem, acc):
    im = Image.new('RGBA', (SW * K, SH * K)); d = SD(im)
    m, e_, a_ = opaque(argb2rgba(mem)), opaque(argb2rgba(edge)), opaque(argb2rgba(acc))
    lo = lo_shape()
    d.polygon(UP, fill=m); d.polygon(lo, fill=mul(m, .9))
    vein = mul(m, .55)
    for tx, ty in [(10, 2), (18, 1), (25, 2), (30, 7), (26, 13), (16, 17), (14, 25), (16, 33), (8, 34)]:
        d.line([(1, 18), (tx, ty)], fill=vein, width=1)
    im = finish(im, None)
    px = im.load()
    W, H = im.size; r = 2 * K
    solid = lambda x, y: 0 <= x < W and 0 <= y < H and px[x, y][3] > 0
    border = [(x, y) for y in range(H) for x in range(4 * K, W) if solid(x, y) and any(
        not solid(x + dx, y + dy) for dx in range(-r, r + 1) for dy in range(-r, r + 1) if abs(dx) + abs(dy) <= r)]
    for x, y in border: px[x, y] = e_
    d = SD(im)
    for x, y in UP[2:8] + lo[3:7]:
        d.point([(x - (1 if x > 16 else -1), y + (1 if y < 18 else -1))], fill=(255, 255, 255, 255))
    for cx, cy, r in [(22, 7, 3), (11, 26, 2.3)]:  # «глазки» на крыльях
        d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=a_)
        d.ellipse([cx - r * .5, cy - r * .5, cx + r * .5, cy + r * .5], fill=e_)
        d.point([(cx - r * .3, cy - r * .3)], fill=(255, 255, 255, 255))
    return im


def sprite_fairy(edge, mem, acc):
    im = Image.new('RGBA', (SW * K, SH * K)); d = SD(im)
    m, e_, a_ = argb2rgba(mem), argb2rgba(edge), argb2rgba(acc)
    d.polygon(UP, fill=m); d.polygon(lo_shape(1.1), fill=mul(m, .92))
    for tx, ty in [(12, 2), (22, 1), (30, 6), (20, 16), (16, 30), (8, 34)]:
        d.line([(1, 18), (tx, ty)], fill=(e_[0], e_[1], e_[2], 200), width=1)
    im = finish(im, (e_[0], e_[1], e_[2], 235))
    d = SD(im)
    for x, y in [(24, 5), (15, 8), (27, 11), (10, 28), (14, 33), (19, 4), (6, 22)]:
        d.point([(x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)], fill=(a_[0], a_[1], a_[2], 210))
        d.point([(x, y)], fill=(255, 255, 255, 255))
    return im


def sprite_fin(bone, mem, acc):
    im = Image.new('RGBA', (SW * K, SH * K)); d = SD(im)
    m, b_, a_ = argb2rgba(mem), argb2rgba(bone), argb2rgba(acc)
    root, R = (0, 16), 22
    angs = [-58, -30, -5, 22, 48, 70]
    ends = [(root[0] + math.cos(math.radians(a)) * R * (1 - abs(a - 10) / 220), root[1] + math.sin(math.radians(a)) * R * (1 - abs(a - 10) / 220)) for a in angs]
    pts = [root]
    for e0, e1 in zip(ends, ends[1:]):
        mid = ((e0[0] + e1[0]) / 2, (e0[1] + e1[1]) / 2)
        pts += quad_pts(e0, (mid[0] + (root[0] - mid[0]) * .22, mid[1] + (root[1] - mid[1]) * .22), e1)
    d.polygon(pts, fill=m)
    im = finish(im, opaque(mul(m, .6))); d = SD(im)
    for e in ends: d.line([root, e], fill=b_, width=1)
    for e in ends: d.point([e], fill=a_)
    d.line([root, (8, 9)], fill=mul(b_, 1.2), width=2)
    return im


def sprite_leaf(bone, mem, acc):
    im = Image.new('RGBA', (SW * K, SH * K)); d = SD(im)
    m, b_, a_ = opaque(argb2rgba(mem)), argb2rgba(bone), argb2rgba(acc)
    d.polygon(ell(16, 9, 16, 7, math.radians(-22), 26), fill=m)
    d.polygon(ell(11, 24, 11, 5.5, math.radians(28), 22), fill=mul(m, .88))
    im = finish(im, mul(m, .5)); d = SD(im)
    d.line([(1, 15), (30, 3)], fill=b_, width=1); d.line([(1, 18), (20, 29)], fill=b_, width=1)
    for k in range(1, 6):
        x = k * 5; y = 15 - x * .41
        d.line([(x, y), (x + 4, y - 5)], fill=a_, width=1); d.line([(x, y), (x + 3, y + 4)], fill=a_, width=1)
    for k in range(1, 4):
        x = k * 5; y = 18 + x * .58
        d.line([(x, y), (x + 3, y - 4)], fill=a_, width=1); d.line([(x, y), (x + 1, y + 4)], fill=a_, width=1)
    return im


FNS = {'feather': sprite_feather, 'membrane': sprite_membrane, 'butterfly': sprite_butterfly, 'fairy': sprite_fairy, 'fin': sprite_fin, 'leaf': sprite_leaf}


def sprite_to_boxes(im, side, glow=False, depth=.45):
    """Жадно сливает пиксели одного цвета в прямоугольники → тонкие кубики. Флаг 2 = пиксель-арт (без фаски), 3 = + свечение."""
    px = im.load(); W, H = im.size
    used = [[False] * W for _ in range(H)]
    out = []
    for y in range(H):
        for x in range(W):
            c = px[x, y]
            if used[y][x] or c[3] == 0: continue
            x2 = x
            while x2 + 1 < W and not used[y][x2 + 1] and px[x2 + 1, y] == c: x2 += 1
            y2 = y
            while y2 + 1 < H and all(not used[y2 + 1][k] and px[k, y2 + 1] == c for k in range(x, x2 + 1)): y2 += 1
            for yy in range(y, y2 + 1):
                for xx in range(x, x2 + 1): used[yy][xx] = True
            xa, xb = side * x * SP, side * (x2 + 1) * SP
            out.append([round(min(xa, xb), 3), round((y - ROOT_Y) * SP, 3), 0, round(max(xa, xb), 3), round((y2 + 1 - ROOT_Y) * SP, 3), depth, rgba2argb(c), 3 if glow else 2])
    return out


def sprite_groups(im, id_, glow=False):
    """Две HD-пластины (лево/право) + тёмный слой позади для объёма."""
    w, h = SW * SP, SH * SP
    q = lambda side: {'s': id_, 'w': round(side * w, 3), 'y1': round(-ROOT_Y * SP, 3), 'y2': round((SH - ROOT_Y) * SP, 3), 'gl': 1 if glow else 0}
    return q
