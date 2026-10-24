package app.space.feature.calculator;

import android.content.Context;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import app.space.core.data.AppPrefs;
import app.space.core.design.UiKit;
import app.space.core.suite.Screen;
import app.space.core.suite.ScreenHost;

/**
 * The calculator, recreated after Google's and then compared against it feature by feature
 * on the owner's instruction: "List all features available in Google calculator and compare
 * with yours in each aspects and pick the findings and work on them."
 *
 * <p>What the comparison found and this revision closes: Google's scientific panel (sin cos
 * tan and their inverses, ln, log, sqrt, factorial, power, pi and e, with a DEG/RAD toggle)
 * behind a page swap the way Google's portrait swipe does it; Google's history (swipe down on
 * the display, tap a line to reuse its result, clear it when you want) persisted in prefs so
 * it survives like Google's does; Google's hold-to-copy on the display; Google's swipeable
 * display for numbers longer than the screen (a filling HorizontalScrollView that scrolls to
 * the end you are typing); and Google's key haptics.
 *
 * <p>What it keeps from the 3.11.1 rebuild: Google's basic pad key for key -- display on top
 * with the live result in grey beneath, five rows of four, double-width filled equals, no
 * bracket key on the basic page -- and Google's percent, which is postfix and contextual.
 *
 * <p>The layout rule this screen already died once for: keys take their height from the row
 * (MATCH_PARENT inside a weighted row). The first version gave keys height zero inside a
 * horizontal row, where weight governs width only, and shipped an invisible keypad to a real
 * phone. The contract pins the params line.
 *
 * <p>State lives in this object rather than in the views, so the expression survives a
 * re-render. Results are formatted by {@link CalculatorRules#format} and errors show as
 * "Error" instead of crashing.
 */
public final class CalculatorScreen implements Screen {

    /** The basic page, Google's arrangement. A null cell means the key to its left spans it. */
    private static final String[][] ROWS = {
            {"C", "\u232b", "%", "\u00f7"},
            {"7", "8", "9", "\u00d7"},
            {"4", "5", "6", "\u2212"},
            {"1", "2", "3", "+"},
            {"0", ".", "=", null},
    };

    /** The scientific page, the way Google's portrait panel swaps the whole keypad over. */
    private static final String[][] SCIENTIFIC_ROWS = {
            {"sin(", "cos(", "tan(", "\u221a("},
            {"asin(", "acos(", "atan(", "ln("},
            {"log(", "^", "!", "\u03c0"},
            {"e", "(", ")", "C"},
            {"DEG", "\u232b", "=", null},
    };

    private static final String HISTORY_KEY = "calculator_history";
    private static final int HISTORY_CAP = 50;

    private final ScreenHost host;
    private final StringBuilder expression = new StringBuilder();
    private String result = "";
    private boolean justEvaluated;
    private boolean historyOpen;
    private boolean scientificOpen;
    private boolean degrees = true;

    public CalculatorScreen(ScreenHost host) {
        this.host = host;
    }

    @Override public String title() { return "Calculator"; }

