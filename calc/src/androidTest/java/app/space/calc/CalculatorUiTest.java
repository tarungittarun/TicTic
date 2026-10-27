package app.space.calc;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.longClick;
import static androidx.test.espresso.action.ViewActions.swipeDown;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasFocus;
import static androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static androidx.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.view.View;
import androidx.test.espresso.ViewAction;
import android.widget.EditText;

import androidx.test.espresso.action.CoordinatesProvider;
import androidx.test.espresso.action.GeneralClickAction;
import androidx.test.espresso.action.Press;
import androidx.test.espresso.action.Tap;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.hamcrest.Matcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * The device-level half of the owner's 2026-10-26 demand: "have end to end test of
 * application in terms of all aspects". These run on a real emulator in the Tests
 * workflow and walk the exact stories his log told: implicit multiplication through
 * the pad door, the changeable-mind operator, the history sheet opening and
 * the history popup opening on a downward swipe and closing on its own button, and a
 * caret that exists, is focused and is visible.
 */
@RunWith(AndroidJUnit4.class)
public class CalculatorUiTest {

    private static final int DISPLAY = app.space.feature.calculator.R.id.calc_display;
    private static final int HISTORY_SCROLL = app.space.feature.calculator.R.id.calc_history_scroll;
    private static final int HISTORY_BTN = app.space.feature.calculator.R.id.calc_history_btn;
    private static final int CHEVRON_ROW = app.space.feature.calculator.R.id.calc_chevron_row;
    private static final int HISTORY_CLOSE = app.space.feature.calculator.R.id.calc_history_close;
    private static final int MENU_BTN = app.space.feature.calculator.R.id.calc_menu_btn;
    private static final int MEMORY_LIST = app.space.feature.calculator.R.id.calc_memory_list;
    private static final int MEMORY_CLOSE = app.space.feature.calculator.R.id.calc_memory_close;

    @Rule public ActivityScenarioRule<CalcActivity> rule = new ActivityScenarioRule<>(CalcActivity.class);

    /** The app's diagnostics file is opt-in on a phone; in CI it is the only
     *  transcript that survives a run, so every story switches it on before its
     *  first gesture and a failure can be read like the owner's shared logs. */
    @org.junit.Before public void enableAppLogForCi() {
        app.space.core.data.AppLog.setEnabled(
                androidx.test.InstrumentationRegistry.getTargetContext(), true);
    }

    /** System.out never reaches `am instrument` stdout, but a FAILURE MESSAGE
     *  does -- the printer carries it into ui.log. So every story that fails
     *  re-throws its failure with the app's own transcript for that story
     *  stapled underneath: what the product saw, key by key, hold by hold,
     *  at the moment the assertion broke. */
    @org.junit.Rule public org.junit.rules.TestRule transcriptOnFailure =
            new org.junit.rules.TestRule() {
        @Override public org.junit.runners.model.Statement apply(
                final org.junit.runners.model.Statement base,
                org.junit.runner.Description description) {
            return new org.junit.runners.model.Statement() {
                @Override public void evaluate() throws Throwable {
                    try {
                        base.evaluate();
                    } catch (Throwable t) {
                        String tail = "";
                        try {
                            tail = app.space.core.data.AppLog.tail(
                                    androidx.test.InstrumentationRegistry
                                            .getTargetContext(), 900);
                        } catch (Throwable ignored) {
                            // the transcript must never mask the failure itself
                        }
                        // The binder parcel that carries the suite's results has
                        // a hard 1MB cap, and 36 espresso failures dragging their
                        // full view hierarchies blew it -- TransactionTooLarge
                        // killed the instrumentation before it could report. So
                        // the enriched message TRIMS the hierarchy dump: the
                        // assertion line plus the app's own transcript, small
                        // enough that the summary always survives.
                        String head = String.valueOf(t.getMessage());
                        int vd = head.indexOf("View Details:");
                        if (vd > 0) head = head.substring(0, vd)
                                + "[view hierarchy trimmed]\n";
                        if (head.length() > 900) head = head.substring(0, 900);
                        AssertionError enriched = new AssertionError(
                                head + "===APP TRANSCRIPT===\n" + tail, t);
                        enriched.setStackTrace(t.getStackTrace());
                        throw enriched;
                    }
                }
            };
        }
    };

    /** The runner's adb-side pulls came back empty on the farm twice, so the
     *  suite carries its own transcript out through instrumentation stdout,
     *  which always lands in ui.log: the app's own words about what actually
     *  happened, printed after the last story. */
    @org.junit.AfterClass public static void dumpAppTranscript() {
        android.content.Context ctx =
                androidx.test.InstrumentationRegistry.getTargetContext();
        System.out.println("===APP TRANSCRIPT TAIL===");
        System.out.println(app.space.core.data.AppLog.tail(ctx, 12000));
        System.out.println("===END APP TRANSCRIPT===");
    }

    /** A pad key by its label, never the display or the preview that may show the same text. */
    /** Round 19: the handles on screen are Android's own teardrops now, so the
     *  story finds them the way a thumb does -- down on the strip below the held
     *  end, a slide, a lift -- injected at WINDOW level so the scroll parent gets
     *  its chance to steal and refuses. A probe that lands off the handle reads
     *  as a tap and collapses the selection, so each attempt re-raises it with a
     *  real long press first, exactly as his thumb would. */
    private static ViewAction narrowRightHandleTo(final int offset) {
        return stockHandleDrag(false, offset);
    }

    private static ViewAction narrowLeftHandleTo(final int offset) {
        return stockHandleDrag(true, offset);
    }

    private static ViewAction stockHandleDrag(final boolean left, final int toOffset) {
        return new ViewAction() {
            @Override public Matcher<View> getConstraints() { return isDisplayed(); }
            @Override public String getDescription() {
                return "drag the " + (left ? "left" : "right")
                        + " stock handle to offset " + toOffset;
            }
            @Override public void perform(androidx.test.espresso.UiController ui, View view) {
                android.widget.EditText ed = (android.widget.EditText) view;
                float d = view.getResources().getDisplayMetrics().density;
                float lineBottom = ed.getBaseline() + ed.getLayout().getLineDescent(0);
                float[] ys = new float[]{20 * d, 12 * d, 28 * d, 6 * d, 36 * d, 44 * d};
                for (float y : ys) {
                    if (!ed.hasSelection()) holdCenter(ui, view);
                    if (!ed.hasSelection()) continue;
                    android.text.Layout lo = ed.getLayout();
                    float padL = ed.getCompoundPaddingLeft();
                    int anchor = left ? ed.getSelectionStart() : ed.getSelectionEnd();
                    float fromX = padL + lo.getPrimaryHorizontal(anchor);
                    float toX = padL + lo.getPrimaryHorizontal(toOffset);
                    int before = left ? ed.getSelectionStart() : ed.getSelectionEnd();
                    drag(ui, view, fromX, toX, lineBottom + y);
                    ui.loopMainThreadForAtLeast(200);
                    int after = left ? ed.getSelectionStart() : ed.getSelectionEnd();
                    if (after != before) return;
                }
                throw new RuntimeException("stock handle drag never moved the "
                        + (left ? "left" : "right") + " boundary");
            }
        };
    }

    /** One finger: down, hold past the long-press timeout, slide, lift. */
    private static ViewAction holdThenDragTo(final int toOffset) {
        return new ViewAction() {
            @Override public Matcher<View> getConstraints() { return isDisplayed(); }
            @Override public String getDescription() {
                return "hold then drag to offset " + toOffset;
            }
            @Override public void perform(androidx.test.espresso.UiController ui, View view) {
                android.widget.EditText ed = (android.widget.EditText) view;
                int[] loc = new int[2];
                view.getLocationInWindow(loc);
                // same round-34 fix as holdCenter: the hold that starts the
                // drag must begin on the text, wherever the text actually lies.
                float cx = textCenterX(view, loc);
                float cy = loc[1] + view.getHeight() / 2f;
                float toX = loc[0] + ed.getCompoundPaddingLeft()
                        + ed.getLayout().getPrimaryHorizontal(toOffset);
                long t = android.os.SystemClock.uptimeMillis();
                try {
                    android.view.MotionEvent dn = android.view.MotionEvent.obtain(
                            t, t, android.view.MotionEvent.ACTION_DOWN, cx, cy, 0);
                    ui.injectMotionEvent(dn);
                    dn.recycle();
                    ui.loopMainThreadForAtLeast(1300);
                    for (int i = 1; i <= 4; i++) {
                        android.view.MotionEvent m = android.view.MotionEvent.obtain(
                                t, t + 1300 + i * 50L,
                                android.view.MotionEvent.ACTION_MOVE,
                                cx + (toX - cx) * i / 4f, cy, 0);
                        ui.injectMotionEvent(m);
                        m.recycle();
                    }
                    android.view.MotionEvent up = android.view.MotionEvent.obtain(
                            t, t + 1600, android.view.MotionEvent.ACTION_UP, toX, cy, 0);
                    ui.injectMotionEvent(up);
                    up.recycle();
                } catch (Exception failed) {
                    throw new RuntimeException("hold-then-drag could not be injected", failed);
                }
                ui.loopMainThreadForAtLeast(250);
            }
        };
    }

    /** Force the app theme: 1 light, 2 dark, 0 follow system. */
    private void setTheme(int mode) {
        androidx.test.InstrumentationRegistry.getTargetContext().getSharedPreferences(
                app.space.core.data.AppPrefs.PREF, android.content.Context.MODE_PRIVATE)
                .edit().putInt("theme", mode).commit();
    }

    /** Window-injected long press, retried until a selection stands -- immune
     *  to espresso's longClick racing the screen's 350ms hold timer on a
     *  loaded runner. */
    private static ViewAction holdUntilSelected() {
        return new ViewAction() {
            @Override public Matcher<View> getConstraints() { return isDisplayed(); }
            @Override public String getDescription() {
                return "hold the display until a selection stands";
            }
            @Override public void perform(androidx.test.espresso.UiController ui, View view) {
                for (int i = 0; i < 3
                        && !((android.widget.EditText) view).hasSelection(); i++) {
                    holdCenter(ui, view);
                    ui.loopMainThreadForAtLeast(150);
                }
            }
        };
    }

