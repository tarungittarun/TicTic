package app.space.notes.runtime;
import android.view.*;
import android.widget.FrameLayout;
import androidx.test.platform.app.InstrumentationRegistry;
import app.space.notes.domain.Diagram;
import org.junit.Test;
import static org.junit.Assert.*;

public class DiagramResizeViewTest {
 private Diagram original(){return Diagram.empty().put(new Diagram.Node("a","decision","Question",200,200,200,120,1));}
 private DiagramView view(int[] commits){android.content.Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();DiagramView view=new DiagramView(context,original(),false,new DiagramView.Listener(){public void selected(String id){}public void changed(Diagram diagram){commits[0]++;}});new FrameLayout(context).addView(view);view.measure(View.MeasureSpec.makeMeasureSpec(800,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(1000,View.MeasureSpec.EXACTLY));view.layout(0,0,800,1000);view.show(original(),"a");return view;}
 private void event(DiagramView view,int action,float x,float y){long now=android.os.SystemClock.uptimeMillis();MotionEvent event=MotionEvent.obtain(now,now,action,x,y,0);assertTrue(view.onTouchEvent(event));event.recycle();}
 @Test public void cornerDragPreviewsThenCommitsExactlyOnce(){InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{int[] commits={0};DiagramView view=view(commits);event(view,0,400,320);event(view,2,500,400);assertEquals(0,commits[0]);assertEquals(300,view.diagram().find("a").width);event(view,1,500,400);assertEquals(1,commits[0]);assertEquals(200,view.diagram().find("a").height);assertEquals(200,view.diagram().find("a").x);});}
 @Test public void cancelledResizeRestoresOriginalAndNeverCommits(){InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{int[] commits={0};DiagramView view=view(commits);String before=view.diagram().fingerprint();event(view,0,200,200);event(view,2,100,100);assertNotEquals(before,view.diagram().fingerprint());event(view,3,100,100);assertEquals(before,view.diagram().fingerprint());assertEquals(0,commits[0]);});}
 @Test public void draggingInsideSelectedShapeStillMovesRatherThanResizes(){InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{int[] commits={0};DiagramView view=view(commits);event(view,0,300,260);event(view,2,350,310);event(view,1,350,310);Diagram.Node node=view.diagram().find("a");assertEquals(250,node.x);assertEquals(250,node.y);assertEquals(200,node.width);assertEquals(120,node.height);assertEquals(1,commits[0]);});}
}
