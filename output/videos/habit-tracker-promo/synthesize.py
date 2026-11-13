"""Original 144 BPM D-major student-campaign score; seeded mulberry32 noise, no samples."""
import numpy as np, wave, json, subprocess, re
from pathlib import Path
SR=48000; D=15.; B=60/144; N=int(SR*D)
master=np.zeros((N,2),np.float64); kicks=np.zeros(N)
state=0xA51CE

def noise(n):
 global state
 a=np.empty(n)
 for i in range(n):
  state=(state+0x6D2B79F5)&0xffffffff;t=((state^(state>>15))*(1|state))&0xffffffff
  t^=(t+(((t^(t>>7))*(61|t))&0xffffffff))&0xffffffff
  a[i]=((t^(t>>14))&0xffffffff)/2147483648-1
 return a

def add(sig,start,gain=1,pan=0):
 pos=round(start*SR);s=0
 if pos<0:s=-pos;pos=0
 k=min(len(sig)-s,N-pos)
 if k<=0:return
 master[pos:pos+k,0]+=sig[s:s+k]*gain*np.sqrt((1-pan)/2)
 master[pos:pos+k,1]+=sig[s:s+k]*gain*np.sqrt((1+pan)/2)

def tone(freq,dur,decay=5):
 t=np.arange(round(dur*SR))/SR
 env=(1-np.exp(-t*400))*np.exp(-t*decay)
 return sum(np.sin(2*np.pi*freq*h*t)/h**1.6 for h in range(1,6))*env

# Kick with a rapid pitch dive and a click transience.
for b in range(36):
 t=np.arange(int(.38*SR))/SR
 f=44+115*np.exp(-t*30);phase=2*np.pi*(44*t+115/30*(1-np.exp(-30*t)))
 kick=np.sin(phase)*np.exp(-t*13)*(1-np.exp(-t*1800))+noise(len(t))*.055*np.exp(-t*230)
 pos=round(b*B*SR);k=min(len(kick),N-pos);kicks[pos:pos+k]+=kick[:k]
 add(kick,b*B,.92)
 if b%2:
  t=np.arange(int(.22*SR))/SR;n=noise(len(t));hp=n-np.concatenate(([0],n[:-1]))
  sn=(hp*.23+np.sin(2*np.pi*185*t)*.3)*np.exp(-t*23)*(1-np.exp(-t*1200))
  add(sn,b*B,.68,.08)
# Eighth-note hi-hats: a clear rhythmic grid, controlled stereo.
for e in range(72):
 dur=.065 if e%2==0 else .11;t=np.arange(int(dur*SR))/SR;n=noise(len(t));hp=n-np.concatenate(([0],n[:-1]))
 add(hp*np.exp(-t*(62 if e%2==0 else 34)),e*B/2,.060 if e%2==0 else .038,(-.35 if e%2==0 else .35))
