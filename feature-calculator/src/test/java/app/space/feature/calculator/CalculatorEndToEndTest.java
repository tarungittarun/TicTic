package app.space.feature.calculator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

/**
 * End-to-end journeys without a device: every test walks a full user story the way the
 * screen walks it -- key by key through appendToken, then evaluation, formatting, history
 * storage and reuse -- so the pipeline is tested as a pipeline, not only as parts. The
 * owner's demand of 2026-10-26 ("why these kind of basic are not tested") is answered
 * here for everything that is pure logic; the screen-level counterpart runs on an
 * emulator in the Tests workflow.
 */
public class CalculatorEndToEndTest {

    /** The screen's door: one key at a time, tokens included, exactly as typeKey calls it. */
    private static String type(String expression, String... keys) {
        String current = expression;
        for (String key : keys) {
            String next = CalculatorRules.appendToken(current, key);
            if (next == null) fail("key refused mid-journey: " + key + " after " + current);
            current = next;
        }
        return current;
    }

    @Test public void theOwnersLogSequenceNowEndsInAnAnswer() {
        // From his log of 2026-10-25 evening: 533+23+ then = refused the second + and the
        // whole journey died at the pad door. Now the second + is a changed mind, the
        // expression completes, evaluates, formats and lands in history with a timestamp.
        String expr = type("", "5", "3", "3", "+", "2", "3");
        assertEquals("533+23", expr);
        // Pressing + twice is the same pending operator twice; the rules never stack it.
        String withPending = CalculatorRules.appendToken(expr, "+");
        assertEquals("533+23+", withPending);
        assertEquals("533+23+", CalculatorRules.appendToken(withPending, "+"));
        double value = CalculatorRules.evaluate(withPending + "7");
        assertEquals(563d, value, 1e-9);
        String store = CalculatorRules.pushHistory("", "533+23+7", CalculatorRules.format(value), 50);
        String[][] rows = CalculatorRules.historyEntries(store);
        assertEquals(1, rows.length);
        assertEquals("533+23+7", rows[0][0]);
        assertEquals("563", rows[0][1]);
        assertTrue("timestamp rides the entry", rows[0][2].length() > 0);
    }

    @Test public void theChangeableMindSurvivesTheWholePipeline() {
        // 10+ then − is 10− at the pad door (appendToken), and completing it evaluates:
        // the rule, the door and the engine must agree, or the thumb sees "not taking".
        String expr = type("", "1", "0", "+");
        assertEquals("10-", CalculatorRules.appendToken(expr, "-"));
        assertEquals(5d, CalculatorRules.evaluate(CalculatorRules.appendToken(expr, "-") + "5"), 1e-9);
        // And the percent joins the changeable set the same way.
        assertEquals("10%", CalculatorRules.appendToken(expr, "%"));
        assertEquals(0.10d, CalculatorRules.evaluate("10%"), 1e-9);
    }

    @Test public void implicitMultiplicationAnswersTwentyEight() {
        // His exact example: 2(5+2)2 is 28 on Google's calculator. Every key must be
        // accepted at the door and the engine must supply the multiplication.
        String expr = type("", "2", "(", "5", "+", "2", ")", "2");
        assertEquals("2(5+2)2", expr);
        assertEquals(28d, CalculatorRules.evaluate(expr), 1e-9);
        assertEquals("2*(5+2)*2", CalculatorRules.withImplicitMultiplication(expr));
        // The neighbouring shapes behave too.
        assertEquals(12d, CalculatorRules.evaluate("3(4)"), 1e-9);
        assertEquals(6d, CalculatorRules.evaluate("(2)(3)"), 1e-9);
        assertEquals(4d, CalculatorRules.evaluate("5!(2)/60"), 1e-9);
        // And a named function still needs an operand slot, not a value:
        // implicit multiplication opens bare groups, not function calls typed wrong.
        assertNull(CalculatorRules.appendToken("2", "sin("));
    }

