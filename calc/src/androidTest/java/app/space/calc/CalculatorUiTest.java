package app.space.calc;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.swipeUp;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.Visibility.GONE;
import static androidx.test.espresso.matcher.ViewMatchers.hasFocus;
import static androidx.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
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
 * collapsing from the dock, and a caret that exists, is focused and is visible.
 */
@RunWith(AndroidJUnit4.class)
public class CalculatorUiTest {

    private static final int DISPLAY = app.space.feature.calculator.R.id.calc_display;
    private static final int HISTORY_SCROLL = app.space.feature.calculator.R.id.calc_history_scroll;
    private static final int HISTORY_BTN = app.space.feature.calculator.R.id.calc_history_btn;
    private static final int CHEVRON_ROW = app.space.feature.calculator.R.id.calc_chevron_row;

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

    @Test public void historyOpensAndCollapsesFromAnUpwardSwipeOnTheDock() {
        type("1", "+", "1", "=");
        onView(withId(HISTORY_BTN)).perform(click());
        onView(withId(HISTORY_SCROLL)).check(matches(isDisplayed()));
        // His fourth point: scrolling up from the expression box collapses the sheet.
        onView(withId(DISPLAY)).perform(swipeUp());
        onView(withId(HISTORY_SCROLL)).check(matches(withEffectiveVisibility(GONE)));
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

    @Test public void theSheetHidesTheChevronAndShowsClearFully() {
        // His fourth and fifth points tonight.
        type("1", "+", "1", "=");
        onView(withId(CHEVRON_ROW)).check(matches(isDisplayed()));
        onView(withId(HISTORY_BTN)).perform(click());
        onView(withId(CHEVRON_ROW)).check(matches(withEffectiveVisibility(GONE)));
        onView(withText("Clear")).check(matches(isCompletelyDisplayed()));
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
