package app.space.feature.calculator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

/**
 * The arithmetic is the part worth testing. There is no emulator in this project, so a
 * keypad cannot be exercised here; a parser over Strings can be, which is exactly why the
 * evaluator lives apart from the screen.
 */
public class CalculatorRulesTest {

    private static final double EPS = 1e-9;

    @Test public void keysThatCannotAppearHereAreRefusedAtTheKey() {
        // The display must never hold a sequence that is not a number in progress. This is the
        // defect the owner hit: "-+-++-52xxx-+++" was accepted because the rules only ran at
        // equals, not at input.
        assertNull(CalculatorRules.append("", "+"));
        assertNull(CalculatorRules.append("", "*"));
        assertEquals("-", CalculatorRules.append("", "-"));
        assertEquals("5", CalculatorRules.append("", "5"));
        assertEquals("5+", CalculatorRules.append("5", "+"));
        // A second operator replaces the first rather than stacking, except minus, which may
        // follow an operator to make it negative.
        assertEquals("5*", CalculatorRules.append("5+", "*"));
        assertEquals("5+-", CalculatorRules.append("5+", "-"));
        assertEquals("5+2", CalculatorRules.append("5+", "2"));
        // A minus chain was the exact garbage typed on device: "0/0------85522*3*-". One negation
        // minus may follow an operator; a second replaces it, so the chain cannot grow.
        assertEquals("5+-", CalculatorRules.append("5+-", "-"));
        assertEquals("5-", CalculatorRules.append("5-", "-"));
        assertEquals("-", CalculatorRules.append("-", "-"));
        assertEquals("5*", CalculatorRules.append("5*-", "*"));
        // Swapping operators over a pending negation drops the negation: replacing only the last
        // character there stacks two operators, which is what the second red run of this slice was.
        assertEquals("5+", CalculatorRules.append("5*-", "+"));
        assertEquals("5+", CalculatorRules.append("5+-", "+"));
        assertEquals("5*-", CalculatorRules.append("5*-", "-"));
        // A lone leading minus is not an operand; only another minus may follow it.
        assertNull(CalculatorRules.append("-", "*"));
        assertNull(CalculatorRules.append("-", "+"));
        // Brackets open where an operand may start and close only over an open group.
        assertEquals("(", CalculatorRules.append("", "("));
        assertEquals("(2", CalculatorRules.append("(", "2"));
        assertEquals("(2)", CalculatorRules.append("(2", ")"));
        assertEquals("(2+3)*", CalculatorRules.append("(2+3)", "*"));
        assertNull(CalculatorRules.append("2", "("));
        assertNull(CalculatorRules.append("", ")"));
        assertNull(CalculatorRules.append("()", ")"));
        assertNull(CalculatorRules.append("5.2", "."));
        assertEquals("5.2", CalculatorRules.append("5.", "2"));
        // Display glyphs type their ASCII character.
        assertEquals("8/", CalculatorRules.append("8", "\u00f7"));
        assertEquals("8*", CalculatorRules.append("8", "\u00d7"));
    }

    @Test public void addsAndSubtracts() {
        assertEquals(7d, CalculatorRules.evaluate("3+4"), EPS);
        assertEquals(-1d, CalculatorRules.evaluate("3-4"), EPS);
        assertEquals(6d, CalculatorRules.evaluate("1+2+3"), EPS);
    }

    @Test public void multipliesBeforeAdding() {
        // Left-to-right folding would give 20 here; precedence gives 14.
        assertEquals(14d, CalculatorRules.evaluate("2+3*4"), EPS);
        assertEquals(10d, CalculatorRules.evaluate("2*3+4"), EPS);
        assertEquals(2d, CalculatorRules.evaluate("10-4*2"), EPS);
    }

    @Test public void dividesAndReportsDivisionByZeroAsArithmetic() {
        assertEquals(2.5d, CalculatorRules.evaluate("5/2"), EPS);
        try {
            CalculatorRules.evaluate("5/0");
            fail("division by zero must not return a value");
        } catch (ArithmeticException expected) {
            assertTrue(expected.getMessage().contains("division by zero"));
        }
    }

    @Test public void honoursParentheses() {
        assertEquals(20d, CalculatorRules.evaluate("(2+3)*4"), EPS);
        assertEquals(9d, CalculatorRules.evaluate("(1+2)*(1+2)"), EPS);
        assertEquals(14d, CalculatorRules.evaluate("2*(3+4)"), EPS);
    }

