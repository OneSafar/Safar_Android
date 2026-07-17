const fs = require('fs');

const inFile = 'D:\\SAFAR_PARENT\\Safar_Android\\Safar_Android\\app\\src\\main\\res\\drawable\\ic_launcher_foreground.xml';
const outFile = 'D:\\SAFAR_PARENT\\Safar_Android\\Safar_Android\\app\\src\\main\\res\\drawable\\ic_safar_launcher_foreground.xml';

const xml = fs.readFileSync(inFile, 'utf8');

// The transform: val_final = val * 0.084375 + 10.8
function transform(match) {
    let num = parseFloat(match);
    return (num * 0.084375 + 10.8).toFixed(4);
}

// Extract pathDatas
let newXml = xml.replace(/android:pathData="([^"]+)"/g, (match, pathData) => {
    let newPathData = pathData.replace(/-?\d+(\.\d+)?/g, transform);
    return `android:pathData="${newPathData}"`;
});

// Remove the <group> wrapper
newXml = newXml.replace(/<group[^>]*>/g, '');
newXml = newXml.replace(/<\/group>/g, '');

// Update viewport
newXml = newXml.replace(/android:viewportWidth="1024"/g, 'android:viewportWidth="108"');
newXml = newXml.replace(/android:viewportHeight="1024"/g, 'android:viewportHeight="108"');

fs.writeFileSync(outFile, newXml, 'utf8');
console.log('Successfully wrote ' + outFile);
