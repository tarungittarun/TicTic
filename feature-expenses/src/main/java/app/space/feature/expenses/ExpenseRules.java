package app.space.feature.expenses;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Expense totals and category breakdown.
 *
 * <p>Money is held in minor units (paise/cents) as longs throughout. Using doubles for
 * currency is how a total comes out as 30.000000000000004, and it is wrong in a way the
 * user notices immediately in an app whose only job is adding numbers up.
 */
public final class ExpenseRules {

    private ExpenseRules() {}

    /** One expense, in minor units. */
    public static final class Expense {
        public final String id;
        public final String category;
        public final String note;
        public final long amountMinor;
        public final long occurredAt;

        public Expense(String id, String category, String note, long amountMinor, long occurredAt) {
            this.id = id == null ? "" : id;
            this.category = category == null || category.trim().isEmpty() ? "Uncategorised" : category.trim();
            this.note = note == null ? "" : note;
            this.amountMinor = amountMinor;
            this.occurredAt = occurredAt;
        }
    }

    public static long total(List<Expense> expenses) {
        long sum = 0;
        if (expenses != null) for (Expense e : expenses) if (e != null) sum += e.amountMinor;
        return sum;
    }

    /** Totals per category, largest first, then alphabetically so the order is stable. */
    public static List<Map.Entry<String, Long>> byCategory(List<Expense> expenses) {
        Map<String, Long> sums = new LinkedHashMap<>();
        if (expenses != null) {
            for (Expense e : expenses) {
                if (e == null) continue;
                Long prev = sums.get(e.category);
                sums.put(e.category, (prev == null ? 0L : prev) + e.amountMinor);
            }
        }
        List<Map.Entry<String, Long>> out = new ArrayList<>(sums.entrySet());
        Collections.sort(out, new Comparator<Map.Entry<String, Long>>() {
            @Override public int compare(Map.Entry<String, Long> a, Map.Entry<String, Long> b) {
                if (!a.getValue().equals(b.getValue())) return b.getValue().compareTo(a.getValue());
                return a.getKey().compareTo(b.getKey());
            }
        });
        return out;
    }

    /** Expenses inside one calendar month, using the device's own day boundaries. */
    public static List<Expense> inMonth(List<Expense> expenses, long fromMillisInclusive, long toMillisExclusive) {
        List<Expense> out = new ArrayList<>();
        if (expenses == null) return out;
        for (Expense e : expenses) {
            if (e == null) continue;
            if (e.occurredAt >= fromMillisInclusive && e.occurredAt < toMillisExclusive) out.add(e);
        }
        return out;
    }

    /** Newest first; ties break on id so a re-render cannot reshuffle the list. */
    public static List<Expense> sortedByNewest(List<Expense> expenses) {
        List<Expense> out = new ArrayList<>(expenses == null ? Collections.<Expense>emptyList() : expenses);
        Collections.sort(out, new Comparator<Expense>() {
            @Override public int compare(Expense a, Expense b) {
                if (a.occurredAt != b.occurredAt) return a.occurredAt < b.occurredAt ? 1 : -1;
                return a.id.compareTo(b.id);
            }
        });
        return out;
    }

    /** Parses user input in major units into minor units; returns null if it is not money. */
    public static Long parseAmountMinor(String input) {
        if (input == null) return null;
        String s = input.trim();
        if (s.isEmpty()) return null;
        boolean negative = false;
        if (s.charAt(0) == '-') { negative = true; s = s.substring(1).trim(); }
        if (s.isEmpty()) return null;
        String[] parts = s.split("\\.", -1);
        if (parts.length > 2) return null;
        String whole = parts[0];
        String fraction = parts.length == 2 ? parts[1] : "";
        if (whole.isEmpty() && fraction.isEmpty()) return null;
        if (!whole.isEmpty() && !whole.matches("[0-9]+")) return null;
        if (fraction.length() > 2 || !fraction.matches("[0-9]*")) return null;
        while (fraction.length() < 2) fraction = fraction + "0";
        long value;
        try {
            value = (whole.isEmpty() ? 0L : Long.parseLong(whole)) * 100L + Long.parseLong(fraction.isEmpty() ? "0" : fraction);
        } catch (NumberFormatException e) {
            return null;
        }
        return negative ? -value : value;
    }

    /** Formats minor units for display, always two decimal places. */
    public static String format(long amountMinor) {
        boolean negative = amountMinor < 0;
        long abs = Math.abs(amountMinor);
        String s = (abs / 100) + "." + String.format(java.util.Locale.ROOT, "%02d", abs % 100);
        return negative ? "-" + s : s;
    }

    /** Caps stored rows so the list cannot grow without bound. */
    public static List<Expense> capped(List<Expense> expenses, int max) {
        List<Expense> all = sortedByNewest(expenses);
        if (max <= 0 || all.size() <= max) return all;
        return new ArrayList<>(all.subList(0, max));
    }
}
