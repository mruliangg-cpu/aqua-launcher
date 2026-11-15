"""Генератор косметики Aqua: 3D-модели (кубики), плащи, значки. Один источник для мода и лаунчера."""
import json, math, random, os
from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
MOD_ASSETS = f'{ROOT}/aqua-client/src/main/resources/assets/aquaclient'
UI_ASSETS = f'{ROOT}/launcher/src/ui/assets'

C = dict(BLACK=0xFF1A1A1F, PURPLE=0xFF8E3BD9, MAGENTA=0xFFE070FF, GOLD=0xFFF2C230, GOLD_D=0xFFC9971E, RED=0xFFE03A3A, WHITE=0xFFF2F2F2,
         PINK=0xFFF4A6C8, GREEN=0xFF2F7D3A, GREEN_L=0xFF57A44B, TEAL=0xFF35E0BC, PRISM=0xFF4FB7A6, BLUE=0xFF2E6FE0, BROWN=0xFF7A5230,
         BROWN_D=0xFF5A3A20, GREY=0xFF8A8F96, DARK=0xFF2A1A3A, IVORY=0xFFEDE3C8, AQUA=0xFF3EE6FF, YELLOW=0xFFFFD23F, ORANGE=0xFFFF8A3D,
         RUBY=0xFFD81E3C, EMERALD=0xFF22C35E, CORAL=0xFFFF6F61, NAVY=0xFF1C2E5C)
g = lambda k: C[k]

def B(x1, y1, z1, x2, y2, z2, col):
    return [round(min(x1, x2), 3), round(min(y1, y2), 3), round(min(z1, z2), 3), round(max(x1, x2), 3), round(max(y1, y2), 3), round(max(z1, z2), 3), col]

def G(boxes, origin=(0, 0, 0), anims=()):
    return {'o': list(origin), 'a': list(anims), 'b': boxes}

ITEMS = {s: [] for s in ['cape', 'hat', 'face', 'wings', 'back', 'pet', 'aura', 'icon']}
def add(slot, id_, en, ru, emoji, part=None, groups=None):
    it = {'id': id_, 'en': en, 'ru': ru, 'e': emoji}
    if part: it['part'] = part
    if groups is not None: it['g'] = groups
    ITEMS[slot].append(it)

for s in ITEMS: add(s, 'none', 'None', 'Нет', '🚫')

# ================================================================ ШЛЯПЫ (голова)
def tophat(main, band): return [B(-6, -8.6, -6, 6, -8, 6, main), B(-4, -16, -4, 4, -8.6, 4, main), B(-4.1, -10, -4.1, 4.1, -8.6, 4.1, band)]
def crown(c, d, g1, g2):
    b = [B(-4.6, -11, -4.6, 4.6, -8, -3.6, c), B(-4.6, -11, 3.6, 4.6, -8, 4.6, c), B(-4.6, -11, -3.6, -3.6, -8, 3.6, d), B(3.6, -11, -3.6, 4.6, -8, 3.6, d)]
    for x in (-4.6, -0.7, 3.1): b += [B(x, -13, -4.6, x + 1.5, -11, -3.6, c), B(x, -13, 3.6, x + 1.5, -11, 4.6, c)]
    b += [B(-4.6, -13, -0.7, -3.6, -11, 0.7, c), B(3.6, -13, -0.7, 4.6, -11, 0.7, c), B(-0.6, -10.2, -4.9, 0.6, -9, -4.5, g1), B(-3, -10.2, -4.9, -2, -9, -4.5, g2), B(2, -10.2, -4.9, 3, -9, -4.5, g2)]
    return b
def halo(col):
    ring = [B(math.cos(i * math.pi / 20) * 5.2 - .45, -.3, math.sin(i * math.pi / 20) * 5.2 - .45, math.cos(i * math.pi / 20) * 5.2 + .45, .3, math.sin(i * math.pi / 20) * 5.2 + .45, col) for i in range(40)]
    ring += [B(math.cos(i * math.pi / 20) * 5.2 - .2, -.45, math.sin(i * math.pi / 20) * 5.2 - .2, math.cos(i * math.pi / 20) * 5.2 + .2, -.3, math.sin(i * math.pi / 20) * 5.2 + .2, 0xF0FFFFFF) for i in range(0, 40, 2)]
    return [G(ring, (0, -12.5, 0), ['bobHalo', 'spin'])]
H = 'head'
add('hat', 'tophat', 'Top Hat', 'Цилиндр', '🎩', H, [G(tophat(g('BLACK'), g('AQUA')))])
add('hat', 'tophat_white', 'White Top Hat', 'Белый цилиндр', '🎩', H, [G(tophat(g('WHITE'), g('BLACK')))])
add('hat', 'crown', 'Crown', 'Корона', '👑', H, [G(crown(g('GOLD'), g('GOLD_D'), g('RED'), g('BLUE')))])
add('hat', 'crown_ruby', 'Ruby Crown', 'Рубиновая корона', '👑', H, [G(crown(g('RUBY'), 0xFF9E1028, g('GOLD'), g('WHITE')))])
add('hat', 'crown_emerald', 'Emerald Crown', 'Изумрудная корона', '👑', H, [G(crown(g('EMERALD'), 0xFF178A42, g('GOLD'), g('WHITE')))])
add('hat', 'aquacrown', 'Ocean Crown', 'Корона океана', '🔱', H, [G(crown(g('PRISM'), 0xFF2F8F80, 0xFF9FFFF0, g('WHITE')))])
add('hat', 'halo', 'Halo', 'Нимб', '😇', H, halo(0xF0FFE070))
add('hat', 'halo_blue', 'Aqua Halo', 'Аква-нимб', '😇', H, halo(0xF09FF6FF))
add('hat', 'witch', 'Witch Hat', 'Шляпа ведьмы', '🧙', H, [G([B(-7, -8.6, -7, 7, -8, 7, g('DARK')), B(-4, -12, -4, 4, -8.6, 4, g('DARK')), B(-4.05, -9.8, -4.05, 4.05, -8.6, 4.05, 0xFF4CAF50),
    B(-3, -15, -2.5, 3, -12, 3.5, g('DARK')), B(-2, -17, -1, 2, -15, 3, g('DARK')), B(-1, -19, 1, 1, -17, 3.5, g('DARK'))])])
party = [0xFFFF5FA2, 0xFF41D1FF, 0xFFFFD23F, 0xFF7CFC6B]
add('hat', 'party', 'Party Hat', 'Праздничный колпак', '🥳', H, [G([B(-(3.5 - i * .55), -9.5 - i * 1.5, -(3.5 - i * .55), 3.5 - i * .55, -8 - i * 1.5, 3.5 - i * .55, party[i % 4]) for i in range(6)] + [B(-1, -19.5, -1, 1, -17.5, 1, g('WHITE'))])])
def horn(s, c0, c1, tip, n=14, out=1.0):
    bx = []
    for k in range(n):
        t = k / (n - 1)
        x = s * (2.6 + 3.2 * math.sin(t * 1.4) * out); y = -8.2 - 6.5 * t + 2.2 * t * t; z = -0.5 + 4.5 * t * t
        r = 1.1 * (1 - t * .78)
        col = tip if t > .85 else c0 if (k % 3) else c1
        bx.append(B(x - r, y - r, z - r, x + r, y + r, z + r, col))
    return bx
