#!/usr/bin/env python3
"""Moves big engine data out of app/src/main/assets into packs-src/<id>/ (run once).
Pack definitions are shared with tools/make-packs.py."""
import os, shutil, sys, json
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
A = os.path.join(ROOT, 'app/src/main/assets'); P = os.path.join(ROOT, 'packs-src')
F = lambda *ids: [i + '.flitevox' for i in ids]
PACKS = [
 dict(id='flite-en-us-1', target='flite-extra', title='American/Canadian English CMU ARCTIC, part 1 (bdl, clb, jmk, aew)', files={f: 'flite-extra/'+f for f in F('cmu_us_bdl','cmu_us_clb','cmu_us_jmk','cmu_us_aew')}),
 dict(id='flite-en-us-2', target='flite-extra', title='American English CMU ARCTIC, part 2 (ahw, eey, fem, ljm, lnh, rxr)', files={f: 'flite-extra/'+f for f in F('cmu_us_ahw','cmu_us_eey','cmu_us_fem','cmu_us_ljm','cmu_us_lnh','cmu_us_rxr')}),
 dict(id='flite-en-in', target='flite-extra', title='Indian-accent English CMU ARCTIC (ksp, aup, axb, gka, slp)', files={f: 'flite-extra/'+f for f in F('cmu_us_ksp','cmu_us_aup','cmu_us_axb','cmu_us_gka','cmu_us_slp')}),
 dict(id='flite-indic-hi', target='flite-extra', title='Hindi (CMU Indic)', files={f: 'flite-extra/'+f for f in F('cmu_indic_hin_ab')}),
 dict(id='flite-indic-bn', target='flite-extra', title='Bengali (CMU Indic)', files={f: 'flite-extra/'+f for f in F('cmu_indic_ben_rm')}),
 dict(id='flite-indic-gu', target='flite-extra', title='Gujarati (CMU Indic, 3 voices)', files={f: 'flite-extra/'+f for f in F('cmu_indic_guj_ad','cmu_indic_guj_dp','cmu_indic_guj_kt')}),
 dict(id='flite-indic-kn', target='flite-extra', title='Kannada (CMU Indic)', files={f: 'flite-extra/'+f for f in F('cmu_indic_kan_plv')}),
 dict(id='flite-indic-mr', target='flite-extra', title='Marathi (CMU Indic, 2 voices)', files={f: 'flite-extra/'+f for f in F('cmu_indic_mar_aup','cmu_indic_mar_slp')}),
 dict(id='flite-indic-pa', target='flite-extra', title='Punjabi (CMU Indic)', files={f: 'flite-extra/'+f for f in F('cmu_indic_pan_amp')}),
 dict(id='flite-indic-ta', target='flite-extra', title='Tamil (CMU Indic)', files={f: 'flite-extra/'+f for f in F('cmu_indic_tam_sdr')}),
 dict(id='flite-indic-te', target='flite-extra', title='Telugu (CMU Indic, 3 voices)', files={f: 'flite-extra/'+f for f in F('cmu_indic_tel_kpn','cmu_indic_tel_sk','cmu_indic_tel_ss')}),
 dict(id='mbrola-english', target='mbrola-v120', title='MBROLA English (EN1, US1, US2, US3)', files={i: 'mbrola/'+i for i in ('en1','us1','us2','us3')}),
]
OJ = 'openjtalk/open_jtalk_dic_utf_8-1.11/'
PACKS.append(dict(id='openjtalk-ja', target='openjtalk', title='Open JTalk Japanese (dictionary and Nitech voice)',
  files=dict([('dict/'+n, OJ+n) for n in ('char.bin','left-id.def','matrix.bin','pos-id.def','rewrite.def','right-id.def','sys.dic','unk.dic')] + [('voice/nitech_jp_atr503_m001.htsvoice','openjtalk/voice/nitech_jp_atr503_m001.htsvoice')])))
EK = {}
for d, _, fs in os.walk(os.path.join(A, 'ekho')):
    rel = os.path.relpath(d, os.path.join(A, 'ekho'))
    if rel.split(os.sep)[0] == 'licenses': continue
    for f in fs: EK[os.path.normpath(os.path.join(rel, f))] = 'ekho/' + os.path.normpath(os.path.join(rel, f))
PACKS.append(dict(id='ekho-cantonese', target='ekho-160', title='Ekho Cantonese (CantoneseWong syllables)', files=EK, extra={'installed': b'\x01'}))
if __name__ == '__main__':
    if sys.argv[1:] == ['json']:
        print(json.dumps([{k: v for k, v in p.items() if k != 'extra'} for p in PACKS])); sys.exit()
    for p in PACKS:
        for dst, src in p['files'].items():
            s = os.path.join(A, src); d = os.path.join(P, p['id'], dst)
            if not os.path.exists(s): print('MISSING', s); continue
            os.makedirs(os.path.dirname(d), exist_ok=True); shutil.move(s, d)
        for n, b in p.get('extra', {}).items():
            d = os.path.join(P, p['id'], n); os.makedirs(os.path.dirname(d), exist_ok=True); open(d, 'wb').write(b)
    print('moved')
