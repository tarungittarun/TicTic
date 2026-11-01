package app.space.notes.runtime;

import android.content.res.Configuration;
import android.view.*;
import android.widget.*;
import java.util.*;

/** Reparents actual editor controls, never duplicates actions or changes text fields. */
final class EditorRails {
    final LinearLayout root,stage; final ScrollView right; private View panel;
    static EditorRails install(LinearLayout root,LinearLayout top,View writing,LinearLayout dock){
        if(root.getResources().getConfiguration().orientation!=Configuration.ORIENTATION_LANDSCAPE)return null;
        return new EditorRails(root,top,writing,dock);
    }
    private EditorRails(LinearLayout root,LinearLayout top,View writing,LinearLayout dock){
        this.root=root;for(int i=top.getChildCount()-1;i>=0;i--){View child=top.getChildAt(i);if(child instanceof TextView && !(child instanceof Button)){top.removeView(child);root.addView(child,0,new LinearLayout.LayoutParams(-1,-2));}}root.removeView(top);root.removeView(writing);root.removeView(dock);
        stage=new LinearLayout(root.getContext());stage.setTag("editor-landscape-rails");stage.setOrientation(LinearLayout.HORIZONTAL);stage.setBaselineAligned(false);
        root.addView(stage,new LinearLayout.LayoutParams(-1,0,1));
        ScrollView left=rail(top,false);left.setContentDescription("Left editor tools");stage.addView(left,new LinearLayout.LayoutParams(dp(56),-1));
        stage.addView(writing,new LinearLayout.LayoutParams(0,-1,1));
        right=rail(dock,true);right.setContentDescription("Right editor tools");stage.addView(right,new LinearLayout.LayoutParams(dp(80),-1));
    }
    private int dp(int n){return Math.round(n*root.getResources().getDisplayMetrics().density);}
    private void buttons(View parent,List<Button> result){if(parent instanceof Button){result.add((Button)parent);return;}if(parent instanceof ViewGroup){ViewGroup group=(ViewGroup)parent;for(int i=0;i<group.getChildCount();i++)buttons(group.getChildAt(i),result);}}
    private ScrollView rail(LinearLayout controls,boolean reverse){
        List<Button> actions=new ArrayList<>();buttons(controls,actions);
        for(Button action:actions)if(action.getParent() instanceof ViewGroup)((ViewGroup)action.getParent()).removeView(action);
        controls.removeAllViews();controls.setOrientation(LinearLayout.VERTICAL);controls.setGravity(Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL);controls.setPadding(0,dp(4),0,dp(4));
        if(reverse)Collections.reverse(actions);
        for(Button action:actions){action.setMaxLines(2);action.setEllipsize(null);LinearLayout.LayoutParams cell=new LinearLayout.LayoutParams(-1,-2);cell.bottomMargin=dp(4);controls.addView(action,cell);}
        ScrollView viewport=new ScrollView(root.getContext());viewport.setFillViewport(true);viewport.setClipToPadding(false);viewport.addView(controls,new ScrollView.LayoutParams(-1,-2));viewport.post(()->viewport.fullScroll(View.FOCUS_DOWN));return viewport;
    }
    void showPanel(View next){if(panel!=null)hidePanel();if(next.getParent() instanceof ViewGroup)((ViewGroup)next.getParent()).removeView(next);panel=next;right.setVisibility(View.GONE);stage.addView(next,new LinearLayout.LayoutParams(Math.min(dp(360),Math.max(dp(220),(int)(root.getResources().getDisplayMetrics().widthPixels*.44f))),-1));}
    void hidePanel(){if(panel==null)return;stage.removeView(panel);panel=null;right.setVisibility(View.VISIBLE);}
}