    /** A real long press at the middle of the display, window-injected. */
    /** Round 34 forensics, from the app's own streamed transcript: the farm
     *  emulator shows the display 296px wide, and the right-aligned text sits
     *  at the far right -- so the VIEW centre is empty glass left of the text
     *  and every centre hold took the round-28 empty-area branch: caret parked,
     *  nothing held, held=0 across 34 stories. A hold meant to select must land
     *  ON THE TEXT: the middle of the laid-out line, converted to window
     *  coords. Empty text still falls back to the view centre, which is empty
     *  glass by definition and must raise the caret menu, exactly per spec. */
    private static float textCenterX(View view, int[] loc) {
        android.widget.EditText ed = (android.widget.EditText) view;
        try {
            android.text.Layout lo = ed.getLayout();
            if (lo != null && lo.getLineCount() > 0 && ed.getText().length() > 0) {
                float mid = (lo.getLineLeft(0) + lo.getLineRight(0)) / 2f
                        - ed.getScrollX();
                return loc[0] + ed.getCompoundPaddingLeft() + mid;
            }
        } catch (Exception ignored) {
            // fall through to the plain view centre
        }
        return loc[0] + view.getWidth() / 2f;
    }

    private static void holdCenter(androidx.test.espresso.UiController ui, View view) {
        int[] loc = new int[2];
        view.getLocationInWindow(loc);
        float cx = textCenterX(view, loc);
        float cy = loc[1] + view.getHeight() / 2f;
        long t = android.os.SystemClock.uptimeMillis();
        try {
            android.view.MotionEvent dn = android.view.MotionEvent.obtain(t, t,
                    android.view.MotionEvent.ACTION_DOWN, cx, cy, 0);
            ui.injectMotionEvent(dn);
            dn.recycle();
            ui.loopMainThreadForAtLeast(1200);
            android.view.MotionEvent up = android.view.MotionEvent.obtain(t, t + 1200,
                    android.view.MotionEvent.ACTION_UP, cx, cy, 0);
            ui.injectMotionEvent(up);
            up.recycle();
        } catch (Exception failed) {
            throw new RuntimeException("long press could not be injected", failed);
        }
        ui.loopMainThreadForAtLeast(300);
    }

    /** What the rest of the device would read after a Copy: the system clipboard. */
    private static String clipboard() {
        final String[] out = new String[]{""};
        onView(isRoot()).perform(new ViewAction() {
            @Override public Matcher<View> getConstraints() { return isDisplayed(); }
            @Override public String getDescription() { return "read the system clipboard"; }
            @Override public void perform(androidx.test.espresso.UiController ui, View view) {
                android.content.ClipboardManager cm = (android.content.ClipboardManager)
                        androidx.test.InstrumentationRegistry.getInstrumentation()
                                .getTargetContext()
                                .getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                android.content.ClipData d = cm == null ? null : cm.getPrimaryClip();
                out[0] = d == null || d.getItemCount() == 0
                        ? "" : String.valueOf(d.getItemAt(0).getText());
            }
        });
        return out[0];
    }

    /** Count the caret's white pixels by drawing the display to a bitmap: in
     *  dark mode nothing else on the display is pure white, so the count IS the
     *  bar -- painted, not intended. */
    private static int caretPixels() {
        final int[] out = new int[]{0};
        onView(withId(DISPLAY)).check(new androidx.test.espresso.ViewAssertion() {
            @Override public void check(View view, androidx.test.espresso.NoMatchingViewException e) {
                if (e != null) throw e;
                android.graphics.Bitmap bmp = android.graphics.Bitmap.createBitmap(
                        view.getWidth(), view.getHeight(),
                        android.graphics.Bitmap.Config.ARGB_8888);
                view.draw(new android.graphics.Canvas(bmp));
                int n = 0;
                for (int yy = 0; yy < bmp.getHeight(); yy++) {
                    for (int xx = 0; xx < bmp.getWidth(); xx++) {
                        if (bmp.getPixel(xx, yy) == 0xFFFFFFFF) n++;
                    }
                }
                out[0] = n;
            }
        });
        return out[0];
    }

    /** Let the main thread live for this many ms, so a blink phase can turn. */
    private static ViewAction waitMs(final long ms) {
        return new ViewAction() {
            @Override public Matcher<View> getConstraints() { return isDisplayed(); }
            @Override public String getDescription() { return "wait " + ms + " ms"; }
            @Override public void perform(androidx.test.espresso.UiController ui, View view) {
                ui.loopMainThreadForAtLeast(ms);
            }
        };
    }

    /** Is the round-30 real-view caret bar visible right now? It lives in the
     *  display's FrameLayout host under a tag, composited by the framework
     *  itself -- the caret path no emulator could break and no OEM could swallow. */
    private static boolean caretBarVisible() {
        final boolean[] out = new boolean[]{false};
        onView(withId(DISPLAY)).check(new androidx.test.espresso.ViewAssertion() {
            @Override public void check(View view, androidx.test.espresso.NoMatchingViewException e) {
                if (e != null) throw e;
                android.view.ViewGroup parent = (android.view.ViewGroup) view.getParent();
                View bar = parent == null ? null
                        : parent.findViewWithTag("calc_caret_bar");
                out[0] = bar != null && bar.getVisibility() == View.VISIBLE
                        && bar.getWidth() > 0 && bar.getHeight() > 0;
            }
        });
        return out[0];
    }

    /** The held length must be exactly this, because the owner drags for a length. */
    private static androidx.test.espresso.ViewAssertion held(final int want) {
        return new androidx.test.espresso.ViewAssertion() {
            @Override public void check(View view, androidx.test.espresso.NoMatchingViewException e) {
                if (e != null) throw e;
                android.widget.EditText ed = (android.widget.EditText) view;
                int got = ed.getSelectionEnd() - ed.getSelectionStart();
                assertTrue("held=" + got + " want=" + want, got == want);
            }
        };
    }

    /** A thumb down, four moves, a lift -- injected at WINDOW level so every
     *  onInterceptTouchEvent between the window and the display gets its say,
     *  exactly like a finger on his glass. */
    private static void drag(androidx.test.espresso.UiController ui, View view,
                             float fromX, float toX, float y) {
        // Round 18: dispatching straight at the view let the story pass while his
        // thumb died in the HorizontalScrollView parent, which intercepts a horizontal
        // drag and cancels the child. Injecting at WINDOW level walks the real
        // hierarchy -- every onInterceptTouchEvent on the way -- like a finger does.
        int[] loc = new int[2];
        view.getLocationInWindow(loc);
        long t0 = android.os.SystemClock.uptimeMillis();
        try {
            android.view.MotionEvent e = android.view.MotionEvent.obtain(t0, t0,
                    android.view.MotionEvent.ACTION_DOWN, loc[0] + fromX, loc[1] + y, 0);
            ui.injectMotionEvent(e);
            e.recycle();
            for (int i = 1; i <= 4; i++) {
                android.view.MotionEvent m = android.view.MotionEvent.obtain(
                        t0, t0 + i * 40L, android.view.MotionEvent.ACTION_MOVE,
                        loc[0] + fromX + (toX - fromX) * i / 4f, loc[1] + y, 0);
                ui.injectMotionEvent(m);
                m.recycle();
            }
            android.view.MotionEvent u = android.view.MotionEvent.obtain(t0, t0 + 200L,
                    android.view.MotionEvent.ACTION_UP, loc[0] + toX, loc[1] + y, 0);
            ui.injectMotionEvent(u);
            u.recycle();
        } catch (Exception failed) {
            throw new RuntimeException("handle drag could not be injected", failed);
        }
    }

    private static Matcher<View> key(String label) {
        return allOf(withText(label), not(withId(DISPLAY)));
    }

    private static void type(String... keys) {
        for (String k : keys) onView(key(k)).perform(click());
    }

    /** One finger on the glyph boundary at `off`: the owner's mid-text click. */
    private void tapAtOffset(final int off) {
        onView(withId(DISPLAY)).perform(new GeneralClickAction(Tap.SINGLE,
                new CoordinatesProvider() {
                    @Override public float[] calculateCoordinates(View view) {
                        android.widget.EditText ed = (android.widget.EditText) view;
                        android.text.Layout lo = ed.getLayout();
                        return new float[]{ed.getCompoundPaddingLeft()
                                + lo.getPrimaryHorizontal(off) + 2f,
                                ed.getHeight() * 0.5f};
                    }
                }, Press.FINGER, 0, 0));
    }

    /** Round 28: a real long press at a width FRACTION -- the gesture his thumb
     *  makes on the EMPTY part of the expression box, where nothing is written. */
    private static ViewAction holdAt(final float fraction) {
        return new ViewAction() {
            @Override public Matcher<View> getConstraints() { return isDisplayed(); }
            @Override public String getDescription() {
                return "long press at fraction " + fraction;
            }
            @Override public void perform(androidx.test.espresso.UiController ui, View view) {
                int[] loc = new int[2];
                view.getLocationInWindow(loc);
                float cx = loc[0] + view.getWidth() * fraction;
                float cy = loc[1] + view.getHeight() * 0.5f;
                long t = android.os.SystemClock.uptimeMillis();
                try {
                    android.view.MotionEvent dn = android.view.MotionEvent.obtain(
                            t, t, android.view.MotionEvent.ACTION_DOWN, cx, cy, 0);
                    ui.injectMotionEvent(dn);
                    dn.recycle();
                    ui.loopMainThreadForAtLeast(1200);
                    android.view.MotionEvent up = android.view.MotionEvent.obtain(
                            t, t + 1200, android.view.MotionEvent.ACTION_UP, cx, cy, 0);
                    ui.injectMotionEvent(up);
                    up.recycle();
                } catch (Exception failed) {
                    throw new RuntimeException("long press could not be injected", failed);
                }
                ui.loopMainThreadForAtLeast(300);
            }
        };
    }

    /** One finger, one point: the click gesture the owner's thumb makes. */
    private void tapAt(final float fraction) {
        onView(withId(DISPLAY)).perform(new GeneralClickAction(Tap.SINGLE,
                new CoordinatesProvider() {
                    @Override public float[] calculateCoordinates(View view) {
                        return new float[]{view.getWidth() * fraction,
                                view.getHeight() * 0.5f};
                    }
                }, Press.FINGER, 0, 0));
    }

