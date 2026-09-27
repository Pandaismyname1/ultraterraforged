"""Turn javac output from compile-census builds into per-version Markdown reports.

Usage:
    python census.py OUT_DIR LABEL=LOG [LABEL=LOG ...]

Each LOG is the console output of `./gradlew :common:compileJava` from census-build.sh.
Writes OUT_DIR/<LABEL>.md (all distinct errors) for the first log and, for each later log,
OUT_DIR/<LABEL>-new.md with only the errors absent from the previous log. The logs must be
given in version order.
"""
import collections
import os
import re
import sys

BS = chr(92)
ERR = re.compile(r'raccoonman[/' + BS + BS + r']reterraforged[/' + BS + BS + r'](.*?\.java):(\d+): error: (.*)')


def load(fn):
    txt = open(fn, encoding='utf-8', errors='replace').read().splitlines()
    out = {}
    for i, line in enumerate(txt):
        m = ERR.search(line)
        if not m:
            continue
        f = m.group(1).replace(BS, '/')
        ln, msg = m.group(2), m.group(3).strip()
        code = txt[i + 1].strip() if i + 1 < len(txt) else ''
        sym = ''
        for j in range(i + 1, min(len(txt), i + 8)):
            if 'error:' in txt[j]:
                break
            s = re.search(r'symbol:\s+(.*)', txt[j])
            if s:
                sym = s.group(1).strip()
        # gradle prints every error twice; the key dedupes them
        out[(f, msg, sym, code)] = ln
    return out


def write(path, title, errs):
    by = collections.defaultdict(list)
    for (f, msg, sym, code), ln in errs.items():
        by[f].append((int(ln), msg, sym, code))
    with open(path, 'w', encoding='utf-8', newline='\n') as o:
        o.write(f'# {title}\n\n{len(errs)} distinct errors in {len(by)} files. '
                'Paths are relative to `common/src/main/java/raccoonman/reterraforged/`. '
                'javac only reports the first wave; fixing these will surface more (override checks, generics).\n\n')
        for f in sorted(by):
            o.write(f'## {f}\n\n')
            for ln, msg, sym, code in sorted(by[f]):
                line = f'- L{ln}: {msg}'
                if sym:
                    line += f' — `{sym}`'
                if code:
                    line += f'  \n  `{code[:160].replace("`", "")}`'
                o.write(line + '\n')
            o.write('\n')
    print(path, len(errs), 'errors in', len(by), 'files')


def main():
    out_dir = sys.argv[1]
    os.makedirs(out_dir, exist_ok=True)
    runs = [a.split('=', 1) for a in sys.argv[2:]]
    prev = None
    for label, log in runs:
        errs = load(log)
        if prev is None:
            write(os.path.join(out_dir, f'{label}.md'), f'Compile census: current sources against MC {label}', errs)
        else:
            new = {k: v for k, v in errs.items() if k not in prev[1]}
            write(os.path.join(out_dir, f'{label}-new.md'), f'Compile census: errors new in {label} (absent against {prev[0]})', new)
        prev = (label, errs)


if __name__ == '__main__':
    main()
