package app.space.notes.runtime;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;
import app.space.notes.domain.Diagram;
/** Bounded inline vector preview; uses the same renderer as editing/export, no bitmap copies. */
final class DiagramPreview extends View {
 private final Diagram diagram;private final NotesDesign design;
 DiagramPreview(Context context,Diagram diagram,NotesDesign design){super(context);this.diagram=diagram;this.design=design;}
 @Override protected void onDraw(Canvas canvas){super.onDraw(canvas);if(diagram.nodes.isEmpty()){Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);paint.setColor(design.muted);paint.setTextSize(16*getResources().getDisplayMetrics().scaledDensity);canvas.drawText("Empty diagram · tap to edit",16,getHeight()/2f,paint);return;}
  float left=Float.MAX_VALUE,top=Float.MAX_VALUE,right=-Float.MAX_VALUE,bottom=-Float.MAX_VALUE;
  for(Diagram.Node node:diagram.nodes){left=Math.min(left,node.x);top=Math.min(top,node.y);right=Math.max(right,node.x+node.width);bottom=Math.max(bottom,node.y+node.height);}
  float pad=16*getResources().getDisplayMetrics().density,scale=Math.min((getWidth()-pad*2)/Math.max(1,right-left),(getHeight()-pad*2)/Math.max(1,bottom-top));
  canvas.save();canvas.clipRect(0,0,getWidth(),getHeight());canvas.translate((getWidth()-(right-left)*scale)/2-left*scale,(getHeight()-(bottom-top)*scale)/2-top*scale);canvas.scale(scale,scale);DiagramRenderer.draw(canvas,diagram,design.dark,null,design);canvas.restore();
 }
}
