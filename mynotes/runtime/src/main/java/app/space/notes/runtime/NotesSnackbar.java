package app.space.notes.runtime;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Small app-owned snackbar for brief, reversible checklist actions. */
final class NotesSnackbar {
    private final NotesDesign design;
    private final FrameLayout shell,layer;
    private final Runnable timeout=new Runnable(){@Override public void run(){dismiss();}};
    private Runnable action;

    NotesSnackbar(NotesDesign design,FrameLayout shell){
        this.design=design;this.shell=shell;
        layer=new FrameLayout(design.context);layer.setTag("checklist-undo-snackbar");layer.setVisibility(View.GONE);layer.setClickable(false);layer.setFocusable(false);layer.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        shell.addView(layer,new FrameLayout.LayoutParams(-1,-1));
    }

    void show(String message,String actionLabel,String actionDescription,Runnable action){
        dismiss();this.action=action;
        LinearLayout bar=new LinearLayout(design.context);bar.setGravity(Gravity.CENTER_VERTICAL);bar.setPadding(design.dp(16),design.dp(4),design.dp(8),design.dp(4));bar.setMinimumHeight(design.dp(56));bar.setClickable(true);bar.setElevation(design.dp(8));
        GradientDrawable fill=design.shape(design.dark?0xff292d33:0xff272b30,16);fill.setStroke(design.dp(1),design.outline);bar.setBackground(fill);
        TextView label=design.label(message,14,android.graphics.Color.WHITE);label.setTag("checklist-undo-message");label.setMaxLines(2);label.setEllipsize(android.text.TextUtils.TruncateAt.END);label.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);bar.addView(label,new LinearLayout.LayoutParams(0,-2,1));
        Button undo=design.textAction(actionLabel,()->runAction());undo.setTag("checklist-undo-action");undo.setContentDescription(actionDescription);undo.setTextColor(design.dark?0xffffd27f:0xffffd27f);undo.setMinWidth(design.dp(64));undo.setMinHeight(design.dp(48));undo.setSingleLine(true);bar.addView(undo,new LinearLayout.LayoutParams(-2,-2));
        FrameLayout.LayoutParams barLayout=new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL);barLayout.setMargins(design.dp(16),0,design.dp(16),design.dp(16));layer.removeAllViews();layer.addView(bar,barLayout);layer.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);layer.setVisibility(View.VISIBLE);label.sendAccessibilityEvent(android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED);
        layer.removeCallbacks(timeout);layer.postDelayed(timeout,recommendedTimeout());
    }

    private long recommendedTimeout(){
        long fallback=5000;
        if(Build.VERSION.SDK_INT>=29){AccessibilityManager manager=(AccessibilityManager)design.context.getSystemService(Context.ACCESSIBILITY_SERVICE);if(manager!=null)return manager.getRecommendedTimeoutMillis((int)fallback,AccessibilityManager.FLAG_CONTENT_TEXT|AccessibilityManager.FLAG_CONTENT_CONTROLS);}
        return fallback;
    }

    private void runAction(){Runnable pending=action;dismiss();if(pending!=null)pending.run();}

    void dismiss(){
        layer.removeCallbacks(timeout);action=null;layer.setVisibility(View.GONE);layer.removeAllViews();layer.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
    }
}
