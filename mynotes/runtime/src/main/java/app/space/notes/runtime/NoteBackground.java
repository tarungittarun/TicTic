package app.space.notes.runtime;
import android.graphics.*;
import android.graphics.drawable.Drawable;
/** Quiet vector papers, independent of note colour. No downloaded images or hidden permissions. */
final class NoteBackground extends Drawable {
 static final String[] NAMES={"Plain","Fine dots","Wide dots","Ruled","Squared","Graph paper","Diagonal","Crosshatch","Honeycomb","Waves","Stitches","Confetti","Diamonds","Bricks","Music lines","Constellation"};
 private final int pattern,base;private final boolean dark;private final float density;private final boolean textRuling;
 NoteBackground(int pattern,int base,boolean dark,float density){this(pattern,base,dark,density,false);}
 NoteBackground(int pattern,int base,boolean dark,float density,boolean textRuling){this.pattern=pattern;this.base=base;this.dark=dark;this.density=density;this.textRuling=textRuling;}
 @Override public void draw(Canvas c){int saved=c.save();c.translate(getBounds().left,getBounds().top);float w=getBounds().width()/density,h=getBounds().height()/density;c.scale(density,density);Path clip=new Path();clip.addRoundRect(new RectF(0,0,w,h),16,16,Path.Direction.CW);c.clipPath(clip);int[] light={0,0xffd9f1e7,0xffe5ddfa,0xffffe5cf,0xffdceafa,0xffdbf0f0,0xfff5dfed,0xffede6ce,0xffffedbf,0xffd9eef8,0xffeadff5,0xffffe1df,0xffe2eed5,0xfff2dfce,0xffe0e3fa,0xffd9eee8};int[] night={0,0xff173c32,0xff352849,0xff493223,0xff24384b,0xff1b3d40,0xff452c3e,0xff3c3926,0xff4b3e21,0xff223e4c,0xff382e48,0xff4a2c2b,0xff2d4028,0xff453329,0xff2e324b,0xff203e38};int tone=pattern==0?base:(dark?night[pattern]:light[pattern]);if(pattern!=0)tone=Color.rgb((Color.red(tone)*3+Color.red(base))/4,(Color.green(tone)*3+Color.green(base))/4,(Color.blue(tone)*3+Color.blue(base))/4);c.drawColor(tone);if(pattern==0 || (pattern==3 && textRuling)){c.restoreToCount(saved);return;}Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setColor(dark?0x22FFFFFF:0x18000000);p.setStrokeWidth(.8f);p.setStyle(Paint.Style.STROKE);
  int step=pattern==2?32:24;
  for(int y=0;y<h+step;y+=step)for(int x=0;x<w+step;x+=step){switch(pattern){
   case 1:case 2:p.setStyle(Paint.Style.FILL);c.drawCircle(x,y,1,p);break;
   case 3:if(x==0)c.drawLine(0,y,w,y,p);break;
   case 4:c.drawLine(x,y,x+step,y,p);c.drawLine(x,y,x,y+step,p);break;
   case 5:c.drawLine(x,y,x+step,y,p);c.drawLine(x,y,x,y+step,p);p.setAlpha(dark?18:12);c.drawLine(x+12,y,x+12,y+24,p);c.drawLine(x,y+12,x+24,y+12,p);p.setAlpha(dark?34:24);break;
   case 6:c.drawLine(x,y,x+step,y+step,p);break;
   case 7:c.drawLine(x,y,x+step,y+step,p);c.drawLine(x+step,y,x,y+step,p);break;
   case 8:Path hex=new Path();hex.moveTo(x+6,y);hex.lineTo(x+18,y);hex.lineTo(x+24,y+12);hex.lineTo(x+18,y+24);hex.lineTo(x+6,y+24);hex.lineTo(x,y+12);hex.close();c.drawPath(hex,p);break;
   case 9:Path wave=new Path();wave.moveTo(x,y+12);wave.cubicTo(x+6,y,x+18,y+24,x+24,y+12);c.drawPath(wave,p);break;
   case 10:c.drawLine(x+9,y+12,x+15,y+12,p);c.drawLine(x+12,y+9,x+12,y+15,p);break;
   case 11:c.save();c.rotate((x*7+y*3)%180,x+12,y+12);c.drawLine(x+10,y+10,x+14,y+14,p);c.restore();break;
   case 12:Path diamond=new Path();diamond.moveTo(x+12,y+3);diamond.lineTo(x+21,y+12);diamond.lineTo(x+12,y+21);diamond.lineTo(x+3,y+12);diamond.close();c.drawPath(diamond,p);break;
   case 13:if(x==0)c.drawLine(0,y,w,y,p);float shift=(y/step)%2==0?0:12;c.drawLine(x+shift,y,x+shift,y+step,p);break;
   case 14:if(x==0 && (y/step)%3!=2){c.drawLine(0,y,w,y,p);c.drawLine(0,y+6,w,y+6,p);}break;
   case 15:c.drawCircle(x+4,y+6,1.2f,p);c.drawCircle(x+20,y+18,1.2f,p);c.drawLine(x+4,y+6,x+20,y+18,p);break;
  }}c.restoreToCount(saved);
 }
 @Override public void setAlpha(int alpha){}
 @Override public void setColorFilter(ColorFilter filter){}
 @Override public int getOpacity(){return PixelFormat.OPAQUE;}
}
