package app.space.feature.calculator;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.text.Selection;
import android.text.Spannable;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import app.space.core.data.AppLog;
import app.space.core.data.AppPrefs;
import app.space.core.design.UiKit;
import app.space.core.suite.Screen;
import app.space.core.suite.ScreenHost;

/**
 * The calculator, rebuilt a third time against the owner's side-by-side screenshots of
 * Google's calculator (2026-10-25): "Even after giving you proper reference app to recreate,
 * still you are too behind to adopt the same."
 *
 * <p>What the screenshots showed and this revision adopts, point by point:
 * <ul>
 *   <li><b>Keys are buttons.</b> Google's pad is filled shapes with gaps between them --
 *       circles and stadiums on a dark ground, digits one fill, operators a lighter fill,
 *       AC saturated, equals saturated. Ours was bare glyphs floating on the background,
 *       which read as text, not as something to press. Every key here is a filled stadium
 *       with a margin on all four sides, in four visual classes.</li>
 *   <li><b>Google's key order.</b> AC, ( ), %, divide / 7 8 9 times / 4 5 6 minus /
 *       1 2 3 plus / 0 . backspace equals -- the bracket key on the basic pad and the
 *       backspace in the bottom row are exactly where Google keeps them, and equals is one
 *       cell, not a double-width slab.</li>
 *   <li><b>The expandable scientific panel.</b> Google's portrait keeps the basic pad and
 *       grows three rows above it from a chevron at the pad's top edge -- sqrt, pi, power,
 *       factorial / Deg, sin, cos, tan / Inv, e, ln, log -- and collapses them again. The
 *       old page-swap is gone; the chevron opens and closes the panel over the same pad.</li>
 *   <li><b>An editable expression.</b> The display is selectable text with a caret: tap to
 *       place the cursor, drag or hold to select, and every key inserts at the cursor --
 *       validated by replaying the whole expression through the rules, so an insertion that
 *       would break the grammar is refused exactly as appending it would be.</li>
 *   <li><b>Gestures that fire.</b> The swipe-down and the hold lived on a container whose
 *       children consumed every touch, so neither ever ran on a real phone -- which is what
 *       "not working" meant in the owner's report. The swipe now lives in a container that
 *       intercepts its own gesture, and the hold is the framework's own select-and-copy,
 *       because selecting text is what the owner actually wanted the hold to do.</li>
 * </ul>
 *
 * <p>What earlier rounds built and this keeps: the live grey result under the display,
 * postfix contextual percent, history persisted in prefs with tap-to-reuse and a Clear
 * button while it is shown, the memory slot with its M chip, the decimal/fraction view,
 * hardware-keyboard entry through {@link #typeKey}, the landscape six-column combined grid,
 * the three-step display shrink, haptics, and the parent-fit layout rule this screen died
 * once for (keys take their height from the row; the root asks its real parent for its real
 * height at first layout).
 *
 * <p>Every user action writes an outcome line to the diagnostics log -- what was pressed,
 * what the expression became, what was refused and why, what history showed, what copy put
 * on the clipboard, what size the layout actually got. The owner's instruction of
 * 2026-10-25: "let the logs record in detail about user activity and whether those actions
 * got happened or not." A log line that cannot fail is not an outcome, so each one carries
 * the result of the action it describes.
 */
public final class CalculatorScreen implements Screen {

    /** The basic pad in Google's order. The bracket key and the bottom-row backspace are his. */
    private static final String[][] BASE_ROWS = {
            {"AC", "( )", "%", "÷"},
            {"7", "8", "9", "×"},
            {"4", "5", "6", "−"},
            {"1", "2", "3", "+"},
            {"0", ".", "⌫", "="},
    };

    /**
     * The scientific panel Google grows above the basic pad from the chevron: three rows,
     * his order, with the Deg and Inv toggles living inside the panel as keys, which is
     * where his screenshot keeps them. Labels are resolved at build time so Inv renames
     * the trigonometric row in place.
     */
    private static final String[][] PANEL_ROWS = {
            {"√(", "π", "^", "!"},
            {"Deg", "sin(", "cos(", "tan("},
            {"Inv", "e", "ln(", "log("},
    };

