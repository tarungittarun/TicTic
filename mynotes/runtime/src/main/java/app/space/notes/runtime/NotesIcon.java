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
 case "link":c.drawArc(2,8,14,20,40,280,false,p);c.drawArc(10,2,22,14,220,280,false,p);lines(c,8,16,16,8);break;
 case "drag":for(int x=9;x<=15;x+=6)for(int y=6;y<=18;y+=6)c.drawCircle(x,y,.8f,p);break;
 case "crop":lines(c,7,2,7,17,22,17);lines(c,2,7,17,7,17,22);break;
 case "mirror":lines(c,12,2,12,22);lines(c,9,5,3,19,9,19,9,5);lines(c,15,5,21,19,15,19,15,5);break;
 case "rotate-left":c.drawArc(4,5,20,21,210,280,false,p);lines(c,3,3,3,10,10,10);break;
 case "rotate-right":c.drawArc(4,5,20,21,50,280,false,p);lines(c,21,3,21,10,14,10);break;
 case "group":c.drawRoundRect(2,2,15,15,2,2,p);c.drawRoundRect(9,9,22,22,2,2,p);break;
 case "up":lines(c,12,21,12,3);lines(c,5,10,12,3,19,10);break;
 case "down":lines(c,12,3,12,21);lines(c,5,14,12,21,19,14);break;
 case "indent":for(int y=5;y<=19;y+=7)lines(c,11,y,21,y);lines(c,3,8,7,12,3,16);break;
 case "outdent":for(int y=5;y<=19;y+=7)lines(c,11,y,21,y);lines(c,7,8,3,12,7,16);break;
 case "text":lines(c,4,6,20,6);lines(c,12,6,12,21);lines(c,8,21,16,21);break;
 case "merge":lines(c,3,4,9,10,9,13,17,13);lines(c,3,21,9,15,9,13);lines(c,14,8,20,13,14,18);break;
 case "widget":for(int x=3;x<=14;x+=11)for(int y=3;y<=14;y+=11)c.drawRoundRect(x,y,x+7,y+7,1,1,p);break;
 case "info":c.drawCircle(12,12,9,p);c.drawCircle(12,7,1,p);lines(c,12,11,12,17);break;
 case "diagnostics":lines(c,2,12,6,12,9,5,14,19,17,12,22,12);break;
 case "next":lines(c,9,5,16,12,9,19);break;
 case "circle":c.drawCircle(12,12,8,p);break;
 case "save":lines(c,4,3,17,3,21,7,21,21,3,21,3,3);lines(c,7,3,7,9,16,9,16,3);c.drawRect(7,14,17,21,p);break;
 case "copy":c.drawRoundRect(8,7,21,21,2,2,p);lines(c,16,3,3,3,3,16);break;
 case "line":lines(c,4,20,20,4);break;
 case "rectangle":c.drawRect(3,5,21,19,p);break;
 case "oval":c.drawOval(3,5,21,19,p);break;
 case "pencil":lines(c,4,17,16,5,20,9,8,21,3,22,4,17);lines(c,6,16,10,20);break;
 case "highlighter":lines(c,5,14,14,3,21,10,12,20,5,14);lines(c,4,19,10,23);break;
 case "grid":for(int n=4;n<=20;n+=8){lines(c,n,3,n,21);lines(c,3,n,21,n);}break;
 case "image":c.drawRoundRect(3,3,21,21,2,2,p);c.drawCircle(8,8,2,p);lines(c,3,18,9,12,13,16,17,11,21,16);break;
 case "scan":lines(c,3,8,3,3,8,3);lines(c,16,3,21,3,21,8);lines(c,21,16,21,21,16,21);lines(c,8,21,3,21,3,16);lines(c,6,12,18,12);break;
 case "diagram":c.drawRoundRect(2,3,10,10,1,1,p);c.drawRoundRect(14,14,22,21,1,1,p);lines(c,6,10,6,17,14,17);break;
 case "edit":lines(c,4,16,16,4,20,8,8,20,3,21,4,16);lines(c,13,7,17,11);break;
 case "erase":lines(c,5,15,14,4,21,11,12,21,8,21,5,18,5,15);lines(c,9,11,16,17);lines(c,12,21,22,21);break;
 case "check":lines(c,4,12,10,18,21,6);break;
 case "close":lines(c,6,6,18,18);lines(c,18,6,6,18);break;
 case "archive":c.drawRoundRect(3,4,21,8,1,1,p);lines(c,5,8,5,20,19,20,19,8);lines(c,10,12,14,12);break;
 case "trash":lines(c,4,6,20,6);lines(c,9,3,15,3);lines(c,6,6,7,21,17,21,18,6);lines(c,10,10,10,17);lines(c,14,10,14,17);break;
 case "mic":c.drawRoundRect(9,3,15,14,3,3,p);c.drawArc(6,7,18,18,0,180,false,p);lines(c,12,18,12,22);lines(c,8,22,16,22);break;
 case "pause":lines(c,8,5,8,19);lines(c,16,5,16,19);break;
 case "play":lines(c,7,4,20,12,7,20,7,4);break;
 case "stop":c.drawRoundRect(5,5,19,19,2,2,p);break;
 case "export":lines(c,12,3,12,15);lines(c,7,8,12,3,17,8);lines(c,4,14,4,21,20,21,20,14);break;
 case "retry":c.drawArc(4,4,20,20,40,290,false,p);lines(c,20,3,20,9,14,9);break;
 case "menu":for(int y=6;y<=18;y+=6)lines(c,4,y,20,y);break;
 case "add":lines(c,12,5,12,19);lines(c,5,12,19,12);break;
 case "checklist":for(int y=6;y<=18;y+=6){lines(c,3,y,5,y+2,8,y-2);lines(c,11,y,21,y);}break;
 case "search":c.drawCircle(10,10,6,p);lines(c,14.5f,14.5f,21,21);break;
 case "filter":lines(c,4,6,20,6);lines(c,7,12,17,12);lines(c,10,18,14,18);break;
 case "back":lines(c,13,5,6,12,13,19);lines(c,6,12,21,12);break;
 case "redo":c.save();c.translate(24,0);c.scale(-1,1);lines(c,8,5,3,10,8,15);lines(c,3,10,14,10);c.drawArc(9,10,21,22,270,150,false,p);c.restore();break;
 case "undo":lines(c,8,5,3,10,8,15);lines(c,3,10,14,10);c.drawArc(9,10,21,22,270,150,false,p);break;
 case "more":for(int y=5;y<=19;y+=7)c.drawCircle(12,y,1,p);break;
 case "fit":lines(c,3,9,3,3,9,3);lines(c,15,3,21,3,21,9);lines(c,21,15,21,21,15,21);lines(c,9,21,3,21,3,15);break;
 case "stroke":p.setStrokeWidth(1);lines(c,4,5,20,5);p.setStrokeWidth(3);lines(c,4,12,20,12);p.setStrokeWidth(5);lines(c,4,20,20,20);break;
 case "eraser":lines(c,4,14,13,5,20,12,11,21,7,21,4,18,4,14);lines(c,8,10,15,17);lines(c,11,21,21,21);break;
 case "pinned":p.setStyle(Paint.Style.FILL);lines(c,8,3,16,3,15,9,19,14,5,14,9,9,8,3);p.setStyle(Paint.Style.STROKE);lines(c,12,14,12,22);break;
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