    @Test public void aCompletedCalculationContinuesLikeGoogles() {
        // Equals stores the result as the new expression state: an operator continues
        // from the result, a digit starts fresh. The rules half of that contract:
        String result = CalculatorRules.format(CalculatorRules.evaluate("635"));
        assertEquals("635", result);
        assertEquals("635-", CalculatorRules.appendToken(result, "-"));
        assertEquals("5", CalculatorRules.appendToken("", "5"));
    }

    @Test public void percentagesFollowTheDocumentedSemantics() {
        assertEquals(220d, CalculatorRules.evaluate("200+10%"), 1e-9);
        assertEquals(180d, CalculatorRules.evaluate("200-10%"), 1e-9);
        assertEquals(20d, CalculatorRules.evaluate("200*10%"), 1e-9);
        // Under / a percentage is plain hundredths, as under *: 200 / 0.1 = 2000.
        assertEquals(2000d, CalculatorRules.evaluate("200/10%"), 1e-9);
    }

    @Test public void trigonometryTravelsWithTheAngleMode() {
        assertEquals(0.5, CalculatorRules.evaluate("sin(30)", true), 1e-9);
        assertEquals(Math.sin(30), CalculatorRules.evaluate("sin(30)", false), 1e-9);
        assertEquals(1d, CalculatorRules.evaluate("tan(45)", true), 1e-9);
        assertEquals(2d, CalculatorRules.evaluate("log(100)", true), 1e-9);
        assertEquals(1d, CalculatorRules.evaluate("ln(e)", true), 1e-9);
        assertEquals(12d, CalculatorRules.evaluate("sqrt(144)", true), 1e-9);
        assertEquals(3d, CalculatorRules.evaluate("cbrt(27)", true), 1e-9);
        assertEquals(1024d, CalculatorRules.evaluate("2^10", true), 1e-9);
        assertEquals(120d, CalculatorRules.evaluate("5!", true), 1e-9);
        assertEquals(9d, CalculatorRules.evaluate("(-3)^2", true), 1e-9);
    }

    @Test public void invalidMathIsAnErrorStateNeverACrashOrAGuess() {
        try { CalculatorRules.evaluate("1/0"); fail("division by zero must throw"); } catch (Exception expected) { }
        try { CalculatorRules.evaluate("(-1)!"); fail("negative factorial must throw"); } catch (Exception expected) { }
        try { CalculatorRules.evaluate("ln(0)"); fail("log of zero must throw"); } catch (Exception expected) { }
        try { CalculatorRules.evaluate("sqrt(-4)"); fail("root of a negative must throw"); } catch (Exception expected) { }
        try { CalculatorRules.evaluate("2(5+2"); fail("unbalanced groups must throw"); } catch (Exception expected) { }
        assertEquals("Error", CalculatorRules.format(Double.NaN));
        assertEquals("Error", CalculatorRules.format(Double.POSITIVE_INFINITY));
    }

    @Test public void historyIsACappedOrderedDeduplicatedStore() {
        String store = "";
        store = CalculatorRules.pushHistory(store, "1+1", "2", 3);
        store = CalculatorRules.pushHistory(store, "1+1", "2", 3);   // duplicate: refused
        assertEquals(1, CalculatorRules.historyEntries(store).length);
        store = CalculatorRules.pushHistory(store, "2+2", "4", 3);
        store = CalculatorRules.pushHistory(store, "3+3", "6", 3);
        store = CalculatorRules.pushHistory(store, "4+4", "8", 3);   // over the cap
        String[][] rows = CalculatorRules.historyEntries(store);
        assertEquals(3, rows.length);
        assertEquals("4+4", rows[0][0]);  // newest first in the store...
        assertEquals("2+2", rows[2][0]);  // ...oldest last; the screen renders the reverse,
                                          // newest at the bottom, the owner's order.
    }

    @Test public void formattingIsReadableAtEveryMagnitude() {
        assertEquals("0.3", CalculatorRules.format(0.1 + 0.2));
        assertEquals("28", CalculatorRules.format(28d));
        assertEquals("1e+20", CalculatorRules.format(1e20));
        assertEquals("1.5e-12", CalculatorRules.format(1.5e-12));
        assertEquals("-47", CalculatorRules.format(-47d));
    }
}
