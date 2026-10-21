package app.space.core.design;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;
import app.space.core.data.AppPrefs;

/**
 * The drawing primitives every MySpace screen builds with.
 *
 * <p>Extracted from {@code MainActivity}'s private helpers (SUITE-PLAN.md §6.1 step
 * 1h-i) because they are the prerequisite for moving any screen into a
 * {@code :feature-*} module: every one of the clock's six tabs builds its content from
 * {@code dp}, {@code vbox}, {@code hbox}, {@code lp}, {@code label} and {@code shape},
 * and all six were private to one Activity. A feature module cannot reach them, so no
 * screen could move until they had a shared home.
 *
 * <p>Only genuinely self-contained primitives are here. {@code card()},
 * {@code sectionTitle()}, {@code gap()} and {@code glassSurfaceDrawable()} stayed behind
 * in {@code MainActivity} for now: they write into that Activity's own {@code body}
 * container or depend on the glass and elevation settings, so moving them is a separate
 * slice rather than part of this one.
 *
 * <p>Holds a {@link Palette} rather than reading colours itself, so a screen cannot
 * accidentally mix a stale palette with a fresh one. {@code MainActivity} rebuilds its
 * instance whenever the appearance changes.
 */
public final class UiKit {

    private final Context context;
    private final Palette palette;

    public UiKit(Context context, Palette palette) {
        if (context == null) throw new IllegalArgumentException("UiKit requires a Context");
        if (palette == null) throw new IllegalArgumentException("UiKit requires a Palette");
        this.context = context;
        this.palette = palette;
    }

    public Context context() {
        return context;
    }

    public Palette palette() {
        return palette;
    }

    /** Density-independent pixels, rounded the same way the clock has always rounded them. */
    public int dp(float x) {
        return (int) (x * context.getResources().getDisplayMetrics().density + .5f);
    }

    /** Vertical box. */
    public LinearLayout vbox() {
        LinearLayout l = new LinearLayout(context);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    /** Horizontal box, vertically centred — the row layout used across every screen. */
    public LinearLayout hbox() {
        LinearLayout l = new LinearLayout(context);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    /**
     * Layout params in dp, where a negative value means match-parent or wrap-content and
     * is passed through untouched.
     */
    public LinearLayout.LayoutParams lp(int w, int h) {
        return new LinearLayout.LayoutParams(w < 0 ? w : dp(w), h < 0 ? h : dp(h));
    }

    /**
     * The user's chosen app-wide typeface: Signature, Soft (serif), Rounded, Condensed or
     * Casual.
     *
     * <p>Read from preferences on each call rather than cached, because the font choice
     * can change while a screen is open and the clock re-renders on that change.
     */
    public Typeface typeface() {
        switch (AppPrefs.prefs(context).getInt("app_font_style", 0)) {
            case 1: return Typeface.SERIF;
            case 2: return Typeface.create("sans-serif-rounded", Typeface.NORMAL);
            case 3: return Typeface.create("sans-serif-condensed", Typeface.NORMAL);
            case 4: return Typeface.create("casual", Typeface.NORMAL);
            default: return Typeface.create("sans-serif", Typeface.NORMAL);
        }
    }

    /** A text view in the app typeface, vertically centred, in the given colour and size. */
    public TextView label(String s, float size, int color, boolean bold) {
        TextView t = new TextView(context);
        t.setText(s);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setTypeface(typeface(), bold ? Typeface.BOLD : Typeface.NORMAL);
        t.setGravity(Gravity.CENTER_VERTICAL);
        return t;
    }

    /**
     * A solid rounded rectangle whose corner radius follows the user's corner style.
     *
     * <p>The radius floor of 3 keeps a "Crisp" setting from producing a hard corner that
     * reads as a rendering bug next to the rest of the rounded UI.
     */
    public Drawable shape(int color, int radius) {
        int shapedRadius = Math.max(3, Math.round(radius * cornerFactor(cornerStyle(context))));
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(shapedRadius));
        return d;
    }

    /** The user's corner style: 0 Signature, 1 Soft, 2 Crisp, 3 Capsule. */
    public static int cornerStyle(Context c) {
        return AppPrefs.prefs(c).getInt("app_shape_style", 0);
    }

    /**
     * Multiplier applied to a corner radius for the given style.
     *
     * <p>Previously duplicated: {@code MainActivity.shape()} and
     * {@code AppGlassDrawable.cornerScale()} each carried their own copy of these four
     * factors. Two copies of one design decision is how a solid card and a glass panel
     * end up with visibly different corners after someone tunes one of them, so both now
     * read this.
     */
    public static float cornerFactor(int style) {
        return style == 1 ? 1.35f : style == 2 ? .58f : style == 3 ? 1.8f : 1f;
    }
}