    @Test public void aTapWhileTheHistoryPopupRepopulatesDoesNotCrash() {
        // Round 26 regression: his four "keeps stopping" crashes were tap
        // UPs landing on a half-rebuilt DynamicLayout (PackedIntVector
        // mRows=0) while the history swipe repopulated the popup.
        type("1", "2", "3", "4", "5", "6");
        onView(withId(DISPLAY)).perform(swipeDown());
        onView(withId(HISTORY_SCROLL)).check(matches(isDisplayed()));
        tapAt(0.5f);
        onView(withId(DISPLAY)).check(matches(isDisplayed()));
        onView(withId(DISPLAY)).check(held(0));
    }

    @Test public void jitterTapStillClearsTheHeldSelection() {
        // Round 25: his "free but hit or miss" -- a thumb never lifts
        // perfectly still, so a click that wanders a few pixels while a
        // selection stands must STILL clear it.
        type("1", "2", "3", "4", "5", "6");
        onView(withId(DISPLAY)).perform(holdUntilSelected()); // round 34: a raw longClick lands on the VIEW centre, which is empty glass
        // left of the right-aligned text -- holdUntilSelected holds the TEXT centre
        waitMs(150); // let the selection and its menu land before asserting
        onView(withId(DISPLAY)).check(held(6));
        onView(withId(DISPLAY)).perform(new androidx.test.espresso.ViewAction() {
            @Override public Matcher<View> getConstraints() { return isDisplayed(); }
            @Override public String getDescription() { return "jittery tap"; }
            @Override public void perform(androidx.test.espresso.UiController ui, View view) {
                float x = view.getWidth() * 0.3f, y = view.getHeight() * 0.5f;
                long now = android.os.SystemClock.uptimeMillis();
                view.dispatchTouchEvent(android.view.MotionEvent.obtain(
                        now, now, android.view.MotionEvent.ACTION_DOWN, x, y, 0));
                view.dispatchTouchEvent(android.view.MotionEvent.obtain(
                        now, now + 40, android.view.MotionEvent.ACTION_MOVE,
                        x + 9, y + 6, 0));
                view.dispatchTouchEvent(android.view.MotionEvent.obtain(
                        now, now + 90, android.view.MotionEvent.ACTION_UP, x + 9, y + 6, 0));
                ui.loopMainThreadForAtLeast(200);
            }
        });
        onView(withId(DISPLAY)).check(held(0));
    }

    @Test public void tapAwayFromTheExpressionClears() {
        // Round 25: "once I click anywhere little away from expression in the
        // expression box then the selection and the options ... should go".
        type("1", "2", "3", "4", "5", "6");
        onView(withId(DISPLAY)).perform(holdUntilSelected()); // round 34: a raw longClick lands on the VIEW centre, which is empty glass
        // left of the right-aligned text -- holdUntilSelected holds the TEXT centre
        waitMs(150); // let the selection and its menu land before asserting
        onView(withId(DISPLAY)).check(held(6));
        onView(withId(app.space.feature.calculator.R.id.calc_dock))
                .perform(new GeneralClickAction(Tap.SINGLE,
                        new CoordinatesProvider() {
                            @Override public float[] calculateCoordinates(View view) {
                                return new float[]{view.getWidth() * 0.5f,
                                        view.getHeight() - 6f};
                            }
                        }, Press.FINGER, 0, 0));
        onView(withId(DISPLAY)).check(held(0));
    }

    @Test public void aPlainTapParksTheCaretAndReleasesTheSelection() {
        // Round 27, his words: a click anywhere outside the held rectangle
        // releases it, and the caret must show where typing will land. One
        // tap does both: release, then park.
        type("1", "2", "3", "4", "5", "6");
        onView(withId(DISPLAY)).perform(holdUntilSelected()); // round 34: a raw longClick lands on the VIEW centre, which is empty glass
        // left of the right-aligned text -- holdUntilSelected holds the TEXT centre
        waitMs(150); // let the selection and its menu land before asserting
        onView(withId(DISPLAY)).check(held(6));
        tapAtOffset(2);
        onView(withId(DISPLAY)).check(held(0));
        type("7");
        onView(withId(DISPLAY)).check(matches(withText("1,27,345")));
    }

    @Test public void typingALongExpressionKeepsTheCaretOnScreen() {
        // Round 24 regression: on 25 every refresh yanked the inner scroll to a
        // layout-space mismatch and the typed text vanished until a selection
        // scrolled it back -- his "blinking and getting erased".
        type("1", "2", "3", "4", "5", "6", "7", "8", "9", "0",
                "1", "2", "3", "4", "5", "6", "7", "8");
        onView(withId(DISPLAY)).check(new androidx.test.espresso.ViewAssertion() {
            @Override public void check(View view, androidx.test.espresso.NoMatchingViewException e) {
                if (e != null) throw e;
                android.widget.EditText ed = (android.widget.EditText) view;
                android.text.Layout lo = ed.getLayout();
                assertTrue("the display must have a layout", lo != null);
                float prim = lo.getPrimaryHorizontal(ed.getSelectionStart());
                float onScreen = prim - ed.getScrollX()
                        + ed.getCompoundPaddingLeft();
                assertTrue("caret must sit inside the box, was " + onScreen,
                        onScreen >= 0 && onScreen <= ed.getWidth());
            }
        });
    }

    @Test public void selectionEndMarkersArePaintedInDark() {
        // Round 24: "the cursor for left and right boundary is not visible" --
        // his OEM paints the band but no teardrops, so the ends are hand-drawn
        // bars with bulbs; the bitmap count proves they are painted.
        try {
            setTheme(2);
            rule.getScenario().recreate();
            type("1", "2", "3", "4", "5", "6");
            onView(withId(DISPLAY)).perform(holdUntilSelected()); // round 34: a raw longClick lands on the VIEW centre, which is empty glass
        // left of the right-aligned text -- holdUntilSelected holds the TEXT centre
            onView(withId(DISPLAY)).check(new androidx.test.espresso.ViewAssertion() {
                @Override public void check(View view, androidx.test.espresso.NoMatchingViewException e) {
                    if (e != null) throw e;
                    android.graphics.Bitmap bmp = android.graphics.Bitmap.createBitmap(
                            view.getWidth(), view.getHeight(),
                            android.graphics.Bitmap.Config.ARGB_8888);
                    view.draw(new android.graphics.Canvas(bmp));
                    int white = 0;
                    for (int yy = 0; yy < bmp.getHeight(); yy++) {
                        for (int xx = 0; xx < bmp.getWidth(); xx++) {
                            if (bmp.getPixel(xx, yy) == 0xFFFFFFFF) white++;
                        }
                    }
                    assertTrue("the two end markers must be painted, got " + white,
                            white >= 60);
                }
            });
        } finally {
            setTheme(1);
            rule.getScenario().recreate();
        }
    }

    @Test public void theCaretAndInkAreActuallyPaintedInBothThemes() {
        // Round 20: his screenshot held NO caret and light mode wore white digits
        // on white keys. A view can hold every right colour and still paint
        // nothing, so this story draws the display to a bitmap in EACH theme and
        // counts pixels of the caret colour: "visible" means painted on the
        // canvas, not intended in code. In light mode the pitch-black ink of the
        // digits is counted too, because black pixels are the digits and the
        // caret together.
        try {
            for (int mode = 1; mode <= 2; mode++) {
                setTheme(mode);
                rule.getScenario().recreate();
                type("1", "+", "2");
                final int wantMode = mode;
                onView(withId(DISPLAY)).check(new androidx.test.espresso.ViewAssertion() {
                    @Override public void check(View view, androidx.test.espresso.NoMatchingViewException e) {
                        if (e != null) throw e;
                        app.space.feature.calculator.CalculatorScreen.DisplayEditText ed =
                                (app.space.feature.calculator.CalculatorScreen.DisplayEditText) view;
                        android.graphics.Bitmap bmp = android.graphics.Bitmap.createBitmap(
                                view.getWidth(), view.getHeight(),
                                android.graphics.Bitmap.Config.ARGB_8888);
                        view.draw(new android.graphics.Canvas(bmp));
                        int caretPixels = 0, inkPixels = 0, opPixels = 0;
                        int cyan = 0xFF00E5FF, pink = 0xFFFF2D78;
                        int caretWant = wantMode == 1 ? 0xFF000000 : 0xFFFFFFFF;
                        // Light: pitch-black expression ink (the caret shares it).
                        // Dark: the LED gradient's pink end stop, where the
                        // right-aligned glyphs sit.
                        int inkWant = wantMode == 1 ? 0xFF000000 : cyan;
                        assertTrue("theme " + wantMode + " caret colour="
                                + Integer.toHexString(ed.caretColor), ed.caretColor == caretWant);
                        for (int y = 0; y < bmp.getHeight(); y++) {
                            for (int x = 0; x < bmp.getWidth(); x++) {
                                int px = bmp.getPixel(x, y);
                                if (px == ed.caretColor) caretPixels++;
                                if (px == inkWant) inkPixels++;
                                if (px == pink) opPixels++;
                            }
                        }
                        assertTrue("theme " + wantMode + " caret pixels=" + caretPixels,
                                caretPixels >= 20);
                        assertTrue("theme " + wantMode + " ink pixels=" + inkPixels,
                                inkPixels >= 20);
                        if (wantMode == 2) {
                            assertTrue("dark operator pixels=" + opPixels,
                                    opPixels >= 20);
                        }
                    }
                });
            }
        } finally {
            setTheme(0);
            rule.getScenario().recreate();
        }
    }

    @Test public void implicitMultiplicationAnswersTwentyEightOnThePad() {
        type("2", "( )", "5", "+", "2", "( )", "2", "=");
        onView(withId(DISPLAY)).check(matches(withText("28")));
    }

    @Test public void aPendingOperatorIsAChangeableMindOnThePad() {
        type("1", "0", "+", "−");
        onView(withId(DISPLAY)).check(matches(withText("10−")));
        type("5", "=");
        onView(withId(DISPLAY)).check(matches(withText("5")));
    }

    @Test public void swipeDownOnDisplayOpensTheHistoryPopup() {
        // His round-8 message: the history is a popup window now, and dragging down on the
        // expression opens it. The scroller lives inside that popup's own window.
        type("1", "+", "1", "=");
        onView(withId(DISPLAY)).perform(swipeDown());
        onView(withId(HISTORY_SCROLL)).check(matches(isDisplayed()));
    }

    @Test public void historyPopupClosesWithItsCloseButton() {
        type("1", "+", "1", "=");
        onView(withId(HISTORY_BTN)).perform(click());
        onView(withId(HISTORY_SCROLL)).check(matches(isDisplayed()));
        onView(withId(HISTORY_CLOSE)).perform(click());
        onView(withId(HISTORY_SCROLL)).check(doesNotExist());
    }

