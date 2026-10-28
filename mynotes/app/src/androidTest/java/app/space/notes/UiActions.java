package app.space.notes;
import android.view.*;
import android.widget.Button;
import java.util.*;
import static org.junit.Assert.*;
/** Open real disclosure controls when migrating journeys; never force visibility or bypass handlers. */
final class UiActions {
 static boolean matches(View view,String target){String name=view.getContentDescription()==null?"":view.getContentDescription().toString();return target.equals(name) || (target.equals("+ Text") && name.equals("New text note"));}
 static View named(View root,String name){if(name.contentEquals(root.getContentDescription()==null?"":root.getContentDescription()))return root;if(root instanceof ViewGroup){ViewGroup g=(ViewGroup)root;for(int i=0;i<g.getChildCount();i++){View found=named(g.getChildAt(i),name);if(found!=null)return found;}}return null;}
 static boolean inside(View view,View parent){for(View p=view;p!=null;p=p.getParent() instanceof View?(View)p.getParent():null)if(p==parent)return true;return false;}
 static void reveal(View view){View root=view.getRootView();View close=named(root,"Close tools");if(close!=null && close.isShown()){View overlay=(View)close.getParent().getParent().getParent();boolean hidden=false;for(ViewParent p=view.getParent();p instanceof View;p=p.getParent())if(((View)p).getVisibility()==View.GONE)hidden=true;if(!inside(view,overlay) || hidden)close.performClick();}List<View> ancestors=new ArrayList<>();for(ViewParent p=view.getParent();p instanceof View;p=p.getParent())ancestors.add((View)p);Collections.reverse(ancestors);for(View parent:ancestors)if(parent.getVisibility()==View.GONE && parent.getTag() instanceof Button){Button launcher=(Button)parent.getTag();if(!launcher.isShown())reveal(launcher);launcher.performClick();}assertTrue("Control must be shown after opening its category",view.isShown());view.requestRectangleOnScreen(new android.graphics.Rect(0,0,view.getWidth(),view.getHeight()),true);}
}