# Minor analogue bass progression and staccato chord stabs.
roots=[73.416,73.416,82.407,98,73.416,82.407,69.295,98,73.416]
for b in range(34):
 root=roots[b//4];t=np.arange(int(.36*SR))/SR;sig=tone(root,.36,7)
 sig=np.tanh(sig*1.5)*.55;add(sig,b*B+.012,.38)
 if b%4 in (0,3):
  chord=sum(tone(root*4*2**(step/12),.48,8) for step in [0,4,7])/3
  add(chord,b*B,.22,-.2 if b%4==0 else .2)
# FM glass-like lead phrase, contrasting the heavier bass.
notes=[0,4,7,12,7,4,2,7,9,7,4,2,7,12,16,19]
for k in range(32):
 start=(k+.5)*B;freq=293.665*2**(notes[k%len(notes)]/12);t=np.arange(int(.52*SR))/SR
 sig=np.sin(2*np.pi*freq*t+2.3*np.sin(2*np.pi*freq*2*t)*np.exp(-t*12))*(1-np.exp(-t*650))*np.exp(-t*9)
 add(sig,start,.10,np.sin(k*1.8)*.55)
 add(sig,start+B*.75,.028,-np.sin(k*1.8)*.55)
# Transition whooshes finish exactly on the six-beat scene boundaries.
for end in [6*B,12*B,18*B,24*B,30*B]:
 dur=.34;t=np.arange(int(dur*SR))/SR;n=noise(len(t));low=np.convolve(n,np.ones(11)/11,'same')
 sig=(n*.22+low)*np.sin(np.pi*t/dur)**1.4*(t/dur)
 add(sig,end-dur,.24)
 add(tone(110,.6,9),end,.18)
# Deliberate final signature strike and short harmonic release.
for st,gain in [(30*B,.32),(32*B,.22),(34*B,.26)]:
 chord=sum(tone(110*2**(step/12),1.4,3) for step in [0,7,12,16])/4
 add(chord,st,gain)
# A short final fade avoids a truncated waveform.
fade=np.clip((D-np.arange(N)/SR)/.28,0,1);master*=fade[:,None]
master=np.tanh(master*.8)

def wav(path,data):
 data=np.asarray(data);channels=1 if data.ndim==1 else data.shape[1]
 with wave.open(str(path),'wb') as f:
  f.setnchannels(channels);f.setsampwidth(2);f.setframerate(SR);f.writeframes((np.clip(data,-1,1)*32767).astype('<i2').tobytes())
Path('assets/audio').mkdir(parents=True,exist_ok=True)
wav('assets/audio/score-raw.wav',master);wav('assets/audio/kick-reference.wav',kicks*.6)
# Measure kick onset in 1ms RMS windows from the actual synthesized PCM.
ms=SR//1000;energy=np.sqrt(np.mean(kicks[:N//ms*ms].reshape(-1,ms)**2,axis=1))
measured=[]
for b in range(36):
 expected=b*B;lo=max(0,int(expected*1000)-3);hi=min(len(energy),int(expected*1000)+28)
 e=energy[lo:hi];pe=int(np.argmax(e));threshold=max(e)*.13
 crossings=np.where(e[:pe+1]>=threshold)[0]
 measured.append((lo+int(crossings[0] if len(crossings) else pe))/1000)
Path('beats.json').write_text(json.dumps({'bpm':144,'beatDuration':B,'duration':15,'beatTimes':[b*B for b in range(36)],'measuredOnsets':measured,'measurement':'1ms RMS leading-edge detection on rendered kick-reference PCM; visual grid compensated to note starts.','maxOnsetOffsetMs':round(max(abs(measured[b]-b*B)*1000 for b in range(36)),3)},indent=2))

# Limit transient crest factor before loudness normalization, preserving AAC headroom.
def ff(args):return subprocess.run(['ffmpeg','-y','-hide_banner']+args,capture_output=True,text=True,check=True)
def measure(path):
 r=ff(['-i',path,'-af','loudnorm=I=-14:TP=-1.5:LRA=8:print_format=json','-f','null','-'])
 return json.loads(re.search(r'\{[^{}]*"input_i"[^{}]*\}',r.stderr,re.S).group())
ff(['-i','assets/audio/score-raw.wav','-af','alimiter=limit=0.42:level=false:attack=2:release=45','-c:a','pcm_s24le','assets/audio/score-limited.wav'])
s=measure('assets/audio/score-limited.wav')
f=f'loudnorm=I=-14:TP=-1.5:LRA=8:measured_I={s["input_i"]}:measured_TP={s["input_tp"]}:measured_LRA={s["input_lra"]}:measured_thresh={s["input_thresh"]}:offset={s["target_offset"]}:linear=true'
ff(['-i','assets/audio/score-limited.wav','-af',f,'-ar','48000','-c:a','pcm_s24le','assets/audio/score-normalized.wav'])
s=measure('assets/audio/score-normalized.wav');gain=-14-float(s['input_i'])
ff(['-i','assets/audio/score-normalized.wav','-af',f'volume={gain}dB','-c:a','pcm_s24le','assets/audio/score.wav'])
s=measure('assets/audio/score.wav');Path('qa/score-loudness.json').write_text(json.dumps(s,indent=2));print('Original 144 BPM score:',s['input_i'],'LUFS;',s['input_tp'],'dBTP')