    @Test public void theBracketKeyClosesWhatItOpened() {
        // His first point tonight: one button opens AND closes, Google's way.
        type("5", "( )", "1", "+", "1");
        type("( )");
        onView(withId(DISPLAY)).check(matches(withText("5(1+1)")));
        type("( )");
        onView(withId(DISPLAY)).check(matches(withText("5(1+1)(")));
        type("2", "( )", "=");
        onView(withId(DISPLAY)).check(matches(withText("20")));
    }

    @Test public void theCursorEditsInsideTheExpression() {
        // His second point: the tap must move the caret INTO the expression, so the next
        // key lands where the thumb pointed, not at the end.
        type("1", "2", "+", "3", "4");
        // Round 24: a click now selects the content under the finger (his ask),
        // and a second click parks the caret at that point -- so inserting mid
        // expression is tap-tap-type.
        // Round 27: one tap parks the caret; typing inserts right there.
        tapAtOffset(1);
        onView(withId(DISPLAY)).check(held(0));
        type("5");
        onView(withId(DISPLAY)).check(new androidx.test.espresso.ViewAssertion() {
            @Override public void check(View view, androidx.test.espresso.NoMatchingViewException e) {
                if (e != null) throw e;
                String txt = ((android.widget.EditText) view).getText().toString();
                assertTrue("the 5 must land inside, not at the end: " + txt,
                        txt.length() == 6 && txt.contains("5") && !txt.endsWith("45"));
            }
        });
    }

    @Test public void historyOpensWithTheNewestOnTopAndScrollsToTheOldest() {
        // Round 34, his words: "New entry should be on top like in Google
        // calculator." Twelve entries: the sheet opens with the newest FIRST
        // and the oldest is reached by scrolling down through time.
        for (int i = 1; i <= 12; i++) {
            type(String.valueOf(i), "+", "0", "=");
        }
        onView(withId(HISTORY_BTN)).perform(click());
        onView(withText("12+0")).check(matches(isDisplayed()));
        onView(withText("1+0")).check(matches(not(isDisplayed())));
        for (int i = 0; i < 4; i++) {
            onView(withId(HISTORY_SCROLL)).perform(androidx.test.espresso.action.ViewActions.swipeUp());
        }
        onView(withText("1+0")).check(matches(isDisplayed()));
    }

    @Test public void aHistoryTapCarriesTheExpressionIntoTheDisplay() {
        // Round 34, his words: "on click on history item nothing happens, the
        // expression that I have entered should entered in display." The tap
        // must paint the stored expression into the box AND close the sheet,
        // in that order -- paint first, teardown guarded.
        type("7", "+", "8", "=");
        onView(withId(DISPLAY)).check(matches(withText("15")));
        type("AC");
        onView(withId(HISTORY_BTN)).perform(click());
        onView(withText("7+8")).perform(click());
        onView(withId(DISPLAY)).check(matches(withText("7+8")));
        onView(withId(HISTORY_SCROLL)).check(doesNotExist());
        type("=");
        onView(withId(DISPLAY)).check(matches(withText("15")));
    }

    @Test public void theHistoryPopupShowsClearFullyAndKeepsThePad() {
        // His round-7 points carried into the popup: Clear fully visible, and -- because
        // the history floats in its own window -- the chevron strip and the pad never move.
        type("1", "+", "1", "=");
        onView(withId(CHEVRON_ROW)).check(matches(isDisplayed()));
        onView(withId(HISTORY_BTN)).perform(click());
        onView(withText("Clear")).check(matches(isCompletelyDisplayed()));
        // The popup floats in its own window: what must never move is the pad
        // beneath it, so the pad key is the assertion while the popup stands;
        // the chevron strip is checked again once the popup is closed.
        onView(key("7")).check(matches(isDisplayed()));
        onView(withContentDescription("Close history")).perform(click());
        onView(withId(CHEVRON_ROW)).check(matches(isDisplayed()));
    }

    @Test public void indianGroupingAppearsAsYouType() {
        // Round 9: "when numbers are typed, use commas as per Indian numbering."
        type("1", "2", "3", "4", "5", "6", "7");
        onView(withId(DISPLAY)).check(matches(withText("12,34,567")));
        type("8", "9", "0");
        onView(withId(DISPLAY)).check(matches(withText("1,23,45,67,890")));
    }

    @Test public void theDisplayNeverOffersTheKeyboard() {
        // Round 9, his first line: moving the cursor summoned the device keyboard and the
        // calculator became inoperable. The display must not be a text editor at all.
        onView(withId(DISPLAY)).perform(click());
        onView(withId(DISPLAY)).check(matches(hasFocus()));
        onView(withId(DISPLAY)).check(new androidx.test.espresso.ViewAssertion() {
            @Override public void check(View view, androidx.test.espresso.NoMatchingViewException e) {
                if (e != null) throw e;
                assertTrue("the display must never attach an IME",
                        !view.onCheckIsTextEditor());
            }
        });
    }

    @Test public void invSwapsThePanelAndComputesTheInverse() {
        // Round 9: Inv must change the whole panel Google's way, and the swapped caps
        // must really compute the inverse: asin(1) is 90 in degrees.
        onView(withId(CHEVRON_ROW)).perform(click());
        onView(key("Inv")).perform(click());
        onView(key("sin⁻¹")).check(matches(isDisplayed()));
        onView(key("x²")).check(matches(isDisplayed()));
        onView(key("eˣ")).check(matches(isDisplayed()));
        onView(key("sin⁻¹")).perform(click());
        type("1", "( )", "=");
        onView(withId(DISPLAY)).check(matches(withText("90")));
    }

    @Test public void memoryStoresAndRecallsFromItsPopup() {
        // Round 9: the three dots store the display, and View memory opens a small popup
        // where entries can be moved up and down and tapped back into the expression.
        type("5", "=");
        onView(withId(DISPLAY)).perform(holdUntilSelected()); // round 34: a raw longClick lands on the VIEW centre, which is empty glass
        // left of the right-aligned text -- holdUntilSelected holds the TEXT centre
        onView(withText("MAdd")).perform(click());
        type("AC", "7", "=");
        onView(withId(DISPLAY)).perform(holdUntilSelected()); // round 34: a raw longClick lands on the VIEW centre, which is empty glass
        // left of the right-aligned text -- holdUntilSelected holds the TEXT centre
        onView(withText("MAdd")).perform(click());
        onView(withId(MENU_BTN)).perform(click());
        onView(withText("Memory")).perform(click());
        onView(withId(MEMORY_LIST)).check(matches(isDisplayed()));
        onView(allOf(withText("5"), isDescendantOfA(withId(MEMORY_LIST))))
                .check(matches(isDisplayed()));
        onView(allOf(withText("7"), isDescendantOfA(withId(MEMORY_LIST))))
                .check(matches(isDisplayed()));
        // move the first entry (5) down, so 7 becomes place 1
        onView(withContentDescription("Move down from place 1")).perform(click());
        onView(withId(MEMORY_LIST)).check(new androidx.test.espresso.ViewAssertion() {
            @Override public void check(View view, androidx.test.espresso.NoMatchingViewException e) {
                if (e != null) throw e;
                android.widget.LinearLayout list = (android.widget.LinearLayout) view;
                android.widget.LinearLayout firstRow = (android.widget.LinearLayout) list.getChildAt(0);
                String first = ((android.widget.TextView) firstRow.getChildAt(0)).getText().toString();
                assertTrue("7 must hold place 1 after the move, not " + first,
                        "7".equals(first));
            }
        });
        // tapping an entry keeps it: it lands in the expression and the popup leaves
        onView(allOf(withText("5"), isDescendantOfA(withId(MEMORY_LIST)))).perform(click());
        onView(withId(MEMORY_LIST)).check(doesNotExist());
        onView(withId(DISPLAY)).check(matches(withText("5")));
    }

    @Test public void effectsPickerListsFivePressFeelsAndPersists() {
        // Round 10: at least four animations, kept in three dots as settings >> effects.
        onView(withId(MENU_BTN)).perform(click());
        onView(withText("Effects")).perform(click());
        onView(withText(containsString("Spring"))).check(matches(isDisplayed()));
        onView(withText(containsString("Squeeze"))).check(matches(isDisplayed()));
        onView(withText(containsString("Glow"))).check(matches(isDisplayed()));
        onView(withText(containsString("Lift"))).check(matches(isDisplayed()));
        onView(withText(containsString("Pulse"))).check(matches(isDisplayed()));
        onView(withText(containsString("Lift"))).perform(click());
        onView(withId(MENU_BTN)).perform(click());
        onView(withText("Effects")).perform(click());
        onView(withText(startsWith("✓  Lift"))).check(matches(isDisplayed()));
    }

    @Test public void theAnswerReadsAtDisplayGrade() {
        // Round 10: the answer must not read small -- 56sp on a short result.
        type("1", "+", "1", "=");
        onView(withId(DISPLAY)).check(matches(withText("2")));
        onView(withId(DISPLAY)).check(new androidx.test.espresso.ViewAssertion() {
            @Override public void check(View view, androidx.test.espresso.NoMatchingViewException e) {
                if (e != null) throw e;
                android.widget.TextView tv = (android.widget.TextView) view;
                float sp = tv.getTextSize() / view.getResources().getDisplayMetrics().scaledDensity;
                assertTrue("the answer must read at display grade, was " + sp + "sp",
                        sp >= 50f);
            }
        });
    }

    @Test public void scientificEqualsAutoClosesOpenBrackets() {
        // Round 11: "scientific operations are giving error" -- sin(30 with no closing
        // bracket must answer 0.5, Google's way, not Error.
        onView(withId(CHEVRON_ROW)).perform(click());
        onView(key("sin(")).perform(click());
        type("3", "0", "=");
        onView(withId(DISPLAY)).check(matches(withText("0.5")));
    }