horns = horn(-1, 0xFF2A1A3A, 0xFF3A2A52, g('AQUA')) + horn(1, 0xFF2A1A3A, 0xFF3A2A52, g('AQUA'))
add('hat', 'horns', 'Dragon Horns', 'Рога дракона', '😈', H, [G(horns)])
bunny = []
for s in (-1, 1): bunny += [B(s * 1.5, -15, -.5, s * 3.5, -8, .5, g('WHITE')), B(s * 1.8, -14.5, -.65, s * 3.2, -9, -.5, g('PINK'))]
add('hat', 'bunny', 'Bunny Ears', 'Уши кролика', '🐰', H, [G(bunny)])
cat = []
for s in (-1, 1): cat += [B(s * 2, -10, -1, s * 4, -8, .5, g('BLACK')), B(s * 2.5, -11, -.8, s * 3.5, -10, .3, g('BLACK')), B(s * 2.4, -9.8, -1.1, s * 3.6, -8.4, -1, g('PINK'))]
add('hat', 'cat', 'Cat Ears', 'Кошачьи уши', '🐱', H, [G(cat)])
vik = [B(-4.7, -8.9, -4.7, 4.7, -6.3, 4.7, 0xFF9AA3AD), B(-4.2, -10, -4.2, 4.2, -8.9, 4.2, 0xFF8A939D), B(-3.2, -10.8, -3.2, 3.2, -10, 3.2, 0xFF7E8791),
       B(-4.8, -7.2, -4.8, 4.8, -6.2, 4.8, g('BROWN_D')), B(-.5, -10.9, -4.9, .5, -6.2, -4.6, 0xFFC0C8D0), B(-.6, -6.3, -4.95, .6, -3.6, -4.6, 0xFF9AA3AD)]
for x in (-3.4, -1.2, 1.2, 3.4): vik.append(B(x - .3, -7, -4.95, x + .3, -6.4, -4.8, g('GOLD')))
for s in (-1, 1):
    for k in range(10):
        t = k / 9; x = s * (4.6 + 2.6 * t); y = -8.2 - 4.8 * t * t; r = .95 * (1 - t * .7)
        vik.append(B(x - r, y - r, -r, x + r, y + r, r, g('IVORY') if t < .8 else 0xFFFFFFFF))
    vik.append(B(s * 4.6 - .6, -8.8, -.9, s * 4.6 + .6, -7.4, .9, g('GOLD')))
add('hat', 'viking', 'Viking Helmet', 'Шлем викинга', '⚔️', H, [G(vik)])
add('hat', 'santa', 'Santa Hat', 'Шапка Санты', '🎅', H, [G([B(-4.6, -9.2, -4.6, 4.6, -7.6, 4.6, g('WHITE')), B(-4, -12, -4, 4, -9.2, 4, g('RED')), B(-3, -14, -2, 3, -12, 4, g('RED')), B(-2, -15.5, 0, 2, -14, 5, g('RED')), B(-1.2, -16, 4, 1.2, -14.2, 6.4, g('WHITE'))])])
wiz = [B(-6.5, -8.6, -6.5, 6.5, -8, 6.5, 0xFF2B3FA8)] + [B(-(4 - i * .6), -10 - i * 1.6, -(4 - i * .6) + i * .3, 4 - i * .6, -8.6 - i * 1.6, 4 - i * .6 + i * .3, 0xFF2B3FA8) for i in range(6)]
add('hat', 'wizard', 'Wizard Hat', 'Шляпа волшебника', '🪄', H, [G(wiz + [B(-1.5, -11, -4.2, -.5, -10, -4, g('GOLD')), B(1, -13, -3.6, 2, -12, -3.4, g('GOLD'))])])
hp = [B(-4.6, -9, -1, 4.6, -8.2, 1, g('BLACK'))]
for s in (-1, 1): hp += [B(s * 4, -8.2, -1, s * 4.7, -4, 1, g('BLACK')), B(s * 4.2, -5.5, -2, s * 5.6, -1.5, 2, g('AQUA'))]
add('hat', 'headphones', 'Headphones', 'Наушники', '🎧', H, [G(hp)])
add('hat', 'beanie', 'Aqua Beanie', 'Аква-шапка', '🧢', H, [G([B(-4.4, -9.6, -4.4, 4.4, -7, 4.4, g('AQUA')), B(-4.6, -8, -4.6, 4.6, -6.6, 4.6, g('WHITE')), B(-1.2, -11, -1.2, 1.2, -9.6, 1.2, g('WHITE'))])])
add('hat', 'cowboy', 'Cowboy Hat', 'Ковбойская шляпа', '🤠', H, [G([B(-7, -8.4, -7, 7, -7.9, 7, g('BROWN')), B(-4, -12, -4, 4, -8.4, 4, g('BROWN')), B(-3, -12.3, -1, 3, -11.6, 1, g('BROWN_D')), B(-4.05, -9.3, -4.05, 4.05, -8.4, 4.05, g('BLACK'))])])
add('hat', 'propeller', 'Propeller Cap', 'Кепка с пропеллером', '🚁', H, [G([B(-4.4, -9.5, -4.4, 4.4, -7.6, 4.4, g('RED')), B(-4.45, -9.5, -1, 4.45, -7.6, 1, g('YELLOW')), B(-4, -8, -7, 4, -7.6, -4.4, g('BLUE')), B(-.4, -11, -.4, .4, -9.5, .4, g('GREY'))]),
    G([B(-5, -.3, -.6, 5, .3, .6, g('YELLOW')), B(-.6, -.3, -5, .6, .3, 5, g('BLUE'))], (0, -11.3, 0), ['propeller'])])

# ================================================================ ЛИЦО
def glasses(frame, l1, l2=None):
    l2 = l2 or l1
    return [B(-4.4, -5.4, -4.6, 4.4, -4.8, -4.25, frame), B(-3.7, -4.8, -4.6, -.6, -3, -4.25, l1), B(.6, -4.8, -4.6, 3.7, -3, -4.25, l2),
            B(-.6, -4.8, -4.6, .6, -4.2, -4.25, frame), B(-4.5, -5.4, -4.3, -4.2, -4.8, 0, frame), B(4.2, -5.4, -4.3, 4.5, -4.8, 0, frame)]
add('face', 'sunglasses', 'Sunglasses', 'Солнечные очки', '🕶️', H, [G(glasses(g('BLACK'), 0xE0101018))])
add('face', 'sunglasses_gold', 'Gold Shades', 'Золотые очки', '🕶️', H, [G(glasses(g('GOLD'), 0xD0FFB020))])
add('face', 'sunglasses_aqua', 'Aqua Shades', 'Аква-очки', '🕶️', H, [G(glasses(g('AQUA'), 0xC03EE6FF))])
add('face', 'sunglasses_pink', 'Pink Shades', 'Розовые очки', '🕶️', H, [G(glasses(0xFFFF7EB6, 0xC0FF9ACD))])
add('face', '3d', '3D Glasses', '3D-очки', '👓', H, [G(glasses(g('WHITE'), 0xC0FF3030, 0xC030E0FF))])
add('face', 'monocle', 'Monocle', 'Монокль', '🧐', H, [G([B(.5, -5.2, -4.5, 3.5, -4.8, -4.3, g('GOLD')), B(.5, -2.8, -4.5, 3.5, -2.4, -4.3, g('GOLD')), B(.5, -5.2, -4.5, .9, -2.4, -4.3, g('GOLD')),
    B(3.1, -5.2, -4.5, 3.5, -2.4, -4.3, g('GOLD')), B(.9, -4.8, -4.45, 3.1, -2.8, -4.35, 0x50C0F0FF), B(3.2, -2.4, -4.45, 3.4, 1, -4.35, g('GOLD'))])])
