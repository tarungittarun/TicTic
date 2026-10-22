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
 * A calculator laid out the way Google's is: the display owns the top third, the keypad owns
 * the rest and sits against the bottom of the screen, never floating in the middle of it.
 *
 * <p>The previous layout stacked a fixed-height display and four fixed-height rows at the top
 * of the screen and left the lower half empty, which on a phone reads as a broken screen.
 * Heights here are weights, so the keypad fills whatever the display does not take, on any
 * screen size.
 *
 * <p>State lives in this object rather than in the views, so the expression survives a
 * re-render. Results are formatted by {@link CalculatorRules#format} and errors show as
 * "Error" instead of crashing: a malformed expression is what a user types by accident,
 * not an exceptional condition.
 */
public final class CalculatorScreen implements Screen {

    /**
     * The keypad, Google's arrangement: clear / group / backspace / divide across the top,
     * digits in three rows with the operators down the right side, and zero, point and a
     * double-width equals along the bottom. A null cell means the previous key spans it.
     */
    private static final String[][] ROWS = {
            {"C", "( )", "\u232b", "\u00f7"},
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
        root.setPadding(ui.dp(14), ui.dp(10), ui.dp(14), ui.dp(10));

        // The display: what is typed, large, and beneath it what it would evaluate to. Google
        // shows the live result while you type; a calculator that stays silent until equals
        // makes you discover mistakes at the worst moment.
        LinearLayout display = ui.vbox();
        root.addView(display, new LinearLayout.LayoutParams(-1, 0, 2f));

        final TextView expressionView = ui.label(typed(), 34, ui.palette().text, true);
        expressionView.setGravity(Gravity.RIGHT | Gravity.BOTTOM);
        expressionView.setMaxLines(2);
        LinearLayout.LayoutParams ep = ui.lp(-1, -2);
        ep.bottomMargin = ui.dp(4);
        display.addView(expressionView, ep);

        final TextView previewView = ui.label(preview(), 15, ui.palette().muted, false);
        previewView.setGravity(Gravity.RIGHT | Gravity.TOP);
        previewView.setMaxLines(1);
        display.addView(previewView, ui.lp(-1, -2));

        // The keypad: five rows sharing the remaining height equally, so the keys grow with the
        // screen instead of leaving dead space under them.
        for (String[] rowKeys : ROWS) {
            LinearLayout row = ui.hbox();
            for (int col = 0; col < rowKeys.length; col++) {
                final String key = rowKeys[col];
                if (key == null) continue; // spanned by the key to its left
                boolean accentKey = "=".equals(key);
                TextView button = ui.pillButton(key,
                        accentKey ? ui.palette().accent : ui.palette().surface,
                        accentKey ? android.graphics.Color.WHITE : ui.palette().text);
                if ("( )".equals(key) || "\u232b".equals(key)) {
                    button.setTextSize(18);
                }
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, 0, 1f);
                if (accentKey) params.weight = 2f; // equals spans the last two cells
                params.leftMargin = ui.dp(3);
                params.rightMargin = ui.dp(3);
                params.topMargin = ui.dp(3);
                params.bottomMargin = ui.dp(3);
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
        // After equals, a digit or an open bracket starts a new expression while an operator
        // carries on from the result; after Error, everything starts over, because there is no
        // result to carry on from.
        if (justEvaluated) {
            boolean fresh;
            if ("Error".equals(result)) {
                fresh = true;
            } else {
                fresh = "( )".equals(key)
                        || (key.length() == 1
                            && (Character.isDigit(key.charAt(0)) || key.charAt(0) == '.'
                                || key.charAt(0) == '('));
            }
            if (fresh) { expression.setLength(0); result = ""; }
            justEvaluated = false;
        }
        if ("( )".equals(key)) {
            // One key, both brackets: the rules say which one fits here, opening first.
            String next = CalculatorRules.append(expression.toString(), "(");
            if (next == null) next = CalculatorRules.append(expression.toString(), ")");
            if (next == null) return;
            accept(next);
            return;
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
        // display can never hold a sequence that is not a number in progress.
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
