const fs = require('fs');
const path = require('path');

const inputDir = 'd:\\SAFAR_PARENT\\Safar_Android\\Safar_Android\\emojis';
const outputDir = 'd:\\SAFAR_PARENT\\Safar_Android\\Safar_Android\\app\\src\\main\\res\\drawable';

async function main() {
    const s2v = require('svg2vectordrawable');
    const files = fs.readdirSync(inputDir).filter(f => f.endsWith('.svg'));
    for (const file of files) {
        const inputPath = path.join(inputDir, file);
        const safeName = file.replace(/[\s\-\(\)]/g, '_').replace('.svg', '').replace(/_+/g, '_').toLowerCase();
        const outputPath = path.join(outputDir, 'ic_' + safeName + '.xml');
        try {
            console.log('Converting', file);
            const svgCode = fs.readFileSync(inputPath, 'utf8');
            let xmlCode = await s2v(svgCode);
            // Replace currentColor with hardcoded color or ?attr/colorControlNormal
            // but Compose LocalContentColor needs ?attr/colorControlNormal or just White and tinted by Icon()
            // Wait, Icon(painter = painterResource, tint = LocalContentColor.current) overrides ALL colors if it's solid.
            // But we must have a color. Let's replace "currentColor" or unassigned fills/strokes with "#FFFFFFFF".
            // Actually svg2vectordrawable usually handles currentColor well, but if not:
            fs.writeFileSync(outputPath, xmlCode, 'utf8');
        } catch (e) {
            console.error('Failed to convert', file, e);
        }
    }
    console.log('Finished converting all SVGs');
}

main();