def mustache(c): return [B(-3, -2.6, -4.5, 3, -1.8, -4.15, c), B(-4, -2.2, -4.5, -3, -1.2, -4.15, c), B(3, -2.2, -4.5, 4, -1.2, -4.15, c)]
add('face', 'mustache', 'Mustache', 'Усы', '🥸', H, [G(mustache(g('BROWN_D')))])
add('face', 'mustache_white', 'Grey Mustache', 'Седые усы', '🥸', H, [G(mustache(0xFFD8D8D8))])
add('face', 'endermask', 'Enderman Mask', 'Маска эндермена', '👾', H, [G([B(-4.3, -5.2, -4.5, 4.3, -2.8, -4.2, g('BLACK')), B(-3.6, -4.6, -4.6, -1, -3.4, -4.5, g('MAGENTA')), B(1, -4.6, -4.6, 3.6, -3.4, -4.5, g('MAGENTA'))])])
add('face', 'clown', 'Clown Nose', 'Нос клоуна', '🤡', H, [G([B(-.9, -3.8, -5.4, .9, -2, -4, g('RED'))])])
def visor(c): return [B(-4.5, -5.4, -4.7, 4.5, -3, -4.2, c), B(-4.6, -5.6, -4.5, 4.6, -5.3, 0, g('GREY'))]
add('face', 'visor', 'Cyber Visor', 'Визор', '🥽', H, [G(visor(0xD03EE6FF))])
add('face', 'visor_red', 'Red Visor', 'Красный визор', '🥽', H, [G(visor(0xD0FF3A4E))])
add('face', 'visor_green', 'Green Visor', 'Зелёный визор', '🥽', H, [G(visor(0xD03AFF7A))])
add('face', 'ninja', 'Ninja Mask', 'Маска ниндзя', '🥷', H, [G([B(-4.35, -3.2, -4.4, 4.35, -.2, 4.35, g('BLACK')), B(-1, -3.2, 4.35, 1, -1.4, 6, g('BLACK'))])])
add('face', 'diver', 'Diving Mask', 'Маска для ныряния', '🤿', H, [G([B(-4.4, -5.6, -4.75, 4.4, -2.6, -4.2, 0xFF2B2B2B), B(-3.8, -5.2, -4.85, 3.8, -3, -4.7, 0x9035E0FF),
    B(-4.5, -5.2, -4.3, 4.5, -4.6, 4.5, g('BLACK')), B(4.3, -11, -1, 5.1, -3, 0, g('YELLOW')), B(4.3, -3.2, -4, 5.1, -2.4, 0, g('YELLOW'))])])
heart = [B(-3.8, -4.8, -4.6, -.8, -3.4, -4.3, 0xD0FF3A6E), B(-3.3, -5.3, -4.6, -2.5, -4.8, -4.3, 0xD0FF3A6E), B(-2.1, -5.3, -4.6, -1.3, -4.8, -4.3, 0xD0FF3A6E), B(-3.1, -3.4, -4.6, -1.5, -2.8, -4.3, 0xD0FF3A6E)]
heart += [B(-b[3], b[1], b[2], -b[0], b[4], b[5], b[6]) for b in heart]
add('face', 'heartglasses', 'Heart Glasses', 'Очки-сердечки', '😍', H, [G(heart + [B(-.8, -4.6, -4.55, .8, -4.2, -4.3, 0xFFFF7EB6), B(-4.5, -4.8, -4.3, -4.2, -4.3, 0, 0xFFFF7EB6), B(4.2, -4.8, -4.3, 4.5, -4.3, 0, 0xFFFF7EB6)])])
star = []
for cx in (-2.2, 2.2): star += [B(cx - 1.6, -4.4, -4.6, cx + 1.6, -3.6, -4.3, 0xE0FFD23F), B(cx - .4, -5.6, -4.6, cx + .4, -2.4, -4.3, 0xE0FFD23F), B(cx - 1, -5, -4.6, cx + 1, -3, -4.3, 0xE0FFD23F)]
add('face', 'starglasses', 'Star Glasses', 'Очки-звёзды', '🤩', H, [G(star + [B(-.6, -4.2, -4.5, .6, -3.8, -4.3, g('GOLD'))])])
add('face', 'bandana', 'Bandana', 'Бандана', '🧣', H, [G([B(-4.35, -3, -4.45, 4.35, 0, 4.35, g('RED')), B(-1, -3, 4.35, 1, -1, 5.5, g('RED')), B(-3, -1.6, -4.5, -2, -.6, -4.45, g('WHITE')), B(1.5, -2.4, -4.5, 2.5, -1.4, -4.45, g('WHITE'))])])
add('face', 'eyepatch', 'Pirate Eyepatch', 'Пиратская повязка', '🏴‍☠️', H, [G([B(.8, -5.2, -4.5, 3.2, -3, -4.3, g('BLACK')), B(-4.4, -6.3, -4.35, 4.4, -5.9, -4.25, g('BLACK')), B(-4.45, -6.3, -4.3, -4.25, -5.9, 4.4, g('BLACK')), B(4.25, -6.3, -4.3, 4.45, -5.9, 4.4, g('BLACK'))])])
nerd = []
for x in (-3.9, .5): nerd += [B(x, -5.4, -4.6, x + 3.4, -5, -4.25, g('BLACK')), B(x, -3.2, -4.6, x + 3.4, -2.8, -4.25, g('BLACK')), B(x, -5.4, -4.6, x + .4, -2.8, -4.25, g('BLACK')), B(x + 3, -5.4, -4.6, x + 3.4, -2.8, -4.25, g('BLACK')), B(x + .4, -5, -4.55, x + 3, -3.2, -4.3, 0x40E0F8FF)]
add('face', 'nerdglasses', 'Nerd Glasses', 'Очки ботаника', '🤓', H, [G(nerd + [B(-.5, -4.6, -4.55, .5, -4.2, -4.3, g('BLACK'))])])

# ================================================================ КРЫЛЬЯ (тело)
def shade(c, f):
    a = (c >> 24) & 255; r = min(255, int(((c >> 16) & 255) * f)); g_ = min(255, int(((c >> 8) & 255) * f)); b_ = min(255, int((c & 255) * f))
    return (a << 24) | (r << 16) | (g_ << 8) | b_
def line_boxes(p0, p1, th, z1, z2, col, side, step=0.7):
    out = []; n = max(1, int(math.dist(p0, p1) / step))
    for k in range(n + 1):
        x = p0[0] + (p1[0] - p0[0]) * k / n; y = p0[1] + (p1[1] - p0[1]) * k / n
        out.append(B(side * (x - th / 2), y - th / 2, z1, side * (x + th / 2), y + th / 2, z2, col))
    return out
def feather_wing(bone, mem, acc, side):
    out = []
    arm = lambda x: -1 - x * 0.3
    for i in range(14):  # костяная дуга
        out.append(B(side * i, arm(i + .5) - .1, -.3, side * (i + 1), arm(i + .5) + 1.3, .55, bone))
        out.append(B(side * i, arm(i + .5) - .25, -.2, side * (i + 1), arm(i + .5) + .15, .45, shade(bone, 1.15)))
    for i in range(14):  # кроющие перья
        top = arm(i + .5) + 1.2
        out.append(B(side * (i + .05), top, -.05, side * (i + .95), top + 2.6, .4, shade(mem, 1.08 if i % 2 else 1)))
    for i in range(14):  # маховые перья: длиннее к краю, заострённые
        top = arm(i + .5) + 3
        ln = 5.5 + i * 0.75 if i < 11 else 13.5 - (i - 11) * 2.2
        col = shade(mem, 0.92 if i % 2 else 0.84)
        out.append(B(side * (i + .08), top, .05, side * (i + .92), top + ln, .3, col))
        out.append(B(side * (i + .3), top + ln, .05, side * (i + .7), top + ln + 1, .3, col))
        out.append(B(side * (i + .45), top, .3, side * (i + .55), top + ln, .36, shade(acc, .9)))  # стержень пера
    return out
