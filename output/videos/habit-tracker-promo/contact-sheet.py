from PIL import Image,ImageDraw
from pathlib import Path
import sys
src=Path(sys.argv[1]);out=Path(sys.argv[2]);files=sorted(src.glob('frame-*.png'))
cols=4;tw=360;th=203;pad=26
im=Image.new('RGB',(cols*tw,((len(files)+cols-1)//cols)*(th+pad)),'#343434');d=ImageDraw.Draw(im)
for i,f in enumerate(files):
 x=i%cols*tw;y=i//cols*(th+pad)
 fr=Image.open(f).convert('RGB').resize((tw,th),Image.Resampling.LANCZOS);im.paste(fr,(x,y));d.text((x+9,y+th+6),f.name.replace('frame-','').replace('.png',''),fill='white')
im.save(out)
