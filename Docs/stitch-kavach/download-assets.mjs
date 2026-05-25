import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';
import { execSync } from 'child_process';

const dir = path.dirname(fileURLToPath(import.meta.url));
const screens = ['about-permissions', 'shield-configuration', 'active-session', 'session-summary'];

for (const name of screens) {
  const j = JSON.parse(fs.readFileSync(path.join(dir, `screen-${name}.json`), 'utf8'));
  const screen = JSON.parse(j.result.content[0].text);
  const data = screen.screen ?? screen;
  const imgUrl = data.screenshot.downloadUrl;
  const htmlUrl = data.htmlCode.downloadUrl;
  execSync(`curl.exe -sL "${imgUrl}" -o "${path.join(dir, `${name}.png`)}"`, { stdio: 'inherit' });
  execSync(`curl.exe -sL "${htmlUrl}" -o "${path.join(dir, `${name}.html`)}"`, { stdio: 'inherit' });
  console.log(`Downloaded ${name}`);
}
