package app.space.notes.runtime;

import android.graphics.*;
import android.text.*;
import app.space.notes.domain.Diagram;

/** Same vector renderer for the editor and PDF. No network, bitmap asset or font dependency. */
public final class DiagramRenderer {
    private DiagramRenderer() { }
    public static void draw(Canvas canvas,Diagram diagram,boolean dark,String selected) {
        Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);paint.setStrokeWidth(3);int ink=Color.parseColor(dark?"#F1F3F4":"#202124");
        for(Diagram.Edge edge:diagram.edges){
            Diagram.Node a=diagram.find(edge.from),b=diagram.find(edge.to);float ax=a.x+a.width/2f,ay=a.y+a.height/2f,bx=b.x+b.width/2f,by=b.y+b.height/2f;
            float dx=bx-ax,dy=by-ay;
            if(dx==0 && dy==0)continue;
            float[] start=boundary(a,dx,dy),end=boundary(b,-dx,-dy);float angle=(float)Math.atan2(end[1]-start[1],end[0]-start[0]);
            paint.setStyle(Paint.Style.STROKE);paint.setColor(ink);canvas.drawLine(start[0],start[1],end[0],end[1],paint);
            Path head=new Path();head.moveTo(end[0],end[1]);head.lineTo(end[0]-16*(float)Math.cos(angle-.45),end[1]-16*(float)Math.sin(angle-.45));head.moveTo(end[0],end[1]);head.lineTo(end[0]-16*(float)Math.cos(angle+.45),end[1]-16*(float)Math.sin(angle+.45));canvas.drawPath(head,paint);
            if(!edge.label.isEmpty())drawText(canvas,edge.label,(start[0]+end[0])/2-90,(start[1]+end[1])/2-32,180,50,ink,20);
        }
        for(Diagram.Node node:diagram.nodes){
            RectF r=new RectF(node.x,node.y,node.x+node.width,node.y+node.height);Path path=shape(node,r);
            if(!node.shape.equals("text")){paint.setStyle(Paint.Style.FILL);paint.setColor(NotesBoard.colour(node.color,dark));canvas.drawPath(path,paint);}
            paint.setStyle(Paint.Style.STROKE);paint.setColor(node.id.equals(selected)?Color.parseColor("#B17400"):ink);paint.setStrokeWidth(node.id.equals(selected)?6:3);
            if(!node.shape.equals("text") || node.id.equals(selected))canvas.drawPath(path,paint);
            float inset=node.shape.equals("decision")?node.width*.23f:node.shape.equals("data")?node.width*.17f:12;
            drawText(canvas,node.text,node.x+inset,node.y+8,node.width-2*inset,node.height-16,ink,24);
        }
    }
    public static float[] boundary(Diagram.Node n,float dx,float dy){
        float cx=n.x+n.width/2f,cy=n.y+n.height/2f;
        if(dx==0 && dy==0)return new float[]{cx,cy};
        float sx=Math.abs(dx)/(n.width/2f),sy=Math.abs(dy)/(n.height/2f),factor;
        if(n.shape.equals("decision"))factor=1/(sx+sy);
        else if(n.shape.equals("ellipse") || n.shape.equals("terminator"))factor=(float)(1/Math.sqrt(sx*sx+sy*sy));
        else factor=1/Math.max(sx,sy);
        return new float[]{cx+dx*factor,cy+dy*factor};
    }
    private static Path shape(Diagram.Node n,RectF r){
        Path p=new Path();
        switch(n.shape){
            case "decision":p.moveTo(r.centerX(),r.top);p.lineTo(r.right,r.centerY());p.lineTo(r.centerX(),r.bottom);p.lineTo(r.left,r.centerY());p.close();break;
            case "data":float offset=r.width()*.15f;p.moveTo(r.left+offset,r.top);p.lineTo(r.right,r.top);p.lineTo(r.right-offset,r.bottom);p.lineTo(r.left,r.bottom);p.close();break;
            case "ellipse":p.addOval(r,Path.Direction.CW);break;
            case "terminator":p.addRoundRect(r,r.height()/2,r.height()/2,Path.Direction.CW);break;
            default:p.addRect(r,Path.Direction.CW);
        }
        return p;
    }
    private static void drawText(Canvas canvas,String text,float x,float y,float width,float height,int color,int size){
        TextPaint tp=new TextPaint(Paint.ANTI_ALIAS_FLAG);tp.setColor(color);tp.setTextSize(size);
        StaticLayout layout=StaticLayout.Builder.obtain(text,0,text.length(),tp,Math.max(1,(int)width)).setAlignment(Layout.Alignment.ALIGN_CENTER).setIncludePad(false).setEllipsize(TextUtils.TruncateAt.END).setMaxLines(Math.max(1,(int)(height/(size*1.25f)))).build();
        canvas.save();canvas.clipRect(x,y,x+width,y+height);canvas.translate(x,y+Math.max(0,(height-layout.getHeight())/2));layout.draw(canvas);canvas.restore();
    }
}
