#!/usr/bin/env python3
"""تشغيل الاختبارات المثبتة بشهادة التطبيق على جهاز اختبار منفصل.
تغيّر الاختبارات التفضيلات وتكتب عينات. اختبار Export يحتاج حالة أذونات أولية
غير ممنوحة؛ استخدم --reset-storage-permissions معه فقط على جهاز الاختبار.
"""
import argparse
import json
import subprocess
import time
from pathlib import Path

p = argparse.ArgumentParser(description=__doc__)
p.add_argument('--adb', default='adb')
p.add_argument('--output', type=Path, default=Path('android-test-results'))
p.add_argument('--runners', nargs='*', default=['SimpleTts', 'FliteExpansion', 'FestivalExpansion', 'Ekho', 'OpenJtalk', 'ArabicExpansion', 'Standalone', 'Localization', 'Rawi', 'ManualDiacritization', 'AllVoices48k'])
p.add_argument('--reset-storage-permissions', action='store_true', help='سحب أذونات التخزين قبل Export على جهاز اختبار فقط')
p.add_argument('--export-mode', choices=['grant','deny'], default='grant', help='Export permission scenario')
args = p.parse_args()
args.output.mkdir(parents=True, exist_ok=True)
summary = []
for name in args.runners:
    if name == 'Export' and args.reset_storage_permissions:
        for permission in ['READ_EXTERNAL_STORAGE', 'WRITE_EXTERNAL_STORAGE']:
            subprocess.run([args.adb, 'shell', 'pm', 'revoke', 'org.oldtts.android', 'android.permission.' + permission], check=True)
        subprocess.run([args.adb, 'shell', 'am', 'force-stop', 'org.oldtts.android'], check=True)
    component = 'org.oldtts.android.test/org.retro.flitetts.' + name + 'Instrumentation'
    print('START ' + name, flush=True)
    start = time.monotonic()
    command = [args.adb, 'shell', 'am', 'instrument', '-r', '-w']
    if name == 'Export':
        command += ['-e', 'mode', args.export_mode]
    command.append(component)
    process = subprocess.Popen(command, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True)
    lines = []
    for line in process.stdout:
        print(line, end='', flush=True)
        lines.append(line)
    rc = process.wait()
    text = ''.join(lines)
    (args.output / ('test-' + name + '.txt')).write_text(text)
    # Some custom UI runners finish with their PASS stream but no terminal code.
    # Never treat an explicit failure, cancelled code, or error result as success.
    ok = rc == 0 and 'PASS' in text and 'FAIL' not in text and 'error=' not in text and 'INSTRUMENTATION_CODE: 0' not in text
    summary.append({'runner': name, 'passed': ok, 'seconds': round(time.monotonic() - start, 2)})
    (args.output / 'instrumentation-summary.json').write_text(json.dumps(summary, indent=2) + '\n')
    if not ok:
        raise SystemExit('FAIL ' + name)
    print('PASS ' + name, flush=True)
print('PASS all requested instrumentations', flush=True)
