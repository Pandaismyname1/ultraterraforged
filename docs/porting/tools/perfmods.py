"""Which builds the performance mods have, per Minecraft version and loader, from the Modrinth API (AllTheLeaks, which
is only on CurseForge, through the CFWidget API). Used to update PerformanceMods and the dev runs when porting:

    python docs/porting/tools/perfmods.py                  # the table for every version below
    python docs/porting/tools/perfmods.py 1.21.1 fabric    # the newest build of each for one version and loader,
                                                           # with the Modrinth version id to pin in build.gradle
"""
import json, sys, urllib.request

VERSIONS = ['1.20.1', '1.21.1', '1.21.4', '1.21.5', '1.21.8', '1.21.10', '1.21.11', '26.1', '26.1.2', '26.2', '26.3']
LOADERS = ['fabric', 'forge', 'neoforge']
# the mods and the ports or forks that stand in for them
MODRINTH = ['c2me-fabric', 'c2me-neoforge', 'c2me-ocl', 'noisiumed', 'noisium', 'lithium', 'radium', 'canary',
            'modernfix', 'sodium', 'embeddium', 'connector', 'forgified-fabric-api']
CURSEFORGE = ['alltheleaks']


def get(url):
    return json.load(urllib.request.urlopen(urllib.request.Request(url, headers={'User-Agent': 'ReTerraForged-porting-tools'})))


def modrinth(slug):
    """(game version, loader) -> newest version, newest first as the API lists them."""
    best = {}
    for v in get(f'https://api.modrinth.com/v2/project/{slug}/version'):
        for g in v['game_versions']:
            for l in v['loaders']:
                best.setdefault((g, l), {'number': v['version_number'], 'id': v['id'], 'type': v['version_type'],
                                         'file': next((f for f in v['files'] if f['primary']), v['files'][0])['filename']})
    return best


def curseforge(slug):
    best = {}
    data = get(f'https://api.cfwidget.com/minecraft/mc-mods/{slug}')
    for f in data['files']:
        games = [v for v in f['versions'] if v[:1].isdigit()]
        loaders = [v.lower() for v in f['versions'] if v in ('Forge', 'NeoForge', 'Fabric')]
        for g in games:
            for l in loaders:
                best.setdefault((g, l), {'number': f['name'], 'id': f"curse.maven:{slug}-{data['id']}:{f['id']}", 'type': f['type'], 'file': f['name']})
    return best


def main():
    mods = {slug: modrinth(slug) for slug in MODRINTH}
    mods.update({slug: curseforge(slug) for slug in CURSEFORGE})
    if len(sys.argv) == 3:
        game, loader = sys.argv[1:]
        for slug, builds in mods.items():
            b = builds.get((game, loader))
            print(f'{slug:22} ' + (f"{b['number']:40} {b['type']:8} {b['id']}" if b else '-'))
        return
    for slug, builds in mods.items():
        print(slug)
        for g in VERSIONS:
            row = [f"{l}={builds[(g, l)]['number']}" + ('' if builds[(g, l)]['type'] == 'release' else f" ({builds[(g, l)]['type']})")
                   for l in LOADERS if (g, l) in builds]
            if row:
                print(f'  {g:8} ' + '  '.join(row))


if __name__ == '__main__':
    main()
