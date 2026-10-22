#!/usr/bin/env python3
"""Re-run isolated request/response JSON serialization before and after R8."""
from pathlib import Path
import json, subprocess
root = Path(__file__).resolve().parent
paths = json.loads((root / 'paths.json').read_text())
for variant in ['old', 'fixed']:
    cmd = [paths['jdk'] + '/bin/java', '-cp', paths['r8'], 'com.android.tools.r8.R8',
           '--classfile', '--release', '--output', str(root / (variant + '-minified.jar')),
           '--pg-conf', str(root / 'rules.pro'), '--pg-map-output', str(root / (variant + '-mapping.txt')),
           '--lib', paths['jdk']]
    for dependency in ['gson', 'kotlin', 'annotation', 'jetbrains']:
        cmd += ['--lib', paths[dependency]]
    cmd += [str(root / (variant + '-input.jar'))]
    shrunk = subprocess.run(cmd, capture_output=True, text=True)
    (root / (variant + '-shrink.log')).write_text(shrunk.stdout + shrunk.stderr)
    shrunk.check_returncode()
    cmd = [paths['jdk'] + '/bin/java', '-cp', ':'.join([str(root / (variant + '-minified.jar')),
           paths['gson'], paths['kotlin']]), 'HabitWireProbe', 'require-pass']
    probe = subprocess.run(cmd, capture_output=True, text=True)
    output = probe.stdout + probe.stderr
    (root / (variant + '-probe.log')).write_text(output)
    print(variant + ': exit ' + str(probe.returncode) + '\n' + output)
    if variant == 'old':
        assert probe.returncode != 0 and 'requestKeyPreserved=false' in output, 'Old DTO should reproduce failure'
    else:
        assert probe.returncode == 0 and 'contract=PASS' in output, 'Fixed DTO should pass after shrinking'
print('Before/after minified regression passed')
