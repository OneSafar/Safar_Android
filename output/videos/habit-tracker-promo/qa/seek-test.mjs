import puppeteer from '/Users/shashankkumar/.npm/_npx/e36c17b8d592732d/node_modules/puppeteer-core/lib/puppeteer/puppeteer-core.js';
import {writeFile} from 'node:fs/promises';import {pathToFileURL} from 'node:url';
const browser=await puppeteer.launch({executablePath:'/Applications/Google Chrome.app/Contents/MacOS/Google Chrome',headless:true,args:['--allow-file-access-from-files']});
const page=await browser.newPage();await page.setViewport({width:1920,height:1080});await page.goto(pathToFileURL(process.cwd()+'/qa/seek-fixture.html').href);
await page.evaluate(async()=>{await Promise.all([window.HabitAssetsReady,document.fonts.load('400 200px CampaignDisplay'),document.fonts.load('400 64px WorkUI')])});
const times=[0,.416667,1.4,2.49,2.5,3.3,4.99,5,6.3,7.49,7.5,8.4,9.99,10,11.1,12.49,12.5,13,14.99];
async function pixels(t){return page.evaluate(t=>{window.seek(t);return [...document.querySelectorAll('canvas')].find(c=>getComputedStyle(c.parentNode).display!=='none').toDataURL()},t)}
const first={};for(const t of times)first[t]=await pixels(t);const samples=[];for(const t of times.slice().reverse())samples.push({time:t,pixelIdentical:await pixels(t)===first[t]});
const report={ok:samples.every(x=>x.pixelIdentical),method:'19 complete canvas frames, ascending then descending, exact PNG comparison',samples};await writeFile('qa/seek-determinism.json',JSON.stringify(report,null,2));await browser.close();console.log(report.ok?'PASS: 19 seek-order samples are pixel-identical.':'FAIL');if(!report.ok)process.exit(1);
