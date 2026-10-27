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
    // Round 31: "in 3 dots add options of shape of buttons on keypad. Let
    // there be 5 trending options along with no shape." Six names, one pref.
    private static final String KEYSHAPE_KEY = "calculator_keyshape";
    // Round 33: "Add color theme options in 3 dots. These colors should apply
    // mainly for key buttons... group them and have a few theme" and "a option
    // to follow the MySpace appearance settings, name it as MySpace settings".
    // The default theme IS the app's own palette; the rest recolour the pad by
    // group -- numbers, operations, scientific, commands -- and every ink goes
    // through readableInk(), so no theme can put a symbol on a fill it cannot
    // be seen against in either mode.
    private static final String KEYTHEME_KEY = "calculator_keytheme";
    static final String[] KEY_THEMES = {"MySpace settings", "Graphite",
            "Ocean", "Sunset", "Mint"};
    private static final int G_NUM = 0, G_OP = 1, G_SCI = 2, G_CMD = 3;
    private static final String[] KEY_SHAPES = {"Circle", "Squircle", "Pill",
            "Rounded square", "No shape"};
    private static final int SEL_COPY = 9101, SEL_CUT = 9102,
            SEL_PASTE = 9103, SEL_ALL = 9104, SEL_MEM = 9105, SEL_MR = 9106;

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
    private PopupWindow strokePopup;
    private LinearLayout strokeBox;
    private java.util.List<View> strokeRows = new java.util.ArrayList<>();
    private int strokeHighlight = -1;
    private int strokeTint;
    private boolean strokeMoved;
    private float strokeDownX, strokeDownY;
    private String strokeChild;
    private final android.os.Handler strokeHandler = new android.os.Handler();
    private Runnable strokeDwell;

    /** Test door: the live stroke box, so instrumented stories can aim a real
     *  injected finger at a row instead of guessing where the popup landed. */
    public static LinearLayout strokeBoxDebug;
    private LinearLayout padContainer;
    private boolean padLandscape;
    private ScrollView historyScroll;
    private SwipeBox displayArea;
    private float lastTouchX, lastTouchY;
    private boolean longPressFired;
    private boolean extending;
    private boolean downMoved;
    private boolean selectedAtDown;
    private boolean multiTouch;
    private float downX, downY;
    private int scrollAtDown;
    private int heldA, heldB;
    // Round 25: the hold timer that replaces every OEM long press.
    private final Runnable holdSelect = new Runnable() {
        @Override public void run() {
            if (longPressFired || dragHandle != 0 || multiTouch) return;
            AppLog.event(lastContext, "calc",
                    "hold timer fired: selecting at the held point");
            if (selectWordAt(downX, downY)) longPressFired = true;
        }
    };
    private int extendAnchor;
    private boolean longPressHeld;
    private boolean touchMoved;
    private int dragHandle;
    private int menuSelA, menuSelB;
    // Round 28: the menu raised over a bare caret (long press on the empty part
    // of the box) carries Google's caret trio instead of the selection set.
    private boolean caretMode;
    // Round 30: five rounds of canvas-drawn carets died on his OEM in ways no
    // emulator reproduces, while every geometry number read perfect. The caret
    // is therefore ALSO a real View in the hierarchy -- composited by the
    // framework itself, outside any onDraw -- plus a build stamp and a log line
    // for every placement and blink, so the next shared log ends the saga
    // either way.
    private View caretBar;
    private Runnable caretBlinker;
    private boolean caretOn = true;
    private android.view.ActionMode currentSelectionMode;
    // Round 24 (javac 19): selectWordAt() is a method now, shared by the long
    // press and the plain click, so the menu callback outlives buildDisplay.
    private android.view.ActionMode.Callback selectionMenu;
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
        // Round 33: the dots are now a stroke handle: DOWN opens the menu and
        // grabs the gesture, MOVE highlights, UP chooses -- one stroke, through
        // children and all. A plain tap still opens the click-through menu.
        dots.setOnTouchListener(new View.OnTouchListener() {
            @Override public boolean onTouch(View v, MotionEvent e) {
                return strokeMenu(context, ui, v, e);
            }
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
        padLandscape = landscape;
        rebuildPad(context, ui);
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
        String theme = keyTheme(context);
        if (!"MySpace settings".equals(theme) && kind != EQ) {
            int g = groupOf(key);
            fill = themeFill(theme, g, dark);
            ink = readableInk(fill, themeInk(theme, g, dark));
            if (kind == TOG && toggleOn) {
                fill = mix(fill, ui.palette().accent, 0.42f);
                ink = readableInk(fill, 0xFFFFFFFF);
            }
        }
        PadButton button = new PadButton(context);
        button.setText(key);
        button.setGravity(Gravity.CENTER);
        button.setSingleLine(true);
        button.setTextSize(size);
        button.setTextColor(ink);
        button.setBackground(keyShape(context, ui, fill, gradient));
        button.setContentDescription(key);
        return button;
    }

    /** The persisted colour theme, unknown names falling back to the app's own. */
    private static String keyTheme(Context context) {
        String name = AppPrefs.prefs(context).getString(KEYTHEME_KEY, "MySpace settings");
        for (String known : KEY_THEMES) {
            if (known.equals(name)) return name;
        }
        return "MySpace settings";
    }

    /** Which of the owner's four groups a key belongs to: numbers, operations,
     *  scientific, commands (AC and backspace). Equals keeps the app gradient. */
    private static int groupOf(String key) {
        if ("AC".equals(key) || "⌫".equals(key)) return G_CMD;
        for (String[] row : PANEL_ROWS) {
            for (String k : row) {
                if (k.equals(key)) return G_SCI;
            }
        }
        if (key.length() == 1
                && (Character.isDigit(key.charAt(0)) || key.charAt(0) == '.')) return G_NUM;
        return G_OP;
    }

    /** Group fills per theme per mode. Dark fills stay deep, light fills stay
     *  pale -- the two modes never share a value, which is what keeps every
     *  theme readable on both. */
    private static int themeFill(String theme, int g, boolean dark) {
        if ("Graphite".equals(theme)) {
            if (dark) return new int[]{0xFF2A2E33, 0xFF3A3F45, 0xFF23272B, 0xFF454B52}[g];
            return new int[]{0xFFF1F3F4, 0xFFDFE1E5, 0xFFE8EAED, 0xFFCFD3D8}[g];
        }
        if ("Ocean".equals(theme)) {
            if (dark) return new int[]{0xFF0E2A3F, 0xFF12455E, 0xFF0B2233, 0xFF15506B}[g];
            return new int[]{0xFFDCEEF7, 0xFFB7DEEE, 0xFFE4F2F9, 0xFFA3D3E8}[g];
        }
        if ("Sunset".equals(theme)) {
            if (dark) return new int[]{0xFF3A2020, 0xFF5C2E22, 0xFF2E1A1A, 0xFF6B3A24}[g];
            return new int[]{0xFFFBE9E0, 0xFFF5D3BE, 0xFFFDF0E9, 0xFFEDBF9E}[g];
        }
        if (dark) return new int[]{0xFF12312A, 0xFF1B4A3C, 0xFF0E2721, 0xFF22584A}[g];
        return new int[]{0xFFE2F4EA, 0xFFC2E5D2, 0xFFEBF7F0, 0xFFABDCC4}[g];
    }

    /** Group inks per theme per mode, before the contrast guard adjusts them. */
    private static int themeInk(String theme, int g, boolean dark) {
        if ("Graphite".equals(theme)) {
            if (dark) return new int[]{0xFFF2F4F6, 0xFFDCE6EF, 0xFFDDE2E7, 0xFFF2F4F6}[g];
            return new int[]{0xFF17191C, 0xFF3C4043, 0xFF3C4043, 0xFF17191C}[g];
        }
        if ("Ocean".equals(theme)) {
            if (dark) return new int[]{0xFFD6E9F5, 0xFF8FD8F2, 0xFFBFE0EF, 0xFFEAF7FD}[g];
            return new int[]{0xFF0B2A3A, 0xFF075E79, 0xFF14455C, 0xFF06344A}[g];
        }
        if ("Sunset".equals(theme)) {
            if (dark) return new int[]{0xFFFFE3D6, 0xFFFFC9A3, 0xFFF5D9C8, 0xFFFFE8D2}[g];
            return new int[]{0xFF4A2013, 0xFF8A3B12, 0xFF6B3A24, 0xFF5C2E12}[g];
        }
        if (dark) return new int[]{0xFFD9F2E6, 0xFF9FE8C6, 0xFFC4E8D6, 0xFFE2F7EC}[g];
        return new int[]{0xFF123B2E, 0xFF0E5E43, 0xFF1C4A38, 0xFF0B4630}[g];
    }

    /** Round 33, his visibility order: "actual numbers, symbols or whatever are
     *  should be properly visible." If a theme's ink sits too close to its fill
     *  in luminance, the guard swaps in the nearest extreme instead -- so a bad
     *  combination cannot ship an unreadable key. */
    static int readableInk(int fill, int ink) {
        float lf = android.graphics.Color.luminance(fill);
        float li = android.graphics.Color.luminance(ink);
        if (Math.abs(lf - li) < 0.35f) {
            return lf > 0.5f ? 0xFF14181C : 0xFFF6F8FA;
        }
        return ink;
    }

    /** Round 31: the keypad's silhouette is the owner's to pick, persisted and
     *  rebuilt live. Round 32 trimmed the list on his order ("remove hexagon")
     *  and fixed two of the five: "Pill and circle shape looks similar. Keep
     *  proper circles" -- so Circle is a true disc now, not a clamped radius --
     *  and "if no shape is selected then it should be plain no border... when
     *  user clicks the key, on that time you can show the effect in rectangle"
     *  -- so No shape idles invisible and flashes a rectangle under the thumb. */
    private static android.graphics.drawable.Drawable keyShape(Context context,
            UiKit ui, int fill, int[] gradient) {
        return keyShapeNamed(context, ui, shapeName(context), fill, gradient);
    }

    /** The persisted pick, with old or removed names (Hexagon, anything else)
     *  falling back to Circle instead of drawing a ghost option. */
    private static String shapeName(Context context) {
        String name = AppPrefs.prefs(context).getString(KEYSHAPE_KEY, "Circle");
        for (String known : KEY_SHAPES) {
            if (known.equals(name)) return name;
        }
        return "Circle";
    }

    private static android.graphics.drawable.Drawable keyShapeNamed(Context context,
            UiKit ui, String name, int fill, int[] gradient) {
        if ("No shape".equals(name)) {
            // Plain glass: nothing at rest, a rectangle of accent flash while
            // the thumb is down -- the press itself is the only shape.
            android.graphics.drawable.StateListDrawable sld =
                    new android.graphics.drawable.StateListDrawable();
            sld.addState(new int[]{android.R.attr.state_pressed},
                    new android.graphics.drawable.ColorDrawable(noShapeFlash(ui)));
            sld.addState(new int[]{}, new android.graphics.drawable.ColorDrawable(
                    android.graphics.Color.TRANSPARENT));
            return sld;
        }
        if ("Circle".equals(name)) return new CircleDrawable(fill, gradient);
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        if (gradient != null) {
            g.setOrientation(android.graphics.drawable.GradientDrawable.Orientation.LEFT_RIGHT);
            g.setColors(gradient);
        }
        float r;
        if ("Rounded square".equals(name)) r = ui.dp(10);
        else if ("Squircle".equals(name)) r = ui.dp(26);
        else r = ui.dp(60); // Pill: a stadium the full width of the key
        g.setCornerRadius(r);
        return g;
    }

    /** The rectangle flash a plain key shows while pressed. Shared by the keys
     *  and by the picker's swatch so the option previews its own behaviour. */
    static int noShapeFlash(UiKit ui) {
        return mix(ui.palette().surface, ui.palette().accent, 0.30f);
    }

    /** A proper circle: diameter is the key's shorter side, centred -- so it can
     *  never read as the pill's capsule however wide the cell is. The equals key
     *  keeps its accent-to-pink gradient across the disc. */
    static final class CircleDrawable extends android.graphics.drawable.Drawable {
        private final android.graphics.Paint p =
                new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        private final int[] gradient;
        private int fill;

        CircleDrawable(int fill, int[] gradient) {
            this.fill = fill;
            this.gradient = gradient;
            p.setStyle(android.graphics.Paint.Style.FILL);
            p.setColor(fill);
        }

        @Override public void draw(android.graphics.Canvas c) {
            android.graphics.Rect b = getBounds();
            if (b.isEmpty()) return;
            if (gradient != null) {
                p.setShader(new android.graphics.LinearGradient(
                        b.left, b.centerY(), b.right, b.centerY(), gradient, null,
                        android.graphics.Shader.TileMode.CLAMP));
            }
            float r = Math.min(b.width(), b.height()) / 2f;
            c.drawCircle(b.centerX(), b.centerY(), r, p);
        }

        @Override public void setAlpha(int a) { p.setAlpha(a); }
        @Override public void setColorFilter(android.graphics.ColorFilter f) { p.setColorFilter(f); }
        @Override public int getOpacity() { return android.graphics.PixelFormat.TRANSLUCENT; }
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
        // Round 23, the research-backed rebuild: a single-line EditText already
        // scrolls its own text and owns its selection -- every calculator that
        // gets this right uses the plain widget. My HorizontalScrollView wrapper
        // measured it UNSPECIFIED, so on his OEM the text layout was built 2^20
        // px wide and never rebuilt: the caret lived a million pixels off the
        // glass while emulators, which remeasure, said visible. Plain widget,
        // MATCH_PARENT, native inner scrolling.
        displayView = new DisplayEditText(context,
                Math.max(3, (int) (4 * context.getResources().getDisplayMetrics().density)));
        displayView.setId(R.id.calc_display);
        // Round 18: "first keep the expression typed in the top part of expression
        // box." It hung off the BOTTOM of a tall box, floating mid-air in his
        // screenshot. Top it is, with the live answer beneath it.
        displayView.setGravity(Gravity.RIGHT | Gravity.TOP);
        // The teardrop bulbs hang below the text line; without reserved room they
        // clipped at the view's edge -- "the thing which user has to hold and change
        // the selected portion is not completely visible".
        // Round 28, his log: `caret at x=1132 viewW=1132` -- with RIGHT gravity
        // and ZERO horizontal padding the content edge IS the view edge, so the
        // end-of-line caret and the right boundary teardrop were drawn one bar
        // width OFF the canvas: invisible cursor, one-sided selection. Ten dp
        // of side padding puts the content edge back on the glass.
        // Round 31: "keep more empty space in the right of the expression,
        // atleast another character's space... now it's just selecting last
        // operand." Ten dp was a sliver no thumb could hold; forty dp is a
        // whole character of empty glass where a hold raises the caret menu
        // (MUse, Paste, Select all) instead of grabbing the last operand.
        displayView.setPadding(ui.dp(10), 0, ui.dp(40), ui.dp(34));
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
        // The caret must be VISIBLE, his point: the theme's default was not showing on his
        // device, so it is drawn explicitly in the accent colour, two density pixels wide.
        displayView.setCursorVisible(true);
        final android.graphics.drawable.GradientDrawable cursorMark =
                new android.graphics.drawable.GradientDrawable();
        cursorMark.setColor(ui.palette().dark ? 0xFFFFFFFF : PITCH_BLACK);
        cursorMark.setSize(ui.dp(3), ui.dp(24));
        // Round 10: his log shows 181 "cursor placed" lines and still no caret seen, so
        // the caret stops being the text's own colour and becomes the accent, four density
        // pixels wide -- the one colour on the screen nothing else shares.
        displayView.setCaretColors(ui.palette().dark ? 0xFFFFFFFF : PITCH_BLACK,
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
        // Round 21: his log settled it -- fifty-five gestures on 3.12.22 and the
        // stock long-press selection opened ZERO times on his device, while the
        // same view passes every selection story on the emulator. The OEM edits
        // the gesture, so the gesture layer is ours again, but the SELECTION is
        // the framework's: real Selection spans, the framework highlight, the
        // framework floating action mode. Three finger paths, all logged:
        // hold-then-drag extends the held portion (Google's own behaviour),
        // a thumb down near either end drags that boundary with live layout
        // geometry, and everything else falls through to the editor.
        // Round 25, "take reference of Android and polish it": the hit-or-miss
        // was a RACE -- our listener selected, then the framework Editor's own
        // UP handling collapsed the selection ~80 ms later (his log: "selection
        // 0..5 held" then "selection 5..5 held"), and getOffsetForPosition
        // ignored the inner scroll so handle drags landed where they liked.
        // One state machine owns the gesture now: our own 350 ms hold timer
        // (no reliance on any OEM long press), every UP we acted on is
        // CONSUMED so the Editor cannot undo it, and every offset is mapped
        // through the layout with the inner scroll applied.
        displayView.setOnTouchListener(new View.OnTouchListener() {
            @Override public boolean onTouch(View v, MotionEvent e) {
                int act = e.getActionMasked();
                if (act == MotionEvent.ACTION_DOWN) {
                    if (e.getPointerCount() > 1) {
                        multiTouch = true;
                        displayView.removeCallbacks(holdSelect);
                        return false;
                    }
                    multiTouch = false;
                    lastTouchX = e.getX();
                    lastTouchY = e.getY();
                    downX = e.getX();
                    downY = e.getY();
                    longPressFired = false;
                    extending = false;
                    downMoved = false;
                    selectedAtDown = displayView.hasSelection();
                    dragHandle = 0;
                    scrollAtDown = displayView.getScrollX();
                    try {
                    if (displayView.hasSelection()) {
                        android.text.Layout lo = displayView.getLayout();
                        if (lo != null) {
                            float d = v.getResources().getDisplayMetrics().density;
                            float padL = displayView.getCompoundPaddingLeft();
                            int base = displayView.getBaseline();
                            float top = base + lo.getLineAscent(0) - 40 * d;
                            float bot = base + lo.getLineDescent(0) + 80 * d;
                            if (e.getY() >= top && e.getY() <= bot) {
                                int hln = lo.getLineForOffset(
                                        displayView.getSelectionStart());
                                float lx = padL + displayView.caretX(
                                        lo, displayView.getSelectionStart(), hln);
                                float rx = padL + displayView.caretX(
                                        lo, displayView.getSelectionEnd(), hln);
                                if (Math.abs(e.getX() - lx) <= 40 * d) dragHandle = -1;
                                else if (Math.abs(e.getX() - rx) <= 40 * d) dragHandle = 1;
                                else if (e.getX() > lx && e.getX() < rx) {
                                    dragHandle = e.getX() - lx <= rx - e.getX() ? -1 : 1;
                                }
                                if (dragHandle != 0 && displayView.getParent() != null) {
                                    displayView.getParent()
                                            .requestDisallowInterceptTouchEvent(true);
                                    AppLog.event(lastContext, "calc",
                                            "handle drag began on the "
                                            + (dragHandle < 0 ? "left" : "right")
                                            + " boundary (live geometry)");
                                }
                            }
                        }
                    }
                    } catch (Exception ex) {
                        dragHandle = 0;
                    }
                    if (dragHandle == 0) {
                        displayView.removeCallbacks(holdSelect);
                        displayView.postDelayed(holdSelect, 350);
                    }
                    return dragHandle != 0;
                }
                if (act == MotionEvent.ACTION_POINTER_DOWN) {
                    multiTouch = true;
                    displayView.removeCallbacks(holdSelect);
                    return false;
                }
                if (act == MotionEvent.ACTION_MOVE) {
                    if (multiTouch) return false;
                    if (Math.abs(e.getX() - lastTouchX) + Math.abs(e.getY() - lastTouchY)
                            > android.view.ViewConfiguration.get(v.getContext())
                                    .getScaledTouchSlop() * 2) {
                        downMoved = true;
                        displayView.removeCallbacks(holdSelect);
                    }
                    if (dragHandle != 0) {
                        CharSequence held = displayView.getText();
                        if (held instanceof Spannable) {
                            int off = offsetAt(e.getX(), e.getY());
                            off = Math.max(0, Math.min(off, held.length()));
                            int a = displayView.getSelectionStart();
                            int b = displayView.getSelectionEnd();
                            if (dragHandle < 0) off = Math.min(off, b - 1);
                            else off = Math.max(off, a + 1);
                            Selection.setSelection((Spannable) held,
                                    dragHandle < 0 ? off : a,
                                    dragHandle < 0 ? b : off);
                            displayView.invalidate();
                        }
                        return true;
                    }
                    if (longPressFired && !extending && downMoved) {
                        extending = true;
                        AppLog.event(lastContext, "calc",
                                "hold-then-drag began: extending the held portion");
                    }
                    if (extending) {
                        CharSequence held = displayView.getText();
                        if (held instanceof Spannable) {
                            int off = offsetAt(e.getX(), e.getY());
                            off = Math.max(0, Math.min(off, held.length()));
                            Selection.setSelection((Spannable) held, extendAnchor, off);
                            displayView.invalidate();
                        }
                        return true;
                    }
                    return false;
                }
                if (act == MotionEvent.ACTION_UP
                        || act == MotionEvent.ACTION_CANCEL) {
                    displayView.removeCallbacks(holdSelect);
                    if (multiTouch) {
                        multiTouch = false;
                        dragHandle = 0;
                        extending = false;
                        longPressFired = false;
                        return false;
                    }
                    if (dragHandle != 0 || extending) {
                        // Round 31, his words: "when a something is selected and
                        // then if user clicks again somewhere in the expression,
                        // it may be a selected portion or non selected portion.
                        // Cursor should go to clicked position and release
                        // selection." A finger that came down on the held band
                        // or a handle and NEVER MOVED is a tap, not a drag: it
                        // parks the caret where it landed and lets go.
                        if (!downMoved && dragHandle != 0) {
                            dragHandle = 0;
                            extending = false;
                            if (displayView.getParent() != null) {
                                displayView.getParent()
                                        .requestDisallowInterceptTouchEvent(false);
                            }
                            clearSelectionAt(e.getX(), e.getY());
                            return true;
                        }
                        AppLog.event(lastContext, "calc", "selection drag ended: chars "
                                + displayView.getSelectionStart() + ".."
                                + displayView.getSelectionEnd() + " held");
                        dragHandle = 0;
                        extending = false;
                        if (displayView.getParent() != null) {
                            displayView.getParent()
                                    .requestDisallowInterceptTouchEvent(false);
                        }
                        return true;
                    }
                    if (longPressFired) {
                        // Re-assert what the hold selected and CONSUME the up so
                        // the Editor's tap caret can never collapse it again.
                        CharSequence held = displayView.getText();
                        if (held instanceof Spannable && heldB > heldA) {
                            Selection.setSelection((Spannable) held, heldA, heldB);
                        }
                        longPressFired = false;
                        AppLog.event(lastContext, "calc", "hold released: chars "
                                + displayView.getSelectionStart() + ".."
                                + displayView.getSelectionEnd() + " stay held");
                        return true;
                    }
                    if (act == MotionEvent.ACTION_CANCEL) return false;
                    boolean panned = Math.abs(displayView.getScrollX() - scrollAtDown) > 4;
                    if (!panned) {
                        // Round 27, his words: "if anywhere in a expression box
                        // other than the rectangle portion of selected region
                        // should release it" and "need a cursor for reference
                        // where user is typing and to move in between". One
                        // gesture answers both: a plain tap releases whatever
                        // is held AND parks the caret under the finger, so the
                        // next key lands where the thumb pointed. Selection
                        // stays the hold's job, exactly like Android's editors.
                        clearSelectionAt(e.getX(), e.getY());
                        return true;
                    }
                    AppLog.event(lastContext, "calc", "display gesture ended: "
                            + "selection " + displayView.getSelectionStart()
                            + ".." + displayView.getSelectionEnd() + " held");
                    return false;
                }
                return false;
            }
        });
        // Round 25: a click anywhere else in the expression box -- the live
        // answer row, the margins -- clears the held portion and the menu.
        View.OnClickListener awayClear = new View.OnClickListener() {
            @Override public void onClick(View v) {
                clearSelectionAway();
            }
        };
        currentBox.setOnClickListener(awayClear);
        displayArea.setOnClickListener(awayClear);
        selectionMenu =
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
                    caretMode = false;
                } else if (ds >= 0) {
                    menuSelA = ds;
                    menuSelB = ds;
                }
                menu.clear();
                if (caretMode) {
                    // Round 28, his Google screenshot: over a bare caret the
                    // menu is MR, Paste, Select all -- the memory recall that
                    // inserts the saved value WHERE THE CARET STANDS.
                    menu.add(0, SEL_MR, 0, "MUse");
                    menu.add(0, SEL_PASTE, 1, "Paste");
                    menu.add(0, SEL_ALL, 2, "Select all");
                    AppLog.event(lastContext, "calc",
                            "caret menu opened: MUse, Paste, Select all");
                } else {
                    // Round 28, his words: "when selected, along with copy, cut
                    // options, add Mem option as well" -- Mem stores the held
                    // value in the memory list; MR brings it back at the caret
                    // or over the held range.
                    menu.add(0, SEL_CUT, 0, "Cut");
                    menu.add(0, SEL_COPY, 1, "Copy");
                    menu.add(0, SEL_MEM, 2, "MAdd");
                    menu.add(0, SEL_MR, 3, "MUse");
                    menu.add(0, SEL_PASTE, 4, "Paste");
                    menu.add(0, SEL_ALL, 5, "Select all");
                    AppLog.event(lastContext, "calc", "selection started: chars "
                            + menuSelA + ".." + menuSelB
                            + " held by the stock editor, handles and menu raised");
                    AppLog.event(lastContext, "calc", "selection menu opened: Cut, "
                            + "Copy, MAdd, MUse, Paste, Select all");
                }
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
                        spliceRaw(context, a, b, clean, "paste");
                        mode.finish();
                        return true;
                    }
                    case SEL_MEM: {
                        // Round 28: "along with copy, cut options, add Mem
                        // option as well" -- the held value joins the memory
                        // list the owner arranges under the three dots.
                        CharSequence heldTxt = displayView.getText();
                        String shown = heldTxt.subSequence(
                                Math.max(0, Math.min(menuSelA, heldTxt.length())),
                                Math.max(0, Math.min(menuSelB, heldTxt.length())))
                                .toString();
                        String num = shown.replace(",", "");
                        try {
                            Double.parseDouble(num);
                        } catch (Exception ex) {
                            AppLog.event(context, "calc",
                                    "MAdd refused: selection holds " + shown);
                            host.toast("Select a number to store");
                            return true;
                        }
                        java.util.List<String> list = memoryList(context);
                        list.add(shown);
                        saveMemory(context, list);
                        AppLog.event(context, "calc", "memory saved from selection: "
                                + shown + ", memory now holds " + list.size());
                        host.toast("Added to memory");
                        mode.finish();
                        return true;
                    }
                    case SEL_MR: {
                        // Round 28, his Google screenshot: MR inserts the
                        // recently saved memory WHERE THE CARET STANDS, or over
                        // the held range -- mid-expression, no re-typing.
                        java.util.List<String> mem = memoryList(context);
                        if (mem.isEmpty()) {
                            AppLog.event(context, "calc", "MUse refused: memory empty");
                            host.toast("Memory is empty");
                            mode.finish();
                            return true;
                        }
                        String v = mem.get(mem.size() - 1);
                        String clean = v.replace(",", "");
                        spliceRaw(context, a, b, clean, "MUse");
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
                caretMode = false;
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
        LinearLayout.LayoutParams dp2 = ui.lp(-1, 0);
        dp2.weight = 1f;
        // Round 30, "cursor not visible" after five canvas attempts: the caret
        // becomes A REAL VIEW -- a thin bar composited by the framework itself
        // in a FrameLayout host over the display, positioned from the same
        // paint-measured anchor, blinking by visibility. Whatever his OEM does
        // to onDraw canvases, it cannot swallow a view in the tree; and the
        // canvas bar stays beneath it for the emulators and the pixel stories.
        final android.widget.FrameLayout displayHost =
                new android.widget.FrameLayout(context);
        displayHost.addView(displayView,
                new android.widget.FrameLayout.LayoutParams(-1, -1));
        caretBar = new View(context);
        caretBar.setTag("calc_caret_bar");
        caretBar.setBackgroundColor(
                ui.palette().dark ? 0xFFFFFFFF : PITCH_BLACK);
        caretBar.setVisibility(View.GONE);
        displayHost.addView(caretBar, new android.widget.FrameLayout.LayoutParams(
                Math.max(3, (int) (4 * context.getResources()
                        .getDisplayMetrics().density)), 8));
        parent.addView(displayHost, dp2);
        AppLog.event(context, "calc", "calc screen build 3.12.36: the caret is "
                + "a real view over the canvas bar");
        displayView.caretPhaseReset = new Runnable() {
            @Override public void run() {
                caretOn = true;
                displayView.caretOn = true;
                displayView.removeCallbacks(caretBlinker);
                displayView.postDelayed(caretBlinker, 700);
                placeCaretBar();
            }
        };
        caretBlinker = new Runnable() {
            @Override public void run() {
                caretOn = !caretOn;
                displayView.caretOn = caretOn;
                displayView.invalidate();
                placeCaretBar();
                AppLog.event(lastContext, "calc", caretOn
                        ? "caret blink: bar shown" : "caret blink: bar hidden");
                displayView.postDelayed(this, 500);
            }
        };
        displayView.postDelayed(caretBlinker, 500);
        previewView = ui.label(" ", 20, PREVIEW_PINK, false);
        // Round 19: the live answer glows pink-to-amber under the gradient
        // expression -- the two brightest things on the screen.
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
        applyLedInk(context, shown);
        // Round 21: the caret mystery, closed by construction. The display lives
        // in a horizontal scroller whose child grows with the text; with the
        // scroller parked at zero a caret at the end of a long expression is
        // drawn OFF the visible window -- invisible on his glass, perfectly
        // present in code. Every refresh now scrolls the caret into view and
        // writes its geometry to the log, so the next shared log ends the
        // guessing either way.
        displayView.post(new Runnable() {
            @Override public void run() {
              try {
                android.text.Layout lo = displayView.getLayout();
                if (lo == null) return;
                int cs = displayView.getSelectionStart();
                cs = Math.max(0, Math.min(cs, displayView.length()));
                // Round 28: one line, always -- the display is maxLines(1), and
                // getLineForOffset was another torn-layout reader in the frame.
                int ln = 0;
                float x = displayView.getCompoundPaddingLeft()
                        + displayView.caretX(lo, cs, ln);
                int childW = displayView.getWidth();
                int scrollX = displayView.getScrollX();
                AppLog.event(context, "calc", "caret at x=" + (int) x
                        + " viewW=" + childW + " innerScrollX=" + scrollX
                        + " layoutW=" + lo.getWidth()
                        + " ink=" + String.format("#%08X", displayView.caretColor)
                        + " caretW=" + displayView.caretWidth
                        + " bar=" + (caretBar != null
                                && caretBar.getVisibility() == View.VISIBLE
                                ? "on at " + (int) caretBar.getX()
                                : "off"));
                // The EditText scrolls its own text now; keep the caret inside
                // the visible part of that inner scroll.
                float d = context.getResources().getDisplayMetrics().density;
                // Round 24: compare in LAYOUT space and clamp to the real
                // scroll range. Round 23 compared the on-screen caret x with
                // the raw layout-space scrollX and yanked the text a million
                // pixels away on every refresh -- his "blinking and getting
                // erased" while typing; only selecting scrolled it back.
                int padL2 = displayView.getCompoundPaddingLeft();
                int padR2 = displayView.getCompoundPaddingRight();
                float layoutX = x + scrollX;
                int maxScroll = Math.max(0,
                        lo.getWidth() - Math.max(1, childW - padL2 - padR2));
                int want = scrollX;
                if (x + 40 * d > childW - padR2) {
                    want = (int) (layoutX + 40 * d - (childW - padR2));
                } else if (x < padL2) {
                    want = (int) (layoutX - padL2 - 8 * d);
                }
                want = Math.max(0, Math.min(want, maxScroll));
                if (Math.abs(want - scrollX) > 4) {
                    displayView.scrollTo(want, 0);
                    AppLog.event(context, "calc",
                            "caret scrolled into view at innerScrollX=" + want);
                }
                placeCaretBar();
              } catch (Exception e) {
                AppLog.event(context, "calc", "caret geometry guarded: "
                        + e.getClass().getSimpleName());
              }
            }
        });
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
                    // Round 22: his log and screenshot -- a history line came back
                    // as "35,353-4,353" and every equals after it said Error. The
                    // stored expression is the DISPLAY form; commas are display
                    // furniture and must not re-enter the raw expression.
                    expression.append(expr.replace(",", "")
                            .replace('×', '*').replace('÷', '/').replace('−', '-'));
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
    /** Round 31: the keypad silhouette picker -- five trending shapes and no
     *  shape at all, single-choice, persisted, and the pad rebuilds under the
     *  thumb without recreating the screen. */
    private void openKeyShape(final Context context, final UiKit ui) {
        LinearLayout box = pickerBox(context, ui, "Key shape");
        box.addView(shapeRows(context, ui), ui.lp(-1, -2));
        showCenterPopup(context, ui, box);
        AppLog.event(context, "calc",
                "key shape picker opened: current " + shapeName(context));
    }

    /** The five silhouette rows, shared by the tap-through picker and the
     *  single-stroke child menu; each row previews its own drawable. */
    private LinearLayout shapeRows(final Context context, final UiKit ui) {
        final String current = shapeName(context);
        LinearLayout vbox = ui.vbox();
        for (int i = 0; i < KEY_SHAPES.length; i++) {
            final String name = KEY_SHAPES[i];
            final boolean on = name.equals(current);
            LinearLayout row = ui.hbox();
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(ui.dp(14), ui.dp(9), ui.dp(14), ui.dp(9));
            row.setTag(name);
            View swatch = new View(context);
            swatch.setBackground("No shape".equals(name)
                    ? new android.graphics.drawable.ColorDrawable(noShapeFlash(ui))
                    : keyShapeNamed(context, ui, name,
                            on ? ui.palette().accent : ui.palette().muted, null));
            // Wider than tall on purpose: on a square swatch the pill's clamped
            // radius would draw a circle and repeat the very confusion he
            // reported ("Pill and circle shape looks similar").
            row.addView(swatch, ui.lp(30, 20));
            TextView label = ui.label((on ? "\u2713  " : "     ") + name, 14,
                    on ? ui.palette().accent : ui.palette().text, on);
            LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(-2, -2);
            llp.leftMargin = ui.dp(12);
            row.addView(label, llp);
            vbox.addView(row, ui.lp(-1, -2));
            row.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    if (menuPopup != null) menuPopup.dismiss();
                    menuPopup = null;
                    applyShape(context, ui, name);
                }
            });
        }
        return vbox;
    }

    private void applyShape(Context context, UiKit ui, String name) {
        AppPrefs.prefs(context).edit().putString(KEYSHAPE_KEY, name).apply();
        AppLog.event(context, "calc", "key shape set to " + name + ", pad rebuilt");
        host.toast("Keys: " + name);
        rebuildPad(context, ui);
    }

    /** Round 33: the colour themes, same signature picker; the first option is
     *  the app's own appearance, named as he asked: "MySpace settings". */
    private void openKeyTheme(final Context context, final UiKit ui) {
        LinearLayout box = pickerBox(context, ui, "Key theme");
        box.addView(themeRows(context, ui), ui.lp(-1, -2));
        showCenterPopup(context, ui, box);
        AppLog.event(context, "calc",
                "key theme picker opened: current " + keyTheme(context));
    }

    private LinearLayout themeRows(final Context context, final UiKit ui) {
        final String current = keyTheme(context);
        final boolean dark = ui.palette().dark;
        LinearLayout vbox = ui.vbox();
        for (int i = 0; i < KEY_THEMES.length; i++) {
            final String name = KEY_THEMES[i];
            final boolean on = name.equals(current);
            LinearLayout row = ui.hbox();
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(ui.dp(14), ui.dp(9), ui.dp(14), ui.dp(9));
            row.setTag(name);
            LinearLayout swatch = ui.hbox();
            for (int g = 0; g < 3; g++) {
                View chip = new View(context);
                int fill = "MySpace settings".equals(name)
                        ? (g == 0 ? ui.palette().surface
                                : g == 1 ? mix(ui.palette().surface, ui.palette().accent, 0.42f)
                                : mix(ui.palette().surface, dark ? 0xFFFFC94D : 0xFFB26A00, 0.16f))
                        : themeFill(name, g, dark);
                chip.setBackground(pill(ui, fill, 5));
                swatch.addView(chip, ui.lp(12, 20));
            }
            row.addView(swatch, ui.lp(-2, 20));
            TextView label = ui.label((on ? "\u2713  " : "     ") + name, 14,
                    on ? ui.palette().accent : ui.palette().text, on);
            LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(-2, -2);
            llp.leftMargin = ui.dp(12);
            row.addView(label, llp);
            vbox.addView(row, ui.lp(-1, -2));
            row.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    if (menuPopup != null) menuPopup.dismiss();
                    menuPopup = null;
                    applyTheme(context, ui, name);
                }
            });
        }
        return vbox;
    }

    private void applyTheme(Context context, UiKit ui, String name) {
        AppPrefs.prefs(context).edit().putString(KEYTHEME_KEY, name).apply();
        AppLog.event(context, "calc", "key theme set to " + name + ", pad rebuilt");
        host.toast("Key colours: " + name);
        rebuildPad(context, ui);
    }

    private LinearLayout pickerBox(Context context, UiKit ui, String title) {
        LinearLayout box = ui.vbox();
        box.setBackground(pill(ui, ui.palette().surface, 16));
        box.setPadding(ui.dp(8), ui.dp(6), ui.dp(8), ui.dp(8));
        TextView head = ui.label(title, 13, ui.palette().muted, false);
        head.setPadding(ui.dp(14), ui.dp(8), ui.dp(14), ui.dp(4));
        box.addView(head, ui.lp(-1, -2));
        return box;
    }

    private void showCenterPopup(Context context, UiKit ui, LinearLayout box) {
        menuPopup = new PopupWindow(box, ui.dp(250), -2, true);
        menuPopup.setElevation(ui.dp(8));
        menuPopup.setAnimationStyle(R.style.CalcPopupAnim);
        menuPopup.showAtLocation(rootView, Gravity.CENTER, 0, 0);
    }

    /** Round 31: the pad, rebuildable in place -- the key-shape picker swaps
     *  every key background without recreating the screen or losing the panel
     *  state. capButtons is rebuilt with it, because press() looks keys up there. */
    private void rebuildPad(Context context, UiKit ui) {
        padContainer.removeAllViews();
        capButtons = new java.util.HashMap<>();
        panelBox = ui.vbox();
        panelBox.setVisibility(panelOpen && !padLandscape ? View.VISIBLE : View.GONE);
        padContainer.addView(panelBox, new LinearLayout.LayoutParams(-1, 0,
                panelOpen && !padLandscape ? 3f : 0f));
        if (!padLandscape) {
            for (String[] row : PANEL_ROWS) addPadRow(context, ui, panelBox, row, 12);
        }
        LinearLayout baseBox = ui.vbox();
        padContainer.addView(baseBox, new LinearLayout.LayoutParams(-1, 0, 5f));
        for (String[] row : (padLandscape ? LANDSCAPE_ROWS : BASE_ROWS)) {
            addPadRow(context, ui, baseBox, row, padLandscape ? 12 : 18);
        }
        GelMotion.apply(padContainer, ui.palette().accent);
    }

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
    /** Round 33, his words: "presently, I have to click on 3 dots then click
     *  into one of them, then again... Implement Drag-to-Select single-stroke
     *  menu where pressing the button opens it, dragging highlights items, and
     *  lifting the finger triggers the action. It must support continuous
     *  tracking into child submenus without releasing the finger... Present
     *  option of access also should be there." The dots view accepts the DOWN,
     *  so Android keeps routing the whole gesture to it while the finger roams
     *  over popups that declare themselves untouchable; dwelling on a parent
     *  row swaps the popup's rows for that child's, still mid-stroke. A tap
     *  (no movement) opens the old click-through menu exactly as before. */
    private boolean strokeMenu(final Context context, final UiKit ui,
                               final View anchor, MotionEvent e) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                dismissStroke();
                strokeMoved = false;
                strokeDownX = e.getRawX();
                strokeDownY = e.getRawY();
                strokeChild = null;
                strokeHighlight = -1;
                strokeTint = mix(ui.palette().surface, ui.palette().accent, 0.22f);
                strokeBox = ui.vbox();
                strokeBox.setBackground(pill(ui, ui.palette().surface, 16));
                strokeBox.setPadding(ui.dp(6), ui.dp(6), ui.dp(6), ui.dp(6));
                fillStrokeRows(context, ui, null);
                strokePopup = new PopupWindow(strokeBox, ui.dp(220), -2, false);
                strokePopup.setTouchable(false);
                strokePopup.setFocusable(false);
                strokePopup.setElevation(ui.dp(8));
                strokePopup.setAnimationStyle(R.style.CalcPopupAnim);
                strokePopup.showAsDropDown(anchor, 0, ui.dp(4), Gravity.RIGHT);
                AppLog.event(context, "calc",
                        "menu stroke opened: drag to highlight, lift to choose");
                return true;
            case MotionEvent.ACTION_MOVE:
                if (strokePopup == null) return true;
                if (!strokeMoved && Math.hypot(e.getRawX() - strokeDownX,
                        e.getRawY() - strokeDownY) > ui.dp(8)) {
                    strokeMoved = true;
                }
                if (!strokeMoved) return true;
                final int idx = strokeRowAt(e.getRawX(), e.getRawY());
                if (idx == strokeHighlight) return true;
                setStrokeHighlight(idx);
                if (strokeDwell != null) {
                    strokeHandler.removeCallbacks(strokeDwell);
                    strokeDwell = null;
                }
                if (idx >= 0 && isStrokeParent(strokeRows.get(idx))) {
                    strokeDwell = new Runnable() {
                        @Override public void run() {
                            String name = (String) strokeRows.get(idx).getTag();
                            strokeChild = name;
                            fillStrokeRows(context, ui, name);
                            strokeHighlight = -1;
                            AppLog.event(context, "calc", "stroke child opened: " + name);
                        }
                    };
                    strokeHandler.postDelayed(strokeDwell, 320);
                }
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (strokeDwell != null) {
                    strokeHandler.removeCallbacks(strokeDwell);
                    strokeDwell = null;
                }
                if (strokePopup == null) return true;
                if (!strokeMoved) {
                    dismissStroke();
                    AppLog.event(context, "calc", "menu tapped open: click access kept");
                    openMenu(context, ui, anchor);
                    return true;
                }
                String child = strokeChild;
                String name = strokeHighlight >= 0
                        ? (String) strokeRows.get(strokeHighlight).getTag() : null;
                dismissStroke();
                if (name == null) {
                    AppLog.event(context, "calc",
                            "stroke ended on empty space: nothing chosen");
                    return true;
                }
                AppLog.event(context, "calc", "stroke selected " + name
                        + (child == null ? "" : " from " + child));
                runStrokeChoice(context, ui, child, name);
                return true;
            default:
                return strokePopup != null;
        }
    }

    private boolean isStrokeParent(View row) {
        String t = (String) row.getTag();
        return "Memory".equals(t) || "Key theme".equals(t)
                || "Key shape".equals(t) || "Effects".equals(t);
    }

    private void runStrokeChoice(Context context, UiKit ui, String child, String name) {
        if (child == null) {
            if ("Memory".equals(name)) { openMemory(context, ui); return; }
            if ("Key theme".equals(name)) { openKeyTheme(context, ui); return; }
            if ("Key shape".equals(name)) { openKeyShape(context, ui); return; }
            if ("Effects".equals(name)) { openEffects(context, ui); return; }
            onMenuChoice(context, ui, name);
            return;
        }
        if ("Key shape".equals(child)) { applyShape(context, ui, name); return; }
        if ("Key theme".equals(child)) { applyTheme(context, ui, name); return; }
        if ("Effects".equals(child)) {
            for (int i = 0; i < EFFECT_NAMES.length; i++) {
                if (EFFECT_NAMES[i].equals(name)) applyEffectIdx(context, i);
            }
            return;
        }
        if ("Clear memory".equals(name)) { clearMemoryNow(context, ui); return; }
        useMemoryValue(context, name);
    }

    private void fillStrokeRows(Context context, UiKit ui, String child) {
        strokeBox.removeAllViews();
        strokeRows = new java.util.ArrayList<>();
        TextView head = ui.label(child == null ? "Menu" : child, 13,
                ui.palette().muted, false);
        head.setPadding(ui.dp(8), ui.dp(4), ui.dp(8), ui.dp(4));
        strokeBox.addView(head, ui.lp(-1, -2));
        LinearLayout rows;
        if (child == null) rows = topStrokeRows(context, ui);
        else if ("Key shape".equals(child)) rows = shapeRows(context, ui);
        else if ("Key theme".equals(child)) rows = themeRows(context, ui);
        else if ("Effects".equals(child)) rows = effectsRows(context, ui);
        else rows = memoryQuickRows(context, ui);
        strokeBox.addView(rows, ui.lp(-1, -2));
        for (int i = 0; i < rows.getChildCount(); i++) strokeRows.add(rows.getChildAt(i));
        strokeBoxDebug = strokeBox;
    }

    private LinearLayout topStrokeRows(Context context, UiKit ui) {
        LinearLayout vbox = ui.vbox();
        String[] names = {
                AppPrefs.prefs(context).getBoolean(HAPTICS_KEY, true)
                        ? "Haptics: On" : "Haptics: Off",
                "Memory", showFraction ? "Show as decimal" : "Show as fraction",
                "Key theme", "Key shape", "Effects"};
        for (String n : names) {
            TextView row = ui.label(n, 14, ui.palette().text, false);
            row.setPadding(ui.dp(14), ui.dp(11), ui.dp(14), ui.dp(11));
            row.setTag(n);
            vbox.addView(row, ui.lp(-1, -2));
        }
        return vbox;
    }

    private LinearLayout memoryQuickRows(Context context, UiKit ui) {
        LinearLayout vbox = ui.vbox();
        for (String value : memoryList(context)) {
            TextView row = ui.label(groupIndian(value), 16, ui.palette().text, false);
            row.setPadding(ui.dp(14), ui.dp(10), ui.dp(14), ui.dp(10));
            row.setTag(value);
            vbox.addView(row, ui.lp(-1, -2));
        }
        TextView clear = ui.label("Clear memory", 14, ui.palette().muted, false);
        clear.setPadding(ui.dp(14), ui.dp(10), ui.dp(14), ui.dp(10));
        clear.setTag("Clear memory");
        vbox.addView(clear, ui.lp(-1, -2));
        return vbox;
    }

    private int strokeRowAt(float rawX, float rawY) {
        int[] loc = new int[2];
        for (int i = 0; i < strokeRows.size(); i++) {
            View row = strokeRows.get(i);
            row.getLocationOnScreen(loc);
            if (rawY >= loc[1] && rawY <= loc[1] + row.getHeight()
                    && rawX >= loc[0] - row.getWidth() * 0.25f
                    && rawX <= loc[0] + row.getWidth() * 1.25f) return i;
        }
        return -1;
    }

    private void setStrokeHighlight(int idx) {
        strokeHighlight = idx;
        for (int i = 0; i < strokeRows.size(); i++) {
            strokeRows.get(i).setBackgroundColor(i == idx
                    ? strokeTint : android.graphics.Color.TRANSPARENT);
        }
    }

    private void dismissStroke() {
        if (strokePopup != null) {
            strokePopup.dismiss();
            strokePopup = null;
        }
        strokeBoxDebug = null;
        strokeRows = new java.util.ArrayList<>();
        strokeHighlight = -1;
    }

    /** Test door: screen rect of a live stroke row, by its label. */
    public static int[] debugStrokeRowRect(String tag) {
        LinearLayout box = strokeBoxDebug;
        if (box == null) return null;
        int[] loc = new int[2];
        for (int i = 0; i < box.getChildCount(); i++) {
            View child = box.getChildAt(i);
            if (!(child instanceof LinearLayout)) continue;
            for (int j = 0; j < ((LinearLayout) child).getChildCount(); j++) {
                View row = ((LinearLayout) child).getChildAt(j);
                if (tag.equals(row.getTag())) {
                    row.getLocationOnScreen(loc);
                    return new int[]{loc[0], loc[1],
                            loc[0] + row.getWidth(), loc[1] + row.getHeight()};
                }
            }
        }
        return null;
    }

    private void openMenu(final Context context, UiKit ui, View anchor) {
        closeSelectionMenu();
        AppLog.event(context, "calc", "menu opened from the three dots");
        java.util.List<String> items = new java.util.ArrayList<>();
        items.add(AppPrefs.prefs(context).getBoolean(HAPTICS_KEY, true)
                ? "Haptics: On" : "Haptics: Off");
        // Round 31: "in 3 dots remove add memory" -- storing lives in the
        // selection menu as MAdd now; the three dots keep the other five.
        // Round 33: memory management folds into a single child (view, reorder,
        // clear live inside it) so the top level keeps five slots plus Effects
        // even with Key theme joining the family.
        items.add("Memory");
        items.add(showFraction ? "Show as decimal" : "Show as fraction");
        items.add("Key theme");
        items.add("Key shape");
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
        } else if (choice.equals("Memory")) {
            openMemory(context, ui);
        } else if (choice.startsWith("Show as")) {
            showFraction = !showFraction;
            AppLog.event(context, "calc", showFraction
                    ? "result view: fraction" : "result view: decimal");
            refreshDisplay(context);
        } else if (choice.equals("Key theme")) {
            openKeyTheme(context, ui);
        } else if (choice.equals("Key shape")) {
            openKeyShape(context, ui);
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
        LinearLayout box = pickerBox(context, ui, "Button effects");
        box.addView(effectsRows(context, ui), ui.lp(-1, -2));
        showCenterPopup(context, ui, box);
        AppLog.event(context, "calc", "effects picker opened: current style "
                + EFFECT_NAMES[PadButton.effectStyle(context)]);
    }

    private LinearLayout effectsRows(final Context context, final UiKit ui) {
        final int current = PadButton.effectStyle(context);
        LinearLayout vbox = ui.vbox();
        for (int i = 0; i < EFFECT_NAMES.length; i++) {
            final int idx = i;
            TextView row = ui.label((i == current ? "\u2713  " : "     ") + EFFECT_NAMES[i]
                    + "  \u00b7  " + EFFECT_HINTS[i], 14,
                    i == current ? ui.palette().accent : ui.palette().text,
                    i == current);
            row.setPadding(ui.dp(14), ui.dp(11), ui.dp(14), ui.dp(11));
            row.setTag(EFFECT_NAMES[i]);
            vbox.addView(row, ui.lp(-1, -2));
            row.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    if (menuPopup != null) menuPopup.dismiss();
                    menuPopup = null;
                    applyEffectIdx(context, idx);
                }
            });
        }
        return vbox;
    }

    private void applyEffectIdx(Context context, int idx) {
        AppPrefs.prefs(context).edit().putInt(EFFECTS_KEY, idx).apply();
        AppLog.event(context, "calc", "effects style \u2192 " + EFFECT_NAMES[idx]
                + ", persisted");
        host.toast(EFFECT_NAMES[idx] + " press effect on");
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
        // Round 33: clearing lives inside the memory child now, one tap below
        // the entries it empties.
        final TextView clear = ui.label("Clear memory", 13, ui.palette().muted, false);
        clear.setPadding(ui.dp(14), ui.dp(10), ui.dp(14), ui.dp(4));
        clear.setTag("Clear memory");
        box.addView(clear, ui.lp(-1, -2));
        clear.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                clearMemoryNow(context, ui);
                fillMemory(context, ui, list, holder);
            }
        });
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
            list.addView(ui.label("Nothing stored yet. Hold a number and tap MAdd to "
                    + "store it.", 13, ui.palette().muted, false), ui.lp(-1, -2));
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
                    useMemoryValue(context, value);
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

    /** One stored value back into the expression at the caret, shared by the
     *  memory popup, the stroke child and (as MUse) the selection menu. */
    private void useMemoryValue(Context context, String value) {
        insertNumber(value);
        refreshDisplay(context);
        AppLog.event(context, "calc", "memory recalled: " + value
                + " inserted, expression now " + readable(expression.toString()));
    }

    private void clearMemoryNow(Context context, UiKit ui) {
        int n = memoryList(context).size();
        saveMemory(context, new java.util.ArrayList<String>());
        AppLog.event(context, "calc", "memory cleared: " + n + " entries removed");
        host.toast("Memory cleared");
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

    /** Round 21: LED ink with nothing to get wrong. Per-character colour spans
     *  carry no coordinates (the gradient shaders sampled the wrong space on his
     *  device and printed one flat colour), and a glow layer in dark mode gives
     *  the tubes their halo. Light mode: pitch black, no glow, his words. */
    private void applyLedInk(Context context, String shown) {
        if (displayView == null || previewView == null) return;
        if (host.palette().dark) {
            CharSequence cur = displayView.getText();
            if (cur instanceof Spannable) {
                Spannable sp = (Spannable) cur;
                for (int i = 0; i < sp.length(); i++) {
                    char c = sp.charAt(i);
                    int col = "+-*/%^!\u00d7\u00f7\u2212".indexOf(c) >= 0 ? 0xFFFF2D78
                            : c == '(' || c == ')' ? 0xFF7C4DFF : 0xFF00E5FF;
                    sp.setSpan(new android.text.style.ForegroundColorSpan(col),
                            i, i + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
            displayView.setShadowLayer(14f, 0f, 0f, 0x6600E5FF);
            previewView.setTextColor(0xFFFFAB40);
            previewView.setShadowLayer(12f, 0f, 0f, 0x66FFAB40);
        } else {
            displayView.setShadowLayer(0f, 0f, 0f, 0);
            displayView.setTextColor(PITCH_BLACK);
            previewView.setShadowLayer(0f, 0f, 0f, 0);
            previewView.setTextColor(PITCH_BLACK);
        }
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

        /** Round 29, his words: "cursor is not visible, it should be blinking
         *  atleast. Take reference of how android is." Android's own cursor
         *  controller stands SOLID while the cursor moves and blinks on a
         *  half-second rhythm at rest; a steady thin bar his eye missed becomes
         *  a rhythm his eye cannot miss. The blink is a plain invalidate loop,
         *  the same thing the framework's Blink listener does. */
        /** Round 30: the blink phase lives on the screen now, because the screen
         *  owns the second caret -- the real view -- and the two must blink as one. */
        public boolean caretOn = true;
        public Runnable caretPhaseReset;

        @Override protected void onSelectionChanged(int selStart, int selEnd) {
            super.onSelectionChanged(selStart, selEnd);
            // Solid while the cursor moves, blinking once it rests -- exactly
            // the framework rhythm, on a cursor his device can actually see.
            caretOn = true;
            if (caretPhaseReset != null) caretPhaseReset.run();
            invalidate();
        }

        /** For the instrumented story: is the bar in its visible phase now? */
        boolean caretShowing() {
            return caretOn && getSelectionStart() == getSelectionEnd();
        }

        /** Round 22: the caret mystery, solved by his own log -- on his Moto,
         *  Layout.getPrimaryHorizontal() returned values near 2^20 (x=1048501 for
         *  a 1132px view), so the caret was drawn a million pixels off the glass
         *  while every emulator said "visible". Colour spans carry no metrics, so
         *  measuring the line's run with the view's own paint is the truth; the
         *  Layout is only a fallback when the measure lands outside the view. */
        public float caretX(android.text.Layout lo, int off, int ln) {
            // Round 28, his log: 39 torn-layout guards in ONE handle drag, and a
            // caret logged at x=1132 on a 1132px view. On his OEM the display's
            // DynamicLayout is born 2^20 wide, tears (mRows=0) through gesture
            // frames and carries a stale max scroll -- getPrimaryHorizontal and
            // the line table all lie there, while the eyes see one steady truth:
            // gravity RIGHT pins the last glyph to the content's right edge. So
            // the caret and both handles now measure from the view's OWN paint
            // against that anchor; the layout's line-left is used only when it
            // reads sane (emulator, mid-scroll), and a torn read changes nothing.
            String txt = getText().toString();
            android.graphics.Paint p = getPaint();
            float textW = p.measureText(txt);
            float contentW = getWidth() - getCompoundPaddingLeft()
                    - getCompoundPaddingRight();
            float left = contentW - textW;
            try {
                if (lo != null && lo.getLineCount() > 0) {
                    float cand = lo.getLineLeft(0) - getScrollX();
                    if (cand >= -contentW && cand <= contentW) left = cand;
                }
            } catch (Exception ignored) {
                // A torn line table costs nothing here: the anchor stands.
            }
            int o = Math.max(0, Math.min(off, txt.length()));
            return left + p.measureText(txt, 0, o);
        }

        /** The colour the hand-drawn caret paints with, for the pixel story. */
        public int caretColor;

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
            // Round 29, THE LAST SILENT FAILURE: his r28 log proved the geometry
            // right (caret x=1104 on a 1132px view, ink #FFFFFFFF, caretW=11,
            // zero torn guards) and STILL no bar on his glass -- because this
            // method read the layout's LINE TABLE (getLineForOffset,
            // getLineAscent, getLineDescent, getLineBaseline) and on his OEM
            // those throw on a torn table into a catch that skipped the frame
            // WITHOUT A LOG. The bar now measures from the view's own paint and
            // baseline -- FontMetrics for the vertical, the paint-measured
            // anchor for x -- nothing left in this block can tear.
            android.text.Layout lo = getLayout();
            super.onDraw(c);
            if (lo == null) return;
            try {
                int padL = getCompoundPaddingLeft();
                // Round 27: the caret's home at the end of the line is the
                // content edge -- flush against the last glyph; round 28's side
                // padding keeps that edge on the glass.
                float maxX = getWidth() - getCompoundPaddingRight();
                float minX = padL;
                android.graphics.Paint fp = getPaint();
                float base = getBaseline();
                float t = base + fp.ascent() + 2;
                float b2 = base + fp.descent() - 2;
                int selA = getSelectionStart();
                int selB = getSelectionEnd();
                if (selB > selA) {
                    // Android hides the cursor while a range is held; the two
                    // boundary bars and bulbs stand in its place (round 24).
                    float dd = getResources().getDisplayMetrics().density;
                    float ax = padL + caretX(lo, selA, 0);
                    float bx = padL + caretX(lo, selB, 0);
                    ax = Math.min(Math.max(ax, minX), maxX);
                    bx = Math.min(Math.max(bx, minX), maxX);
                    c.drawRect(ax, t, ax + caretWidth, b2, caretPaint);
                    c.drawRect(bx, t, bx + caretWidth, b2, caretPaint);
                    c.drawCircle(ax + caretWidth / 2f, b2 + 14 * dd,
                            7 * dd, caretPaint);
                    c.drawCircle(bx + caretWidth / 2f, b2 + 14 * dd,
                            7 * dd, caretPaint);
                } else if (caretOn) {
                    // The blinking bar: solid while it moves (onSelectionChanged
                    // resets the phase), half-second blink at rest, exactly the
                    // rhythm Android's own cursor keeps.
                    int cs = selA >= 0 ? selA : Math.max(0, fallbackCaret);
                    if (cs <= length()) {
                        float x = padL + caretX(lo, cs, 0);
                        if (x > maxX) x = maxX;
                        if (x < minX) x = minX;
                        c.drawRect(x, t, x + caretWidth, b2, caretPaint);
                    }
                }
            } catch (Exception e) {
                // Round 26: skip the frame; never the process. Nothing left in
                // this block reads a line table, so this is belt and braces.
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
                        // Round 27: his log showed claims whose UP never
                        // arrived, so the popup opened only by button. The
                        // history opens AT the claim now; the UP merely ends
                        // the gesture.
                        if (listener != null) listener.onSwipeDown();
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
                }
                return true;
            }
            return super.onTouchEvent(e);
        }
    }

    @Override public void onResume() { }

    @Override public void onPause() {
        // Round 33: every popup here is a window of its own. If the screen is
        // torn down while one stands open -- app switched, activity reclaimed,
        // suite moving to the next story -- the window outlives its screen and
        // lands on whatever comes next: a ghost panel on his phone, a touch
        // thief in the tests. Pause takes them all down, stops the blink, and
        // resets the history flag so a dead popup can never block the next
        // hold-select.
        if (historyPopup != null && historyPopup.isShowing()) historyPopup.dismiss();
        historyPopup = null;
        if (menuPopup != null && menuPopup.isShowing()) menuPopup.dismiss();
        menuPopup = null;
        dismissStroke();
        if (strokeDwell != null) {
            strokeHandler.removeCallbacks(strokeDwell);
            strokeDwell = null;
        }
        closeSelectionMenu();
        if (displayView != null && caretBlinker != null) {
            displayView.removeCallbacks(caretBlinker);
        }
        historyOpen = false;
        AppLog.event(lastContext, "calc",
                "screen paused: popups dismissed, blink stopped");
    }

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
    /** Round 25: map a touch point to a text offset through the layout with
     *  the inner scroll applied -- getOffsetForPosition ignored it on his
     *  device, which is why the handles dragged hit-or-miss. */
    private int offsetAt(float x, float y) {
        // Round 28, his log: a handle drag produced THIRTY-NINE consecutive
        // "offsetAt guarded a torn layout" lines and the held range never
        // moved -- on his OEM every drag frame meets a torn DynamicLayout, so
        // the guarded layout path skipped the whole gesture. The mapping is now
        // the inverse of caretX's paint-measured anchor: binary search over the
        // view's own measureText, which no line table can tear. The layout is
        // consulted for its line-left only, inside a guard that keeps the
        // round-26 log line honest when it tears.
        CharSequence txt = displayView.getText();
        if (txt == null || txt.length() == 0) return 0;
        String s = txt.toString();
        android.graphics.Paint p = displayView.getPaint();
        float textW = p.measureText(s);
        float padL = displayView.getCompoundPaddingLeft();
        float contentW = displayView.getWidth() - padL
                - displayView.getCompoundPaddingRight();
        float left = contentW - textW;
        try {
            android.text.Layout lo = displayView.getLayout();
            if (lo != null && lo.getLineCount() > 0) {
                float cand = lo.getLineLeft(0) - displayView.getScrollX();
                if (cand >= -contentW && cand <= contentW) left = cand;
            }
        } catch (Exception e) {
            AppLog.event(lastContext, "calc", "offsetAt guarded a torn layout: "
                    + e.getClass().getSimpleName());
        }
        float target = x - padL - left;
        int loI = 0, hiI = s.length();
        while (loI < hiI) {
            int mid = (loI + hiI) / 2;
            if (p.measureText(s, 0, mid) < target) loI = mid + 1;
            else hiI = mid;
        }
        if (loI > 0) {
            float before = p.measureText(s, 0, loI - 1);
            float after = p.measureText(s, 0, loI);
            if (Math.abs(before - target) < Math.abs(after - target)) loI--;
        }
        return Math.max(0, Math.min(s.length(), loI));
    }

    /** Round 28: the on-screen text extent of the single display line, in view
     *  coordinates, from the same paint-measured anchor caretX uses. The hold
     *  gesture needs it to tell "on the expression" from "on empty box". */
    private float[] textExtent() {
        CharSequence txt = displayView.getText();
        String s = txt == null ? "" : txt.toString();
        android.graphics.Paint p = displayView.getPaint();
        float textW = p.measureText(s);
        float padL = displayView.getCompoundPaddingLeft();
        float contentW = displayView.getWidth() - padL
                - displayView.getCompoundPaddingRight();
        float left = contentW - textW;
        try {
            android.text.Layout lo = displayView.getLayout();
            if (lo != null && lo.getLineCount() > 0) {
                float cand = lo.getLineLeft(0) - displayView.getScrollX();
                if (cand >= -contentW && cand <= contentW) left = cand;
            }
        } catch (Exception ignored) {
            // anchor stands
        }
        return new float[]{padL + left, padL + left + textW};
    }

    /** Round 28: paste and MR share one validated splice -- insert clean raw
     *  text over [a,b), refresh, park the caret after it, log the verdict. */
    private boolean spliceRaw(Context context, int a, int b, String clean,
                              String verb) {
        String raw = expression.toString();
        a = Math.max(0, Math.min(a, raw.length()));
        b = Math.max(a, Math.min(b, raw.length()));
        String candidate = raw.substring(0, a) + clean + raw.substring(b);
        if (wholeExpressionValid(candidate)) {
            expression.setLength(0);
            expression.append(candidate);
            justEvaluated = false;
            result = "";
            caret = a + clean.length();
            refreshDisplay(context);
            AppLog.event(context, "calc", verb + " inserted "
                    + clean.length() + " chars at " + a);
            return true;
        }
        AppLog.event(context, "calc", verb + " refused: " + clean
                + " is not a valid expression here");
        return false;
    }

    /** Round 30: position the real caret-bar view from the same paint-measured
     *  anchor the canvas bar uses, and honour the blink phase and the
     *  Android rule that a held selection hides the cursor. */
    private void placeCaretBar() {
        if (caretBar == null || displayView == null) return;
        try {
            if (displayView.getSelectionEnd() > displayView.getSelectionStart()) {
                if (caretBar.getVisibility() != View.GONE) {
                    caretBar.setVisibility(View.GONE);
                }
                return;
            }
            int cs = displayView.getSelectionStart();
            cs = Math.max(0, Math.min(cs, displayView.length()));
            float x = displayView.getCompoundPaddingLeft()
                    + displayView.caretX(displayView.getLayout(), cs, 0);
            float maxX = displayView.getWidth()
                    - displayView.getCompoundPaddingRight();
            float minX = displayView.getCompoundPaddingLeft();
            if (x > maxX) x = maxX;
            if (x < minX) x = minX;
            android.graphics.Paint fp = displayView.getPaint();
            float base = displayView.getBaseline();
            float t = base + fp.ascent() + 2;
            float b2 = base + fp.descent() - 2;
            android.view.ViewGroup.LayoutParams lp = caretBar.getLayoutParams();
            if (lp != null) {
                int h = Math.max(4, (int) (b2 - t));
                if (lp.height != h) {
                    lp.height = h;
                    caretBar.setLayoutParams(lp);
                }
            }
            caretBar.setX(x);
            caretBar.setY(t);
            int want = caretOn ? View.VISIBLE : View.GONE;
            if (caretBar.getVisibility() != want) caretBar.setVisibility(want);
        } catch (Exception e) {
            AppLog.event(lastContext, "calc", "caret bar placement guarded: "
                    + e.getClass().getSimpleName());
        }
    }

    /** Round 25: the click-while-held clear, at the finger's point. */
    private void clearSelectionAt(float x, float y) {
        CharSequence held = displayView.getText();
        if (!(held instanceof Spannable)) return;
        int off = Math.max(0, Math.min(offsetAt(x, y), held.length()));
        Selection.setSelection((Spannable) held, off, off);
        caret = dispToRaw(off);
        if (currentSelectionMode != null) currentSelectionMode.finish();
        AppLog.event(lastContext, "calc",
                "tap placed the caret at " + caret + ", selection released");
        displayView.invalidate();
    }

    /** Round 25: a click away from the expression clears to the end. */
    private void clearSelectionAway() {
        CharSequence held = displayView.getText();
        if (!(held instanceof Spannable) || !displayView.hasSelection()) return;
        Selection.setSelection((Spannable) held, held.length(), held.length());
        caret = dispToRaw(held.length());
        if (currentSelectionMode != null) currentSelectionMode.finish();
        AppLog.event(lastContext, "calc",
                "tap away from the expression cleared the selection, caret at end");
        displayView.invalidate();
    }

    /** Round 24: one word-selection routine for the hold and for the
     *  plain tap he asked for -- "if a we click somewhere in expression box
     *  than the content, it should get select as well". */
    private boolean selectWordAt(float x, float y) {
        // Round 26: the 23:53:12 crash was a tap's UP landing while the
        // history swipe was repopulating the popup; gestures wait for calm.
        if (historyOpen) return false;
        CharSequence txt = displayView.getText();
        if (!(txt instanceof Spannable) || txt.length() == 0) return false;
        // Round 28, his words: "if pressed a hold on other area of expression
        // bar where expression is not even written, then also some operator of
        // expression is getting selected. It should not." The clamp inside the
        // old offset mapping turned a press on EMPTY box into offset 0 -- the
        // first word, an operator, held. Now the press point is tested against
        // the painted text extent; outside it the hold parks the caret at the
        // near end and raises Google's caret menu (MR, Paste, Select all)
        // instead of selecting anything.
        float d = displayView.getResources().getDisplayMetrics().density;
        float[] ext = textExtent();
        if (x < ext[0] - 8 * d || x > ext[1] + 8 * d) {
            int park = x > ext[1] ? txt.length() : 0;
            Selection.setSelection((Spannable) txt, park, park);
            extendAnchor = park;
            heldA = park;
            heldB = park;
            caret = dispToRaw(park);
            caretMode = true;
            displayView.startActionMode(selectionMenu,
                    android.view.ActionMode.TYPE_FLOATING);
            AppLog.event(lastContext, "calc",
                    "long press on empty box: no selection, caret parked at "
                    + caret + ", caret menu raised");
            displayView.invalidate();
            return true;
        }
        caretMode = false;
        int off = offsetAt(x, y);
        off = Math.max(0, Math.min(off, txt.length() - 1));
        char ch = txt.charAt(off);
        int a = off, b = off + 1;
        if (Character.isDigit(ch) || ch == ',' || ch == '.') {
            while (a > 0 && (Character.isDigit(txt.charAt(a - 1))
                    || txt.charAt(a - 1) == ',' || txt.charAt(a - 1) == '.')) a--;
            while (b < txt.length() && (Character.isDigit(txt.charAt(b))
                    || txt.charAt(b) == ',' || txt.charAt(b) == '.')) b++;
        }
        Selection.setSelection((Spannable) txt, a, b);
        extendAnchor = a;
        heldA = a;
        heldB = b;
        AppLog.event(lastContext, "calc", "selection started: chars " + a
                + ".." + b
                + " held by the app gesture, framework selection set");
        displayView.startActionMode(selectionMenu,
                android.view.ActionMode.TYPE_FLOATING);
        return true;
    }

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
