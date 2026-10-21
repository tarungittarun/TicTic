package app.space.core.design;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import app.space.core.data.AppTheme;

/**
 * The app's themed alert dialog, as an {@link AlertDialog.Builder} drop-in.
 *
 * <p>Extracted from {@code MainActivity}'s private inner class {@code TicAlertBuilder}
 * (SUITE-PLAN.md §6.1 step 1h-iv·3). It was the single largest blocker to moving screens
 * out of {@code :app}: 22 call sites across six editor dialogs
 * ({@code showReminderEditor}, {@code showAlarmEditor}, {@code showRoutineEditor},
 * {@code buildActivity}, {@code showAlarmDefaultsEditor}, {@code chooseTimerLength}) all
 * obtained their dialog from it, and a private inner class of the Activity cannot be
 * reached from a feature module. Four of the five remaining clock screens were gated on
 * this one class.
 *
 * <p>It is design-system code rather than clock code: the title row is built from
 * {@link UiKit#hbox()}, {@link UiKit#label} and {@link UiKit#dp} and coloured from the
 * {@link Palette}. Nothing here knows what an alarm or a routine is.
 *
 * <p>Three behaviours a rebuilt dialog would silently lose:
 * <ul>
 *   <li>a custom title row carrying its own close button, so a dialog is dismissible
 *       without a hardware back key or an outside tap;</li>
 *   <li>a rounded, stroked window panel in the surface colour, so dialogs match the pages
 *       instead of showing the platform rectangle;</li>
 *   <li>{@link GelMotion} applied to the window on show, so dialogs animate like
 *       everything else.</li>
 * </ul>
 */
public final class AppAlertBuilder extends AlertDialog.Builder {

    private final UiKit ui;
    private final Context themedContext;
    private final AlertDialog[] dialogRef = new AlertDialog[1];

    /**
     * @param context a dialog-themed context (callers wrap the host in a
     *                {@code ContextThemeWrapper} chosen from {@link AppTheme#dark})
     * @param ui      the design system for the current palette
     */
    public AppAlertBuilder(Context context, UiKit ui) {
        super(context);
        this.themedContext = context;
        this.ui = ui;
    }

    @Override
    public AlertDialog.Builder setTitle(CharSequence title) {
        if (title == null) return super.setTitle(title);
        Palette p = ui.palette();
        LinearLayout row = ui.hbox();
        row.setPadding(ui.dp(8), ui.dp(2), ui.dp(2), ui.dp(2));
        TextView heading = ui.label(title.toString(), 18, p.text, true);
        heading.setMaxLines(2);
        row.addView(heading, new LinearLayout.LayoutParams(0, -2, 1));
        TextView close = ui.label("\u00d7", 27, p.muted, true);
        close.setGravity(Gravity.CENTER);
        close.setContentDescription("Close dialog");
        close.setFocusable(true);
        close.setClickable(true);
        close.setBackground(ui.shape(AppTheme.dark(themedContext) ? 0xff263632 : 0xffe5ece8, 22));
        row.addView(close, ui.lp(44, 44));
        close.setOnClickListener(v -> {
            if (dialogRef[0] != null) dialogRef[0].dismiss();
        });
        super.setCustomTitle(row);
        return this;
    }

    @Override
    public AlertDialog.Builder setTitle(int titleId) {
        return setTitle(themedContext.getText(titleId));
    }

    @Override
    public AlertDialog create() {
        AlertDialog dialog = super.create();
        dialogRef[0] = dialog;
        dialog.setCanceledOnTouchOutside(true);
        android.view.Window window = dialog.getWindow();
        if (window != null) {
            Palette p = ui.palette();
            Drawable panel = ui.shape(p.surface, 24);
            if (panel instanceof GradientDrawable) ((GradientDrawable) panel).setStroke(ui.dp(1), p.border);
            window.setBackgroundDrawable(panel);
        }
        return dialog;
    }

    @Override
    public AlertDialog show() {
        AlertDialog dialog = create();
        dialog.show();
        // The original called fitAlertDialog twice back to back. It is idempotent — it
        // only sets the window layout and caps scroll heights — so the repeat was
        // dropped rather than carried across.
        DialogFit.fit(ui, dialog);
        if (dialog.getWindow() != null) GelMotion.apply(dialog.getWindow().getDecorView(), ui.palette().accent);
        return dialog;
    }
}
