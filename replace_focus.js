const fs = require('fs');
const path = require('path');

function processFile(filePath) {
    let content = fs.readFileSync(filePath, 'utf8');
    let newContent = content;

    if (filePath.endsWith('.kt')) {
        // Regex to find string literals: "..."
        // Then replace Focus inside them
        newContent = newContent.replace(/"([^"\\]|\\.)*"/g, (match) => {
            let replaced = match.replace(/\bFocus\b/g, 'Ekagra');
            replaced = replaced.replace(/\bfocus\b/g, 'ekagra');
            return replaced;
        });
    } else if (filePath.endsWith('.xml')) {
        // Regex to find text between > and <
        newContent = newContent.replace(/>([^<]+)</g, (match) => {
            let replaced = match.replace(/\bFocus\b/g, 'Ekagra');
            replaced = replaced.replace(/\bfocus\b/g, 'ekagra');
            return replaced;
        });
        
        // Also handle android:title="..." or other attributes in layout files if needed
        // Just to be safe, replace in attributes too:
        newContent = newContent.replace(/="([^"]*)"/g, (match) => {
            let replaced = match.replace(/\bFocus\b/g, 'Ekagra');
            replaced = replaced.replace(/\bfocus\b/g, 'ekagra');
            return replaced;
        });
    }

    if (newContent !== content) {
        fs.writeFileSync(filePath, newContent, 'utf8');
        console.log(`Updated ${filePath}`);
    }
}

function walkDir(dir) {
    const files = fs.readdirSync(dir);
    for (const file of files) {
        const fullPath = path.join(dir, file);
        const stat = fs.statSync(fullPath);
        if (stat.isDirectory()) {
            walkDir(fullPath);
        } else if (stat.isFile() && (fullPath.endsWith('.kt') || fullPath.endsWith('.xml'))) {
            processFile(fullPath);
        }
    }
}

const targetDir = path.join(__dirname, 'app', 'src', 'main');
walkDir(targetDir);
console.log('Done!');
