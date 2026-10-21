package app.space.feature.expenses;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ExpenseRulesTest {

    private static ExpenseRules.Expense e(String id, String cat, long minor, long at) {
        return new ExpenseRules.Expense(id, cat, "", minor, at);
    }

    @Test public void totalsInMinorUnitsWithoutRoundingError() {
        // 0.1 + 0.2 in doubles is 0.30000000000000004; in minor units it is exactly 30.
        assertEquals(30L, ExpenseRules.total(Arrays.asList(e("1", "a", 10, 0), e("2", "b", 20, 0))));
        assertEquals(0L, ExpenseRules.total(null));
        assertEquals(0L, ExpenseRules.total(new ArrayList<ExpenseRules.Expense>()));
    }

    @Test public void categoryBreakdownIsLargestFirstThenAlphabetical() {
        List<Map.Entry<String, Long>> out = ExpenseRules.byCategory(Arrays.asList(
                e("1", "Food", 500, 0), e("2", "Travel", 900, 0), e("3", "Food", 400, 0),
                e("4", "Bills", 900, 0)));
        assertEquals(3, out.size());
        // Travel and Bills tie on 900, so they sort by name rather than by insertion
        // order, which would otherwise change when the list is re-read.
        assertEquals("Bills", out.get(0).getKey());
        assertEquals(Long.valueOf(900), out.get(0).getValue());
        assertEquals("Travel", out.get(1).getKey());
        assertEquals("Food", out.get(2).getKey());
        assertEquals(Long.valueOf(900), out.get(2).getValue());
    }

    @Test public void aBlankCategoryBecomesUncategorised() {
        assertEquals("Uncategorised", new ExpenseRules.Expense("1", "  ", "", 1, 0).category);
        assertEquals("Uncategorised", new ExpenseRules.Expense("1", null, "", 1, 0).category);
        assertEquals("Food", new ExpenseRules.Expense("1", " Food ", "", 1, 0).category);
    }

    @Test public void monthFilterIsHalfOpen() {
        List<ExpenseRules.Expense> out = ExpenseRules.inMonth(
                Arrays.asList(e("1", "a", 1, 100), e("2", "a", 1, 200), e("3", "a", 1, 300)), 100, 300);
        // The exclusive upper bound is what stops an expense on the first instant of next
        // month being counted in both months.
        assertEquals(2, out.size());
        assertTrue(ExpenseRules.inMonth(null, 0, 1).isEmpty());
    }

    @Test public void sortsNewestFirstWithStableTies() {
        List<ExpenseRules.Expense> out = ExpenseRules.sortedByNewest(Arrays.asList(
                e("b", "x", 1, 100), e("a", "x", 1, 100), e("c", "x", 1, 300)));
        assertEquals("c", out.get(0).id);
        assertEquals("a", out.get(1).id);
        assertEquals("b", out.get(2).id);
    }

    @Test public void parsesAmountsIntoMinorUnits() {
        assertEquals(Long.valueOf(12000), ExpenseRules.parseAmountMinor("120"));
        assertEquals(Long.valueOf(12050), ExpenseRules.parseAmountMinor("120.50"));
        assertEquals(Long.valueOf(12050), ExpenseRules.parseAmountMinor("120.5"));
        assertEquals(Long.valueOf(5), ExpenseRules.parseAmountMinor(".05"));
        assertEquals(Long.valueOf(0), ExpenseRules.parseAmountMinor("0"));
        assertEquals(Long.valueOf(-12000), ExpenseRules.parseAmountMinor("-120"));
        assertEquals(Long.valueOf(12000), ExpenseRules.parseAmountMinor("  120  "));
        // A trailing decimal point is what a user leaves behind when they change their mind
        // mid-typing. Padding it to whole units is friendlier than refusing the entry, and
        // it cannot misstate the amount: 12. is 12.00, that is 1200 minor units.
        assertEquals(Long.valueOf(1200), ExpenseRules.parseAmountMinor("12."));
        assertEquals(Long.valueOf(0), ExpenseRules.parseAmountMinor("0."));
    }

    @Test public void refusesInputThatIsNotMoneyRatherThanStoringZero() {
        // Returning null is the point: a typo stored as 0 would silently skew every total.
        for (String bad : new String[]{"", "   ", null, "abc", "12.345", "1.2.3", "--5", ".", "1.2.", "12,50"}) {
            assertNull("should reject: '" + bad + "'", ExpenseRules.parseAmountMinor(bad));
        }
    }

    @Test public void formatsWithExactlyTwoDecimalPlaces() {
        assertEquals("120.00", ExpenseRules.format(12000));
        assertEquals("0.05", ExpenseRules.format(5));
        assertEquals("120.50", ExpenseRules.format(12050));
        assertEquals("-120.00", ExpenseRules.format(-12000));
        assertEquals("0.00", ExpenseRules.format(0));
    }

    @Test public void theCapKeepsTheNewest() {
        List<ExpenseRules.Expense> in = new ArrayList<>();
        for (int i = 0; i < 10; i++) in.add(e("n" + i, "a", 1, i));
        List<ExpenseRules.Expense> out = ExpenseRules.capped(in, 4);
        assertEquals(4, out.size());
        assertEquals("n9", out.get(0).id);
    }
}