def membrane_wing(bone, mem, acc, side):
    out = []
    W, T = (7, -3.5), (14, -5.5)
    ends = [(0, 9.5), (7.5, 10.5), (11, 8), (13.6, 3.5), (14, -5.5)]
    def arm_y(x): return x / 7 * W[1] if x <= 7 else W[1] + (x - 7) / 7 * (T[1] - W[1])
    def bottom(x):
        for (ax, ay), (bx, by) in zip(ends, ends[1:]):
            if ax <= x <= bx:
                t = (x - ax) / max(.01, bx - ax)
                return ay + (by - ay) * t - math.sin(math.pi * t) * 1.8
        return ends[-1][1]
    for i in range(14):
        x = i + .5; top = arm_y(x) + .5; bot = bottom(x)
        if bot > top: out.append(B(side * i, top, .05, side * (i + 1), bot, .3, shade(mem, 1 - (i % 2) * .06)))
    out += line_boxes((0, 0), W, 1.3, -.25, .5, bone, side) + line_boxes(W, T, 1.1, -.2, .5, bone, side)
    for e in ends[1:4]: out += line_boxes(W, e, .6, -.05, .4, shade(bone, .9), side, .8)
    out += [B(side * 6.6, W[1] - 1.8, -.2, side * 7.4, W[1] - .4, .45, acc), B(side * 13.6, T[1] - 1.5, -.15, side * 14.4, T[1] + .2, .4, acc)]
    return out
import wing_sprites as WS
import item_sprites as IS
SPRITES = {}
def wings(shape, bone, mem, acc, id_=None):
    im = WS.FNS[shape](bone, mem, acc)
    SPRITES[id_] = im
    for d in (f'{MOD_ASSETS}/textures/wings', f'{UI_ASSETS}/wings'): os.makedirs(d, exist_ok=True); im.save(f'{d}/{id_}.png')
    h1, h2 = round(-WS.ROOT_Y * WS.SP, 3), round((WS.SH - WS.ROOT_Y) * WS.SP, 3)
    sp = lambda side: {'s': id_, 'w': round(side * WS.SW * WS.SP, 3), 'y1': h1, 'y2': h2, 'gl': 1 if shape == 'fairy' else 0}
    return [{'o': [-1.5, 1.2, 2.3], 'a': ['flapL'], 'b': [], 'sp': sp(-1)}, {'o': [1.5, 1.2, 2.3], 'a': ['flapR'], 'b': [], 'sp': sp(1)}]
W = 'body'
for id_, en, ru, e, sh, bo, me, ac in [
    ('dragon', 'Dragon Wings', 'Крылья дракона', '🐲', 'membrane', g('DARK'), 0xD08E3BD9, g('MAGENTA')),
    ('dragon_red', 'Red Dragon Wings', 'Крылья красного дракона', '🐉', 'membrane', 0xFF4A1010, 0xD0D82C2C, 0xFFFF8A3D),
    ('dragon_aqua', 'Sea Dragon Wings', 'Крылья морского дракона', '🐉', 'membrane', g('NAVY'), 0xC03EE6FF, g('TEAL')),
    ('dragon_gold', 'Golden Dragon Wings', 'Крылья золотого дракона', '🐉', 'membrane', g('GOLD_D'), 0xD0F2C230, 0xFFFFF0A0),
    ('angel', 'Angel Wings', 'Крылья ангела', '🕊️', 'feather', g('WHITE'), 0xF0FFFFFF, 0xFFE8EEF5),
    ('angel_gold', 'Golden Angel Wings', 'Золотые крылья ангела', '👼', 'feather', g('GOLD'), 0xF0FFF3B0, g('GOLD_D')),
    ('angel_black', 'Dark Angel Wings', 'Тёмные крылья ангела', '🖤', 'feather', 0xFF202024, 0xF0303036, 0xFF4A4A55),
    ('bat', 'Bat Wings', 'Крылья летучей мыши', '🦇', 'membrane', 0xFF202020, 0xE03A2A3A, g('BLACK')),
    ('bat_red', 'Vampire Wings', 'Крылья вампира', '🧛', 'membrane', 0xFF2A0A0A, 0xE07A1020, g('BLACK')),
    ('butterfly', 'Butterfly Wings', 'Крылья бабочки', '🦋', 'butterfly', g('BLACK'), 0xF0FF8A3D, g('YELLOW')),
    ('butterfly_blue', 'Blue Butterfly Wings', 'Голубые крылья бабочки', '🦋', 'butterfly', g('BLACK'), 0xF02E9BFF, g('AQUA')),
    ('butterfly_pink', 'Pink Butterfly Wings', 'Розовые крылья бабочки', '🦋', 'butterfly', g('BLACK'), 0xF0FF7EB6, g('WHITE')),
    ('butterfly_green', 'Green Butterfly Wings', 'Зелёные крылья бабочки', '🦋', 'butterfly', g('BLACK'), 0xF05ED45E, g('YELLOW')),
    ('fairy', 'Fairy Wings', 'Крылья феи', '🧚', 'fairy', 0xFF9FFFF0, 0x9035E0FF, 0xC0E070FF),
    ('fairy_pink', 'Pink Fairy Wings', 'Розовые крылья феи', '🧚', 'fairy', 0xFFFFC0E0, 0x90FF7EB6, 0xC0FFFFFF),
    ('fairy_gold', 'Golden Fairy Wings', 'Золотые крылья феи', '🧚', 'fairy', 0xFFFFF0A0, 0x90FFD23F, 0xC0FFFFFF),
    ('phantom', 'Phantom Wings', 'Крылья фантома', '👻', 'membrane', 0xFF3A4B7A, 0xD04A6FA8, 0xFF9FE870),
    ('fin', 'Fish Fins', 'Плавники', '🐟', 'fin', g('AQUA'), 0xC03EE6FF, g('WHITE')),
    ('fin_coral', 'Coral Fins', 'Коралловые плавники', '🐠', 'fin', g('CORAL'), 0xC0FF8A7A, g('YELLOW')),
    ('leaf', 'Leaf Wings', 'Крылья из листьев', '🍃', 'leaf', 0xFF3D7A2A, 0xE05ED45E, 0xFF9FE870)]:
    add('wings', id_, en, ru, e, W, wings(sh, bo, me, ac, id_))

# ================================================================ СПИНА
def shell(c, l, rim): return [B(-4.6, .4, 2, 4.6, 11, 4, c), B(-3.5, 1.5, 4, 3.5, 10, 4.8, l), B(-1.2, 3, 4.8, 1.2, 8, 5.2, c), B(-4.8, 10.5, 1.8, 4.8, 11.4, 4.2, rim)]
add('back', 'turtleshell', 'Turtle Shell', 'Панцирь черепахи', '🐢', W, [G(shell(g('GREEN'), g('GREEN_L'), 0xFF6FBF5B))])
add('back', 'turtleshell_gold', 'Golden Shell', 'Золотой панцирь', '🐢', W, [G(shell(g('GOLD_D'), g('GOLD'), 0xFFFFF0A0))])
add('back', 'turtleshell_aqua', 'Aqua Shell', 'Аква-панцирь', '🐢', W, [G(shell(0xFF1784AD, g('AQUA'), g('TEAL')))])
def pack(c, d): return [B(-3.5, 1, 2, 3.5, 10, 5, c), B(-3.6, 1, 2, 3.6, 4, 5.2, d), B(-.6, 3.5, 5.1, .6, 5, 5.4, g('GOLD')), B(-3, 7, 5, 3, 9.5, 5.6, d)]
add('back', 'backpack', 'Backpack', 'Рюкзак', '🎒', W, [G(pack(g('BROWN'), g('BROWN_D')))])
add('back', 'backpack_blue', 'Blue Backpack', 'Синий рюкзак', '🎒', W, [G(pack(g('BLUE'), g('NAVY')))])
add('back', 'backpack_red', 'Red Backpack', 'Красный рюкзак', '🎒', W, [G(pack(g('RED'), 0xFF8E1F1F))])
add('back', 'backpack_pink', 'Pink Backpack', 'Розовый рюкзак', '🎒', W, [G(pack(0xFFFF7EB6, 0xFFC85A8C))])
def jet(c):
    body = [B(-.4, 2, 2.5, .4, 8, 4.5, g('BLACK'))]
    flames = []
    for s in (-1, 1):
        body += [B(s * .4, .5, 2, s * 3.6, 10, 5, c), B(s * 1, 10, 2.5, s * 3, 11, 4.5, g('BLACK'))]
        flames += [G([B(s * 1.3, 0, 2.8, s * 2.7, 3, 4.2, 0xC0FF9A2E), B(s * 1.6, 0, 3.1, s * 2.4, 2, 3.9, 0xE0FFE066)], (0, 11, 0), ['flicker'])]
    return [G(body)] + flames
