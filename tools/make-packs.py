#!/usr/bin/env python3
"""Builds the downloadable voice-pack ZIPs from packs-src/ and the in-app catalog.
usage: make-packs.py <out-dir> [version]. Output: <out>/<id>.zip, <out>/PACKS-SHA256SUMS.txt and
app/src/main/assets/packs/catalog.json (manifest hashes are compiled into the app)."""
import os, sys, json, hashlib, zipfile
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import importlib.util
spec = importlib.util.spec_from_file_location('layout', os.path.join(os.path.dirname(os.path.abspath(__file__)), 'pack-layout.py'))
L = importlib.util.module_from_spec(spec); spec.loader.exec_module(L)
ROOT = L.ROOT; SRC = L.P
out = sys.argv[1]; version = sys.argv[2] if len(sys.argv) > 2 else '1.9.0'
os.makedirs(out, exist_ok=True)
def sha(path):
    h = hashlib.sha256()
    with open(path, 'rb') as f:
        for b in iter(lambda: f.read(1 << 20), b''): h.update(b)
    return h.hexdigest()
catalog = []; sums = []
for p in L.PACKS:
    base = os.path.join(SRC, p['id']); files = []
    for dirpath, _, names in os.walk(base):
        for n in names:
            full = os.path.join(dirpath, n); rel = os.path.relpath(full, base).replace(os.sep, '/')
            if rel in ('manifest.json', 'README-PACK.txt'): continue
            files.append({'path': rel, 'size': os.path.getsize(full), 'sha256': sha(full)})
    files.sort(key=lambda f: f['path'])
    manifest = {'format': 1, 'pack': p['id'], 'version': version, 'target': p['target'], 'title': p['title'], 'files': files}
    mbytes = (json.dumps(manifest, indent=1, sort_keys=True) + '\n').encode()
    readme = ('Simple TTS voice pack: %s\n\nThis folder (%s) must be moved, with all its files, into the folder\n"Simple TTS data" in the root of the phone\'s internal storage, so that the path is\n"Simple TTS data/%s/manifest.json". Then open Simple TTS: it checks every file\n(SHA-256) and activates the voices. Nothing is uploaded; the app has no internet permission.\n') % (p['title'], p['id'], p['id'])
    zpath = os.path.join(out, p['id'] + '.zip')
    with zipfile.ZipFile(zpath, 'w', zipfile.ZIP_DEFLATED, compresslevel=9) as z:
        def add(name, data=None, path=None):
            zi = zipfile.ZipInfo(p['id'] + '/' + name, (2026, 1, 1, 0, 0, 0)); zi.compress_type = zipfile.ZIP_DEFLATED; zi.external_attr = 0o644 << 16
            if data is not None: z.writestr(zi, data, compresslevel=9)
            else:
                with open(path, 'rb') as f, z.open(zi, 'w') as w:
                    for b in iter(lambda: f.read(1 << 20), b''): w.write(b)
        add('manifest.json', mbytes); add('README-PACK.txt', readme.encode())
        for f in files: add(f['path'], path=os.path.join(base, f['path']))
    entry = {'id': p['id'], 'title': p['title'], 'target': p['target'], 'zip': p['id'] + '.zip', 'zipBytes': os.path.getsize(zpath), 'zipSha256': sha(zpath),
             'manifestSha256': hashlib.sha256(mbytes).hexdigest(), 'installBytes': sum(f['size'] for f in files), 'files': [{'path': f['path'], 'size': f['size']} for f in files]}
    catalog.append(entry); sums.append('%s  %s' % (entry['zipSha256'], entry['zip']))
    os.makedirs(base, exist_ok=True); open(os.path.join(base, 'manifest.json'), 'wb').write(mbytes)
    print(p['id'], entry['zipBytes'], entry['installBytes'])
open(os.path.join(out, 'PACKS-SHA256SUMS.txt'), 'w').write('\n'.join(sums) + '\n')
cat = os.path.join(ROOT, 'app/src/main/assets/packs'); os.makedirs(cat, exist_ok=True)
open(os.path.join(cat, 'catalog.json'), 'w').write(json.dumps(catalog, indent=1, sort_keys=True) + '\n')
