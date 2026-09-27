"""Show how members of Minecraft classes change across versions.

Usage (from a directory containing maps/<version>.json made by dumpjar.py):
    python q.py 'world/level/levelgen/NoiseChunk:<init>' 'client/Minecraft:setScreen' ...

Each argument is `<class path under net/minecraft/>[:<member name prefix>]`. An empty prefix
lists every member. Output prints a line only when the matching members change.
"""
import json
import sys

VERSIONS = ['1.20.1', '1.21.1', '26.1', '26.1.2', '26.2', '26.3']
D = {v: json.load(open(f'maps/{v}.json')) for v in VERSIONS}

for arg in sys.argv[1:]:
    cls, _, pre = arg.partition(':')
    print('##', cls, pre)
    last = None
    for v in VERSIONS:
        c = D[v].get('net/minecraft/' + cls)
        if c is None:
            s = 'GONE'
        else:
            items = sorted([m for m in c['methods'] if m.startswith(pre)]
                           + ['F ' + f + ':' + d[0] for f, d in c['fields'].items() if f.startswith(pre)])
            s = ' | '.join(items)
            for p in ('Lnet/minecraft/', 'Ljava/util/', 'Ljava/lang/', 'Lcom/mojang/serialization/'):
                s = s.replace(p, '')
        if s != last:
            print('  ', v, s[:1500])
        last = s
