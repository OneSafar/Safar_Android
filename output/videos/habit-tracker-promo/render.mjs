/** Reproducible delivery entrypoint: node render.mjs */
import {spawnSync} from 'node:child_process';
import {existsSync,readFileSync,writeFileSync,mkdirSync} from 'node:fs';
import {fileURLToPath} from 'node:url';
import {dirname,join} from 'node:path';
const dir=dirname(fileURLToPath(import.meta.url));process.chdir(dir);
if(Number(process.versions.node.split('.')[0])<22){
 const node22='/opt/homebrew/opt/node@22/bin/node';
 if(!existsSync(node22))throw Error('HyperFrames requires Node 22+. Run this script with Node 22+.');
 const r=spawnSync(node22,[fileURLToPath(import.meta.url),...process.argv.slice(2)],{stdio:'inherit'});process.exit(r.status??1);
}
const review=JSON.parse(readFileSync('qa/review-scores.json','utf8'));
if(Object.values(review.final).some(n=>n<8))throw Error('Review gate: every category must score at least 8.');
const env={...process.env,PATH:dirname(process.execPath)+':'+process.env.PATH};
function run(cmd,args){const r=spawnSync(cmd,args,{cwd:dir,env,stdio:'inherit'});if(r.status!==0)throw Error(`${cmd} failed (${r.status})`)}
mkdirSync('renders',{recursive:true});
run('npx',['--yes','hyperframes@0.9.5','render','.','--quality','looks','--fps','30','--format','mp4','--strict','--output','renders/hyperframes-master.mp4']);
// HyperFrames looks is libx264 CRF16. Preserve its video bitstream; mux the normalized original score.
run('ffmpeg',['-y','-hide_banner','-i','renders/hyperframes-master.mp4','-i','assets/audio/score.wav','-map','0:v:0','-map','1:a:0','-c:v','copy','-c:a','aac','-b:a','320k','-t','15','-movflags','+faststart','-metadata','title=YOUR DAY. IN SYNC.','-metadata','comment=HyperFrames • 1920x1080 • 30fps • H.264 CRF16 • original synthesized score','renders/HABIT_TRACKER_PROMO.mp4']);
const p=spawnSync('ffprobe',['-v','error','-show_streams','-show_format','-of','json','renders/HABIT_TRACKER_PROMO.mp4'],{encoding:'utf8'});
if(p.status!==0)throw Error('ffprobe failed');const probe=JSON.parse(p.stdout);const v=probe.streams.find(s=>s.codec_type==='video');
if(v.codec_name!=='h264'||v.pix_fmt!=='yuv420p'||Math.abs(Number(probe.format.duration)-15)>.05)throw Error('Delivery contract verification failed');
writeFileSync('qa/final-probe.json',JSON.stringify(probe,null,2));
console.log('\nDELIVERED: '+join(dir,'renders/HABIT_TRACKER_PROMO.mp4'));
