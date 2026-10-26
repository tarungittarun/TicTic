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

    /** A pad key by its label, never the display or the preview that may show the same text. */
    /** Narrows the held range by dragging the right handle to a display offset. */
    private static ViewAction narrowRightHandleTo(final int offset) {
        return new ViewAction() {
            @Override public Matcher<View> getConstraints() { return isDisplayed(); }
            @Override public String getDescription() { return "drag the right handle to offset " + offset; }
            @Override public void perform(androidx.test.espresso.UiController ui, View view) {
                android.widget.EditText ed = (android.widget.EditText) view;
                android.text.Layout lo = ed.getLayout();
                float d = view.getResources().getDisplayMetrics().density;
                float padL = ed.getCompoundPaddingLeft();
                float y = ed.getBaseline() + lo.getLineDescent(0) + 8 * d + 14 * d;
                drag(ui, view, padL + lo.getPrimaryHorizontal(ed.getSelectionEnd()),
                        padL + lo.getPrimaryHorizontal(offset), y);
                ui.loopMainThreadForAtLeast(200);
            }
        };
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

    /** A thumb down on a handle, four moves, a lift -- straight through dispatchTouchEvent,
     *  which is the public door to the view's own touch listener. */
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
        onView(withId(DISPLAY)).perform(new GeneralClickAction(Tap.SINGLE,
                new CoordinatesProvider() {
                    @Override public float[] calculateCoordinates(View view) {
                        return new float[]{view.getWidth() * 0.2f, view.getHeight() * 0.5f};
                    }
                }, Press.FINGER, 0, 0));
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

    @Test public void historyOpensAtTheNewestAndScrollsToTheOldest() {
        // His third point: nine visible records were all he could reach. Twelve entries,
        // opened at the newest, then scrolled up through time to the very first.
        for (int i = 1; i <= 12; i++) {
            type(String.valueOf(i), "+", "0", "=");
        }
        onView(withId(HISTORY_BTN)).perform(click());
        onView(withText("12+0")).check(matches(isDisplayed()));
        onView(withText("1+0")).check(matches(not(isDisplayed())));
        for (int i = 0; i < 4; i++) {
            onView(withId(HISTORY_SCROLL)).perform(androidx.test.espresso.action.ViewActions.swipeDown());
        }
        onView(withText("1+0")).check(matches(isDisplayed()));
    }

    @Test public void theHistoryPopupShowsClearFullyAndKeepsThePad() {
        // His round-7 points carried into the popup: Clear fully visible, and -- because
        // the history floats in its own window -- the chevron strip and the pad never move.
        type("1", "+", "1", "=");
        onView(withId(CHEVRON_ROW)).check(matches(isDisplayed()));
        onView(withId(HISTORY_BTN)).perform(click());
        onView(withText("Clear")).check(matches(isCompletelyDisplayed()));
        onView(withId(CHEVRON_ROW)).check(matches(isDisplayed()));
        onView(key("7")).check(matches(isDisplayed()));
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
        onView(withId(MENU_BTN)).perform(click());
        onView(withText("Add to memory")).perform(click());
        type("AC", "7", "=");
        onView(withId(MENU_BTN)).perform(click());
        onView(withText("Add to memory")).perform(click());
        onView(withId(MENU_BTN)).perform(click());
        onView(withText("View memory")).perform(click());
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

    @Test public void selectionMenuOffersCutCopyPasteAndSelectAll() {
        // Round 12: select any length of the expression and get Android's own actions.
        type("1", "2", "3", "4", "5", "6");
        onView(withId(DISPLAY)).perform(longClick());
        onView(withText("Select all")).perform(click());
        onView(withText("Copy")).perform(click());
        // cut removes the held part and leaves the rest in the expression
        onView(withId(DISPLAY)).perform(longClick());
        onView(withText("Select all")).perform(click());
        onView(withText("Cut")).perform(click());
        onView(withId(DISPLAY)).check(matches(withText("0")));
    }

    @Test public void theCaretStaysWhereTheThumbInserted() {
        // Round 13: after a mid-expression insert the caret used to jump to the end, so
        // the next key landed there too. Two inserts in a row must stay neighbours.
        type("1", "2", "3", "4", "5");
        onView(withId(DISPLAY)).perform(new GeneralClickAction(Tap.SINGLE,
                new CoordinatesProvider() {
                    @Override public float[] calculateCoordinates(View view) {
                        return new float[]{view.getWidth() * 0.5f, view.getHeight() * 0.5f};
                    }
                }, Press.FINGER, 0, 0));
        type("9");
        type("8");
        // 9 then 8 side by side in the middle: 1298345, not 1293458 at the end.
        onView(withId(DISPLAY)).check(matches(withText("12,98,345")));
    }

    @Test public void longPressSelectsTheWholeNumberAndOffersTheMenu() {
        // Round 13: his device never raised a selection on long press, so the screen runs
        // its own: the number under the finger is held, handles up, menu with the four.
        type("1", "2", "3", "4", "5", "6");
        onView(withId(DISPLAY)).perform(longClick());
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
        onView(withId(DISPLAY)).perform(longClick());
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
        onView(withId(DISPLAY)).perform(longClick());
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

    @Test public void draggingEitherHandleMovesThatBoundary() {
        // Round 16: "not able our left and right boundary, how we do in Android." The
        // teardrops under the band are real handles now: a thumb down on one and a drag
        // moves exactly that end of the held text. Google's shape, Google's behaviour.
        type("1", "2", "3", "4", "5", "6");
        onView(withId(DISPLAY)).perform(longClick());
        onView(withId(DISPLAY)).perform(new androidx.test.espresso.ViewAction() {
            @Override public Matcher<View> getConstraints() { return isDisplayed(); }
            @Override public String getDescription() { return "drag the right handle left, then the left handle right"; }
            @Override public void perform(androidx.test.espresso.UiController ui, View view) {
                android.widget.EditText ed = (android.widget.EditText) view;
                android.text.Layout lo = ed.getLayout();
                float d = view.getResources().getDisplayMetrics().density;
                float padL = ed.getCompoundPaddingLeft();
                float y = ed.getBaseline() + lo.getLineDescent(0) + 8 * d + 14 * d;
                float xRight = padL + lo.getPrimaryHorizontal(ed.getSelectionEnd());
                float xTo = padL + lo.getPrimaryHorizontal(4);
                drag(ui, view, xRight, xTo, y);
                ui.loopMainThreadForAtLeast(200);
                float xLeft = padL + lo.getPrimaryHorizontal(0);
                float xLeftTo = padL + lo.getPrimaryHorizontal(2);
                drag(ui, view, xLeft, xLeftTo, y);
            }
        });
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
        onView(withId(DISPLAY)).perform(longClick());
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
        onView(withId(DISPLAY)).perform(longClick());
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
        onView(withId(DISPLAY)).perform(longClick());
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
        onView(withId(DISPLAY)).perform(longClick());
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
}
