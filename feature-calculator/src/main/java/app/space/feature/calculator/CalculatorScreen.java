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
        if ("C".equals(key)) {
            expression.setLength(0);
            result = null;
            return;
        }
        if ("=".equals(key)) {
            // Equals evaluates what is typed; a trailing operator is dropped first, the way a
            // pocket calculator reads 5 + = as 5.
            String typed = expression.toString();
            while (!typed.isEmpty() && CalculatorRules.isOperator(typed.charAt(typed.length() - 1))) {
                typed = typed.substring(0, typed.length() - 1);
            }
            if (typed.isEmpty()) return;
            try {
                result = CalculatorRules.format(CalculatorRules.evaluate(typed));
                expression.setLength(0);
                expression.append(result);
            } catch (Exception e) {
                result = "Error";
            }
            return;
        }
        // The rules decide, not the keyboard: an unacceptable key is simply not typed, so the
        // display can never hold a sequence that is not a number in progress.
        String next = CalculatorRules.append(expression.toString(), key);
        if (next == null) return;
        expression.setLength(0);
        expression.append(next);
        result = null;
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
