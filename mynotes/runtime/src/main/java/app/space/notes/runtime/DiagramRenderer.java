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
        Diagram.Node selectedNode=diagram.find(selected);
        for(Diagram.Node node:diagram.nodes){
            boolean highlighted=node.id.equals(selected) || (selectedNode!=null && !selectedNode.group.isEmpty() && selectedNode.group.equals(node.group));
            RectF r=new RectF(node.x,node.y,node.x+node.width,node.y+node.height);Path path=shape(node,r);
            if(!node.shape.equals("text")){paint.setStyle(Paint.Style.FILL);paint.setColor(NotesBoard.colour(node.color,dark));canvas.drawPath(path,paint);}
            paint.setStyle(Paint.Style.STROKE);paint.setColor(highlighted?Color.parseColor("#B17400"):ink);paint.setStrokeWidth(highlighted?6:3);
            if(!node.shape.equals("text") || highlighted)canvas.drawPath(path,paint);
            details(canvas,node,r,paint);
            float inset=node.shape.equals("decision")?node.width*.23f:node.shape.equals("data")?node.width*.17f:12;
            drawText(canvas,node.text,node.x+inset,node.y+8,node.width-2*inset,node.height-16,ink,24);
        }
    }
    public static float[] boundary(Diagram.Node n,float dx,float dy){
        float cx=n.x+n.width/2f,cy=n.y+n.height/2f;
        if(dx==0 && dy==0)return new float[]{cx,cy};
        if(!java.util.Arrays.asList("process","decision","ellipse","terminator","data","text").contains(n.shape)){
            float[] points=shape(n,new RectF(n.x,n.y,n.x+n.width,n.y+n.height)).approximate(.25f);float nearest=Float.POSITIVE_INFINITY;
            for(int i=3;i<points.length;i+=3){float ax=points[i-2],ay=points[i-1],ex=points[i+1]-ax,ey=points[i+2]-ay;float cross=dx*ey-dy*ex;if(Math.abs(cross)<.0001f)continue;float t=((ax-cx)*ey-(ay-cy)*ex)/cross,u=((ax-cx)*dy-(ay-cy)*dx)/cross;if(t>=0 && u>=0 && u<=1)nearest=Math.min(nearest,t);}
            if(!Float.isInfinite(nearest))return new float[]{cx+dx*nearest,cy+dy*nearest};
        }
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
            case "triangle":polygon(p,r,.5f,0,1,1,0,1);break;
            case "rightTriangle":polygon(p,r,0,0,1,1,0,1);break;
            case "pentagon":regular(p,r,5,false);break;
            case "hexagon":regular(p,r,6,false);break;
            case "octagon":regular(p,r,8,false);break;
            case "star":regular(p,r,10,true);break;
            case "trapezoid":polygon(p,r,.2f,0,.8f,0,1,1,0,1);break;
            case "arrow":polygon(p,r,0,.25f,.65f,.25f,.65f,0,1,.5f,.65f,1,.65f,.75f,0,.75f);break;
            case "chevron":polygon(p,r,0,0,.65f,0,1,.5f,.65f,1,0,1,.35f,.5f);break;
            case "cube":polygon(p,r,0,.2f,.7f,0,1,.2f,1,.8f,.3f,1,0,.8f);break;
            case "cylinder":p.moveTo(r.left,r.top+r.height()*.16f);p.cubicTo(r.left,r.top-r.height()*.05f,r.right,r.top-r.height()*.05f,r.right,r.top+r.height()*.16f);p.lineTo(r.right,r.bottom-r.height()*.16f);p.cubicTo(r.right,r.bottom+r.height()*.05f,r.left,r.bottom+r.height()*.05f,r.left,r.bottom-r.height()*.16f);p.close();break;
            case "document":p.moveTo(r.left,r.top);p.lineTo(r.right,r.top);p.lineTo(r.right,r.bottom-r.height()*.15f);p.cubicTo(r.right-r.width()*.35f,r.bottom-r.height()*.45f,r.left+r.width()*.35f,r.bottom+r.height()*.15f,r.left,r.bottom-r.height()*.15f);p.close();break;
            case "cloud":p.moveTo(r.left+r.width()*.15f,r.top+r.height()*.75f);p.cubicTo(r.left-r.width()*.07f,r.top+r.height()*.7f,r.left,r.top+r.height()*.35f,r.left+r.width()*.2f,r.top+r.height()*.35f);p.cubicTo(r.left+r.width()*.15f,r.top,r.left+r.width()*.55f,r.top-r.height()*.05f,r.left+r.width()*.6f,r.top+r.height()*.2f);p.cubicTo(r.right,r.top+r.height()*.05f,r.right,r.top+r.height()*.4f,r.right-r.width()*.06f,r.top+r.height()*.45f);p.cubicTo(r.right+r.width()*.15f,r.bottom,r.left+r.width()*.5f,r.bottom,r.left+r.width()*.45f,r.bottom-r.height()*.12f);p.cubicTo(r.left+r.width()*.2f,r.bottom+r.height()*.05f,r.left+r.width()*.1f,r.bottom-r.height()*.1f,r.left+r.width()*.15f,r.top+r.height()*.75f);p.close();break;
            case "semicircle":p.moveTo(r.left,r.bottom);p.cubicTo(r.left,r.top-r.height()*.33f,r.right,r.top-r.height()*.33f,r.right,r.bottom);p.close();break;
            case "ellipse":p.addOval(r,Path.Direction.CW);break;
            case "terminator":p.addRoundRect(r,r.height()/2,r.height()/2,Path.Direction.CW);break;
            default:p.addRect(r,Path.Direction.CW);
        }
        return p;
    }
    private static void polygon(Path p,RectF r,float... coordinates){for(int i=0;i<coordinates.length;i+=2){float x=r.left+coordinates[i]*r.width(),y=r.top+coordinates[i+1]*r.height();if(i==0)p.moveTo(x,y);else p.lineTo(x,y);}p.close();}
    private static void regular(Path p,RectF r,int count,boolean star){for(int i=0;i<count;i++){double angle=-Math.PI/2+i*2*Math.PI/count;float radius=star && i%2==1?.43f:1;float x=r.centerX()+(float)Math.cos(angle)*r.width()/2*radius,y=r.centerY()+(float)Math.sin(angle)*r.height()/2*radius;if(i==0)p.moveTo(x,y);else p.lineTo(x,y);}p.close();}
    private static void details(Canvas c,Diagram.Node n,RectF r,Paint paint){
        if(n.shape.equals("cylinder"))c.drawArc(r.left,r.top,r.right,r.top+r.height()*.32f,0,180,false,paint);
        if(n.shape.equals("cube")){Path p=new Path();p.moveTo(r.left,r.top+r.height()*.2f);p.lineTo(r.left+r.width()*.3f,r.top+r.height()*.4f);p.lineTo(r.right,r.top+r.height()*.2f);p.moveTo(r.left+r.width()*.3f,r.top+r.height()*.4f);p.lineTo(r.left+r.width()*.3f,r.bottom);c.drawPath(p,paint);}
    }
    private static void drawText(Canvas canvas,String text,float x,float y,float width,float height,int color,int size){
        TextPaint tp=new TextPaint(Paint.ANTI_ALIAS_FLAG);tp.setColor(color);tp.setTextSize(size);
        StaticLayout layout=StaticLayout.Builder.obtain(text,0,text.length(),tp,Math.max(1,(int)width)).setAlignment(Layout.Alignment.ALIGN_CENTER).setIncludePad(false).setEllipsize(TextUtils.TruncateAt.END).setMaxLines(Math.max(1,(int)(height/(size*1.25f)))).build();
        canvas.save();canvas.clipRect(x,y,x+width,y+height);canvas.translate(x,y+Math.max(0,(height-layout.getHeight())/2));layout.draw(canvas);canvas.restore();
    }
}
