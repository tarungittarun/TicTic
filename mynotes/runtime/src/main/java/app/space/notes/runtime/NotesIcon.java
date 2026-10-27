package app.space.notes.runtime;

import android.graphics.*;
import android.graphics.drawable.Drawable;
/** Consistent 24dp outline symbols: no emoji/font-dependent glyphs. Names live on controls. */
final class NotesIcon extends Drawable {
 private final String name;private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
 NotesIcon(String name,int color){this.name=name;p.setColor(color);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.8f);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);}
 private void lines(Canvas c,float...xy){Path path=new Path();path.moveTo(xy[0],xy[1]);for(int i=2;i<xy.length;i+=2)path.lineTo(xy[i],xy[i+1]);c.drawPath(path,p);}
 @Override public void draw(Canvas original){Canvas c=original;int save=c.save();c.translate(getBounds().left,getBounds().top);c.scale(getBounds().width()/24f,getBounds().height()/24f);
 switch(name){
 case "menu":for(int y=6;y<=18;y+=6)lines(c,4,y,20,y);break;
 case "add":lines(c,12,5,12,19);lines(c,5,12,19,12);break;
 case "checklist":for(int y=6;y<=18;y+=6){lines(c,3,y,5,y+2,8,y-2);lines(c,11,y,21,y);}break;
 case "filter":lines(c,4,6,20,6);lines(c,7,12,17,12);lines(c,10,18,14,18);break;
 case "back":lines(c,13,5,6,12,13,19);lines(c,6,12,21,12);break;
 case "undo":lines(c,8,5,3,10,8,15);lines(c,3,10,14,10);c.drawArc(9,10,21,22,270,150,false,p);break;
 case "more":for(int y=5;y<=19;y+=7)c.drawCircle(12,y,1,p);break;
 case "pin":lines(c,8,3,16,3,15,9,19,14,5,14,9,9,8,3);lines(c,12,14,12,22);break;
 case "format":lines(c,5,18,12,4,19,18);lines(c,8,13,16,13);lines(c,4,22,20,22);break;
 case "label":lines(c,3,3,12,3,22,13,13,22,3,12,3,3);c.drawCircle(8,8,1.4f,p);break;
 case "palette":c.drawCircle(12,12,9,p);c.drawCircle(9,7,1,p);c.drawCircle(15,7,1,p);c.drawCircle(6,13,1,p);c.drawCircle(17,13,1,p);break;
 case "share":c.drawCircle(18,5,3,p);c.drawCircle(5,12,3,p);c.drawCircle(18,19,3,p);lines(c,8,11,15,6);lines(c,8,13,15,18);break;
 case "bell":lines(c,5,17,7,14,7,9);c.drawArc(7,4,17,14,180,180,false,p);lines(c,17,9,17,14,19,17,5,17);c.drawArc(10,18,14,22,0,180,false,p);break;
 case "folder":lines(c,3,6,10,6,12,9,21,9,21,20,3,20,3,6);break;
 case "settings":c.drawCircle(12,12,4,p);for(int i=0;i<8;i++){c.save();c.rotate(i*45,12,12);lines(c,12,2,12,5);c.restore();}c.drawCircle(12,12,8,p);break;
 default:c.drawRoundRect(4,3,20,21,3,3,p);lines(c,8,8,16,8);lines(c,8,12,16,12);lines(c,8,16,13,16);
 }c.restoreToCount(save);}
 @Override public void setAlpha(int alpha){p.setAlpha(alpha);}@Override public void setColorFilter(ColorFilter filter){p.setColorFilter(filter);}@Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