add('back', 'jetpack', 'Jetpack', 'Реактивный ранец', '🚀', W, jet(g('GREY')))
add('back', 'jetpack_gold', 'Golden Jetpack', 'Золотой ранец', '🚀', W, jet(g('GOLD')))
add('back', 'enderchest', 'Ender Chest', 'Эндер-сундук', '🧰', W, [G([B(-3.5, 1, 2, 3.5, 8, 8, 0xFF1E3B3B), B(-3.6, 1, 2, 3.6, 3, 8.1, 0xFF2C5656), B(-.7, 2.4, 8, .7, 4.2, 8.4, g('TEAL')), B(-3.5, 3, 2, 3.5, 3.3, 8.05, g('BLACK'))])])
q = [B(-1.5, -1, 2, 1.5, 9, 4.5, g('BROWN')), B(-1.6, 6, 1.9, 1.6, 7, 4.6, g('BROWN_D'))]
for x in (-.9, .1): q += [B(x, -5, 3, x + .6, -1, 3.6, g('IVORY')), B(x - .2, -6, 2.9, x + .8, -5, 3.7, g('WHITE'))]
add('back', 'quiver', 'Quiver', 'Колчан', '🏹', W, [G(q)])
def guitar(c): return [B(-1, -4, 2.2, 1, 4, 3, g('BROWN_D')), B(-3.5, 4, 2, 3.5, 11, 3.5, c), B(-1, 6, 3.5, 1, 8, 3.6, g('BLACK')), B(-1.5, -5.5, 2.1, 1.5, -4, 3.1, g('BLACK'))]
add('back', 'guitar', 'Guitar', 'Гитара', '🎸', W, [G(IS.extrude(IS.guitar(0xFFD2691E, 0xFF7A3A10), .62, 0, 5.5, 2.2, 3.2))])
add('back', 'guitar_blue', 'Electric Guitar', 'Электрогитара', '🎸', W, [G(IS.extrude(IS.guitar(0xFF2E6FE0, 0xFF1C2E5C), .62, 0, 5.5, 2.2, 3.2))])
def board(c, s): return [B(-2.5, -4, 2.2, 2.5, 13, 3.2, c), B(-.5, -4, 3.2, .5, 13, 3.3, s), B(-.3, 9, 3.2, .3, 12, 4.8, s), B(-1.8, -5, 2.2, 1.8, -4, 3.2, c), B(-1.8, 13, 2.2, 1.8, 14, 3.2, c)]
add('back', 'surfboard', 'Surfboard', 'Доска для сёрфинга', '🏄', W, [G(IS.extrude(IS.surfboard(0xFF3EE6FF, 0xFFFFFFFF), .62, 0, 5, 2.2, 3))])
add('back', 'surfboard_red', 'Sunset Surfboard', 'Закатная доска', '🏄', W, [G(IS.extrude(IS.surfboard(0xFFFF6F61, 0xFFFFD23F), .62, 0, 5, 2.2, 3))])
add('back', 'scubatank', 'Scuba Tank', 'Акваланг', '🤿', W, [G([B(-3.5, .5, 2, -.5, 10, 5, g('YELLOW')), B(.5, .5, 2, 3.5, 10, 5, g('YELLOW')), B(-2.6, -.6, 3, -1.4, .5, 4, g('BLACK')), B(1.4, -.6, 3, 2.6, .5, 4, g('BLACK')), B(-3.6, 4, 1.9, 3.6, 5, 5.1, g('BLACK'))])])
add('back', 'shield', 'Knight Shield', 'Рыцарский щит', '🛡️', W, [G(IS.extrude(IS.shield(), .68, 0, 5.5, 2.2, 3.1))])
add('back', 'sword', 'Diamond Sword', 'Алмазный меч', '🗡️', W, [G(IS.extrude(IS.sword(), 1.0, 0, 5, 2.2, 3.1))])
add('back', 'trident', 'Trident', 'Трезубец', '🔱', W, [G(IS.extrude(IS.trident(), 1.0, 0, 4.5, 2.2, 3.1))])
add('back', 'fishingrod', 'Fishing Rod', 'Удочка', '🎣', W, [G(IS.extrude(IS.fishing_rod(), 1.0, 0, 4.5, 2.2, 2.9))])

# ================================================================ ПИТОМЦЫ (летают у плеча)
PO = (-13, -6, 2)
def pet(boxes, extra=()): return [G(boxes, PO, ['bob', 'sway'] + list(extra))]
def turtle(c, l):
    b = [B(-3, -2.5, -3, 3, 0, 3, c), B(-2, -3.2, -2, 2, -2.5, 2, l), B(-1, -1.8, -5, 1, 0, -3, 0xFF6FBF5B), B(-.8, -1.4, -5.1, -.3, -.9, -4.9, g('BLACK')), B(.3, -1.4, -5.1, .8, -.9, -4.9, g('BLACK'))]
    for x in (-4, 3):
        for z in (-2.5, 1.5): b.append(B(x, -1, z, x + 1, 0, z + 1, 0xFF5AA84A))
    return b
add('pet', 'turtle', 'Mini Turtle', 'Мини-черепаха', '🐢', 'root', pet(turtle(g('GREEN'), g('GREEN_L'))))
add('pet', 'turtle_gold', 'Golden Turtle', 'Золотая черепашка', '🐢', 'root', pet(turtle(g('GOLD_D'), g('GOLD'))))
add('pet', 'pearl', 'Pearl Orb', 'Орб жемчуга', '🔮', 'root', pet([B(-2, -2, -2, 2, 2, 2, 0xFF22917F), B(-1.5, -2.3, -1.5, 1.5, 2.3, 1.5, 0xFF3CC9A6), B(-.8, -.8, -2.4, .8, .8, 2.4, 0xFF0B3B36)], ['spinFast']))
add('pet', 'enderman', 'Mini Enderman', 'Мини-эндермен', '🖤', 'root', pet([B(-1.5, -6, -1.5, 1.5, -3, 1.5, g('BLACK')), B(-1.2, -4.6, -1.6, -.3, -4.1, -1.5, g('MAGENTA')), B(.3, -4.6, -1.6, 1.2, -4.1, -1.5, g('MAGENTA')),
    B(-1, -3, -.6, 1, 1, .6, g('BLACK')), B(-1.6, -3, -.3, -1, 2, .3, g('BLACK')), B(1, -3, -.3, 1.6, 2, .3, g('BLACK')), B(-.9, 1, -.3, -.3, 4, .3, g('BLACK')), B(.3, 1, -.3, .9, 4, .3, g('BLACK'))]))
