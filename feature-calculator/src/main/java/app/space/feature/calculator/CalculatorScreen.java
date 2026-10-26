package app.space.feature.calculator;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.text.Selection;
import android.text.Spannable;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ScrollView;
import android.widget.TextView;

import app.space.core.data.AppLog;
import app.space.core.data.AppPrefs;
import app.space.core.design.GelMotion;
import app.space.core.design.TicIconView;
import app.space.core.design.UiKit;
import app.space.core.suite.Screen;
import app.space.core.suite.ScreenHost;

/**
 * The calculator, rebuilt a fourth time against the owner's on-device session of
 * 2026-10-25 -- thirteen numbered points, seven screenshots, and a shared log that
 * convicted the previous architecture: one "calc laid out" line after EVERY key press,
 * because every key press rebuilt the entire screen.
 *
 * <p>That rebuild was the root of four of his complaints at once. It is why the screen
 * blinked on every tap (point 4); why the cursor could never be placed or moved, because
 * the view holding the caret was destroyed and recreated between the tap and the next
 * (point 12); why transitions were impossible, because nothing survived to animate
 * (points 2 and 11); and why the display could look empty while the log showed keys
 * landing. So the rule of this revision: <b>a key press updates views in place and
 * rebuilds nothing.</b> The screen is built once per structural change -- orientation,
 * history open or closed, the memory chip appearing -- and everything else is a setText,
 * a weight, an alpha.
 *
 * <p>His other points, against his words:
 * <ul>
 *   <li><b>Bigger keys, his shapes</b> (1): the pad's margins tightened and the display's
 *       share trimmed so the keys get the space; basic keys are rounded SQUARES, the
 *       scientific panel's shorter rows rounded RECTANGLES, the two radii distinct.</li>
 *   <li><b>Smooth transitions</b> (2, 7, 11): the scientific panel grows and collapses by
 *       animating its row-block's weight; the history sheet expands into the display area
 *       the same way, with the current expression docked beneath it so the calculator is
 *       never more than a scroll up (8); the menu rises as a popup. Every one carries an
 *       alpha with it, and history rows, dock and pad keep separate colours, sizes and
 *       borders so the sections never blur into each other.</li>
 *   <li><b>No captions</b> (3): nothing on screen says "scientific" or "basic".</li>
 *   <li><b>A visible chevron</b> (5): a bordered, accent-coloured pill with a double
 *       chevron, flip-facing for open and closed.</li>
 *   <li><b>The mystery dot is gone</b> (6): the top-left pill that read as a button and a
 *       dot is replaced by a real History button carrying the clock-with-arrow glyph; the
 *       overflow became a labelled Menu pill, because an unlabelled dot was exactly what
 *       he could not read.</li>
 *   <li><b>History as Google sheets it</b> (7, 8, 9, 10): entries expand over the display
 *       with the current expression docked at the bottom; each line carries its timestamp
 *       -- "25th Oct, 17:15" -- to the right of the expression; tapping a line puts that
 *       line's EXPRESSION into the expression view, not merely its result; Clear and close
 *       sit in the sheet's header.</li>
 * </ul>
 *
 * <p>What earlier rounds built and this keeps: Google's key order and filled separated
 * buttons, the editable caret with whole-expression replay validation, the intercepted
 * swipe-down, framework select-and-copy on the display, postfix percent, the memory slot,
 * the fraction view, hardware keys through {@link #typeKey}, the landscape combined grid,
 * the three-step display shrink, haptics, and the parent-fit rule this screen died once
 * for. Every action still writes its outcome to the diagnostics log -- now one line per
 * action instead of two, because the layout no longer re-measures on every tap.
 */
public final class CalculatorScreen implements Screen {

    /** The basic pad in Google's order. Rounded squares. */
    private static final String[][] BASE_ROWS = {
            {"AC", "( )", "%", "÷"},
            {"7", "8", "9", "×"},
            {"4", "5", "6", "−"},
            {"1", "2", "3", "+"},
            {"0", ".", "⌫", "="},
    };

    /** Google's scientific rows, grown over the pad by the chevron. Rounded rectangles. */
    private static final String[][] PANEL_ROWS = {
            {"√(", "π", "^", "!"},
            {"Deg", "sin(", "cos(", "tan("},
            {"Inv", "e", "ln(", "log("},
    };

    /** Landscape: the scientific column-pair joined to the basic pad, Google's way. */
    private static final String[][] LANDSCAPE_ROWS = {
            {"√(", "π", "AC", "( )", "%", "÷"},
            {"Deg", "Inv", "7", "8", "9", "×"},
            {"sin(", "cos(", "4", "5", "6", "−"},
            {"tan(", "log(", "1", "2", "3", "+"},
            {"^", "!", "0", ".", "⌫", "="},
    };

    private static final String HISTORY_KEY = "calculator_history";
    private static final String MEMORY_KEY = "calculator_memory";
    private static final String DEGREES_KEY = "calculator_degrees";
    private static final String HAPTICS_KEY = "calculator_haptics";
    private static final String EFFECTS_KEY = "calculator_effects";
    private static final int SEL_COPY = 9101, SEL_CUT = 9102,
            SEL_PASTE = 9103, SEL_ALL = 9104;

    /** Round 10: the press-effect names offered under three dots ▸ Effects. */
    private static final String[] EFFECT_NAMES =
            {"Spring", "Squeeze", "Glow", "Lift", "Pulse"};
    private static final String[] EFFECT_HINTS = {
            "bounce back with overshoot", "squash like gel", "soften and dim",
            "rise off the pad", "quick fade pulse"};
    private static final int HISTORY_CAP = 50;
    private static final int ANIM_MS = 220;

    /** Key visual classes: four fills, the way Google's pad reads at a glance. */
    private static final int DIGIT = 0, OP = 1, CMD = 2, EQ = 3, TOG = 4;

    /** The multi-character caps, longest first, for replaying an edited expression. */
    /** The live answer under the expression: Google's preview pink, bright on dark. */
    private static final int PREVIEW_PINK = 0xFFF48FB1;
    // Round 20, his words: "the content in expression box should be bright like
    // led when in dark mode. And content should be pitch black when in light
    // mode." Every display ink branches on the resolved palette from here.
    private static final int[] LED_EXPRESSION = {0xFF00E5FF, 0xFF7C4DFF, 0xFFFF2D78};
    private static final int[] LED_PREVIEW = {0xFFFF4081, 0xFFFFAB40};
    private static final int PITCH_BLACK = 0xFF000000;

    private static final String[] TOKENS = {
            "asin(", "acos(", "atan(", "sin(", "cos(", "tan(", "log(", "ln(", "√(",
            "π", "e", "^", "!", "%", "(", ")", "×", "÷", "−", "+", ".",
            "0", "1", "2", "3", "4", "5", "6", "7", "8", "9",
    };

    private final ScreenHost host;
    private final StringBuilder expression = new StringBuilder();
    private String result = "";
    private boolean justEvaluated;
    private boolean historyOpen;
    private boolean panelOpen;
    private boolean invMode;
    private boolean degrees = true;
    private boolean degreesLoaded;
    private boolean showFraction;
    private int caret;
    private Context lastContext;

    // View references, so a key press never rebuilds anything.
    private DisplayEditText displayView;
    private TextView previewView;
    private LinearLayout historyList;
    private TextView historyTitle;
    private LinearLayout chevronRow;
    private LinearLayout currentBox;
    private PopupWindow historyPopup;
    private View rootView;
    private LinearLayout panelBox;
    private ChevronView chevron;
    private PopupWindow menuPopup;
    private LinearLayout padContainer;
    private ScrollView historyScroll;
    private SwipeBox displayArea;
    private float lastTouchX, lastTouchY;
    private boolean longPressHeld;
    private boolean touchMoved;
    private int dragHandle;
    private int menuSelA, menuSelB;
    private android.view.ActionMode currentSelectionMode;
    private java.util.Map<String, TextView> capButtons = new java.util.HashMap<>();

    public CalculatorScreen(ScreenHost host) {
        this.host = host;
    }

    @Override public String title() { return "Calculator"; }

    /** The calculator owns its full height and its own gestures: no host ScrollView. */
    @Override public boolean scrollHost() { return false; }

