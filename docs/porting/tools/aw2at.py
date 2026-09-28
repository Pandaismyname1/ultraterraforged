"""Writes neoforge/src/main/resources/META-INF/accesstransformer.cfg from the access widener.

From 26.1 the build doesn't remap, and Loom can't turn the widener into an access transformer on its own, so NeoForge's
transformer is a file in the repo; neoforge:checkAccessTransformer fails the build when it no longer matches. Run from
the repo root: python docs/porting/tools/aw2at.py
"""
import re

WIDENER = 'common/src/main/resources/ultraterraforged.accesswidener'
TRANSFORMER = 'neoforge/src/main/resources/META-INF/accesstransformer.cfg'

lines = []
for line in open(WIDENER, encoding='utf-8'):
    parts = re.sub(r'#.*', '', line).split()
    if len(parts) < 3 or parts[0] == 'accessWidener':
        continue
    access, kind, owner = parts[0], parts[1], parts[2].replace('/', '.')
    # accessible: public; extendable and mutable: public and not final
    modifier = 'public' if access == 'accessible' else 'public-f'
    if kind == 'class':
        lines.append(f'{modifier} {owner}')
    elif kind == 'method':
        lines.append(f'{modifier} {owner} {parts[3]}{parts[4]}')
    elif kind == 'field':
        lines.append(f'{modifier} {owner} {parts[3]}')

with open(TRANSFORMER, 'w', encoding='utf-8', newline='\n') as out:
    out.write('# Generated from ' + WIDENER + ' by docs/porting/tools/aw2at.py; edit the widener, then rerun it\n')
    out.write('\n'.join(lines) + '\n')
print(f'{len(lines)} entries -> {TRANSFORMER}')
