"""Плоские предметы на спине как объёмный пиксель-арт (как предметы Minecraft)."""
import math
from PIL import Image, ImageDraw
from wing_sprites import finish, mul, mix, rgba2argb


def C(h, a=255): return ((h >> 16) & 255, (h >> 8) & 255, h & 255, a)


def px_line(d, pts, col, w=1):
    d.line(pts, fill=col, width=w)


def sword():
    im = Image.new('RGBA', (16, 16)); p = im.load()
    blade, edge, dark = C(0x7FE9FF), C(0xD8FBFF), C(0x2A8FA8)
    for i in range(9):  # клинок по диагонали
        x, y = 5 + i, 10 - i
        p[x, y] = blade; p[x + 1, y] = edge if i < 8 else blade; p[x, y + 1] = dark
    p[14, 1] = edge; p[13, 1] = blade
    for x, y in [(2, 10), (3, 11), (4, 12), (3, 9), (5, 13), (6, 12), (4, 10)]: p[x, y] = C(0xF2C230)  # гарда
    p[3, 10] = C(0xFFF0A0)
    for i in range(3): p[3 - i + 1 - 1, 12 + i - 1 + 1] = C(0x7A5230)  # рукоять
    p[2, 13] = C(0x7A5230); p[1, 14] = C(0x5A3A20); p[0, 15] = C(0xF2C230); p[1, 15] = C(0xC9971E); p[0, 14] = C(0xC9971E)
    return finish(im, None, light=False)


def trident():
    im = Image.new('RGBA', (20, 20)); p = im.load()
    sh, hi, dk = C(0x4FB7A6), C(0x9FFFF0), C(0x2A6F64)
    for i in range(15): p[2 + i, 17 - i] = sh; p[2 + i, 18 - i] = dk
    for i in range(14): p[3 + i, 17 - i] = hi if i % 4 == 0 else sh
    # три зубца
    for k in range(4):
        p[13 + k, 1] = hi; p[18, 2 + k] = sh; p[16 - k, 0 + k] = sh
    for x, y in [(13, 0), (12, 1), (19, 5), (19, 6), (15, 1), (17, 3), (18, 4)]: p[x, y] = hi
    for x, y in [(13, 4), (14, 5), (15, 6), (14, 3)]: p[x, y] = C(0x35E0BC)
    p[1, 18] = C(0x2A6F64); p[0, 19] = C(0x9FFFF0)
    return finish(im, None, light=False)


def fishing_rod():
    im = Image.new('RGBA', (16, 18)); p = im.load()
    for i in range(14): p[1 + i, 14 - i] = C(0x8B5A2B) if i % 3 else C(0xA87038)
    for i in range(3): p[1 + i, 14 - i] = C(0x3A3A3A)  # рукоять
    p[2, 15] = C(0x5A3A20)
    for y in range(1, 13): p[15, y] = C(0xE8E8E8, 200)
    p[14, 13] = C(0xE03A3A); p[15, 13] = C(0xE03A3A); p[14, 14] = C(0xF2F2F2); p[15, 14] = C(0xF2F2F2); p[15, 12] = C(0xE03A3A)
    return finish(im, None, light=False)


def guitar(body, dark):
    im = Image.new('RGBA', (12, 28)); d = ImageDraw.Draw(im)
    d.ellipse([1, 10, 10, 18], fill=C(body)); d.ellipse([0, 16, 11, 27], fill=C(body))
    d.rectangle([5, 0, 6, 15], fill=C(0x5A3A20))           # гриф
    d.rectangle([4, 0, 7, 2], fill=C(0x2A1A10))            # голова
    for y in (0, 2): im.putpixel((3, y), C(0xF2C230)); im.putpixel((8, y), C(0xF2C230))
    d.ellipse([4, 15, 7, 18], fill=C(0x1A1A1A))            # розетка
    d.rectangle([4, 22, 7, 23], fill=C(dark))              # подставка
    im = finish(im, C(dark))
    d = ImageDraw.Draw(im)
    for x in (5, 6): d.line([(x, 1), (x, 22)], fill=C(0xE8E8E8, 220))
    return im


def shield():
    im = Image.new('RGBA', (14, 16)); d = ImageDraw.Draw(im)
    d.polygon([(0, 0), (13, 0), (13, 8), (7, 15), (6, 15), (0, 8)], fill=C(0x7A5230))
    d.polygon([(2, 2), (11, 2), (11, 8), (7, 13), (6, 13), (2, 8)], fill=C(0x1C6FB0))
    d.line([(2, 6), (11, 6)], fill=C(0x3EE6FF)); d.line([(2, 7), (11, 7)], fill=C(0x9FF6FF))
    d.line([(6, 2), (6, 12)], fill=C(0xF2C230)); d.line([(7, 2), (7, 12)], fill=C(0xF2C230))
    d.rectangle([5, 5, 8, 8], fill=C(0xF2C230)); im.putpixel((6, 6), C(0xFFF0A0))
    return finish(im, C(0x3A2412))


def surfboard(body, stripe):
    im = Image.new('RGBA', (8, 28)); d = ImageDraw.Draw(im)
    d.ellipse([0, 0, 7, 10], fill=C(body)); d.rectangle([0, 5, 7, 22], fill=C(body)); d.ellipse([0, 17, 7, 27], fill=C(body))
    d.line([(3, 1), (3, 26)], fill=C(stripe)); d.line([(4, 1), (4, 26)], fill=C(stripe))
    d.rectangle([1, 9, 6, 10], fill=C(0xFFFFFF)); d.rectangle([1, 12, 6, 12], fill=C(0xFFFFFF))
    return finish(im, mul(C(body), .55))


def extrude(im, px, ox, oy, z1, z2):
    """Пиксели → кубики (слияние одинаковых), центр картинки в (ox, oy) модели. Флаг 2 — пиксель-арт без фаски."""
    W, H = im.size; p = im.load(); used = [[False] * W for _ in range(H)]; out = []
    for y in range(H):
        for x in range(W):
            c = p[x, y]
            if used[y][x] or c[3] == 0: continue
            x2 = x
            while x2 + 1 < W and not used[y][x2 + 1] and p[x2 + 1, y] == c: x2 += 1
            y2 = y
            while y2 + 1 < H and all(not used[y2 + 1][k] and p[k, y2 + 1] == c for k in range(x, x2 + 1)): y2 += 1
            for yy in range(y, y2 + 1):
                for xx in range(x, x2 + 1): used[yy][xx] = True
            out.append([round(ox + (x - W / 2) * px, 3), round(oy + (y - H / 2) * px, 3), z1, round(ox + (x2 + 1 - W / 2) * px, 3), round(oy + (y2 + 1 - H / 2) * px, 3), z2, rgba2argb(c), 2])
    return out
