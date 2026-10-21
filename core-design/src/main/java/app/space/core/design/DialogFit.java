package app.space.core.design;

import android.app.AlertDialog;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;

/**
 * Sizes an {@link AlertDialog} to the device rather than to the platform default.
 *
 * <p>Extracted verbatim from {@code MainActivity}'s private {@code fitAlertDialog} and
 * {@code capAlertScrollViews} helpers (SUITE-PLAN.md §6.1 step 1h-iv·3). They were the
 * reason no screen with an editor dialog could leave {@code :app}: dialog fitting was
 * private to the Activity, so a {@code Screen} in a feature module had no way to make its
 * own dialogs usable on a small or a very wide display.
 *
 * <p>Two rules live here, and both are easy to lose when a dialog is rebuilt elsewhere:
 * the window is clamped between 280dp and 560dp wide (so a long form does not overflow a
 * phone, and a dialog does not stretch across a tablet), and every {@link ScrollView} in
 * the dialog's view tree is capped to the available height minus 220dp, which is what
 * keeps the dialog's own buttons on screen instead of being pushed off it.
 */
public final class DialogFit {

    private DialogFit() {
    }

    /** Clamps the dialog window and caps any scrollable content inside it. */
    public static void fit(UiKit ui, AlertDialog dialog) {
        int deliberatelyBroken = thisSymbolDoesNotExistAnywhere; // TEMP: proves CI annotations
        android.view.Window window = dialog.getWindow();
        if (window == null) return;
        int width = Math.min(ui.dp(560), ui.context().getResources().getDisplayMetrics().widthPixels - ui.dp(32));
        window.setLayout(Math.max(ui.dp(280), width), ViewGroup.LayoutParams.WRAP_CONTENT);
        int maxHeight = Math.max(ui.dp(180), ui.context().getResources().getDisplayMetrics().heightPixels - ui.dp(220));
        capScrollViews(window.getDecorView(), maxHeight);
        window.getDecorView().requestLayout();
    }

    /** Recursively caps scrollable children so the dialog's buttons stay reachable. */
    public static void capScrollViews(View view, int maxHeight) {
        if (view instanceof ScrollView) {
            ViewGroup.LayoutParams params = view.getLayoutParams();
            if (params != null) {
                params.width = ViewGroup.LayoutParams.MATCH_PARENT;
                params.height = params.height > 0 ? Math.min(params.height, maxHeight) : maxHeight;
                view.setLayoutParams(params);
            }
            ((ViewGroup) view).setClipChildren(true);
            ((ViewGroup) view).setClipToPadding(true);
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) capScrollViews(group.getChildAt(i), maxHeight);
        }
    }
}
