package app.space.calc;

import android.content.Context;
import android.content.res.Configuration;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Collections;
import java.util.List;

import app.space.core.data.AppTheme;
import app.space.core.design.Palette;
import app.space.core.design.TicIconView;
import app.space.core.design.UiKit;
import app.space.core.suite.Screen;
import app.space.core.suite.ScreenHost;
import app.space.core.suite.SuiteApp;
import app.space.feature.calculator.CalculatorScreen;

/**
 * The standalone calculator: one screen, one purpose, MySpace's design language.
 *
 * <p>The owner's instruction (2026-10-25): build the calculator separately, make it perfect
 * as a single app, then integrate it into MySpace. So this activity is the smallest honest
 * host the {@link CalculatorScreen} can have -- the same header grammar as MySpace (glyph,
 * wordmark, caption), the same palette resolution its settings drive, and nothing else. The
 * screen brings everything Google's calculator has: the basic pad, the scientific page, the
 * DEG/RAD toggle, history behind a swipe, hold-to-copy, memory, haptics.
 *
 * <p>Point 4 of the instruction -- "be aware of screen and view of user" -- is answered in
 * three places: the screen fits whatever container it is given (portrait phones, tall phones,
 * split screen), landscape swaps the pad to the combined scientific grid the way Google's
 * does, and a hardware keyboard types into the calculator exactly like the pad does, because
 * a calculator that ignores a keyboard is a calculator that surprises people.
 *
 * <p>Implements {@link ScreenHost} in full so the screen cannot tell it is not inside
 * MySpace: the availability and navigation methods answer "no" truthfully, because in this
 * build there is nothing else to reach. When the screen returns home to MySpace, the host it
 * gets will answer them for real, and the screen will not need a line changed.
 */
public final class CalcActivity extends android.app.Activity implements ScreenHost {

    private CalculatorScreen screen;

    @Override
    protected void onCreate(android.os.Bundle saved) {
        super.onCreate(saved);
        applyChrome();
        render();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (screen != null) screen.onResume();
    }

    @Override
    protected void onPause() {
        if (screen != null) screen.onPause();
        super.onPause();
    }

    /** Rotation is handled in place: the screen's state lives in the screen, not the views. */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        applyChrome();
        render();
    }

    private void applyChrome() {
        Palette p = palette();
        getWindow().setStatusBarColor(p.background);
        getWindow().setNavigationBarColor(p.background);
        int flags = p.dark ? 0 : View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        if (!p.dark) flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        getWindow().getDecorView().setSystemUiVisibility(flags);
    }

    private void render() {
        UiKit ui = new UiKit(this, palette());
        LinearLayout root = ui.vbox();
        root.setBackgroundColor(palette().background);

        // MySpace's header grammar: glyph, wordmark, caption. Same stroke, same sizes, so
        // the day this screen moves home nothing about it looks foreign.
        LinearLayout header = ui.hbox();
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(ui.dp(16), ui.dp(10), ui.dp(16), ui.dp(6));
        header.addView(new TicIconView(this, TicIconView.CALCULATOR, palette().accent),
                ui.lp(34, 34));
        TextView word = ui.label("MyCalc", 20, palette().text, true);
        LinearLayout.LayoutParams wp = ui.lp(0, -2, 1f);
        wp.leftMargin = ui.dp(10);
        header.addView(word, wp);
        TextView cap = ui.label("STANDALONE", 10, palette().accent, true);
        cap.setLetterSpacing(.12f);
        header.addView(cap, ui.lp(-2, -2));
        root.addView(header, ui.lp(-1, -2));

        // One screen object for the life of the activity: its expression, history-open and
        // angle-mode state live in it, so a re-render never loses what you were typing.
        if (screen == null) screen = new CalculatorScreen(this);
        root.addView(screen.build(this), new LinearLayout.LayoutParams(-1, 0, 1f));
        setContentView(root);
    }

    // ---- ScreenHost: the contract, answered honestly by a one-app build ----

    @Override public Context context() { return this; }

    @Override
    public Palette palette() {
        return Palette.resolve(AppTheme.dark(this), AppTheme.accent(this));
    }

    @Override
    public void invalidate() {
        runOnUiThread(new Runnable() {
            @Override public void run() { render(); }
        });
    }

    @Override public boolean isAppAvailable(String id) { return false; }

    @Override public List<SuiteApp> availableApps() { return Collections.emptyList(); }

    @Override public boolean openApp(String id) { return false; }

    @Override public boolean openApp(String id, String route) { return false; }

    @Override public void openSettings() { }

    @Override public void openSettings(String groupId) { }

    @Override
    public void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override public void refreshSurfaces() { }

    // ---- hardware keyboard: Google's calculator types; so does this one ----

    @Override
    public boolean onKeyDown(int code, KeyEvent event) {
        if (screen == null) return super.onKeyDown(code, event);
        String key = null;
        if (code >= KeyEvent.KEYCODE_0 && code <= KeyEvent.KEYCODE_9) {
            key = String.valueOf(code - KeyEvent.KEYCODE_0);
        } else if (code >= KeyEvent.KEYCODE_NUMPAD_0 && code <= KeyEvent.KEYCODE_NUMPAD_9) {
            key = String.valueOf(code - KeyEvent.KEYCODE_NUMPAD_0);
        } else {
            switch (code) {
                case KeyEvent.KEYCODE_PERIOD:
                case KeyEvent.KEYCODE_NUMPAD_DOT: key = "."; break;
                case KeyEvent.KEYCODE_PLUS:
                case KeyEvent.KEYCODE_NUMPAD_ADD: key = "+"; break;
                case KeyEvent.KEYCODE_MINUS:
                case KeyEvent.KEYCODE_NUMPAD_SUBTRACT: key = "\u2212"; break;
                case KeyEvent.KEYCODE_STAR:
                case KeyEvent.KEYCODE_NUMPAD_MULTIPLY: key = "\u00d7"; break;
                case KeyEvent.KEYCODE_SLASH:
                case KeyEvent.KEYCODE_NUMPAD_DIVIDE: key = "\u00f7"; break;
                case KeyEvent.KEYCODE_PERCENT: key = "%"; break;
                case KeyEvent.KEYCODE_CARET: key = "^"; break;
                case KeyEvent.KEYCODE_LEFT_PAREN: key = "("; break;
                case KeyEvent.KEYCODE_RIGHT_PAREN: key = ")"; break;
                case KeyEvent.KEYCODE_EQUALS:
                case KeyEvent.KEYCODE_NUMPAD_EQUALS:
                case KeyEvent.KEYCODE_ENTER:
                    screen.typeKey("=");
                    invalidate();
                    return true;
                case KeyEvent.KEYCODE_DEL:
                case KeyEvent.KEYCODE_FORWARD_DEL:
                    screen.typeKey("\u232b");
                    invalidate();
                    return true;
                case KeyEvent.KEYCODE_ESCAPE:
                    screen.typeKey("C");
                    invalidate();
                    return true;
                default: break;
            }
        }
        if (key != null) {
            screen.typeKey(key);
            invalidate();
            return true;
        }
        return super.onKeyDown(code, event);
    }
}