def axo(c, gill, tail): return [B(-1.5, -1.5, -3, 1.5, .5, 3, c), B(-2, -2, -4.5, 2, .5, -2.5, c), B(-3, -2.5, -4, -2, -.5, -3, gill), B(2, -2.5, -4, 3, -.5, -3, gill), B(-.5, -1.5, 3, .5, 0, 6, tail), B(-1.4, -1.4, -4.6, -.8, -.8, -4.4, g('BLACK')), B(.8, -1.4, -4.6, 1.4, -.8, -4.4, g('BLACK'))]
add('pet', 'axolotl', 'Axolotl', 'Аксолотль', '🦎', 'root', pet(axo(g('PINK'), 0xFFE070A0, 0xFFE489B0)))
add('pet', 'axolotl_blue', 'Blue Axolotl', 'Голубой аксолотль', '🦎', 'root', pet(axo(0xFF8FD0FF, 0xFF5AA0E0, 0xFF6FB8F0)))
add('pet', 'axolotl_gold', 'Gold Axolotl', 'Золотой аксолотль', '🦎', 'root', pet(axo(0xFFFFD76B, 0xFFE0B040, 0xFFF0C050)))
def fish(c, s): return [G([B(-1, -2, -3, 1, 1, 2, c), B(-1.05, -2, -1, 1.05, 1, 0, s), B(-1.1, -1, -2.5, -1, -.4, -2, g('BLACK'))], PO, ['bob', 'sway']), G([B(-.4, -1.5, 2, .4, .5, 4, c)], PO, ['bob', 'sway', 'wiggle'])]
add('pet', 'fish', 'Clownfish', 'Рыба-клоун', '🐠', 'root', fish(g('ORANGE'), g('WHITE')))
add('pet', 'fish_blue', 'Blue Tang', 'Синяя рыбка', '🐟', 'root', fish(g('BLUE'), g('YELLOW')))
add('pet', 'fish_yellow', 'Yellow Fish', 'Жёлтая рыбка', '🐡', 'root', fish(g('YELLOW'), g('AQUA')))
add('pet', 'egg', 'Dragon Egg', 'Яйцо дракона', '🥚', 'root', pet([B(-2, -4, -2, 2, 1, 2, 0xFF141019), B(-1.5, -5, -1.5, 1.5, -4, 1.5, 0xFF141019), B(-2.05, -2, -.6, 2.05, -1.4, .6, 0xFF5A2A7A)], ['spin']))
add('pet', 'bee', 'Bee', 'Пчёлка', '🐝', 'root', [G([B(-1.5, -1.5, -2.5, 1.5, 1.5, 2.5, 0xFFF2C94C), B(-1.55, -1.55, -.5, 1.55, 1.55, .5, g('BLACK')), B(-1.55, -1.55, 1.5, 1.55, 1.55, 2, g('BLACK')),
    B(-1, -.6, -2.6, -.4, 0, -2.5, g('BLACK')), B(.4, -.6, -2.6, 1, 0, -2.5, g('BLACK'))], PO, ['bob', 'sway']), G([B(-2.5, -2.7, -1, -.5, -1.5, .5, 0xB0E8F8FF), B(.5, -2.7, -1, 2.5, -1.5, .5, 0xB0E8F8FF)], PO, ['bob', 'sway', 'buzz'])])
add('pet', 'puffer', 'Pufferfish', 'Рыба-фугу', '🐡', 'root', pet([B(-2.5, -2.5, -2.5, 2.5, 2.5, 2.5, 0xFFF2C94C), B(-2.6, .5, -2.6, 2.6, 2.6, 2.6, 0xFFFFF1B8), B(-1.6, -1.2, -2.7, -.8, -.4, -2.5, g('BLACK')), B(.8, -1.2, -2.7, 1.6, -.4, -2.5, g('BLACK')),
    B(-3.2, -1, -.3, -2.5, -.3, .3, 0xFFD4A017), B(2.5, -1, -.3, 3.2, -.3, .3, 0xFFD4A017), B(-.3, -3.2, -.3, .3, -2.5, .3, 0xFFD4A017), B(-.3, -.3, 2.5, .3, .3, 3.2, 0xFFD4A017)]))
jel = [B(-2.5, -3, -2.5, 2.5, -1, 2.5, 0xA0B39DFF), B(-2, -3.6, -2, 2, -3, 2, 0xA0D0C0FF)] + [B(x - .25, -1, z - .25, x + .25, 2.5 + (abs(x) + abs(z)) * .3, z + .25, 0x90E0D0FF) for x in (-1.5, 0, 1.5) for z in (-1.5, 1.5)]
add('pet', 'jelly', 'Jellyfish', 'Медуза', '🪼', 'root', pet(jel))
crab = [B(-2.5, -1.5, -1.5, 2.5, 0, 1.5, 0xFFE0402F), B(-1, -2.5, -1.6, -.4, -1.5, -1, g('BLACK')), B(.4, -2.5, -1.6, 1, -1.5, -1, g('BLACK')), B(-4, -1.6, -2, -2.5, -.2, -.5, 0xFFFF6B4F), B(2.5, -1.6, -2, 4, -.2, -.5, 0xFFFF6B4F)]
for x in (-2.5, -1, .4, 1.9): crab.append(B(x, 0, -1.4, x + .6, 1.2, -1.2, 0xFFB8301F))
add('pet', 'crab', 'Crab', 'Краб', '🦀', 'root', pet(crab))
add('pet', 'dolphin', 'Mini Dolphin', 'Мини-дельфин', '🐬', 'root', pet([B(-1.5, -1.5, -4, 1.5, 1.2, 3, 0xFF6D8FA6), B(-1.2, .2, -4, 1.2, 1.25, 3, 0xFFC9DBE6), B(-.7, -.8, -6, .7, .6, -4, 0xFF6D8FA6), B(-.3, -3, -.5, .3, -1.5, 1.5, 0xFF6D8FA6),
    B(-2.5, -.2, 3, 2.5, .4, 4.5, 0xFF5A7A90), B(-1.55, -.8, -3.4, -1.5, -.3, -3, g('BLACK')), B(1.5, -.8, -3.4, 1.55, -.3, -3, g('BLACK'))]))
add('pet', 'duck', 'Rubber Duck', 'Резиновая уточка', '🦆', 'root', pet([B(-2, -1.5, -2.5, 2, 1, 2.5, g('YELLOW')), B(-1.3, -4, -2.6, 1.3, -1.5, -.4, g('YELLOW')), B(-.8, -2.8, -4, .8, -2.2, -2.6, g('ORANGE')), B(-1.35, -3.3, -2.3, -1.3, -2.8, -1.8, g('BLACK')), B(1.3, -3.3, -2.3, 1.35, -2.8, -1.8, g('BLACK'))]))
add('pet', 'slime', 'Slime', 'Слизень', '🟩', 'root', pet([B(-2.5, -2.5, -2.5, 2.5, 2.5, 2.5, 0xA07CDA5A), B(-1.4, -1.4, -1.4, 1.4, 1.4, 1.4, 0xFF5BAE3E), B(-1.6, -1, -2.6, -.6, 0, -2.5, 0xFF1E3A14), B(.6, -1, -2.6, 1.6, 0, -2.5, 0xFF1E3A14)]))
sq = [B(-2, -5, -2, 2, -1, 2, 0xFF1FB5A8), B(-1.5, -3.6, -2.1, -.5, -2.6, -2, 0xFF9FFFF0), B(.5, -3.6, -2.1, 1.5, -2.6, -2, 0xFF9FFFF0)] + [B(x - .3, -1, z - .3, x + .3, 3, z + .3, 0xFF189A90) for x in (-1.2, 1.2) for z in (-1.2, 1.2)]
add('pet', 'squid', 'Glow Squid', 'Светящийся кальмар', '🦑', 'root', pet(sq))
add('pet', 'ghost', 'Little Ghost', 'Призрачок', '👻', 'root', pet([B(-2, -4, -2, 2, 1, 2, 0xB0F4F8FF), B(-2, 1, -2, -1, 2, -1, 0xB0F4F8FF), B(1, 1, 1, 2, 2, 2, 0xB0F4F8FF), B(-1.2, -2.8, -2.1, -.4, -1.8, -2, g('BLACK')), B(.4, -2.8, -2.1, 1.2, -1.8, -2, g('BLACK'))]))