    @Test public void handlesUnaryMinusAndNestedGroups() {
        assertEquals(-5d, CalculatorRules.evaluate("-5"), EPS);
        assertEquals(-1d, CalculatorRules.evaluate("-(3-2)"), EPS);
        assertEquals(1d, CalculatorRules.evaluate("-(-1)"), EPS);
        assertEquals(3d, CalculatorRules.evaluate("1--2"), EPS);
    }

    @Test public void acceptsDecimals() {
        assertEquals(0.3d, CalculatorRules.evaluate("0.1+0.2"), 1e-9);
        assertEquals(1.5d, CalculatorRules.evaluate(".5+1"), EPS);
    }

    @Test public void malformedInputThrowsRatherThanReturningAWrongNumber() {
        for (String bad : new String[]{"", "   ", "3+", "*3", "3 4", "(3+4", "3+4)", "abc", "3++"}) {
            try {
                CalculatorRules.evaluate(bad);
                fail("expected ArithmeticException for: '" + bad + "'");
            } catch (ArithmeticException expected) {
                // the point of the test
            }
        }
    }

    @Test public void toleratesSurroundingWhitespace() {
        assertEquals(7d, CalculatorRules.evaluate("  3 + 4  "), EPS);
    }

    @Test public void formatsWholeNumbersWithoutADecimalPoint() {
        assertEquals("7", CalculatorRules.format(7d));
        assertEquals("-7", CalculatorRules.format(-7d));
        assertEquals("0", CalculatorRules.format(0d));
    }

    @Test public void formatsFractionsWithoutTrailingZeros() {
        assertEquals("2.5", CalculatorRules.format(2.5d));
        assertEquals("0.3", CalculatorRules.format(0.3d));
        // 1/3 must not print as 0.33333333330000001 or similar padding.
        assertEquals("0.3333333333", CalculatorRules.format(1d / 3d));
    }

    @Test public void formatsNonFiniteResultsAsTheErrorTheDisplayShows() {
        assertEquals("Error", CalculatorRules.format(Double.NaN));
        assertEquals("Error", CalculatorRules.format(Double.POSITIVE_INFINITY));
    }

    @Test public void onlyArithmeticCharactersMayBeAppended() {
        for (char ok : new char[]{'0', '9', '.', '+', '-', '*', '/', '(', ')', '%', '^', '!',
                '\u03c0', 'e'}) {
            assertTrue("should accept " + ok, CalculatorRules.isInputCharacter(ok));
        }
        // The keypad's typographic glyphs must be rejected here, which is what forces the
        // screen to translate them before appending.
        assertFalse(CalculatorRules.isInputCharacter('\u00f7'));
        assertFalse(CalculatorRules.isInputCharacter('\u00d7'));
        assertFalse(CalculatorRules.isInputCharacter('\u2212'));
        // Letters became input characters with the scientific page: function names type
        // straight into the expression. 'a' was a refusal case when the vocabulary was
        // digits and operators; it is the second letter of "tan" now.
        assertTrue(CalculatorRules.isInputCharacter('a'));
    }

