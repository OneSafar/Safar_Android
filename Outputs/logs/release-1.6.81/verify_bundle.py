from pathlib import Path
import gzip
import hashlib
import json
import re
import shutil
import tempfile
import zipfile
import subprocess
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[3]
logs = Path(__file__).resolve().parent
java_bin = Path('/Applications/Android Studio.app/Contents/jbr/Contents/Home/bin')
bundletool = Path('/private/tmp/safar-release-validation-20261019/bundletool-all-1.18.3.jar')
source = root / 'app/build/outputs/bundle/prodRelease/app-prod-release.aab'
target = root / 'Outputs/Safar-prod-release-v1.6.81-81.aab'
previous = root / 'Outputs/Safar-prod-release-v1.6.80-80.aab'

def run(args, name):
    result = subprocess.run([str(a) for a in args], capture_output=True, text=True)
    (logs / name).write_text(result.stdout + result.stderr)
    if result.returncode:
        raise RuntimeError(f'{name} failed; see saved log')
    return result.stdout

assert source.is_file(), 'Production bundle missing'
run([java_bin/'java', '-jar', bundletool, 'validate', f'--bundle={source}'], 'bundle-validation.log')
xml = run([java_bin/'java', '-jar', bundletool, 'dump', 'manifest', f'--bundle={source}', '--module=base'], 'manifest.xml')
manifest = ET.fromstring(xml)
android = '{http://schemas.android.com/apk/res/android}'
assert manifest.get('package') == 'com.safarparmar.app'
assert manifest.get(android+'versionCode') == '81'
assert manifest.get(android+'versionName') == '1.6.81'
application = manifest.find('application')
assert application is not None and application.get(android+'debuggable', 'false') == 'false'
assert not any('ProbeActivity' in element.get(android+'name', '') for element in application.findall('activity'))

signature = run([java_bin/'jarsigner', '-verify', source], 'signature-verification.log')
assert 'jar verified.' in signature
new_cert = run([java_bin/'keytool', '-printcert', '-jarfile', source], 'certificate.txt')
old_cert = run([java_bin/'keytool', '-printcert', '-jarfile', previous], 'previous-certificate.txt')
certificate_pattern = r'SHA256:\s*([A-F0-9:]+)'
fingerprint = re.search(certificate_pattern, new_cert).group(1)
assert fingerprint == re.search(certificate_pattern, old_cert).group(1), 'Signing key changed'

shutil.copy2(source, target)
checksum = hashlib.sha256(target.read_bytes()).hexdigest()
Path(str(target)+'.sha256').write_text(f'{checksum}  {target.name}\n')
mapping = root/'app/build/outputs/mapping/prodRelease/mapping.txt'
mapping_target = root/'Outputs/Safar-prod-release-v1.6.81-81-mapping.txt.gz'
assert mapping.is_file(), 'R8 mapping missing'
mapping_text = mapping.read_text()
assert 'com.safarparmar.app.ui.studyplanner.screens.PlannerGk5ButtonKt ->' in mapping_text, 'GK 5.0 button missing from release mapping'
# These reflection-only models caused the habit save release regression.
for model, fields in {'HabitBatchRequest': ['mutations'], 'HabitBatchResponse': ['version', 'acknowledged']}.items():
    name = 'com.safarparmar.app.feature.habits.data.' + model
    header = name + ' -> ' + name + ':'
    assert header in mapping_text, model + ' removed or renamed'
# Unrenamed fields can be omitted from mapping.txt. Inspect the packaged DEX too.
models = {'HabitBatchRequest': ['mutations'], 'HabitBatchResponse': ['version', 'acknowledged']}
descriptors = {model: 'Lcom/safarparmar/app/feature/habits/data/' + model + ';' for model in models}
dex_blocks = {}
with tempfile.TemporaryDirectory(prefix='safar-v81-dex-') as temp, zipfile.ZipFile(source) as bundle:
    for entry in bundle.namelist():
        if not (entry.startswith('base/dex/') and entry.endswith('.dex')):
            continue
        data = bundle.read(entry)
        if not any(value.encode() in data for value in descriptors.values()):
            continue
        dex = Path(temp) / Path(entry).name
        dex.write_bytes(data)
        result = subprocess.run(['/Users/shashankkumar/Library/Android/sdk/build-tools/37.0.0/dexdump', '-a', str(dex)], capture_output=True, text=True, check=True)
        for block in re.split(r'(?m)^Class #\d+', result.stdout):
            for model, descriptor in descriptors.items():
                if "Class descriptor  : '" + descriptor + "'" in block:
                    dex_blocks[model] = block
for model, fields in models.items():
    assert model in dex_blocks, model + ' missing from packaged DEX'
    field_section = dex_blocks[model].split('Direct methods', 1)[0]
    for field in fields:
        assert re.search(r"name\s+: '" + field + "'", field_section), model + '.' + field + ' missing from packaged DEX'
    assert re.search(r"name\s+: '<init>'", dex_blocks[model]), model + ' constructor missing'
(logs / 'habit-batch-dex.txt').write_text('\n\n'.join(model + '\n' + block for model, block in dex_blocks.items()))
with mapping.open('rb') as source_mapping, gzip.open(mapping_target, 'wb') as output_mapping:
    shutil.copyfileobj(source_mapping, output_mapping)
report = {
    'artifact': str(target), 'package': manifest.get('package'),
    'versionCode': 81, 'versionName': '1.6.81', 'bytes': target.stat().st_size,
    'sha256': checksum, 'signingCertificateSha256': fingerprint,
    'signingMatchesVersion80': True, 'bundletoolValidation': 'passed',
    'jarSignatureVerification': 'passed', 'debuggable': False,
    'debugProbeActivitiesAbsent': True, 'animatedGk5ButtonPresent': True,
    'mapping': str(mapping_target), 'habitBatchModelsAndWireNamesRetained': True, 'storeUploadPerformed': False,
}
(logs/'verification.json').write_text(json.dumps(report, indent=2)+'\n')
print(json.dumps(report, indent=2))
