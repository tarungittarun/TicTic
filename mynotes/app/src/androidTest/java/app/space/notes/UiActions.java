package app.space.notes;
import android.view.*;
import android.widget.Button;
import java.util.*;
import static org.junit.Assert.*;
/** Open real disclosure controls when migrating journeys; never force visibility or bypass handlers. */
final class UiActions {
 static void reveal(View view){List<View> ancestors=new ArrayList<>();for(ViewParent p=view.getParent();p instanceof View;p=p.getParent())ancestors.add((View)p);Collections.reverse(ancestors);for(View parent:ancestors)if(parent.getVisibility()==View.GONE && parent.getTag() instanceof Button)((Button)parent.getTag()).performClick();assertTrue("Control must be shown after opening its category",view.isShown());view.requestRectangleOnScreen(new android.graphics.Rect(0,0,view.getWidth(),view.getHeight()),true);}
}