    @Test public void inverseTrigAndPowerButtonsWorkFromThePanel() {
        // Round 12: sin⁻¹ cos¹ tan⁻¹ were dead -- their name was typed char by char and
        // the first 'a' was refused. Each Inv cap now inserts as one token and computes.
        onView(withId(CHEVRON_ROW)).perform(click());
        onView(key("Inv")).perform(click());
        onView(key("sin⁻¹")).perform(click());
        type("1", "( )", "=");
        onView(withId(DISPLAY)).check(matches(withText("90")));
        type("AC");
        onView(key("cos⁻¹")).perform(click());
        type("1", "( )", "=");
        onView(withId(DISPLAY)).check(matches(withText("0")));
        type("AC");
        onView(key("tan⁻¹")).perform(click());
        type("1", "( )", "=");
        onView(withId(DISPLAY)).check(matches(withText("45")));
        type("AC");
        type("5");
        onView(key("x²")).perform(click());
        type("=");
        onView(withId(DISPLAY)).check(matches(withText("25")));
        type("AC");
        onView(key("10ˣ")).perform(click());
        type("2", "( )", "=");
        onView(withId(DISPLAY)).check(matches(withText("100")));
        type("AC");
        onView(key("eˣ")).perform(click());
        type("0", "( )", "=");
        onView(withId(DISPLAY)).check(matches(withText("1")));
    }

    @Test public void selectionMenuOffersExactlyCutCopyMAddMUsePaste() {
        // Round 34, his words: "in selection menu remove select all option.
        // keep only cut, copy, madd, muse, paste." The five stay; Select all
        // is gone from the sheet; Cut still clears the held digits.
        type("1", "2", "3", "4", "5", "6");
        onView(withId(DISPLAY)).perform(holdUntilSelected()); // round 34: a raw longClick lands on the VIEW centre, which is empty glass
        // left of the right-aligned text -- holdUntilSelected holds the TEXT centre
        onView(withText("Cut")).check(matches(isDisplayed()));
        onView(withText("Copy")).check(matches(isDisplayed()));
        onView(withText("MAdd")).check(matches(isDisplayed()));
        onView(withText("MUse")).check(matches(isDisplayed()));
        onView(withText("Paste")).check(matches(isDisplayed()));
        onView(withText("Select all")).check(doesNotExist());
        onView(withText("Cut")).perform(click());
        onView(withId(DISPLAY)).check(matches(withText("0")));
    }

    @Test public void theCaretStaysWhereTheThumbInserted() {
        // Round 13: after a mid-expression insert the caret used to jump to the end, so
        // the next key landed there too. Two inserts in a row must stay neighbours.
        type("1", "2", "3", "4", "5");
        tapAtOffset(2);
        onView(withId(DISPLAY)).check(held(0));
        type("9");
        type("8");
        // 9 then 8 side by side in the middle: 1298345, not 1293458 at the end.
        onView(withId(DISPLAY)).check(matches(withText("12,98,345")));
    }

    @Test public void longPressSelectsTheWholeNumberAndOffersTheMenu() {
        // Round 19: the LONG PRESS IS ANDROID'S OWN again. Thirteen rounds ago an
        // app-owned long press replaced it because nothing appeared on his device;
        // the real cause was our own listener shadowing TextView's. With the shadow
        // gone the stock gesture holds the number under the finger and raises the
        // menu with the four.
        type("1", "2", "3", "4", "5", "6");
        onView(withId(DISPLAY)).perform(holdUntilSelected()); // round 34: a raw longClick lands on the VIEW centre, which is empty glass
        // left of the right-aligned text -- holdUntilSelected holds the TEXT centre
        onView(withId(DISPLAY)).check(new androidx.test.espresso.ViewAssertion() {
            @Override public void check(View view, androidx.test.espresso.NoMatchingViewException e) {
                if (e != null) throw e;
                android.widget.EditText ed = (android.widget.EditText) view;
                int held = ed.getSelectionEnd() - ed.getSelectionStart();
                assertTrue("the whole number must be held, held=" + held,
                        held == ed.getText().length());
            }
        });
        onView(withText("Copy")).perform(click());
    }

    @Test public void theSelectionSurvivesTheFingerLiftingAndTheMenuFollows() {
        // Round 14: his log collapsed the selection 200 ms after every long press --
        // "selection started: chars 0..6 held" then "cursor placed at N" as the thumb
        // lifted, so the highlight lived only while he held his finger down. The lift
        // must leave the held number highlighted; Copy then closes the menu and the
        // caret stands where the held text ended, so the next key appends there.
        type("1", "2", "3");
        onView(withId(DISPLAY)).perform(holdUntilSelected()); // round 34: a raw longClick lands on the VIEW centre, which is empty glass
        // left of the right-aligned text -- holdUntilSelected holds the TEXT centre
        onView(withId(DISPLAY)).check(new androidx.test.espresso.ViewAssertion() {
            @Override public void check(View view, androidx.test.espresso.NoMatchingViewException e) {
                if (e != null) throw e;
                android.widget.EditText ed = (android.widget.EditText) view;
                int held = ed.getSelectionEnd() - ed.getSelectionStart();
                assertTrue("the number must still be held after the finger lifts, held="
                        + held, held == ed.getText().length());
            }
        });
        onView(withText("Copy")).perform(click());
        onView(withId(DISPLAY)).check(matches(withText("123")));
        onView(withId(DISPLAY)).check(new androidx.test.espresso.ViewAssertion() {
            @Override public void check(View view, androidx.test.espresso.NoMatchingViewException e) {
                if (e != null) throw e;
                android.widget.EditText ed = (android.widget.EditText) view;
                assertTrue("the menu closing must leave a caret, not a ghost selection",
                        ed.getSelectionStart() == ed.getSelectionEnd());
            }
        });
        type("9");
        onView(withId(DISPLAY)).check(matches(withText("1,239")));
    }

    @Test public void anOperatorTypedOverAnOperatorReplacesItMidExpression() {
        // Round 14: "when I try to put +,-,* like operations, then it's not allowing to
        // change once typed." His log holds three refusals for one thumb movement --
        // mid-expression the splice inserted a SECOND operator and the validator
        // declined it. Over an operator, the one left of the caret is replaced now.
        type("1", "+", "1");
        onView(withId(DISPLAY)).perform(new androidx.test.espresso.ViewAction() {
            @Override public Matcher<View> getConstraints() { return isDisplayed(); }
            @Override public String getDescription() { return "park the caret after the plus"; }
            @Override public void perform(androidx.test.espresso.UiController ui, View view) {
                android.text.Spannable s =
                        (android.text.Spannable) ((android.widget.EditText) view).getText();
                android.text.Selection.setSelection(s, 2, 2);
            }
        });
        type("−");
        onView(withId(DISPLAY)).check(matches(withText("1−1")));
        type("×");
        onView(withId(DISPLAY)).check(matches(withText("1×1")));
    }

    @Test public void aPlainTapAfterTheMenuClosesPlacesTheCaretAgain() {
        // Round 14: tapping elsewhere dismisses the selection menu, puts the caret where
        // the thumb landed, and the next key goes exactly there -- not to the end.
        type("1", "2");
        onView(withId(DISPLAY)).perform(holdUntilSelected()); // round 34: a raw longClick lands on the VIEW centre, which is empty glass
        // left of the right-aligned text -- holdUntilSelected holds the TEXT centre
        onView(withId(DISPLAY)).perform(new GeneralClickAction(Tap.SINGLE,
                new CoordinatesProvider() {
                    @Override public float[] calculateCoordinates(View view) {
                        return new float[]{view.getWidth() * 0.25f, view.getHeight() * 0.5f};
                    }
                }, Press.FINGER, 0, 0));
        type("3");
        onView(withId(DISPLAY)).check(matches(withText("312")));
    }

    @Test public void percentChainsAndNegationExactlyAsTheOwnersScreenshot() {
        // Round 15: his screenshot of Google reads 1%%x-1+2 = 1.9999 -- chained percents
        // and a minus living under a multiply. The pad must type that exact sequence.
        type("1", "%", "%", "\u00d7", "\u2212", "1", "+", "2", "=");
        onView(withId(DISPLAY)).check(matches(withText("1.9999")));
    }

    @Test public void percentChainsMidExpressionToo() {
        // Round 15: "it should allow in the middle of typed expression as well. But now
        // it doesn't allows % when tried to." Caret parked inside a number, two percents
        // chain there, and equals still answers -- the chained operand meets the rest of
        // the number as an implicit product, the 2(5+2)2 rule one symbol later.
        type("1", "2", "3", "4");
        onView(withId(DISPLAY)).perform(new androidx.test.espresso.ViewAction() {
            @Override public Matcher<View> getConstraints() { return isDisplayed(); }
            @Override public String getDescription() { return "park the caret inside the number"; }
            @Override public void perform(androidx.test.espresso.UiController ui, View view) {
                android.text.Spannable s =
                        (android.text.Spannable) ((android.widget.EditText) view).getText();
                android.text.Selection.setSelection(s, 2, 2);
            }
        });
        type("%", "%");
        onView(withId(DISPLAY)).check(matches(withText("12%%34")));
        type("=");
        onView(withId(DISPLAY)).check(matches(withText("0.0408")));
    }

    @Test public void theBracketKeyFitsTheCaretNotJustTheEnd() {
        // Round 15: his log -- "key ) refused at 3 (after '()'" then "neither bracket
        // fits" -- because the one bracket key asked the END of the expression which
        // bracket fits while his thumb sat after an open one. Screenshot 1 is Google
        // typing 663((5+853 and answering 568854 with nothing closed.
        type("6", "6", "3", "( )", "5", "+", "8", "5", "3");
        onView(withId(DISPLAY)).perform(new androidx.test.espresso.ViewAction() {
            @Override public Matcher<View> getConstraints() { return isDisplayed(); }
            @Override public String getDescription() { return "park the caret after the 663"; }
            @Override public void perform(androidx.test.espresso.UiController ui, View view) {
                android.text.Spannable s =
                        (android.text.Spannable) ((android.widget.EditText) view).getText();
                android.text.Selection.setSelection(s, 3, 3);
            }
        });
        type("( )");
        onView(withId(DISPLAY)).check(matches(withText("663((5+853")));
        type("=");
        onView(withId(DISPLAY)).check(matches(withText("5,68,854")));
    }

    @Test public void longPressSelectionSurvivesTheFingerLifting() {
        // Round 23 regression: on 22 the UP of the long press itself hit my
        // tap-to-clear and the selection died a millisecond after birth
        // (his log: "selection started" then "selection menu closed").
        type("1", "2", "3", "4", "5", "6");
        onView(withId(DISPLAY)).perform(holdUntilSelected()); // round 34: a raw longClick lands on the VIEW centre, which is empty glass
        // left of the right-aligned text -- holdUntilSelected holds the TEXT centre
        waitMs(150); // let the selection and its menu land before asserting
        onView(withId(DISPLAY)).check(held(6));
        // and it stays useful: typing over the held range replaces it
        type("7");
        onView(withId(DISPLAY)).check(matches(withText("7")));
    }

