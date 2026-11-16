#!/usr/bin/env python3
"""Мост имён для 26.x: строит tiny2-маппинг (official 26.x -> intermediary -> named=Yarn 1.21.11).
Идея: intermediary у Fabric стабилен между версиями, поэтому классы/методы, которые были в 1.21.11,
получают там те же intermediary-id. Берём Yarn 1.21.11 (intermediary->named) и intermediary 26.x
(official->intermediary) и склеиваем по intermediary. Новые символы 26.x остаются на intermediary-имени.

Запускать в CI (нужна сеть): python3 tools/mc/bridge_mappings.py 26.3 1.21.11 out.tiny
"""
import io, json, re, sys, urllib.request, zipfile

UA = {'User-Agent': 'aqua-launcher-ci (github.com/mruliangg-cpu/aqua-launcher)'}

def getj(url):
    return json.load(urllib.request.urlopen(urllib.request.Request(url, headers=UA), timeout=60))

def getb(url):
    return urllib.request.urlopen(urllib.request.Request(url, headers=UA), timeout=120).read()

def maven_jar_url(coord):
    """net.fabricmc:intermediary:26.3 -> список возможных URL jar-а (с -v2 и без)."""
    g, a, v = coord.split(':')
    base = f'https://maven.fabricmc.net/{g.replace(".", "/")}/{a}/{v}'
    return [f'{base}/{a}-{v}-v2.jar', f'{base}/{a}-{v}.jar']

def tiny_from_coord(coord):
    last = None
    for url in maven_jar_url(coord):
        try:
            z = zipfile.ZipFile(io.BytesIO(getb(url)))
            name = next(n for n in z.namelist() if n.endswith('mappings.tiny'))
            print(f'   mappings: {url}', file=sys.stderr)
            return z.read(name).decode('utf-8')
        except Exception as e:
            last = f'{url}: {e}'
    raise RuntimeError(f'нет jar для {coord} ({last})')

def parse_tiny(text):
    """tiny v2 -> (namespaces, classes[list of (names...)], per-class members).
    Возвращаем словари по первому неймспейсу-ключу (мы join-им по intermediary)."""
    lines = text.split('\n')
    header = lines[0].split('\t')
    ns = header[3:]  # пропускаем 'tiny', major, minor — дальше имена неймспейсов
    classes = {}  # key: tuple of class names by index; store member rows
    cur = None
    data = []  # (type, names-by-ns, owner_index_names)
    cls_rows = []
    mem_rows = []
    for ln in lines[1:]:
        if not ln.strip():
            continue
        parts = ln.split('\t')
        indent = len(ln) - len(ln.lstrip('\t'))
        parts = ln.strip().split('\t')
        if parts[0] == 'c' and indent == 0:
            cur = parts[1:]  # class names by ns
            cls_rows.append(cur)
        elif parts[0] in ('m', 'f') and cur is not None:
            # m <desc> <name_ns0> <name_ns1> ...
            desc = parts[1]
            names = parts[2:]
            mem_rows.append((parts[0], cur, desc, names))
    return ns, cls_rows, mem_rows

def main():
    target, base, out = sys.argv[1], sys.argv[2], sys.argv[3]
    # intermediary 26.x — берём координату из ОБЩЕГО списка (per-version endpoint отдаёт заглушку 0.0.0)
    inter_coord = next(i['maven'] for i in getj('https://meta.fabricmc.net/v2/versions/intermediary') if i['version'] == target)
    print(f'intermediary coord: {inter_coord}', file=sys.stderr)
    inter_ns, inter_cls, inter_mem = parse_tiny(tiny_from_coord(inter_coord))
    # yarn base (official, intermediary, named)
    yarn_ver = getj(f'https://meta.fabricmc.net/v2/versions/yarn/{base}')[0]['version']
    print(f'yarn base: {yarn_ver}', file=sys.stderr)
    yarn_ns, yarn_cls, yarn_mem = parse_tiny(tiny_from_coord(f'net.fabricmc:yarn:{yarn_ver}'))
    print(f'inter_ns={inter_ns}  yarn_ns={yarn_ns}', file=sys.stderr)

    iy = yarn_ns.index('intermediary'); ny = yarn_ns.index('named')
    # intermediary-name -> yarn-name (классы)
    cls_named = {row[iy]: row[ny] for row in yarn_cls if len(row) > ny and row[iy]}
    # (owner_inter, member_inter_name, desc?) -> yarn-name
    mem_named = {}
    for typ, owner, desc, names in yarn_mem:
        if len(names) > ny and len(owner) > iy:
            mem_named[(owner[iy], names[iy])] = names[ny]

    ii = inter_ns.index('intermediary')  # official=0, intermediary=1
    # сгруппировать члены по official-имени класса-владельца (быстро, без O(n^2))
    by_owner = {}
    for typ, owner, desc, names in inter_mem:
        by_owner.setdefault(owner[0], []).append((typ, desc, names))
    o = ['tiny\t2\t0\tofficial\tintermediary\tnamed']
    for row in inter_cls:
        off, inter = row[0], row[ii]
        o.append(f'c\t{off}\t{inter}\t{cls_named.get(inter, inter)}')
        for typ, desc, names in by_owner.get(off, ()):
            m_off, m_inter = names[0], names[ii]
            o.append(f'\t{typ}\t{desc}\t{m_off}\t{m_inter}\t{mem_named.get((inter, m_inter), m_inter)}')
    open(out, 'w', encoding='utf-8').write('\n'.join(o) + '\n')
    print(f'написал {out}: {len(inter_cls)} классов', file=sys.stderr)

if __name__ == '__main__':
    main()
