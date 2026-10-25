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
    private static final int HISTORY_CAP = 50;
    private static final int ANIM_MS = 220;

    /** Key visual classes: four fills, the way Google's pad reads at a glance. */
    private static final int DIGIT = 0, OP = 1, CMD = 2, EQ = 3, TOG = 4;

    /** The multi-character caps, longest first, for replaying an edited expression. */
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
    private LinearLayout chipSlot;
    private ChevronView chevron;
    private PopupWindow menuPopup;
    private LinearLayout padContainer;
    private ScrollView historyScroll;
    private SwipeBox displayArea;
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
        View modesRule = new View(context);
        modesRule.setBackgroundColor(mix(ui.palette().surface, ui.palette().text, 0.18f));
        root.addView(modesRule, ui.lp(-1, 1));
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
        chipSlot = ui.hbox();
        modes.addView(chipSlot, ui.lp(-2, 34));
        rebuildChip(context, ui);
        TextView menu = ui.pillButton("Menu", ui.palette().surface, ui.palette().muted);
        menu.setTextSize(12);
        LinearLayout.LayoutParams mnp = ui.lp(-2, 34);
        mnp.leftMargin = ui.dp(6);
        modes.addView(menu, mnp);
        menu.setOnClickListener(new View.OnClickListener() {
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
        GradientDrawable dockBg = new GradientDrawable();
        dockBg.setCornerRadius(ui.dp(14));
        dockBg.setColor(mix(ui.palette().background, ui.palette().surface, 0.28f));
        dockBg.setStroke(1, mix(ui.palette().surface, ui.palette().accent, 0.35f));
        currentBox.setBackground(dockBg);
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
        View padRule = new View(context);
        padRule.setBackgroundColor(mix(ui.palette().surface, ui.palette().text, 0.18f));
        padContainer.addView(padRule, ui.lp(-1, 1));
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
            bg.setColor(borderColor);
            bg.setStroke((int) (context.getResources().getDisplayMetrics().density * 1.5f), color);
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
                    press(context, key);
                }
            });
        }
        parent.addView(row, new LinearLayout.LayoutParams(-1, 0, 1f));
    }

    /** Inv renames the trigonometric caps in place, the way Google's Inv toggle does. */
    private String labelFor(String key) {
        if ("Deg".equals(key)) return degrees ? "Deg" : "Rad";
        if (!invMode) return key;
        if ("sin(".equals(key)) return "asin(";
        if ("cos(".equals(key)) return "acos(";
        if ("tan(".equals(key)) return "atan(";
        return key;
    }

    /** Relabel the toggle-driven caps without rebuilding a single view. */
    private void relabelCaps() {
        for (java.util.Map.Entry<String, TextView> e : capButtons.entrySet()) {
            e.getValue().setText(labelFor(e.getKey()));
        }
    }

    private TextView keyButton(Context context, UiKit ui, String key, float radiusDp) {
        int kind = kindOf(key);
        boolean toggleOn = ("Deg".equals(key) && !degrees) || ("Inv".equals(key) && invMode);
        int fill;
        int ink;
        float size;
        switch (kind) {
            case EQ:
                fill = ui.palette().accent;
                ink = android.graphics.Color.WHITE;
                size = 26;
                break;
            case CMD:
                fill = mix(ui.palette().surface, ui.palette().accent, 0.42f);
                ink = android.graphics.Color.WHITE;
                size = 22;
                break;
            case TOG:
                fill = toggleOn ? mix(ui.palette().surface, ui.palette().accent, 0.42f)
                        : mix(ui.palette().surface, ui.palette().accent, 0.16f);
                ink = toggleOn ? android.graphics.Color.WHITE : ui.palette().accent;
                size = 16;
                break;
            case OP:
                fill = mix(ui.palette().surface, ui.palette().accent, 0.16f);
                ink = ui.palette().accent;
                size = isScientificCap(key) ? 19 : "( )".equals(key) || "⌫".equals(key) ? 22 : 26;
                break;
            default:
                fill = ui.palette().surface;
                ink = ui.palette().text;
                size = isScientificCap(key) ? 19 : 30;
                break;
        }
        TextView button = new TextView(context);
        button.setText(key);
        button.setGravity(Gravity.CENTER);
        button.setSingleLine(true);
        button.setTextSize(size);
        button.setTextColor(ink);
        GradientDrawable shape = new GradientDrawable();
        shape.setCornerRadius(ui.dp(radiusDp));
        shape.setColor(fill);
        button.setBackground(shape);
        button.setContentDescription(key);
        return button;
    }

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
                || "π".equals(key) || "e".equals(key);
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
        final HorizontalScrollView exprScroll = new HorizontalScrollView(context);
        exprScroll.setHorizontalScrollBarEnabled(false);
        exprScroll.setFillViewport(true);
        exprScroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        displayView = new DisplayEditText(context,
                Math.max(2, (int) (3 * context.getResources().getDisplayMetrics().density)));
        displayView.setId(R.id.calc_display);
        displayView.setGravity(Gravity.RIGHT | Gravity.BOTTOM);
        displayView.setMaxLines(1);
        displayView.setFocusable(true);
        displayView.setFocusableInTouchMode(true);
        displayView.setHighlightColor(mix(ui.palette().surface, ui.palette().accent, 0.5f));
        displayView.setTextColor(ui.palette().text);
        // The caret must be VISIBLE, his point: the theme's default was not showing on his
        // device, so it is drawn explicitly in the accent colour, two density pixels wide.
        displayView.setCursorVisible(true);
        final android.graphics.drawable.GradientDrawable cursorMark =
                new android.graphics.drawable.GradientDrawable();
        cursorMark.setColor(ui.palette().accent);
        cursorMark.setSize(ui.dp(3), ui.dp(24));
        displayView.setCaretColors(ui.palette().text,
                mix(ui.palette().accent, ui.palette().background, 0.55f));
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
        displayView.setOnTouchListener(new View.OnTouchListener() {
            @Override public boolean onTouch(View v, MotionEvent e) {
                if (e.getAction() == MotionEvent.ACTION_UP) {
                    // The editor places its own caret; the hand-rolled offset math that used
                    // to run here pinned every tap to 0 on the device -- "cursor placed at 0",
                    // 103 times in his log. Now the log records where the editor actually
                    // landed, after it has landed.
                    displayView.post(new Runnable() {
                        @Override public void run() {
                            caret = Math.max(0, displayView.getSelectionStart());
                            AppLog.event(lastContext, "calc", "cursor placed at " + caret);
                        }
                    });
                }
                return false;
            }
        });
        displayView.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(android.text.Editable s) {
                if (displayView.applying) return;
                // Cut, paste, drag-handle edits and IME-free hand typing all arrive here.
                // What validates joins the expression; what does not is reverted, and both
                // outcomes are logged, because a silent revert is a lie in the log.
                String txt = s.toString();
                String raw = txt.replace('\u00d7', '*').replace('\u00f7', '/')
                        .replace('\u2212', '-');
                if (wholeExpressionValid(raw)) {
                    expression.setLength(0);
                    expression.append(raw);
                    justEvaluated = false;
                    result = "";
                    caret = Math.max(0, displayView.getSelectionStart());
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
        previewView = ui.label(" ", 17, ui.palette().muted, false);
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
        int size = shown.length() > 20 ? 26 : shown.length() > 12 ? 34 : 46;
        displayView.setTextSize(size);
        displayView.applying = true;
        displayView.setText(shown);
        displayView.applying = false;
        restoreCaret();
        if (!displayView.hasFocus()) displayView.requestFocus();
        previewView.setText(preview());
    }

    private void restoreCaret() {
        if (justEvaluated || displayView == null) return;
        CharSequence cs = displayView.getText();
        if (cs instanceof Spannable) {
            int pos = Math.max(0, Math.min(caret, cs.length()));
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
        bg.setColor(mix(ui.palette().background, ui.palette().surface, 0.6f));
        bg.setStroke(1, mix(ui.palette().surface, ui.palette().text, 0.25f));
        box.setBackground(bg);
        box.setPadding(ui.dp(8), ui.dp(6), ui.dp(8), ui.dp(8));
        LinearLayout head = ui.hbox();
        head.setGravity(Gravity.CENTER_VERTICAL);
        box.addView(head, ui.lp(-1, 46));
        historyTitle = ui.label("", 14, ui.palette().text, true);
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
            LinearLayout line = ui.vbox();
            line.setPadding(0, ui.dp(7), 0, ui.dp(7));
            historyList.addView(line, ui.lp(-1, -2));
            LinearLayout pair = ui.hbox();
            pair.setGravity(Gravity.CENTER_VERTICAL);
            line.addView(pair, ui.lp(-1, -2));
            LinearLayout stack = ui.vbox();
            stack.setGravity(Gravity.LEFT);
            pair.addView(stack, new LinearLayout.LayoutParams(0, -2, 1f));
            TextView e = ui.label(expr, 13, ui.palette().muted, false);
            e.setGravity(Gravity.LEFT);
            stack.addView(e, ui.lp(-1, -2));
            TextView r = ui.label(value, 17, ui.palette().text, true);
            r.setGravity(Gravity.LEFT);
            stack.addView(r, ui.lp(-1, -2));
            TextView when = ui.label(stamp(context, entry[2]), 10, ui.palette().muted, false);
            when.setGravity(Gravity.RIGHT | Gravity.TOP);
            LinearLayout.LayoutParams wp = ui.lp(-2, -2);
            wp.leftMargin = ui.dp(8);
            pair.addView(when, wp);
            // A hairline between entries: the owner asked for borders enough to tell the
            // sections apart, and a list without them reads as one long smear.
            View divider = new View(context);
            divider.setBackgroundColor(mix(ui.palette().surface, ui.palette().text, 0.12f));
            line.addView(divider, new LinearLayout.LayoutParams(-1, 1));
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
    private void toggleHistory(final Context context, final UiKit ui) {
        if (historyPopup != null && historyPopup.isShowing()) {
            closeHistory(context);
            return;
        }
        historyOpen = true;
        View content = buildHistoryPopup(context, ui);
        int width = context.getResources().getDisplayMetrics().widthPixels - ui.dp(24);
        int height = (int) (context.getResources().getDisplayMetrics().heightPixels * 0.62f);
        historyPopup = new PopupWindow(content, width, height, true);
        historyPopup.setElevation(ui.dp(10));
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

    private void rebuildChip(Context context, UiKit ui) {
        chipSlot.removeAllViews();
        if (!AppPrefs.prefs(context).contains(MEMORY_KEY)) return;
        final String stored = AppPrefs.prefs(context).getString(MEMORY_KEY, "0");
        TextView chip = ui.pillButton("M", ui.palette().accent, android.graphics.Color.WHITE);
        chip.setTextSize(15);
        chip.setTypeface(chip.getTypeface(), android.graphics.Typeface.BOLD);
        chip.setMinWidth(ui.dp(44));
        chip.setContentDescription("Memory holds " + stored);
        LinearLayout.LayoutParams chp = ui.lp(-2, 34);
        chp.leftMargin = ui.dp(6);
        chipSlot.addView(chip, chp);
        chip.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                insertNumber(stored);
                AppLog.event(context, "calc", "memory recalled: " + stored
                        + " inserted, expression now " + readable(expression.toString()));
                refreshDisplay(context);
            }
        });
    }

    /** The overflow, labelled and themed: a popup in the app's own palette, not a white
     *  system dialog dropped onto a dark screen. */
    private void openMenu(final Context context, UiKit ui, View anchor) {
        AppLog.event(context, "calc", "menu opened");
        final boolean memorySet = AppPrefs.prefs(context).contains(MEMORY_KEY);
        java.util.List<String> items = new java.util.ArrayList<>();
        items.add(showFraction ? "Show as decimal" : "Show as fraction");
        items.add(memorySet ? "Store again in memory" : "Store in memory");
        if (memorySet) {
            items.add("Recall memory");
            items.add("Add to memory");
            items.add("Subtract from memory");
            items.add("Clear memory");
        }
        items.add(AppPrefs.prefs(context).getBoolean(HAPTICS_KEY, true)
                ? "Haptics: On" : "Haptics: Off");
        LinearLayout box = ui.vbox();
        GradientDrawable bg = pill(ui, ui.palette().surface, 16);
        box.setBackground(bg);
        box.setPadding(ui.dp(6), ui.dp(6), ui.dp(6), ui.dp(6));
        for (String item : items) {
            final String choice = item;
            TextView row = ui.label(item, 14, ui.palette().text, false);
            row.setPadding(ui.dp(14), ui.dp(11), ui.dp(14), ui.dp(11));
            box.addView(row, ui.lp(-1, -2));
            row.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    onMenuChoice(context, ui, choice);
                    if (menuPopup != null) menuPopup.dismiss();
                }
            });
        }
        menuPopup = new PopupWindow(box, ui.dp(220), -2, true);
        menuPopup.setElevation(ui.dp(8));
        menuPopup.showAsDropDown(anchor, 0, ui.dp(4), Gravity.RIGHT);
    }

    private void onMenuChoice(Context context, UiKit ui, String choice) {
        if (choice.startsWith("Show as")) {
            showFraction = !showFraction;
            AppLog.event(context, "calc", showFraction
                    ? "result view: fraction" : "result view: decimal");
            refreshDisplay(context);
        } else if (choice.startsWith("Store")) {
            AppPrefs.prefs(context).edit().putString(MEMORY_KEY, displayValue()).apply();
            AppLog.event(context, "calc", "memory stored: " + displayValue());
            host.toast("Stored in memory");
            rebuildChip(context, ui);
        } else if (choice.startsWith("Haptics")) {
            boolean on = !AppPrefs.prefs(context).getBoolean(HAPTICS_KEY, true);
            AppPrefs.prefs(context).edit().putBoolean(HAPTICS_KEY, on).apply();
            AppLog.event(context, "calc", "haptics " + (on ? "on" : "off") + ", persisted");
            host.toast(on ? "Haptics on" : "Haptics off");
        } else if (choice.startsWith("Recall")) {
            String stored = AppPrefs.prefs(context).getString(MEMORY_KEY, "0");
            insertNumber(stored);
            refreshDisplay(context);
            AppLog.event(context, "calc", "memory recalled into the expression: " + stored);
        } else if (choice.startsWith("Add to memory") || choice.startsWith("Subtract")) {
            try {
                double stored = Double.parseDouble(
                        AppPrefs.prefs(context).getString(MEMORY_KEY, "0"));
                double shown = Double.parseDouble(displayValue());
                double next = choice.startsWith("Add") ? stored + shown : stored - shown;
                String formatted = CalculatorRules.format(next);
                AppPrefs.prefs(context).edit().putString(MEMORY_KEY, formatted).apply();
                AppLog.event(context, "calc", (choice.startsWith("Add") ? "memory + " : "memory - ")
                        + shown + " → " + formatted);
                host.toast("Memory is " + formatted);
                rebuildChip(context, ui);
            } catch (Exception e) {
                AppLog.event(context, "calc", "memory math refused: no number on the display");
                host.toast("Nothing to use on the display");
            }
        } else {
            AppPrefs.prefs(context).edit().remove(MEMORY_KEY).apply();
            AppLog.event(context, "calc", "memory cleared");
            host.toast("Memory cleared");
            rebuildChip(context, ui);
        }
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
    static final class DisplayEditText extends android.widget.EditText {
        boolean applying;

        DisplayEditText(Context context) {
            super(context);
            setInputType(android.text.InputType.TYPE_NULL);
            setShowSoftInputOnFocus(false);
            setBackground(null);
        }

        private final android.graphics.Paint caretPaint =
                new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        private final android.graphics.Paint selPaint = new android.graphics.Paint();
        private int caretWidth = 3;

        DisplayEditText(Context context, int widthPx) {
            super(context);
            caretWidth = widthPx;
            caretPaint.setStyle(android.graphics.Paint.Style.FILL);
            selPaint.setStyle(android.graphics.Paint.Style.FILL);
        }

        void setCaretColors(int caretColor, int selectionColor) {
            caretPaint.setColor(caretColor);
            selPaint.setColor(selectionColor);
        }

        /**
         * The caret the owner could never find: drawn by hand, every frame, focused or
         * not, because three rounds of framework cursor machinery stayed invisible on his
         * device. The selection band is drawn too, so selecting text is finally visible.
         */
        @Override protected void onDraw(android.graphics.Canvas c) {
            super.onDraw(c);
            android.text.Layout lo = getLayout();
            if (lo == null) return;
            int padL = getCompoundPaddingLeft();
            int padT = getCompoundPaddingTop();
            int s = getSelectionStart();
            int en = getSelectionEnd();
            if (s >= 0 && en > s) {
                int l1 = lo.getLineForOffset(s);
                int l2 = lo.getLineForOffset(en);
                for (int ln = l1; ln <= l2; ln++) {
                    float x1 = ln == l1 ? lo.getPrimaryHorizontal(s) : lo.getLineLeft(ln);
                    float x2 = ln == l2 ? lo.getPrimaryHorizontal(en) : lo.getLineRight(ln);
                    c.drawRect(padL + x1, padT + lo.getLineTop(ln),
                            padL + x2, padT + lo.getLineBottom(ln), selPaint);
                }
            }
            int cs = Math.max(0, s);
            if (cs <= length()) {
                int ln = lo.getLineForOffset(cs);
                float x = padL + lo.getPrimaryHorizontal(cs);
                c.drawRect(x, padT + lo.getLineTop(ln) + 2,
                        x + caretWidth, padT + lo.getLineBottom(ln) - 2, caretPaint);
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
            return result;
        }
        return expression.length() == 0 ? "0" : readable(expression.toString());
    }

    /** The live result under the display; empty while what is typed cannot evaluate yet. */
    private String preview() {
        if (justEvaluated) return readable(expression.toString());
        String candidate = trimmed();
        if (candidate.isEmpty()) return " ";
        try {
            return "= " + CalculatorRules.format(CalculatorRules.evaluate(candidate, degrees));
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
                return new int[]{Math.min(s, expression.length()), Math.min(e, expression.length())};
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
        String display = readable(raw);
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
        if ("Deg".equals(key)) {
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
            String bracket = CalculatorRules.smartBracket(expression.toString());
            if (insertAtCaret(context, bracket)) {
                AppLog.event(context, "calc", "key ( ) inserted " + bracket
                        + " at " + caret + ": now " + readable(expression.toString()));
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
            String typed = trimmed();
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
        caret = candidate.length();
        AppLog.event(context, "calc", "key " + key + " at " + at + " → "
                + readable(expression.toString()));
        refreshDisplay(context);
        return true;
    }

    /** The display glyphs are typographic; the parser takes ASCII operators. */
    private static String readable(String raw) {
        return raw.replace('*', '×').replace('/', '÷').replace('-', '−');
    }
}
