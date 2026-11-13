/* YOUR DAY. IN SYNC. Original kinetic campaign. All painting is a pure function of t. */
(()=>{'use strict';
const W=1920,H=1080,B=60/144,PI=Math.PI;
const C={green:'#246A4D',paper:'#F6F2E8',ink:'#143A2C'};
const clamp=(v,a=0,b=1)=>Math.max(a,Math.min(b,v)),mix=(a,b,p)=>a+(b-a)*p;
const out=v=>1-Math.pow(1-clamp(v),5),smooth=v=>{v=clamp(v);return v*v*(3-2*v)};
const spring=v=>{v=clamp(v);return v===1?1:1-Math.exp(-8*v)*Math.cos(10*v)};
const assets={};
const definitions={overview:'screens/overview.png',timetable:'screens/timetable.png',habits:'screens/habits.png',schedule:'screens/schedule.png',study:'crops/study.png',reading:'crops/reading.png',exercise:'crops/exercise.png',meditation:'crops/meditation.png',stats:'crops/stats.png',math:'crops/math.png',slot:'crops/slot.png',link:'crops/link.png',repeat:'crops/repeat.png'};
window.HabitAssetsReady=Promise.all(Object.entries(definitions).map(([name,path])=>new Promise((resolve,reject)=>{const i=new Image();i.onload=()=>{assets[name]=i;resolve()};i.onerror=()=>reject(new Error('Missing local asset: '+path));i.src='assets/'+path})));
function rect(g,x,y,w,h,r,fill){g.beginPath();g.roundRect(x,y,w,h,r);g.fillStyle=fill;g.fill()}
function bg(g,c){g.fillStyle=c;g.fillRect(0,0,W,H)}
function circle(g,x,y,r,color){if(r<=0)return;g.beginPath();g.arc(x,y,r,0,2*PI);g.fillStyle=color;g.fill()}
function txt(g,text,x,y,size,color,opt={}){g.save();g.font=`400 ${size}px ${opt.ui?'WorkUI':'CampaignDisplay'}`;g.textBaseline='alphabetic';g.textAlign=opt.align||'left';g.fillStyle=color;g.fillText(text,x,y);g.restore()}
function reveal(g,text,x,y,size,color,t,start=0,dir=1,opt={}){
 const p=out((t-start)/.46);g.save();g.beginPath();g.rect(x-10,y-size*1.1,1150,size*1.35);g.clip();g.translate((1-p)*dir*130,(1-p)*size*1.2);txt(g,text,x,y,size,color,opt);g.restore();
}
function image(g,name,x,y,w,h,rot=0,shadow=true){const im=assets[name];if(!im)return;h=h||w*im.height/im.width;g.save();g.translate(x+w/2,y+h/2);g.rotate(rot);if(shadow){g.shadowColor='rgba(16,46,34,.17)';g.shadowBlur=28;g.shadowOffsetY=18}g.drawImage(im,-w/2,-h/2,w,h);g.restore();}
function phone(g,name,cx,cy,h,rot=0){const im=assets[name];if(!im)return;const w=h*im.width/im.height;image(g,name,cx-w/2,cy-h/2,w,h,rot,true)}
function tick(g,x,y,s,p,color,width=17){p=clamp(p);if(p<=0)return;const a=[x-s*.42,y],b=[x-s*.08,y+s*.32],c=[x+s*.52,y-s*.38],l1=Math.hypot(b[0]-a[0],b[1]-a[1]),l2=Math.hypot(c[0]-b[0],c[1]-b[1]);let d=(l1+l2)*p;g.save();g.strokeStyle=color;g.lineWidth=width;g.lineCap='round';g.lineJoin='round';g.beginPath();g.moveTo(...a);if(d<=l1)g.lineTo(mix(a[0],b[0],d/l1),mix(a[1],b[1],d/l1));else{g.lineTo(...b);d-=l1;g.lineTo(mix(b[0],c[0],d/l2),mix(b[1],c[1],d/l2))}g.stroke();g.restore()}
function arrow(g,x1,y1,x2,y2,p,color,width=12){p=out(p);g.save();g.strokeStyle=color;g.fillStyle=color;g.lineWidth=width;g.lineCap='round';g.beginPath();g.moveTo(x1,y1);const xx=mix(x1,x2,p),yy=mix(y1,y2,p);g.lineTo(xx,yy);g.stroke();if(p>.95){let a=Math.atan2(y2-y1,x2-x1);g.translate(x2,y2);g.rotate(a);g.beginPath();g.moveTo(0,0);g.lineTo(-26,-19);g.lineTo(-26,19);g.closePath();g.fill()}g.restore()}
function cover(g,t,start,color,cx,cy){const p=Math.pow(clamp((t-start)/(2.5-start)),3);if(p)circle(g,cx,cy,2600*p,color)}
function growPanel(g,t,start,color,cx,cy,w,h){const p=Math.pow(clamp((t-start)/(2.5-start)),3);if(!p)return;const ww=w+4600*p,hh=h+3100*p,xx=mix(cx,960,p),yy=mix(cy,540,p);rect(g,xx-ww/2,yy-hh/2,ww,hh,35*(1-p),color)}
function hook(g,t){
 bg(g,C.green);
 txt(g,'BIG GOALS.',110,300,217,C.paper);
 reveal(g,'SMALL',110,520,217,C.paper,t,.15,1);
 reveal(g,'HABITS.',110,740,217,C.paper,t,.38,-1);
 // The real overview screen is the hero; no mock phone chrome is added.
 const a=spring((t+.04)/.72);phone(g,'overview',1460+530*(1-a),523,934,(-.05)*(1-out(t/.8)));
 // An oversized completion gesture is the first visual motif.
 tick(g,1040,520,185,out((t-.40)/.42),C.paper,27);
 const c=spring((t-.82)/.65);image(g,'study',110,815+190*(1-c),820,190,(-.035)*(1-out((t-1.35)/.4)));
 cover(g,t,2.18,C.paper,1040,520);
}
function timetable(g,t){
 bg(g,C.paper);
 reveal(g,'MAKE',110,370,262,C.green,t,-.1,-1);
 reveal(g,'TIME.',110,635,262,C.green,t,.09,1);
 const p=spring((t+.02)/.65);phone(g,'timetable',1455+490*(1-p),530,950,.045*(1-out(t/.8)));
 // An actual source row becomes a close-up; the headline supplies the phone-size read.
 reveal(g,'7 AM. MATH MODE.',118,765,66,C.ink,t,.65,-1,{ui:true});
 const a=spring((t-.65)/.65);image(g,'math',116-700*(1-a),820,950,187,0);
 // A clock hand leads toward the timetable without a decorative HUD.
 g.save();const q=out((t-.5)/.75);g.strokeStyle=C.green;g.lineWidth=16;g.lineCap='round';g.beginPath();g.arc(970,440,151,-PI*.5,-PI*.5+q*PI*1.7);g.stroke();g.beginPath();g.moveTo(970,440);g.lineTo(970,332);g.moveTo(970,440);g.lineTo(1060,440);g.stroke();g.restore();
 growPanel(g,t,2.19,C.green,1455,880,350,50);
}
function link(g,t){
 bg(g,C.green);
 reveal(g,'LINK IT.',110,305,227,C.paper,t,-.1,1);
 reveal(g,'Habits + timetable.',118,410,70,C.paper,t,.1,-1,{ui:true});
 const a=spring((t+.03)/.65);phone(g,'schedule',1475+450*(1-a),535,930,-.035*(1-out(t/.8)));
 // Match the same Math Practice activity in the timetable and selected existing slot.
 const u=spring((t-.25)/.6);image(g,'math',120-500*(1-u),505,890,175,0);
 const v=spring((t-.78)/.6);image(g,'slot',120+550*(1-v),780,890,193,0);
 arrow(g,565,706,565,755,(t-.78)/.38,C.paper,13);
 // Reminder of the feature's native toggle, shown as an unchanged source close-up.
 cover(g,t,2.2,C.paper,565,730);
}
function track(g,t){
 bg(g,C.paper);
 const p=spring((t+.03)/.65);phone(g,'habits',445-500*(1-p),530,950,-.04*(1-out(t/.8)));
 const labels=['TICK.','TRACK.','REPEAT.'];
 for(let i=0;i<3;i++){
  const y=300+i*225;reveal(g,labels[i],940,y,205,C.green,t,i*B-.08,i%2?-1:1);
  circle(g,845,y-81,48,C.green);tick(g,845,y-81,56,out((t-i*B)/.28),C.paper,12);
 }
 const a=spring((t-.95)/.6);image(g,'stats',800,800+190*(1-a),950,216,0);
 cover(g,t,2.2,C.green,845,670);
}
function balance(g,t){
 bg(g,C.green);
 const words=['STUDY.','MOVE.','RESET.'],pics=['study','exercise','meditation'];
 for(let i=0;i<3;i++){
  const y=300+i*260;reveal(g,words[i],110,y,208,C.paper,t,i*B-.08,i%2?-1:1);
  const p=spring((t-i*B-.08)/.65);const w=720,h=w*assets[pics[i]].height/assets[pics[i]].width;
  image(g,pics[i],1080+(i===1?0:650*(1-p)),y-170+(i===1?280*(1-p):0),w,h,(1-p)*(i%2?-.16:.16));
 }
 growPanel(g,t,2.2,C.paper,1430,800,720,165);
}
function close(g,t){
 bg(g,C.paper);
 reveal(g,'SAFAR',118,200,63,C.green,t,-.15,-1,{ui:true});
 reveal(g,'YOUR DAY.',110,435,208,C.green,t,-.12,1);
 reveal(g,'IN SYNC.',110,660,208,C.green,t,.02,-1);
 reveal(g,'Habit Tracker + Timetable',118,806,68,C.ink,t,.04,1,{ui:true});
 reveal(g,'Build your next good day.',118,928,68,C.green,t,.04,-1,{ui:true});
 const p=out(t/.45);phone(g,'overview',1300+450*(1-p),535,820,-.055);
 phone(g,'timetable',1630+600*(1-p),548,868,.045);
}
const scenes=[hook,timetable,link,track,balance,close],ids=['goals','time','link','track','balance','close'];
function draw(canvas,i,t){const g=canvas.getContext('2d',{alpha:false});g.setTransform(1,0,0,1,0,0);g.globalAlpha=1;g.globalCompositeOperation='source-over';g.shadowColor='transparent';g.shadowBlur=0;g.shadowOffsetX=0;g.shadowOffsetY=0;scenes[i](g,clamp(t,0,2.5));}
window.HabitPromo={draw,ids,beat:B};
window.seek=t=>{t=clamp(t,0,14.999999);for(const canvas of document.querySelectorAll('canvas[data-habit-scene]')){const i=+canvas.dataset.habitScene,active=t>=i*2.5&&t<(i+1)*2.5,host=document.getElementById(ids[i]);if(host){host.style.display=active?'block':'none';host.style.visibility=active?'visible':'hidden';host.style.opacity=active?'1':'0'}if(active)draw(canvas,i,t-i*2.5)}};
window.addEventListener('hf-seek',e=>window.seek(e.detail.time));
})();