    @Test public void historyTapGivesAnExpressionThatStillEvaluates() {
        // Round 22, his screenshot: a history line came back into the box and
        // every equals after it said Error -- the stored DISPLAY form carries
        // commas, and commas are not mathematics. Reuse must hand back raw.
        type("3", "5", "3", "5", "3", "−", "4", "3", "5", "3", "=");
        onView(withId(DISPLAY)).check(matches(withText("31,000")));
        onView(withId(HISTORY_BTN)).perform(click());
        onView(withText("35,353−4,353")).perform(click());
        onView(withId(DISPLAY)).check(matches(withText("35,353−4,353")));
        type("=");
        onView(withId(DISPLAY)).check(matches(withText("31,000")));
    }

    @Test public void aPlainTapWhileHeldClearsTheSelectionAndParksTheCaret() {
        // Round 22: "if user clicks somewhere in the expression box to remove
        // selected, its not allowing." Tap = down+up without travel: the held
        // range collapses to the caret at the tap and the menu goes.
        type("1", "2", "3", "4", "5", "6");
        onView(withId(DISPLAY)).perform(holdUntilSelected()); // round 34: a raw longClick lands on the VIEW centre, which is empty glass
        // left of the right-aligned text -- holdUntilSelected holds the TEXT centre
        waitMs(150); // let the selection and its menu land before asserting
        onView(withId(DISPLAY)).check(held(6));
        onView(withId(DISPLAY)).perform(new GeneralClickAction(Tap.SINGLE,
                new CoordinatesProvider() {
                    @Override public float[] calculateCoordinates(View view) {
                        return new float[]{view.getWidth() * 0.25f, view.getHeight() * 0.5f};
                    }
                }, Press.FINGER, 0, 0));
        onView(withId(DISPLAY)).check(held(0));
        type("7");
        onView(withId(DISPLAY)).check(matches(withText("71,23,456")));
    }

    @Test public void draggingInsideTheBandMovesTheNearerEnd() {
        // Round 22: "selects some more or less, it's not allowing" -- the held
        // band itself is a handle now: down inside it grabs the nearer end.
        type("1", "2", "3", "4", "5", "6");
        onView(withId(DISPLAY)).perform(holdUntilSelected()); // round 34: a raw longClick lands on the VIEW centre, which is empty glass
        // left of the right-aligned text -- holdUntilSelected holds the TEXT centre
        onView(withId(DISPLAY)).perform(new androidx.test.espresso.ViewAction() {
            @Override public Matcher<View> getConstraints() { return isDisplayed(); }
            @Override public String getDescription() { return "drag inside the band to offset 2"; }
            @Override public void perform(androidx.test.espresso.UiController ui, View view) {
                android.widget.EditText ed = (android.widget.EditText) view;
                android.text.Layout lo = ed.getLayout();
                float padL = ed.getCompoundPaddingLeft();
                float y = ed.getBaseline() + lo.getLineAscent(0) / 2f;
                drag(ui, view, padL + lo.getPrimaryHorizontal(4),
                        padL + lo.getPrimaryHorizontal(2), y);
                ui.loopMainThreadForAtLeast(200);
            }
        });
        onView(withId(DISPLAY)).check(held(2));
    }

    @Test public void holdThenDragExtendsTheHeldPortion() {
        // Round 21, the gesture his thumb has been making all along: hold the
        // number, then slide without lifting. One window-injected gesture --
        // down, hold past the long-press timeout, four moves, lift -- exactly
        // like a finger, through every interceptor on the way.
        type("1", "2", "3", "4", "5", "6");
        onView(withId(DISPLAY)).perform(holdThenDragTo(2));
        onView(withId(DISPLAY)).check(held(2));
        onView(withText("Copy")).check(matches(isDisplayed()));
    }

    @Test public void draggingEitherHandleMovesThatBoundary() {
        // Round 16 asked for Android's boundaries; round 19 finally hands them
        // over: the teardrops on screen are the framework's own, and this drag
        // walks the real window path -- scroll parent and all. Right end in to
        // offset 4, then left end in to offset 2: two chars must stay held.
        type("1", "2", "3", "4", "5", "6");
        onView(withId(DISPLAY)).perform(holdUntilSelected()); // round 34: a raw longClick lands on the VIEW centre, which is empty glass
        // left of the right-aligned text -- holdUntilSelected holds the TEXT centre
        onView(withId(DISPLAY)).perform(narrowRightHandleTo(4));
        onView(withId(DISPLAY)).perform(narrowLeftHandleTo(2));
        onView(withId(DISPLAY)).check(new androidx.test.espresso.ViewAssertion() {
            @Override public void check(View view, androidx.test.espresso.NoMatchingViewException e) {
                if (e != null) throw e;
                android.widget.EditText ed = (android.widget.EditText) view;
                int held = ed.getSelectionEnd() - ed.getSelectionStart();
                assertTrue("both boundaries must move, held=" + held, held == 2);
            }
        });
    }

    @Test public void copyPutsExactlyTheHeldSliceInTheClipboard() {
        // Round 17, end-user truth: what you see held is what lands in the clipboard.
        // Not "a copy happened" -- the exact slice, commas and all, read back out of
        // the system clipboard the way the next app would read it.
        type("1", "2", "3", "4", "5", "6");
        onView(withId(DISPLAY)).perform(holdUntilSelected()); // round 34: a raw longClick lands on the VIEW centre, which is empty glass
        // left of the right-aligned text -- holdUntilSelected holds the TEXT centre
        onView(withId(DISPLAY)).perform(narrowRightHandleTo(5));
        onView(withText("Copy")).perform(click());
        // The clipboard carries the raw slice -- commas are display furniture, never
        // part of what the next app pastes.
        assertEquals("1234", clipboard());
        onView(withId(DISPLAY)).check(matches(withText("123,456")));
    }

    @Test public void cutRemovesExactlyTheHeldPortionAndLetsYouTypeThere() {
        // Round 17: his log -- "selection cut: 0 chars removed" while a range stood
        // held, then a second cut that ate the whole expression. Cut must remove
        // exactly the held slice, leave the rest, and park the caret in the gap.
        type("1", "2", "3", "4", "5", "6");
        onView(withId(DISPLAY)).perform(holdUntilSelected()); // round 34: a raw longClick lands on the VIEW centre, which is empty glass
        // left of the right-aligned text -- holdUntilSelected holds the TEXT centre
        onView(withId(DISPLAY)).perform(narrowRightHandleTo(3));
        onView(withText("Cut")).perform(click());
        onView(withId(DISPLAY)).check(matches(withText("456")));
        type("9");
        onView(withId(DISPLAY)).check(matches(withText("9,456")));
        assertEquals("123", clipboard());
    }

    @Test public void aTapElsewhereClosesTheMenuAndClearsTheSelection() {
        // Round 17 lifecycle: the highlight is not furniture. Tapping away drops both
        // the menu and the band, and the caret stands where the thumb landed.
        type("1", "2", "3");
        onView(withId(DISPLAY)).perform(holdUntilSelected());
        onView(withId(DISPLAY)).check(held(3));
        onView(withId(DISPLAY)).perform(new GeneralClickAction(Tap.SINGLE,
                new CoordinatesProvider() {
                    @Override public float[] calculateCoordinates(View view) {
                        return new float[]{view.getWidth() * 0.25f, view.getHeight() * 0.5f};
                    }
                }, Press.FINGER, 0, 0));
        onView(withId(DISPLAY)).check(held(0));
        type("7");
        onView(withId(DISPLAY)).check(matches(withText("7123")));
    }

    @Test public void acWhileHeldClearsTheHeldTextAndTheMenu() {
        // Round 17 lifecycle: AC with a selection held clears the expression and the
        // selection together -- no ghost band over an empty display.
        type("1", "2", "3");
        onView(withId(DISPLAY)).perform(holdUntilSelected()); // round 34: a raw longClick lands on the VIEW centre, which is empty glass
        // left of the right-aligned text -- holdUntilSelected holds the TEXT centre
        waitMs(150); // let the selection and its menu land before asserting
        onView(withId(DISPLAY)).check(held(3));
        type("AC");
        onView(withId(DISPLAY)).check(held(0));
        onView(withId(DISPLAY)).check(matches(withText("")));
    }

    @Test public void minusUnderAMultiplyMidExpressionKeepsBoth() {
        // Round 18, his exact sequence: 10*-5 types fine at the end; with the caret
        // moved after the multiply, * then - used to leave 10--5 -- the minus ate the
        // multiply. Mid-expression now mirrors the end: the minus inserts as negation
        // and the multiply stays where his thumb put it.
        type("1", "0", "\u00d7", "\u2212", "5");
        onView(withId(DISPLAY)).check(matches(withText("10\u00d7\u22125")));
        onView(withId(DISPLAY)).perform(new ViewAction() {
            @Override public Matcher<View> getConstraints() { return isDisplayed(); }
            @Override public String getDescription() { return "park the caret after the multiply"; }
            @Override public void perform(androidx.test.espresso.UiController ui, View view) {
                android.text.Spannable s =
                        (android.text.Spannable) ((android.widget.EditText) view).getText();
                android.text.Selection.setSelection(s, 3, 3);
            }
        });
        type("\u00d7", "\u2212");
        onView(withId(DISPLAY)).check(matches(withText("10\u00d7\u2212\u22125")));
    }

    @Test public void decimalsNeverCarryCommas() {
        // Round 13: his screenshot read 1.5,70,79,63,268. Decimals are not thousands.
        type("1", ".", "5", "7", "0");
        onView(withId(DISPLAY)).check(matches(withText("1.570")));
    }

    @Test public void theCaretExistsIsFocusedAndIsVisible() {
        type("1", "2", "3", "4", "5", "6", "7", "8", "9", "0", "1", "2", "3", "4", "5", "6");
        onView(withId(DISPLAY)).perform(new GeneralClickAction(Tap.SINGLE,
                new CoordinatesProvider() {
                    @Override public float[] calculateCoordinates(View view) {
                        return new float[]{view.getWidth() * 0.5f, view.getHeight() * 0.5f};
                    }
                }, Press.FINGER, 0, 0));
        onView(withId(DISPLAY)).check(matches(hasFocus()));
        onView(withId(DISPLAY)).check(new androidx.test.espresso.ViewAssertion() {
            @Override public void check(View view, androidx.test.espresso.NoMatchingViewException e) {
                if (e != null) throw e;
                EditText editor = (EditText) view;
                assertTrue("the cursor must be switched on", editor.isCursorVisible());
                if (android.os.Build.VERSION.SDK_INT >= 29) {
                    assertTrue("the cursor must be drawn, not themed into invisibility",
                            editor.getTextCursorDrawable() != null);
                }
            }
        });
    }

