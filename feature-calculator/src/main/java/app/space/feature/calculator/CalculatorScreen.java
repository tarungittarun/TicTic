package app.space.feature.calculator;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import app.space.core.design.UiKit;
import app.space.core.suite.Screen;
import app.space.core.suite.ScreenHost;

/**
 * The calculator, recreated key for key after Google's, on the owner's instruction:
 * "Exact interface and working as Google calculator."
 *
 * <p>Google's arrangement: the display owns the top of the screen with what is typed large and
 * the live result beneath it in grey; the keypad owns everything below and sits against the
 * bottom edge. Five rows of four: clear, backspace, percent, divide; then three digit rows with
 * the operators down the right side; then zero, point and a double-width equals. There is no
 * bracket key on Google's pad, so there is none here -- the rules still understand brackets,
 * but the keyboard offers exactly what the calculator it recreates offers.
 *
 * <p>Google's look, in this suite's palette: digit keys are bare -- no button chrome at all,
 * just large light glyphs on the dark background; the function and operator keys are bare too
 * but coloured with the accent; only equals is a filled pill, spanning its two cells. Nothing
 * floats in the middle of the screen and nothing is outlined like a 2013 keypad.
 *
 * <p>The layout bug this rewrite kills: the previous keypad gave every key a height of zero
 * inside a horizontal row, where weight does not govern height -- so on a real phone the
 * calculator rendered as an empty screen with a stray glyph. Keys now take their height from
 * the row (MATCH_PARENT inside a weighted row) and the rows take theirs from weights, so the
 * pad fills whatever the display does not take, on any screen. The owner's screenshot was the
 * first time anyone's eyes had been on this screen; it is why "delivered" and "works" are
 * different words.
 *
 * <p>State lives in this object rather than in the views, so the expression survives a
 * re-render. Results are formatted by {@link CalculatorRules#format} and errors show as
 * "Error" instead of crashing: a malformed expression is what a user types by accident,
 * not an exceptional condition.
 */
public final class CalculatorScreen implements Screen {

    /**
     * The keypad, Google's arrangement. A null cell means the key to its left spans it, which
     * is how equals takes the last two cells of the bottom row.
     */
    private static final String[][] ROWS = {
            {"C", "\u232b", "%", "\u00f7"},
            {"7", "8", "9", "\u00d7"},
            {"4", "5", "6", "\u2212"},
            {"1", "2", "3", "+"},
            {"0", ".", "=", null},
    };

    private final ScreenHost host;
    private final StringBuilder expression = new StringBuilder();
    private String result = "";
    private boolean justEvaluated;

    public CalculatorScreen(ScreenHost host) {
        this.host = host;
    }

    @Override public String title() { return "Calculator"; }

    @Override
    public View build(Context context) {
        final UiKit ui = new UiKit(context, host.palette());
        LinearLayout root = ui.vbox();
        root.setPadding(ui.dp(16), ui.dp(10), ui.dp(16), ui.dp(10));

        // The display: what is typed, large and right-aligned like Google's, with the live
        // result beneath in grey. A calculator that stays silent until equals makes you
        // discover mistakes at the worst moment. Long expressions truncate at the START so
        // the end you are typing stays visible, the way Google's scroller does.
        LinearLayout display = ui.vbox();
        root.addView(display, new LinearLayout.LayoutParams(-1, 0, 2f));

        final TextView expressionView = ui.label(typed(), 40, ui.palette().text, false);
        expressionView.setGravity(Gravity.RIGHT | Gravity.BOTTOM);
        expressionView.setSingleLine(true);
        expressionView.setEllipsize(android.text.TextUtils.TruncateAt.START);
        LinearLayout.LayoutParams ep = ui.lp(-1, -2);
        ep.bottomMargin = ui.dp(6);
        display.addView(expressionView, ep);

        final TextView previewView = ui.label(preview(), 17, ui.palette().muted, false);
        previewView.setGravity(Gravity.RIGHT | Gravity.TOP);
        previewView.setMaxLines(1);
        previewView.setEllipsize(android.text.TextUtils.TruncateAt.START);
        display.addView(previewView, ui.lp(-1, -2));

        // The keypad: five rows sharing the remaining height equally. Each KEY fills its row's
        // height -- the zero-height params that made this screen invisible on device are gone.
        for (String[] rowKeys : ROWS) {
            LinearLayout row = ui.hbox();
            for (int col = 0; col < rowKeys.length; col++) {
                final String key = rowKeys[col];
                if (key == null) continue; // spanned by the key to its left
                final boolean equalsKey = "=".equals(key);
                final boolean fnKey = "C".equals(key) || "\u232b".equals(key) || "%".equals(key);
                final boolean opKey = "\u00f7".equals(key) || "\u00d7".equals(key)
                        || "\u2212".equals(key) || "+".equals(key);
                TextView button = new TextView(context);
                button.setText(key);
                button.setGravity(Gravity.CENTER);
                button.setSingleLine(true);
                if (equalsKey) {
                    button.setTextSize(24);
                    button.setTextColor(android.graphics.Color.WHITE);
                    android.graphics.drawable.GradientDrawable pill =
                            new android.graphics.drawable.GradientDrawable();
                    pill.setCornerRadius(ui.dp(30));
                    pill.setColor(ui.palette().accent);
                    button.setBackground(pill);
                } else {
                    button.setTextSize(fnKey || opKey ? 22 : 27);
                    button.setTextColor(fnKey || opKey
                            ? ui.palette().accent : ui.palette().text);
                }
                button.setContentDescription(key);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -1, 1f);
                if (equalsKey) params.weight = 2f; // equals spans the last two cells
                params.leftMargin = ui.dp(4);
                params.rightMargin = ui.dp(4);
                params.topMargin = ui.dp(4);
                params.bottomMargin = ui.dp(4);
                row.addView(button, params);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        press(key);
                        expressionView.setText(typed());
                        previewView.setText(preview());
                    }
                });
            }
            root.addView(row, new LinearLayout.LayoutParams(-1, 0, 1f));
        }
        return root;
    }

    @Override public void onResume() { }

    @Override public void onPause() { }

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
            return "= " + CalculatorRules.format(CalculatorRules.evaluate(candidate));
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

    private void press(String key) {
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
        // percent carries on from the result; after Error, everything starts over, because
        // there is no result to carry on from.
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
            try {
                result = CalculatorRules.format(CalculatorRules.evaluate(typed));
            } catch (Exception e) {
                result = "Error";
                justEvaluated = true;
                return;
            }
            expression.setLength(0);
            expression.append(result);
            justEvaluated = true;
            return;
        }
        // The rules decide, not the keyboard: an unacceptable key is simply not typed, so the
        // display can never hold a sequence that is not a number in progress. Percent included
        // -- it is postfix in the rules, exactly as it is on Google's pad.
        String next = CalculatorRules.append(expression.toString(), key);
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
