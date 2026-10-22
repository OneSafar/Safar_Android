from pathlib import Path
import gzip
import hashlib
import json
import re
import shutil
import subprocess
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[3]
logs = Path(__file__).resolve().parent
java_bin = Path('/Applications/Android Studio.app/Contents/jbr/Contents/Home/bin')
bundletool = Path('/private/tmp/safar-release-validation-20261019/bundletool-all-1.18.3.jar')
source = root / 'app/build/outputs/bundle/prodRelease/app-prod-release.aab'
target = root / 'Outputs/Safar-prod-release-v1.6.80-80.aab'
previous = root / 'Outputs/Safar-prod-release-v1.6.79-79.aab'

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
assert manifest.get(android+'versionCode') == '80'
assert manifest.get(android+'versionName') == '1.6.80'
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
mapping_target = root/'Outputs/Safar-prod-release-v1.6.80-80-mapping.txt.gz'
assert mapping.is_file(), 'R8 mapping missing'
assert 'com.safarparmar.app.ui.studyplanner.screens.PlannerGk5ButtonKt ->' in mapping.read_text(), 'GK 5.0 button missing from release mapping'
with mapping.open('rb') as source_mapping, gzip.open(mapping_target, 'wb') as output_mapping:
    shutil.copyfileobj(source_mapping, output_mapping)
report = {
    'artifact': str(target), 'package': manifest.get('package'),
    'versionCode': 80, 'versionName': '1.6.80', 'bytes': target.stat().st_size,
    'sha256': checksum, 'signingCertificateSha256': fingerprint,
    'signingMatchesVersion79': True, 'bundletoolValidation': 'passed',
    'jarSignatureVerification': 'passed', 'debuggable': False,
    'debugProbeActivitiesAbsent': True, 'animatedGk5ButtonPresent': True,
    'mapping': str(mapping_target), 'storeUploadPerformed': False,
}
(logs/'verification.json').write_text(json.dumps(report, indent=2)+'\n')
print(json.dumps(report, indent=2))