    @Test public void aLongPressOnEmptyBoxParksTheCaretAndRaisesTheCaretMenu() {
        // Round 28, his words: "if pressed a hold on other area of expression
        // bar where expression is not even written, then also some operator of
        // expression is getting selected. It should not." The expression sits
        // at the RIGHT of the box, so the empty part is the left; a hold there
        // must hold NOTHING, park the caret at the near end and raise Google's
        // caret duo (MUse, Paste) -- round 34 dropped Select all from it too:
        // no Cut, no Copy, no Select all, nothing held.
        type("1", "2", "+", "3", "4");
        onView(withId(DISPLAY)).perform(holdAt(0.08f));
        onView(withId(DISPLAY)).check(held(0));
        onView(withText("MUse")).check(matches(isDisplayed()));
        onView(withText("Paste")).check(matches(isDisplayed()));
        onView(withText("Select all")).check(doesNotExist());
        onView(withText("Cut")).check(doesNotExist());
        // The caret parked at 0: the next key lands at the FRONT, proving the
        // park is a real cursor position and not a no-op.
        type("9");
        onView(withId(DISPLAY)).check(matches(withText("912+34")));
    }

    @Test public void memStoresTheHeldNumberAndMrInsertsItAtTheCaret() {
        // Round 28, his two asks in one flow: "along with copy, cut options,
        // add Mem option as well" and "need cursor to use recently saved
        // memory... long pressed between expression, and able to insert the
        // saved memory there" -- the Google Calculator MR he screenshotted.
        type("4", "5", "6");
        onView(withId(DISPLAY)).perform(holdUntilSelected()); // round 34: a raw longClick lands on the VIEW centre, which is empty glass
        // left of the right-aligned text -- holdUntilSelected holds the TEXT centre
        waitMs(150); // let the selection and its menu land before asserting
        onView(withId(DISPLAY)).check(held(3));
        onView(withText("MAdd")).check(matches(isDisplayed()));
        onView(withText("MAdd")).perform(click());
        // A plain tap releases and parks the caret at the front of the box...
        tapAtOffset(0);
        onView(withId(DISPLAY)).check(held(0));
        // ...and a hold on the empty left raises the caret menu, whose MR
        // inserts the recently saved value AT THAT CARET.
        onView(withId(DISPLAY)).perform(holdAt(0.08f));
        onView(withId(DISPLAY)).check(held(0));
        onView(withText("MUse")).perform(click());
        // Indian grouping is display furniture: 456456 reads 4,56,456.
        onView(withId(DISPLAY)).check(matches(withText("4,56,456")));
    }

    @Test public void aTapOnTheHeldPortionParksTheCaretAndReleasesTheSelection() {
        // Round 31, his words: "when a something is selected and then if user
        // clicks again somewhere in the expression, it may be a selected
        // portion or non selected portion. Cursor should go to clicked position
        // and release selection. At a time, selection can be there or a
        // cursor." A still finger on the held band is a TAP, not a zero-move
        // drag: the highlight and the menu go, the caret stands where it landed,
        // and the next key proves the position.
        type("1", "2", "3", "4", "5", "6");
        onView(withId(DISPLAY)).perform(holdUntilSelected()); // round 34: a raw longClick lands on the VIEW centre, which is empty glass
        // left of the right-aligned text -- holdUntilSelected holds the TEXT centre
        waitMs(150); // let the selection and its menu land before asserting
        onView(withId(DISPLAY)).check(held(6));
        onView(withText("Copy")).check(matches(isDisplayed()));
        // Straight down on the held band itself -- the gesture that used to be
        // a zero-move "drag" and kept the selection.
        tapAtOffset(3);
        onView(withId(DISPLAY)).check(held(0));
        onView(withText("Copy")).check(doesNotExist());
        type("9");
        // The tap landed mid-number, so the 9 lands mid-number, not at the end.
        onView(withId(DISPLAY)).check(matches(withText("12,39,456")));
    }

    @Test public void aHoldInTheRightMarginRaisesTheCaretMenuNotTheLastOperand() {
        // Round 31: "keep more empty space in the right of the expression,
        // atleast another character's space. user is not able to press and hold,
        // to just paste or use memory. Now it's just selecting last operand."
        // The box keeps a whole character of empty glass on the right; a hold
        // there parks the caret at the end and raises the caret trio.
        type("1", "2", "+", "3", "4");
        onView(withId(DISPLAY)).perform(holdAt(0.97f));
        onView(withId(DISPLAY)).check(held(0));
        onView(withText("MUse")).check(matches(isDisplayed()));
        onView(withText("Paste")).check(matches(isDisplayed()));
        onView(withText("Cut")).check(doesNotExist());
    }

    @Test public void theKeyShapePickerShowsLivePreviewsAndPersistsThePick() {
        // Round 31 asked for the option; round 32 judged the stock light
        // AlertDialog "outdated" and ordered it matched to the app's design,
        // and ordered the hexagon removed. The picker is now the app's own dark
        // pill popup, each row carrying a swatch drawn by the drawable it
        // selects; five silhouettes remain and the pick persists.
        onView(withId(MENU_BTN)).perform(click());
        onView(withText("Key shape")).perform(click());
        onView(withText(containsString("Hexagon"))).check(doesNotExist());
        onView(withText(containsString("Squircle"))).check(matches(isDisplayed()));
        onView(withText(containsString("No shape"))).check(matches(isDisplayed()));
        onView(withText(containsString("Pill"))).perform(click());
        android.content.Context ctx =
                androidx.test.InstrumentationRegistry.getTargetContext();
        assertEquals("Pill", ctx.getSharedPreferences(
                app.space.core.data.AppPrefs.PREF, android.content.Context.MODE_PRIVATE)
                .getString("calculator_keyshape", "Circle"));
        // and the pad still works after the rebuild
        type("7");
        onView(withId(DISPLAY)).check(matches(withText("7")));
        // leave the pad as the owner found it
        onView(withId(MENU_BTN)).perform(click());
        onView(withText("Key shape")).perform(click());
        onView(withText(containsString("Circle"))).perform(click());
    }

    @Test public void noShapeKeysArePlainGlassAndCircleIsARoundDisc() {
        // Round 32: "if no shape is selected then it should be plain no border.
        // May be when user clicks the key, on that time you can show the effect
        // in rectangle" and "Pill and circle shape looks similar. Keep proper
        // circles." So a No-shape key idles with a state-list (transparent at
        // rest, rectangle flash pressed), and a Circle key carries the disc
        // drawable -- never a clamped radius that reads as a pill.
        onView(withId(MENU_BTN)).perform(click());
        onView(withText("Key shape")).perform(click());
        onView(withText(containsString("No shape"))).perform(click());
        onView(withText("7")).check((v, no) -> assertTrue(
                "plain keys idle as a state list",
                v.getBackground() instanceof android.graphics.drawable.StateListDrawable));
        type("7");
        onView(withId(DISPLAY)).check(matches(withText("7")));
        onView(withId(MENU_BTN)).perform(click());
        onView(withText("Key shape")).perform(click());
        onView(withText(containsString("Circle"))).perform(click());
        onView(withText("8")).check((v, no) -> assertEquals("CircleDrawable",
                v.getBackground().getClass().getSimpleName()));
        type("8");
        onView(withId(DISPLAY)).check(matches(withText("78")));
        // hexagon is gone from the pad's vocabulary entirely
        android.content.Context ctx =
                androidx.test.InstrumentationRegistry.getTargetContext();
        assertEquals("Circle", ctx.getSharedPreferences(
                app.space.core.data.AppPrefs.PREF, android.content.Context.MODE_PRIVATE)
                .getString("calculator_keyshape", "Circle"));
    }

    @Test public void theKeyThemePickerRecoloursByGroupAndFollowsMySpace() {
        // Round 33: "Add color theme options in 3 dots... group them and have a
        // few theme... a option to follow the MySpace appearance settings, name
        // it as MySpace settings." The picker lists the app's own palette first
        // and four grouped colour themes; the pick persists and rebuilds.
        onView(withId(MENU_BTN)).perform(click());
        onView(withText("Key theme")).perform(click());
        onView(withText(containsString("MySpace settings"))).check(matches(isDisplayed()));
        onView(withText(containsString("Ocean"))).check(matches(isDisplayed()));
        onView(withText(containsString("Ocean"))).perform(click());
        android.content.Context ctx =
                androidx.test.InstrumentationRegistry.getTargetContext();
        assertEquals("Ocean", ctx.getSharedPreferences(
                app.space.core.data.AppPrefs.PREF, android.content.Context.MODE_PRIVATE)
                .getString("calculator_keytheme", "MySpace settings"));
        type("7");
        onView(withId(DISPLAY)).check(matches(withText("7")));
        onView(withId(MENU_BTN)).perform(click());
        onView(withText("Key theme")).perform(click());
        onView(withText(containsString("MySpace settings"))).perform(click());
        assertEquals("MySpace settings", ctx.getSharedPreferences(
                app.space.core.data.AppPrefs.PREF, android.content.Context.MODE_PRIVATE)
                .getString("calculator_keytheme", "MySpace settings"));
    }

    @Test public void theThemePickerOffersTheBrightHousesAndTheyApply() {
        // Round 34: "add few more themes ... make theme colours bit lightful
        // and attractive." Rose, Lemon and Iris joined the picker; each must
        // be reachable (the list scrolls now), selectable and persisted, and
        // the pad must keep typing under the new colours.
        onView(withId(MENU_BTN)).perform(click());
        onView(withText("Key theme")).perform(click());
        for (String house : new String[]{"Rose", "Lemon", "Iris"}) {
            onView(withText(containsString(house))).perform(
                    androidx.test.espresso.action.ViewActions.scrollTo());
            onView(withText(containsString(house))).check(matches(isDisplayed()));
        }
        onView(withText(containsString("Iris"))).perform(click());
        android.content.Context ctx =
                androidx.test.InstrumentationRegistry.getTargetContext();
        assertEquals("Iris", ctx.getSharedPreferences(
                app.space.core.data.AppPrefs.PREF, android.content.Context.MODE_PRIVATE)
                .getString("calculator_keytheme", "MySpace settings"));
        type("9", "+", "1", "=");
        onView(withId(DISPLAY)).check(matches(withText("10")));
        // leave the suite on the app's own palette for the stories after this one
        onView(withId(MENU_BTN)).perform(click());
        onView(withText("Key theme")).perform(click());
        onView(withText(containsString("MySpace settings"))).perform(
                androidx.test.espresso.action.ViewActions.scrollTo());
        onView(withText(containsString("MySpace settings"))).perform(click());
    }

