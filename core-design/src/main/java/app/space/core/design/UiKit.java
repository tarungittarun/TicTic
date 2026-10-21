package app.space.core.design;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.content.res.ColorStateList;
import android.widget.Switch;
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

    /**
     * A pill-shaped button: the app typeface, bold, centred, on a rounded fill.
     *
     * <p>Used for every primary action across all six clock tabs, so it is the single
     * most repeated control in the product.
     */
    public TextView pillButton(String title, int color, int ink) {
        TextView b = label(title, 14, ink, true);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(16), dp(11), dp(16), dp(11));
        b.setBackground(shape(color, 18));
        return b;
    }

    /**
     * A card honouring the user's layout density, elevation, border and glass settings.
     *
     * <p>The most settings-sensitive control in the design system: five preferences and
     * the palette all feed one drawable. Every suite app's content sits inside these, so
     * a card drawn here cannot drift from a card drawn there.
     *
     * <p>Note the local {@code fill} — the original in {@code MainActivity} called this
     * variable {@code bg}, shadowing the Activity's {@code bg} palette field in the same
     * method. Renamed because a shadowed palette field is how a card silently picks up the
     * page background instead of its own fill.
     */
    public LinearLayout card() {
        LinearLayout c = vbox();
        int density = AppPrefs.prefs(context).getInt("app_layout_density", 1);
        int horizontal = density == 0 ? 13 : density == 2 ? 22 : 18;
        int vertical = density == 0 ? 11 : density == 2 ? 20 : 16;
        c.setPadding(dp(horizontal), dp(vertical), dp(horizontal), dp(vertical));
        int elevation = AppPrefs.prefs(context).getInt("app_card_elevation", 1);
        int cardColor = glassEnabled() ? palette.surface : cardFillForElevation(elevation);
        Drawable fill = glassEnabled()
                ? glassSurfaceDrawable(cardColor, 22,
                        AppPrefs.prefs(context).getInt("app_glass_opacity", GlassPreset.OPACITY[0]),
                        AppPrefs.prefs(context).getInt("app_glass_finish", GlassPreset.FINISH[0]),
                        AppPrefs.prefs(context).getInt("app_glass_depth", GlassPreset.DEPTH[0]))
                : shape(cardColor, 22);
        int borderStyle = AppPrefs.prefs(context).getInt("app_card_border", 1);
        if (borderStyle > 0 && fill instanceof GradientDrawable) {
            ((GradientDrawable) fill).setStroke(dp(borderStyle == 2 ? 1.6f : 1),
                    borderStyle == 2 ? palette.accent : palette.border);
        }
        c.setBackground(fill);
        int glassDepth = glassEnabled()
                ? AppPrefs.prefs(context).getInt("app_glass_depth", GlassPreset.DEPTH[0]) : 0;
        float z = elevation == 0 ? 0f : elevation == 2 ? 9f + glassDepth * .05f : 4f + glassDepth * .025f;
        c.setElevation(dp(z));
        return c;
    }

    /**
     * A section heading with an optional caption, added to {@code parent}.
     *
     * <p>Takes the parent rather than assuming one: the version in {@code MainActivity}
     * wrote into that Activity's own {@code body} field, which is precisely what stopped a
     * screen from being lifted out of it. A suite app's screen owns its container.
     */
    public void sectionTitle(LinearLayout parent, String title, String caption) {
        TextView t = label(title, 24, palette.text, true);
        LinearLayout.LayoutParams p = lp(-1, -2);
        p.topMargin = dp(10);
        parent.addView(t, p);
        if (caption != null) {
            TextView c = label(caption, 13, palette.muted, false);
            LinearLayout.LayoutParams q = lp(-1, -2);
            q.topMargin = dp(4);
            parent.addView(c, q);
        }
    }

    /**
     * A toggle tinted with the palette: thumb and track both take the accent when checked,
     * and fall back to muted text and the border colour when not.
     *
     * <p>Android's stock {@code Switch} is grey in both states, which disappears against a
     * dark glass card, so every toggle in the product is built through this. Seven call
     * sites in the clock alone; a suite app drawing its own would be visibly off-palette.
     */
    public Switch appSwitch() {
        Switch sw = new Switch(context);
        int[][] states = {new int[]{android.R.attr.state_checked}, new int[]{}};
        sw.setThumbTintList(new ColorStateList(states, new int[]{palette.accent, palette.muted}));
        sw.setTrackTintList(new ColorStateList(states, new int[]{palette.accent, palette.border}));
        return sw;
    }

    /** Vertical spacer added to {@code parent}, for the same reason as {@link #sectionTitle}. */
    public void gap(LinearLayout parent, int h) {
        parent.addView(new View(context), lp(1, h));
    }

    /** Whether the user has turned on the app-glass material. */
    public boolean glassEnabled() {
        return AppPrefs.prefs(context).getBoolean("app_glass_enabled", false);
    }

    /**
     * Card fill for an elevation level: flat takes the page background, normal takes the
     * surface, and raised lightens the surface in dark mode but goes pure white in light
     * mode, where lightening an already-near-white surface would be invisible.
     */
    public int cardFillForElevation(int level) {
        if (level <= 0) return palette.background;
        if (level == 1) return palette.surface;
        return palette.dark ? Palette.blend(palette.surface, Color.WHITE, .18f) : Color.WHITE;
    }

    /** An app-glass panel at the 82% opacity floor, from the user's glass settings. */
    public Drawable glassSurfaceDrawable(int base, int radius, int opacity, int finish, int depth) {
        return AppSurfaceStyle.glass(context, base, radius, opacity, finish, depth, 82);
    }

    /**
     * The same panel with an explicit {@link GlassPreset} rather than the user's saved
     * glass settings — for surfaces that must look the same regardless of preference, such
     * as the Widget Studio preview, which shows what a widget will look like on a home
     * screen the app does not control.
     */
    public Drawable glassSurfaceDrawable(int base, int radius, int opacity, int finish, int depth, int preset) {
        return AppSurfaceStyle.glass(context, base, radius, opacity, finish, depth, preset, 82);
    }

    /** The page background: a subtle accent-tinted gradient when enabled, else flat. */
    public Drawable pageBackground() {
        return AppSurfaceStyle.pageBackground(context);
    }
}
