#!/bin/zsh
set -euo pipefail

project_dir='/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android'
cd "$project_dir"

set -a
source <(tr -d '\r' < SAFAR_release_signing_bundle/local.properties | grep '^SAFAR_RELEASE_')
set +a
export SAFAR_RELEASE_STORE_FILE="$project_dir/SAFAR_release_signing_bundle/safar-release-key.jks"
export JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home'

bundle_path='app/build/outputs/bundle/prodRelease/app-prod-release.aab'

./gradlew clean :app:bundleProdRelease --no-daemon --console=plain
"$JAVA_HOME/bin/keytool" -printcert -jarfile "$bundle_path" | grep -q 'CN=Safar App'
cp "$bundle_path" Outputs/Safar-prod-release-v1.5.8-9-fresh.aab
