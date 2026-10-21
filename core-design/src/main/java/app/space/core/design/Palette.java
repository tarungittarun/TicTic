package app.space.core.design;

/**
 * The six resolved colours every MySpace screen draws with, plus the dark flag that
 * produced them.
 *
 * <p>Extracted from the palette computation that lived inline in {@code MainActivity}
 * (SUITE-PLAN.md §3.1). Passing one immutable value object instead of six ints is what
 * stops each suite app from resolving the theme slightly differently — the reason the
 * clock's muted text and a future expense app's muted text can drift apart is that each
 * recomputes it.
 *
 * <p>Fields are {@code public final} rather than getters: this is a plain value with no
 * invariants to protect, and the codebase style is dense field access.
 */
public final class Palette {

    /** True when the resolved appearance is dark, whether chosen or following system. */
    public final boolean dark;
    /** The user's selected accent, one of {@code AppTheme.ACCENT_COLORS}. */
    public final int accent;
    /** Window and page background. */
    public final int background;
    /** Card and panel fill. */
    public final int surface;
    /** Primary text. */
    public final int text;
    /** Secondary text. Held to a stronger contrast floor than a naive 50% alpha. */
    public final int muted;
    /** Hairline borders and dividers. */
    public final int border;

    public Palette(boolean dark, int accent, int background, int surface, int text, int muted, int border) {
        this.dark = dark;
        this.accent = accent;
        this.background = background;
        this.surface = surface;
        this.text = text;
        this.muted = muted;
        this.border = border;
    }

    /** The palette the clock has shipped since v1.10; kept so extraction is behaviour-preserving. */
    public static Palette resolve(boolean dark, int accent) {
        return dark
                ? new Palette(true, accent, 0xff0c1514, 0xff1b2926, 0xfff1f5f3, 0xffaabbb5, 0xff354a44)
                : new Palette(false, accent, 0xffedf2ef, 0xfffcfdfc, 0xff18312f, 0xff5d7069, 0xffd5e0db);
    }
}
