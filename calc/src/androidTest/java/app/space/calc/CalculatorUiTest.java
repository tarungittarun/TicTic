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
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertTrue;

import android.view.View;
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