# ================================================================ АУРЫ
for id_, en, ru, e in [('portal', 'Portal', 'Портал', '🟪'), ('endrod', 'End Rod', 'Энд-род', '✨'), ('hearts', 'Hearts', 'Сердечки', '💖'), ('notes', 'Notes', 'Ноты', '🎵'),
    ('bubbles', 'Bubbles', 'Пузыри', '🫧'), ('snow', 'Snow', 'Снег', '❄️'), ('flame', 'Flame', 'Пламя', '🔥'), ('totem', 'Totem', 'Тотем', '🗿'), ('cherry', 'Petals', 'Лепестки', '🌸'),
    ('soul', 'Soul Fire', 'Огонь душ', '💙'), ('glow', 'Glow', 'Сияние', '💚'), ('dolphin', 'Dolphin Trail', 'След дельфина', '🐬'), ('drip', 'Water Drops', 'Капли', '💧'),
    ('happy', 'Sparkles', 'Искорки', '🌟'), ('crit', 'Crits', 'Криты', '⚔️'), ('enchant', 'Enchant Runes', 'Руны', '📖'), ('cloud', 'Clouds', 'Облака', '☁️'),
    ('spore', 'Spores', 'Споры', '🍄'), ('ash', 'White Ash', 'Пепел', '🌫️'), ('rain', 'Rain', 'Дождь', '🌧️')]:
    add('aura', id_, en, ru, e)

# ================================================================ ПЛАЩИ (текстуры 64×32)
CAPES = [('ender', 'Ender', 'Эндер', '🟣'), ('portal', 'Portal (animated)', 'Портал (анимация)', '🌀'), ('eye', 'Eye of Ender', 'Око Края', '👁️'), ('pearl', 'Pearl', 'Жемчуг', '🔮'),
         ('aqua', 'Aqua', 'Аква', '🌊'), ('dragon', 'Dragon', 'Дракон', '🐉'), ('turtle', 'Turtle', 'Черепаха', '🐢'), ('galaxy', 'Galaxy', 'Галактика', '🌌'), ('fire', 'Fire', 'Огонь', '🔥'),
         ('ice', 'Ice', 'Лёд', '🧊'), ('rainbow', 'Rainbow', 'Радуга', '🌈'), ('wave', 'Ocean Wave', 'Океанская волна', '🌊'), ('sakura', 'Sakura', 'Сакура', '🌸'), ('gold', 'Gold', 'Золото', '🏆'),
         ('coral', 'Coral Reef', 'Коралловый риф', '🪸'), ('deepsea', 'Deep Sea', 'Глубина', '🐋'), ('sunset', 'Sunset', 'Закат', '🌅'), ('lava', 'Lava', 'Лава', '🌋'),
         ('emerald', 'Emerald', 'Изумруд', '💚'), ('amethyst', 'Amethyst', 'Аметист', '💜')]
for id_, en, ru, e in CAPES: add('cape', id_, en, ru, e)
def border(im, col):
    for x in range(1, 11): im.putpixel((x, 1), col); im.putpixel((x, 16), col)
    for y in range(1, 17): im.putpixel((1, y), col); im.putpixel((10, y), col)
    for x in range(12, 22): im.putpixel((x, 1), col); im.putpixel((x, 16), col)
    for y in range(1, 17): im.putpixel((12, y), col); im.putpixel((21, y), col)
def cape(name, fn, b):
    im = Image.new('RGBA', (64, 32))
    for y in range(32):
        for x in range(64): im.putpixel((x, y), tuple(max(0, min(255, int(v))) for v in fn(x, y)) + (255,))
    border(im, b + (255,))
    for d in (f'{MOD_ASSETS}/textures/cape', f'{UI_ASSETS}/capes'): os.makedirs(d, exist_ok=True); im.save(f'{d}/{name}.png')
random.seed(11)
coral_dots = {(random.randint(0, 63), random.randint(0, 31)): random.choice([(255, 111, 97), (255, 210, 63), (244, 166, 200), (95, 242, 200)]) for _ in range(120)}
cape('coral', lambda x, y: coral_dots.get((x, y), (20 + 10 * math.sin(x * .3), 120 + 40 * math.sin(y * .3 + x * .1), 170 + 40 * math.sin(y * .2))), (255, 111, 97))
random.seed(12); glows = {(random.randint(0, 63), random.randint(0, 31)) for _ in range(25)}
cape('deepsea', lambda x, y: (120, 255, 230) if (x, y) in glows else (5 + y, 20 + y * 2, 50 + y * 3), (62, 230, 255))
cape('sunset', lambda x, y: (255, 120 + y * 3, 60 + y * 2) if y < 17 else (60, 40 + y, 120 + y), (255, 210, 63))
cape('lava', lambda x, y: (200 + 55 * math.sin(x * .7 + y * .4), 60 + 60 * max(0, math.sin(x * .5 - y * .8)), 20), (255, 160, 40))
cape('emerald', lambda x, y: (20 + 30 * ((x + y) % 4 == 0), 150 + 60 * math.sin(x * .6 + y * .3), 80 + 30 * math.sin(y)), (180, 255, 200))
cape('amethyst', lambda x, y: (140 + 50 * math.sin(x * .9 + y * .5), 60 + 30 * math.sin(y * .7), 200 + 40 * math.sin(x * .4)), (230, 190, 255))