    @Test public void inputIsBounded() {
        assertTrue(CalculatorRules.MAX_LENGTH > 0);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < CalculatorRules.MAX_LENGTH + 20; i++) sb.append('1');
        // Evaluating a very long expression must not hang or overflow the stack.
        CalculatorRules.evaluate(sb.toString());
    }

    @Test public void percentIsGoogleNotPrimarySchool() {
        // On its own and under * and /: plain hundredths.
        assertEquals(0.5, CalculatorRules.evaluate("50%"), 1e-9);
        assertEquals(20d, CalculatorRules.evaluate("200*10%"), 1e-9);
        assertEquals(0.5, CalculatorRules.evaluate("(50)%"), 1e-9);
        // Under + and -: that percent OF THE RUNNING SUM, which is what Google computes.
        assertEquals(220d, CalculatorRules.evaluate("200+10%"), 1e-9);
        assertEquals(180d, CalculatorRules.evaluate("200-10%"), 1e-9);
        assertEquals(2.24, CalculatorRules.evaluate("2+3*4%"), 1e-9);
        assertEquals(180d, CalculatorRules.evaluate("200+-10%"), 1e-9);
    }

    @Test public void percentIsPostfixAndRefusedWithoutANumber() {
        assertEquals("5%", CalculatorRules.append("5", "%"));
        assertEquals("(5)%", CalculatorRules.append("(5)", "%"));
        assertNull(CalculatorRules.append("", "%"));
        assertNull(CalculatorRules.append("5+", "%"));
        assertNull(CalculatorRules.append("5%", "%"));
    }

    @Test public void scientificPanelIsDegreesByDefaultWithARadianEscape() {
        assertEquals(0.5, CalculatorRules.evaluate("sin(30)"), 1e-9);
        assertEquals(0.5, CalculatorRules.evaluate("cos(60)"), 1e-9);
        assertEquals(1d, CalculatorRules.evaluate("tan(45)"), 1e-9);
        assertEquals(30d, CalculatorRules.evaluate("asin(0.5)"), 1e-9);
        assertEquals(Math.sin(30), CalculatorRules.evaluate("sin(30)", false), 1e-9);
        assertEquals(1d, CalculatorRules.evaluate("ln(e)"), 1e-9);
        assertEquals(2d, CalculatorRules.evaluate("log(100)"), 1e-9);
        assertEquals(3d, CalculatorRules.evaluate("sqrt(9)"), 1e-9);
        assertEquals(3d, CalculatorRules.evaluate("\u221a(9)"), 1e-9);
        assertEquals(Math.PI, CalculatorRules.evaluate("\u03c0"), 1e-9);
    }

    @Test public void powerIsRightAssociativeAndBindsTighterThanMultiply() {
        assertEquals(512d, CalculatorRules.evaluate("2^3^2"), 1e-9);
        assertEquals(1024d, CalculatorRules.evaluate("2^10"), 1e-9);
        assertEquals(50d, CalculatorRules.evaluate("2+3*4^2"), 1e-9);
        assertEquals(-4d, CalculatorRules.evaluate("-2^2"), 1e-9);
    }

    @Test public void factorialIsIntegersOnlyWithinDoubleRange() {
        assertEquals(120d, CalculatorRules.evaluate("5!"), 1e-9);
        assertEquals(1d, CalculatorRules.evaluate("0!"), 1e-9);
        try { CalculatorRules.evaluate("171!"); fail("171! overflows a double"); }
        catch (ArithmeticException expected) { }
        try { CalculatorRules.evaluate("(-1)!"); fail("negative factorial"); }
        catch (ArithmeticException expected) { }
        try { CalculatorRules.evaluate("asin(2)"); fail("asin domain"); }
        catch (ArithmeticException expected) { }
        try { CalculatorRules.evaluate("sqrt(-1)"); fail("sqrt domain"); }
        catch (ArithmeticException expected) { }
    }

    @Test public void scientificTokensObeyTheSameRefusalDisciplineAsKeys() {
        assertEquals("sin(", CalculatorRules.appendToken("", "sin("));
        assertEquals("5+sin(", CalculatorRules.appendToken("5+", "sin("));
        assertNull(CalculatorRules.appendToken("5", "sin("));
        assertEquals("5!", CalculatorRules.appendToken("5", "!"));
        assertNull(CalculatorRules.appendToken("", "!"));
        assertNull(CalculatorRules.appendToken("5!", "!"));
        assertEquals("5!^", CalculatorRules.appendToken("5!", "^"));
        assertEquals("5^", CalculatorRules.appendToken("5", "^"));
        assertNull(CalculatorRules.appendToken("5+", "^"));
        assertEquals("\u03c0", CalculatorRules.appendToken("", "\u03c0"));
        assertNull(CalculatorRules.appendToken("5", "e"));
    }

    @Test public void historyIsNewestFirstDeduplicatedAndCapped() {
        String store = CalculatorRules.pushHistory("", "1+1", "2", 50);
        store = CalculatorRules.pushHistory(store, "2+2", "4", 50);
        String[][] entries = CalculatorRules.historyEntries(store);
        assertEquals(2, entries.length);
        assertEquals("2+2", entries[0][0]);
        assertEquals("4", entries[0][1]);
        // A repeat moves to the top instead of duplicating.
        store = CalculatorRules.pushHistory(store, "1+1", "2", 50);
        entries = CalculatorRules.historyEntries(store);
        assertEquals(2, entries.length);
        assertEquals("1+1", entries[0][0]);
        // The cap drops the oldest, never the newest.
        store = CalculatorRules.pushHistory(store, "3+3", "6", 2);
        assertEquals(2, CalculatorRules.historyEntries(store).length);
        assertEquals("3+3", CalculatorRules.historyEntries(store)[0][0]);
        // A corrupt line is skipped, not crashed on: history is prefs text, and prefs text
        // is whatever an older version or a restored backup left behind.
        assertEquals(1, CalculatorRules.historyEntries("corrupt\n1+1=2").length);
        assertEquals(0, CalculatorRules.historyEntries("").length);
    }
}
