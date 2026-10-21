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
 * A four-by-four keypad and a display.
 *
 * <p>State lives in this object rather than in the views, so the expression survives a
 * re-render. Results are formatted by {@link CalculatorRules#format} and errors show as
 * "Error" instead of crashing: a malformed expression is what a user types by accident,
 * not an exceptional condition.
 */
public final class CalculatorScreen implements Screen {

    private static final String[][] ROWS = {
            {"7", "8", "9", "\u00f7"},
            {"4", "5", "6", "\u00d7"},
            {"1", "2", "3", "\u2212"},
            {"C", "0", "=", "+"},
    };

    private final ScreenHost host;
    private final StringBuilder expression = new StringBuilder();
    private String result = "0";
    private boolean justEvaluated;

    public CalculatorScreen(ScreenHost host) {
        this.host = host;
    }

    @Override public String title() { return "Calculator"; }

    @Override
    public View build(Context context) {
        final UiKit ui = new UiKit(context, host.palette());
        LinearLayout root = ui.vbox();
        root.setPadding(ui.dp(12), ui.dp(8), ui.dp(12), ui.dp(12));

        final TextView resultView = ui.label(result, 40, ui.palette().text, true);
        resultView.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        resultView.setMaxLines(1);
        root.addView(resultView, ui.lp(-1, 92));

        final TextView expressionView = ui.label(typed(), 13, ui.palette().muted, false);
        expressionView.setGravity(Gravity.RIGHT);
        expressionView.setMaxLines(1);
        root.addView(expressionView, ui.lp(-1, 26));

        for (String[] rowKeys : ROWS) {
            LinearLayout row = ui.hbox();
            for (String key : rowKeys) {
                boolean accentKey = key.equals("=");
                TextView button = ui.pillButton(key,
                        accentKey ? ui.palette().accent : ui.palette().surface,
                        accentKey ? android.graphics.Color.WHITE : ui.palette().text);
                LinearLayout.LayoutParams params = ui.lp(0, 58);
                params.weight = 1f;
                params.leftMargin = ui.dp(3);
                params.rightMargin = ui.dp(3);
                row.addView(button, params);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        press(key);
                        resultView.setText(result);
                        expressionView.setText(typed());
                    }
                });
            }
            LinearLayout.LayoutParams rowParams = ui.lp(-1, -2);
            rowParams.topMargin = ui.dp(6);
            root.addView(row, rowParams);
        }
        return root;
    }

    @Override public void onResume() { }

    @Override public void onPause() { }

    /** The typed expression, shown above the result so the user can see what evaluated. */
    private String typed() {
        return expression.length() == 0 ? " " : readable(expression.toString());
    }

    private void press(String key) {
        if (key.equals("C")) {
            expression.setLength(0);
            result = "0";
            justEvaluated = false;
            return;
        }
        if (key.equals("=")) {
            if (expression.length() == 0) return;
            try {
                double value = CalculatorRules.evaluate(expression.toString());
                result = CalculatorRules.format(value);
                expression.setLength(0);
                expression.append(result);
                justEvaluated = true;
            } catch (ArithmeticException e) {
                result = "Error";
                expression.setLength(0);
                justEvaluated = true;
            }
            return;
        }
        // After a result, a digit starts a new calculation but an operator continues it.
        if (justEvaluated && !isOperator(key)) {
            expression.setLength(0);
            result = "0";
        }
        justEvaluated = false;
        if (expression.length() >= CalculatorRules.MAX_LENGTH) return;
        char c = asciiForKey(key);
        if (c == 0 || !CalculatorRules.isInputCharacter(c)) return;
        expression.append(c);
    }

    private static boolean isOperator(String key) {
        return key.equals("+") || key.equals("\u2212") || key.equals("\u00d7") || key.equals("\u00f7");
    }

    /**
     * The keypad shows typographic operators but the parser takes ASCII, so the glyph must
     * be translated before it is appended. Appending the glyph itself would make every
     * operator key silently do nothing, because isInputCharacter rejects it.
     */
    private static char asciiForKey(String key) {
        if (key.equals("\u00f7")) return '/';
        if (key.equals("\u00d7")) return '*';
        if (key.equals("\u2212")) return '-';
        return key.length() == 1 ? key.charAt(0) : 0;
    }

    /** The display glyphs are typographic; the parser takes ASCII operators. */
    private static String readable(String raw) {
        return raw.replace('*', '\u00d7').replace('/', '\u00f7').replace('-', '\u2212');
    }
}