    /**
     * Landscape, the way Google does it: the scientific column-pair joins the basic pad in
     * one six-column grid, because a rotated phone has the width and nobody should page
     * between halves of a calculator. Deg and Inv come along as keys; ln and e stay in the
     * portrait panel, the two constants a rotated phone can spare.
     */
    private static final String[][] LANDSCAPE_ROWS = {
            {"√(", "π", "AC", "( )", "%", "÷"},
            {"Deg", "Inv", "7", "8", "9", "×"},
            {"sin(", "cos(", "4", "5", "6", "−"},
            {"tan(", "log(", "1", "2", "3", "+"},
            {"^", "!", "0", ".", "⌫", "="},
    };

    private static final String HISTORY_KEY = "calculator_history";
    private static final String MEMORY_KEY = "calculator_memory";
    private static final int HISTORY_CAP = 50;

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
    private boolean showFraction;
    private int caret;
    private Context lastContext;
    private TextView displayView;

    public CalculatorScreen(ScreenHost host) {
        this.host = host;
    }

    @Override public String title() { return "Calculator"; }

    @Override
    public View build(final Context context) {
        lastContext = context;
        displayView = null;
        final UiKit ui = new UiKit(context, host.palette());
        LinearLayout root = ui.vbox();
        root.setPadding(ui.dp(16), ui.dp(8), ui.dp(16), ui.dp(10));
        // The shell hands every screen's root wrap-content params, and a wrap-content parent
        // gives weighted children nothing to distribute: on one device the keypad vanished,
        // on the owner's it collapsed and floated mid-screen. Claiming the whole viewport
        // overcorrected -- the host's real area is the viewport MINUS its header and bottom
        // bar, so the claim pushed the mode row under the header and cut the equals row
        // behind the bar. The durable answer is to stop guessing: at first layout, ask the
        // actual parent for its actual height and fit it exactly. Weights then distribute
        // real space, the display owns the top and the pad sits against the bottom, on any
        // host chrome and any screen size.
        final boolean landscapeAtBuild = context.getResources().getConfiguration().orientation
                == android.content.res.Configuration.ORIENTATION_LANDSCAPE;
        final int rowsAtBuild = padRowCount(landscapeAtBuild);
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
                        // The outcome line: not "the screen built" but what the layout
                        // actually became, which is the half a build log cannot see.
                        AppLog.event(context, "calc", "laid out: parent " + avail
                                + "px tall, pad " + rowsAtBuild + " rows, panel "
                                + (panelOpen ? "open" : "closed"));
                    }
                }
            }
        });

        // ---- the quiet chrome row: memory chip, overflow menu, and the gesture hint ----
        LinearLayout modes = ui.hbox();
        modes.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(modes, ui.lp(-1, 30));
        if (AppPrefs.prefs(context).contains(MEMORY_KEY)) {
            TextView chip = ui.pillButton("M", ui.palette().accent, android.graphics.Color.WHITE);
            chip.setTextSize(10);
            modes.addView(chip, ui.lp(-2, 28));
            chip.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    String stored = AppPrefs.prefs(context).getString(MEMORY_KEY, "0");
                    insertNumber(context, stored);
                    AppLog.event(context, "calc", "memory recalled: " + stored
                            + " inserted, expression now " + readable(expression.toString()));
                    host.invalidate();
                }
            });
        }
        TextView menu = ui.pillButton("⋮", ui.palette().surface, ui.palette().muted);
        menu.setTextSize(12);
        LinearLayout.LayoutParams mnp = ui.lp(-2, 28);
        mnp.leftMargin = ui.dp(6);
        modes.addView(menu, mnp);
        menu.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { openMenu(context); }
        });
        TextView hint = ui.label("tap: cursor · hold: select/copy · swipe down: history",
                9, ui.palette().muted, false);
        hint.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        modes.addView(hint, new LinearLayout.LayoutParams(0, 30, 1f));

        // ---- the display area: a container that owns the swipe-down gesture itself ----
        SwipeBox display = new SwipeBox(context);
        display.setOrientation(LinearLayout.VERTICAL);
        root.addView(display, new LinearLayout.LayoutParams(-1, 0, 3f));
        display.listener = new SwipeBox.Listener() {
            @Override public void onSwipeDown() {
                historyOpen = !historyOpen;
                AppLog.event(context, "calc", (historyOpen ? "swipe down: history shown, "
                        + entryCount(context) + " entries" : "swipe up: history closed"));
                host.invalidate();
            }
        };

        if (historyOpen) {
            buildHistoryPage(context, ui, display);
        } else {
            buildDisplay(context, ui, display);
        }

        // ---- Google's chevron: the scientific panel grows over the basic pad ----
        if (!landscapeAtBuild) {
            LinearLayout chevronRow = ui.hbox();
            chevronRow.setGravity(Gravity.CENTER_VERTICAL);
            root.addView(chevronRow, ui.lp(-1, 30));
            TextView chevron = ui.pillButton(panelOpen ? "⌄" : "⌃",
                    ui.palette().surface, ui.palette().accent);
            chevron.setTextSize(12);
            chevron.setContentDescription(panelOpen ? "Close scientific keys"
                    : "Open scientific keys");
            chevronRow.addView(chevron, ui.lp(-2, 28));
            chevron.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    panelOpen = !panelOpen;
                    AppLog.event(context, "calc", panelOpen
                            ? "scientific panel expanded: 3 rows added above the pad"
                            : "scientific panel collapsed: back to the basic pad");
                    host.invalidate();
                }
            });
            chevronRow.addView(ui.label(panelOpen ? "scientific" : "",
                    9, ui.palette().muted, false), ui.lp(-2, -2));
        }

        // ---- the pad: filled, separated buttons, Google's order ----
        String[][] rows = landscapeAtBuild ? LANDSCAPE_ROWS
                : panelOpen ? combined(PANEL_ROWS, BASE_ROWS) : BASE_ROWS;
        for (String[] rowKeys : rows) {
            LinearLayout row = ui.hbox();
            for (String rawKey : rowKeys) {
                final String key = labelFor(rawKey);
                TextView button = keyButton(context, ui, key);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -1, 1f);
                // Separated buttons: a gap on every side, which is what makes each key read
                // as its own button instead of a glyph in a grid of nothing.
                params.leftMargin = ui.dp(5);
                params.rightMargin = ui.dp(5);
                params.topMargin = ui.dp(5);
                params.bottomMargin = ui.dp(5);
                row.addView(button, params);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        v.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP);
                        press(context, key);
                        host.invalidate();
                    }
                });
            }
            root.addView(row, new LinearLayout.LayoutParams(-1, 0, 1f));
        }
        return root;
    }

    private int padRowCount(boolean landscape) {
        if (landscape) return LANDSCAPE_ROWS.length;
        return BASE_ROWS.length + (panelOpen ? PANEL_ROWS.length : 0);
    }

    private static String[][] combined(String[][] a, String[][] b) {
        String[][] out = new String[a.length + b.length][];
        System.arraycopy(a, 0, out, 0, a.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
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

    /**
     * One filled stadium button. Four visual classes, because Google's pad is not one colour
     * of key: digits sit on the card fill, operators on a lighter tinted fill, AC and the
     * active toggles on a saturated tint, equals on the accent itself. Bare glyphs on the
     * background -- what this screen had until now -- read as text, not as buttons.
     */
    private TextView keyButton(Context context, UiKit ui, String key) {
        int kind = kindOf(key);
        boolean toggleOn = ("Deg".equals(key) && !degrees) || ("Inv".equals(key) && invMode);
        int fill;
        int ink;
        float size;
        switch (kind) {
            case EQ:
                fill = ui.palette().accent;
                ink = android.graphics.Color.WHITE;
                size = 24;
                break;
            case CMD:
                fill = mix(ui.palette().surface, ui.palette().accent, 0.42f);
                ink = android.graphics.Color.WHITE;
                size = 20;
                break;
            case TOG:
                fill = toggleOn ? mix(ui.palette().surface, ui.palette().accent, 0.42f)
                        : mix(ui.palette().surface, ui.palette().accent, 0.16f);
                ink = toggleOn ? android.graphics.Color.WHITE : ui.palette().accent;
                size = 15;
                break;
            case OP:
                fill = mix(ui.palette().surface, ui.palette().accent, 0.16f);
                ink = ui.palette().accent;
                size = isScientificCap(key) ? 18 : "( )".equals(key) || "⌫".equals(key) ? 20 : 24;
                break;
            default:
                fill = ui.palette().surface;
                ink = ui.palette().text;
                size = isScientificCap(key) ? 18 : 28;
                break;
        }
        TextView button = new TextView(context);
        button.setText(key);
        button.setGravity(Gravity.CENTER);
        button.setSingleLine(true);
        button.setTextSize(size);
        button.setTextColor(ink);
        GradientDrawable shape = new GradientDrawable();
        // A stadium: the radius clamps to half the height, which is Google's expanded-key
        // silhouette and reads as a button at any cell proportion.
        shape.setCornerRadius(ui.dp(200));
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

    /**
     * The live display: selectable text with a caret, in a filling scroller.
     *
     * <p>Selectable is the whole point: the owner's third complaint was that the display
     * would not even let him select what he wanted. The framework gives a selectable
     * TextView the caret, the drag handles and the copy menu for free, and every key
     * inserts at the caret, so the typed equation is editable exactly like his second
     * complaint asked. The caret is restored after every rebuild, because the views are
     * rebuilt on each invalidate but the position is the user's, not ours.
     */
    private void buildDisplay(Context context, UiKit ui, LinearLayout display) {
        final HorizontalScrollView exprScroll = new HorizontalScrollView(context);
        exprScroll.setHorizontalScrollBarEnabled(false);
        exprScroll.setFillViewport(true);
        exprScroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        // Long numbers shrink instead of scrolling away: three sizes, chosen by length,
        // so a twelve-digit result is still legible and a forty-character expression fits.
        int shown = typed().length();
        int size = shown > 20 ? 26 : shown > 12 ? 34 : 46;
        final TextView expressionView = ui.label(typed(), size, ui.palette().text, false);
        expressionView.setGravity(Gravity.RIGHT | Gravity.BOTTOM);
        expressionView.setSingleLine(true);
        expressionView.setTextIsSelectable(true);
        expressionView.setFocusable(true);
        expressionView.setFocusableInTouchMode(true);
        expressionView.setHighlightColor(mix(ui.palette().surface, ui.palette().accent, 0.5f));
        displayView = expressionView;
        restoreCaret(expressionView);
        exprScroll.addView(expressionView, new LinearLayout.LayoutParams(-2, -1));
        LinearLayout.LayoutParams sp = ui.lp(-1, 0);
        sp.weight = 1f;
        display.addView(exprScroll, sp);
        exprScroll.post(new Runnable() {
            @Override public void run() { exprScroll.fullScroll(View.FOCUS_RIGHT); }
        });

        final TextView previewView = ui.label(preview(), 17, ui.palette().muted, false);
        previewView.setGravity(Gravity.RIGHT | Gravity.TOP);
        previewView.setMaxLines(1);
        previewView.setEllipsize(android.text.TextUtils.TruncateAt.START);
        display.addView(previewView, ui.lp(-1, -2));
    }

    private void restoreCaret(TextView view) {
        if (justEvaluated) return;
        CharSequence cs = view.getText();
        if (cs instanceof Spannable) {
            int pos = Math.max(0, Math.min(caret, cs.length()));
            Selection.setSelection((Spannable) cs, pos, pos);
        }
    }

    /** Google's history: newest first, tap a line to reuse its result, clear when you want. */
    private void buildHistoryPage(final Context context, UiKit ui, LinearLayout display) {
        LinearLayout head = ui.hbox();
        head.setGravity(Gravity.CENTER_VERTICAL);
        display.addView(head, ui.lp(-1, 34));
        final int entries = entryCount(context);
        head.addView(ui.label("History" + (entries > 0 ? " · " + entries : ""),
                14, ui.palette().text, true), new LinearLayout.LayoutParams(0, -2, 1f));
        // The owner's fifth point, verbatim: when history is shown there is a button to
        // clear it. It counts what it is about to delete, in the log and on the toast.
        TextView clear = ui.pillButton("Clear", ui.palette().surface, ui.palette().text);
        head.addView(clear, ui.lp(-2, 32));
        clear.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                AppPrefs.prefs(context).edit().remove(HISTORY_KEY).apply();
                AppLog.event(context, "calc", "history cleared: " + entries + " entries removed");
                host.toast("History cleared");
                host.invalidate();
            }
        });
        TextView close = ui.pillButton("⌃", ui.palette().surface, ui.palette().accent);
        LinearLayout.LayoutParams cp = ui.lp(-2, 32);
        cp.leftMargin = ui.dp(6);
        head.addView(close, cp);
        close.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                historyOpen = false;
                AppLog.event(context, "calc", "history closed by button");
                host.invalidate();
            }
        });

        ScrollView scroll = new ScrollView(context);
        scroll.setVerticalScrollBarEnabled(false);
        LinearLayout list = ui.vbox();
        scroll.addView(list, new LinearLayout.LayoutParams(-1, -2));
        display.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));
        String[][] rows = CalculatorRules.historyEntries(
                AppPrefs.prefs(context).getString(HISTORY_KEY, ""));
        if (rows.length == 0) {
            list.addView(ui.label("No calculations yet. Results land here as you make them.",
                    13, ui.palette().muted, false), ui.lp(-1, -2));
        }
        for (String[] entry : rows) {
            final String value = entry[1];
            LinearLayout line = ui.hbox();
            line.setGravity(Gravity.CENTER_VERTICAL);
            line.setPadding(0, ui.dp(5), 0, ui.dp(5));
            list.addView(line, ui.lp(-1, -2));
            line.addView(ui.label(entry[0] + "  =  ", 14, ui.palette().muted, false),
                    ui.lp(-2, -2));
            line.addView(ui.label(value, 16, ui.palette().text, true),
                    new LinearLayout.LayoutParams(0, -2, 1f));
            line.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    insertNumber(context, value);
                    historyOpen = false;
                    AppLog.event(context, "calc", "history line reused: " + value
                            + ", expression now " + readable(expression.toString()));
                    host.invalidate();
                }
            });
            line.setOnLongClickListener(new View.OnLongClickListener() {
                @Override public boolean onLongClick(View v) {
                    copy(context, value);
                    return true;
                }
            });
        }
    }

    private static int entryCount(Context context) {
        return CalculatorRules.historyEntries(
                AppPrefs.prefs(context).getString(HISTORY_KEY, "")).length;
    }

    /**
     * A container that claims the vertical swipe itself, in {@code onInterceptTouchEvent},
     * because a swipe listener on a parent whose children consume every touch never fires.
     * That is precisely why the owner's phone never opened history on a swipe down: the old
     * listener sat on the display box while the scroller inside it ate the gesture.
     * Intercepting mid-gesture cancels the child, which is what claiming a gesture means.
     */
    static final class SwipeBox extends LinearLayout {
        interface Listener { void onSwipeDown(); }
        Listener listener;
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
                    // Unmistakably downward and more vertical than a selection drag, or the
                    // gesture that selects text would also flip the history open.
                    if (!claimed && dy > 60 && dy > Math.abs(dx) * 2) {
                        claimed = true;
                        return true;
                    }
                    return claimed;
                default:
                    return false;
            }
        }

        @Override public boolean onTouchEvent(MotionEvent e) {
            if (claimed) {
                if (e.getAction() == MotionEvent.ACTION_UP || e.getAction() == MotionEvent.ACTION_CANCEL) {
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

    /** The overflow menu: history, the fraction view, and the memory slot's store and clear. */
    private void openMenu(final Context context) {
        final boolean memorySet = AppPrefs.prefs(context).contains(MEMORY_KEY);
        java.util.List<String> items = new java.util.ArrayList<>();
        items.add("History");
        items.add(showFraction ? "Show as decimal" : "Show as fraction");
        items.add(memorySet ? "Store again in memory" : "Store in memory");
        if (memorySet) items.add("Clear memory");
        final String[] labels = items.toArray(new String[0]);
        new android.app.AlertDialog.Builder(context)
                .setTitle(displayValue())
                .setItems(labels, new android.content.DialogInterface.OnClickListener() {
                    @Override public void onClick(android.content.DialogInterface d, int which) {
                        String choice = labels[which];
                        if ("History".equals(choice)) {
                            historyOpen = !historyOpen;
                            AppLog.event(context, "calc", (historyOpen ? "history opened from menu, "
                                    + entryCount(context) + " entries" : "history closed from menu"));
                        } else if (choice.startsWith("Show as")) {
                            showFraction = !showFraction;
                            AppLog.event(context, "calc", showFraction
                                    ? "result view: fraction" : "result view: decimal");
                        } else if (choice.startsWith("Store")) {
                            AppPrefs.prefs(context).edit()
                                    .putString(MEMORY_KEY, displayValue()).apply();
                            AppLog.event(context, "calc", "memory stored: " + displayValue());
                            host.toast("Stored in memory");
                        } else {
                            AppPrefs.prefs(context).edit().remove(MEMORY_KEY).apply();
                            AppLog.event(context, "calc", "memory cleared");
                            host.toast("Memory cleared");
                        }
                        host.invalidate();
                    }
                })
                .show();
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
    private void insertNumber(Context context, String value) {
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

    /** The selection the framework is holding in the display, or the caret we restored. */
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
     * The whole edited expression replayed through the rules, token by token. Appending only
     * ever validates the join at the end; an insertion in the middle makes two joins, and the
     * one after the caret is exactly the one appending can never see. Replaying the finished
     * string left to right checks every join there is, so an insertion that would break the
     * grammar is refused with the same discipline as a key that would.
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

    /** The cap a backspace deletes: a whole function token when one ends at the caret,
     *  because deleting half of "sin(" leaves "sin", which is not mathematics. */
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
            return;
        }
        if ("Deg".equals(key)) {
            degrees = !degrees;
            AppLog.event(context, "calc", "angle mode → " + (degrees ? "DEG" : "RAD"));
            return;
        }
        if ("Inv".equals(key)) {
            invMode = !invMode;
            AppLog.event(context, "calc", invMode
                    ? "inverse on: sin cos tan show their inverses"
                    : "inverse off: sin cos tan show themselves");
            return;
        }
        if ("( )".equals(key)) {
            // Google's single bracket key chooses for you: open where a group may open,
            // close where one is open and closable. Both choices go through the same whole
            // expression validation as any insertion.
            if (insertAtCaret(context, "(")) return;
            if (insertAtCaret(context, ")")) return;
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
                return;
            }
            if (sel[0] != sel[1]) {
                String next = expression.substring(0, sel[0]) + expression.substring(sel[1]);
                expression.setLength(0);
                expression.append(next);
                caret = sel[0];
                AppLog.event(context, "calc", "key ⌫: deleted selection, now "
                        + readable(expression.toString()));
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
            // After equals, a digit or a point starts a new expression while an operator or
            // a percent carries on from the result; after Error, everything starts over.
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
        String candidate = raw.substring(0, at) + (sel[1] > sel[0] ? "" : rawKey)
                + (sel[1] > sel[0] ? rawKey : "") + raw.substring(sel[1]);
        if (sel[1] > sel[0]) {
            candidate = raw.substring(0, at) + rawKey + raw.substring(sel[1]);
        }
        if (!wholeExpressionValid(candidate)) {
            char before = at == 0 ? ' ' : raw.charAt(at - 1);
            AppLog.event(context, "calc", "key " + key + " refused at " + at
                    + " (after '" + before + "'): the expression would not evaluate");
            return false;
        }
        expression.setLength(0);
        expression.append(candidate);
        caret = at + rawKey.length();
        AppLog.event(context, "calc", "key " + key + " at " + at + " → "
                + readable(expression.toString()));
        return true;
    }

    /** The display glyphs are typographic; the parser takes ASCII operators. */
    private static String readable(String raw) {
        return raw.replace('*', '×').replace('/', '÷').replace('-', '−');
    }
}
