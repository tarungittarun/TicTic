package app.space.notes.runtime;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import app.space.notes.domain.Diagram;
import java.util.Collections;
/** Shape picker preview uses exactly the same vector geometry as the canvas and exported PDF. */
final class DiagramShapeIcon extends Drawable {
 private final Diagram diagram;private final boolean dark;
 DiagramShapeIcon(String shape,boolean dark){this.dark=dark;diagram=new Diagram(Collections.singletonList(new Diagram.Node("preview",shape,"",0,0,200,140,0)),Collections.emptyList());}
 @Override public void draw(Canvas canvas){int save=canvas.save();canvas.translate(getBounds().left,getBounds().top);canvas.scale(getBounds().width()/206f,getBounds().height()/146f);canvas.translate(3,3);DiagramRenderer.draw(canvas,diagram,dark,null);canvas.restoreToCount(save);}
 @Override public void setAlpha(int alpha){}
 @Override public void setColorFilter(ColorFilter filter){}
 @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