    @Test public void aSingleStrokeFromTheDotsRunsTheHighlightedLeaf() {
        // Round 33: "pressing the button opens it, dragging highlights items,
        // and lifting the finger triggers the action." One finger, no second
        // tap: down on the dots, drag onto Haptics, lift -- the pref flips.
        android.content.Context ctx =
                androidx.test.InstrumentationRegistry.getTargetContext();
        boolean before = ctx.getSharedPreferences(
                app.space.core.data.AppPrefs.PREF, android.content.Context.MODE_PRIVATE)
                .getBoolean("calculator_haptics", true);
        final int[] dots = new int[2];
        rule.getScenario().onActivity(a ->
                a.findViewById(MENU_BTN).getLocationOnScreen(dots));
        long down = android.os.SystemClock.uptimeMillis();
        pointer(down, android.view.MotionEvent.ACTION_DOWN, dots[0] + 8, dots[1] + 8);
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
                .waitForIdleSync();
        waitMs(350); // the stroke popup needs a layout pass before rows have rects
        int[] row = app.space.feature.calculator.CalculatorScreen
                .debugStrokeRowRect("Haptics: " + (before ? "On" : "Off"));
        assertTrue("stroke menu must expose its rows to a real finger", row != null);
        float cx = (row[0] + row[2]) / 2f, cy = (row[1] + row[3]) / 2f;
        pointer(down, android.view.MotionEvent.ACTION_MOVE, cx, cy);
        pointer(down, android.view.MotionEvent.ACTION_MOVE, cx, cy);
        pointer(down, android.view.MotionEvent.ACTION_UP, cx, cy);
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
                .waitForIdleSync();
        boolean after = ctx.getSharedPreferences(
                app.space.core.data.AppPrefs.PREF, android.content.Context.MODE_PRIVATE)
                .getBoolean("calculator_haptics", true);
        assertTrue("lifting on the highlighted row must flip haptics", after != before);
        assertTrue("the stroke popup must be gone after the lift",
                app.space.feature.calculator.CalculatorScreen
                        .debugStrokeRowRect("Haptics: On") == null);
    }

    @Test public void aSingleStrokeTravelsIntoKeyShapeWithoutLifting() {
        // Round 33: "It must support continuous tracking into child submenus
        // without releasing the finger." Down on the dots, drag onto Key shape,
        // dwell so the child swaps in, keep dragging onto Pill, lift -- Pill is
        // applied from one unbroken stroke.
        final int[] dots = new int[2];
        rule.getScenario().onActivity(a ->
                a.findViewById(MENU_BTN).getLocationOnScreen(dots));
        long down = android.os.SystemClock.uptimeMillis();
        pointer(down, android.view.MotionEvent.ACTION_DOWN, dots[0] + 8, dots[1] + 8);
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
                .waitForIdleSync();
        waitMs(350);
        int[] parent = app.space.feature.calculator.CalculatorScreen
                .debugStrokeRowRect("Key shape");
        assertTrue(parent != null);
        float px = (parent[0] + parent[2]) / 2f, py = (parent[1] + parent[3]) / 2f;
        pointer(down, android.view.MotionEvent.ACTION_MOVE, px, py);
        pointer(down, android.view.MotionEvent.ACTION_MOVE, px, py);
        waitMs(520); // the dwell that opens the child mid-stroke
        waitMs(250); // and the child's rows need their own layout pass
        int[] pill = app.space.feature.calculator.CalculatorScreen
                .debugStrokeRowRect("Pill");
        assertTrue("the child rows must arrive without lifting the finger", pill != null);
        float qx = (pill[0] + pill[2]) / 2f, qy = (pill[1] + pill[3]) / 2f;
        pointer(down, android.view.MotionEvent.ACTION_MOVE, qx, qy);
        pointer(down, android.view.MotionEvent.ACTION_MOVE, qx, qy);
        pointer(down, android.view.MotionEvent.ACTION_UP, qx, qy);
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
                .waitForIdleSync();
        android.content.Context ctx =
                androidx.test.InstrumentationRegistry.getTargetContext();
        assertEquals("Pill", ctx.getSharedPreferences(
                app.space.core.data.AppPrefs.PREF, android.content.Context.MODE_PRIVATE)
                .getString("calculator_keyshape", "Circle"));
        // leave the pad as the owner found it, through the tap-through menu
        onView(withId(MENU_BTN)).perform(click());
        onView(withText("Key shape")).perform(click());
        onView(withText(containsString("Circle"))).perform(click());
    }

    private void pointer(long downTime, int action, float x, float y) {
        android.view.MotionEvent e = android.view.MotionEvent.obtain(downTime,
                android.os.SystemClock.uptimeMillis(), action, x, y, 0);
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
                .sendPointerSync(e);
        e.recycle();
    }

    @Test public void theCaretBarIsARealViewThatBlinksWithThePhase() {
        // Round 30, after five canvas-drawn carets died on his OEM while every
        // geometry number read perfect: the caret is ALSO a real view in the
        // hierarchy. This story asserts it exists, has size, stands visible
        // right after a key, and blinks at rest -- a view the framework
        // composites cannot be swallowed by any onDraw.
        try {
            setTheme(2);
            rule.getScenario().recreate();
            type("1", "2");
            assertTrue("the real caret bar must stand visible after a key",
                    caretBarVisible());
            onView(isRoot()).perform(waitMs(800));
            boolean b1 = caretBarVisible();
            onView(isRoot()).perform(waitMs(500));
            boolean b2 = caretBarVisible();
            onView(isRoot()).perform(waitMs(500));
            boolean b3 = caretBarVisible();
            assertTrue("the real caret bar must blink at rest: "
                    + b1 + "," + b2 + "," + b3, !(b1 == b2 && b2 == b3));
        } finally {
            setTheme(1);
            rule.getScenario().recreate();
        }
    }

    @Test public void theCaretStandsSolidThenBlinksLikeAndroidsOwn() {
        // Round 29, his words: "cursor is not visible, it should be blinking
        // atleast. Take reference of how android is." Android's cursor stands
        // solid while it moves and blinks at rest. Solid is checked right after
        // a key; then three samples half a blink period apart, taken once the
        // solid window has passed, must NOT all agree -- a steady bar his eye
        // missed becomes a rhythm his eye cannot miss.
        try {
            setTheme(2);
            rule.getScenario().recreate();
            type("1", "2", "3");
            assertTrue("the caret must stand solid right after a key, pixels="
                    + caretPixels(), caretPixels() > 0);
            onView(isRoot()).perform(waitMs(800));
            int s1 = caretPixels();
            onView(isRoot()).perform(waitMs(500));
            int s2 = caretPixels();
            onView(isRoot()).perform(waitMs(500));
            int s3 = caretPixels();
            assertTrue("the caret must blink at rest: " + s1 + "," + s2 + "," + s3,
                    !(s1 == s2 && s2 == s3));
        } finally {
            setTheme(1);
            rule.getScenario().recreate();
        }
    }

    @Test public void theRightBoundaryStaysOnGlassWhenTheSelectionReachesTheEnd() {
        // Round 28, his log: `caret at x=1132 viewW=1132` -- zero horizontal
        // padding put the content edge ON the view edge, so the right boundary
        // bar and bulb drew one bar width OFF the canvas: a one-sided
        // selection. With side padding the rightmost caret-ink pixel of a
        // full-length selection must sit strictly inside the glass.
        try {
            setTheme(2);
            rule.getScenario().recreate();
            type("1", "2", "3", "4", "5", "6");
            // Round 34 removed Select all; the geometry contract below is about
            // the laid-out line's edges, so any hold that raises the menu works.
            onView(withId(DISPLAY)).perform(holdUntilSelected()); // round 34: a raw longClick lands on the VIEW centre, which is empty glass
        // left of the right-aligned text -- holdUntilSelected holds the TEXT centre
            onView(withId(DISPLAY)).check(new androidx.test.espresso.ViewAssertion() {
                @Override public void check(View view, androidx.test.espresso.NoMatchingViewException e) {
                    if (e != null) throw e;
                    android.widget.EditText ed = (android.widget.EditText) view;
                    // The on-glass contract: real side padding, and the laid-out
                    // line's right edge inside the content box -- the two facts
                    // his log lacked when the end caret read x=1132 on a 1132px
                    // view and the right teardrop drew off the canvas.
                    float d = view.getResources().getDisplayMetrics().density;
                    assertTrue("the box must keep side padding for the end caret, padR="
                            + ed.getCompoundPaddingRight(),
                            ed.getCompoundPaddingRight() >= (int) (10 * d));
                    android.text.Layout lo = ed.getLayout();
                    float lineRight = lo.getLineRight(0) - ed.getScrollX();
                    float edge = view.getWidth() - ed.getCompoundPaddingRight();
                    assertTrue("the line's right edge must sit on the glass, lineRight="
                            + lineRight + " edge=" + edge, lineRight <= edge + 2);
                    // and the two boundary markers are actually painted.
                    android.graphics.Bitmap bmp = android.graphics.Bitmap.createBitmap(
                            view.getWidth(), view.getHeight(),
                            android.graphics.Bitmap.Config.ARGB_8888);
                    view.draw(new android.graphics.Canvas(bmp));
                    int white = 0;
                    for (int yy = 0; yy < bmp.getHeight(); yy++) {
                        for (int xx = 0; xx < bmp.getWidth(); xx++) {
                            if (bmp.getPixel(xx, yy) == 0xFFFFFFFF) white++;
                        }
                    }
                    assertTrue("the end markers must be painted, got " + white,
                            white >= 60);
                }
            });
        } finally {
            setTheme(1);
            rule.getScenario().recreate();
        }
    }
}