    @Override
    public View build(final Context context) {
        lastContext = context;
        capButtons = new java.util.HashMap<>();
        final UiKit ui = new UiKit(context, host.palette());
        final boolean landscape = context.getResources().getConfiguration().orientation
                == android.content.res.Configuration.ORIENTATION_LANDSCAPE;
        LinearLayout root = ui.vbox();
        root.setPadding(ui.dp(12), ui.dp(8), ui.dp(12), ui.dp(10));
        // The shell hands every screen's root wrap-content params, and a wrap-content parent
        // gives weighted children nothing to distribute. At first layout, ask the actual
        // parent for its actual height and fit it exactly; weights then distribute real
        // space on any host chrome and any screen size.
        root.post(new Runnable() {
            @Override public void run() {
                android.view.ViewParent parent = root.getParent();
                if (parent instanceof View) {
                    int avail = ((View) parent).getHeight();
                    if (avail > 0) {
                        android.view.ViewGroup.LayoutParams lp = root.getLayoutParams();
                        if (lp != null && lp.height != avail) {
                            lp.height = avail;
                            root.setLayoutParams(lp);
                        }
                        AppLog.event(context, "calc", "laid out: parent " + avail
                                + "px tall, pad " + (landscape ? LANDSCAPE_ROWS.length
                                : BASE_ROWS.length + (panelOpen ? PANEL_ROWS.length : 0))
                                + " rows, panel " + (panelOpen ? "open" : "closed"));
                    }
                }
            }
        });

        // ---- chrome row: a real History button, the memory chip, a labelled Menu ----
        LinearLayout modes = ui.hbox();
        modes.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(modes, ui.lp(-1, 38));
        LinearLayout historyBtn = new LinearLayout(context);
        historyBtn.setOrientation(LinearLayout.HORIZONTAL);
        historyBtn.setGravity(Gravity.CENTER);
        historyBtn.setBackground(pill(ui, ui.palette().surface, 19));
        historyBtn.setPadding(ui.dp(10), ui.dp(6), ui.dp(10), ui.dp(6));
        historyBtn.addView(new TicIconView(context, TicIconView.HISTORY, ui.palette().accent),
                ui.lp(18, 18));
        if (!degreesLoaded) {
            degrees = AppPrefs.prefs(context).getBoolean(DEGREES_KEY, true);
            degreesLoaded = true;
            AppLog.event(context, "calc", "angle mode loaded from prefs: "
                    + (degrees ? "DEG" : "RAD"));
        }
        historyBtn.setContentDescription("History");
        historyBtn.setId(R.id.calc_history_btn);
        modes.addView(historyBtn, ui.lp(-2, 34));
        historyBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { toggleHistory(context, ui); }
        });
        View spacer = new View(context);
        modes.addView(spacer, new LinearLayout.LayoutParams(0, -2, 1f));
        // Round 9: "keep only history button at left side and 3 dots in the right side
        // instead of menu." Nothing else lives in this row any more.
        LinearLayout dots = ui.vbox();
        dots.setGravity(Gravity.CENTER);
        dots.setId(R.id.calc_menu_btn);
        dots.setContentDescription("More options");
        dots.setPadding(ui.dp(12), ui.dp(5), ui.dp(6), ui.dp(5));
        for (int i = 0; i < 3; i++) {
            View d = new View(context);
            GradientDrawable dot = new GradientDrawable();
            dot.setShape(GradientDrawable.OVAL);
            dot.setColor(ui.palette().muted);
            d.setBackground(dot);
            LinearLayout.LayoutParams dzp = ui.lp(4, 4);
            if (i > 0) dzp.topMargin = ui.dp(3);
            dots.addView(d, dzp);
        }
        modes.addView(dots, ui.lp(-2, -2));
        dots.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { openMenu(context, ui, v); }
        });

        // ---- the display area: history sheet over a docked current expression ----
        displayArea = new SwipeBox(context);
        displayArea.setOrientation(LinearLayout.VERTICAL);
        root.addView(displayArea, new LinearLayout.LayoutParams(-1, 0, 3f));
        displayArea.listener = new SwipeBox.Listener() {
            @Override public void onSwipeDown() { toggleHistory(context, ui); }
        };

        // The owner's verdict of 2026-10-26 night: the in-layout sheet never earned his
        // trust for scrolling, so history lives in its own PopupWindow now -- a real
        // window with a real ScrollView, no parent left to fight over his drags.
        currentBox = ui.vbox();
        currentBox.setId(R.id.calc_dock);
        // Round 9: "no need of borders, instead keep it simple" and "make it like Google
        // calculator, plain and simple" -- the expression sits on the background itself,
        // no card, no stroke, no rule above or below.
        displayArea.logContext = context;
        LinearLayout.LayoutParams cbp = new LinearLayout.LayoutParams(-1, 0, 3f);
        cbp.topMargin = ui.dp(3);
        displayArea.addView(currentBox, cbp);
        buildDisplay(context, ui, currentBox);

        // ---- Google's chevron, bordered and accent-coloured so it reads at a glance ----
        if (!landscape) {
            chevronRow = ui.hbox();
            // His point tonight: the strip sits centred between display and pad, and ANY
            // tap in that strip toggles the scientific rows, not only the 48dp pill.
            chevronRow.setGravity(Gravity.CENTER);
            chevronRow.setId(R.id.calc_chevron_row);
            chevronRow.setContentDescription("Toggle the scientific keys");
            root.addView(chevronRow, ui.lp(-1, 40));
            chevron = new ChevronView(context, ui.palette().accent, ui.palette().surface);
            styleChevron(ui);
            chevronRow.addView(chevron, ui.lp(48, 40));
            chevron.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { togglePanel(context, ui); }
            });
            chevronRow.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { togglePanel(context, ui); }
            });
        }

        // ---- the pad: animated panel block over the always-present basic block ----
        padContainer = ui.vbox();
        padContainer.setId(R.id.calc_pad);
        root.addView(padContainer, new LinearLayout.LayoutParams(-1, 0, 5f));
        panelBox = ui.vbox();
        panelBox.setVisibility(panelOpen && !landscape ? View.VISIBLE : View.GONE);
        padContainer.addView(panelBox, new LinearLayout.LayoutParams(-1, 0,
                panelOpen && !landscape ? 3f : 0f));
        if (!landscape) {
            for (String[] row : PANEL_ROWS) addPadRow(context, ui, panelBox, row, 12);
        }
        LinearLayout baseBox = ui.vbox();
        padContainer.addView(baseBox, new LinearLayout.LayoutParams(-1, 0, 5f));
        for (String[] row : (landscape ? LANDSCAPE_ROWS : BASE_ROWS)) {
            addPadRow(context, ui, baseBox, row, landscape ? 12 : 18);
        }
        // Round 9: "have some animations in each button click" -- the suite's own press
        // motion (style and intensity from Settings, reduce-motion honoured) over every
        // clickable view the screen just built, so MyCalc gets it without a host too.
        GelMotion.apply(root, ui.palette().accent);
        rootView = root;
        return root;
    }

    private void styleChevron(UiKit ui) {
        // His point: the arrow read backwards. Closed means the extra rows are waiting
        // BELOW the strip, so the arrow points down; open, they fold away upward.
        chevron.setDirection(panelOpen ? ChevronView.UP : ChevronView.DOWN);
        chevron.setContentDescription(panelOpen ? "Close the extra keys" : "Open the extra keys");
    }

    /**
     * A chevron that is drawn, not typed. The U+2303/U+2304 glyphs the previous rounds used
     * are absent from some device fonts -- on the owner's phone the pill collapsed to an
     * unclickable sliver of border around nothing, which is his first point this round.
     * Two stroked V's on a bordered pill render identically on every Android ever shipped,
     * and the pill is now forty-eight by forty, comfortably a thumb target.
     */
    static final class ChevronView extends View {
        static final int UP = 0, DOWN = 1;
        private int direction = UP;
        private final android.graphics.Paint paint = new android.graphics.Paint();

        ChevronView(Context context, int color, int borderColor) {
            super(context);
            paint.setColor(color);
            paint.setStyle(android.graphics.Paint.Style.STROKE);
            paint.setStrokeWidth(context.getResources().getDisplayMetrics().density * 2.2f);
            paint.setStrokeCap(android.graphics.Paint.Cap.ROUND);
            paint.setAntiAlias(true);
            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(context.getResources().getDisplayMetrics().density * 20);
            // Round 9: "no need of borders" reaches the chevron too -- the arrow floats
            // on the background with no ring around it.
            bg.setColor(0x00000000);
            setBackground(bg);
        }

        void setDirection(int d) { direction = d; invalidate(); }

        @Override protected void onDraw(android.graphics.Canvas c) {
            float w = getWidth(), h = getHeight();
            float cx = w / 2f;
            float dx = w * 0.15f;
            float dy = h * 0.10f;
            float rowGap = h * 0.16f;
            // Two stacked V's: pointing up when the panel is closed (tap to grow the extra
            // keys upward over the pad), pointing down when it is open (tap to fold them).
            for (int i = 0; i < 2; i++) {
                float y = h / 2f + (i == 0 ? -rowGap / 2 : rowGap / 2);
                if (direction == UP) {
                    c.drawLine(cx - dx, y + dy, cx, y - dy, paint);
                    c.drawLine(cx, y - dy, cx + dx, y + dy, paint);
                } else {
                    c.drawLine(cx - dx, y - dy, cx, y + dy, paint);
                    c.drawLine(cx, y + dy, cx + dx, y - dy, paint);
                }
            }
        }
    }

    private static GradientDrawable pill(UiKit ui, int color, float radiusDp) {
        GradientDrawable d = new GradientDrawable();
        d.setCornerRadius(ui.dp(radiusDp));
        d.setColor(color);
        return d;
    }

    /** One pad row of filled, separated buttons. Radius differs by block: the owner asked
     *  for rounded squares on the basic pad and rounded rectangles on the scientific one. */
    private void addPadRow(Context context, UiKit ui, LinearLayout parent, String[] rowKeys,
                           float radiusDp) {
        LinearLayout row = ui.hbox();
        for (String rawKey : rowKeys) {
            final String key = labelFor(rawKey);
            TextView button = keyButton(context, ui, key, radiusDp);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -1, 1f);
            params.leftMargin = ui.dp(4);
            params.rightMargin = ui.dp(4);
            params.topMargin = ui.dp(4);
            params.bottomMargin = ui.dp(4);
            row.addView(button, params);
            capButtons.put(rawKey, button);
            button.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    if (AppPrefs.prefs(v.getContext()).getBoolean(HAPTICS_KEY, true)) {
                        v.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP);
                    }
                    press(context, labelFor(rawKey));
                }
            });
        }
        parent.addView(row, new LinearLayout.LayoutParams(-1, 0, 1f));
    }

    /** Inv renames the trigonometric caps in place, the way Google's Inv toggle does. */
    private String labelFor(String key) {
        if ("Deg".equals(key)) return degrees ? "Deg" : "Rad";
        if (!invMode) return key;
        // Round 9: "when inv is clicked in keypad, am able to see many other changes in
        // Google calculator, but not in our app" -- Google swaps the whole panel, not just
        // the trig row: sqrt becomes square, ln becomes e to the x, log becomes ten to the x.
        if ("sin(".equals(key)) return "sin⁻¹";
        if ("cos(".equals(key)) return "cos⁻¹";
        if ("tan(".equals(key)) return "tan⁻¹";
        if ("√(".equals(key)) return "x²";
        if ("ln(".equals(key)) return "eˣ";
        if ("log(".equals(key)) return "10ˣ";
        return key;
    }

    /** What an Inv-swapped cap actually inserts, or null for an ordinary key. */
    private static String invToken(String label) {
        if ("sin⁻¹".equals(label)) return "asin(";
        if ("cos⁻¹".equals(label)) return "acos(";
        if ("tan⁻¹".equals(label)) return "atan(";
        if ("x²".equals(label)) return "^2";
        if ("eˣ".equals(label)) return "e^(";
        if ("10ˣ".equals(label)) return "10^(";
        return null;
    }

    /** Relabel the toggle-driven caps without rebuilding a single view, and repaint the
     *  two toggles so Inv and Deg read as ON the way Google fills them. */
    private void relabelCaps() {
        for (java.util.Map.Entry<String, TextView> e : capButtons.entrySet()) {
            e.getValue().setText(labelFor(e.getKey()));
        }
        if (lastContext == null) return;
        UiKit ui = new UiKit(lastContext, host.palette());
        String[] toggles = {"Inv", "Deg"};
        for (String k : toggles) {
            TextView b = capButtons.get(k);
            if (b == null) continue;
            boolean on = ("Deg".equals(k) && !degrees) || ("Inv".equals(k) && invMode);
            GradientDrawable shape = new GradientDrawable();
            shape.setCornerRadius(ui.dp(12));
            shape.setColor(on ? mix(ui.palette().surface, ui.palette().accent, 0.42f)
                    : mix(ui.palette().surface, ui.palette().accent, 0.16f));
            b.setBackground(shape);
            b.setTextColor(on ? android.graphics.Color.WHITE : ui.palette().accent);
        }
    }

    private TextView keyButton(Context context, UiKit ui, String key, float radiusDp) {
        int kind = kindOf(key);
        boolean toggleOn = ("Deg".equals(key) && !degrees) || ("Inv".equals(key) && invMode);
        int fill;
        int ink;
        float size;
        int[] gradient = null;
        boolean dark = ui.palette().dark;
        switch (kind) {
            case EQ:
                fill = ui.palette().accent;
                gradient = new int[]{ui.palette().accent, PREVIEW_PINK};
                ink = android.graphics.Color.WHITE;
                size = 26;
                break;
            case CMD:
                fill = mix(ui.palette().surface, ui.palette().accent, 0.42f);
                ink = ui.palette().text;
                size = 22;
                break;
            case TOG:
                fill = toggleOn ? mix(ui.palette().surface, ui.palette().accent, 0.42f)
                        : mix(ui.palette().surface, ui.palette().accent, 0.16f);
                ink = toggleOn ? android.graphics.Color.WHITE : ui.palette().accent;
                size = 16;
                break;
            case OP:
                // Round 19: bright amber operators against the cool digit keys.
                // Round 20: light mode got cream keys with amber ink on them --
                // invisible. Bright amber on dark, deep amber on light.
                fill = mix(ui.palette().surface, dark ? 0xFFFFC94D : 0xFFB26A00, 0.16f);
                ink = dark ? 0xFFFFC94D : 0xFFB26A00;
                size = isScientificCap(key) ? 19 : "( )".equals(key) || "⌫".equals(key) ? 22 : 26;
                break;
            default:
                fill = ui.palette().surface;
                // Round 20: white digits on white keys was the worst of it.
                // Digits take the palette's own text colour: near-white on dark,
                // near-black on light.
                ink = ui.palette().text;
                size = isScientificCap(key) ? 19 : 30;
                break;
        }
        PadButton button = new PadButton(context);
        button.setText(key);
        button.setGravity(Gravity.CENTER);
        button.setSingleLine(true);
        button.setTextSize(size);
        button.setTextColor(ink);
        GradientDrawable shape = new GradientDrawable();
        shape.setCornerRadius(ui.dp(radiusDp));
        shape.setColor(fill);
        if (gradient != null) {
            shape.setOrientation(android.graphics.drawable.GradientDrawable.Orientation.LEFT_RIGHT);
            shape.setColors(gradient);
        }
        button.setBackground(shape);
        button.setContentDescription(key);
        return button;
    }

    /** Round 19: flat fills read dull. The equals key carries a left-to-right
     *  accent-to-pink gradient; see the EQ case in the key builder. */
    private static int kindOf(String key) {
        if ("=".equals(key)) return EQ;
        if ("AC".equals(key)) return CMD;
        if ("Deg".equals(key) || "Inv".equals(key)) return TOG;
        if ("÷".equals(key) || "×".equals(key) || "−".equals(key) || "+".equals(key)
                || "%".equals(key) || "( )".equals(key) || "⌫".equals(key)) return OP;
        if (isScientificCap(key)) return OP;
        return DIGIT;
    }

    private static boolean isScientificCap(String key) {
        return key.endsWith("(") || "^".equals(key) || "!".equals(key)
                || "π".equals(key) || "e".equals(key)
                || key.endsWith("⁻¹") || "x²".equals(key) || key.endsWith("ˣ");
    }

    private static int mix(int a, int b, float t) {
        int ar = (a >> 16) & 255, ag = (a >> 8) & 255, ab = a & 255;
        int br = (b >> 16) & 255, bg = (b >> 8) & 255, bb = b & 255;
        return 0xff000000 | ((int) (ar + (br - ar) * t) << 16)
                | ((int) (ag + (bg - ag) * t) << 8) | (int) (ab + (bb - ab) * t);
    }

    // ---- the docked current expression ----------------------------------------------------

    /** Selectable text with a caret: tap places the cursor, hold and drag select, and every
     *  key inserts at the cursor. The view is created once and only ever setText afterwards,
     *  which is what finally lets the caret survive between taps. */
    private void buildDisplay(Context context, UiKit ui, LinearLayout parent) {
        final SelectionSafeHorizontalScrollView exprScroll =
                new SelectionSafeHorizontalScrollView(context);
        exprScroll.setHorizontalScrollBarEnabled(false);
        exprScroll.setFillViewport(true);
        exprScroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        displayView = new DisplayEditText(context,
                Math.max(3, (int) (4 * context.getResources().getDisplayMetrics().density)));
        displayView.setId(R.id.calc_display);
        exprScroll.watched = displayView;
        // Round 18: "first keep the expression typed in the top part of expression
        // box." It hung off the BOTTOM of a tall box, floating mid-air in his
        // screenshot. Top it is, with the live answer beneath it.
        displayView.setGravity(Gravity.RIGHT | Gravity.TOP);
        // The teardrop bulbs hang below the text line; without reserved room they
        // clipped at the view's edge -- "the thing which user has to hold and change
        // the selected portion is not completely visible".
        displayView.setPadding(0, 0, 0, ui.dp(34));
        displayView.setMaxLines(1);
        displayView.setFocusable(true);
        displayView.setFocusableInTouchMode(true);
        // Round 19: "why can't you use default android which is very good and
        // robust" -- the framework's OWN highlight is back, in a bright pink
        // that reads as held on the dark surface. The hand-drawn band is gone.
        displayView.setHighlightColor(0x66F48FB1);
        // "The expression got typed are not looking enough poppy or bright. It looks
        // dull." Pure white on the dark surface, the way Google's own display reads.
        displayView.setTextColor(android.graphics.Color.WHITE);
        // Round 19: "the content text, expression, very thing looks dull. Make
        // it look stunning, colorful and bright." The typed expression is
        // painted with a cyan-violet-pink gradient across the box, not flat ink.
        themeInk(displayView, ui.palette().dark, LED_EXPRESSION, PITCH_BLACK);
        // The caret must be VISIBLE, his point: the theme's default was not showing on his
        // device, so it is drawn explicitly in the accent colour, two density pixels wide.
        displayView.setCursorVisible(true);
        final android.graphics.drawable.GradientDrawable cursorMark =
                new android.graphics.drawable.GradientDrawable();
        cursorMark.setColor(ui.palette().dark ? 0xFF00E5FF : PITCH_BLACK);
        cursorMark.setSize(ui.dp(3), ui.dp(24));
        // Round 10: his log shows 181 "cursor placed" lines and still no caret seen, so
        // the caret stops being the text's own colour and becomes the accent, four density
        // pixels wide -- the one colour on the screen nothing else shares.
        displayView.setCaretColors(ui.palette().dark ? 0xFF00E5FF : PITCH_BLACK,
                0x66B9BBF4);
        if (android.os.Build.VERSION.SDK_INT >= 29) {
            displayView.setTextCursorDrawable(cursorMark);
        } else {
            try {
                java.lang.reflect.Field f =
                        android.widget.TextView.class.getDeclaredField("mCursorDrawable");
                f.setAccessible(true);
                f.set(displayView,
                        new android.graphics.drawable.Drawable[]{cursorMark, cursorMark});
            } catch (Exception ignored) {
                // Below Q without the field, the theme's own cursor stands.
            }
        }
        displayView.post(new Runnable() {
            @Override public void run() { displayView.requestFocus(); }
        });
        // Round 19: the screen no longer owns a single gesture. This listener
        // only WATCHES -- it returns false for everything, so the stock editor
        // receives every event and runs its own caret placement, its own
        // long-press selection and its own handle drags. What it records is the
        // outcome his log needs: what is held after each finger lift.
        displayView.setOnTouchListener(new View.OnTouchListener() {
            @Override public boolean onTouch(View v, MotionEvent e) {
                if (e.getAction() == MotionEvent.ACTION_UP) {
                    AppLog.event(lastContext, "calc", "display gesture ended: "
                            + "selection " + displayView.getSelectionStart()
                            + ".." + displayView.getSelectionEnd() + " held");
                }
                return false;
            }
        });
        // Round 12: "select the expression in the required length ... copy / paste / cut
        // ... like Android by default". The handles and the floating menu are the
        // framework's own; this callback guarantees the four actions on every OEM and
        // writes the outcome of each to the log.
        final android.view.ActionMode.Callback selectionMenu =
                new android.view.ActionMode.Callback() {
            @Override public boolean onCreateActionMode(android.view.ActionMode mode,
                                                        android.view.Menu menu) {
                currentSelectionMode = mode;
                // Round 17: his log -- "selection cut: 0 chars removed" while the menu
                // stood open over a held range. On his device the selection can read
                // empty by the time the thumb reaches Cut, so the menu remembers what
                // it was raised over and acts on THAT when the live selection is gone.
                int ds = displayView.getSelectionStart();
                int de = displayView.getSelectionEnd();
                if (ds >= 0 && de > ds) {
                    menuSelA = ds;
                    menuSelB = de;
                }
                menu.clear();
                menu.add(0, SEL_COPY, 0, "Copy");
                menu.add(0, SEL_CUT, 1, "Cut");
                menu.add(0, SEL_PASTE, 2, "Paste");
                menu.add(0, SEL_ALL, 3, "Select all");
                AppLog.event(lastContext, "calc", "selection started: chars "
                        + menuSelA + ".." + menuSelB
                        + " held by the stock editor, handles and menu raised");
                AppLog.event(lastContext, "calc",
                        "selection menu opened: Copy, Cut, Paste, Select all");
                return true;
            }

            @Override public boolean onPrepareActionMode(android.view.ActionMode mode,
                                                         android.view.Menu menu) {
                return true;
            }

            @Override public boolean onActionItemClicked(android.view.ActionMode mode,
                                                         android.view.MenuItem item) {
                int ls = displayView.getSelectionStart();
                int le = displayView.getSelectionEnd();
                if (ls >= 0 && le > ls) {
                    menuSelA = ls;
                    menuSelB = le;
                }
                int a = dispToRaw(menuSelA);
                int b = dispToRaw(menuSelB);
                String raw = expression.toString();
                a = Math.max(0, Math.min(a, raw.length()));
                b = Math.max(a, Math.min(b, raw.length()));
                switch (item.getItemId()) {
                    case SEL_COPY:
                        copy(context, raw.substring(a, b));
                        AppLog.event(context, "calc", "selection copied: "
                                + (b - a) + " chars");
                        mode.finish();
                        return true;
                    case SEL_CUT:
                        copy(context, raw.substring(a, b));
                        String cut = raw.substring(0, a) + raw.substring(b);
                        expression.setLength(0);
                        expression.append(cut);
                        justEvaluated = false;
                        result = "";
                        caret = a;
                        refreshDisplay(context);
                        AppLog.event(context, "calc", "selection cut: " + (b - a)
                                + " chars removed, now " + readable(cut));
                        mode.finish();
                        return true;
                    case SEL_PASTE: {
                        String clip = clipboardText(context);
                        if (clip == null || clip.isEmpty()) {
                            AppLog.event(context, "calc", "paste refused: clipboard empty");
                            mode.finish();
                            return true;
                        }
                        String clean = clip.replace(",", "").replace("\n", "")
                                .replace("\r", "").replace('\u00d7', '*')
                                .replace('\u00f7', '/').replace('\u2212', '-');
                        String candidate = raw.substring(0, a) + clean + raw.substring(b);
                        if (wholeExpressionValid(candidate)) {
                            expression.setLength(0);
                            expression.append(candidate);
                            justEvaluated = false;
                            result = "";
                            caret = a + clean.length();
                            refreshDisplay(context);
                            AppLog.event(context, "calc", "paste inserted "
                                    + clean.length() + " chars at " + a);
                        } else {
                            AppLog.event(context, "calc", "paste refused: "
                                    + clean + " is not a valid expression here");
                        }
                        mode.finish();
                        return true;
                    }
                    case SEL_ALL: {
                        CharSequence txt = displayView.getText();
                        Selection.setSelection((Spannable) txt, 0, txt.length());
                        AppLog.event(context, "calc", "select all: "
                                + txt.length() + " display chars held");
                        return true;
                    }
                    default:
                        return false;
                }
            }

            @Override public void onDestroyActionMode(android.view.ActionMode mode) {
                currentSelectionMode = null;
                // Round 14: when the menu goes away the highlight goes with it, caret at
                // the end of what was held -- otherwise a ghost band stays painted over
                // the expression with nothing behind it.
                CharSequence gone = displayView.getText();
                if (gone instanceof Spannable && displayView.hasSelection()) {
                    int end = Math.max(0, Selection.getSelectionEnd(gone));
                    Selection.setSelection((Spannable) gone, end, end);
                    caret = dispToRaw(end);
                }
                AppLog.event(lastContext, "calc", "selection menu closed");
            }
        };
        displayView.setCustomSelectionActionModeCallback(selectionMenu);
        // Round 13: on his device the framework long-press never opened a selection at all
        // -- the log holds zero selection lines. So the screen runs its own: a long press
        // selects the whole number (or the operator) under the finger, raises the handles,
        // and starts the Cut/Copy/Paste/Select-all menu itself. Dragging the handles then
        // adjusts the length exactly as it does in any editor.
        // Round 19: the app-owned long press is GONE, and with it the shadow it
        // cast: a view whose long-click listener returns true never reaches
        // TextView's own long-press selection, which is why thirteen rounds of
        // "default android" never ran on his device. Hold-to-select, the
        // teardrop handles and the whole drag machinery are Android's own now --
        // "by hook or crook make it work" is answered by stepping out of the way.
        displayView.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(android.text.Editable s) {
                if (displayView.applying) return;
                // Cut, paste, drag-handle edits and IME-free hand typing all arrive here.
                // What validates joins the expression; what does not is reverted, and both
                // outcomes are logged, because a silent revert is a lie in the log.
                String txt = s.toString();
                String raw = txt.replace(",", "").replace('\u00d7', '*')
                        .replace('\u00f7', '/').replace('\u2212', '-');
                if (wholeExpressionValid(raw)) {
                    expression.setLength(0);
                    expression.append(raw);
                    justEvaluated = false;
                    result = "";
                    caret = Math.max(0, dispToRaw(displayView.getSelectionStart()));
                    AppLog.event(lastContext, "calc", "expression edited by hand to "
                            + readable(raw));
                    previewView.setText(preview());
                } else {
                    AppLog.event(lastContext, "calc", "hand edit refused: " + txt);
                    displayView.applying = true;
                    displayView.setText(readable(expression.toString()));
                    displayView.applying = false;
                    restoreCaret();
                }
            }
        });
        exprScroll.addView(displayView, new LinearLayout.LayoutParams(-2, -1));
        LinearLayout.LayoutParams sp = ui.lp(-1, 0);
        sp.weight = 1f;
        parent.addView(exprScroll, sp);
        previewView = ui.label(" ", 20, PREVIEW_PINK, false);
        // Round 19: the live answer glows pink-to-amber under the gradient
        // expression -- the two brightest things on the screen.
        themeInk(previewView, ui.palette().dark, LED_PREVIEW, PITCH_BLACK);
        previewView.setGravity(Gravity.RIGHT | Gravity.TOP);
        previewView.setMaxLines(1);
        previewView.setEllipsize(android.text.TextUtils.TruncateAt.START);
        parent.addView(previewView, ui.lp(-1, -2));
        refreshDisplay(context);
        displayView.requestFocus();
    }

    /** The in-place refresh: new text, new size, caret restored. No view is created here. */
    private void refreshDisplay(Context context) {
        if (displayView == null) return;
        String shown = typed();
        // Round 10: "both are little small and not looking good" -- the expression and the
        // answer read at display grade now: 56sp until it gets long, then stepping down.
        int size = shown.length() > 24 ? 28 : shown.length() > 16 ? 40
                : shown.length() > 10 ? 48 : 56;
        displayView.setTextSize(size);
        displayView.applying = true;
        displayView.setText(shown);
        displayView.applying = false;
        restoreCaret();
        if (!displayView.hasFocus()) displayView.requestFocus();
        previewView.setText(preview());
        // Google's two-line answer: while typing, the live result sits UNDER the
        // expression; once equals has spoken, the expression moves ABOVE the big answer.
        LinearLayout hostRow = (LinearLayout) previewView.getParent();
        int want = justEvaluated ? 0 : 1;
        if (hostRow != null && hostRow.indexOfChild(previewView) != want) {
            hostRow.removeView(previewView);
            hostRow.addView(previewView, want, ui_lp_wrap(hostRow));
        }
        previewView.setTextSize(justEvaluated ? 18 : 20);
    }

    private static LinearLayout.LayoutParams ui_lp_wrap(LinearLayout parent) {
        return new LinearLayout.LayoutParams(-1, -2);
    }

    private void restoreCaret() {
        if (justEvaluated || displayView == null) return;
        CharSequence cs = displayView.getText();
        if (cs instanceof Spannable) {
            int rawLen = dispToRaw(cs.length());
            int pos = rawToDisp(Math.max(0, Math.min(caret, rawLen)));
            displayView.fallbackCaret = pos;
            Selection.setSelection((Spannable) cs, pos, pos);
        }
    }

    // ---- history: its own window ----------------------------------------------------------

    /**
     * History as its own PopupWindow, the owner's verdict of 2026-10-26 night: "open a pop
     * window where I should be able to see the history of expressions and there it should
     * be able to scroll. Don't message here." A real window with a real ScrollView has no
     * parent layout left to fight over his drags -- every scroll failure so far lived in a
     * parent stealing or losing touches, and a popup window has no such parent.
     */
    private View buildHistoryPopup(final Context context, final UiKit ui) {
        LinearLayout box = ui.vbox();
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(ui.dp(16));
        bg.setColor(mix(ui.palette().background, ui.palette().surface, 0.85f));
        box.setBackground(bg);
        box.setPadding(ui.dp(8), ui.dp(6), ui.dp(8), ui.dp(8));
        LinearLayout head = ui.hbox();
        head.setGravity(Gravity.CENTER_VERTICAL);
        box.addView(head, ui.lp(-1, 46));
        historyTitle = ui.label("", 16, ui.palette().text, true);
        head.addView(historyTitle, new LinearLayout.LayoutParams(0, -2, 1f));
        TextView clear = ui.pillButton("Clear", ui.palette().surface, ui.palette().text);
        clear.setTextSize(13);
        // A wrapped pill inside a fixed header was the clipping he kept seeing: the pill
        // gets a fixed height that the header is guaranteed to contain.
        LinearLayout.LayoutParams clp = ui.lp(-2, 34);
        head.addView(clear, clp);
        clear.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                final int entries = entryCount(context);
                if (entries == 0) {
                    AppLog.event(context, "calc", "history clear skipped: already empty");
                    host.toast("History is already empty");
                    return;
                }
                AppLog.event(context, "calc", "history clear asked for confirmation: "
                        + entries + " entries at stake");
                new android.app.AlertDialog.Builder(context)
                        .setTitle("Clear history?")
                        .setMessage(entries + (entries == 1 ? " entry" : " entries")
                                + " will be removed. This cannot be undone.")
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton("Clear",
                                new android.content.DialogInterface.OnClickListener() {
                            @Override public void onClick(android.content.DialogInterface d, int w) {
                                AppPrefs.prefs(context).edit().remove(HISTORY_KEY).apply();
                                AppLog.event(context, "calc", "history cleared: " + entries
                                        + " entries removed, after confirmation");
                                host.toast("History cleared");
                                populateHistory(context, ui);
                            }
                        }).show();
            }
        });
        ChevronView close = new ChevronView(context, ui.palette().accent, ui.palette().surface);
        close.setDirection(ChevronView.UP);
        close.setId(R.id.calc_history_close);
        close.setContentDescription("Close history");
        LinearLayout.LayoutParams cp = ui.lp(40, 34);
        cp.leftMargin = ui.dp(6);
        head.addView(close, cp);
        close.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { closeHistory(context); }
        });
        historyScroll = new ScrollView(context);
        historyScroll.setId(R.id.calc_history_scroll);
        historyList = ui.vbox();
        historyScroll.addView(historyList, new LinearLayout.LayoutParams(-1, -2));
        box.addView(historyScroll, new LinearLayout.LayoutParams(-1, 0, 1f));
        populateHistory(context, ui);
        return box;
    }

    private void populateHistory(final Context context, final UiKit ui) {
        historyList.removeAllViews();
        String[][] rows = CalculatorRules.historyEntries(
                AppPrefs.prefs(context).getString(HISTORY_KEY, ""));
        historyTitle.setText(rows.length == 0 ? "History" : "History · " + rows.length);
        if (rows.length == 0) {
            historyList.addView(ui.label("No calculations yet. Results land here as you make them.",
                    13, ui.palette().muted, false), ui.lp(-1, -2));
            return;
        }
        // His third point: the newest belongs at the BOTTOM, with older entries above it,
        // so scrolling up walks back through time in the order it happened.
        for (int i = rows.length - 1; i >= 0; i--) {
            String[] rowEntry = rows[i];
            final String[] entry = rowEntry;
            final String expr = entry[0];
            final String value = entry[1];
            // Round 13: "design history page little attractive" -- each entry is a soft
            // filled card (no stroke, no border): the expression and its time on the first
            // line, the answer big beneath it. Tap reuses, long-press copies, as before.
            LinearLayout line = ui.vbox();
            GradientDrawable card = new GradientDrawable();
            card.setCornerRadius(ui.dp(14));
            card.setColor(mix(ui.palette().background, ui.palette().surface, 0.8f));
            line.setBackground(card);
            line.setPadding(ui.dp(14), ui.dp(10), ui.dp(14), ui.dp(12));
            LinearLayout.LayoutParams lineLp = ui.lp(-1, -2);
            lineLp.bottomMargin = ui.dp(6);
            historyList.addView(line, lineLp);
            LinearLayout top = ui.hbox();
            top.setGravity(Gravity.CENTER_VERTICAL);
            line.addView(top, ui.lp(-1, -2));
            TextView e = ui.label(groupIndian(expr), 14, ui.palette().text, false);
            e.setGravity(Gravity.LEFT);
            top.addView(e, new LinearLayout.LayoutParams(0, -2, 1f));
            TextView when = ui.label(stamp(context, entry[2]), 11, ui.palette().muted, false);
            when.setGravity(Gravity.RIGHT);
            LinearLayout.LayoutParams wp = ui.lp(-2, -2);
            wp.leftMargin = ui.dp(8);
            top.addView(when, wp);
            TextView r = ui.label("=  " + groupIndian(value), 20, android.graphics.Color.WHITE, true);
            r.setGravity(Gravity.LEFT);
            LinearLayout.LayoutParams rp = ui.lp(-1, -2);
            rp.topMargin = ui.dp(2);
            line.addView(r, rp);
            line.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    // His ninth point: the stored EXPRESSION comes into the expression view,
                    // not merely its result, so it can be edited and carried on from.
                    expression.setLength(0);
                    expression.append(expr.replace('×', '*').replace('÷', '/').replace('−', '-'));
                    justEvaluated = false;
                    result = "";
                    caret = expression.length();
                    AppLog.event(context, "calc", "history line reused: expression "
                            + expr + " into the expression view");
                    toggleHistory(context, ui);
                    refreshDisplay(context);
                }
            });
            line.setOnLongClickListener(new View.OnLongClickListener() {
                @Override public boolean onLongClick(View v) {
                    copy(context, value);
                    return true;
                }
            });
        }
        if (historyOpen && historyScroll != null) {
            historyScroll.post(new Runnable() {
                @Override public void run() { historyScroll.fullScroll(View.FOCUS_DOWN); }
            });
        }
    }

    /** "25th Oct, 17:15" -- the owner's format, ordinal day and all. */
    static String stamp(Context context, String millis) {
        if (millis == null || millis.isEmpty()) return "";
        try {
            long ts = Long.parseLong(millis);
            java.util.Calendar c = java.util.Calendar.getInstance();
            c.setTimeInMillis(ts);
            int day = c.get(java.util.Calendar.DAY_OF_MONTH);
            String suffix = day % 100 >= 11 && day % 100 <= 13 ? "th"
                    : day % 10 == 1 ? "st" : day % 10 == 2 ? "nd" : day % 10 == 3 ? "rd" : "th";
            String month = new java.text.SimpleDateFormat("MMM", java.util.Locale.ENGLISH)
                    .format(c.getTime());
            String time = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.ENGLISH)
                    .format(c.getTime());
            return day + suffix + " " + month + ", " + time;
        } catch (Exception e) {
            return "";
        }
    }

    /** Expand or collapse the sheet over the docked expression, animated both ways. */
    /** Round 17, "over all it feels like not polished for its lifecycle": a held
     *  selection belongs to the expression on screen. The moment the screen moves on
     *  -- clear, equals, history, the three-dot menu -- the floating menu and its
     *  highlight go with it instead of hanging over the next thing the thumb does. */
    private void closeSelectionMenu() {
        if (currentSelectionMode != null) currentSelectionMode.finish();
        menuSelA = 0;
        menuSelB = 0;
    }

    private void toggleHistory(final Context context, final UiKit ui) {
        if (historyPopup != null && historyPopup.isShowing()) {
            closeHistory(context);
            return;
        }
        closeSelectionMenu();
        historyOpen = true;
        View content = buildHistoryPopup(context, ui);
        int width = context.getResources().getDisplayMetrics().widthPixels - ui.dp(24);
        int height = (int) (context.getResources().getDisplayMetrics().heightPixels * 0.62f);
        historyPopup = new PopupWindow(content, width, height, true);
        historyPopup.setElevation(ui.dp(10));
        historyPopup.setAnimationStyle(R.style.CalcPopupAnim);
        int[] loc = new int[2];
        displayArea.getLocationInWindow(loc);
        historyPopup.showAtLocation(rootView, Gravity.TOP | Gravity.CENTER_HORIZONTAL,
                0, loc[1] + ui.dp(2));
        historyPopup.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override public void onDismiss() {
                historyOpen = false;
                if (displayArea != null) displayArea.historyShowing = false;
                AppLog.event(context, "calc", "history popup dismissed");
            }
        });
        if (displayArea != null) displayArea.historyShowing = true;
        AppLog.event(context, "calc", "history popup opened: " + entryCount(context)
                + " entries in their own scrollable window");
        if (historyScroll != null) {
            historyScroll.post(new Runnable() {
                @Override public void run() { historyScroll.fullScroll(View.FOCUS_DOWN); }
            });
        }
    }

    private void closeHistory(Context context) {
        if (historyPopup != null && historyPopup.isShowing()) historyPopup.dismiss();
        historyOpen = false;
        if (displayArea != null) displayArea.historyShowing = false;
        AppLog.event(context, "calc", "history popup closed by button");
    }

    /** The scientific panel grows over the basic pad by animating its block's weight, with
     *  an alpha ride so the caps arrive instead of popping. */
    private void togglePanel(final Context context, final UiKit ui) {
        panelOpen = !panelOpen;
        AppLog.event(context, "calc", panelOpen
                ? "scientific panel expanded: 3 rows added above the pad"
                : "scientific panel collapsed: back to the basic pad");
        styleChevron(ui);
        panelBox.setVisibility(View.VISIBLE);
        panelBox.setAlpha(panelOpen ? 0f : 1f);
        animateWeight(panelBox, panelOpen ? 0f : 3f, panelOpen ? 3f : 0f,
                new Runnable() {
                    @Override public void run() {
                        if (!panelOpen) panelBox.setVisibility(View.GONE);
                    }
                });
        ValueAnimator fade = ValueAnimator.ofFloat(panelOpen ? 0f : 1f, panelOpen ? 1f : 0f);
        fade.setDuration(ANIM_MS);
        fade.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override public void onAnimationUpdate(ValueAnimator a) {
                panelBox.setAlpha((Float) a.getAnimatedValue());
            }
        });
        fade.start();
    }

    private static void animateWeight(final View view, float from, float to, final Runnable end) {
        final android.view.ViewGroup.LayoutParams lp = view.getLayoutParams();
        if (!(lp instanceof LinearLayout.LayoutParams)) { if (end != null) end.run(); return; }
        final LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) lp;
        // Android's reduced-motion setting is a user request, not a suggestion: with
        // animators off the weights jump straight to their destination.
        if (!ValueAnimator.areAnimatorsEnabled()) {
            params.weight = to;
            view.setLayoutParams(params);
            if (end != null) end.run();
            return;
        }
        ValueAnimator a = ValueAnimator.ofFloat(from, to);
        a.setDuration(ANIM_MS);
        a.setInterpolator(new DecelerateInterpolator());
        a.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override public void onAnimationUpdate(ValueAnimator animator) {
                params.weight = (Float) animator.getAnimatedValue();
                view.setLayoutParams(params);
            }
        });
        if (end != null) a.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(android.animation.Animator animator) { end.run(); }
        });
        a.start();
    }

    // ---- chrome actions --------------------------------------------------------------------

    // ---- memory: a list the owner arranges himself ----------------------------------------

    private static java.util.List<String> memoryList(Context context) {
        java.util.List<String> out = new java.util.ArrayList<>();
        String stored = AppPrefs.prefs(context).getString(MEMORY_KEY, "");
        if (stored != null && !stored.isEmpty()) {
            for (String part : stored.split("\n")) {
                if (!part.isEmpty()) out.add(part);
            }
        }
        return out;
    }

    private static void saveMemory(Context context, java.util.List<String> list) {
        StringBuilder b = new StringBuilder();
        for (String v : list) {
            if (b.length() > 0) b.append('\n');
            b.append(v);
        }
        android.content.SharedPreferences.Editor ed = AppPrefs.prefs(context).edit();
        if (b.length() == 0) ed.remove(MEMORY_KEY);
        else ed.putString(MEMORY_KEY, b.toString());
        ed.apply();
    }

    /**
     * Round 9: the three dots carry exactly five things -- haptics on/off, add to memory,
     * clear memory, view memory, show as fraction -- and nothing else.
     */
    private void openMenu(final Context context, UiKit ui, View anchor) {
        closeSelectionMenu();
        AppLog.event(context, "calc", "menu opened from the three dots");
        java.util.List<String> items = new java.util.ArrayList<>();
        items.add(AppPrefs.prefs(context).getBoolean(HAPTICS_KEY, true)
                ? "Haptics: On" : "Haptics: Off");
        items.add("Add to memory");
        items.add("Clear memory");
        items.add("View memory");
        items.add(showFraction ? "Show as decimal" : "Show as fraction");
        items.add("Effects");
        LinearLayout box = ui.vbox();
        box.setBackground(pill(ui, ui.palette().surface, 16));
        box.setPadding(ui.dp(6), ui.dp(6), ui.dp(6), ui.dp(6));
        for (String item : items) {
            final String choice = item;
            TextView row = ui.label(item, 14, ui.palette().text, false);
            row.setPadding(ui.dp(14), ui.dp(11), ui.dp(14), ui.dp(11));
            box.addView(row, ui.lp(-1, -2));
            row.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    // Round 11: the row used to dismiss whatever popup existed AFTER the
                    // choice ran -- which killed the Effects picker the instant it opened.
                    // A choice that opens its own popup keeps it; anything else closes us.
                    PopupWindow self = menuPopup;
                    menuPopup = null;
                    onMenuChoice(context, ui, choice);
                    if (menuPopup == null && self != null) self.dismiss();
                }
            });
        }
        menuPopup = new PopupWindow(box, ui.dp(220), -2, true);
        menuPopup.setElevation(ui.dp(8));
        menuPopup.setAnimationStyle(R.style.CalcPopupAnim);
        menuPopup.showAsDropDown(anchor, 0, ui.dp(4), Gravity.RIGHT);
    }

    private void onMenuChoice(Context context, UiKit ui, String choice) {
        if (choice.startsWith("Haptics")) {
            boolean on = !AppPrefs.prefs(context).getBoolean(HAPTICS_KEY, true);
            AppPrefs.prefs(context).edit().putBoolean(HAPTICS_KEY, on).apply();
            AppLog.event(context, "calc", "haptics " + (on ? "on" : "off") + ", persisted");
            host.toast(on ? "Haptics on" : "Haptics off");
        } else if (choice.equals("Add to memory")) {
            String v = displayValue();
            try {
                Double.parseDouble(v);
            } catch (Exception e) {
                AppLog.event(context, "calc", "memory add refused: display holds " + v);
                host.toast("Nothing to store on the display");
                return;
            }
            java.util.List<String> list = memoryList(context);
            list.add(v);
            saveMemory(context, list);
            AppLog.event(context, "calc", "memory added: " + v
                    + ", memory now holds " + list.size());
            host.toast("Added to memory");
        } else if (choice.equals("Clear memory")) {
            int n = memoryList(context).size();
            saveMemory(context, new java.util.ArrayList<String>());
            AppLog.event(context, "calc", "memory cleared: " + n + " entries removed");
            host.toast("Memory cleared");
        } else if (choice.equals("View memory")) {
            openMemory(context, ui);
        } else if (choice.startsWith("Show as")) {
            showFraction = !showFraction;
            AppLog.event(context, "calc", showFraction
                    ? "result view: fraction" : "result view: decimal");
            refreshDisplay(context);
        } else if (choice.equals("Effects")) {
            openEffects(context, ui);
        }
    }

    /**
     * Round 10: "have atleast 4 animations and keep in in 3 dots as settings >> effects."
     * Five press feels, picked here, persisted, and honoured by every pad key from the next
     * press onward; reduce-motion and disabled animator scales still win over all of them.
     */
    private void openEffects(final Context context, UiKit ui) {
        final int current = PadButton.effectStyle(context);
        LinearLayout box = ui.vbox();
        box.setBackground(pill(ui, ui.palette().surface, 16));
        box.setPadding(ui.dp(6), ui.dp(6), ui.dp(6), ui.dp(6));
        TextView head = ui.label("Button effects", 13, ui.palette().muted, false);
        head.setPadding(ui.dp(14), ui.dp(8), ui.dp(14), ui.dp(4));
        box.addView(head, ui.lp(-1, -2));
        for (int i = 0; i < EFFECT_NAMES.length; i++) {
            final int idx = i;
            TextView row = ui.label((i == current ? "✓  " : "     ") + EFFECT_NAMES[i]
                    + "  ·  " + EFFECT_HINTS[i], 14,
                    i == current ? ui.palette().accent : ui.palette().text,
                    i == current);
            row.setPadding(ui.dp(14), ui.dp(11), ui.dp(14), ui.dp(11));
            box.addView(row, ui.lp(-1, -2));
            row.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    AppPrefs.prefs(context).edit().putInt(EFFECTS_KEY, idx).apply();
                    AppLog.event(context, "calc", "effects style → " + EFFECT_NAMES[idx]
                            + ", persisted");
                    host.toast(EFFECT_NAMES[idx] + " press effect on");
                    if (menuPopup != null) menuPopup.dismiss();
                }
            });
        }
        menuPopup = new PopupWindow(box, ui.dp(250), -2, true);
        menuPopup.setElevation(ui.dp(8));
        menuPopup.setAnimationStyle(R.style.CalcPopupAnim);
        menuPopup.showAtLocation(rootView, Gravity.CENTER, 0, 0);
        AppLog.event(context, "calc", "effects picker opened: current style "
                + EFFECT_NAMES[current]);
    }

    /**
     * Round 9, his fourth dot item: "a small popup window should be opened for seeing
     * memory available and user should be able keep required memory and arrange in
     * required order like move from one place to up/down." Tap a value to keep using it;
     * the two chevrons move it through the list, and the order persists.
     */
    private void openMemory(final Context context, final UiKit ui) {
        LinearLayout box = ui.vbox();
        box.setBackground(pill(ui, ui.palette().surface, 16));
        box.setPadding(ui.dp(8), ui.dp(6), ui.dp(8), ui.dp(8));
        LinearLayout head = ui.hbox();
        head.setGravity(Gravity.CENTER_VERTICAL);
        box.addView(head, ui.lp(-1, 40));
        head.addView(ui.label("Memory", 14, ui.palette().text, true),
                new LinearLayout.LayoutParams(0, -2, 1f));
        ChevronView close = new ChevronView(context, ui.palette().accent, ui.palette().surface);
        close.setDirection(ChevronView.UP);
        close.setId(R.id.calc_memory_close);
        close.setContentDescription("Close memory");
        head.addView(close, ui.lp(36, 30));
        final ScrollView scroll = new ScrollView(context);
        final LinearLayout list = ui.vbox();
        list.setId(R.id.calc_memory_list);
        scroll.addView(list, new LinearLayout.LayoutParams(-1, -2));
        box.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));
        final PopupWindow[] holder = new PopupWindow[1];
        close.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (holder[0] != null) holder[0].dismiss();
                AppLog.event(context, "calc", "memory popup closed by button");
            }
        });
        fillMemory(context, ui, list, holder);
        int width = context.getResources().getDisplayMetrics().widthPixels - ui.dp(72);
        int height = (int) (context.getResources().getDisplayMetrics().heightPixels * 0.42f);
        PopupWindow popup = new PopupWindow(box, width, height, true);
        popup.setElevation(ui.dp(10));
        popup.setAnimationStyle(R.style.CalcPopupAnim);
        int[] loc = new int[2];
        displayArea.getLocationInWindow(loc);
        popup.showAtLocation(rootView, Gravity.TOP | Gravity.CENTER_HORIZONTAL,
                0, loc[1] + ui.dp(2));
        holder[0] = popup;
        AppLog.event(context, "calc", "memory popup opened: "
                + memoryList(context).size() + " entries, movable up and down");
    }

    private void fillMemory(final Context context, final UiKit ui,
                            final LinearLayout list, final PopupWindow[] holder) {
        list.removeAllViews();
        final java.util.List<String> values = memoryList(context);
        if (values.isEmpty()) {
            list.addView(ui.label("Nothing stored yet. Add the display to memory from "
                    + "the three dots.", 13, ui.palette().muted, false), ui.lp(-1, -2));
            return;
        }
        for (int i = 0; i < values.size(); i++) {
            final int at = i;
            final String value = values.get(i);
            LinearLayout row = ui.hbox();
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, ui.dp(5), 0, ui.dp(5));
            list.addView(row, ui.lp(-1, -2));
            TextView label = ui.label(groupIndian(value), 17, ui.palette().text, false);
            row.addView(label, new LinearLayout.LayoutParams(0, -2, 1f));
            label.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    insertNumber(value);
                    refreshDisplay(context);
                    AppLog.event(context, "calc", "memory recalled: " + value
                            + " inserted, expression now " + readable(expression.toString()));
                    if (holder[0] != null) holder[0].dismiss();
                }
            });
            ChevronView up = new ChevronView(context, at == 0
                    ? mix(ui.palette().surface, ui.palette().text, 0.35f) : ui.palette().accent,
                    ui.palette().surface);
            up.setDirection(ChevronView.UP);
            up.setContentDescription("Move up from place " + (at + 1));
            row.addView(up, ui.lp(32, 28));
            up.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { moveMemory(context, ui, list, holder, at, -1); }
            });
            ChevronView down = new ChevronView(context, at == values.size() - 1
                    ? mix(ui.palette().surface, ui.palette().text, 0.35f) : ui.palette().accent,
                    ui.palette().surface);
            down.setDirection(ChevronView.DOWN);
            down.setContentDescription("Move down from place " + (at + 1));
            row.addView(down, ui.lp(32, 28));
            down.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { moveMemory(context, ui, list, holder, at, 1); }
            });
        }
    }

    private void moveMemory(Context context, UiKit ui, LinearLayout list,
                            PopupWindow[] holder, int at, int dir) {
        java.util.List<String> values = memoryList(context);
        int to = at + dir;
        if (at < 0 || at >= values.size() || to < 0 || to >= values.size()) {
            AppLog.event(context, "calc", "memory move refused: entry " + at
                    + " cannot go " + (dir < 0 ? "up" : "down"));
            return;
        }
        String moved = values.remove(at);
        values.add(to, moved);
        saveMemory(context, values);
        AppLog.event(context, "calc", "memory moved: " + moved + " from place "
                + (at + 1) + " to place " + (to + 1));
        fillMemory(context, ui, list, holder);
    }

    private static int entryCount(Context context) {
        return CalculatorRules.historyEntries(
                AppPrefs.prefs(context).getString(HISTORY_KEY, "")).length;
    }

    /**
     * The expression box as a real editor, the owner's sixth point: a visible blinking
     * cursor where he is typing, handles he can drag to a position, and the framework's
     * own select / copy / cut / paste menu over whatever he selected. Input type is null,
     * so no soft keyboard ever rises between him and the pad, and every hardware key is
     * handed straight back to the screen's typeKey door -- the editor displays and selects,
     * the pad and the keyboard type.
     */
    /**
     * Round 10: "the way how button react or animate is not good for now. Change it make it
     * look good." The pad keys own their press feel here -- dispatchTouchEvent is taken
     * over outright, so neither host's generic motion layer can sand it away -- and the
     * style comes from three dots ▸ Effects: Spring, Squeeze, Glow, Lift or Pulse.
     */
    static final class PadButton extends TextView {

        PadButton(Context context) {
            super(context);
        }

        static int effectStyle(Context c) {
            return Math.max(0, Math.min(4, AppPrefs.prefs(c).getInt(EFFECTS_KEY, 0)));
        }

        static boolean motionOff(Context c) {
            if (AppPrefs.prefs(c).getBoolean("app_reduce_motion", false)) return true;
            try {
                return !android.animation.ValueAnimator.areAnimatorsEnabled();
            } catch (Exception e) {
                return false;
            }
        }

        @Override public boolean dispatchTouchEvent(MotionEvent e) {
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    if (!isEnabled()) return false;
                    setPressed(true);
                    press(this, true);
                    return true;
                case MotionEvent.ACTION_MOVE:
                    if (e.getX() < 0 || e.getY() < 0
                            || e.getX() > getWidth() || e.getY() > getHeight()) {
                        setPressed(false);
                        press(this, false);
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                    setPressed(false);
                    press(this, false);
                    performClick();
                    return true;
                case MotionEvent.ACTION_CANCEL:
                    setPressed(false);
                    press(this, false);
                    return true;
                default:
                    return true;
            }
        }

        private static void prep(View v) {
            v.setPivotX(v.getWidth() / 2f);
            v.setPivotY(v.getHeight() / 2f);
        }

        private static void press(View view, boolean down) {
            Context c = view.getContext();
            if (motionOff(c)) {
                view.setScaleX(1f);
                view.setScaleY(1f);
                view.setAlpha(1f);
                view.setTranslationZ(0f);
                return;
            }
            prep(view);
            float z = 6f * c.getResources().getDisplayMetrics().density;
            view.animate().cancel();
            if (down) {
                switch (effectStyle(c)) {
                    case 1: view.animate().scaleX(1.08f).scaleY(0.86f).setDuration(70).start(); break;
                    case 2: view.animate().scaleX(0.94f).scaleY(0.94f).alpha(0.72f).setDuration(70).start(); break;
                    case 3: view.animate().scaleX(1.05f).scaleY(1.05f).translationZ(z).setDuration(80).start(); break;
                    case 4: view.animate().alpha(0.55f).setDuration(50).start(); break;
                    default: view.animate().scaleX(0.88f).scaleY(0.88f).setDuration(60).start(); break;
                }
                return;
            }
            switch (effectStyle(c)) {
                case 1: view.animate().scaleX(1f).scaleY(1f).setDuration(180)
                        .setInterpolator(new DecelerateInterpolator()).start(); break;
                case 2: view.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(160).start(); break;
                case 3: view.animate().scaleX(1f).scaleY(1f).translationZ(0f).setDuration(200).start(); break;
                case 4: view.animate().alpha(1f).scaleX(1.04f).scaleY(1.04f).setDuration(90)
                        .withEndAction(new Runnable() {
                            @Override public void run() {
                                view.animate().scaleX(1f).scaleY(1f).setDuration(130).start();
                            }
                        }).start(); break;
                default: view.animate().scaleX(1f).scaleY(1f).setDuration(260)
                        .setInterpolator(new android.view.animation.OvershootInterpolator(2.6f))
                        .start(); break;
            }
        }
    }

    /** Round 19: the stock selection handles live inside the display view's own
     *  touch handling, so a horizontal-scroll parent that intercepts the drag
     *  kills them -- the exact theft two rounds of owner logs proved. While a
     *  selection is up (handles visible) this scroller refuses to intercept;
     *  with nothing held it scrolls the long expression exactly as before. */
    static final class SelectionSafeHorizontalScrollView extends HorizontalScrollView {
        android.widget.EditText watched;

        SelectionSafeHorizontalScrollView(Context context) { super(context); }

        @Override public boolean onInterceptTouchEvent(MotionEvent ev) {
            if (ev.getAction() == MotionEvent.ACTION_MOVE && watched != null
                    && watched.hasSelection()) {
                return false;
            }
            return super.onInterceptTouchEvent(ev);
        }
    }

    /** Round 20: dark mode paints the view's text with a left-to-right LED
     *  gradient across its own width; light mode clears the shader and takes
     *  pitch-black ink. Re-applied whenever the width changes. */
    private static void themeInk(final android.widget.TextView v, final boolean dark,
            final int[] colors, final int lightInk) {
        v.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override public void onLayoutChange(View vv, int l, int t, int r, int b,
                    int ol, int ot, int or, int ob) {
                int w = vv.getWidth();
                if (w <= 0) return;
                android.widget.TextView tv = (android.widget.TextView) vv;
                if (dark) {
                    tv.getPaint().setShader(new android.graphics.LinearGradient(
                            0, 0, w, 0, colors, null,
                            android.graphics.Shader.TileMode.CLAMP));
                } else {
                    tv.getPaint().setShader(null);
                    tv.setTextColor(lightInk);
                }
                vv.invalidate();
            }
        });
    }

    public static final class DisplayEditText extends android.widget.EditText {
        boolean applying;

        DisplayEditText(Context context) {
            super(context);
            setInputType(android.text.InputType.TYPE_NULL);
            setShowSoftInputOnFocus(false);
            setBackground(null);
        }

        private final android.graphics.Paint caretPaint =
                new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        private int caretWidth = 3;

        DisplayEditText(Context context, int widthPx) {
            super(context);
            caretWidth = widthPx;
            caretPaint.setStyle(android.graphics.Paint.Style.FILL);
            // Round 9, his first line: "when tried to move the cursor, the device keyboard
            // is coming and not able to operate the calculator." This constructor -- the
            // one the screen actually uses -- never carried the suppression the unused
            // one-arg constructor had. Now the view is not a text editor at all: no input
            // connection, no IME attachment, touch selection and caret placement stay.
            setInputType(android.text.InputType.TYPE_NULL);
            setShowSoftInputOnFocus(false);
            setBackground(null);
        }

        @Override public android.view.inputmethod.InputConnection onCreateInputConnection(
                android.view.inputmethod.EditorInfo outAttrs) {
            if (outAttrs != null) outAttrs.inputType = android.text.InputType.TYPE_NULL;
            return null;
        }

        /** Where the screen's own caret stands, in display coordinates: the bar's
         *  fallback when the framework selection reads empty, as it does on his device. */
        int fallbackCaret;

        /** The colour the hand-drawn caret paints with, for the pixel story. */
        int caretColor;

        void setCaretColors(int caretColor, int selectionColor) {
            this.caretColor = caretColor;
            caretPaint.setColor(caretColor);
            // Round 19: only the caret colour lives here now. The selection band and
            // the teardrop handles the previous rounds drew by hand are deleted --
            // the framework paints its own highlight and its own handles, which is
            // the robustness his message asked for.
        }



        /**
         * The caret the owner could never find: drawn by hand, every frame, focused or
         * not, because three rounds of framework cursor machinery stayed invisible on his
         * device. The selection band is drawn too, so selecting text is finally visible.
         */
        @Override protected void onDraw(android.graphics.Canvas c) {
            // Round 19: band and teardrops are gone from here -- the framework
            // paints its own highlight and its own handles. Only the caret stays
            // hand-drawn: three rounds proved his device will not show the
            // theme's cursor, and a visible caret is a standing requirement.
            android.text.Layout lo = getLayout();
            int padL = 0, base = 0, base0 = 0;
            int s = -1;
            if (lo != null) {
                padL = getCompoundPaddingLeft();
                base = getBaseline();
                base0 = lo.getLineBaseline(0);
                s = getSelectionStart();
            }
            super.onDraw(c);
            if (lo == null) return;
            int cs = s >= 0 ? s : Math.max(0, fallbackCaret);
            if (cs <= length()) {
                int ln = lo.getLineForOffset(cs);
                float x = padL + lo.getPrimaryHorizontal(cs);
                int lb = base + lo.getLineBaseline(ln) - base0;
                // Round 20: his screenshot holds no caret at all. With RIGHT
                // gravity the layout's own maths can park the bar out in the
                // reserved right padding -- drawn, but where no eye looks. The
                // bar is clamped inside the content box now, and the instrumented
                // suite draws the view to a bitmap and counts caret pixels, so
                // "visible" means painted, not intended.
                float maxX = getWidth() - getCompoundPaddingRight() - caretWidth;
                if (x > maxX) x = maxX;
                float minX = getCompoundPaddingLeft();
                if (x < minX) x = minX;
                c.drawRect(x, lb + lo.getLineAscent(ln) + 2,
                        x + caretWidth, lb + lo.getLineDescent(ln) - 2, caretPaint);
            }
        }

        @Override public boolean onKeyDown(int code, android.view.KeyEvent e) {
            return false;
        }
    }

    /**
     * A container that claims the vertical swipe itself, in {@code onInterceptTouchEvent},
     * because a swipe listener on a parent whose children consume every touch never fires.
     */
    static final class SwipeBox extends LinearLayout {
        interface Listener { void onSwipeDown(); }
        Listener listener;
        boolean historyShowing;
        Context logContext;
        private float downX, downY;
        private boolean claimed;

        SwipeBox(Context context) { super(context); }

        @Override public boolean onInterceptTouchEvent(MotionEvent e) {
            switch (e.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    downX = e.getX();
                    downY = e.getY();
                    claimed = false;
                    return false;
                case MotionEvent.ACTION_MOVE:
                    float dy = e.getY() - downY;
                    float dx = e.getX() - downX;
                    // The only gesture left here: a downward drag on the expression opens
                    // the history popup. Everything inside the popup scrolls in its own
                    // window, where no parent can steal it.
                    if (!claimed && !historyShowing && dy > 60 && dy > Math.abs(dx) * 2) {
                        claimed = true;
                        AppLog.event(logContext, "calc",
                                "swipe claimed downward: opening the history popup");
                        return true;
                    }
                    return claimed;
                default:
                    return false;
            }
        }

        @Override public boolean onTouchEvent(MotionEvent e) {
            if (claimed) {
                if (e.getAction() == MotionEvent.ACTION_UP
                        || e.getAction() == MotionEvent.ACTION_CANCEL) {
                    claimed = false;
                    if (listener != null) listener.onSwipeDown();
                }
                return true;
            }
            return super.onTouchEvent(e);
        }
    }

    @Override public void onResume() { }

    @Override public void onPause() { }

    /**
     * One key, from anywhere: a hardware keyboard in the standalone build types through
     * here exactly as a thumb does, and the rules refuse what they would refuse a thumb.
     */
    public boolean typeKey(String key) {
        if (lastContext == null || key == null) return false;
        press(lastContext, key);
        return true;
    }

    /** What a copy puts on the clipboard: the result if there is one, else what is typed. */
    private String displayValue() {
        if (justEvaluated) return result;
        String candidate = trimmed();
        if (!candidate.isEmpty()) {
            try {
                return CalculatorRules.format(CalculatorRules.evaluate(candidate, degrees));
            } catch (Exception e) {
                // fall through to the typed text
            }
        }
        return typed();
    }

    private static String clipboardText(Context context) {
        try {
            android.content.ClipboardManager cm = (android.content.ClipboardManager)
                    context.getSystemService(Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = cm.getPrimaryClip();
            if (clip == null || clip.getItemCount() == 0) return null;
            CharSequence cs = clip.getItemAt(0).coerceToText(context);
            return cs == null ? null : cs.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private void copy(Context context, String text) {
        try {
            android.content.ClipboardManager cm = (android.content.ClipboardManager)
                    context.getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(android.content.ClipData.newPlainText("MySpace calculation", text));
            AppLog.event(context, "calc", "copied " + text.length() + " chars: " + text);
            host.toast("Copied " + text);
        } catch (Exception e) {
            AppLog.event(context, "calc", "copy FAILED: " + e);
            host.toast("Could not copy.");
        }
    }

    /** History and memory reuse: typed in one character at a time, so the rules validate
     *  the stored value exactly as if your thumb had typed it. */
    private void insertNumber(String value) {
        String current = justEvaluated ? "" : expression.toString();
        justEvaluated = false;
        result = "";
        for (int i = 0; i < value.length(); i++) {
            String next = CalculatorRules.append(current, String.valueOf(value.charAt(i)));
            if (next == null) continue;
            current = next;
        }
        expression.setLength(0);
        expression.append(current);
        caret = expression.length();
    }

    /** The main display: the result just after equals, otherwise what is typed. */
    private String typed() {
        if (justEvaluated) {
            if (result.isEmpty()) return "0";
            if (showFraction) {
                try {
                    String fraction = CalculatorRules.toFraction(Double.parseDouble(result));
                    if (fraction != null) return fraction;
                } catch (Exception e) {
                    // fall through to the decimal
                }
            }
            return groupIndian(result);
        }
        return expression.length() == 0 ? "0" : readable(expression.toString());
    }

    /** The live result under the display; empty while what is typed cannot evaluate yet. */
    private String preview() {
        if (justEvaluated) return readable(expression.toString());
        String candidate = trimmed();
        if (candidate.isEmpty()) return " ";
        try {
            return "= " + groupIndian(
                    CalculatorRules.format(CalculatorRules.evaluate(candidate, degrees)));
        } catch (Exception e) {
            return " ";
        }
    }

    /** The expression with any trailing operator dropped, which is what equals evaluates. */
    private String trimmed() {
        String typed = expression.toString();
        while (!typed.isEmpty() && CalculatorRules.isOperator(typed.charAt(typed.length() - 1))) {
            typed = typed.substring(0, typed.length() - 1);
        }
        return typed;
    }

    // ---- editing at the cursor -------------------------------------------------------------

    private int[] selection() {
        if (!justEvaluated && displayView != null) {
            int s = displayView.getSelectionStart();
            int e = displayView.getSelectionEnd();
            if (s >= 0 && e >= 0) {
                return new int[]{Math.min(dispToRaw(s), expression.length()),
                        Math.min(dispToRaw(e), expression.length())};
            }
        }
        int c = Math.max(0, Math.min(caret, expression.length()));
        return new int[]{c, c};
    }

    /**
     * The whole edited expression replayed through the rules, token by token: an insertion
     * in the middle makes a join after the caret that appending can never see, so the
     * finished string is re-validated left to right before it is accepted.
     */
    static boolean wholeExpressionValid(String raw) {
        // Round 12: his log -- "key 3 refused at 4 ... the expression would not evaluate",
        // every time the caret stood inside a grouped number. readable() now carries Indian
        // commas, and the tokenizer knows no comma; validate the PLAIN display form.
        String display = raw.replace('*', '\u00d7').replace('/', '\u00f7')
                .replace('-', '\u2212');
        String built = "";
        int i = 0;
        while (i < display.length()) {
            String token = null;
            for (String candidate : TOKENS) {
                if (display.startsWith(candidate, i)) { token = candidate; break; }
            }
            if (token == null) return false;
            built = CalculatorRules.appendToken(built, token);
            if (built == null) return false;
            i += token.length();
        }
        return built.equals(raw);
    }

    /** The cap a backspace deletes: a whole function token when one ends at the cursor. */
    private static int deletableUnit(String raw, int at) {
        for (String token : TOKENS) {
            if (token.length() > 1 && at >= token.length()
                    && raw.substring(at - token.length(), at).equals(token)) {
                return token.length();
            }
        }
        return 1;
    }

    private void press(Context context, String key) {
        if ("AC".equals(key)) {
            closeSelectionMenu();
            String was = expression.length() == 0 && !justEvaluated ? "" : typed();
            expression.setLength(0);
            result = "";
            justEvaluated = false;
            caret = 0;
            AppLog.event(context, "calc", was.isEmpty()
                    ? "key AC: nothing to clear" : "key AC: cleared " + was);
            refreshDisplay(context);
            return;
        }
        String inverse = invToken(key);
        if (inverse != null) {
            // Round 12: sin⁻¹ cos⁻¹ tan⁻¹ were dead on the pad -- their text was inserted
            // one CHAR at a time, and the very first 'a' is no token, so the rules refused
            // it. A function name goes in as ONE token; the power and exponential caps
            // still type their little sequences char by char.
            if (inverse.endsWith("(") && inverse.length() > 2) {
                insertAtCaret(context, inverse);
            } else {
                for (int i = 0; i < inverse.length(); i++) {
                    if (!insertAtCaret(context, String.valueOf(inverse.charAt(i)))) break;
                }
            }
            AppLog.event(context, "calc", "key " + key + " inserted " + inverse
                    + ": now " + readable(expression.toString()));
            return;
        }
        if ("Deg".equals(key) || "Rad".equals(key)) {
            degrees = !degrees;
            AppPrefs.prefs(context).edit().putBoolean(DEGREES_KEY, degrees).apply();
            AppLog.event(context, "calc", "angle mode → " + (degrees ? "DEG" : "RAD")
                    + ", persisted");
            relabelCaps();
            refreshDisplay(context);
            return;
        }
        if ("Inv".equals(key)) {
            invMode = !invMode;
            AppLog.event(context, "calc", invMode
                    ? "inverse on: sin cos tan show their inverses"
                    : "inverse off: sin cos tan show themselves");
            relabelCaps();
            return;
        }
        if ("( )".equals(key)) {
            // One button opens and closes, Google's way: the rules decide which bracket
            // fits, so 5(1+1 then ( ) closes the group instead of stacking another.
            // Round 15: smartBracket reads the END of the expression, but the thumb is
            // at the CARET. Mid-expression the two disagree -- his log: "key ) refused at
            // 3 (after '()'" then "neither bracket fits", because only the end-wise choice
            // was ever tried. Google's one bracket key fits the cursor, so the key now
            // offers the other bracket when the first does not fit where the thumb is.
            String bracket = CalculatorRules.smartBracket(expression.toString());
            if (insertAtCaret(context, bracket)) {
                AppLog.event(context, "calc", "key ( ) inserted " + bracket
                        + " at " + caret + ": now " + readable(expression.toString()));
                return;
            }
            String other = "(".equals(bracket) ? ")" : "(";
            if (insertAtCaret(context, other)) {
                AppLog.event(context, "calc", "key ( ) inserted " + other
                        + " at " + caret + " (the caret's choice, not the end's): now "
                        + readable(expression.toString()));
                return;
            }
            AppLog.event(context, "calc", "key ( ) refused: neither bracket fits at the cursor");
            return;
        }
        int[] sel = selection();
        if ("⌫".equals(key)) {
            if (justEvaluated) {
                expression.setLength(0);
                if (!"Error".equals(result)) expression.append(result);
                justEvaluated = false;
                result = "";
                caret = expression.length();
                AppLog.event(context, "calc", "key ⌫: editing the result, now "
                        + readable(expression.toString()));
                refreshDisplay(context);
                return;
            }
            if (sel[0] != sel[1]) {
                String next = expression.substring(0, sel[0]) + expression.substring(sel[1]);
                expression.setLength(0);
                expression.append(next);
                caret = sel[0];
                AppLog.event(context, "calc", "key ⌫: deleted selection, now "
                        + readable(expression.toString()));
                refreshDisplay(context);
                return;
            }
            if (sel[0] == 0) {
                AppLog.event(context, "calc", "key ⌫: nothing before the cursor");
                return;
            }
            int unit = deletableUnit(expression.toString(), sel[0]);
            String next = expression.substring(0, sel[0] - unit) + expression.substring(sel[0]);
            expression.setLength(0);
            expression.append(next);
            caret = sel[0] - unit;
            AppLog.event(context, "calc", "key ⌫ at " + caret + ": now "
                    + readable(expression.toString()));
            refreshDisplay(context);
            return;
        }
        if ("=".equals(key)) {
            closeSelectionMenu();
            // Round 11: "scientific operations are giving error" -- sin(30 met a parser
            // that demands its closing bracket. Google simply closes what you left open
            // when you press equals, and so do we now.
            String typed = CalculatorRules.autoClose(trimmed());
            if (typed.isEmpty()) {
                AppLog.event(context, "calc", "key =: nothing to evaluate");
                return;
            }
            String evaluated;
            try {
                evaluated = CalculatorRules.format(CalculatorRules.evaluate(typed, degrees));
            } catch (Exception e) {
                result = "Error";
                justEvaluated = true;
                caret = 0;
                AppLog.event(context, "calc", "key = on " + readable(typed)
                        + " → Error: the expression does not evaluate");
                refreshDisplay(context);
                return;
            }
            AppPrefs.prefs(context).edit().putString(HISTORY_KEY,
                    CalculatorRules.pushHistory(
                            AppPrefs.prefs(context).getString(HISTORY_KEY, ""),
                            readable(typed), evaluated, HISTORY_CAP)).apply();
            AppLog.event(context, "calc", "key = on " + readable(typed) + " → " + evaluated
                    + ", stored in history");
            result = evaluated;
            expression.setLength(0);
            expression.append(result);
            justEvaluated = true;
            caret = 0;
            refreshDisplay(context);
            if (historyOpen) populateHistory(context, new UiKit(context, host.palette()));
            return;
        }
        insertAtCaret(context, key);
    }

    /**
     * Insert one cap at the cursor, replacing any selection, validated as a whole
     * expression. Returns whether the insertion happened, so the bracket key can try its
     * second choice and every refusal can be logged as a refusal rather than a silence.
     */
    private boolean insertAtCaret(Context context, String key) {
        int[] sel = selection();
        String raw = expression.toString();
        if (justEvaluated) {
            boolean fresh = "Error".equals(result)
                    || (key.length() == 1 && (Character.isDigit(key.charAt(0)) || key.charAt(0) == '.'));
            if (fresh) {
                raw = "";
            }
            justEvaluated = false;
            result = fresh ? "" : result;
            sel = new int[]{raw.length(), raw.length()};
        }
        int at = sel[0];
        String rawKey = key.replace('×', '*').replace('÷', '/').replace('−', '-');
        String candidate;
        if (sel[1] == at && at == raw.length()) {
            // Typing at the end goes through the rules themselves -- the changeable-mind
            // operator replacement, percent over a pending operator, implicit multiplication.
            // The old splice-and-replay refused all of them at the pad door, which is what
            // his log showed thirty times over: "key + refused at 7 (after '+')".
            candidate = CalculatorRules.appendToken(raw, rawKey);
            if (candidate == null) {
                char before = at == 0 ? ' ' : raw.charAt(at - 1);
                AppLog.event(context, "calc", "key " + key + " refused at " + at
                        + " (after '" + before + "'): the rules declined it");
                return false;
            }
        } else if (sel[1] == at && rawKey.length() == 1
                && CalculatorRules.isOperator(rawKey.charAt(0))
                && at > 0 && CalculatorRules.isOperator(raw.charAt(at - 1))) {
            // Round 18: "user can type 10*-5 at first. But when he moves his cursor and
            // tries to put * and - after that, - replaces *." At the END a minus under a
            // multiply inserts as negation; mid-expression the replace-everything rule
            // ate the multiply instead. Mid now mirrors the end exactly: a minus under
            // *, / or ^ (or a binary minus) INSERTS as negation; over a plus it is the
            // changeable mind; over a negation slot another operator takes the pair.
            char before = raw.charAt(at - 1);
            boolean unarySlot = before == '-'
                    && (at == 1 || CalculatorRules.isOperator(raw.charAt(at - 2))
                            || raw.charAt(at - 2) == '(');
            if (rawKey.charAt(0) == '-' && !unarySlot && before != '+') {
                candidate = raw.substring(0, at) + rawKey + raw.substring(sel[1]);
            } else if (unarySlot && rawKey.charAt(0) != '-') {
                candidate = raw.substring(0, at - 2) + rawKey + raw.substring(at);
            } else {
                candidate = raw.substring(0, at - 1) + rawKey + raw.substring(at);
            }
            if (!wholeExpressionValid(candidate)) {
                AppLog.event(context, "calc", "key " + key + " refused at " + at
                        + " (after '" + before + "'): the expression would not evaluate");
                return false;
            }
        } else if (sel[1] == at && rawKey.length() == 1
                && CalculatorRules.isOperator(rawKey.charAt(0))
                && at < raw.length() && CalculatorRules.isOperator(raw.charAt(at))) {
            // Same changeable mind from the other side: caret parked just LEFT of an
            // operator replaces the one on its right.
            candidate = raw.substring(0, at) + rawKey + raw.substring(at + 1);
            if (!wholeExpressionValid(candidate)) {
                char before = at == 0 ? ' ' : raw.charAt(at - 1);
                AppLog.event(context, "calc", "key " + key + " refused at " + at
                        + " (after '" + before + "'): the expression would not evaluate");
                return false;
            }
        } else {
            // A caret edit inside the expression splices and replays, as before.
            candidate = raw.substring(0, at) + rawKey + raw.substring(sel[1]);
            if (!wholeExpressionValid(candidate)) {
                char before = at == 0 ? ' ' : raw.charAt(at - 1);
                AppLog.event(context, "calc", "key " + key + " refused at " + at
                        + " (after '" + before + "'): the expression would not evaluate");
                return false;
            }
        }
        expression.setLength(0);
        expression.append(candidate);
        // Round 13: "once I insert any operand or operator then it's moving to the end" --
        // the caret used to jump to the end of the candidate every time. It now stands
        // right after what the thumb just inserted, until the thumb moves it again.
        int added = candidate.length() - (raw.length() - (sel[1] - at));
        caret = at + added;
        AppLog.event(context, "calc", "key " + key + " at " + at + " → "
                + readable(expression.toString()) + ", caret stays at " + caret);
        refreshDisplay(context);
        return true;
    }

    /** The display glyphs are typographic; the parser takes ASCII operators. */
    private static String readable(String raw) {
        return groupIndian(
                raw.replace('*', '×').replace('/', '÷').replace('-', '−'));
    }

    /**
     * Round 9: "when numbers are typed, use commas as per Indian numbering." Every run of
     * digits on the display is grouped the Indian way -- three from the right, then twos:
     * 1234567 reads 12,34,567. The expression itself stays raw; commas live only in what
     * the eye sees, and the two offset spaces are mapped below so taps and the caret keep
     * landing where the thumb meant.
     */
    static String groupIndian(String s) {
        if (s == null) return "";
        StringBuilder out = new StringBuilder();
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c < '0' || c > '9') { out.append(c); i++; continue; }
            int j = i;
            while (j < s.length() && s.charAt(j) >= '0' && s.charAt(j) <= '9') j++;
            // Round 13: his screenshot read 1.5,70,79,63,268 -- the fraction was grouped
            // too. Digits after a dot are decimals, not thousands: never group them.
            boolean fraction = out.length() > 0 && out.charAt(out.length() - 1) == '.';
            out.append(fraction ? s.substring(i, j) : groupRun(s.substring(i, j)));
            i = j;
        }
        return out.toString();
    }

    private static String groupRun(String digits) {
        int n = digits.length();
        if (n < 4) return digits;
        StringBuilder b = new StringBuilder();
        int lead = (n - 3) % 2 == 1 ? 1 : 2;
        b.append(digits, 0, lead);
        int k = lead;
        while (k < n - 3) {
            b.append(',');
            b.append(digits, k, k + 2);
            k += 2;
        }
        b.append(',');
        b.append(digits, k, n);
        return b.toString();
    }

    /** Display offset (with commas) to expression offset (without). */
    private int dispToRaw(int off) {
        CharSequence d = displayView == null ? "" : displayView.getText();
        int raw = 0;
        for (int i = 0; i < off && i < d.length(); i++) {
            if (d.charAt(i) != ',') raw++;
        }
        return raw;
    }

    /** Expression offset to display offset, walking past the commas. */
    private int rawToDisp(int off) {
        CharSequence d = displayView == null ? "" : displayView.getText();
        int raw = 0;
        for (int i = 0; i < d.length(); i++) {
            if (raw == off) return i;
            if (d.charAt(i) != ',') raw++;
        }
        return d.length();
    }
}
