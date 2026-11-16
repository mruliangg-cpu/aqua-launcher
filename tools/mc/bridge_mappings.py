#!/usr/bin/env python3
"""Мост имён для 26.x: tiny2 (official 26.x -> intermediary -> named=Yarn 1.21.11).
intermediary у Fabric стабилен между версиями: классы/методы из 1.21.11 имеют те же intermediary-id в 26.x.
Склеиваем по intermediary. Новые символы 26.x остаются на intermediary-имени.

intermediary-jar = tiny v1 (official, intermediary); yarn-jar = tiny v2 (intermediary, named).
Запуск в CI: python3 tools/mc/bridge_mappings.py 26.3 1.21.11 out.tiny
"""
import io, json, sys, urllib.request, zipfile

UA = {'User-Agent': 'aqua-launcher-ci (github.com/mruliangg-cpu/aqua-launcher)'}
def getj(url): return json.load(urllib.request.urlopen(urllib.request.Request(url, headers=UA), timeout=60))
def getb(url): return urllib.request.urlopen(urllib.request.Request(url, headers=UA), timeout=180).read()

def tiny_text(coord):
    g, a, v = coord.split(':')
    base = f'https://maven.fabricmc.net/{g.replace(".", "/")}/{a}/{v}'
    last = None
    for url in (f'{base}/{a}-{v}-v2.jar', f'{base}/{a}-{v}.jar'):
        try:
            z = zipfile.ZipFile(io.BytesIO(getb(url)))
            name = next(n for n in z.namelist() if n.endswith('mappings.tiny'))
            return z.read(name).decode('utf-8')
        except Exception as e:
            last = f'{url}: {e}'
    raise RuntimeError(f'нет jar для {coord} ({last})')

def parse_v1(text):
    """intermediary: v1\tofficial\tintermediary ; CLASS/METHOD/FIELD."""
    cls = {}   # official -> intermediary
    mem = []   # (type, owner_official, desc, official_name, intermediary_name)
    for ln in text.split('\n'):
        if not ln or ln.startswith('v1\t'):
            continue
        p = ln.split('\t')
        if p[0] == 'CLASS' and len(p) >= 3:
            cls[p[1]] = p[2]
        elif p[0] in ('METHOD', 'FIELD') and len(p) >= 5:
            mem.append((p[0].lower()[0], p[1], p[2], p[3], p[4]))
    return cls, mem

def parse_yarn_v2(text):
    """yarn v2: namespaces intermediary, named (нет official). c/m/f с отступами."""
    cls = {}    # intermediary -> named
    mem = {}    # (owner_inter, member_inter) -> named
    cur = None
    for ln in text.split('\n'):
        if not ln or ln.startswith('tiny\t'):
            continue
        p = ln.strip().split('\t')
        if ln[0] == 'c':
            cur = p[1]  # intermediary
            if len(p) >= 3:
                cls[p[1]] = p[2]
        elif p[0] in ('m', 'f') and cur is not None and len(p) >= 4:
            # m <desc> <inter> <named>
            mem[(cur, p[2])] = p[3]
    return cls, mem

def main():
    target, base, out = sys.argv[1], sys.argv[2], sys.argv[3]
    inter_coord = next(i['maven'] for i in getj('https://meta.fabricmc.net/v2/versions/intermediary') if i['version'] == target)
    print(f'intermediary coord: {inter_coord}', file=sys.stderr)
    itext = tiny_text(inter_coord)
    print('INTER HEAD:\n' + '\n'.join(repr(l) for l in itext.split('\n')[:4]), file=sys.stderr)
    icls, imem = parse_v1(itext)
    yarn_ver = getj(f'https://meta.fabricmc.net/v2/versions/yarn/{base}')[0]['version']
    print(f'yarn base: {yarn_ver}', file=sys.stderr)
    ycls, ymem = parse_yarn_v2(tiny_text(f'net.fabricmc:yarn:{yarn_ver}'))
    print(f'intermediary classes={len(icls)} members={len(imem)}; yarn classes={len(ycls)} members={len(ymem)}', file=sys.stderr)

    # члены intermediary сгруппировать по official-владельцу
    by_owner = {}
    for typ, owner_off, desc, off_name, inter_name in imem:
        by_owner.setdefault(owner_off, []).append((typ, desc, off_name, inter_name))

    o = ['tiny\t2\t0\tofficial\tintermediary\tnamed']
    hits = 0
    for off, inter in icls.items():
        named = ycls.get(inter, inter)
        if inter in ycls:
            hits += 1
        o.append(f'c\t{off}\t{inter}\t{named}')
        for typ, desc, off_name, inter_name in by_owner.get(off, ()):
            m_named = ymem.get((inter, inter_name), inter_name)
            o.append(f'\t{typ}\t{desc}\t{off_name}\t{inter_name}\t{m_named}')
    open(out, 'w', encoding='utf-8').write('\n'.join(o) + '\n')
    print(f'написал {out}: {len(icls)} классов, из них с Yarn-именем {hits}', file=sys.stderr)

if __name__ == '__main__':
    main()