# ================================================================ ЗНАЧКИ У НИКА (8×8)
ICONS = [
 ('turtle', 'Turtle', 'Черепаха', '🐢', ["..GGGG..", ".GPGGPG.", "HGGGGGG.", "EHGPGGG.", "HHGGGGPG", ".GGGGGG.", ".F....F.", "F......F"], {'G': (47, 125, 58), 'P': (111, 191, 91), 'H': (111, 191, 91), 'E': (20, 40, 20), 'F': (90, 168, 74)}),
 ('pearl', 'Pearl', 'Жемчуг', '🔮', ["..####..", ".#tTTt#.", "#tTggTt#", "#TgGGgT#", "#TgGGgT#", "#tTggTt#", ".#tTTt#.", "..####.."], {'#': (11, 59, 54), 't': (19, 95, 85), 'T': (34, 145, 127), 'g': (60, 201, 166), 'G': (139, 245, 217)}),
 ('fish', 'Fish', 'Рыбка', '🐟', ["........", "...OO...", "..OOOO.O", ".EOWOOOO", "OOOWOOOO", "..OOOO.O", "...OO...", "........"], {'O': (255, 138, 61), 'W': (255, 255, 255), 'E': (20, 20, 30)}),
 ('heart', 'Heart', 'Сердце', '❤️', [".RR..RR.", "RLRRRRRR", "RLRRRRRR", "RRRRRRRR", ".RRRRRR.", "..RRRR..", "...RR...", "........"], {'R': (230, 50, 80), 'L': (255, 170, 190)}),
 ('star', 'Star', 'Звезда', '⭐', ["...YY...", "...YY...", "YYYYYYYY", ".YYYYYY.", "..YYYY..", ".YY..YY.", "YY....YY", "........"], {'Y': (255, 216, 60)}),
 ('crown', 'Crown', 'Корона', '👑', ["........", "Y..Y..Y.", "YY.YY.YY", "YYYYYYYY", "YRYYBYRY", "YYYYYYYY", "........", "........"], {'Y': (242, 194, 48), 'R': (216, 30, 60), 'B': (46, 111, 224)}),
 ('shell', 'Shell', 'Ракушка', '🐚', ["...PP...", "..PLLP..", ".PLPPLP.", "PLPLLPLP", "PLPLLPLP", ".PPPPPP.", "..P..P..", "........"], {'P': (244, 166, 200), 'L': (255, 225, 235)}),
 ('wave', 'Wave', 'Волна', '🌊', ["........", "..WWW...", ".WAAAW..", "WAA..AW.", "AA.....A", "AAA..AAA", "BBBBBBBB", "BBBBBBBB"], {'W': (255, 255, 255), 'A': (62, 230, 255), 'B': (23, 132, 173)}),
 ('diamond', 'Diamond', 'Алмаз', '💎', ["..CCCC..", ".CWCCCC.", "CCCCCCCC", ".CCCCCC.", "..CCCC..", "...CC...", "........", "........"], {'C': (127, 233, 255), 'W': (255, 255, 255)}),
 ('flame', 'Flame', 'Огонь', '🔥', ["...R....", "..RR.R..", ".RRORR..", ".ROOORR.", "RROYYORR", "ROYYYYOR", ".RYYYYR.", "..RRRR.."], {'R': (230, 60, 30), 'O': (255, 140, 30), 'Y': (255, 220, 80)}),
 ('bolt', 'Lightning', 'Молния', '⚡', ["....YY..", "...YY...", "..YY....", ".YYYYYY.", "....YY..", "...YY...", "..YY....", ".Y......"], {'Y': (255, 216, 60)}),
 ('moon', 'Moon', 'Луна', '🌙', ["..YYY...", ".YY.....", "YY......", "YY......", "YY......", ".YY.....", "..YYY...", "........"], {'Y': (255, 236, 150)}),
 ('sun', 'Sun', 'Солнце', '☀️', ["Y..Y..Y.", ".YYYYY..", ".YOOOY..", "YYOOOYYY", ".YOOOY..", ".YYYYY..", "Y..Y..Y.", "........"], {'Y': (255, 216, 60), 'O': (255, 160, 40)}),
 ('skull', 'Skull', 'Череп', '💀', [".WWWWWW.", "WWWWWWWW", "WKKWWKKW", "WKKWWKKW", "WWWKKWWW", ".WWWWWW.", ".W.W.W..", "........"], {'W': (240, 240, 240), 'K': (20, 20, 25)}),
 ('clover', 'Clover', 'Клевер', '🍀', [".GG..GG.", "GGGGGGGG", "GGGGGGGG", ".GGGGGG.", "GGGGGGGG", "GGG.GGGG", ".G..B...", "....B..."], {'G': (60, 190, 80), 'B': (90, 60, 30)}),
 ('music', 'Music', 'Музыка', '🎵', ["...BBBBB", "...B...B", "...B...B", "...B...B", ".BBB.BBB", "BBBB.BBB", "BBB..BB.", "........"], {'B': (62, 230, 255)}),
 ('snowflake', 'Snowflake', 'Снежинка', '❄️', ["...W....", ".W.W.W..", "..WWW...", "WWWWWWW.", "..WWW...", ".W.W.W..", "...W....", "........"], {'W': (210, 245, 255)}),
 ('anchor', 'Anchor', 'Якорь', '⚓', ["...GG...", "...GG...", ".GGGGGG.", "...GG...", "G..GG..G", "GG.GG.GG", ".GGGGGG.", "...GG..."], {'G': (160, 180, 200)}),
 ('dolphin', 'Dolphin', 'Дельфин', '🐬', ["....G...", "...GG...", ".GGGGGG.", "GEGGGGGGG", "GGLLLLGG", ".LLLL.GG", "......G.", "........"], {'G': (109, 143, 166), 'L': (201, 219, 230), 'E': (14, 26, 34)}),
 ('gem', 'Ruby', 'Рубин', '♦️', ["...RR...", "..RLRR..", ".RLRRRR.", "RRRRRRRR", ".RRRRRR.", "..RRRR..", "...RR...", "........"], {'R': (216, 30, 60), 'L': (255, 140, 160)}),
]
providers = []
os.makedirs(f'{MOD_ASSETS}/textures/font', exist_ok=True); os.makedirs(f'{MOD_ASSETS}/font', exist_ok=True); os.makedirs(f'{UI_ASSETS}/icons', exist_ok=True)
for i, (id_, en, ru, e, rows, pal) in enumerate(ICONS):
    im = Image.new('RGBA', (8, 8), (0, 0, 0, 0))
    for y, r in enumerate(rows[:8]):
        for x, ch in enumerate(r[:8]):
            if ch in pal: im.putpixel((x, y), pal[ch] + (255,))
    im.save(f'{MOD_ASSETS}/textures/font/{id_}.png')
    im.resize((32, 32), Image.NEAREST).save(f'{UI_ASSETS}/icons/{id_}.png')
    providers.append({'type': 'bitmap', 'file': f'aquaclient:font/{id_}.png', 'ascent': 7, 'height': 8, 'chars': [chr(0xE000 + i)]})
    add('icon', id_, en, ru, e)
json.dump({'providers': providers}, open(f'{MOD_ASSETS}/font/icons.json', 'w'), indent=1)

# ================================================================ ДОВОДКА
for it in ITEMS['face']:   # лицо: впереди второго слоя скина (z < -4.5), дужки — снаружи головы
    for grp in it.get('g', []):
        for bx in grp['b']:
            if bx[2] < -4.0: bx[2] = round(bx[2] - .45, 3)
            if bx[5] < -4.0: bx[5] = round(bx[5] - .45, 3)
            if bx[3] <= -4.15: bx[0] = round(bx[0] - .4, 3); bx[3] = round(bx[3] - .4, 3)
            elif bx[0] >= 4.15: bx[0] = round(bx[0] + .4, 3); bx[3] = round(bx[3] + .4, 3)
for it in ITEMS['pet']:    # питомцы крупнее, с бликом в глазах
    for grp in it.get('g', []):
        grp['o'] = [-15, -7.5, 2]
        extra = []
        for bx in grp['b']:
            for k in range(6): bx[k] = round(bx[k] * 1.5, 3)
            if bx[6] == g('BLACK') and (bx[3] - bx[0]) < 1.2 and (bx[4] - bx[1]) < 1.2:
                extra.append(B(bx[0], bx[1], bx[2] - .02, bx[0] + (bx[3] - bx[0]) * .45, bx[1] + (bx[4] - bx[1]) * .45, bx[5] + .02, 0xFFFFFFFF))
        grp['b'] += extra

# ================================================================ СВЕЧЕНИЕ
GLOW_ITEMS = {'halo', 'halo_blue', 'visor', 'visor_red', 'visor_green', 'fairy', 'fairy_pink', 'fairy_gold', 'jelly', 'ghost', 'squid'}
GLOW_COLORS = {g('MAGENTA'), 0xC0FF9A2E, 0xE0FFE066, 0xF0FFFFFF, 0xFF9FFFF0, 0xFF3CC9A6, 0xFF0B3B36, 0xFF5A2A7A, 0xFF9FE870}
for slot in ('hat', 'face', 'wings', 'back', 'pet'):
    for it in ITEMS[slot]:
        for grp in it.get('g', []):
            for bx in grp['b']:
                if len(bx) > 7: continue
                if it['id'] in GLOW_ITEMS or bx[6] in GLOW_COLORS: bx.append(1)

# ================================================================ ВЫВОД
data = {'slots': list(ITEMS.keys()), 'items': ITEMS, 'icons': [i[0] for i in ICONS]}
for s, lst in ITEMS.items(): print(s, len(lst) - 1)
os.makedirs('/tmp/wingsprites', exist_ok=True)
for k, im in SPRITES.items(): im.resize((im.width * 2, im.height * 2), Image.NEAREST).save(f'/tmp/wingsprites/{k}.png')
json.dump(data, open(f'{MOD_ASSETS}/cosmetics.json', 'w'), separators=(',', ':'))
json.dump(data, open(f'{UI_ASSETS}/cosmetics.json', 'w'), separators=(',', ':'))
open(f'{UI_ASSETS}/cosmetics.js', 'w').write('window.COSMETICS_DATA = ' + json.dumps(data, separators=(',', ':'), ensure_ascii=False) + ';\n')