    @Override
    public View build(final Context context) {
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
                    }
                }
            }
        });

        // ---- mode row first, small and quiet at the top where it reads as chrome, not as
        //      part of the calculator; Google shows nothing here and the pad must own the eye ----
        LinearLayout modes = ui.hbox();
        modes.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(modes, ui.lp(-1, 30));
        TextView page = ui.pillButton(scientificOpen ? "Basic" : "Scientific",
                ui.palette().surface, ui.palette().muted);
        page.setTextSize(10);
        modes.addView(page, ui.lp(-2, 28));
        page.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { scientificOpen = !scientificOpen; host.invalidate(); }
        });
        TextView angle = ui.pillButton(degrees ? "DEG" : "RAD",
                ui.palette().surface, ui.palette().muted);
        angle.setTextSize(10);
        LinearLayout.LayoutParams apm = ui.lp(-2, 28);
        apm.leftMargin = ui.dp(6);
        modes.addView(angle, apm);
        angle.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { degrees = !degrees; host.invalidate(); }
        });
        TextView hint = ui.label("hold display: copy \u00b7 swipe down: history",
                9, ui.palette().muted, false);
        hint.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        modes.addView(hint, new LinearLayout.LayoutParams(0, 30, 1f));

        // ---- the display: swipe down for history, hold to copy, scroll when long ----
        LinearLayout display = ui.vbox();
        root.addView(display, new LinearLayout.LayoutParams(-1, 0, 3f));
        display.setOnTouchListener(new SwipeDownListener() {
            @Override void onSwipeDown() { historyOpen = !historyOpen; host.invalidate(); }
        });
        display.setOnLongClickListener(new View.OnLongClickListener() {
            @Override public boolean onLongClick(View v) {
                copy(context, displayValue());
                return true;
            }
        });

        if (historyOpen) {
            buildHistoryPage(context, ui, display);
        } else {
            buildDisplay(context, ui, display);
        }

        // ---- the keypad: basic page or scientific page, rows weighted, keys row-high ----
        String[][] rows = scientificOpen ? SCIENTIFIC_ROWS : ROWS;
        for (String[] rowKeys : rows) {
            LinearLayout row = ui.hbox();
            for (int col = 0; col < rowKeys.length; col++) {
                final String key = rowKeys[col];
                if (key == null) continue; // spanned by the key to its left
                if ("DEG".equals(key)) {
                    TextView toggle = ui.pillButton(degrees ? "DEG" : "RAD",
                            ui.palette().surface, ui.palette().accent);
                    toggle.setGravity(Gravity.CENTER);
                    LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(0, -1, 1f);
                    tp.leftMargin = ui.dp(4);
                    tp.rightMargin = ui.dp(4);
                    tp.topMargin = ui.dp(4);
                    tp.bottomMargin = ui.dp(4);
                    row.addView(toggle, tp);
                    toggle.setOnClickListener(new View.OnClickListener() {
                        @Override public void onClick(View v) { degrees = !degrees; host.invalidate(); }
                    });
                    continue;
                }
                final boolean equalsKey = "=".equals(key);
                final boolean fnKey = "C".equals(key) || "\u232b".equals(key) || "%".equals(key);
                final boolean opKey = "\u00f7".equals(key) || "\u00d7".equals(key)
                        || "\u2212".equals(key) || "+".equals(key);
                final boolean sciKey = key.endsWith("(") || "^".equals(key) || "!".equals(key)
                        || "\u03c0".equals(key) || "e".equals(key) || "(".equals(key) || ")".equals(key);
                TextView button = new TextView(context);
                button.setText(key);
                button.setGravity(Gravity.CENTER);
                button.setSingleLine(true);
                if (equalsKey) {
                    button.setTextSize(24);
                    button.setTextColor(android.graphics.Color.WHITE);
                    android.graphics.drawable.GradientDrawable pill =
                            new android.graphics.drawable.GradientDrawable();
                    pill.setCornerRadius(ui.dp(200)); // clamps to a stadium, Google's equals shape
                    pill.setColor(ui.palette().accent);
                    button.setBackground(pill);
                } else {
                    button.setTextSize(sciKey ? 20 : fnKey || opKey ? 26 : 32);
                    button.setTextColor(fnKey || opKey || sciKey
                            ? ui.palette().accent : ui.palette().text);
                }
                button.setContentDescription(key);
                // Google's keys answer the finger: the borderless ripple is the platform's
                // own touch feedback, drawn over the bare glyph without adding chrome.
                try {
                    android.util.TypedValue tv = new android.util.TypedValue();
                    context.getTheme().resolveAttribute(
                            android.R.attr.selectableItemBackgroundBorderless, tv, true);
                    button.setForeground(context.getDrawable(tv.resourceId));
                } catch (Exception ignored) {
                }
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -1, 1f);
                if (equalsKey) params.weight = 2f; // equals spans the last two cells
                params.leftMargin = ui.dp(4);
                params.rightMargin = ui.dp(4);
                params.topMargin = ui.dp(4);
                params.bottomMargin = ui.dp(4);
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

    /** The live display: typed expression in a filling scroller, live result beneath in grey. */
    private void buildDisplay(Context context, UiKit ui, LinearLayout display) {
        final HorizontalScrollView exprScroll = new HorizontalScrollView(context);
        exprScroll.setHorizontalScrollBarEnabled(false);
        exprScroll.setFillViewport(true);
        exprScroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        final TextView expressionView = ui.label(typed(), 46, ui.palette().text, false);
        expressionView.setGravity(Gravity.RIGHT | Gravity.BOTTOM);
        expressionView.setSingleLine(true);
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

    /** Google's history: newest first, tap a line to reuse its result, clear when you want. */
    private void buildHistoryPage(final Context context, UiKit ui, LinearLayout display) {
        LinearLayout head = ui.hbox();
        head.setGravity(Gravity.CENTER_VERTICAL);
        display.addView(head, ui.lp(-1, 34));
        head.addView(ui.label("History", 14, ui.palette().text, true),
                new LinearLayout.LayoutParams(0, -2, 1f));
        TextView clear = ui.pillButton("Clear", ui.palette().surface, ui.palette().text);
        head.addView(clear, ui.lp(-2, 32));
        clear.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                AppPrefs.prefs(context).edit().remove(HISTORY_KEY).apply();
                host.invalidate();
            }
        });
        TextView close = ui.pillButton("\u2303", ui.palette().surface, ui.palette().accent);
        LinearLayout.LayoutParams cp = ui.lp(-2, 32);
        cp.leftMargin = ui.dp(6);
        head.addView(close, cp);
        close.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { historyOpen = false; host.invalidate(); }
        });

        ScrollView scroll = new ScrollView(context);
        scroll.setVerticalScrollBarEnabled(false);
        LinearLayout list = ui.vbox();
        scroll.addView(list, new LinearLayout.LayoutParams(-1, -2));
        display.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));
        String[][] entries = CalculatorRules.historyEntries(
                AppPrefs.prefs(context).getString(HISTORY_KEY, ""));
        if (entries.length == 0) {
            list.addView(ui.label("No calculations yet. Results land here as you make them.",
                    13, ui.palette().muted, false), ui.lp(-1, -2));
        }
        for (String[] entry : entries) {
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
                    insertNumber(value);
                    historyOpen = false;
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

    /** A vertical swipe on the display, which is how Google opens and closes the history. */
    private abstract static class SwipeDownListener implements View.OnTouchListener {
        private float downY;
        abstract void onSwipeDown();
        // DOWN must return false: consuming it here would stop the view's own touch
        // handling, and the hold-to-copy long click is exactly that handling. The swipe
        // is claimed only on UP, and only when it was unmistakably downward.
        @Override public boolean onTouch(View v, MotionEvent e) {
            if (e.getAction() == MotionEvent.ACTION_DOWN) { downY = e.getY(); return false; }
            if (e.getAction() == MotionEvent.ACTION_UP && e.getY() - downY > 60) {
                onSwipeDown();
                return true;
            }
            return false;
        }
    }

    @Override public void onResume() { }

    @Override public void onPause() { }

    /** What a hold-copy puts on the clipboard: the result if there is one, else what is typed. */
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
            host.toast("Copied " + text);
        } catch (Exception e) {
            host.toast("Could not copy.");
        }
    }

    /** History reuse: the stored result is typed in one character at a time, so the rules
     *  validate it exactly as if your thumb had typed it. */
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
    }

    /** The main display: the result just after equals, otherwise what is typed. */
    private String typed() {
        if (justEvaluated) return result.isEmpty() ? "0" : result;
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

    private void press(Context context, String key) {
        if ("C".equals(key)) {
            expression.setLength(0);
            result = "";
            justEvaluated = false;
            return;
        }
        if ("\u232b".equals(key)) {
            // After equals, backspace edits the result like any other typed text.
            if (justEvaluated) {
                expression.setLength(0);
                if (!"Error".equals(result)) expression.append(result);
                justEvaluated = false;
            }
            if (expression.length() > 0) expression.setLength(expression.length() - 1);
            return;
        }
        // After equals, a digit or a point starts a new expression while an operator or a
        // percent carries on from the result; after Error, everything starts over.
        if (justEvaluated) {
            boolean fresh;
            if ("Error".equals(result)) {
                fresh = true;
            } else {
                fresh = key.length() == 1
                        && (Character.isDigit(key.charAt(0)) || key.charAt(0) == '.');
            }
            if (fresh) { expression.setLength(0); result = ""; }
            justEvaluated = false;
        }
        if ("=".equals(key)) {
            String typed = trimmed();
            if (typed.isEmpty()) return;
            String evaluated;
            try {
                evaluated = CalculatorRules.format(CalculatorRules.evaluate(typed, degrees));
            } catch (Exception e) {
                result = "Error";
                justEvaluated = true;
                return;
            }
            AppPrefs.prefs(context).edit().putString(HISTORY_KEY,
                    CalculatorRules.pushHistory(
                            AppPrefs.prefs(context).getString(HISTORY_KEY, ""),
                            readable(typed), evaluated, HISTORY_CAP)).apply();
            result = evaluated;
            expression.setLength(0);
            expression.append(result);
            justEvaluated = true;
            return;
        }
        // The rules decide, not the keyboard: an unacceptable key is simply not typed, so the
        // display can never hold a sequence that is not a number in progress.
        String next = CalculatorRules.appendToken(expression.toString(), key);
        if (next == null) return;
        accept(next);
    }

    private void accept(String next) {
        expression.setLength(0);
        expression.append(next);
        result = "";
        justEvaluated = false;
    }

    /** The display glyphs are typographic; the parser takes ASCII operators. */
    private static String readable(String raw) {
        return raw.replace('*', '\u00d7').replace('/', '\u00f7').replace('-', '\u2212');
    }
}
