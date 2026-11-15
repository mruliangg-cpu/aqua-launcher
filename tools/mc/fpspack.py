"""Скачивает FPS-пакет лаунчера (Sodium, Lithium и т.д.) с Modrinth в run/mods — для smoke-теста совместимости."""
import json, os, sys, urllib.request, urllib.parse

MC = sys.argv[1]
# В dev-окружении старых версий другой LWJGL, и Sodium 0.5 отказывается стартовать (у игроков такого нет).
DEV_SKIP = {'sodium'} if MC.startswith('1.20') else set()
SLUGS = ['sodium', 'lithium', 'ferrite-core', 'entityculling', 'immediatelyfast', 'modernfix', 'dynamic-fps', 'moreculling', 'cloth-config', 'badoptimizations', 'krypton']
SKIP = {'P7dR8mSH'}  # fabric-api уже есть в dev-окружении
os.makedirs('run/mods', exist_ok=True)

def get(url):
    req = urllib.request.Request(url, headers={'User-Agent': 'aqua-launcher-ci'})
    with urllib.request.urlopen(req, timeout=60) as r: return json.load(r)

done = set()
def install(pid, required):
    if pid in done or pid in SKIP: return
    done.add(pid)
    q = urllib.parse.urlencode({'loaders': '["fabric"]', 'game_versions': json.dumps([MC])})
    vs = get(f'https://api.modrinth.com/v2/project/{pid}/version?{q}')
    if not vs:
        print(f'FPSPACK skip {pid}: no build for {MC}'); return
    v = vs[0]
    f = next((x for x in v['files'] if x.get('primary')), v['files'][0])
    urllib.request.urlretrieve(f['url'], os.path.join('run/mods', f['filename']))
    print(f"FPSPACK {pid} -> {f['filename']}")
    for d in v.get('dependencies', []):
        if d.get('dependency_type') != 'required': continue
        pid2 = d.get('project_id') or (get(f"https://api.modrinth.com/v2/version/{d['version_id']}")['project_id'] if d.get('version_id') else None)
        if pid2: install(pid2, True)

for s in [x for x in SLUGS if x not in DEV_SKIP]:
    try: install(get(f'https://api.modrinth.com/v2/project/{s}')['id'], False)
    except Exception as e: print(f'FPSPACK fail {s}: {e}')
